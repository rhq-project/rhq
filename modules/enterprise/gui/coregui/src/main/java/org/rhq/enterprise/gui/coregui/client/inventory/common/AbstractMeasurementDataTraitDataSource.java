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
package org.rhq.enterprise.gui.coregui.client.inventory.common;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.fields.*;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import org.rhq.core.domain.criteria.MeasurementDataTraitCriteria;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.MeasurementDataGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A server-side SmartGWT DataSource for reading {@link MeasurementDataTrait trait data}.
 *
 * @author Ian Springer
 */
public abstract class AbstractMeasurementDataTraitDataSource extends RPCDataSource<MeasurementDataTrait> {
    private MeasurementDataGWTServiceAsync measurementService = GWTServiceLookup.getMeasurementDataService();

    protected AbstractMeasurementDataTraitDataSource() {
        setCanMultiSort(true);
        List<DataSourceField> fields = createFields();
        addFields(fields);
    }

    protected List<DataSourceField> createFields() {
        List<DataSourceField> fields = new ArrayList<DataSourceField>();

        // Note: The fields 

        DataSourceIntegerField idField = new DataSourceIntegerField(MeasurementDataTraitCriteria.SORT_FIELD_SCHEDULE_ID,
                "Id");
        idField.setHidden(true);
        fields.add(idField);

        DataSourceTextField nameField = new DataSourceTextField(MeasurementDataTraitCriteria.SORT_FIELD_NAME,
                "Trait");
        fields.add(nameField);

        // TODO: Include description from metric def?

        DataSourceTextField valueField = new DataSourceTextField(MeasurementDataTraitCriteria.SORT_FIELD_VALUE,
                "Value");
        fields.add(valueField);

        DataSourceIntegerField timestampField = new DataSourceIntegerField(MeasurementDataTraitCriteria.SORT_FIELD_TIMESTAMP,
                "Last Changed");
        fields.add(timestampField);

        return fields;
    }

    protected void executeFetch(final DSRequest request, final DSResponse response) {
        final long startTime = System.currentTimeMillis();

        final MeasurementDataTraitCriteria criteria = getCriteria(request);

        this.measurementService.findTraitsByCriteria(criteria, new AsyncCallback<PageList<MeasurementDataTrait>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to fetch traits for criteria " + criteria,
                        caught);
                response.setStatus(RPCResponse.STATUS_FAILURE);
                processResponse(request.getRequestId(), response);
            }

            public void onSuccess(PageList<MeasurementDataTrait> result) {
                long fetchDuration = System.currentTimeMillis() - startTime;
                System.out.println(result.size() + " traits fetched in: " + fetchDuration + "ms");

                response.setData(buildRecords(result));
                // For paging to work, we have to specify size of full result set.
                response.setTotalRows(result.getTotalSize());
                processResponse(request.getRequestId(), response);
            }
        });
    }

    protected MeasurementDataTraitCriteria getCriteria(DSRequest request) {
        MeasurementDataTraitCriteria criteria = new MeasurementDataTraitCriteria();

        Criteria requestCriteria = request.getCriteria();
        if (requestCriteria != null) {
            Map values = requestCriteria.getValues();
            for (Object key : values.keySet()) {
                String fieldName = (String) key;
                if (fieldName.equals(MeasurementDataTraitCriteria.FILTER_FIELD_RESOURCE_ID)) {
                    Integer resourceId = (Integer) values.get(fieldName);
                    criteria.addFilterResourceId(resourceId);
                } else if (fieldName.equals(MeasurementDataTraitCriteria.FILTER_FIELD_GROUP_ID)) {
                    Integer groupId = (Integer) values.get(fieldName);
                    criteria.addFilterGroupId(groupId);
                }
            }
        }

        criteria.setPageControl(getPageControl(request));
        return criteria;
    }

    @Override
    public MeasurementDataTrait copyValues(ListGridRecord from) {
        return null;
    }

    @Override
    public ListGridRecord copyValues(MeasurementDataTrait from) {
        ListGridRecord record = new ListGridRecord();

        record.setAttribute(MeasurementDataTraitCriteria.SORT_FIELD_SCHEDULE_ID, from.getScheduleId());
        record.setAttribute(MeasurementDataTraitCriteria.SORT_FIELD_TIMESTAMP, from.getTimestamp());
        record.setAttribute(MeasurementDataTraitCriteria.SORT_FIELD_NAME, from.getSchedule().getDefinition().getDisplayName());
        record.setAttribute(MeasurementDataTraitCriteria.SORT_FIELD_VALUE, from.getValue());

        // TODO: resourceId and resourceGroupId (in subclasses)
        
        return record;
    }
}
