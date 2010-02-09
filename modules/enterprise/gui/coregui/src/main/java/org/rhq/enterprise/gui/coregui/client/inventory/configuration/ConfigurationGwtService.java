package org.rhq.enterprise.gui.coregui.client.inventory.configuration;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.google.gwt.core.client.GWT;

@RemoteServiceRelativePath("ConfigurationGwtService")
public interface ConfigurationGwtService extends RemoteService {

    Configuration getPluginConfiguration(int resourceId);

    ConfigurationDefinition getPluginConfigurationDefinition(int resourceTypeId);

    Configuration getResourceConfiguration(int resourceId);

    ConfigurationDefinition getResourceConfigurationDefinition(int resourceTypeId);

    void dummy(RawConfiguration config);

    /**
     * Utility/Convenience class.
     * Use ConfigurationGwtService.App.getInstance() to access static instance of ConfigurationGwtServiceAsync
     */
    public static class App {
        private static final ConfigurationGwtServiceAsync ourInstance = (ConfigurationGwtServiceAsync) GWT.create(ConfigurationGwtService.class);

        public static ConfigurationGwtServiceAsync getInstance() {
            return ourInstance;
        }
    }
}
