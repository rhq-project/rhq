package org.rhq.core.domain.plugin;

import java.io.Serializable;

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.util.StringUtils;

/**
 * Provides metadata that describes control operations that are defined in a server plugin descriptor.
 * This is a domain object so it can be used by remote clients that do not have access to the
 * server-side only ControlDefinition object (such as GWT clients).
 * 
 * @author John Mazzitelli
 */
public class ServerPluginControlDefinition implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String displayName;
    private String description;
    private ConfigurationDefinition parameters;
    private ConfigurationDefinition results;

    public ServerPluginControlDefinition(String name, String displayName, String description,
        ConfigurationDefinition parameters, ConfigurationDefinition results) {

        if (name == null) {
            throw new NullPointerException("name == null");
        }

        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.parameters = parameters;
        this.results = results;
    }

    protected ServerPluginControlDefinition() {
        // needed for GWT
    }

    public String getName() {
        return this.name;
    }

    public String getDisplayName() {
        String retStr = this.displayName;
        if (retStr == null) {
            retStr = StringUtils.deCamelCase(this.name);
        }
        return retStr;
    }

    public String getDescription() {
        return this.description;
    }

    public ConfigurationDefinition getParameters() {
        return this.parameters;
    }

    public ConfigurationDefinition getResults() {
        return this.results;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ControlDefinition name=[").append(this.name).append("]");
        builder.append(", description=[").append(this.description).append("]");
        return builder.toString();
    }
}
