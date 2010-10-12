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

import java.util.List;

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
    protected List<DataSourceField> createFields() {
        List<DataSourceField> fields = super.createFields();
        DataSourceField resourceGroupIdField = new DataSourceIntegerField(MeasurementScheduleCriteria.FILTER_FIELD_RESOURCE_GROUP_ID,
                "Resource Group Id");
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
    protected void enableSchedules(AbstractMeasurementScheduleListView measurementScheduleListView,
                                   final int[] measurementDefinitionIds, final List<String> measurementDefinitionDisplayNames) {
        final String s = (measurementDefinitionIds.length > 1) ? "s" : "";
        this.measurementService.enableSchedulesForCompatibleGroup(this.resourceGroupId, measurementDefinitionIds,
             new AsyncCallback<Void>() {
                 @Override
                 public void onFailure(Throwable throwable) {
                     CoreGUI.getErrorHandler().handleError("Failed to enable collection of metric" + s + " " 
                             + measurementDefinitionDisplayNames + " for Resource group with id [" + resourceGroupId + "].",
                             throwable);
                 }

                 @Override
                 public void onSuccess(Void aVoid) {
                     CoreGUI.getMessageCenter().notify(new Message("Enabled collection of selected metric" + s + ".",
                         "Enabled collection of metric" + s + " "
                             + measurementDefinitionDisplayNames + " for Resource group with id [" + resourceGroupId +
                                             "].", Message.Severity.Info));

                 }
         });
    }

    @Override
    protected void disableSchedules(AbstractMeasurementScheduleListView measurementScheduleListView, int[] measurementDefinitionIds, final List<String> measurementDefinitionDisplayNames) {
        final String s = (measurementDefinitionIds.length > 1) ? "s" : "";
        this.measurementService.disableSchedulesForCompatibleGroup(this.resourceGroupId, measurementDefinitionIds,
             new AsyncCallback<Void>() {
             @Override
             public void onFailure(Throwable throwable) {
                 CoreGUI.getErrorHandler().handleError("Failed to disable collection of metric" + s + " "
                         + measurementDefinitionDisplayNames + " for Resource group with id [" + resourceGroupId + "].",
                         throwable);
             }

             @Override
             public void onSuccess(Void aVoid) {
                 CoreGUI.getMessageCenter().notify(new Message("Disabled collection of selected metric" + s + ".",
                     "Disabled collection of metric" + s + " "
                         + measurementDefinitionDisplayNames + " for Resource group with id [" + resourceGroupId +
                                         "].", Message.Severity.Info));

             }
         });
    }

    @Override
    protected void updateSchedules(AbstractMeasurementScheduleListView measurementScheduleListView,
                                   int[] measurementDefinitionIds, final List<String> measurementDefinitionDisplayNames, final long collectionInterval) {
        final String s = (measurementDefinitionIds.length > 1) ? "s" : "";
        this.measurementService.updateSchedulesForCompatibleGroup(this.resourceGroupId, measurementDefinitionIds, collectionInterval,
            new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable throwable) {
                CoreGUI.getErrorHandler().handleError("Failed to set collection interval to " + (collectionInterval / 1000)
                        + " seconds for metric" + s + " " + measurementDefinitionDisplayNames + " for Resource group with id ["
                        + resourceGroupId + "].",
                        throwable);
            }

            @Override
            public void onSuccess(Void aVoid) {
                CoreGUI.getMessageCenter().notify(new Message("Updated collection intervals of selected metric" + s + ".",
                    "Collection interval for metric" + s + " "
                        + measurementDefinitionDisplayNames + " for Resource group with id [" + resourceGroupId +
                                        "] set to " + (collectionInterval / 1000) + " seconds.", Message.Severity.Info));

            }
        });
    }
}
