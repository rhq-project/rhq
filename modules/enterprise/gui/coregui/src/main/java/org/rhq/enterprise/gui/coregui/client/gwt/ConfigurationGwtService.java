package org.rhq.enterprise.gui.coregui.client.gwt;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("ConfigurationGWTService")
public interface ConfigurationGWTService extends RemoteService {

    Configuration getPluginConfiguration(int resourceId);

    ConfigurationDefinition getPluginConfigurationDefinition(int resourceTypeId);

    Configuration getResourceConfiguration(int resourceId);

    ConfigurationDefinition getResourceConfigurationDefinition(int resourceTypeId);

    RawConfiguration dummy(RawConfiguration config);

}
