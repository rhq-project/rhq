package org.rhq.enterprise.gui.common.framework;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;

public abstract class EnterpriseFacesContextUIBean {
    public Subject getSubject() {
        return EnterpriseFacesContextUtility.getSubject();
    }

    public Resource getResource() {
        return EnterpriseFacesContextUtility.getResource();
    }

    public ResourceGroup getResourceGroup() {
        return EnterpriseFacesContextUtility.getResourceGroup();
    }

    public ResourceType getResourceType() {
        return EnterpriseFacesContextUtility.getResourceType();
    }

    public WebUser getWebUser() {
        return EnterpriseFacesContextUtility.getWebUser();
    }
}
