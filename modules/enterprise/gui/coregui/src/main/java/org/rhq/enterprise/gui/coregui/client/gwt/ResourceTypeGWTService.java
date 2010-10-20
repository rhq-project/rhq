package org.rhq.enterprise.gui.coregui.client.gwt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceTypeTemplateCountComposite;
import org.rhq.core.domain.util.PageList;

@RemoteServiceRelativePath("ResourceTypeGWTService")
public interface ResourceTypeGWTService extends RemoteService {

    PageList<ResourceType> findResourceTypesByCriteria(ResourceTypeCriteria criteria);

    ArrayList<ResourceType> getResourceTypesForResourceAncestors(int resourceId);

    ArrayList<ResourceType> getAllResourceTypeAncestors(int resourceTypeId);

    HashMap<Integer, String> getResourceTypeDescendantsWithOperations(int resourceTypeId);

    Map<Integer, ResourceTypeTemplateCountComposite> getTemplateCountCompositeMap();
}
