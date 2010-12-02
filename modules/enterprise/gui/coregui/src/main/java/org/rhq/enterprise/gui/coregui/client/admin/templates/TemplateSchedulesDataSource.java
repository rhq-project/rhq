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
package org.rhq.enterprise.gui.coregui.client.admin.templates;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
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
 * A DataSource for reading and updating the default metric schedules ("metric templates") for a particular ResourceType.
 *
 * @author Ian Springer
 */
public class TemplateSchedulesDataSource extends AbstractMeasurementScheduleCompositeDataSource {
    private MeasurementDataGWTServiceAsync measurementService = GWTServiceLookup.getMeasurementDataService();
    private int resourceTypeId;

    public TemplateSchedulesDataSource(int resourceTypeId) {
        this.resourceTypeId = resourceTypeId;
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        DataSourceField resourceGroupIdField = new DataSourceIntegerField(
            MeasurementScheduleCriteria.FILTER_FIELD_RESOURCE_TYPE_ID, "Resource Type Id");
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
        Integer typeId = requestCriteria.getAttributeAsInt(MeasurementScheduleCriteria.FILTER_FIELD_RESOURCE_TYPE_ID);
        return EntityContext.forTemplate(typeId);
    }

    @Override
    public ListGridRecord copyValues(MeasurementScheduleComposite from) {
        ListGridRecord record = super.copyValues(from);
        record.setAttribute(MeasurementScheduleCriteria.FILTER_FIELD_RESOURCE_TYPE_ID, this.resourceTypeId);
        return record;
    }

    @Override
    protected void enableSchedules(final AbstractMeasurementScheduleListView measurementScheduleListView,
        final int[] measurementDefinitionIds, final List<String> measurementDefinitionDisplayNames) {
        TemplateSchedulesView templateSchedulesView = (TemplateSchedulesView)measurementScheduleListView;
        boolean updateExistingSchedules = templateSchedulesView.isUpdateExistingSchedules();
        final String s = (measurementDefinitionIds.length > 1) ? "s" : "";
        this.measurementService.enableSchedulesForResourceType(measurementDefinitionIds, updateExistingSchedules,
            new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable throwable) {
                    CoreGUI.getErrorHandler().handleError(
                        "Failed to enable collection of metric" + s + " " + measurementDefinitionDisplayNames
                            + " by default for ResourceType with id [" + resourceTypeId + "].", throwable);
                }

                @Override
                public void onSuccess(Void aVoid) {
                    CoreGUI.getMessageCenter().notify(
                        new Message("Enabled collection of selected metric" + s + ".", "Enabled collection of metric"
                            + s + " " + measurementDefinitionDisplayNames + " by default for ResourceType with id ["
                            + resourceTypeId + "].", Message.Severity.Info));
                    measurementScheduleListView.refresh();
                }
            });
    }

    @Override
    protected void disableSchedules(final AbstractMeasurementScheduleListView measurementScheduleListView,
        int[] measurementDefinitionIds, final List<String> measurementDefinitionDisplayNames) {
        TemplateSchedulesView templateSchedulesView = (TemplateSchedulesView)measurementScheduleListView;
        boolean updateExistingSchedules = templateSchedulesView.isUpdateExistingSchedules();
        final String s = (measurementDefinitionIds.length > 1) ? "s" : "";
        this.measurementService.disableSchedulesForResourceType(measurementDefinitionIds, updateExistingSchedules,
            new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable throwable) {
                    CoreGUI.getErrorHandler().handleError(
                        "Failed to disable collection of metric" + s + " " + measurementDefinitionDisplayNames
                            + " by default for ResourceType with id [" + resourceTypeId + "].", throwable);
                }

                @Override
                public void onSuccess(Void aVoid) {
                    CoreGUI.getMessageCenter().notify(
                        new Message("Disabled collection of selected metric" + s + ".", "Disabled collection of metric"
                            + s + " " + measurementDefinitionDisplayNames + " by default for ResourceType with id ["
                            + resourceTypeId + "].", Message.Severity.Info));
                    measurementScheduleListView.refresh();
                }
            });
    }

    @Override
    protected void updateSchedules(final AbstractMeasurementScheduleListView measurementScheduleListView,
        int[] measurementDefinitionIds, final List<String> measurementDefinitionDisplayNames,
        final long collectionInterval) {
        TemplateSchedulesView templateSchedulesView = (TemplateSchedulesView)measurementScheduleListView;
        boolean updateExistingSchedules = templateSchedulesView.isUpdateExistingSchedules();
        final String s = (measurementDefinitionIds.length > 1) ? "s" : "";
        this.measurementService.updateSchedulesForResourceType(measurementDefinitionIds, collectionInterval,
            updateExistingSchedules, new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable throwable) {
                    CoreGUI.getErrorHandler().handleError(
                        "Failed to set collection interval to " + (collectionInterval / 1000) + " seconds for metric"
                            + s + " " + measurementDefinitionDisplayNames + " by default for ResourceType with id ["
                            + resourceTypeId + "].", throwable);
                }

                @Override
                public void onSuccess(Void aVoid) {
                    CoreGUI.getMessageCenter().notify(
                        new Message("Updated collection intervals of selected metric" + s + ".",
                            "Collection interval for metric" + s + " " + measurementDefinitionDisplayNames
                                + " by default for ResourceType with id [" + resourceTypeId + "] set to "
                                + (collectionInterval / 1000) + " seconds.", Message.Severity.Info));
                    measurementScheduleListView.refresh();
                }
            });
    }
}
