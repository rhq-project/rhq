package org.rhq.enterprise.server.resource.metadata;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.configuration.metadata.ConfigurationDefinitionUpdateReport;
import org.rhq.enterprise.server.configuration.metadata.ConfigurationMetadataManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Date;
import java.util.List;

@Stateless
public class PluginConfigurationMetadataManagerBean implements PluginConfigurationMetadataManagerLocal {

    private static Log log = LogFactory.getLog(PluginConfigurationMetadataManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityMgr;

    @EJB
    private ConfigurationMetadataManagerLocal configurationMetadataMgr;

    @EJB
    private SubjectManagerLocal subjectMgr;

    @EJB
    private ResourceManagerLocal resourceMgr;

    @Override
    public void updatePluginConfigurationDefinition(ResourceType existingType, ResourceType newType) {
        if (log.isDebugEnabled()) {
            log.debug("Updating plugin configuration definition for " + existingType);
        }
        ConfigurationDefinition existingConfigurationDefinition = existingType.getPluginConfigurationDefinition();
        if (newType.getPluginConfigurationDefinition() != null) {
            // all new
            if (existingConfigurationDefinition == null) {
                if (log.isDebugEnabled()) {
                    log.debug(existingType + " currently does not have a plugin configuration definition. Adding " +
                            "new plugin configuration.");
                }
                entityMgr.persist(newType.getPluginConfigurationDefinition());
                existingType.setPluginConfigurationDefinition(newType.getPluginConfigurationDefinition());
            } else // update the configuration
            {
                if (log.isDebugEnabled()) {
                    log.debug("Updating plugin configuration definition for " + existingType);
                }
                ConfigurationDefinitionUpdateReport updateReport = configurationMetadataMgr
                    .updateConfigurationDefinition(newType.getPluginConfigurationDefinition(),
                        existingConfigurationDefinition);

                if (updateReport.getNewPropertyDefinitions().size() > 0 ||
                    updateReport.getUpdatedPropertyDefinitions().size() > 0) {
                    Subject overlord = subjectMgr.getOverlord();
                    ResourceCriteria criteria = new ResourceCriteria();
                    criteria.addFilterResourceTypeId(existingType.getId());
                    List<Resource> resources = resourceMgr.findResourcesByCriteria(overlord, criteria);

                    for (Resource resource : resources) {
                        updateResourcePluginConfiguration(resource, updateReport);
                    }
                }
            }
        } else {
            // resourceType.getPlu... is null -> remove the existing config
            if (existingConfigurationDefinition != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Removing plugin configuration definition for " + existingType);
                }
                existingType.setPluginConfigurationDefinition(null);
                entityMgr.remove(existingConfigurationDefinition);
            }
        }
    }

    private void updateResourcePluginConfiguration(Resource resource, ConfigurationDefinitionUpdateReport updateReport) {
        Configuration pluginConfiguration = resource.getPluginConfiguration();
        boolean modified = false;
        int numberOfProperties = pluginConfiguration.getProperties().size();
        ConfigurationTemplate template = updateReport.getConfigurationDefinition().getDefaultTemplate();
        Configuration templateConfiguration = template.getConfiguration();

        for (PropertyDefinition propertyDef : updateReport.getNewPropertyDefinitions()) {
            if (propertyDef.isRequired()) {
                Property templateProperty = templateConfiguration.get(propertyDef.getName());
                pluginConfiguration.put(templateProperty.deepCopy(false));
                modified = true;
            }
        }

        for (PropertyDefinition propertyDef : updateReport.getUpdatedPropertyDefinitions()) {
            if (propertyDef.isRequired()) {
                String propertyValue = pluginConfiguration.getSimpleValue(propertyDef.getName(), null);
                if (propertyValue == null) {
                    Property templateProperty = templateConfiguration.get(propertyDef.getName());
                    pluginConfiguration.put(templateProperty.deepCopy(false));
                    modified = true;
                }
            }
        }

        if (modified) {
            resource.setMtime(new Date().getTime());
        }
    }
}
