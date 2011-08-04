package org.rhq.enterprise.server.plugin.pc;

import java.util.Collections;
import java.util.Map;

import org.rhq.enterprise.server.xmlschema.ScheduledJobDefinition;

public class StatefulScheduledJobInvocationContext extends ScheduledJobInvocationContext {
    private Map<String, String> jobData;

    public StatefulScheduledJobInvocationContext(ScheduledJobDefinition jobDefinition,
        ServerPluginContext pluginContext, ServerPluginComponent serverPluginComponent, Map<String, String> jobData) {
        super(jobDefinition, pluginContext, serverPluginComponent);
        this.jobData = jobData;
    }

    /**
     * Adds a property to the context that is persisted across invocations of the job.
     *
     * @param key The property name
     * @param value The property value
     */
    public void put(String key, String value) {
        jobData.put(key, value);
    }

    /**
     * Retrieves a property value from the context.
     *
     * @param key The property key
     * @return The property value or <code>null<code> if the key is not found
     */
    public String get(String key) {
        return jobData.get(key);
    }

    /**
     * Removes the property value associated with the specified key
     *
     * @param key The property key
     * @return The value previously associated with the key or <code>null</code> if the key is present in the context
     */
    public String remove(String key) {
        return jobData.remove(key);
    }

    /**
     * Checks to see whether or not the property key is stored in the context.
     * @param key The property key
     * @return <code>true</code> if the key is found, <code>false</code> otherwise.
     */
    public boolean containsKey(String key) {
        return jobData.containsKey(key);
    }

    /**
     * Returns a <strong>read-only</strong> view of the properties stored in the context.
     *
     * @return A <strong>read-only</strong> view of the properties stored in the context.
     */
    public Map<String, String> getJobData() {
        return Collections.unmodifiableMap(jobData);
    }

}
