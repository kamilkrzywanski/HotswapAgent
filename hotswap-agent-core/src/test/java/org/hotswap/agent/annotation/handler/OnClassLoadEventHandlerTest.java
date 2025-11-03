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
package org.hotswap.agent.annotation.handler;

import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.config.PluginManager;
import org.hotswap.agent.config.PluginRegistry;
import org.hotswap.agent.testData.SimplePlugin;
import org.hotswap.agent.util.HaClassFileTransformer;
import org.hotswap.agent.util.HotswapTransformer;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author Jiri Bubnik
 */
public class OnClassLoadEventHandlerTest {

    private PluginManager pluginManager;
    private PluginRegistry pluginRegistry;
    private HotswapTransformer hotswapTransformer;

    @Before
    public void setUp() {
        pluginManager = mock(PluginManager.class);
        pluginRegistry = mock(PluginRegistry.class);
        hotswapTransformer = mock(HotswapTransformer.class);

        when(pluginManager.getHotswapTransformer()).thenReturn(hotswapTransformer);
        when(pluginManager.getPluginRegistry()).thenReturn(pluginRegistry);

        when(pluginRegistry.getAppClassLoader(any())).thenReturn(getClass().getClassLoader());
    }

    @Test
    public void testInitMethod() throws Exception {
        OnClassLoadedHandler onClassLoadedHandler = new OnClassLoadedHandler(pluginManager);

        SimplePlugin simplePlugin = new SimplePlugin();
        Method method = SimplePlugin.class.getMethod("transform");
        PluginAnnotation<OnClassLoadEvent> pluginAnnotation = new PluginAnnotation<>(
                SimplePlugin.class,
                simplePlugin,
                method.getAnnotation(OnClassLoadEvent.class),
                method
        );

        boolean result = onClassLoadedHandler.initMethod(pluginAnnotation);

        assertTrue("Init successful", result);

        verify(hotswapTransformer, times(1))
                .registerTransformer(eq(getClass().getClassLoader()),
                        eq("org.hotswap.example.type"),
                        any(HaClassFileTransformer.class));
    }

    @Test
    public void testTransform() throws Exception {
    }
}
