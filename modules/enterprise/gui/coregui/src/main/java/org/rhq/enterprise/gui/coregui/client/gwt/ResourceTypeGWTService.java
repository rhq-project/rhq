package org.rhq.enterprise.gui.coregui.client.gwt;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("ResourceTypeGWTService")
public interface ResourceTypeGWTService extends RemoteService {

    PageList<ResourceType> findResourceTypesByCriteria(ResourceTypeCriteria criteria);

    RawConfiguration dummy(RawConfiguration config);


}
