package org.rhq.sample.perspective;

import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.client.RemoteClient;
import org.rhq.enterprise.server.perspective.AbstractPerspectiveUIBean;
import org.rhq.enterprise.server.resource.ResourceManagerRemote;

import org.jboss.seam.annotations.Name;

/**
 * A Seam component that utilizes the RHQ remote API.
 * 
 * @author Ian Springer
 */
@Name("RandomResourceUIBean")
public class RandomResourceUIBean extends AbstractPerspectiveUIBean {
    private final Log log = LogFactory.getLog(this.getClass());

    private Resource randomResource;

    public RandomResourceUIBean() {
        return;
    }

    public Resource getRandomResource() throws Exception {
        if (this.randomResource == null) {
            this.randomResource = createRandomResource();
            log.info("Retrieved random Resource " + this.randomResource);
        }
        return this.randomResource;
    }

    private Resource createRandomResource() throws Exception {
        RemoteClient remoteClient = getRemoteClient();
        Subject subject = getSubject();
        // ***NOTE***: The javassist.NotFoundException stack traces that are logged by this call can be ignored.
        ResourceManagerRemote resourceManager = remoteClient.getResourceManagerRemote();
        ResourceCriteria resourceCriteria = new ResourceCriteria();            
        PageList<Resource> allResources = resourceManager.findResourcesByCriteria(subject, resourceCriteria);
        Random randomGenerator = new Random();
        int randomIndex = randomGenerator.nextInt(allResources.size());
        return allResources.get(randomIndex);
    }    
}