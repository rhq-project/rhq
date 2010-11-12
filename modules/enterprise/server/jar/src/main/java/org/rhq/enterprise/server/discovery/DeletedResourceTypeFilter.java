package org.rhq.enterprise.server.discovery;

import java.util.HashSet;
import java.util.Set;

import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;

public class DeletedResourceTypeFilter implements InventoryReportFilter {

    private SubjectManagerLocal subjectMgr;

    private ResourceTypeManagerLocal resourceTypeMgr;

    private Set<String> deletedTypes;

    public DeletedResourceTypeFilter(SubjectManagerLocal subjectManager, ResourceTypeManagerLocal resourceTypeManager) {
        subjectMgr = subjectManager;
        resourceTypeMgr = resourceTypeManager;
        deletedTypes = new HashSet<String>();
        loadDeletedTypes();
    }

    private void loadDeletedTypes() {
        ResourceTypeCriteria criteria = new ResourceTypeCriteria();
        criteria.addFilterDeleted(true);
        PageList<ResourceType> results = resourceTypeMgr.findResourceTypesByCriteria(subjectMgr.getOverlord(),
            criteria);
        for (ResourceType type : results) {
            deletedTypes.add(type.getName() + "::" + type.getPlugin());
        }
    }

    public boolean accept(InventoryReport report) {
        for (Resource resource : report.getAddedRoots()) {
            if (containsDeletedType(resource)) {
                return false;
            }
        }

        return true;
    }

    private boolean containsDeletedType(Resource resource) {
        if (isDeleted(resource.getResourceType())) {
            return true;
        }
        for (Resource child : resource.getChildResources()) {
            if (containsDeletedType(child)) {
                return true;
            }
        }
        return false;
    }

    private boolean isDeleted(ResourceType type) {
        return deletedTypes.contains(type.getName() + "::" + type.getPlugin()) ||
               resourceTypeMgr.getResourceTypeByNameAndPlugin(type.getName(), type.getPlugin()) == null;
    }
}
