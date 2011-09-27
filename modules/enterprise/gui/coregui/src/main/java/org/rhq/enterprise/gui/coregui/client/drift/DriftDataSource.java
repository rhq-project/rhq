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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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
import org.rhq.core.domain.criteria.GenericDriftCriteria;
import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftCategory;
import org.rhq.core.domain.drift.DriftComposite;
import org.rhq.core.domain.drift.DriftConfigurationDefinition.DriftHandlingMode;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
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
 * @author John Mazzitelli
 */
public class DriftDataSource extends RPCDataSource<DriftComposite, GenericDriftCriteria> {

    public static final String CATEGORY_ICON_NEW = ImageManager.getDriftCategoryIcon(null);
    public static final String CATEGORY_ICON_ADD = ImageManager.getDriftCategoryIcon(DriftCategory.FILE_ADDED);
    public static final String CATEGORY_ICON_CHANGE = ImageManager.getDriftCategoryIcon(DriftCategory.FILE_CHANGED);
    public static final String CATEGORY_ICON_REMOVE = ImageManager.getDriftCategoryIcon(DriftCategory.FILE_REMOVED);

    public static final String ATTR_ID = "id";
    public static final String ATTR_CTIME = "ctime";
    public static final String ATTR_CATEGORY = "category";
    public static final String ATTR_CHANGESET_VERSION = "changeSetVersion";
    public static final String ATTR_CHANGESET_CONFIG = "changSetConfig";
    public static final String ATTR_PATH = "path";

    public static final String FILTER_CATEGORIES = "categories";
    public static final String FILTER_DEFINITION = "definition";
    public static final String FILTER_PATH = "path";
    public static final String FILTER_SNAPSHOT = "snapshot";

    private DriftGWTServiceAsync driftService = GWTServiceLookup.getDriftService();

    private EntityContext entityContext;

    public DriftDataSource() {
        this(EntityContext.forSubsystemView());
    }

    public DriftDataSource(EntityContext context) {
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
        ArrayList<ListGridField> fields = new ArrayList<ListGridField>(7);

        ListGridField ctimeField = new ListGridField(ATTR_CTIME, MSG.common_title_createTime());
        ctimeField.setCellFormatter(new TimestampCellFormatter());
        ctimeField.setShowHover(true);
        ctimeField.setHoverCustomizer(TimestampCellFormatter.getHoverCustomizer(ATTR_CTIME));
        fields.add(ctimeField);

        ListGridField changeSetConfigField = new ListGridField(ATTR_CHANGESET_CONFIG, MSG.common_title_definition());
        fields.add(changeSetConfigField);

        ListGridField changeSetVersionField = new ListGridField(ATTR_CHANGESET_VERSION, MSG.view_drift_table_snapshot());
        fields.add(changeSetVersionField);

        ListGridField categoryField = new ListGridField(ATTR_CATEGORY, MSG.common_title_category());
        categoryField.setType(ListGridFieldType.IMAGE);
        categoryField.setAlign(Alignment.CENTER);
        categoryField.setShowHover(true);
        categoryField.setHoverCustomizer(new HoverCustomizer() {
            @Override
            public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                String cat = record.getAttribute(ATTR_CATEGORY);
                if (CATEGORY_ICON_ADD.equals(cat)) {
                    return MSG.view_drift_category_fileAdded();
                } else if (CATEGORY_ICON_CHANGE.equals(cat)) {
                    return MSG.view_drift_category_fileChanged();
                } else if (CATEGORY_ICON_REMOVE.equals(cat)) {
                    return MSG.view_drift_category_fileRemoved();
                } else if (CATEGORY_ICON_NEW.equals(cat)) {
                    return MSG.view_drift_category_fileNew();
                } else {
                    return ""; // will never get here
                }
            }
        });
        fields.add(categoryField);

