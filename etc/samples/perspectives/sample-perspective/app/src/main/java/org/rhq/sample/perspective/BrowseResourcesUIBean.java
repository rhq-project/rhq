package org.rhq.sample.perspective;

import org.jboss.seam.annotations.Name;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.client.RemoteClient;
import org.rhq.enterprise.server.resource.ResourceManagerRemote;

/**
 * A Seam component that utilizes the RHQ remote API to obtain a paged list of all inventoried Resources.
 *
 * @author Ian Springer
 */
@Name("BrowseResourcesUIBean")
public class BrowseResourcesUIBean extends AbstractPerspectiveUIBean {
    private ResourcesDataModel dataModel;

    public BrowseResourcesUIBean() {
        return;
    }

    public ResourcesDataModel getDataModel() throws Exception {
        if (this.dataModel == null) {
            this.dataModel = createDataModel();
        }
        return this.dataModel;
    }

    private ResourcesDataModel createDataModel() throws Exception {
        RemoteClient remoteClient = getRemoteClient();
        Subject subject = getSubject();

        // ***NOTE***: The javassist.NotFoundException stack traces that are logged by this call can be ignored.
        ResourceManagerRemote resourceManager = remoteClient.getResourceManagerRemote();                

        return new ResourcesDataModel(subject, resourceManager);
    }
}