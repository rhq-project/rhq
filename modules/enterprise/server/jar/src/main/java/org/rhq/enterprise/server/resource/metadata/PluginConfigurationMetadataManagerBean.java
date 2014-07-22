package org.rhq.enterprise.server.resource.metadata;

import java.util.Arrays;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.configuration.metadata.ConfigurationDefinitionUpdateReport;
import org.rhq.enterprise.server.configuration.metadata.ConfigurationMetadataManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.BatchIterator;

@Stateless
public class PluginConfigurationMetadataManagerBean implements PluginConfigurationMetadataManagerLocal {

    private static Log log = LogFactory.getLog(PluginConfigurationMetadataManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityMgr;

    @EJB
    private ConfigurationMetadataManagerLocal configurationMetadataMgr;

    @EJB
    private PluginConfigurationMetadataManagerLocal pluginConfigurationMetadataMgr;

    @EJB
    private SubjectManagerLocal subjectMgr;

    @EJB
    private ResourceManagerLocal resourceMgr;

    @Override
    public void updatePluginConfigurationDefinition(ResourceType existingType, ResourceType newType) {
        if (log.isDebugEnabled()) {
            log.debug("Updating plugin configuration definition for " + existingType);
        }

        existingType = entityMgr.find(ResourceType.class, existingType.getId());
        ConfigurationDefinition existingConfigurationDefinition = existingType.getPluginConfigurationDefinition();
        if (newType.getPluginConfigurationDefinition() != null) {
            // all new
            if (existingConfigurationDefinition == null) {
                if (log.isDebugEnabled()) {
                    log.debug(existingType + " currently does not have a plugin configuration definition. Adding "
                        + "new plugin configuration.");
                }
                entityMgr.persist(newType.getPluginConfigurationDefinition());
                existingType.setPluginConfigurationDefinition(newType.getPluginConfigurationDefinition());

            } else { // update the configuration
                if (log.isDebugEnabled()) {
                    log.debug("Updating plugin configuration definition for " + existingType);
                }
                ConfigurationDefinitionUpdateReport updateReport = configurationMetadataMgr
                    .updateConfigurationDefinition(newType.getPluginConfigurationDefinition(),
                        existingConfigurationDefinition);

                if (updateReport.getNewPropertyDefinitions().size() > 0
                    || updateReport.getUpdatedPropertyDefinitions().size() > 0) {

                    // don't pull/update every resource entity in at this point, do it in batches
                    List<Integer> resourceIds = resourceMgr.findIdsByTypeIds(Arrays.asList(existingType.getId()));
                    BatchIterator<Integer> batchIterator = new BatchIterator<Integer>(resourceIds, 200);
                    while (batchIterator.hasMoreBatches()) {
                        pluginConfigurationMetadataMgr.updateResourcePluginConfigurationsInNewTransaction(
                            batchIterator.getNextBatch(), updateReport);
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

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateResourcePluginConfigurationsInNewTransaction(List<Integer> resourceIds,
        ConfigurationDefinitionUpdateReport updateReport) {

        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterIds(resourceIds.toArray(new Integer[resourceIds.size()]));
        criteria.setPageControl(PageControl.getUnlimitedInstance());
        List<Resource> resources = resourceMgr.findResourcesByCriteria(subjectMgr.getOverlord(), criteria);

        for (Resource resource : resources) {
            boolean modified = false;
            ConfigurationTemplate template = updateReport.getConfigurationDefinition().getDefaultTemplate();
            Configuration templateConfiguration = template.getConfiguration();

            for (PropertyDefinition propertyDef : updateReport.getNewPropertyDefinitions()) {
                if (propertyDef.isRequired()) {
                    Property templateProperty = templateConfiguration.get(propertyDef.getName());
                    if (templateProperty == null) {
                        throw new IllegalArgumentException("The property [" + propertyDef.getName()
                            + "] marked as required in the configuration definition of ["
                            + propertyDef.getConfigurationDefinition().getName() + "] has no attribute 'default'");
                    } else {
                        // we only pull the configuration when an update is needed. The getProperties call
                        // just ensures the lazy load happens.
                        Configuration pluginConfiguration = resource.getPluginConfiguration();
                        int numberOfProperties = pluginConfiguration.getProperties().size();
                        pluginConfiguration.put(templateProperty.deepCopy(false));
                        modified = true;
                    }
                }
            }

            for (PropertyDefinition propertyDef : updateReport.getUpdatedPropertyDefinitions()) {
                if (propertyDef.isRequired()) {
                    // we only pull the configuration when an update is needed. The getProperties call
                    // just ensures the lazy load happens.
                    Configuration pluginConfiguration = resource.getPluginConfiguration();
                    int numberOfProperties = pluginConfiguration.getProperties().size();
                    String propertyValue = pluginConfiguration.getSimpleValue(propertyDef.getName(), null);
                    if (propertyValue == null) {
                        Property templateProperty = templateConfiguration.get(propertyDef.getName());
                        pluginConfiguration.put(templateProperty.deepCopy(false));
                        modified = true;
                    }
                }
            }

            if (modified) {
                resource.setAgentSynchronizationNeeded();
            }
        }
    }
}
