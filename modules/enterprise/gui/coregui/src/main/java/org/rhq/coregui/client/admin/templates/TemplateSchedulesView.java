/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.coregui.client.admin.templates;

import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.ResourceTypeUtility;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.inventory.common.AbstractSchedulesView;
import org.rhq.coregui.client.util.message.Message;

/**
 * A view for viewing and updating the default metric schedules ("metric templates") for a particular ResourceType.
 *
 * @author Jay Shaughnessy
 * @author Ian Springer
 */
public class TemplateSchedulesView extends AbstractSchedulesView {

    private boolean updateExistingSchedules = true;
    private String typeId;

    public TemplateSchedulesView(ResourceType type, Set<Permission> globalPermissions) {
        super(getTitle(type), EntityContext.forTemplate(type.getId()), globalPermissions
            .contains(Permission.MANAGE_INVENTORY));

        this.typeId = String.valueOf(type.getId());
    }

    public static String getTitle(ResourceType type) {
        return MSG.view_adminConfig_metricTemplates() + " [" + ResourceTypeUtility.displayName(type) + "]";
    }

    @Override
    protected void configureTable() {
        super.configureTable();

        addExtraWidget(new UpdateExistingSchedulesWidget(this), true);
    }

    public boolean isUpdateExistingSchedules() {
        return updateExistingSchedules;
    }

    public void setUpdateExistingSchedules(boolean updateExistingSchedules) {
        this.updateExistingSchedules = updateExistingSchedules;
    }

    @Override
    protected void enableSchedules(final int[] measurementDefinitionIds,
        final List<String> measurementDefinitionDisplayNames) {

        boolean updateExistingSchedules = isUpdateExistingSchedules();
        final String s = (measurementDefinitionIds.length > 1) ? "s" : "";
        this.measurementService.enableSchedulesForResourceType(measurementDefinitionIds, updateExistingSchedules,
            new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable throwable) {
                    CoreGUI.getErrorHandler().handleError(
                        MSG.datasource_templateSchedules_enabled_failed(s,
                            measurementDefinitionDisplayNames.toString(), typeId), throwable);
                }

                @Override
                public void onSuccess(Void aVoid) {
                    CoreGUI.getMessageCenter().notify(
                        new Message(MSG.datasource_templateSchedules_enabled(s), MSG
                            .datasource_templateSchedules_enabled_detailed(s,
                                measurementDefinitionDisplayNames.toString(), typeId), Message.Severity.Info));
                    refresh();
                }
            });
    }

    @Override
    protected void disableSchedules(int[] measurementDefinitionIds, final List<String> measurementDefinitionDisplayNames) {

        boolean updateExistingSchedules = isUpdateExistingSchedules();
        final String s = (measurementDefinitionIds.length > 1) ? "s" : "";
        this.measurementService.disableSchedulesForResourceType(measurementDefinitionIds, updateExistingSchedules,
            new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable throwable) {
                    CoreGUI.getErrorHandler().handleError(
                        MSG.datasource_templateSchedules_disabled_failed(s,
                            measurementDefinitionDisplayNames.toString(), typeId), throwable);
                }

                @Override
                public void onSuccess(Void aVoid) {
                    CoreGUI.getMessageCenter().notify(
                        new Message(MSG.datasource_templateSchedules_disabled(s), MSG
                            .datasource_templateSchedules_disabled_detailed(s,
                                measurementDefinitionDisplayNames.toString(), typeId), Message.Severity.Info));
                    refresh();
                }
            });
    }

    @Override
    protected void updateSchedules(int[] measurementDefinitionIds,
        final List<String> measurementDefinitionDisplayNames, final long collectionInterval) {

        boolean updateExistingSchedules = isUpdateExistingSchedules();
        final String s = (measurementDefinitionIds.length > 1) ? "s" : "";
        this.measurementService.updateSchedulesForResourceType(measurementDefinitionIds, collectionInterval,
            updateExistingSchedules, new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable throwable) {
                    CoreGUI.getErrorHandler().handleError(
                        MSG.datasource_templateSchedules_updated_failed(String.valueOf(collectionInterval / 1000), s,
                            measurementDefinitionDisplayNames.toString(), typeId), throwable);
                }

                @Override
                public void onSuccess(Void aVoid) {
                    CoreGUI.getMessageCenter().notify(
                        new Message(MSG.datasource_templateSchedules_updated(s), MSG
                            .datasource_templateSchedules_updated_detail(s,
                                measurementDefinitionDisplayNames.toString(), typeId,
                                String.valueOf(collectionInterval / 1000)), Message.Severity.Info));
                    refresh();
                }
            });
    }
}
