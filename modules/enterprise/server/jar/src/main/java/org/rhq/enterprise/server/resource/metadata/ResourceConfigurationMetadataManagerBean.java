package org.rhq.enterprise.server.resource.metadata;

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.configuration.metadata.ConfigurationMetadataManagerLocal;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class ResourceConfigurationMetadataManagerBean implements ResourceConfigurationMetadataManagerLocal {

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityMgr;

    @EJB
    private ConfigurationMetadataManagerLocal configurationMetadataMgr;

    @Override
    public void updateResourceConfigurationDefinition(ResourceType existingType, ResourceType newType) {
        ConfigurationDefinition newResourceConfigurationDefinition = newType.getResourceConfigurationDefinition();
        if (newResourceConfigurationDefinition != null) {
            if (existingType.getResourceConfigurationDefinition() == null) {
                entityMgr.persist(newResourceConfigurationDefinition);
                existingType.setResourceConfigurationDefinition(newResourceConfigurationDefinition);
            } else {
                ConfigurationDefinition existingDefinition = existingType.getResourceConfigurationDefinition();
                configurationMetadataMgr.updateConfigurationDefinition(newResourceConfigurationDefinition,
                    existingDefinition);
            }
        } else { // newDefinition == null
            if (existingType.getResourceConfigurationDefinition() != null) {
                existingType.setResourceConfigurationDefinition(null);
            }
        }
    }

}
