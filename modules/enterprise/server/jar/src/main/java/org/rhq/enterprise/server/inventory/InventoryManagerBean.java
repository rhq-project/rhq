package org.rhq.enterprise.server.inventory;

import org.rhq.core.clientapi.util.StringUtil;
import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.*;

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

    @Override
    public int markTypesDeleted(List<Integer> resourceTypeIds) {
        ResourceTypeCriteria criteria = new ResourceTypeCriteria();
        criteria.addFilterIds(resourceTypeIds.toArray(new Integer[resourceTypeIds.size()]));
        criteria.fetchResources(true);

        List<ResourceType> resourceTypes = resourceTypeMgr.findResourceTypesByCriteria(subjectMgr.getOverlord(),
                criteria);

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

}
