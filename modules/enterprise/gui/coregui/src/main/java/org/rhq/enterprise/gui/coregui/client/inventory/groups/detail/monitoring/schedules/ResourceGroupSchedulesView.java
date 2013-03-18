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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.schedules;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractSchedulesView;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * The group Monitoring>Schedules subtab.
 *
 * @author Jay Shaughnessy
 * @author Ian Springer
 */
public class ResourceGroupSchedulesView extends AbstractSchedulesView {

    private static final String TITLE = MSG.view_group_meas_schedules_title();

    private ResourceGroupComposite resourceGroupComposite;
    private int groupId;

    public ResourceGroupSchedulesView(ResourceGroupComposite resourceGroupComposite) {
        super(TITLE, EntityContext.forGroup(resourceGroupComposite.getResourceGroup().getId()), resourceGroupComposite
            .getResourcePermission().isMeasure());

        this.resourceGroupComposite = resourceGroupComposite;
        this.groupId = resourceGroupComposite.getResourceGroup().getId();
    }

    @Override
    protected void enableSchedules(final int[] measurementDefinitionIds,
        final List<String> measurementDefinitionDisplayNames) {

        this.measurementService.enableSchedulesForCompatibleGroup(this.groupId, measurementDefinitionIds,
            new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable throwable) {
                    CoreGUI.getErrorHandler().handleError(
                        MSG.dataSource_schedules_enableFailure_group(String.valueOf(measurementDefinitionIds.length),
                            String.valueOf(groupId), measurementDefinitionDisplayNames.toString()), throwable);
                }

                @Override
                public void onSuccess(Void aVoid) {
                    CoreGUI.getMessageCenter().notify(
                        new Message(MSG.dataSource_schedules_enableSuccessful_concise(String
                            .valueOf(measurementDefinitionIds.length)), MSG
                            .dataSource_schedules_enableSuccessful_full_group(
                                String.valueOf(measurementDefinitionIds.length), String.valueOf(groupId),
                                measurementDefinitionDisplayNames.toString()), Message.Severity.Info));
                    refresh();
                }
            });
    }

    @Override
    protected void disableSchedules(final int[] measurementDefinitionIds,
        final List<String> measurementDefinitionDisplayNames) {

        this.measurementService.disableSchedulesForCompatibleGroup(this.groupId, measurementDefinitionIds,
            new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable throwable) {
                    CoreGUI.getErrorHandler().handleError(
                        MSG.dataSource_schedules_disableFailure_group(String.valueOf(measurementDefinitionIds.length),
                            String.valueOf(groupId), measurementDefinitionDisplayNames.toString()), throwable);
                }

                @Override
                public void onSuccess(Void aVoid) {
                    CoreGUI.getMessageCenter().notify(
                        new Message(MSG.dataSource_schedules_disableSuccessful_concise(String
                            .valueOf(measurementDefinitionIds.length)), MSG
                            .dataSource_schedules_disableSuccessful_full_group(
                                String.valueOf(measurementDefinitionIds.length), String.valueOf(groupId),
                                measurementDefinitionDisplayNames.toString()), Message.Severity.Info));
                    refresh();
                }
            });
    }

    @Override
    protected void updateSchedules(final int[] measurementDefinitionIds,
        final List<String> measurementDefinitionDisplayNames, final long collectionInterval) {

        this.measurementService.updateSchedulesForCompatibleGroup(this.groupId, measurementDefinitionIds,
            collectionInterval, new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable throwable) {
                    CoreGUI.getErrorHandler().handleError(
                        MSG.dataSource_schedules_updateFailure_group(String.valueOf(measurementDefinitionIds.length),
                            String.valueOf(groupId), measurementDefinitionDisplayNames.toString(),
                            String.valueOf(collectionInterval / 1000)), throwable);
                }

                @Override
                public void onSuccess(Void aVoid) {
                    String collIntervalStr = String.valueOf(collectionInterval / 1000);
                    CoreGUI.getMessageCenter().notify(
                        new Message(MSG.dataSource_schedules_updateSuccessful_concise(collIntervalStr,
                            String.valueOf(measurementDefinitionIds.length)), MSG
                            .dataSource_schedules_updateSuccessful_full_group(collIntervalStr,
                                String.valueOf(measurementDefinitionIds.length), String.valueOf(groupId),
                                measurementDefinitionDisplayNames.toString()), Message.Severity.Info));
                    refresh();
                }
            });
    }

    public ResourceGroupComposite getResourceGroupComposite() {
        return resourceGroupComposite;
    }

    public int getGroupId() {
        return groupId;
    }
}
