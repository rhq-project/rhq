/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.coregui.client.report.measurement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.measurement.composite.MeasurementOOBComposite;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository.TypesLoadedCallback;
import org.rhq.coregui.client.util.MeasurementConverterClient;
import org.rhq.coregui.client.util.RPCDataSource;

/**
 * @author Greg Hinkle
 */
public class MeasurementOOBDataSource extends RPCDataSource<MeasurementOOBComposite, Criteria> {

    private int maximumFactor = 0;

    public MeasurementOOBDataSource() {
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    /**
     * The view that contains the list grid which will display this datasource's data will call this
     * method to get the field information which is used to control the display of the data.
     * 
     * @return list grid fields used to display the datasource data
     */
    public ArrayList<ListGridField> getListGridFields() {
        ArrayList<ListGridField> fields = new ArrayList<ListGridField>();

        ListGridField resourceNameField = new ListGridField(AncestryUtil.RESOURCE_NAME, MSG.common_title_resource());
        resourceNameField.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                String url = LinkManager.getResourceLink(listGridRecord.getAttributeAsInt(AncestryUtil.RESOURCE_ID));
                return LinkManager.getHref(url, o.toString());
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
        ancestryField.setCanSortClientOnly(true);
        fields.add(ancestryField);

        ListGridField scheduleNameField = new ListGridField("scheduleName", MSG
            .dataSource_measurementOob_field_scheduleName());
        fields.add(scheduleNameField);

        ListGridField bandField = new ListGridField("formattedBaseband", MSG
            .dataSource_measurementOob_field_formattedBaseband());
        bandField.setCanSortClientOnly(true);
        fields.add(bandField);

        ListGridField outlierField = new ListGridField("formattedOutlier", MSG
            .dataSource_measurementOob_field_formattedOutlier());
        outlierField.setCanSortClientOnly(true);
        fields.add(outlierField);

        ListGridField factorField = new ListGridField("factor", MSG.dataSource_measurementOob_field_factor());
        fields.add(factorField);

        resourceNameField.setWidth("20%");
        ancestryField.setWidth("30%");
        scheduleNameField.setWidth("20%");
        bandField.setWidth("10%");
        outlierField.setWidth("10%");
        factorField.setWidth("10%");

        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response, final Criteria unused) {

        PageControl pc = getPageControl(request);

        GWTServiceLookup.getMeasurementDataService().getSchedulesWithOOBs(null, null, null, pc,
            new AsyncCallback<PageList<MeasurementOOBComposite>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.dataSource_measurementOob_error_fetchFailure(), caught);
                }

                public void onSuccess(final PageList<MeasurementOOBComposite> result) {
                    Set<Integer> typesSet = new HashSet<Integer>();
                    Set<String> ancestries = new HashSet<String>();
                    for (MeasurementOOBComposite composite : result) {
                        typesSet.add(composite.getResourceTypeId());
                        ancestries.add(composite.getResourceAncestry());
                    }

                    // In addition to the types of the result resources, get the types of their ancestry
                    typesSet.addAll(AncestryUtil.getAncestryTypeIds(ancestries));

                    ResourceTypeRepository typeRepo = ResourceTypeRepository.Cache.getInstance();
                    typeRepo.getResourceTypes(typesSet.toArray(new Integer[typesSet.size()]),
                        new TypesLoadedCallback() {
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
                                    record.setAttribute(AncestryUtil.RESOURCE_ANCESTRY_VALUE, AncestryUtil
                                        .getAncestryValue(record));
                                }
                                response.setData(records);
                                // for paging to work we have to specify size of full result set
                                response.setTotalRows(result.getTotalSize());
                                processResponse(request.getRequestId(), response);
                            }
                        });
                }
            });

    }

    @Override
    protected Criteria getFetchCriteria(DSRequest request) {
        // we don't use criterias for this datasource, just return null
        return null;

    }

    protected String getSortFieldForColumn(String columnName) {
        // Allow server-side sorting for only unmodified queries, these keywords are for MeasurementOOBManagerBean.
        // Rest of the fields should have client-side sorting only.
        String sortField = null;
        if("scheduleName".equals(columnName)) {
            sortField = "def.displayName";
        } else if("resourceName".equals(columnName)) {
            sortField = "res.name";
        } else if("timestamp".equals(columnName)) {
            sortField = "o.timestamp";
        } else if("factor".equals(columnName)) {
            sortField = "o.oobFactor";
        }
        return sortField;
    }

    @Override
    public MeasurementOOBComposite copyValues(Record from) {
        throw new UnsupportedOperationException("OOBs Read only");
    }

    @Override
    public ListGridRecord[] buildRecords(Collection<MeasurementOOBComposite> dataObjects) {
        for (MeasurementOOBComposite oob : dataObjects) {
            if (oob.getFactor() > maximumFactor) {
                maximumFactor = oob.getFactor();
            }
        }

        return super.buildRecords(dataObjects);
    }

    @Override
    public ListGridRecord copyValues(MeasurementOOBComposite from) {
        applyFormatting(from);

        ListGridRecord record = new ListGridRecord();

        record.setAttribute("scheduleId", from.getScheduleId());
        record.setAttribute("scheduleName", from.getScheduleName());
        record.setAttribute("definitionId", from.getDefinitionId());

        record.setAttribute("factor", from.getFactor());
        record.setAttribute("formattedBaseband", from.getFormattedBaseband());
        record.setAttribute("formattedOutlier", from.getFormattedOutlier());
        record.setAttribute("blMin", from.getBlMin());
        record.setAttribute("blMax", from.getBlMax());
        record.setAttribute("parentId", from.getParentId());
        record.setAttribute("parentName", from.getParentName());

        // for ancestry handling       
        record.setAttribute(AncestryUtil.RESOURCE_ID, from.getResourceId());
        record.setAttribute(AncestryUtil.RESOURCE_NAME, from.getResourceName());
        record.setAttribute(AncestryUtil.RESOURCE_ANCESTRY, from.getResourceAncestry());
        record.setAttribute(AncestryUtil.RESOURCE_TYPE_ID, from.getResourceTypeId());

        int factorRankingWidth = (int) (((double) from.getFactor()) / (double) maximumFactor * 100d);

        record.setBackgroundComponent(new HTMLFlow("<div style=\"width: " + factorRankingWidth
            + "%; height: 100%; background-color: #A5B391;\">&nbsp;</div>"));

        return record;

    }

    private void applyFormatting(MeasurementOOBComposite oob) {
        oob.setFormattedOutlier(MeasurementConverterClient.format(oob.getOutlier(), oob.getUnits(), true));
        formatBaseband(oob);
    }

    private void formatBaseband(MeasurementOOBComposite oob) {
        String min = MeasurementConverterClient.format(oob.getBlMin(), oob.getUnits(), true);
        String max = MeasurementConverterClient.format(oob.getBlMax(), oob.getUnits(), true);
        oob.setFormattedBaseband(min + ", " + max);
    }
}
