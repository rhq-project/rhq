package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.schedule;

import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.operation.schedule.AbstractOperationScheduleDetailsView;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.operation.schedule.OperationScheduleDataSource;

/**
 *
 */
public class ResourceOperationScheduleDetailsView extends AbstractOperationScheduleDetailsView {

    public ResourceOperationScheduleDetailsView(String locatorId, ResourceComposite resourceComposite, int scheduleId) {
        super(locatorId, new ResourceOperationScheduleDataSource(resourceComposite),
                resourceComposite.getResource().getResourceType(), scheduleId);
    }

}
