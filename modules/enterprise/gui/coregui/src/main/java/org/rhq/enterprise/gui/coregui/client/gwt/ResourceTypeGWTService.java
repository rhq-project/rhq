package org.rhq.enterprise.gui.coregui.client.gwt;

import java.util.ArrayList;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;

@RemoteServiceRelativePath("ResourceTypeGWTService")
public interface ResourceTypeGWTService extends RemoteService {

    PageList<ResourceType> findResourceTypesByCriteria(ResourceTypeCriteria criteria);

    ArrayList<ResourceType> getResourceTypesForResourceAncestors(int resourceId);

    ArrayList<ResourceType> getAllResourceTypeAncestors(int resourceTypeId);

    ArrayList<ResourceType> getResourceTypeDescendantsWithOperations(int resourceTypeId);
}
