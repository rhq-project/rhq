package org.rhq.enterprise.gui.coregui.server.gwt;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.enterprise.gui.coregui.client.gwt.ConfigurationGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class ConfigurationGWTServiceImpl extends AbstractGWTServiceImpl implements ConfigurationGWTService {


    public Configuration getPluginConfiguration(int resourceId) {

        ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();

        Configuration configuration = configurationManager.getPluginConfiguration(getSessionSubject(), resourceId);

        return SerialUtility.prepare(configuration, "PluginConfiguration");
    }

    public ConfigurationDefinition getPluginConfigurationDefinition(int resourceTypeId) {
        ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();

        ConfigurationDefinition definition = configurationManager.getPluginConfigurationDefinitionForResourceType(getSessionSubject(), resourceTypeId);
        return SerialUtility.prepare(definition, "PluginDefinition");
    }


    public Configuration getResourceConfiguration(int resourceId) {

        ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();

        Configuration configuration = configurationManager.getResourceConfiguration(getSessionSubject(),  resourceId);
        return SerialUtility.prepare(configuration, "ResourceConfiguration");
    }

    public ConfigurationDefinition getResourceConfigurationDefinition(int resourceTypeId) {
        ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();

        ConfigurationDefinition definition = configurationManager.getResourceConfigurationDefinitionWithTemplatesForResourceType(getSessionSubject(), resourceTypeId);
        return SerialUtility.prepare(definition, "ResourceDefinition");
    }

    public RawConfiguration dummy(RawConfiguration config) {
        System.out.println(config.getPath());
        return new RawConfiguration();
        // Dummy method for gwt compiler
    }
}
