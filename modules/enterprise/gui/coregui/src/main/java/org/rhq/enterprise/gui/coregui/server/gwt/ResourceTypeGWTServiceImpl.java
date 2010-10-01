package org.rhq.enterprise.gui.coregui.server.gwt;

import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceTypeGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ResourceTypeGWTServiceImpl extends AbstractGWTServiceImpl implements ResourceTypeGWTService {

    private static final long serialVersionUID = 1L;

    public PageList<ResourceType> findResourceTypesByCriteria(ResourceTypeCriteria criteria) {
        try {
            ResourceTypeManagerLocal typeManager = LookupUtil.getResourceTypeManager();

            return SerialUtility.prepare(typeManager.findResourceTypesByCriteria(getSessionSubject(), criteria),
                "ResourceTypes.findResourceTypesByCriteria");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    public RawConfiguration dummy(RawConfiguration config) {
        System.out.println(config.getPath());
        return new RawConfiguration();
    }
}