package org.rhq.sample.perspective;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.model.AbstractPagedDataModel;
import org.rhq.enterprise.server.resource.ResourceManagerRemote;

/**
 * @author Ian Springer
 */
public class ResourcesDataModel extends AbstractPagedDataModel<Resource, Integer> {
    private Subject subject;
    private ResourceManagerRemote resourceManager;

    public ResourcesDataModel(Subject subject, ResourceManagerRemote resourceManager) {
        this.subject = subject;
        this.resourceManager = resourceManager;
    }

    @Override
    protected PageList<Resource> findObjects(PageControl pageControl) {
        ResourceCriteria resourceCriteria = new ResourceCriteria();
        resourceCriteria.setPageControl(pageControl);
        return this.resourceManager.findResourcesByCriteria(this.subject, resourceCriteria);
    }

    @Override
    protected PageControl getDefaultPageControl() {
        return new PageControl(0, 10);
    }
}
