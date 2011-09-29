/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.drift;

import java.util.ArrayList;
import java.util.Collection;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.DriftConfigurationCriteria;
import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.core.domain.drift.DriftConfiguration.BaseDirectory;
import org.rhq.core.domain.drift.DriftConfigurationDefinition.DriftHandlingMode;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.gwt.DriftGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * @author Jay Shaughnessy
 * @author John Mazzitelli
 */
public class DriftConfigurationDataSource extends RPCDataSource<DriftConfiguration, DriftConfigurationCriteria> {

    public static final String ATTR_ENTITY = "object";
    public static final String ATTR_ID = "id";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_INTERVAL = "interval";
    public static final String ATTR_DRIFT_HANDLING_MODE = "driftHandlingMode";
    public static final String ATTR_BASE_DIR_STRING = "baseDirString";
    public static final String ATTR_ENABLED = "enabled";

    public static final String DRIFT_HANDLING_MODE_NORMAL = MSG.view_drift_table_driftHandlingMode_normal();
    public static final String DRIFT_HANDLING_MODE_PLANNED = MSG.view_drift_table_driftHandlingMode_plannedChanges();

    private DriftGWTServiceAsync driftService = GWTServiceLookup.getDriftService();
    private EntityContext entityContext;

    public DriftConfigurationDataSource() {
        this(EntityContext.forSubsystemView());
    }

    public DriftConfigurationDataSource(EntityContext context) {
        this.entityContext = context;
        addDataSourceFields();
    }

