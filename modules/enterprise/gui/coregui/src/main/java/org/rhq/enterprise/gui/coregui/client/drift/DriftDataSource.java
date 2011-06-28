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
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.DriftCriteria;
import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftCategory;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.enterprise.gui.coregui.client.gwt.DriftGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository.TypesLoadedCallback;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * @author Jay Shaughnessy
 */
public class DriftDataSource extends RPCDataSource<Drift, DriftCriteria> {

    public static final String FILTER_CATEGORIES = "categories";

    private DriftGWTServiceAsync driftService = GWTServiceLookup.getDriftService();

    private EntityContext entityContext;

    public DriftDataSource() {
        this(EntityContext.forSubsystemView());
    }

    public DriftDataSource(EntityContext context) {
        super();
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

        ListGridField ctimeField = new ListGridField("ctime", MSG.common_title_createTime());
        ctimeField.setCellFormatter(new TimestampCellFormatter());
        ctimeField.setShowHover(true);
        ctimeField.setHoverCustomizer(TimestampCellFormatter.getHoverCustomizer("ctime"));
        fields.add(ctimeField);

        ListGridField categoryField = new ListGridField("category", MSG.common_title_category());
        fields.add(categoryField);

        ListGridField pathField = new ListGridField("path", MSG.common_title_path());
        fields.add(pathField);

        if (this.entityContext.type != EntityContext.Type.Resource) {
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

            ctimeField.setWidth(100);
            categoryField.setWidth(100);
            pathField.setWidth("35%");
            resourceNameField.setWidth("25%");
            ancestryField.setWidth("40%");

        } else {
            ctimeField.setWidth(200);
            pathField.setWidth("*");
            categoryField.setWidth(100);
        }

        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response, final DriftCriteria criteria) {
        if (criteria == null) {
            // the user selected no categories in the filter - it makes sense from the UI perspective to show 0 rows
            response.setTotalRows(0);
            processResponse(request.getRequestId(), response);
            return;
        }

        final long start = System.currentTimeMillis();

        this.driftService.findDriftsByCriteria(criteria, new AsyncCallback<PageList<Drift>>() {

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_drift_failure_load(), caught);
                response.setStatus(RPCResponse.STATUS_FAILURE);
                processResponse(request.getRequestId(), response);
            }

            public void onSuccess(PageList<Drift> result) {
                if (Log.isDebugEnabled()) {
                    long fetchTime = System.currentTimeMillis() - start;
                    Log.debug(result.size() + " drifts fetched in: " + fetchTime + "ms");
                }

                dataRetrieved(result, response, request);
            }
        });
    }

    /**
     * Additional processing to support entity-specific or cross-resource views, and something that can be overidden.
     */
    protected void dataRetrieved(final PageList<Drift> result, final DSResponse response, final DSRequest request) {
        switch (entityContext.type) {

        // no need to disambiguate, the drifts are for a single resource
        case Resource:
            response.setData(buildRecords(result));
            // for paging to work we have to specify size of full result set
            response.setTotalRows(getTotalRows(result, response, request));
            processResponse(request.getRequestId(), response);
            break;

        // disambiguate as the results could be cross-resource
        default:
            Set<Integer> typesSet = new HashSet<Integer>();
            Set<String> ancestries = new HashSet<String>();
            for (Drift drift : result) {
                Resource resource = drift.getChangeSet().getResource();
                typesSet.add(resource.getResourceType().getId());
                ancestries.add(resource.getAncestry());
            }

            // In addition to the types of the result resources, get the types of their ancestry
            typesSet.addAll(AncestryUtil.getAncestryTypeIds(ancestries));

            ResourceTypeRepository typeRepo = ResourceTypeRepository.Cache.getInstance();
            typeRepo.getResourceTypes(typesSet.toArray(new Integer[typesSet.size()]), new TypesLoadedCallback() {
                @Override
                public void onTypesLoaded(Map<Integer, ResourceType> types) {
                    // Smartgwt has issues storing a Map as a ListGridRecord attribute. Wrap it in a pojo.                
                    AncestryUtil.MapWrapper typesWrapper = new AncestryUtil.MapWrapper(types);

                    Record[] records = buildRecords(result);
                    for (Record record : records) {
                        // To avoid a lot of unnecessary String construction, be lazy about building ancestry hover text.
                        // Store the types map off the records so we can build a detailed hover string as needed.                      
                        record.setAttribute(AncestryUtil.RESOURCE_ANCESTRY_TYPES, typesWrapper);

                        // Build the decoded ancestry Strings now for display
                        record
                            .setAttribute(AncestryUtil.RESOURCE_ANCESTRY_VALUE, AncestryUtil.getAncestryValue(record));
                    }
                    response.setData(records);
                    // for paging to work we have to specify size of full result set
                    response.setTotalRows(getTotalRows(result, response, request));
                    processResponse(request.getRequestId(), response);
                }
            });
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
     * @return should not exceed result.getTotalSize(). 
     */
    protected int getTotalRows(final PageList<Drift> result, final DSResponse response, final DSRequest request) {

        return result.getTotalSize();
    }

    @Override
    protected DriftCriteria getFetchCriteria(DSRequest request) {
        DriftCategory[] categoriesFilter = getArrayFilter(request, FILTER_CATEGORIES, DriftCategory.class);

        if (categoriesFilter == null || categoriesFilter.length == 0) {
            return null; // user didn't select any priorities - return null to indicate no data should be displayed
        }

        DriftCriteria criteria = new DriftCriteria();
        criteria.addFilterCategories(categoriesFilter);

        switch (entityContext.getType()) {
        case Resource:
            criteria.addFilterResourceIds(entityContext.getResourceId());
            break;

        case ResourceGroup:
            // TODO ?

        default:
            // no filter
        }

        criteria.fetchChangeSet(true);
        criteria.setPageControl(getPageControl(request));

        return criteria;
    }

    @Override
    protected String getSortFieldForColumn(String columnName) {
        if (AncestryUtil.RESOURCE_ANCESTRY.equals(columnName)) {
            return "changeSet.resource.ancestry";
        }

        return super.getSortFieldForColumn(columnName);
    }

    @Override
    public Drift copyValues(Record from) {
        return null;
    }

    @Override
    public ListGridRecord copyValues(Drift from) {
        return convert(from);
    }

    public static ListGridRecord convert(Drift from) {
        ListGridRecord record = new ListGridRecord();
        record.setAttribute("id", from.getId());
        record.setAttribute("ctime", new Date(from.getCtime()));
        record.setAttribute("category", from.getCategory().name());
        record.setAttribute("path", from.getPath());

        DriftChangeSet changeSet = from.getChangeSet();
        Resource resource = changeSet.getResource();

        // for ancestry handling       
        record.setAttribute(AncestryUtil.RESOURCE_ID, resource.getId());
        record.setAttribute(AncestryUtil.RESOURCE_NAME, resource.getName());
        record.setAttribute(AncestryUtil.RESOURCE_ANCESTRY, resource.getAncestry());
        record.setAttribute(AncestryUtil.RESOURCE_TYPE_ID, resource.getResourceType().getId());

        return record;
    }

    protected void executeRemove(Record recordToRemove, final DSRequest request, final DSResponse response) {
        Window.alert(String.valueOf(recordToRemove.getAttributeAsInt("id")));
    }

    public DriftGWTServiceAsync getDriftService() {
        return driftService;
    }

    protected EntityContext getEntityContext() {
        return entityContext;
    }

    protected void setEntityContext(EntityContext entityContext) {
        this.entityContext = entityContext;
    }

}