package org.rhq.coregui.client.gwt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.resource.MissingPolicy;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceTypeTemplateCountComposite;
import org.rhq.core.domain.util.PageList;

@RemoteServiceRelativePath("ResourceTypeGWTService")
public interface ResourceTypeGWTService extends RemoteService {

    void setResourceTypeIgnoreFlag(int resourceTypeId, boolean ignoreFlag) throws RuntimeException;

    void setResourceTypeMissingPolicy(int resourceTypeId, MissingPolicy policy) throws RuntimeException;

    PageList<ResourceType> findResourceTypesByCriteria(ResourceTypeCriteria criteria) throws RuntimeException;

    ArrayList<ResourceType> getResourceTypesForResourceAncestors(int resourceId) throws RuntimeException;

    ArrayList<ResourceType> getAllResourceTypeAncestors(int resourceTypeId) throws RuntimeException;

    HashMap<Integer, String> getResourceTypeDescendantsWithOperations(int resourceTypeId) throws RuntimeException;

    Map<Integer, ResourceTypeTemplateCountComposite> getTemplateCountCompositeMap() throws RuntimeException;
}
