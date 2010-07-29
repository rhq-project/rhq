package org.rhq.enterprise.gui.coregui.server.gwt;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PluginConfigurationUpdate;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.criteria.ResourceConfigurationUpdateCriteria;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.coregui.client.gwt.ConfigurationGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.configuration.ConfigurationUpdateStillInProgressException;
import org.rhq.enterprise.server.util.LookupUtil;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class ConfigurationGWTServiceImpl extends AbstractGWTServiceImpl implements ConfigurationGWTService {


    private ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();

    public Configuration getPluginConfiguration(int resourceId) {


        Configuration configuration = configurationManager.getPluginConfiguration(getSessionSubject(), resourceId);

        return SerialUtility.prepare(configuration, "PluginConfiguration");
    }

    public ConfigurationDefinition getPluginConfigurationDefinition(int resourceTypeId) {

        ConfigurationDefinition definition = configurationManager.getPluginConfigurationDefinitionForResourceType(getSessionSubject(), resourceTypeId);
        return SerialUtility.prepare(definition, "PluginDefinition");
    }


    public Configuration getResourceConfiguration(int resourceId) {

        Configuration configuration = configurationManager.getResourceConfiguration(getSessionSubject(), resourceId);
        return SerialUtility.prepare(configuration, "ResourceConfiguration");
    }

    public ConfigurationDefinition getResourceConfigurationDefinition(int resourceTypeId) {

        ConfigurationDefinition definition = configurationManager.getResourceConfigurationDefinitionWithTemplatesForResourceType(getSessionSubject(), resourceTypeId);
        return SerialUtility.prepare(definition, "ResourceDefinition");
    }

    public PageList<ResourceConfigurationUpdate> findResourceConfigurationUpdates(
            Integer resourceId, Long beginDate, Long endDate, boolean suppressOldest, PageControl pc) {

        PageList<ResourceConfigurationUpdate> result =
                configurationManager.findResourceConfigurationUpdates(
                        getSessionSubject(), resourceId, beginDate, endDate, suppressOldest, pc);

        return SerialUtility.prepare(result, "ConfigurationService.findResourceConfigurationUpdates");
    }


    public ResourceConfigurationUpdate updateResourceConfiguration(int resourceId, Configuration configuration) {
        ResourceConfigurationUpdate update =
                configurationManager.updateResourceConfiguration(getSessionSubject(), resourceId, configuration);

        return SerialUtility.prepare(update, "ConfigurationService.updateResourceConfiguration");
    }

    public PluginConfigurationUpdate updatePluginConfiguration(int resourceId, Configuration configuration) {
        PluginConfigurationUpdate update =
                configurationManager.updatePluginConfiguration(getSessionSubject(), resourceId, configuration);

        return SerialUtility.prepare(update, "ConfigurationService.updatePluginConfiguration");
    }

    public PageList<ResourceConfigurationUpdate> findResourceConfigurationUpdatesByCriteria(ResourceConfigurationUpdateCriteria criteria) {
        PageList<ResourceConfigurationUpdate> updates =
                configurationManager.findResourceConfigurationUpdatesByCriteria(
                        getSessionSubject(), criteria
                );

        return SerialUtility.prepare(updates, "ConfigurationService.findResourceConfigurationUpdatesByCriteria");
    }

    public RawConfiguration dummy(RawConfiguration config) {
        System.out.println(config.getPath());
        return new RawConfiguration();
        // Dummy method for gwt compiler
    }
}
