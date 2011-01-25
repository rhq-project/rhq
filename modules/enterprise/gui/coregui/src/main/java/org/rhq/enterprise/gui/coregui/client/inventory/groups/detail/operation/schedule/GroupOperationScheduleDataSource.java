/*
 * RHQ Management Platform
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.operation.schedule;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.operation.bean.GroupOperationSchedule;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.operation.schedule.OperationScheduleDataSource;

import java.util.List;

/**
 * A DataSource for {@link org.rhq.core.domain.operation.bean.GroupOperationSchedule}s for a given
 * {@link org.rhq.core.domain.resource.group.ResourceGroup}.
 *
 * @author Ian Springer
 */
public class GroupOperationScheduleDataSource extends OperationScheduleDataSource<GroupOperationSchedule> {

    private ResourceGroupComposite groupComposite;

    public GroupOperationScheduleDataSource(ResourceGroupComposite groupComposite) {
        super(groupComposite.getResourceGroup().getResourceType());
        this.groupComposite = groupComposite;
    }

    @Override
    protected GroupOperationSchedule createOperationSchedule() {
        GroupOperationSchedule groupOperationSchedule = new GroupOperationSchedule();
        groupOperationSchedule.setGroup(this.groupComposite.getResourceGroup());
        return groupOperationSchedule;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response) {
        operationService.findScheduledGroupOperations(this.groupComposite.getResourceGroup().getId(),
            new AsyncCallback<List<GroupOperationSchedule>>() {
                public void onSuccess(List<GroupOperationSchedule> result) {
                    Record[] records = buildRecords(result);
                    response.setData(records);
                    processResponse(request.getRequestId(), response);
                }

                public void onFailure(Throwable caught) {
                    throw new RuntimeException("Failed to find scheduled operations for "
                        + groupComposite.getResourceGroup() + ".", caught);
                }
            });
    }

    @Override
    protected void executeAdd(Record recordToAdd, final DSRequest request, final DSResponse response) {
        Configuration parameters = (Configuration) request.getAttributeAsObject("parameters");
        recordToAdd.setAttribute(Field.PARAMETERS, parameters);
        final GroupOperationSchedule scheduleToAdd = copyValues(recordToAdd);
        // TODO
        /*operationService.scheduleResourceOperation(scheduleToAdd, new AsyncCallback<Integer>() {
            public void onSuccess(Integer scheduleId) {
                scheduleToAdd.setId(scheduleId);
                sendSuccessResponse(request, response, scheduleToAdd);
            }

            public void onFailure(Throwable caught) {
                throw new RuntimeException("Failed to add " + scheduleToAdd, caught);
            }
        });*/
    }

    @Override
    protected void executeRemove(Record recordToRemove, final DSRequest request, final DSResponse response) {
        final GroupOperationSchedule scheduleToRemove = copyValues(recordToRemove);
        // TODO
        /*operationService.unscheduleResourceOperation(scheduleToRemove, new AsyncCallback<Void>() {
            public void onSuccess(Void result) {
                sendSuccessResponse(request, response, scheduleToRemove);
            }

            public void onFailure(Throwable caught) {
                throw new RuntimeException(caught);
            }
        });*/
    }

}
