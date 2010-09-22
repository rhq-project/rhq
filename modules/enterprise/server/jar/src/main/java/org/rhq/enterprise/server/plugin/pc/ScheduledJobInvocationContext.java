/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.enterprise.server.plugin.pc;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.rhq.enterprise.server.xmlschema.ScheduledJobDefinition;

/**
 * A scheduled job's invocation method can take a single argument of this type.
 * If this is the case, the scheduled job's method will be passed an object of this type enabling
 * the job being invoked to be given information about the invocation.
 *  
 * @author John Mazzitelli
 */
public class ScheduledJobInvocationContext {
    private final ScheduledJobDefinition jobDefinition;
    private final ServerPluginContext serverPluginContext;
    private final ServerPluginComponent serverPluginComponent;
    private Map<String, Serializable> properties;

    public ScheduledJobInvocationContext(ScheduledJobDefinition jobDefinition, ServerPluginContext pluginContext,
        ServerPluginComponent serverPluginComponent, Map<String, Serializable> properties) {
        this.jobDefinition = jobDefinition;
        this.serverPluginContext = pluginContext;
        this.serverPluginComponent = serverPluginComponent;
        this.properties = properties;
    }

    /**
     * The definition of the triggered job that owns this context object.
     *  
     * @return the triggered job's definition
     */
    public ScheduledJobDefinition getJobDefinition() {
        return this.jobDefinition;
    }

    /**
     * General information for the job's plugin.
     * 
     * @return the triggered job's plugin context
     */
    public ServerPluginContext getServerPluginContext() {
        return this.serverPluginContext;
    }

    /**
     * If the job's plugin has a plugin component defined, this will return the instance of that component.
     * This is helpful for stateless jobs to obtain information from the plugin component, which is stateful.
     * 
     * @return the plugin component instance from the triggered job's plugin, or <code>null</code> if
     *         there is no plugin component for the plugin
     */
    public ServerPluginComponent getServerPluginComponent() {
        return serverPluginComponent;
    }

    public Map<String, Serializable> getProperties() {
        return properties;
    }
}
