package org.rhq.enterprise.server.xmlschema;

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.util.StringUtils;

/**
 * Provides metadata that describes control operations that are defined in a server plugin descriptor.
 * 
 * @author John Mazzitelli
 */
public class ControlDefinition {
    private final String name;
    private final String displayName;
    private final String description;
    private final ConfigurationDefinition parameters;
    private final ConfigurationDefinition results;

    public ControlDefinition(String name, String displayName, String description, ConfigurationDefinition parameters,
        ConfigurationDefinition results) {

        if (name == null) {
            throw new NullPointerException("name == null");
        }

        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.parameters = parameters;
        this.results = results;
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
