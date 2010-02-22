package org.rhq.enterprise.gui.coregui.server.gwt;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceTypeGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ResourceTypeGWTServiceImpl extends AbstractGWTServiceImpl implements ResourceTypeGWTService {

    public PageList<ResourceType> findResourceTypesByCriteria(ResourceTypeCriteria criteria) {

        ResourceTypeManagerLocal typeManager = LookupUtil.getResourceTypeManager();

        return SerialUtility.prepare(typeManager.findResourceTypesByCriteria(getSessionSubject(), criteria), "ResourceTypes.findResourceTypesByCriteria");
    }

    public RawConfiguration dummy(RawConfiguration config) {
        System.out.println(config.getPath());
        return new RawConfiguration();
    }
}