package org.rhq.sample.perspective;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.client.RemoteClient;
import org.rhq.enterprise.server.resource.ResourceManagerRemote;

import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.web.RequestParameter;

/**
 * A Seam component that utilizes the RHQ remote API.
 *
 * @author Ian Springer
 */
@Name("SampleResourceUIBean")
public class SampleResourceUIBean {
    @RequestParameter("id")
    private Integer resourceId;

    private Resource resource;

    public Resource getResource() throws Exception {
        if (this.resource == null) {
            this.resource = createResource();
        }
        return this.resource;
    }

    private Resource createResource() throws Exception {
        if (this.resourceId == null) {
            throw new IllegalStateException("The 'id' HTTP request parameter is required by this page.");
        }
        RemoteClient remoteClient = new RemoteClient(null, "127.0.0.1", 7080);
        Subject subject = remoteClient.login("rhqadmin", "rhqadmin");
        ResourceManagerRemote resourceManager = remoteClient.getResourceManagerRemote();
        ResourceCriteria resourceCriteria = new ResourceCriteria();
        resourceCriteria.addFilterId(this.resourceId);
        PageList<Resource> resources = resourceManager.findResourcesByCriteria(subject, resourceCriteria);
        if (resources.isEmpty()) {
            throw new IllegalStateException("No Resource exists with the id " + this.resourceId + ".");
        }
        return resources.get(0);
    }
}