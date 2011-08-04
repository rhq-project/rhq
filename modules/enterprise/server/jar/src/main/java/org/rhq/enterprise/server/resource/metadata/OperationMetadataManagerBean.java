package org.rhq.enterprise.server.resource.metadata;

import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.OperationHistory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.operation.OperationManagerLocal;

@Stateless
public class OperationMetadataManagerBean implements OperationMetadataManagerLocal {

    private static final Log log = LogFactory.getLog(OperationMetadataManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityMgr;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateMetadata(ResourceType existingType, ResourceType newType) {
        log.debug("Updating operation definitions for " + existingType);

        existingType = entityMgr.find(ResourceType.class, existingType.getId());

        Set<OperationDefinition> existingDefinitions = existingType.getOperationDefinitions();
        Set<OperationDefinition> newDefinitions = newType.getOperationDefinitions();

        Set<OperationDefinition> newOps = CollectionsUtil.missingInFirstSet(existingDefinitions, newDefinitions);
        Set<OperationDefinition> opsToRemove = CollectionsUtil.missingInFirstSet(newDefinitions, existingDefinitions);

        existingDefinitions.retainAll(newDefinitions);

        if (log.isDebugEnabled()) {
            log.debug("Operation definitions to be added: " + newOps);
            log.debug("Operation definitions to be removed: " + opsToRemove);
            log.debug("Operation definitions to be updated: " + existingDefinitions);
        }

        // loop over the OperationDefinitions that are neither new nor deleted
        // and update them from the resourceType
        for (OperationDefinition def : existingDefinitions) {
            for (OperationDefinition nDef : newDefinitions) {
                if (def.equals(nDef)) {
                    def.setDescription(nDef.getDescription());
                    def.setDisplayName(nDef.getDisplayName());
                    def.setParametersConfigurationDefinition(nDef.getParametersConfigurationDefinition());
                    def.setResourceVersionRange(nDef.getResourceVersionRange());
                    def.setResultsConfigurationDefinition(nDef.getResultsConfigurationDefinition());
                    def.setTimeout(nDef.getTimeout());
                }
            }
        }

        for (OperationDefinition newOp : newOps) {
            existingType.addOperationDefinition(newOp); // does the back link as well
        }

        existingDefinitions.removeAll(opsToRemove);
        for (OperationDefinition opToDelete : opsToRemove) {
            // First delete history items that hang on the definition
            Query q = entityMgr.createNamedQuery(OperationHistory.QUERY_DELETE_BY_DEFINITION);
            q.setParameter("definition",opToDelete);
            q.executeUpdate();

            entityMgr.remove(opToDelete);
        }
    }

}
