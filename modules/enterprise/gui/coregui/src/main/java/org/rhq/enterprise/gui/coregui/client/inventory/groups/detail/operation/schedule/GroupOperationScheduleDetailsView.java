package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.operation.schedule;

import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.operation.schedule.AbstractOperationScheduleDetailsView;

/**
 *
 */
public class GroupOperationScheduleDetailsView extends AbstractOperationScheduleDetailsView {

    public GroupOperationScheduleDetailsView(String locatorId, ResourceGroupComposite groupComposite, int scheduleId) {
        super(locatorId, new GroupOperationScheduleDataSource(groupComposite),
                groupComposite.getResourceGroup().getResourceType(), scheduleId);
    }

}
