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
package org.rhq.coregui.client.inventory.resource.detail.operation.schedule;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;

import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.operation.bean.ResourceOperationSchedule;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.coregui.client.inventory.common.detail.operation.schedule.AbstractOperationScheduleDataSource;

/**
 * A DataSource for {@link ResourceOperationSchedule}s for a given {@link Resource}.
 *
 * @author Ian Springer
 */
public class ResourceOperationScheduleDataSource extends AbstractOperationScheduleDataSource<ResourceOperationSchedule> {

    private ResourceComposite resourceComposite;

    public ResourceOperationScheduleDataSource(ResourceComposite resourceComposite) {
        super(resourceComposite.getResource().getResourceType());
        this.resourceComposite = resourceComposite;
    }

    @Override
    protected ResourceOperationSchedule createOperationSchedule() {
        ResourceOperationSchedule resourceOperationSchedule = new ResourceOperationSchedule();
        Resource fakeResource = new Resource();
        fakeResource.setId(resourceComposite.getResource().getId());
        resourceOperationSchedule.setResource(fakeResource);
        
//        resourceOperationSchedule.setResource(this.resourceComposite.getResource());
//        this was causing the serialization issues in GWT 2.5.0 (bz1058318), however there is no need to send the 
//        fully initialized resource instance over the wire, because the SLFB needs only the id
        return resourceOperationSchedule;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response, final Criteria unused) {
        final Integer scheduleId = request.getCriteria().getAttributeAsInt(Field.ID);
        if (scheduleId != null) {
            operationService.getResourceOperationSchedule(scheduleId, new AsyncCallback<ResourceOperationSchedule>() {
                public void onSuccess(ResourceOperationSchedule result) {
                    sendSuccessResponse(request, response, result);
                }

                public void onFailure(Throwable caught) {
                    sendFailureResponse(request, response, "Failed to fetch ResourceOperationSchedule with id "
                        + scheduleId + ".", caught);
                }
            });
        } else {
            operationService.findScheduledResourceOperations(this.resourceComposite.getResource().getId(),
                new AsyncCallback<List<ResourceOperationSchedule>>() {
                    public void onSuccess(List<ResourceOperationSchedule> result) {
                        Record[] records = buildRecords(result);
                        response.setData(records);
                        processResponse(request.getRequestId(), response);
                    }

                    public void onFailure(Throwable caught) {
                        throw new RuntimeException("Failed to find scheduled operations for "
                            + resourceComposite.getResource() + ".", caught);
                    }
                });
        }
    }

    @Override
    protected void executeAdd(Record recordToAdd, final DSRequest request, final DSResponse response) {
        addRequestPropertiesToRecord(request, recordToAdd);

        final ResourceOperationSchedule scheduleToAdd = copyValues(recordToAdd);

        operationService.scheduleResourceOperation(scheduleToAdd, new AsyncCallback<Integer>() {
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
    protected void executeRemove(Record recordToRemove, final DSRequest request, final DSResponse response) {
        final ResourceOperationSchedule scheduleToRemove = copyValues(recordToRemove);

        operationService.unscheduleResourceOperation(scheduleToRemove, new AsyncCallback<Void>() {
            public void onSuccess(Void result) {
                sendSuccessResponse(request, response, scheduleToRemove);
            }

            public void onFailure(Throwable caught) {
                throw new RuntimeException(caught);
            }
        });
    }
}
