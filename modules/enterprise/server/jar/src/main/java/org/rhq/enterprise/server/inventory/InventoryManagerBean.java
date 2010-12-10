package org.rhq.enterprise.server.inventory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.metadata.ResourceMetadataManagerLocal;
import org.rhq.enterprise.server.util.BatchIterator;

/**
 * This API could not be added directly {@link org.rhq.enterprise.server.resource.ResourceTypeManagerBean} because it
 * would create a circular dependency with {@link org.rhq.enterprise.server.resource.ResourceManagerBean}, resulting
 * in a deployment failure.
 */
@Stateless
public class InventoryManagerBean implements InventoryManagerLocal {

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    EntityManager entityMgr;

    @EJB
    private SubjectManagerLocal subjectMgr;

    @EJB
    private ResourceTypeManagerLocal resourceTypeMgr;

    @EJB
    private ResourceManagerLocal resourceMgr;

    @EJB
    private ResourceMetadataManagerLocal metadataMgr;

    @Override
    @SuppressWarnings("unchecked")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public int markTypesDeleted(List<Integer> resourceTypeIds) {
        int typesDeleted = 0;
        BatchIterator<Integer> batchIterator = new BatchIterator<Integer>(resourceTypeIds);
        for (List<Integer> typeIdsBatch : batchIterator) {
            List<Integer> resourceIds = resourceMgr.findIdsByTypeIds(typeIdsBatch);
            resourceMgr.uninventoryResources(subjectMgr.getOverlord(), toIntArray(resourceIds));

            Query query = entityMgr.createNamedQuery(ResourceType.QUERY_MARK_TYPES_DELETED);
            query.setParameter("resourceTypeIds", typeIdsBatch);
            typesDeleted += query.executeUpdate();
        }
        return typesDeleted;
    }

    private int[] toIntArray(List<Integer> list) {
        int[] array = new int[list.size()];
        int i = 0;
        for (Integer integer : list) {
            array[i++] = integer;
        }
        return array;
    }

    @Override
    public List<ResourceType> getDeletedTypes() {
        ResourceTypeCriteria criteria = new ResourceTypeCriteria();
        criteria.addFilterDeleted(true);

        return resourceTypeMgr.findResourceTypesByCriteria(subjectMgr.getOverlord(), criteria);
    }

    @Override
    public boolean isReadyForPermanentRemoval(ResourceType resourceType) {
        if (!resourceType.isDeleted()) {
            return false;
        }
        Number count = (Number) entityMgr.createQuery("select count(r) from Resource r where r.resourceType = :type")
            .setParameter("type", resourceType)
            .getSingleResult();
        return count.intValue() == 0;
    }

    @Override
    public void purgeDeletedResourceType(ResourceType resourceType) {
        try {
            metadataMgr.completeRemoveResourceType(subjectMgr.getOverlord(), resourceType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to purge resource types", e);
        }
    }
}
