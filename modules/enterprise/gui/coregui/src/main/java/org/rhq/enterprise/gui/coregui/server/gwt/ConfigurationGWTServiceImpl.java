package org.rhq.enterprise.gui.coregui.server.gwt;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PluginConfigurationUpdate;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.criteria.ResourceConfigurationUpdateCriteria;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.coregui.client.gwt.ConfigurationGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ConfigurationGWTServiceImpl extends AbstractGWTServiceImpl implements ConfigurationGWTService {

    private static final long serialVersionUID = 1L;

    private ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();

    public Configuration getPluginConfiguration(int resourceId) {
        try {
            Configuration configuration = configurationManager.getPluginConfiguration(getSessionSubject(), resourceId);
            return SerialUtility.prepare(configuration, "PluginConfiguration");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public ConfigurationDefinition getPluginConfigurationDefinition(int resourceTypeId) {
        try {
            ConfigurationDefinition definition = configurationManager.getPluginConfigurationDefinitionForResourceType(
                getSessionSubject(), resourceTypeId);
            return SerialUtility.prepare(definition, "PluginDefinition");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public Configuration getResourceConfiguration(int resourceId) {
        try {
            Configuration configuration = configurationManager
                .getResourceConfiguration(getSessionSubject(), resourceId);
            return SerialUtility.prepare(configuration, "ResourceConfiguration");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public ConfigurationDefinition getResourceConfigurationDefinition(int resourceTypeId) {
        try {
            ConfigurationDefinition definition = configurationManager
                .getResourceConfigurationDefinitionWithTemplatesForResourceType(getSessionSubject(), resourceTypeId);
            return SerialUtility.prepare(definition, "ResourceDefinition");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public PageList<ResourceConfigurationUpdate> findResourceConfigurationUpdates(Integer resourceId, Long beginDate,
        Long endDate, boolean suppressOldest, PageControl pc) {
        try {
            PageList<ResourceConfigurationUpdate> result = configurationManager.findResourceConfigurationUpdates(
                getSessionSubject(), resourceId, beginDate, endDate, suppressOldest, pc);
            return SerialUtility.prepare(result, "ConfigurationService.findResourceConfigurationUpdates");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public ResourceConfigurationUpdate updateResourceConfiguration(int resourceId, Configuration configuration) {
        try {
            ResourceConfigurationUpdate update = configurationManager.updateResourceConfiguration(getSessionSubject(),
                resourceId, configuration);
            return SerialUtility.prepare(update, "ConfigurationService.updateResourceConfiguration");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public PluginConfigurationUpdate updatePluginConfiguration(int resourceId, Configuration configuration) {
        try {
            PluginConfigurationUpdate update = configurationManager.updatePluginConfiguration(getSessionSubject(),
                resourceId, configuration);
            return SerialUtility.prepare(update, "ConfigurationService.updatePluginConfiguration");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public PageList<ResourceConfigurationUpdate> findResourceConfigurationUpdatesByCriteria(
        ResourceConfigurationUpdateCriteria criteria) {
        try {
            PageList<ResourceConfigurationUpdate> updates = configurationManager
                .findResourceConfigurationUpdatesByCriteria(getSessionSubject(), criteria);
            return SerialUtility.prepare(updates, "ConfigurationService.findResourceConfigurationUpdatesByCriteria");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public RawConfiguration dummy(RawConfiguration config) {
        com.allen_sauer.gwt.log.client.Log.info(config.getPath());
        return new RawConfiguration();
        // Dummy method for gwt compiler
    }
}
