/*
 * Copyright 2013-2026 the HotswapAgent authors.
 *
 * This file is part of HotswapAgent.
 *
 * HotswapAgent is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 2 of the License, or (at your
 * option) any later version.
 *
 * HotswapAgent is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with HotswapAgent. If not, see http://www.gnu.org/licenses/.
 */
package org.hotswap.agent.plugin.spring.files;

import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.logging.AgentLoggerHandler;
import org.hotswap.agent.plugin.spring.reload.SpringChangedAgent;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class XmlBeanDefinitionScannerAgentTest {

    private final List<DefaultListableBeanFactory> beanFactories = new ArrayList<>();

    @After
    public void tearDown() {
        // the agent registers each bean factory for reloads, do not leave these to other tests
        for (DefaultListableBeanFactory beanFactory : beanFactories) {
            SpringChangedAgent.destroyBeanFactory(beanFactory);
        }
    }

    private DefaultListableBeanFactory newBeanFactory() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactories.add(beanFactory);
        return beanFactory;
    }

    /**
     * Spring Data describes the source of a repository bean definition only by text
     * ("... defined in @EnableJpaRepositories declared on ..."), there is no XML file to find an agent for.
     */
    @Test
    public void registerBeanWithResourceDescriptionDoesNotWarn() {
        RootBeanDefinition beanDefinition = new RootBeanDefinition(Object.class);
        beanDefinition.setResourceDescription(
                "app.PersonRepository defined in @EnableJpaRepositories declared on DatabaseConfiguration");

        List<String> warnings = collectWarnings(() ->
                XmlBeanDefinitionScannerAgent.registerBean("personRepository", beanDefinition));

        assertEquals(new ArrayList<String>(), warnings);
    }

    /**
     * The changed XML file is found in a classpath directory with any name, not only target/classes (#397).
     */
    @Test
    public void changedXmlInAnyClasspathDirectoryFindsAgent() throws Exception {
        DefaultListableBeanFactory beanFactory = newBeanFactory();
        XmlBeanDefinitionScannerAgent.registerXmlBeanDefinitionScannerAgent(new XmlBeanDefinitionReader(beanFactory),
                new ClassPathResource("xmlContext.xml"));

        XmlBeanDefinitionScannerAgent agent = XmlBeanDefinitionScannerAgent.getAgent(beanFactory,
                getClass().getClassLoader().getResource("xmlContext.xml"));
        assertNotNull(agent);
        for (String dir : new String[]{"target/classes", "build/resources/main", "bin", "out/production/resources",
                "src/main/resources"}) {
            assertSame(dir, agent, XmlBeanDefinitionScannerAgent.getAgent(beanFactory,
                    new URL("file:/project/" + dir + "/xmlContext.xml")));
        }
        assertNull(XmlBeanDefinitionScannerAgent.getAgent(beanFactory,
                new URL("file:/project/build/resources/main/otherxmlContext.xml")));
    }

    /**
     * All changed XML files are passed to the Spring plugin, e.g. MyBatis mappers (#656).
     */
    @Test
    public void changedXmlWithoutAgentDoesNotWarn() throws Exception {
        DefaultListableBeanFactory beanFactory = newBeanFactory();
        XmlBeanDefinitionScannerAgent.registerXmlBeanDefinitionScannerAgent(new XmlBeanDefinitionReader(beanFactory),
                new ClassPathResource("xmlContext.xml"));
        URL mapper = new URL("file:/project/build/resources/main/mapper/FooMapper.xml");

        List<String> warnings = collectWarnings(() -> assertTrue(XmlBeanDefinitionScannerAgent.reloadXmls(beanFactory,
                Collections.singleton(mapper), Collections.emptySet()).isEmpty()));

        assertEquals(new ArrayList<String>(), warnings);
    }

    /**
     * XML loaded from a byte array has no URL (#431).
     */
    @Test
    public void registerAgentForByteArrayResourceDoesNotFail() {
        DefaultListableBeanFactory beanFactory = newBeanFactory();
        List<String> warnings = collectWarnings(() ->
                XmlBeanDefinitionScannerAgent.registerXmlBeanDefinitionScannerAgent(
                        new XmlBeanDefinitionReader(beanFactory), new ByteArrayResource("<beans/>".getBytes())));

        assertEquals(new ArrayList<String>(), warnings);
    }

    private static List<String> collectWarnings(Runnable action) {
        List<String> warnings = new ArrayList<>();
        AgentLoggerHandler originalHandler = AgentLogger.getHandler();
        AgentLogger.setHandler(new AgentLoggerHandler() {
            @Override
            public void print(Class clazz, AgentLogger.Level level, String message, Throwable throwable, Object... args) {
                if (level == AgentLogger.Level.WARNING || level == AgentLogger.Level.ERROR) {
                    warnings.add(message);
                }
                super.print(clazz, level, message, throwable, args);
            }
        });
        try {
            action.run();
        } finally {
            AgentLogger.setHandler(originalHandler);
        }
        return warnings;
    }
}