    /**
     * The view that contains the list grid which will display this datasource's data will call this
     * method to get the field information which is used to control the display of the data.
     * 
     * @return list grid fields used to display the datasource data
     */
    public ArrayList<ListGridField> getListGridFields() {
        ArrayList<ListGridField> fields = new ArrayList<ListGridField>(6);

        ListGridField nameField = new ListGridField(ATTR_NAME, MSG.common_title_name());
        fields.add(nameField);

        ListGridField enabledField = new ListGridField(ATTR_ENABLED, MSG.common_title_enabled());
        enabledField.setType(ListGridFieldType.IMAGE);
        enabledField.setAlign(Alignment.CENTER);
        fields.add(enabledField);

        ListGridField driftHandlingModeField = new ListGridField(ATTR_DRIFT_HANDLING_MODE, MSG
            .view_drift_table_driftHandlingMode());
        fields.add(driftHandlingModeField);

        ListGridField intervalField = new ListGridField(ATTR_INTERVAL, MSG.common_title_interval());
        fields.add(intervalField);

        ListGridField baseDirField = new ListGridField(ATTR_BASE_DIR_STRING, MSG.view_drift_table_baseDir());
        // can't sort on this because it's not an entity field, it's derived from the config only
        baseDirField.setCanSort(false);
        fields.add(baseDirField);

        if (this.entityContext.type != EntityContext.Type.Resource) {
            ListGridField resourceNameField = new ListGridField(AncestryUtil.RESOURCE_NAME, MSG.common_title_resource());
            resourceNameField.setCellFormatter(new CellFormatter() {
                public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                    Integer resourceId = listGridRecord.getAttributeAsInt(AncestryUtil.RESOURCE_ID);
                    String url = LinkManager.getResourceLink(resourceId);
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

            nameField.setWidth("20%");
            enabledField.setWidth(60);
            driftHandlingModeField.setWidth("10%");
            intervalField.setWidth(100);
            baseDirField.setWidth("*");
            resourceNameField.setWidth("20%");
            ancestryField.setWidth("40%");
        } else {
            nameField.setWidth("20%");
            enabledField.setWidth(60);
            driftHandlingModeField.setWidth("10%");
            intervalField.setWidth(100);
            baseDirField.setWidth("*");
        }

        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response,
        final DriftConfigurationCriteria criteria) {
        this.driftService.findDriftConfigurationsByCriteria(criteria,
            new AsyncCallback<PageList<DriftConfiguration>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_drift_failure_load(), caught);
                    response.setStatus(RPCResponse.STATUS_FAILURE);
                    processResponse(request.getRequestId(), response);
                }

                public void onSuccess(PageList<DriftConfiguration> result) {
                    dataRetrieved(result, response, request);
                }
            });
    }

    /**
     * Additional processing to support entity-specific or cross-resource views, and something that can be overidden.
     */
    protected void dataRetrieved(final PageList<DriftConfiguration> result, final DSResponse response,
        final DSRequest request) {
        switch (entityContext.type) {

        // no need to disambiguate, the drift configs are for a single resource
        case Resource:
            response.setData(buildRecords(result));
            // for paging to work we have to specify size of full result set
            response.setTotalRows(getTotalRows(result, response, request));
            processResponse(request.getRequestId(), response);
            break;

        case ResourceGroup:
            //TODO

        default:
            throw new IllegalArgumentException("Unsupported Context Type: " + entityContext);
        }
    }

    /**
     * Sub-classes can override this to add fine-grained control over the result set size. By default the
     * total rows are set to the total result set for the query, allowing proper paging.  But some views (portlets)
     * may want to limit results to a small set (like most recent).  
     * @param result
     * @param response
     * @param request
     * 
     * @return should not exceed result.size(). 
     */
    protected int getTotalRows(final Collection<DriftConfiguration> result, final DSResponse response,
        final DSRequest request) {
        return result.size();
    }

    @Override
    protected DriftConfigurationCriteria getFetchCriteria(DSRequest request) {

        DriftConfigurationCriteria criteria = new DriftConfigurationCriteria();
        switch (entityContext.getType()) {
        case Resource:
            criteria.addFilterResourceIds(entityContext.getResourceId());
            break;

        case ResourceGroup:
            //TODO

        default:
            // no filter
        }

        criteria.fetchConfiguration(true);
        criteria.setPageControl(getPageControl(request));

        return criteria;
    }

    /*
    @Override
    protected String getSortFieldForColumn(String columnName) {
        if (AncestryUtil.RESOURCE_ANCESTRY.equals(columnName)) {
            return "ancestry";
        }

        return super.getSortFieldForColumn(columnName);
    }
    */

    @Override
    public DriftConfiguration copyValues(Record from) {
        return (DriftConfiguration) from.getAttributeAsObject(ATTR_ENTITY);
    }

    @Override
    public ListGridRecord copyValues(DriftConfiguration from) {
        return convert(from);
    }

    public static ListGridRecord convert(DriftConfiguration from) {
        ListGridRecord record = new ListGridRecord();

        record.setAttribute(ATTR_ENTITY, from);

        record.setAttribute(ATTR_ID, from.getId());
        record.setAttribute(ATTR_NAME, from.getName());
        record.setAttribute(ATTR_DRIFT_HANDLING_MODE, getDriftHandlingModeDisplayName(from.getDriftHandlingMode()));
        record.setAttribute(ATTR_INTERVAL, String.valueOf(from.getInterval()));
        record.setAttribute(ATTR_BASE_DIR_STRING, getBaseDirString(from.getBasedir()));
        record.setAttribute(ATTR_ENABLED, ImageManager.getAvailabilityIcon(from.isEnabled()));

        // // for ancestry handling       
        // Resource resource = ...
        // record.setAttribute(AncestryUtil.RESOURCE_ID, resource.getId());
        // record.setAttribute(AncestryUtil.RESOURCE_NAME, resource.getName());
        // record.setAttribute(AncestryUtil.RESOURCE_ANCESTRY, resource.getAncestry());
        // record.setAttribute(AncestryUtil.RESOURCE_TYPE_ID, resource.getResourceType().getId());

        return record;
    }

    public static String getDriftHandlingModeDisplayName(DriftHandlingMode driftHandlingMode) {
        switch (driftHandlingMode) {
        case plannedChanges:
            return DRIFT_HANDLING_MODE_PLANNED;

        default:
            return DRIFT_HANDLING_MODE_NORMAL;
        }
    }

    private static String getBaseDirString(BaseDirectory basedir) {
        return basedir.getValueContext() + ":" + basedir.getValueName();
    }

    protected EntityContext getEntityContext() {
        return entityContext;
    }

    protected void setEntityContext(EntityContext entityContext) {
        this.entityContext = entityContext;
    }

}