package org.rhq.enterprise.gui.coregui.server.gwt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PluginConfigurationUpdate;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.composite.ResourceConfigurationComposite;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.group.GroupPluginConfigurationUpdate;
import org.rhq.core.domain.configuration.group.GroupResourceConfigurationUpdate;
import org.rhq.core.domain.criteria.GroupPluginConfigurationUpdateCriteria;
import org.rhq.core.domain.criteria.GroupResourceConfigurationUpdateCriteria;
import org.rhq.core.domain.criteria.PluginConfigurationUpdateCriteria;
import org.rhq.core.domain.criteria.ResourceConfigurationUpdateCriteria;
import org.rhq.core.domain.resource.composite.DisambiguationReport;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.IntExtractor;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.coregui.client.gwt.ConfigurationGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.disambiguation.DefaultDisambiguationUpdateStrategies;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * 
 */
public class ConfigurationGWTServiceImpl extends AbstractGWTServiceImpl implements ConfigurationGWTService {

    private static final long serialVersionUID = 1L;

    private static final IntExtractor<ResourceConfigurationComposite> RESOURCE_CONFIGURATION_COMPOSITE_RESOURCE_ID_EXTRACTOR = new IntExtractor<ResourceConfigurationComposite>() {
        public int extract(ResourceConfigurationComposite configurationComposite) {
            return configurationComposite.getResourceId();
        }
    };

    private ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();
    private ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
    private ResourceGroupManagerLocal groupManager = LookupUtil.getResourceGroupManager();

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

    public PageList<PluginConfigurationUpdate> findPluginConfigurationUpdatesByCriteria(
        PluginConfigurationUpdateCriteria criteria) {
        try {
            PageList<PluginConfigurationUpdate> updates = configurationManager
                .findPluginConfigurationUpdatesByCriteria(getSessionSubject(), criteria);
            return SerialUtility.prepare(updates, "ConfigurationService.findPluginConfigurationUpdatesByCriteria");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public PageList<GroupResourceConfigurationUpdate> findGroupResourceConfigurationUpdatesByCriteria(
        GroupResourceConfigurationUpdateCriteria criteria) {
        try {
            PageList<GroupResourceConfigurationUpdate> updates = configurationManager
                .findGroupResourceConfigurationUpdatesByCriteria(getSessionSubject(), criteria);
            return SerialUtility.prepare(updates,
                "ConfigurationService.findGroupResourceConfigurationUpdatesByCriteria");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public PageList<GroupPluginConfigurationUpdate> findGroupPluginConfigurationUpdatesByCriteria(
        GroupPluginConfigurationUpdateCriteria criteria) {
        try {
            PageList<GroupPluginConfigurationUpdate> updates = configurationManager
                .findGroupPluginConfigurationUpdatesByCriteria(getSessionSubject(), criteria);
            return SerialUtility.prepare(updates, "ConfigurationService.findGroupPluginConfigurationUpdatesByCriteria");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public List<DisambiguationReport<ResourceConfigurationComposite>> findResourceConfigurationsForGroup(int groupId) {
        try {
            ResourceGroup group = this.groupManager.getResourceGroup(getSessionSubject(), groupId);
            Map<Integer, Configuration> configurations = this.configurationManager
                .getResourceConfigurationMapForCompatibleGroup(group);
            List<ResourceConfigurationComposite> configurationComposites = convertToCompositesList(configurations);

            // Disambiguate - i.e. generate unambiguous Resource names for each of the Resource id's.
            List<DisambiguationReport<ResourceConfigurationComposite>> disambiguatedConfigurationComposites = resourceManager
                .disambiguate(configurationComposites, RESOURCE_CONFIGURATION_COMPOSITE_RESOURCE_ID_EXTRACTOR,
                    DefaultDisambiguationUpdateStrategies.getDefault());

            return SerialUtility.prepare(disambiguatedConfigurationComposites,
                "ConfigurationService.findResourceConfigurationsForGroup");
        } catch (RuntimeException e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public List<DisambiguationReport<ResourceConfigurationComposite>> findPluginConfigurationsForGroup(int groupId) {
        try {
            Map<Integer, Configuration> configurations = this.configurationManager
                .getPluginConfigurationsForCompatibleGroup(getSessionSubject(), groupId);
            List<ResourceConfigurationComposite> configurationComposites = convertToCompositesList(configurations);

            // Disambiguate - i.e. generate unambiguous Resource names for each of the Resource id's.
            List<DisambiguationReport<ResourceConfigurationComposite>> disambiguatedConfigurationComposites = resourceManager
                .disambiguate(configurationComposites, RESOURCE_CONFIGURATION_COMPOSITE_RESOURCE_ID_EXTRACTOR,
                    DefaultDisambiguationUpdateStrategies.getDefault());

            return SerialUtility.prepare(disambiguatedConfigurationComposites,
                "ConfigurationService.findPluginConfigurationsForGroup");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public void updateResourceConfigurationsForGroup(int groupId,
        List<ResourceConfigurationComposite> resourceConfigurations) {
        try {
            Map<Integer, Configuration> configurations = convertToMap(resourceConfigurations);
            this.configurationManager.scheduleGroupResourceConfigurationUpdate(getSessionSubject(), groupId,
                configurations);
        } catch (RuntimeException e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public void updatePluginConfigurationsForGroup(int groupId,
        List<ResourceConfigurationComposite> pluginConfigurations) {
        try {
            Map<Integer, Configuration> configurations = convertToMap(pluginConfigurations);
            this.configurationManager.scheduleGroupPluginConfigurationUpdate(getSessionSubject(), groupId,
                configurations);
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    /*
        // Dummy method for gwt compiler
        public RawConfiguration dummy(RawConfiguration config) {
            Log.info(config.getPath());
            return new RawConfiguration();
        }
    */

    private List<ResourceConfigurationComposite> convertToCompositesList(Map<Integer, Configuration> configurations) {
        List<ResourceConfigurationComposite> configurationComposites = new ArrayList<ResourceConfigurationComposite>(
            configurations.size());
        for (Integer resourceId : configurations.keySet()) {
            Configuration configuration = configurations.get(resourceId);
            ResourceConfigurationComposite configurationComposite = new ResourceConfigurationComposite(resourceId,
                configuration);
            configurationComposites.add(configurationComposite);
        }
        return configurationComposites;
    }

    private Map<Integer, Configuration> convertToMap(List<ResourceConfigurationComposite> resourceConfigurations) {
        Map<Integer, Configuration> configurations = new HashMap<Integer, Configuration>(resourceConfigurations.size());
        for (ResourceConfigurationComposite resourceConfiguration : resourceConfigurations) {
            configurations.put(resourceConfiguration.getResourceId(), resourceConfiguration.getConfiguration());
        }
        return configurations;
    }
}
