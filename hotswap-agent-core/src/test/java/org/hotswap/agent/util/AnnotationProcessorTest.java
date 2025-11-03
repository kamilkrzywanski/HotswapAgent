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
package org.hotswap.agent.util;

import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.annotation.Init;
import org.hotswap.agent.annotation.handler.*;
import org.hotswap.agent.config.PluginManager;
import org.hotswap.agent.testData.SimplePlugin;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

/**
 * Created by bubnik on 11.10.13.
 */
public class AnnotationProcessorTest {

    private PluginManager pluginManager;
    private HotswapTransformer hotswapTransformer;
    private InitHandler initHandler;
    private OnClassLoadedHandler onClassLoadedHandler;

    @Before
    public void setUp() {
        pluginManager = mock(PluginManager.class);
        hotswapTransformer = mock(HotswapTransformer.class);
        initHandler = mock(InitHandler.class);
        onClassLoadedHandler = mock(OnClassLoadedHandler.class);

        when(pluginManager.getHotswapTransformer()).thenReturn(hotswapTransformer);

        when(initHandler.initMethod(any(PluginAnnotation.class))).thenReturn(true);
        when(onClassLoadedHandler.initMethod(any(PluginAnnotation.class))).thenReturn(true);
    }

    @Test
    public void testProcess() throws Exception {
        AnnotationProcessor annotationProcessor = new AnnotationProcessor(pluginManager);

        annotationProcessor.addAnnotationHandler(Init.class, initHandler);
        annotationProcessor.addAnnotationHandler(OnClassLoadEvent.class, onClassLoadedHandler);

        annotationProcessor.processAnnotations(new SimplePlugin());

        verify(initHandler, times(2)).initMethod(any(PluginAnnotation.class));

        verify(onClassLoadedHandler, times(1)).initMethod(any(PluginAnnotation.class));
    }
}
