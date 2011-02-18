/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.configuration;

import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.types.FieldType;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.criteria.ResourceConfigurationUpdateCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.ConfigurationGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Greg Hinkle
 */
public class ConfigurationHistoryDataSource extends RPCDataSource<ResourceConfigurationUpdate> {

    public static abstract class Field {
        public static final String ID = "id";
        public static final String RESOURCE = "resource";
        public static final String CREATED_TIME = "createdTime";
        public static final String STATUS = "status";
        public static final String SUBJECT = "subject";
        public static final String RESOURCE_TYPE_ID = "resourceTypeId";
        public static final String CONFIGURATION = "configuration";
        public static final String DURATION = "duration";
        public static final String ERROR_MESSAGE = "errorMessage";
        public static final String MODIFIED_TIME = "modifiedTime";
        public static final String OBJECT = "object"; // the full entity object is stored in this attribute
    }

    public static abstract class CriteriaField {
        public static final String RESOURCE_ID = "resourceId";
    }

    private ConfigurationGWTServiceAsync configurationService = GWTServiceLookup.getConfigurationService();

    public ConfigurationHistoryDataSource() {
        super();
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        DataSourceIntegerField idField = new DataSourceIntegerField(Field.ID,
            MSG.dataSource_configurationHistory_field_id());
        idField.setPrimaryKey(true);
        fields.add(idField);

        DataSourceTextField resourceField = new DataSourceTextField(Field.RESOURCE,
            MSG.dataSource_configurationHistory_field_resource());
        fields.add(resourceField);

        DataSourceTextField submittedField = new DataSourceTextField(Field.CREATED_TIME,
            MSG.dataSource_configurationHistory_field_createdTime());
        submittedField.setType(FieldType.DATETIME);
        fields.add(submittedField);

        DataSourceTextField statusField = new DataSourceTextField(Field.STATUS,
            MSG.dataSource_configurationHistory_field_status());
        fields.add(statusField);

        DataSourceTextField subjectField = new DataSourceTextField(Field.SUBJECT,
            MSG.dataSource_configurationHistory_field_subject());
        fields.add(subjectField);

        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response) {

        ResourceConfigurationUpdateCriteria criteria = new ResourceConfigurationUpdateCriteria();
        criteria.fetchConfiguration(true);
        criteria.fetchResource(true);

        criteria.setPageControl(getPageControl(request));

        Integer resourceId = (Integer) request.getCriteria().getValues().get(CriteriaField.RESOURCE_ID);
        if (resourceId != null) {
            criteria.addFilterResourceIds(resourceId);
        }

        configurationService.findResourceConfigurationUpdatesByCriteria(criteria,
            new AsyncCallback<PageList<ResourceConfigurationUpdate>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.dataSource_configurationHistory_error_fetchFailure(),
                        caught);
                    response.setStatus(DSResponse.STATUS_FAILURE);
                    processResponse(request.getRequestId(), response);
                }

                public void onSuccess(PageList<ResourceConfigurationUpdate> result) {
                    response.setData(buildRecords(result));
                    response.setTotalRows(result.getTotalSize());
                    processResponse(request.getRequestId(), response);
                }
            });
    }

    @Override
    public ResourceConfigurationUpdate copyValues(Record from) {
        return (ResourceConfigurationUpdate) from.getAttributeAsObject(Field.OBJECT);
    }

    @Override
    public ListGridRecord copyValues(ResourceConfigurationUpdate from) {
        ListGridRecord record = new ListGridRecord();
        record.setAttribute(Field.ID, from.getId());
        record.setAttribute(Field.RESOURCE, from.getResource());
        record.setAttribute(Field.RESOURCE_TYPE_ID, from.getResource().getResourceType().getId());
        record.setAttribute(Field.SUBJECT, from.getSubjectName());
        record.setAttribute(Field.CONFIGURATION, from.getConfiguration());
        record.setAttribute(Field.CREATED_TIME, new Date(from.getCreatedTime()));
        record.setAttribute(Field.DURATION, from.getDuration());
        record.setAttribute(Field.ERROR_MESSAGE, from.getErrorMessage());
        record.setAttribute(Field.MODIFIED_TIME, new Date(from.getModifiedTime()));
        record.setAttribute(Field.STATUS, from.getStatus().name());
        record.setAttribute(Field.OBJECT, from);
        return record;
    }
}
