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
package org.rhq.coregui.client.inventory.groups.detail.operation.schedule;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.operation.bean.GroupOperationSchedule;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.coregui.client.inventory.common.detail.operation.schedule.AbstractOperationScheduleDataSource;

/**
 * A DataSource for {@link org.rhq.core.domain.operation.bean.GroupOperationSchedule}s for a given
 * {@link org.rhq.core.domain.resource.group.ResourceGroup}.
 *
 * @author Ian Springer
 */
public class GroupOperationScheduleDataSource extends AbstractOperationScheduleDataSource<GroupOperationSchedule> {

    public static abstract class Field extends AbstractOperationScheduleDataSource.Field {
        public static final String HALT_ON_FAILURE = "haltOnFailure";
        public static final String EXECUTION_ORDER = "executionOrder";
    }

    public static abstract class RequestProperty extends AbstractOperationScheduleDataSource.RequestProperty {
        public static final String EXECUTION_ORDER = "executionOrder";
    }

    private ResourceGroupComposite groupComposite;

    public GroupOperationScheduleDataSource(ResourceGroupComposite groupComposite) {
        super(groupComposite.getResourceGroup().getResourceType());
        this.groupComposite = groupComposite;
    }

    @Override
    protected GroupOperationSchedule createOperationSchedule() {
        GroupOperationSchedule groupOperationSchedule = new GroupOperationSchedule();
        ResourceGroup fakeGroup = new ResourceGroup("dummy");
        fakeGroup.setId(groupComposite.getResourceGroup().getId());
        groupOperationSchedule.setGroup(this.groupComposite.getResourceGroup());
        
//      resourceOperationSchedule.setResource(this.resourceComposite.getResource());
//      this was causing the serialization issues in GWT 2.5.0 (bz1058318), however there is no need to send the 
//      fully initialized resource group instance over the wire, because the SLFB needs only the id        
//        groupOperationSchedule.setGroup(this.groupComposite.getResourceGroup());
        return groupOperationSchedule;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response, final Criteria unused) {
        final Integer scheduleId = request.getCriteria().getAttributeAsInt(Field.ID);
        if (scheduleId != null) {
            operationService.getGroupOperationSchedule(scheduleId, new AsyncCallback<GroupOperationSchedule>() {
                public void onSuccess(GroupOperationSchedule result) {
                    sendSuccessResponse(request, response, result);
                }

                public void onFailure(Throwable caught) {
                    sendFailureResponse(request, response, "Failed to fetch GroupOperationSchedule with id "
                        + scheduleId + ".", caught);
                }
            });
        } else {
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
    }

    @Override
    protected void executeAdd(Record recordToAdd, final DSRequest request, final DSResponse response) {
        addRequestPropertiesToRecord(request, recordToAdd);

        final GroupOperationSchedule scheduleToAdd = copyValues(recordToAdd);

        operationService.scheduleGroupOperation(scheduleToAdd, new AsyncCallback<Integer>() {
            public void onSuccess(Integer scheduleId) {
                scheduleToAdd.setId(scheduleId);
                sendSuccessResponse(request, response, scheduleToAdd);
            }

            public void onFailure(Throwable caught) {
                throw new RuntimeException("Failed to add " + scheduleToAdd, caught);
            }
        });
    }

    @Override
    protected void addRequestPropertiesToRecord(DSRequest request, Record record) {
        super.addRequestPropertiesToRecord(request, record);

        List<Resource> executionOrder = (List<Resource>) request.getAttributeAsObject(RequestProperty.EXECUTION_ORDER);
        record.setAttribute(Field.EXECUTION_ORDER, executionOrder);
    }

    @Override
    protected void executeRemove(Record recordToRemove, final DSRequest request, final DSResponse response) {
        final GroupOperationSchedule scheduleToRemove = copyValues(recordToRemove);

        operationService.unscheduleGroupOperation(scheduleToRemove, new AsyncCallback<Void>() {
            public void onSuccess(Void result) {
                sendSuccessResponse(request, response, scheduleToRemove);
            }

            public void onFailure(Throwable caught) {
                throw new RuntimeException("Failed to remove " + scheduleToRemove, caught);
            }
        });
    }

    @Override
    public ListGridRecord copyValues(GroupOperationSchedule from) {
        ListGridRecord record = super.copyValues(from);

        record.setAttribute(Field.EXECUTION_ORDER, from.getExecutionOrder());
        record.setAttribute(Field.HALT_ON_FAILURE, from.getHaltOnFailure());

        return record;
    }

    @Override
    public GroupOperationSchedule copyValues(Record from) {
        GroupOperationSchedule groupOperationSchedule = super.copyValues(from);

        List<Resource> executionOrder = (List<Resource>) from.getAttributeAsObject(Field.EXECUTION_ORDER);
        groupOperationSchedule.setExecutionOrder(executionOrder);
        groupOperationSchedule.setHaltOnFailure(from.getAttributeAsBoolean(Field.HALT_ON_FAILURE));

        return groupOperationSchedule;
    }

}
