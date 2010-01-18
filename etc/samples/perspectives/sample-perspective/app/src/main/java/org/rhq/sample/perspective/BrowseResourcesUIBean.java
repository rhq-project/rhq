package org.rhq.sample.perspective;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.model.PagedDataModel;
import org.rhq.core.gui.model.PagedDataProvider;
import org.rhq.core.util.collection.ArrayUtils;
import org.rhq.enterprise.client.RemoteClient;
import org.rhq.enterprise.server.resource.ResourceManagerRemote;
import org.rhq.enterprise.server.perspective.AbstractPagedDataPerspectiveUIBean;
import org.richfaces.model.selection.Selection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A Seam component that utilizes the RHQ remote API to obtain a paged list of all inventoried Resources.
 *
 * @author Ian Springer
 */
@Name("BrowseResourcesUIBean")
@Scope(ScopeType.CONVERSATION)
public class BrowseResourcesUIBean extends AbstractPagedDataPerspectiveUIBean {
    private PagedDataModel<Resource> dataModel;

    public BrowseResourcesUIBean() {
        return;
    }

    @Begin(join = true)
    public PagedDataModel<Resource> getDataModel() throws Exception {
        if (this.dataModel == null) {
            this.dataModel = createDataModel();
        }
        return this.dataModel;
    }

    public void uninventorySelectedResources() throws Exception {
        RemoteClient remoteClient = getRemoteClient();
        Subject subject = getSubject();

        // ***NOTE***: The javassist.NotFoundException stack traces that are logged by this call can be ignored.
        ResourceManagerRemote resourceManager = remoteClient.getResourceManagerRemote();

        Selection selection = getSelection();
        Iterator<Object> keyIterator = selection.getKeys();
        List<Integer> resourceIds = new ArrayList<Integer>();
        while (keyIterator.hasNext()) {
            Integer resourceId = (Integer) keyIterator.next();
            if (selection.isSelected(resourceId)) {
                resourceIds.add(resourceId);
            }
        }
        int[] ids = ArrayUtils.unwrapCollection(resourceIds);

        resourceManager.uninventoryResources(subject, ids);
        // TODO: Add a Faces Message.

        // Reset the data model, so the current page will get refreshed to reflect the Resources we just uninventoried.
        this.dataModel = null;
    }

    @Override
    protected PageControl getDefaultPageControl() {
        PageControl defaultPageControl = super.getDefaultPageControl();
        defaultPageControl.addDefaultOrderingField("r.id");
        return defaultPageControl;
    }

    private PagedDataModel<Resource> createDataModel() throws Exception {
        RemoteClient remoteClient = getRemoteClient();
        Subject subject = getSubject();

        // ***NOTE***: The javassist.NotFoundException stack traces that are logged by this call can be ignored.
        ResourceManagerRemote resourceManager = remoteClient.getResourceManagerRemote();

        ResourcesDataProvider dataProvider = new ResourcesDataProvider(subject, resourceManager);
        return new PagedDataModel<Resource>(dataProvider);
    }

    private class ResourcesDataProvider implements PagedDataProvider<Resource> {
        private Subject subject;
        private ResourceManagerRemote resourceManager;

        public ResourcesDataProvider(Subject subject, ResourceManagerRemote resourceManager) {
            this.subject = subject;
            this.resourceManager = resourceManager;
        }

        public PageList<Resource> getDataPage(PageControl pageControl) {
            ResourceCriteria resourceCriteria = new ResourceCriteria();
            resourceCriteria.setPageControl(pageControl);
            return this.resourceManager.findResourcesByCriteria(this.subject, resourceCriteria);
        }
    }
}