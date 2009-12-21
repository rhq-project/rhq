package org.rhq.sample.perspective;

import java.util.Random;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.client.RemoteClient;
import org.rhq.enterprise.server.resource.ResourceManagerRemote;

import org.jboss.seam.annotations.Name;

/**
 * A Seam component that utilizes the RHQ remote API.
 *
 * @author Ian Springer
 */
@Name("SampleUIBean")
public class SampleResourceUIBean {
    Resource randomResource;

    public SampleResourceUIBean() throws Exception {
        this.randomResource = createRandomResource();
    }

    public Resource getRandomResource() {
        return this.randomResource;
    }

    private Resource createRandomResource() throws Exception {
        RemoteClient remoteClient = new RemoteClient(null, "127.0.0.1", 7080);
        Subject subject = remoteClient.login("rhqadmin", "rhqadmin");
        ResourceManagerRemote resourceManager = remoteClient.getResourceManagerRemote();
        ResourceCriteria resourceCriteria = new ResourceCriteria();
        PageList<Resource> allResources = resourceManager.findResourcesByCriteria(subject, resourceCriteria);
        Random randomGenerator = new Random();
        int randomIndex = randomGenerator.nextInt(allResources.size());
        return allResources.get(randomIndex);
    }
}