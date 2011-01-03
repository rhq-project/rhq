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

    public Configuration getPluginConfiguration(int resourceId) throws RuntimeException {
        try {
            Configuration configuration = configurationManager.getPluginConfiguration(getSessionSubject(), resourceId);
            return SerialUtility.prepare(configuration, "PluginConfiguration");
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public ConfigurationDefinition getPluginConfigurationDefinition(int resourceTypeId) throws RuntimeException {
        try {
            ConfigurationDefinition definition = configurationManager.getPluginConfigurationDefinitionForResourceType(
                getSessionSubject(), resourceTypeId);
            return SerialUtility.prepare(definition, "PluginDefinition");
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public Configuration getResourceConfiguration(int resourceId) throws RuntimeException {
        try {
            Configuration configuration = configurationManager
                .getResourceConfiguration(getSessionSubject(), resourceId);
            return SerialUtility.prepare(configuration, "ResourceConfiguration");
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public ConfigurationDefinition getResourceConfigurationDefinition(int resourceTypeId) throws RuntimeException {
        try {
            ConfigurationDefinition definition = configurationManager
                .getResourceConfigurationDefinitionWithTemplatesForResourceType(getSessionSubject(), resourceTypeId);
            return SerialUtility.prepare(definition, "ResourceDefinition");
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public PageList<ResourceConfigurationUpdate> findResourceConfigurationUpdates(Integer resourceId, Long beginDate,
        Long endDate, boolean suppressOldest, PageControl pc) throws RuntimeException {
        try {
            PageList<ResourceConfigurationUpdate> result = configurationManager.findResourceConfigurationUpdates(
                getSessionSubject(), resourceId, beginDate, endDate, suppressOldest, pc);
            return SerialUtility.prepare(result, "ConfigurationService.findResourceConfigurationUpdates");
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public ResourceConfigurationUpdate updateResourceConfiguration(int resourceId, Configuration configuration)
        throws RuntimeException {
        try {
            ResourceConfigurationUpdate update = configurationManager.updateResourceConfiguration(getSessionSubject(),
                resourceId, configuration);
            return SerialUtility.prepare(update, "ConfigurationService.updateResourceConfiguration");
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public PluginConfigurationUpdate updatePluginConfiguration(int resourceId, Configuration configuration)
        throws RuntimeException {
        try {
            PluginConfigurationUpdate update = configurationManager.updatePluginConfiguration(getSessionSubject(),
                resourceId, configuration);
            return SerialUtility.prepare(update, "ConfigurationService.updatePluginConfiguration");
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public PageList<ResourceConfigurationUpdate> findResourceConfigurationUpdatesByCriteria(
        ResourceConfigurationUpdateCriteria criteria) throws RuntimeException {
        try {
            PageList<ResourceConfigurationUpdate> updates = configurationManager
                .findResourceConfigurationUpdatesByCriteria(getSessionSubject(), criteria);
            return SerialUtility.prepare(updates, "ConfigurationService.findResourceConfigurationUpdatesByCriteria");
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public PageList<PluginConfigurationUpdate> findPluginConfigurationUpdatesByCriteria(
        PluginConfigurationUpdateCriteria criteria) throws RuntimeException {
        try {
            PageList<PluginConfigurationUpdate> updates = configurationManager
                .findPluginConfigurationUpdatesByCriteria(getSessionSubject(), criteria);
            return SerialUtility.prepare(updates, "ConfigurationService.findPluginConfigurationUpdatesByCriteria");
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public PageList<GroupResourceConfigurationUpdate> findGroupResourceConfigurationUpdatesByCriteria(
        GroupResourceConfigurationUpdateCriteria criteria) throws RuntimeException {
        try {
            PageList<GroupResourceConfigurationUpdate> updates = configurationManager
                .findGroupResourceConfigurationUpdatesByCriteria(getSessionSubject(), criteria);
            return SerialUtility.prepare(updates,
                "ConfigurationService.findGroupResourceConfigurationUpdatesByCriteria");
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public PageList<GroupPluginConfigurationUpdate> findGroupPluginConfigurationUpdatesByCriteria(
        GroupPluginConfigurationUpdateCriteria criteria) throws RuntimeException {
        try {
            PageList<GroupPluginConfigurationUpdate> updates = configurationManager
                .findGroupPluginConfigurationUpdatesByCriteria(getSessionSubject(), criteria);
            return SerialUtility.prepare(updates, "ConfigurationService.findGroupPluginConfigurationUpdatesByCriteria");
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public List<DisambiguationReport<ResourceConfigurationComposite>> findResourceConfigurationsForGroup(int groupId)
        throws RuntimeException {
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
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public List<DisambiguationReport<ResourceConfigurationComposite>> findPluginConfigurationsForGroup(int groupId)
        throws RuntimeException {
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
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public List<DisambiguationReport<ResourceConfigurationComposite>> findPluginConfigurationsForGroupUpdate(
        int groupUpdateId) throws RuntimeException {
        try {
            Map<Integer, Configuration> configurations = this.configurationManager
                .getPluginConfigurationMapForGroupUpdate(groupUpdateId);
            List<ResourceConfigurationComposite> configurationComposites = convertToCompositesList(configurations);

            // Disambiguate - i.e. generate unambiguous Resource names for each of the Resource id's.
            List<DisambiguationReport<ResourceConfigurationComposite>> disambiguatedConfigurationComposites = resourceManager
                .disambiguate(configurationComposites, RESOURCE_CONFIGURATION_COMPOSITE_RESOURCE_ID_EXTRACTOR,
                    DefaultDisambiguationUpdateStrategies.getDefault());

            return SerialUtility.prepare(disambiguatedConfigurationComposites,
                "ConfigurationService.findPluginConfigurationsForGroupUpdate");
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public void updateResourceConfigurationsForGroup(int groupId,
        List<ResourceConfigurationComposite> resourceConfigurations) throws RuntimeException {
        try {
            Map<Integer, Configuration> configurations = convertToMap(resourceConfigurations);
            this.configurationManager.scheduleGroupResourceConfigurationUpdate(getSessionSubject(), groupId,
                configurations);
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public void updatePluginConfigurationsForGroup(int groupId,
        List<ResourceConfigurationComposite> pluginConfigurations) throws RuntimeException {
        try {
            Map<Integer, Configuration> configurations = convertToMap(pluginConfigurations);
            this.configurationManager.scheduleGroupPluginConfigurationUpdate(getSessionSubject(), groupId,
                configurations);
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public void deleteGroupPluginConfigurationUpdate(Integer groupId, Integer[] groupPluginConfigUpdateIds)
        throws RuntimeException {
        try {
            this.configurationManager.deleteGroupPluginConfigurationUpdates(getSessionSubject(), groupId,
                groupPluginConfigUpdateIds);
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public void deleteGroupResourceConfigurationUpdate(Integer groupId, Integer[] groupResourceConfigUpdateIds)
        throws RuntimeException {
        try {
            this.configurationManager.deleteGroupResourceConfigurationUpdates(getSessionSubject(), groupId,
                groupResourceConfigUpdateIds);
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    private List<ResourceConfigurationComposite> convertToCompositesList(Map<Integer, Configuration> configurations)
        throws RuntimeException {
        try {
            List<ResourceConfigurationComposite> configurationComposites = new ArrayList<ResourceConfigurationComposite>(
                configurations.size());
            for (Integer resourceId : configurations.keySet()) {
                Configuration configuration = configurations.get(resourceId);
                ResourceConfigurationComposite configurationComposite = new ResourceConfigurationComposite(resourceId,
                    configuration);
                configurationComposites.add(configurationComposite);
            }
            return configurationComposites;
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    private Map<Integer, Configuration> convertToMap(List<ResourceConfigurationComposite> resourceConfigurations)
        throws RuntimeException {
        try {
            Map<Integer, Configuration> configurations = new HashMap<Integer, Configuration>(resourceConfigurations
                .size());
            for (ResourceConfigurationComposite resourceConfiguration : resourceConfigurations) {
                configurations.put(resourceConfiguration.getResourceId(), resourceConfiguration.getConfiguration());
            }
            return configurations;
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }
}
