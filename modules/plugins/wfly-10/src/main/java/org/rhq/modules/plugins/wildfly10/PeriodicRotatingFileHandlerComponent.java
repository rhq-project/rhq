package org.rhq.modules.plugins.wildfly10;

import org.rhq.core.domain.configuration.Configuration;

public class PeriodicRotatingFileHandlerComponent extends BaseComponent {

        @Override
    public Configuration loadResourceConfiguration() throws Exception {
        Configuration configuration = super.loadResourceConfiguration();
        // if named-formatter is set, we don't want t show the default value returned for formatter.
        if (configuration.getSimpleValue("named-formatter") != null) {
            configuration.remove("formatter");
        }
        return  configuration;
    }

}
