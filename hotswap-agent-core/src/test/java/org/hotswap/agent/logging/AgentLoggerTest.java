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
package org.hotswap.agent.logging;

import org.hotswap.agent.testData.SimplePlugin;
import org.junit.Test;

import static org.mockito.Mockito.*;

/**
 * Created by bubnik on 12.10.13.
 */
public class AgentLoggerTest {

    @Test
    public void testDefaultLevel() throws Exception {
        AgentLoggerHandler handler = mock(AgentLoggerHandler.class);

        final Class<?> clazz = SimplePlugin.class;
        final String message = "Test";
        final Throwable error = new Throwable();

        AgentLogger.setHandler(handler);

        AgentLogger logger = AgentLogger.getLogger(clazz);

        logger.error(message);
        logger.info(message, error);
        logger.trace(message, error);

        verify(handler, times(1)).print(clazz, AgentLogger.Level.ERROR, message, null);
        verify(handler, times(1)).print(clazz, AgentLogger.Level.INFO, message, error);
        verify(handler, never()).print(clazz, AgentLogger.Level.TRACE, message, error);

        AgentLogger.setHandler(new AgentLoggerHandler());
    }
}
