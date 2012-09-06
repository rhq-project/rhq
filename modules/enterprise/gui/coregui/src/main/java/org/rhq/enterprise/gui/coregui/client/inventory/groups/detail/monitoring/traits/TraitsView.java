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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.traits;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.MeasurementDataTraitCriteria;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.util.collection.ArrayUtils;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMeasurementDataTraitDataSource;
import org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMeasurementDataTraitListView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableListGrid;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * The group Monitoring>Traits subtab.
 *
 * @author Ian Springer
 */
public class TraitsView extends AbstractMeasurementDataTraitListView {
    private int groupId;

    public TraitsView(String locatorId, int groupId) {
        super(locatorId, new TraitsDataSource(groupId), createCriteria(groupId));
        this.groupId = groupId;
    }

    @Override
    protected void configureTable() {
        super.configureTable();

        ListGrid listGrid = getListGrid();

        listGrid.setShowAllRecords(true);
        //listGrid.setGroupStartOpen(GroupStartOpen.ALL);
        //listGrid.groupBy(MeasurementDataTraitCriteria.SORT_FIELD_DISPLAY_NAME);

        ListGridField resourceNameField = listGrid.getField(MeasurementDataTraitCriteria.SORT_FIELD_RESOURCE_NAME);
        resourceNameField.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                String url = LinkManager.getResourceLink(listGridRecord.getAttributeAsInt(AncestryUtil.RESOURCE_ID));
                return SeleniumUtility.getLocatableHref(url, o.toString(), null);
            }
        });
        resourceNameField.setShowHover(true);
        resourceNameField.setHoverCustomizer(new HoverCustomizer() {
            public String hoverHTML(Object value, ListGridRecord listGridRecord, int rowNum, int colNum) {
                return AncestryUtil.getResourceHoverHTML(listGridRecord, 0);
            }
        });
        //resourceNameField.setCanGroupBy(true);

        AncestryUtil.setupAncestryListGridField(listGrid);
    }

    @Override
    public Canvas getDetailsView(Integer definitionId) {
        return new TraitsDetailView(extendLocatorId("Detail"), this.groupId, definitionId);
    }

    private static Criteria createCriteria(int groupId) {
        Criteria criteria = new Criteria();

        criteria.addCriteria(MeasurementDataTraitCriteria.FILTER_FIELD_GROUP_ID, groupId);
        criteria.addCriteria(MeasurementDataTraitCriteria.FILTER_FIELD_MAX_TIMESTAMP, true);

        return criteria;
    }

    @Override
    protected TableAction getLiveValueAction() {
        return new TableAction() {
            @Override
            public boolean isEnabled(ListGridRecord[] selection) {
                return selection != null && selection.length > 0;
            }

            @Override
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                if (selection == null || selection.length == 0) {
                    return;
                }
                final Map<String, ListGridRecord> selectedRecords = new HashMap<String, ListGridRecord>();
                int[] definitionIds = new int[selection.length];
                int i = 0;
                Set<Integer> resourceIds = new HashSet<Integer>();
                for (ListGridRecord record : selection) {
                    Integer defId = record
                        .getAttributeAsInt(AbstractMeasurementDataTraitDataSource.FIELD_METRIC_SCHED_ID);
                    definitionIds[i++] = defId.intValue();
                    int resourceId = record.getAttributeAsInt(MeasurementDataTraitCriteria.FILTER_FIELD_RESOURCE_ID);
                    resourceIds.add(resourceId);

                    selectedRecords.put(
                        resourceId + ":"
                            + record.getAttribute(AbstractMeasurementDataTraitDataSource.FIELD_METRIC_NAME), record);
                }

                // actually go out and ask the agents for the data
                GWTServiceLookup.getMeasurementDataService(60000).findLiveDataForGroup(groupId,
                    ArrayUtils.unwrapCollection(resourceIds), definitionIds, new AsyncCallback<Set<MeasurementData>>() {
                        @Override
                        public void onSuccess(Set<MeasurementData> result) {
                            if (result == null) {
                                result = new HashSet<MeasurementData>(0);
                            }
                            ArrayList<ListGridRecord> records = new ArrayList<ListGridRecord>(result.size());
                            for (MeasurementData data : result) {
                                ListGridRecord record = selectedRecords.get(data.getName());
                                record.setAttribute("value", data.getValue());
                                records.add(record);
                            }
                            Collections.sort(records, new Comparator<ListGridRecord>() {
                                public int compare(ListGridRecord o1, ListGridRecord o2) {
                                    return o1.getAttribute("name").compareTo(o2.getAttribute("name"));
                                }
                            });
                            showLiveData(records);
                        }

                        @Override
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError(MSG.view_measureTable_getLive_failure(), caught);
                        }
                    });
            }
        };
    }

    @Override
    protected LocatableListGrid decorateLiveDataGrid(List<ListGridRecord> records) {
        LocatableListGrid liveDataGrid = new LocatableListGrid(extendLocatorId("liveDataListGrid"));
        liveDataGrid.setShowAllRecords(true);
        liveDataGrid.setData(records.toArray(new ListGridRecord[records.size()]));
        liveDataGrid.setSelectionType(SelectionStyle.NONE);
        ListGridField name = new ListGridField(MeasurementDataTraitCriteria.SORT_FIELD_DISPLAY_NAME,
            MSG.dataSource_traits_field_trait());
        ListGridField value = new ListGridField("value", MSG.common_title_value());
        ListGridField resourceNameField = new ListGridField(MeasurementDataTraitCriteria.SORT_FIELD_RESOURCE_NAME,
            MSG.common_title_resource());
        resourceNameField.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                String url = LinkManager.getResourceLink(listGridRecord.getAttributeAsInt(AncestryUtil.RESOURCE_ID));
                return SeleniumUtility.getLocatableHref(url, o.toString(), null);
            }
        });
        resourceNameField.setShowHover(true);
        resourceNameField.setHoverCustomizer(new HoverCustomizer() {
            public String hoverHTML(Object value, ListGridRecord listGridRecord, int rowNum, int colNum) {
                return AncestryUtil.getResourceHoverHTML(listGridRecord, 0);
            }
        });
        liveDataGrid.setFields(name, value, resourceNameField, AncestryUtil.setupAncestryListGridField());

        return liveDataGrid;
    }
}
