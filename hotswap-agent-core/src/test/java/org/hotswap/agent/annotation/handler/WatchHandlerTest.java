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
package org.hotswap.agent.annotation.handler;

import org.hotswap.agent.annotation.OnClassFileEvent;
import org.hotswap.agent.annotation.Plugin;
import org.hotswap.agent.config.PluginConfiguration;
import org.hotswap.agent.config.PluginManager;
import org.hotswap.agent.config.PluginRegistry;
import org.hotswap.agent.watch.WatchEventListener;
import org.hotswap.agent.watch.Watcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WatchHandlerTest {

    @Plugin(name = "ClassFilePlugin", testedVersions = "")
    public static class ClassFilePlugin {
        @OnClassFileEvent(classNameRegexp = ".*")
        public void onClassFile() {
        }
    }

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    Mockery context = new Mockery() {{
        setImposteriser(ClassImposteriser.INSTANCE);
    }};

    /**
     * extraClasspath may be added to the class loader after its plugins were initialized (Tomcat plugin),
     * so it must be watched even when the class loader does not contain it yet.
     */
    @Test
    public void watchesExtraClasspathNotKnownToClassLoader() throws Exception {
        File classes = folder.newFolder("classes");
        File extra = folder.newFolder("extra");
        List<File> watched = registerClassFileListener(classes, extra);

        assertTrue(watched.contains(classes.getCanonicalFile()));
        assertTrue(watched.contains(extra.getCanonicalFile()));
    }

    @Test
    public void watchesExtraClasspathKnownToClassLoaderOnce() throws Exception {
        File classes = folder.newFolder("classes");
        File extra = folder.newFolder("extra");
        List<File> watched = registerClassFileListener(classes, extra, extra);

        assertEquals(1, Collections.frequency(watched, extra.getCanonicalFile()));
        assertTrue(watched.contains(classes.getCanonicalFile()));
    }

    private List<File> registerClassFileListener(File classes, File extra, File... extraClassLoaderDirs) throws Exception {
        URL[] urls = new URL[1 + extraClassLoaderDirs.length];
        urls[0] = classes.toURI().toURL();
        for (int i = 0; i < extraClassLoaderDirs.length; i++) {
            urls[i + 1] = extraClassLoaderDirs[i].toURI().toURL();
        }
        final URLClassLoader classLoader = new URLClassLoader(urls, null);

        final PluginManager pluginManager = context.mock(PluginManager.class);
        final PluginRegistry pluginRegistry = context.mock(PluginRegistry.class);
        final PluginConfiguration pluginConfiguration = context.mock(PluginConfiguration.class);
        final Watcher watcher = context.mock(Watcher.class);
        final ClassFilePlugin plugin = new ClassFilePlugin();
        final List<File> watched = new ArrayList<>();

        context.checking(new Expectations() {{
            allowing(pluginManager).getPluginRegistry(); will(returnValue(pluginRegistry));
            allowing(pluginRegistry).getAppClassLoader(plugin); will(returnValue(classLoader));
            allowing(pluginManager).getPluginConfiguration(classLoader); will(returnValue(pluginConfiguration));
            allowing(pluginConfiguration).getExtraClasspath(); will(returnValue(new URL[]{extra.toURI().toURL()}));
            allowing(pluginConfiguration).getWatchResources(); will(returnValue(new URL[0]));
            allowing(pluginManager).getWatcher(); will(returnValue(watcher));
            allowing(watcher).addEventListener(with(same(classLoader)), with(any(URI.class)), with(any(WatchEventListener.class)));
            will(new CustomAction("collect watched directory") {
                @Override
                public Object invoke(Invocation invocation) throws Throwable {
                    watched.add(Paths.get((URI) invocation.getParameter(1)).toFile().getCanonicalFile());
                    return null;
                }
            });
        }});

        Method method = ClassFilePlugin.class.getMethod("onClassFile");
        PluginAnnotation<OnClassFileEvent> pluginAnnotation = new PluginAnnotation<>(ClassFilePlugin.class, plugin,
                method.getAnnotation(OnClassFileEvent.class), method);
        assertTrue(new WatchHandler<OnClassFileEvent>(pluginManager).initMethod(pluginAnnotation));
        context.assertIsSatisfied();
        return watched;
    }
}
