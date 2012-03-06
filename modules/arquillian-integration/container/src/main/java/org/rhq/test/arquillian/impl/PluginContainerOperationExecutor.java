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

package org.rhq.test.arquillian.impl;

import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.ServiceLoader;

import org.rhq.core.pc.PluginContainer;
import org.rhq.test.arquillian.spi.PluginContainerOperation;
import org.rhq.test.arquillian.spi.events.PluginContainerOperationsExecuted;
import org.rhq.test.arquillian.spi.events.PluginContainerPrepared;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class PluginContainerOperationExecutor {

    @Inject
    private Instance<ServiceLoader> serviceLoader;
    
    @Inject
    private Instance<PluginContainer> pluginContainer;
    
    @Inject
    private Event<PluginContainerOperationsExecuted> pluginContainerOperationsExecuted;
    
    public void run(@Observes PluginContainerPrepared event) {
        for(PluginContainerOperation op : serviceLoader.get().all(PluginContainerOperation.class)) {
            op.execute(pluginContainer.get(), event);
        }
        pluginContainerOperationsExecuted.fire(new PluginContainerOperationsExecuted(event.getTestInstance(), event.getTestMethod()));
    }
}
