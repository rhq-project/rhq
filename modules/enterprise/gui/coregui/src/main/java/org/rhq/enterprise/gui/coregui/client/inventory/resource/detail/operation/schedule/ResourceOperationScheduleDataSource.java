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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.schedule;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;

import org.rhq.core.domain.operation.bean.ResourceOperationSchedule;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.operation.schedule.OperationScheduleDataSource;

/**
 * A DataSource for {@link ResourceOperationSchedule}s for a given {@link Resource}.
 *
 * @author Ian Springer
 */
public class ResourceOperationScheduleDataSource extends OperationScheduleDataSource<ResourceOperationSchedule> {

    private ResourceComposite resourceComposite;

    public ResourceOperationScheduleDataSource(ResourceComposite resourceComposite) {
        super(resourceComposite.getResource().getResourceType());
        this.resourceComposite = resourceComposite;
    }

    @Override
    protected ResourceOperationSchedule createOperationSchedule() {
        return new ResourceOperationSchedule();
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response) {
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
