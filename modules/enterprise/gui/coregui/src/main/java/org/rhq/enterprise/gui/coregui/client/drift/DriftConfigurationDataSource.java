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
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository.TypesLoadedCallback;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * @author Jay Shaughnessy
 */
public class DriftConfigurationDataSource extends RPCDataSource<DriftConfiguration, ResourceCriteria> {

    public static final String FILTER_CATEGORIES = "categories";

    public static final String ATTR_ENTITY = "Entity";

    private ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService();

    private EntityContext entityContext;

    public DriftConfigurationDataSource() {
        this(EntityContext.forSubsystemView());
    }

    public DriftConfigurationDataSource(EntityContext context) {
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

        ListGridField nameField = new ListGridField("name", MSG.common_title_name());
        fields.add(nameField);

        ListGridField intervalField = new ListGridField("interval", MSG.common_title_interval());
        fields.add(intervalField);

        ListGridField baseDirField = new ListGridField("baseDir", MSG.view_drift_table_baseDir());
        fields.add(baseDirField);

        ListGridField enabledField = new ListGridField("enabled", MSG.common_title_enabled());
        fields.add(enabledField);

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

            nameField.setWidth("15%");
            intervalField.setWidth(100);
            enabledField.setWidth("5%");
            baseDirField.setWidth("20%");
            resourceNameField.setWidth("20%");
            ancestryField.setWidth("40%");
        } else {
            nameField.setWidth("15%");
            intervalField.setWidth(100);
            enabledField.setWidth("5%");
            baseDirField.setWidth("80%");
        }

        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response, final ResourceCriteria criteria) {

        final long start = System.currentTimeMillis();

        this.resourceService.findResourcesByCriteria(criteria, new AsyncCallback<PageList<Resource>>() {

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_inventory_resources_loadFailed(), caught);
                response.setStatus(RPCResponse.STATUS_FAILURE);
                processResponse(request.getRequestId(), response);
            }

            public void onSuccess(PageList<Resource> result) {
                if (Log.isDebugEnabled()) {
                    long fetchTime = System.currentTimeMillis() - start;
                    Log.debug(result.size() + " resources (with drift configs) fetched in: " + fetchTime + "ms");
                }

                dataRetrieved(result, response, request);
            }
        });
    }

    /**
     * Additional processing to support entity-specific or cross-resource views, and something that can be overidden.
     */
    protected void dataRetrieved(final PageList<Resource> result, final DSResponse response, final DSRequest request) {
        switch (entityContext.type) {

        // no need to disambiguate, the dift configs are for a single resource
        case Resource:
            Set<DriftConfiguration> driftConfigs = DriftConfiguration.valueOf(result.get(0));
            response.setData(buildRecords(driftConfigs));
            // for paging to work we have to specify size of full result set
            response.setTotalRows(getTotalRows(driftConfigs, response, request));
            processResponse(request.getRequestId(), response);
            break;

        // disambiguate as the results could be cross-resource
        default:
            Set<Integer> typesSet = new HashSet<Integer>();
            Set<String> ancestries = new HashSet<String>();
            for (Resource resource : result) {
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

                    Set<DriftConfiguration> driftConfigs = getDriftConfigs(result);
                    Record[] records = buildRecords(driftConfigs);
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
                    response.setTotalRows(getTotalRows(driftConfigs, response, request));
                    processResponse(request.getRequestId(), response);
                }
            });
        }
    }

    private Set<DriftConfiguration> getDriftConfigs(PageList<Resource> resources) {
        Set<DriftConfiguration> result = new HashSet<DriftConfiguration>();
        for (Resource resource : resources) {
            result.addAll(DriftConfiguration.valueOf(resource.getDriftConfigurations()));
        }
        return result;
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
    protected int getTotalRows(final Set<DriftConfiguration> result, final DSResponse response, final DSRequest request) {

        return result.size();
    }

    @Override
    protected ResourceCriteria getFetchCriteria(DSRequest request) {

        ResourceCriteria criteria = new ResourceCriteria();
        switch (entityContext.getType()) {
        case Resource:
            criteria.addFilterId(entityContext.getResourceId());
            break;

        case ResourceGroup:
            criteria.addFilterExplicitGroupIds(entityContext.getGroupId());
            break;

        default:
            // no filter
        }

        criteria.fetchDriftConfigurations(true);
        criteria.setPageControl(getPageControl(request));

        return criteria;
    }

    @Override
    protected String getSortFieldForColumn(String columnName) {
        if (AncestryUtil.RESOURCE_ANCESTRY.equals(columnName)) {
            return "ancestry";
        }

        return super.getSortFieldForColumn(columnName);
    }

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

        record.setAttribute("id", from.getId());
        record.setAttribute("name", from.getName());
        record.setAttribute("interval", String.valueOf(from.getInterval()));
        record.setAttribute("baseDir", from.getBasedir());
        record.setAttribute("enabled", from.getEnabled());

        Resource resource = from.getResource();

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

    public ResourceGWTServiceAsync getResourceService() {
        return resourceService;
    }

    protected EntityContext getEntityContext() {
        return entityContext;
    }

    protected void setEntityContext(EntityContext entityContext) {
        this.entityContext = entityContext;
    }

}