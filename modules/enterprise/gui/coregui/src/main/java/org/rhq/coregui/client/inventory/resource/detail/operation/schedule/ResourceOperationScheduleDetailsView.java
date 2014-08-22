/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
 * All rights reserved.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.coregui.client.inventory.resource.detail.operation.schedule;

import com.google.gwt.user.client.rpc.AsyncCallback;

import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.operation.OperationHistory;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.inventory.common.detail.operation.schedule.AbstractOperationScheduleDetailsView;
import org.rhq.coregui.client.util.async.Command;
import org.rhq.coregui.client.util.async.CountDownLatch;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.message.Message.Severity;

/**
 * The details view of the Resource Operations>Schedules subtab.
 *
 * @author Ian Springer
 */
public class ResourceOperationScheduleDetailsView extends AbstractOperationScheduleDetailsView {

    private ResourceComposite resourceComposite;
    private ResourceOperationHistory operationExample;

    public ResourceOperationScheduleDetailsView(ResourceComposite resourceComposite, int scheduleId) {
        super(new ResourceOperationScheduleDataSource(resourceComposite), resourceComposite.getResource()
            .getResourceType(), scheduleId);
        this.resourceComposite = resourceComposite;
    }

    @Override
    protected boolean hasControlPermission() {
        return this.resourceComposite.getResourcePermission().isControl();
    }

    @Override
    protected int getResourceId() {
        return this.resourceComposite.getResource().getId();
    }

    @Override
    protected OperationHistory getOperationExample() {
        return operationExample;
    }

    @Override
    protected void init(final boolean isReadOnly) {
        if (isNewRecord() && getOperationExampleId() != null) {

            final CountDownLatch latch = CountDownLatch.create(1, new Command() {
                @Override
                public void execute() {
                    ResourceOperationScheduleDetailsView.super.init(isReadOnly);
                }
            });

            ResourceOperationHistoryCriteria historyCriteria = new ResourceOperationHistoryCriteria();
            historyCriteria.addFilterId(getOperationExampleId());
            historyCriteria.fetchOperationDefinition(true);
            historyCriteria.fetchParameters(true);
            historyCriteria.setPageControl(PageControl.getSingleRowInstance());
            GWTServiceLookup.getOperationService().findResourceOperationHistoriesByCriteria(historyCriteria,
                new LoadExampleCallback(latch));

        } else {
            super.init(isReadOnly);
        }
    }

    private class LoadExampleCallback implements AsyncCallback<PageList<ResourceOperationHistory>> {
        private final CountDownLatch latch;

        public LoadExampleCallback(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onFailure(Throwable throwable) {
            CoreGUI.getMessageCenter().notify(
                new Message(MSG.view_operationScheduleDetails_load_example_failure(), throwable, Severity.Warning));
            latch.countDown();
        }

        @Override
        public void onSuccess(PageList<ResourceOperationHistory> resourceOperationHistories) {
            if (resourceOperationHistories.getTotalSize() == 0) {
                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.view_operationScheduleDetails_example_not_found(), Severity.Warning));
            } else if (resourceOperationHistories.getTotalSize() != 1) {
                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.view_operationScheduleDetails_example_not_unique(), Severity.Warning));
            } else {
                operationExample = resourceOperationHistories.get(0);
            }
            latch.countDown();
        }
    }
}
