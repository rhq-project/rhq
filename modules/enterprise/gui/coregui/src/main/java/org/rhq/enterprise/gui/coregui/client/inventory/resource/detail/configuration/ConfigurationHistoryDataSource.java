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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.DateDisplayFormat;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RecordClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordClickHandler;

import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.criteria.ResourceConfigurationUpdateCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ErrorMessageWindow;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.gwt.ConfigurationGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * A data source that loads information about all the configuration changes that happened
 * for a resource or across all inventory.
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
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
        public static final String GROUP_CONFIG_UPDATE_ID = "groupConfigUpdateId";
        public static final String GROUP_ID = "groupId"; // will only be non-null if group config update id is non-null
        public static final String DURATION = "duration";
        public static final String ERROR_MESSAGE = "errorMessage";
        public static final String MODIFIED_TIME = "modifiedTime";
        public static final String CURRENT_CONFIG = "currentConfig"; // will be true if the history item represents the current config
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

    /**
     * Views that use this data source can call this method to get a list of fields
     * that can be used in a list grid to show the data for this data source.
     * 
     * @param includeResourceFields if true, the list of fields that are returned will
     *                              include fields to show individual resource data.
     *                              Pass in false if you are only collecting data on a
     *                              single resource, since you don't need every row to 
     *                              show the same data on the same resource.
     * @return fields
     */
    public ArrayList<ListGridField> getListGridFields(boolean includeResourceFields) {
        ArrayList<ListGridField> fields = new ArrayList<ListGridField>();

        ListGridField idField = new ListGridField(Field.ID, MSG.common_title_version());
        idField.setShowHover(true);
        idField.setHoverCustomizer(new HoverCustomizer() {
            @Override
            public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                if (Boolean.parseBoolean(record.getAttribute(Field.CURRENT_CONFIG))) {
                    return MSG.dataSource_configurationHistory_currentConfig();
                }
                return null;
            }
        });
        fields.add(idField);

        ListGridField submittedTimeField = new ListGridField(Field.CREATED_TIME, MSG
            .dataSource_configurationHistory_dateSubmitted());
        submittedTimeField.setType(ListGridFieldType.DATE);
        submittedTimeField.setDateFormatter(DateDisplayFormat.TOLOCALESTRING);
        fields.add(submittedTimeField);

        ListGridField completedTimeField = new ListGridField(Field.MODIFIED_TIME, MSG
            .dataSource_configurationHistory_dateCompleted());
        completedTimeField.setType(ListGridFieldType.DATE);
        completedTimeField.setDateFormatter(DateDisplayFormat.TOLOCALESTRING);
        fields.add(completedTimeField);

        ListGridField statusField = new ListGridField(Field.STATUS, MSG.common_title_status());
        statusField.setAlign(Alignment.CENTER);
        statusField.setCellFormatter(new CellFormatter() {
            public String format(Object value, ListGridRecord listGridRecord, int i, int i1) {
                ConfigurationUpdateStatus status = ConfigurationUpdateStatus.valueOf(value.toString());
                return Canvas.imgHTML(ImageManager.getResourceConfigurationIcon(status), 16, 16);
            }
        });
        statusField.setShowHover(true);
        statusField.setHoverCustomizer(new HoverCustomizer() {
            @Override
            public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                String html = null;
                String err = record.getAttribute(Field.ERROR_MESSAGE);
                if (err != null && err.length() > 0) {
                    html = MSG.dataSource_configurationHistory_clickToSeeError();
                } else {
                    ConfigurationUpdateStatus status = ConfigurationUpdateStatus.valueOf(record
                        .getAttribute(Field.STATUS));
                    switch (status) {
                    case SUCCESS: {
                        html = MSG.common_status_success();
                        break;
                    }
                    case FAILURE: {
                        html = MSG.common_status_failed();
                        break;
                    }
                    case INPROGRESS: {
                        html = MSG.common_status_inprogress();
                        break;
                    }
                    case NOCHANGE: {
                        html = MSG.common_status_success();
                        break;
                    }
                    }
                }
                return html;
            }
        });
        statusField.addRecordClickHandler(new RecordClickHandler() {
            @Override
            public void onRecordClick(RecordClickEvent event) {
                String err = event.getRecord().getAttribute(Field.ERROR_MESSAGE);
                if (err != null && err.length() > 0) {
                    err = "<pre>" + err + "</pre>";
                    new ErrorMessageWindow("errWin", MSG.common_title_error(), err).show();
                }
            }
        });
        fields.add(statusField);

        ListGridField subjectField = new ListGridField(Field.SUBJECT, MSG.common_title_user());
        fields.add(subjectField);

        ListGridField updateTypeField = new ListGridField(Field.GROUP_CONFIG_UPDATE_ID, MSG
            .dataSource_configurationHistory_updateType());
        updateTypeField.setCellFormatter(new CellFormatter() {
            @Override
            public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                if (value == null) {
                    return MSG.dataSource_configurationHistory_updateType_individual();
                }
                Integer groupId = record.getAttributeAsInt(Field.GROUP_ID);
                return "<a href=\""
                    + LinkManager.getGroupResourceConfigurationUpdateHistoryLink(groupId, ((Number) value).intValue())
                    + "\">" + MSG.dataSource_configurationHistory_updateType_group() + "</a>";
            }
        });
        fields.add(updateTypeField);

        // determine the widths of our columns
        if (includeResourceFields) {
            ListGridField resourceField = new ListGridField(Field.RESOURCE, MSG.common_title_resource());
            resourceField.setCellFormatter(new CellFormatter() {
                public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                    if (listGridRecord == null) {
                        return "unknown";
                    }
                    Resource res = (Resource) listGridRecord.getAttributeAsObject(Field.RESOURCE);
                    String url = LinkManager.getResourceLink(res.getId());
                    // TODO disambiguate the resource name
                    return SeleniumUtility.getLocatableHref(url, res.getName(), null);
                }
            });
            fields.add(resourceField);

            idField.setWidth("10%");
            submittedTimeField.setWidth("20%");
            completedTimeField.setWidth("20%");
            statusField.setWidth("10%");
            subjectField.setWidth("10%");
            updateTypeField.setWidth("10%");
            resourceField.setWidth("*");
        } else {
            idField.setWidth("10%");
            submittedTimeField.setWidth("20%");
            completedTimeField.setWidth("20%");
            statusField.setWidth("10%");
            subjectField.setWidth("10%");
            updateTypeField.setWidth("*");
        }

        return fields;
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();
        DataSourceIntegerField idField = new DataSourceIntegerField(Field.ID, MSG.common_title_version());
        idField.setPrimaryKey(true);
        fields.add(idField);
        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response) {

        ResourceConfigurationUpdateCriteria criteria = new ResourceConfigurationUpdateCriteria();
        criteria.fetchConfiguration(true);
        criteria.fetchResource(true);
        criteria.fetchGroupConfigurationUpdate(true);

        criteria.setPageControl(getPageControl(request));

        final Integer resourceId = (Integer) request.getCriteria().getValues().get(CriteriaField.RESOURCE_ID);
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

                public void onSuccess(final PageList<ResourceConfigurationUpdate> result) {
                    final ListGridRecord[] records = buildRecords(result);
                    if (resourceId == null) {
                        response.setData(records);
                        response.setTotalRows(result.getTotalSize());
                        processResponse(request.getRequestId(), response);
                        return; // we can finish now, we don't need any additional information
                    }

                    // we are obtaining a single resource's history items. Let's find out which is
                    // its latest, current config item so we can mark it as such
                    configurationService.getLatestResourceConfigurationUpdate(resourceId.intValue(),
                        new AsyncCallback<ResourceConfigurationUpdate>() {
                            @Override
                            public void onSuccess(ResourceConfigurationUpdate latestResult) {
                                if (latestResult != null) {
                                    for (ListGridRecord record : records) {
                                        boolean latest = record.getAttributeAsInt(Field.ID).intValue() == latestResult
                                            .getId();
                                        record.setAttribute(Field.CURRENT_CONFIG, latest);
                                    }
                                }
                                finish();
                            }

                            @Override
                            public void onFailure(Throwable caught) {
                                // should we show an error message? this just means we can't show any item as the "current" one
                                finish();
                            }

                            private void finish() {
                                response.setData(records);
                                response.setTotalRows(result.getTotalSize());
                                processResponse(request.getRequestId(), response);
                            }
                        });
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
        record.setAttribute(Field.STATUS, from.getStatus().name());
        record.setAttribute(Field.CONFIGURATION, from.getConfiguration());
        record.setAttribute(Field.DURATION, from.getDuration());
        record.setAttribute(Field.ERROR_MESSAGE, from.getErrorMessage());
        record.setAttribute(Field.CREATED_TIME, new Date(from.getCreatedTime()));
        // if it is still in progress, the modified time (which we label as "date completed") is meaningless since it isn't completed yet 
        if (from.getStatus() != ConfigurationUpdateStatus.INPROGRESS) {
            record.setAttribute(Field.MODIFIED_TIME, new Date(from.getModifiedTime()));
        }
        if (from.getGroupConfigurationUpdate() != null) {
            record.setAttribute(Field.GROUP_CONFIG_UPDATE_ID, from.getGroupConfigurationUpdate().getId());
            record.setAttribute(Field.GROUP_ID, from.getGroupConfigurationUpdate().getGroup().getId()); // note group must be eagerly loaded here
        }
        record.setAttribute(Field.OBJECT, from);
        return record;
    }
}
