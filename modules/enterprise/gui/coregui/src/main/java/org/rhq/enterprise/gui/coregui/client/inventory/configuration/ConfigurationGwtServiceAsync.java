package org.rhq.enterprise.gui.coregui.client.inventory.configuration;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface ConfigurationGwtServiceAsync {


    void getResourceConfiguration(int resourceId, AsyncCallback<Configuration> callback);

    void getResourceConfigurationDefinition(int resourceTypeId, AsyncCallback<ConfigurationDefinition> callback);

}
