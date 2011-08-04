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

import java.util.Map;

import org.rhq.enterprise.server.xmlschema.ScheduledJobDefinition;

/**
 * The actual quartz job that the plugin container will submit when it needs to invoke
 * a concurrent scheduled job on behalf of a plugin. This is a normal non-stateful "job"
 * which tells quartz that it can run multiple jobs concurrently.
 * 
 * Note that server plugin developers do not extend this class. Instead, developers
 * have their plugin components or their job class POJOs implement no-arg methods,
 * or methods that take {@link ScheduledJobInvocationContext} as a single argument.
 *  
 * @author John Mazzitelli
 */
public class ConcurrentJobWrapper extends AbstractJobWrapper {
    @Override
    protected ScheduledJobInvocationContext createContext(ScheduledJobDefinition jobDefinition,
        ServerPluginContext pluginContext, ServerPluginComponent serverPluginComponent, Map<String, String> jobData) {
        return new ScheduledJobInvocationContext(jobDefinition, pluginContext, serverPluginComponent);
    }
}
