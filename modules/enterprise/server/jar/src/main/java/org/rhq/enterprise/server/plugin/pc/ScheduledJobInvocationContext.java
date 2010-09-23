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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.rhq.enterprise.server.xmlschema.ScheduledJobDefinition;

/**
 * <p>
 * A scheduled job's invocation method can take a single argument of this type.
 * If this is the case, the scheduled job's method will be passed an object of this type enabling
 * the job being invoked to be given information about the invocation.
 * </p>
 * <p>
 * When a job is <strong>not</strong> marked for concurrent execution, it can store data in the context in the form of
 * key/value properties. These properties will be persisted across invocations of the job. If the job is marked to as
 * clustered, the job data will persist across server restarts as well. Lastly, if the job is marked to run
 * concurrently, the methods for accessing/updating properties will throw an exception.
 * </p>
 *  
 * @author John Mazzitelli
 */
public class ScheduledJobInvocationContext {
    private final ScheduledJobDefinition jobDefinition;
    private final ServerPluginContext serverPluginContext;
    private final ServerPluginComponent serverPluginComponent;

    public ScheduledJobInvocationContext(ScheduledJobDefinition jobDefinition, ServerPluginContext pluginContext,
        ServerPluginComponent serverPluginComponent) {
        this.jobDefinition = jobDefinition;
        this.serverPluginContext = pluginContext;
        this.serverPluginComponent = serverPluginComponent;
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

    /**
     * Adds a property to the context that is persisted across invocations of the job. The property is persisted across
     * server restarts <strong>only if</strong> the scheduled job is declared to run as clustered. Note that method
     * will throw an exception if the job is declared for concurrent execution.
     *
     * @param key The property name
     * @param value The property value
     * @throws UnsupportedOperationException if the job is marked for concurrent execution
     */
    public void put(String key, String value) {
        throw new IllegalArgumentException("This operation is only supported for stateful jobs.");
    }

    /**
     * Retrieves a property value from the context.
     *
     * @param key The property key
     * @return The property value or <code>null<code> if the key is not found
     * @throws UnsupportedOperationException if the job is marked for concurrent execution
     */
    public String get(String key) {
        throw new IllegalArgumentException("This operation is only supported for stateful jobs.");
    }

    /**
     * Removes the property value associated with the specified key
     *
     * @param key The property key
     * @return The value previously associated with the key or <code>null</code> if the key is present in the context
     * @throws UnsupportedOperationException if the job is marked for concurrent execution
     */
    public String remove(String key) {
        throw new IllegalArgumentException("This operation is only supported for stateful jobs.");
    }

    /**
     * Checks to see whether or not the property key is stored in the context.
     * @param key The property key
     * @return <code>true</code> if the key is found, <code>false</code> otherwise.
     * @throws UnsupportedOperationException if the job is marked for concurrent execution
     */
    public boolean containsKey(String key) {
        throw new IllegalArgumentException("This operation is only supported for stateful jobs.");
    }

    /**
     * Returns a <strong>read-only</strong> view of the properties stored in the context.
     *
     * @return A <strong>read-only</strong> view of the properties stored in the context.
     * @throws UnsupportedOperationException if the job is marked for concurrent execution
     */
    public Map<String, String> getJobData() {
        throw new IllegalArgumentException("This operation is only supported for stateful jobs.");
    }

}
