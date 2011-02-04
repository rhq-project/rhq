package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.schedule;

import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.operation.schedule.AbstractOperationScheduleDetailsView;

/**
 * @author Ian Springer
 */
public class ResourceOperationScheduleDetailsView extends AbstractOperationScheduleDetailsView {

    private ResourceComposite resourceComposite;

    public ResourceOperationScheduleDetailsView(String locatorId, ResourceComposite resourceComposite, int scheduleId) {
        super(locatorId, new ResourceOperationScheduleDataSource(resourceComposite),
                resourceComposite.getResource().getResourceType(), scheduleId);

        this.resourceComposite = resourceComposite;
    }

    @Override
    protected boolean hasControlPermission() {
        return this.resourceComposite.getResourcePermission().isControl();
    }

}
