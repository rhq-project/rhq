package org.rhq.enterprise.gui.coregui.server.gwt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceLineageComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceTypeGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ResourceTypeGWTServiceImpl extends AbstractGWTServiceImpl implements ResourceTypeGWTService {

    private static final long serialVersionUID = 1L;

    @Override
    public PageList<ResourceType> findResourceTypesByCriteria(ResourceTypeCriteria criteria) {
        try {
            ResourceTypeManagerLocal typeManager = LookupUtil.getResourceTypeManager();

            return SerialUtility.prepare(typeManager.findResourceTypesByCriteria(getSessionSubject(), criteria),
                "ResourceTypes.findResourceTypesByCriteria");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    /**
     * Given a resource ID, this gets all resource types for all ancestors in that resource's lineage.
     */
    @Override
    public ArrayList<ResourceType> getResourceTypesForResourceAncestors(int resourceId) {
        try {
            ResourceManagerLocal manager = LookupUtil.getResourceManager();
            List<ResourceLineageComposite> lineage = manager.getResourceLineage(getSessionSubject(), resourceId);
            ArrayList<ResourceType> types = new ArrayList<ResourceType>(lineage.size());
            for (ResourceLineageComposite composite : lineage) {
                types.add(composite.getResource().getResourceType());
            }
            return SerialUtility.prepare(types, "ResourceTypes.getResourceTypesForResourceAncestors");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    @Override
    public ArrayList<ResourceType> getAllResourceTypeAncestors(int resourceTypeId) {
        try {
            ResourceTypeManagerLocal typeManager = LookupUtil.getResourceTypeManager();
            List<ResourceType> list = typeManager.getAllResourceTypeAncestors(getSessionSubject(), resourceTypeId);
            return SerialUtility
                .prepare(new ArrayList<ResourceType>(list), "ResourceTypes.getAllResourceTypeAncestors");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    @Override
    public HashMap<Integer, String> getResourceTypeDescendantsWithOperations(int resourceTypeId) {
        try {
            ResourceTypeManagerLocal typeManager = LookupUtil.getResourceTypeManager();
            HashMap<Integer, String> map = typeManager.getResourceTypeDescendantsWithOperations(getSessionSubject(),
                resourceTypeId);
            return SerialUtility.prepare(map, "ResourceTypes.getResourceTypeDescendantsWithOperations");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }
}