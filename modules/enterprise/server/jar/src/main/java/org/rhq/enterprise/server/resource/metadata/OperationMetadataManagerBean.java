package org.rhq.enterprise.server.resource.metadata;

import org.apache.commons.collections.CollectionUtils;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.RHQConstants;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@Stateless
public class OperationMetadataManagerBean implements OperationMetadataManagerLocal {

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityMgr;

    @Override
    public void updateMetadata(ResourceType existingType, ResourceType newType) {
        Set<OperationDefinition> existingDefinitions = existingType.getOperationDefinitions();
        Set<OperationDefinition> newDefinitions = newType.getOperationDefinitions();

        Set<OperationDefinition> newOps = missingInFirstSet(existingDefinitions, newDefinitions);
        Set<OperationDefinition> opsToRemove = missingInFirstSet(newDefinitions, existingDefinitions);

        existingDefinitions.retainAll(newDefinitions);

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
            entityMgr.remove(opToDelete);
        }
    }

    private <T> Set<T> missingInFirstSet(Set<T> first, Set<T> reference) {
        Set<T> result = new HashSet<T>();

        if (reference != null) {
            // First collection is null -> everything is missing
            if (first == null) {
                result.addAll(reference);
                return result;
            }

            // else loop over the set and sort out the right items.
            for (T item : reference) {
                //                if (!first.contains(item)) {
                //                    result.add(item);
                //                }
                boolean found = false;
                Iterator<T> iter = first.iterator();
                while (iter.hasNext()) {
                    T f = iter.next();
                    if (f.equals(item)) {
                        found = true;
                        break;
                    }
                }
                if (!found)
                    result.add(item);
            }
        }

        return result;
//        return new HashSet<T>(CollectionUtils.retainAll(first, reference));
    }

    private <T> Set<T> intersection(Set<T> first, Set<T> second) {
        Set<T> result = new HashSet<T>();
        if ((first != null) && (second != null)) {
            result.addAll(first);
            //            result.retainAll(second);
            Iterator<T> iter = result.iterator();
            boolean found;
            while (iter.hasNext()) {
                T item = iter.next();
                found = false;
                for (T s : second) {
                    if (s.equals(item))
                        found = true;
                }
                if (!found)
                    iter.remove();
            }
        }

        return result;
//        return new HashSet<T>(CollectionUtils.intersection(first, second));
    }
}
