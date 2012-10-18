/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.schedules;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractSchedulesView;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * The Resource Monitoring>Schedules subtab.
 *
 * @author Jay Shaughnessy
 * @author Ian Springer
 */
public class ResourceSchedulesView extends AbstractSchedulesView {

    private static final String TITLE = MSG.view_resource_monitor_schedules_title();

    private int resourceId;

    public ResourceSchedulesView(String locatorId, ResourceComposite resourceComposite) {
        super(locatorId, TITLE, EntityContext.forResource(resourceComposite.getResource().getId()), resourceComposite
            .getResourcePermission().isMeasure());

        this.resourceId = resourceComposite.getResource().getId();
    }

    @Override
    protected void enableSchedules(final int[] measurementDefinitionIds,
        final List<String> measurementDefinitionDisplayNames) {

        measurementService.enableSchedulesForResource(this.resourceId, measurementDefinitionIds,
            new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable throwable) {
                    CoreGUI.getErrorHandler().handleError(
                        MSG.dataSource_schedules_enableFailure_resource(
                            String.valueOf(measurementDefinitionIds.length), String.valueOf(resourceId),
                            measurementDefinitionDisplayNames.toString()), throwable);
                }

                @Override
                public void onSuccess(Void aVoid) {
                    CoreGUI.getMessageCenter().notify(
                        new Message(MSG.dataSource_schedules_enableSuccessful_concise(String
                            .valueOf(measurementDefinitionIds.length)), MSG
                            .dataSource_schedules_enableSuccessful_full_resource(
                                String.valueOf(measurementDefinitionIds.length), String.valueOf(resourceId),
                                measurementDefinitionDisplayNames.toString()), Message.Severity.Info));
                    refresh();
                }
            });
    }

    @Override
    protected void disableSchedules(final int[] measurementDefinitionIds,
        final List<String> measurementDefinitionDisplayNames) {

        measurementService.disableSchedulesForResource(this.resourceId, measurementDefinitionIds,
            new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable throwable) {
                    CoreGUI.getErrorHandler().handleError(
                        MSG.dataSource_schedules_disableFailure_resource(
                            String.valueOf(measurementDefinitionIds.length), String.valueOf(resourceId),
                            measurementDefinitionDisplayNames.toString()), throwable);
                }

                @Override
                public void onSuccess(Void aVoid) {
                    CoreGUI.getMessageCenter().notify(
                        new Message(MSG.dataSource_schedules_disableSuccessful_concise(String
                            .valueOf(measurementDefinitionIds.length)), MSG
                            .dataSource_schedules_disableSuccessful_full_resource(
                                String.valueOf(measurementDefinitionIds.length), String.valueOf(resourceId),
                                measurementDefinitionDisplayNames.toString()), Message.Severity.Info));
                    refresh();
                }
            });
    }

    @Override
    protected void updateSchedules(final int[] measurementDefinitionIds,
        final List<String> measurementDefinitionDisplayNames, final long collectionInterval) {

        measurementService.updateSchedulesForResource(this.resourceId, measurementDefinitionIds, collectionInterval,
            new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable throwable) {
                    CoreGUI.getErrorHandler().handleError(
                        MSG.dataSource_schedules_updateFailure_resource(
                            String.valueOf(measurementDefinitionIds.length), String.valueOf(resourceId),
                            measurementDefinitionDisplayNames.toString(), String.valueOf(collectionInterval / 1000)),
                        throwable);
                }

                @Override
                public void onSuccess(Void aVoid) {
                    String collIntervalStr = String.valueOf(collectionInterval / 1000);
                    CoreGUI.getMessageCenter().notify(
                        new Message(MSG.dataSource_schedules_updateSuccessful_concise(collIntervalStr,
                            String.valueOf(measurementDefinitionIds.length)), MSG
                            .dataSource_schedules_updateSuccessful_full_resource(collIntervalStr,
                                String.valueOf(measurementDefinitionIds.length), String.valueOf(resourceId),
                                measurementDefinitionDisplayNames.toString()), Message.Severity.Info));
                    refresh();
                }
            });
    }



    public int getResourceId() {
        return resourceId;
    }

}
