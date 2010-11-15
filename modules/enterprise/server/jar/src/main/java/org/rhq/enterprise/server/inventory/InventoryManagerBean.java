package org.rhq.enterprise.server.inventory;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.metadata.ResourceMetadataManagerLocal;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.*;

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
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public int markTypesDeleted(List<ResourceType> resourceTypes) {
        if (resourceTypes.size() == 0) {
            return 0;
        }

        Set<Integer> ids = new HashSet<Integer>();
        Set<Resource> resources = new HashSet<Resource>();

        for (ResourceType type : resourceTypes) {
            ids.add(type.getId());
            resources.addAll(type.getResources());
        }

        int[] resourceIds = new int[resources.size()];
        int i = 0;
        for (Resource resource : resources) {
            resourceIds[i++] = resource.getId();
        }

        resourceMgr.uninventoryResources(subjectMgr.getOverlord(), resourceIds);

        Map<Integer,  SortedSet<ResourceType>> childTypes =
                resourceTypeMgr.getChildResourceTypesForResourceTypes(resourceTypes);
        for (SortedSet<ResourceType> children : childTypes.values()) {
            for (ResourceType childType : children) {
                ids.add(childType.getId());
            }
        }

        Query query = entityMgr.createNamedQuery(ResourceType.QUERY_MARK_TYPES_DELETED);
        query.setParameter("resourceTypeIds", ids);
        return query.executeUpdate();
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

        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterResourceTypeId(resourceType.getId());
        criteria.addFilterInventoryStatus(null);

        List<Resource> resources = resourceMgr.findResourcesByCriteria(subjectMgr.getOverlord(), criteria);
        return resources.isEmpty();
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
