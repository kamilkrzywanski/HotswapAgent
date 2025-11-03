/*
 * Copyright 2013-2025 the HotswapAgent authors.
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
package org.hotswap.agent;

import static org.mockito.Mockito.*;

import org.hotswap.agent.annotation.handler.AnnotationProcessor;
import org.hotswap.agent.config.PluginManager;
import org.hotswap.agent.config.PluginRegistry;
import org.hotswap.agent.testData.SimplePlugin;
import org.hotswap.agent.util.scanner.ClassPathAnnotationScanner;
import org.junit.Test;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.Collections;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * @author Jiri Bubnik
 */
public class PluginManagerTest {

    @Test
    public void testInit() throws Exception {
        ClassPathAnnotationScanner annotationScanner = mock(ClassPathAnnotationScanner.class);
        AnnotationProcessor annotationProcessor = mock(AnnotationProcessor.class);
        Instrumentation instrumentation = mock(Instrumentation.class);

        doNothing().when(instrumentation).addTransformer(any(ClassFileTransformer.class));
        when(annotationScanner.scanPlugins(any(ClassLoader.class), anyString()))
                .thenReturn(Collections.singletonList(SimplePlugin.class.getName()));
        when(annotationProcessor.processAnnotations(any(Class.class), any(Class.class)))
                .thenReturn(true);

        PluginManager pluginManager = PluginManager.getInstance();
        PluginRegistry pluginRegistry = pluginManager.getPluginRegistry();
        pluginRegistry.setAnnotationScanner(annotationScanner);
        pluginRegistry.setAnnotationProcessor(annotationProcessor);

        pluginManager.init(instrumentation);

        assertEquals("Plugin registered", 1, pluginRegistry.getRegisteredPlugins().size());
        assertTrue("Plugin correct class",
                pluginRegistry.getRegisteredPlugins().keySet().iterator().next().equals(SimplePlugin.class));
    }


}
