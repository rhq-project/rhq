/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.rhq.test.arquillian.spi;

import org.jboss.arquillian.test.spi.event.suite.TestEvent;

import org.rhq.core.pc.PluginContainer;

/**
 * Implementations of this service interface are run after all of the {@link PluginContainerOperation}s
 * are run. They are meant to "cure" the Plugin Container from the state the operations put it in.
 * <p>
 * For example, running the discovery on the plugin container in the full agent-mode will kick
 * off asynchronous child discoveries for each committed resource. This is not desirable in the
 * test environment because we don't want any asynchronous activity during test execution.
 * <p>
 * Therefore there can be a "remedy" for the {@code PluginContainer} which is going to wait until
 * all the async work has completed and {@code PluginContainer} is therefore in a predictable state. 
 *
 * @author Lukas Krejci
 */
public interface PluginContainerOperationRemedy {

    /**
     * Puts the {@link PluginContainer} in a stable state after some operation execution.
     * 
     * @param pc
     * @param event
     */
    void cure(PluginContainer pc, TestEvent event);
}