        ListGridField pathField = new ListGridField(ATTR_PATH, MSG.common_title_path());
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
            changeSetVersionField.setWidth(100);
            categoryField.setWidth(100);
            pathField.setWidth("*");
            resourceNameField.setWidth("25%");
            ancestryField.setWidth("40%");

        } else {
            ctimeField.setWidth(200);
            changeSetVersionField.setWidth(100);
            categoryField.setWidth(100);
            pathField.setWidth("*");
        }

        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response, final GenericDriftCriteria criteria) {
        if (criteria == null) {
            // the user selected no categories in the filter - it makes sense from the UI perspective to show 0 rows
            response.setTotalRows(0);
            processResponse(request.getRequestId(), response);
            return;
        }

        this.driftService.findDriftCompositesByCriteria(criteria, new AsyncCallback<PageList<DriftComposite>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_drift_failure_load(), caught);
                response.setStatus(RPCResponse.STATUS_FAILURE);
                processResponse(request.getRequestId(), response);
            }

            public void onSuccess(PageList<DriftComposite> result) {
                // only get the desired config names (substring match)
                // note - this does not alter the PageList row count, which, I think, makes this
                //        ok without messing up paging.
                String configFilter = getFilter(request, FILTER_DEFINITION, String.class);
                if (null != configFilter && !configFilter.isEmpty()) {
                    configFilter = configFilter.toLowerCase();
                    for (Iterator<DriftComposite> i = result.getValues().iterator(); i.hasNext();) {
                        DriftComposite composite = i.next();
                        if (!composite.getDriftConfigName().toLowerCase().contains(configFilter)) {
                            i.remove();
                        }
                    }
                }

                dataRetrieved(result, response, request);
            }
        });
    }

    /**
     * Additional processing to support entity-specific or cross-resource views, and something that can be overidden.
     */
    protected void dataRetrieved(final PageList<DriftComposite> result, final DSResponse response,
        final DSRequest request) {
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
            for (DriftComposite driftComposite : result) {
                Resource resource = driftComposite.getResource();
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
     * 
     * @return should not exceed result.getTotalSize(). 
     */
    protected int getTotalRows(final PageList<DriftComposite> result, final DSResponse response, final DSRequest request) {
        return result.getTotalSize();
    }

    @Override
    protected GenericDriftCriteria getFetchCriteria(DSRequest request) {
        DriftCategory[] categoriesFilter = getArrayFilter(request, FILTER_CATEGORIES, DriftCategory.class);

        if (categoriesFilter == null || categoriesFilter.length == 0) {
            return null; // user didn't select any priorities - return null to indicate no data should be displayed
        }

        String changeSetFilter = getFilter(request, FILTER_SNAPSHOT, String.class);
        String pathFilter = getFilter(request, FILTER_PATH, String.class);
        // note, this criteria does not allow for query-time config name filtering. That filter is applied lazily
        // to the query results.

        GenericDriftCriteria criteria = new GenericDriftCriteria();
        // grab the change set for the drift
        criteria.fetchChangeSet(true);

        // only get the desired drift categories
        criteria.addFilterCategories(categoriesFilter);

        // only get the desired changeset version (substring match)
        if (null != changeSetFilter && !changeSetFilter.isEmpty()) {
            try {
                Integer version = Integer.valueOf(changeSetFilter);
                criteria.addFilterChangeSetStartVersion(version);
                criteria.addFilterChangeSetEndVersion(version);
            } catch (Exception e) {
                // ignore the specified filter, it's an invalid integer
                // do not fetch tracking entries from the coverage changeset
                criteria.addFilterChangeSetStartVersion(1);
            }
        } else {
            // do not fetch tracking entries from the coverage changeset 
            criteria.addFilterChangeSetStartVersion(1);
        }

        // only get the desired paths (substring match)
        if (null != pathFilter && !pathFilter.isEmpty()) {
            criteria.addFilterPath(pathFilter);
        }

        // do not get planned drifts
        criteria.addFilterDriftHandlingModes(DriftHandlingMode.normal);

        switch (entityContext.getType()) {
        case Resource:
            criteria.addFilterResourceIds(entityContext.getResourceId());
            break;
        case ResourceGroup:
            // TODO ?
            break;
        default:
            // no filter
        }

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
    public DriftComposite copyValues(Record from) {
        return null; // if we need to later, just have convert() put the 'from' drift object in an attribute on the record
    }

    @Override
    public ListGridRecord copyValues(DriftComposite from) {
        return convert(from);
    }

    public static ListGridRecord convert(DriftComposite from) {
        ListGridRecord record = new ListGridRecord();
        Drift<?, ?> drift = from.getDrift();
        record.setAttribute(ATTR_ID, drift.getId());
        record.setAttribute(ATTR_CTIME, new Date(drift.getCtime()));
        switch (drift.getChangeSet().getCategory()) {
        case COVERAGE:
            record.setAttribute(ATTR_CATEGORY, ImageManager.getDriftCategoryIcon(null));
            break;
        case DRIFT:
            record.setAttribute(ATTR_CATEGORY, ImageManager.getDriftCategoryIcon(drift.getCategory()));
            break;
        }
        record.setAttribute(ATTR_PATH, drift.getPath());
        record.setAttribute(ATTR_CHANGESET_CONFIG, from.getDriftConfigName());
        record.setAttribute(ATTR_CHANGESET_VERSION, drift.getChangeSet().getVersion());

        // for ancestry handling     
        Resource resource = from.getResource();
        if (resource != null) {
            record.setAttribute(AncestryUtil.RESOURCE_ID, resource.getId());
            record.setAttribute(AncestryUtil.RESOURCE_NAME, resource.getName());
            record.setAttribute(AncestryUtil.RESOURCE_ANCESTRY, resource.getAncestry());
            record.setAttribute(AncestryUtil.RESOURCE_TYPE_ID, resource.getResourceType().getId());
        }

        return record;
    }

    protected DriftGWTServiceAsync getDriftService() {
        return driftService;
    }

    protected EntityContext getEntityContext() {
        return entityContext;
    }

    protected void setEntityContext(EntityContext entityContext) {
        this.entityContext = entityContext;
    }

}