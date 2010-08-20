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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.schedules;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import org.rhq.core.domain.criteria.MeasurementScheduleCriteria;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.MeasurementDataGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMeasurementScheduleDataSource;
import org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMeasurementScheduleListView;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

import java.util.List;

/**
 * A DataSource for reading and updating the metric schedules for the current Resource.
 *
 * @author Ian Springer
 */
public class SchedulesDataSource extends AbstractMeasurementScheduleDataSource {
    private MeasurementDataGWTServiceAsync measurementService = GWTServiceLookup.getMeasurementDataService();
    private int resourceId;

    public SchedulesDataSource(int resourceId) {
        this.resourceId = resourceId;
    }

    @Override
    protected List<DataSourceField> createFields() {
        List<DataSourceField> fields = super.createFields();
        DataSourceField resourceIdField = new DataSourceIntegerField(MeasurementScheduleCriteria.FILTER_FIELD_RESOURCE_ID,
                "Resource Id");
        resourceIdField.setHidden(true);
        fields.add(resourceIdField);
        return fields;
    }

    @Override
    public ListGridRecord copyValues(MeasurementSchedule from) {
        ListGridRecord record = super.copyValues(from);
        record.setAttribute(MeasurementScheduleCriteria.FILTER_FIELD_RESOURCE_ID, this.resourceId);
        return record;
    }

    @Override
    protected void enableSchedules(AbstractMeasurementScheduleListView measurementScheduleListView,
                                   int[] measurementDefinitionIds, final List<String> measurementDefinitionDisplayNames) {
         this.measurementService.enableSchedulesForResource(this.resourceId, measurementDefinitionIds,
             new AsyncCallback<Void>() {
                 @Override
                 public void onFailure(Throwable throwable) {
                     CoreGUI.getErrorHandler().handleError("Failed to enable collection of metrics "
                             + measurementDefinitionDisplayNames + " for Resource with id [" + resourceId + "].",
                             throwable);
                 }

                 @Override
                 public void onSuccess(Void aVoid) {
                     CoreGUI.getMessageCenter().notify(new Message("Enabled collection of metrics "
                             + measurementDefinitionDisplayNames + " for Resource with id [" + resourceId +
                                             "].", Message.Severity.Info));

                 }
         });
    }

    @Override
    protected void disableSchedules(AbstractMeasurementScheduleListView measurementScheduleListView, int[] measurementDefinitionIds, final List<String> measurementDefinitionDisplayNames) {
         this.measurementService.disableSchedulesForResource(this.resourceId, measurementDefinitionIds,
             new AsyncCallback<Void>() {
                 @Override
                 public void onFailure(Throwable throwable) {
                     CoreGUI.getErrorHandler().handleError("Failed to disable collection of metrics "
                             + measurementDefinitionDisplayNames + " for Resource with id [" + resourceId + "].",
                             throwable);
                 }

                 @Override
                 public void onSuccess(Void aVoid) {
                     CoreGUI.getMessageCenter().notify(new Message("Disabled collection of metrics "
                             + measurementDefinitionDisplayNames + " for Resource with id [" + resourceId +
                                             "].", Message.Severity.Info));

                 }
         });
    }

    @Override
    protected void updateSchedules(AbstractMeasurementScheduleListView measurementScheduleListView,
                                   int[] measurementDefinitionIds, final List<String> measurementDefinitionDisplayNames, final long collectionInterval) {
        this.measurementService.updateSchedulesForResource(this.resourceId, measurementDefinitionIds, collectionInterval,
            new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable throwable) {
                CoreGUI.getErrorHandler().handleError("Failed to set collection interval to " + (collectionInterval / 1000)
                        + " seconds for metrics " + measurementDefinitionDisplayNames + " for Resource with id ["
                        + resourceId + "].",
                        throwable);
            }

            @Override
            public void onSuccess(Void aVoid) {
                CoreGUI.getMessageCenter().notify(new Message("Collection interval for metrics "
                        + measurementDefinitionDisplayNames + " for Resource with id [" + resourceId +
                                        "] set to " + (collectionInterval / 1000) + " seconds.", Message.Severity.Info));

            }
        });
    }
}
