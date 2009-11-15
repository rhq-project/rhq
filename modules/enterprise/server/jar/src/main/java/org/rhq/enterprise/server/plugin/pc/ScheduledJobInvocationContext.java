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
    private final ServerPluginContext pluginContext;

    public ScheduledJobInvocationContext(ScheduledJobDefinition jobDefinition, ServerPluginContext pluginContext) {
        this.jobDefinition = jobDefinition;
        this.pluginContext = pluginContext;
    }

    public ScheduledJobDefinition getJobDefinition() {
        return jobDefinition;
    }

    public ServerPluginContext getPluginContext() {
        return pluginContext;
    }
}
