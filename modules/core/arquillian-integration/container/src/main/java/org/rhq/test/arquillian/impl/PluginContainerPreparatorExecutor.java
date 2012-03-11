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
import org.rhq.test.arquillian.spi.PluginContainerPreparator;
import org.rhq.test.arquillian.spi.events.PluginContainerDiscovered;
import org.rhq.test.arquillian.spi.events.PluginContainerPrepared;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class PluginContainerPreparatorExecutor {

    @Inject
    private Instance<PluginContainer> pluginContainer;
    
    @Inject
    private Instance<ServiceLoader> serviceLoader;
    
    @Inject
    private Event<PluginContainerPrepared> pluginContainerPrepared;
    
    public void handle(@Observes PluginContainerDiscovered event) {
        for(PluginContainerPreparator prep : serviceLoader.get().all(PluginContainerPreparator.class)) {
            prep.prepare(pluginContainer.get(), event);
        }
        
        pluginContainerPrepared.fire(new PluginContainerPrepared(event.getTestInstance(), event.getTestMethod()));
    }
}
