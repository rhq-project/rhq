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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.schedules;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.MeasurementScheduleCriteria;
import org.rhq.core.domain.measurement.composite.MeasurementScheduleComposite;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.MeasurementDataGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMeasurementScheduleCompositeDataSource;
import org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMeasurementScheduleListView;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * A DataSource for reading and updating the metric schedules for the current group.
 *
 * @author Ian Springer
 */
public class SchedulesDataSource extends AbstractMeasurementScheduleCompositeDataSource {
    private MeasurementDataGWTServiceAsync measurementService = GWTServiceLookup.getMeasurementDataService();
    private int resourceGroupId;

    public SchedulesDataSource(int resourceGroupId) {
        this.resourceGroupId = resourceGroupId;
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        DataSourceField resourceGroupIdField = new DataSourceIntegerField(
            MeasurementScheduleCriteria.FILTER_FIELD_RESOURCE_GROUP_ID, MSG
                .dataSource_schedules_field_resourceGroupId());
        resourceGroupIdField.setHidden(true);
        fields.add(resourceGroupIdField);

        return fields;
    }

    @Override
    protected void executeFetch(DSRequest request, DSResponse response) {
        super.executeFetch(request, response);

    }

    @Override
    protected EntityContext getEntityContext(DSRequest request) {
        Criteria requestCriteria = request.getCriteria();
        Integer groupId = requestCriteria.getAttributeAsInt(MeasurementScheduleCriteria.FILTER_FIELD_RESOURCE_GROUP_ID);
        return EntityContext.forGroup(groupId);
    }

    @Override
    public ListGridRecord copyValues(MeasurementScheduleComposite from) {
        ListGridRecord record = super.copyValues(from);
        record.setAttribute(MeasurementScheduleCriteria.FILTER_FIELD_RESOURCE_GROUP_ID, this.resourceGroupId);
        return record;
    }

    @Override
    protected void enableSchedules(final AbstractMeasurementScheduleListView measurementScheduleListView,
        final int[] measurementDefinitionIds, final List<String> measurementDefinitionDisplayNames) {

        this.measurementService.enableSchedulesForCompatibleGroup(this.resourceGroupId, measurementDefinitionIds,
            new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable throwable) {
                    CoreGUI.getErrorHandler().handleError(
                        MSG.dataSource_schedules_enableFailure(String.valueOf(measurementDefinitionIds.length), String
                            .valueOf(resourceGroupId), measurementDefinitionDisplayNames.toString()), throwable);
                }

                @Override
                public void onSuccess(Void aVoid) {
                    CoreGUI.getMessageCenter().notify(
                        new Message(MSG.dataSource_schedules_enableSuccessful_concise(String
                            .valueOf(measurementDefinitionIds.length)), MSG.dataSource_schedules_enableSuccessful_full(
                            String.valueOf(measurementDefinitionIds.length), String.valueOf(resourceGroupId),
                            measurementDefinitionDisplayNames.toString()), Message.Severity.Info));
                    measurementScheduleListView.refresh();
                }
            });
    }

    @Override
    protected void disableSchedules(final AbstractMeasurementScheduleListView measurementScheduleListView,
        final int[] measurementDefinitionIds, final List<String> measurementDefinitionDisplayNames) {

        this.measurementService.disableSchedulesForCompatibleGroup(this.resourceGroupId, measurementDefinitionIds,
            new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable throwable) {
                    CoreGUI.getErrorHandler().handleError(
                        MSG.dataSource_schedules_disableFailure(String.valueOf(measurementDefinitionIds.length), String
                            .valueOf(resourceGroupId), measurementDefinitionDisplayNames.toString()), throwable);
                }

                @Override
                public void onSuccess(Void aVoid) {
                    CoreGUI.getMessageCenter().notify(
                        new Message(MSG.dataSource_schedules_disableSuccessful_concise(String
                            .valueOf(measurementDefinitionIds.length)), MSG
                            .dataSource_schedules_disableSuccessful_full(String
                                .valueOf(measurementDefinitionIds.length), String.valueOf(resourceGroupId),
                                measurementDefinitionDisplayNames.toString()), Message.Severity.Info));
                    measurementScheduleListView.refresh();
                }
            });
    }

    @Override
    protected void updateSchedules(final AbstractMeasurementScheduleListView measurementScheduleListView,
        final int[] measurementDefinitionIds, final List<String> measurementDefinitionDisplayNames,
        final long collectionInterval) {

        this.measurementService.updateSchedulesForCompatibleGroup(this.resourceGroupId, measurementDefinitionIds,
            collectionInterval, new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable throwable) {
                    CoreGUI.getErrorHandler().handleError(
                        MSG.dataSource_schedules_updateFailure(String.valueOf(measurementDefinitionIds.length), String
                            .valueOf(resourceGroupId), measurementDefinitionDisplayNames.toString(), String
                            .valueOf(collectionInterval / 1000)), throwable);
                }

                @Override
                public void onSuccess(Void aVoid) {
                    String collIntervalStr = String.valueOf(collectionInterval / 1000);
                    CoreGUI.getMessageCenter().notify(
                        new Message(MSG.dataSource_schedules_updateSuccessful_concise(collIntervalStr, String
                            .valueOf(measurementDefinitionIds.length)), MSG.dataSource_schedules_updateSuccessful_full(
                            collIntervalStr, String.valueOf(measurementDefinitionIds.length), String
                                .valueOf(resourceGroupId), measurementDefinitionDisplayNames.toString()),
                            Message.Severity.Info));
                    measurementScheduleListView.refresh();
                }
            });
    }
}
