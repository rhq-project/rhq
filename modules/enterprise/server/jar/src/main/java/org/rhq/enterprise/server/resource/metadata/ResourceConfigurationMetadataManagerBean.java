package org.rhq.enterprise.server.resource.metadata;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.configuration.metadata.ConfigurationMetadataManagerLocal;

@Stateless
public class ResourceConfigurationMetadataManagerBean implements ResourceConfigurationMetadataManagerLocal {

    private static final Log log = LogFactory.getLog(ResourceConfigurationMetadataManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityMgr;

    @EJB
    private ConfigurationMetadataManagerLocal configurationMetadataMgr;

    @Override
    public void updateResourceConfigurationDefinition(ResourceType existingType, ResourceType newType) {
        log.debug("Updating resource configuration definition for " + existingType);

        existingType = entityMgr.find(ResourceType.class, existingType.getId());
        ConfigurationDefinition newResourceConfigurationDefinition = newType.getResourceConfigurationDefinition();
        if (newResourceConfigurationDefinition != null) {
            if (existingType.getResourceConfigurationDefinition() == null) {
                if (log.isDebugEnabled()) {
                    log.debug(existingType + " currently does not have a resource configuration definition. Adding " +
                            "new resource configuration definition.");
                }
                entityMgr.persist(newResourceConfigurationDefinition);
                existingType.setResourceConfigurationDefinition(newResourceConfigurationDefinition);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Updating existing resource configuration definition for " + existingType);
                }
                ConfigurationDefinition existingDefinition = existingType.getResourceConfigurationDefinition();
                configurationMetadataMgr.updateConfigurationDefinition(newResourceConfigurationDefinition,
                    existingDefinition);
            }
        } else { // newDefinition == null
            if (existingType.getResourceConfigurationDefinition() != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Removing resource configuration definition for " + existingType);
                }
                existingType.setResourceConfigurationDefinition(null);
            }
        }
    }

}
