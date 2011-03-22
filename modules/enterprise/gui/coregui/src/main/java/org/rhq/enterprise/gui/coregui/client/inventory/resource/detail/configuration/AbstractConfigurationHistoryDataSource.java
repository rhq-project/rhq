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

import org.rhq.core.domain.configuration.AbstractConfigurationUpdate;
import org.rhq.core.domain.configuration.AbstractResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.criteria.AbstractResourceConfigurationUpdateCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.gui.coregui.client.ErrorMessageWindow;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.gwt.ConfigurationGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * A superclass data source that loads information about all the plugin/resource configuration changes that happened
 * for a resource or across all inventory.
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public abstract class AbstractConfigurationHistoryDataSource<T extends AbstractResourceConfigurationUpdate, C extends AbstractResourceConfigurationUpdateCriteria>
    extends RPCDataSource<T, C> {

    public static abstract class Field {
        public static final String ID = "id";
        public static final String CREATED_TIME = "createdTime";
        public static final String STATUS = "status";
        public static final String SUBJECT = "subject";
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

    protected ConfigurationGWTServiceAsync getConfigurationService() {
        return this.configurationService;
    }

    public AbstractConfigurationHistoryDataSource() {
        super();
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    protected String getStatusHtmlString(Record record) {
        String html = null;
        AbstractConfigurationUpdate obj = (AbstractConfigurationUpdate) record.getAttributeAsObject(Field.OBJECT);
        switch (obj.getStatus()) {
        case SUCCESS: {
            html = MSG.view_configurationHistoryList_table_statusSuccess();
            break;
        }
        case INPROGRESS: {
            html = "<p>" + MSG.view_configurationHistoryList_table_statusInprogress() + "</p>";
            break;
        }
        case NOCHANGE: {
            html = MSG.view_configurationHistoryList_table_statusNochange();
            break;
        }
        case FAILURE: {
            html = obj.getErrorMessage();
            if (html == null) {
                html = "<p>" + MSG.view_configurationHistoryList_table_statusFailure() + "</p>";
            } else {
                if (html.length() > 80) {
                    // this was probably an error stack trace, snip it so the tooltip isn't too big
                    html = "<pre>" + html.substring(0, 80) + "...</pre><p>"
                        + MSG.view_configurationHistoryList_table_clickStatusIcon() + "</p>";
                } else {
                    html = "<pre>" + html + "</pre>";
                }
            }
            break;
        }
        }
        return html;
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
                return Canvas.imgHTML(getConfigurationUpdateStatusIcon(status), 16, 16);
            }
        });
        statusField.setShowHover(true);
        statusField.setHoverCustomizer(new HoverCustomizer() {
            @Override
            public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                String html = getStatusHtmlString(record);
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
                return "<a href=\"" + getGroupConfigurationUpdateHistoryLink(groupId, (Number) value) + "\">"
                    + MSG.dataSource_configurationHistory_updateType_group() + "</a>";
            }
        });
        fields.add(updateTypeField);

        // determine the widths of our columns
        if (includeResourceFields) {
            ListGridField resourceNameField = new ListGridField(AncestryUtil.RESOURCE_NAME, MSG.common_title_resource());
            resourceNameField.setCellFormatter(new CellFormatter() {
                public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                    String url = LinkManager
                        .getResourceLink(listGridRecord.getAttributeAsInt(AncestryUtil.RESOURCE_ID));
                    return SeleniumUtility.getLocatableHref(url, o.toString(), null);
                }
            });
            resourceNameField.setShowHover(true);
            resourceNameField.setHoverCustomizer(new HoverCustomizer() {

                public String hoverHTML(Object value, ListGridRecord listGridRecord, int rowNum, int colNum) {
                    return AncestryUtil.getResourceHoverHTML(listGridRecord, 0);
                }
            });
            fields.add(resourceNameField);

            ListGridField ancestryField = AncestryUtil.setupAncestryListGridField();
            fields.add(ancestryField);

            idField.setWidth("10%");
            submittedTimeField.setWidth(150);
            completedTimeField.setWidth(150);
            statusField.setWidth("10%");
            subjectField.setWidth("10%");
            updateTypeField.setWidth("10%");
            resourceNameField.setWidth("30%");
            ancestryField.setWidth("*");
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

    @SuppressWarnings("unchecked")
    @Override
    public T copyValues(Record from) {
        return (T) from.getAttributeAsObject(Field.OBJECT);
    }

    @Override
    public ListGridRecord copyValues(T from) {
        ListGridRecord record = new ListGridRecord();
        record.setAttribute(Field.ID, from.getId());
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
        if (from.getAbstractGroupConfigurationUpdate() != null) {
            record.setAttribute(Field.GROUP_CONFIG_UPDATE_ID, from.getAbstractGroupConfigurationUpdate().getId());
            record.setAttribute(Field.GROUP_ID, from.getAbstractGroupConfigurationUpdate().getGroup().getId()); // note group must be eagerly loaded here
        }

        // for ancestry handling
        Resource resource = from.getResource();
        record.setAttribute(AncestryUtil.RESOURCE_ID, resource.getId());
        record.setAttribute(AncestryUtil.RESOURCE_NAME, resource.getName());
        record.setAttribute(AncestryUtil.RESOURCE_ANCESTRY, resource.getAncestry());
        record.setAttribute(AncestryUtil.RESOURCE_TYPE_ID, resource.getResourceType().getId());

        record.setAttribute(Field.OBJECT, from);
        return record;
    }

    protected abstract String getConfigurationUpdateStatusIcon(ConfigurationUpdateStatus status);

    protected abstract String getGroupConfigurationUpdateHistoryLink(Integer groupId, Number value);
}
