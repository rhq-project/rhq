package org.rhq.enterprise.server.perspective;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.web.RequestParameter;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.client.RemoteClient;
import org.rhq.enterprise.server.resource.ResourceManagerRemote;

/**
 * A base class for Seam components that utilize the RHQ remote API in the context of a specific Resource.
 */
public class AbstractPerspectiveResourceUIBean extends AbstractPerspectiveUIBean {
    private final Log log = LogFactory.getLog(this.getClass());

    @RequestParameter
    private Integer rhqResourceId;

    private Resource resource;

    public Resource getResource() throws Exception {
        if (this.resource == null) {
            this.resource = loadResource();
            log.debug("Retrieved current Resource " + this.resource);
        }
        return this.resource;
    }

    private Resource loadResource() throws Exception {
        if (this.rhqResourceId == null) {
            throw new IllegalStateException("The 'rhqResourceId' HTTP request parameter is required by this page.");
        }        
        RemoteClient remoteClient = this.perspectiveClient.getRemoteClient();
        Subject subject = this.perspectiveClient.getSubject();

        // ***NOTE***: The javassist.NotFoundException stack traces that are logged by this call can be ignored.
        ResourceManagerRemote resourceManager = remoteClient.getResourceManager();
        ResourceCriteria resourceCriteria = new ResourceCriteria();
        resourceCriteria.addFilterId(this.rhqResourceId);
        PageList<Resource> resources = resourceManager.findResourcesByCriteria(subject, resourceCriteria);
        if (resources.isEmpty()) {
            throw new IllegalStateException("No Resource exists with the id " + this.rhqResourceId + ".");
        }        
        return resources.get(0);
    }
}
