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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.traits;

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
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.MeasurementDataTraitCriteria;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMeasurementDataTraitDataSource;
import org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMeasurementDataTraitListView;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableListGrid;

/**
 * The Resource Monitoring>Traits subtab.
 *
 * @author Ian Springer
 */
public class TraitsView extends AbstractMeasurementDataTraitListView {
    private int resourceId;

    public TraitsView(int resourceId) {
        super(new TraitsDataSource(), createCriteria(resourceId));
        this.resourceId = resourceId;
    }

    @Override
    public Canvas getDetailsView(Integer definitionId) {
        return new TraitsDetailView(extendLocatorId("Detail"), this.resourceId, definitionId);
    }

    @Override
    protected void configureTable() {
        super.configureTable();
    }

    private static Criteria createCriteria(int resourceId) {
        Criteria criteria = new Criteria();

        criteria.addCriteria(MeasurementDataTraitCriteria.FILTER_FIELD_RESOURCE_ID, resourceId);
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
                final Map<String, String> scheduleNames = new HashMap<String, String>();
                int[] definitionIds = new int[selection.length];
                int i = 0;
                for (ListGridRecord record : selection) {
                    Integer defId = record
                        .getAttributeAsInt(AbstractMeasurementDataTraitDataSource.FIELD_METRIC_SCHED_ID);
                    definitionIds[i++] = defId.intValue();

                    scheduleNames.put(record.getAttribute(AbstractMeasurementDataTraitDataSource.FIELD_METRIC_NAME),
                        record.getAttribute(MeasurementDataTraitCriteria.SORT_FIELD_DISPLAY_NAME));
                }

                // actually go out and ask the agents for the data
                GWTServiceLookup.getMeasurementDataService(60000).findLiveData(resourceId, definitionIds,
                    new AsyncCallback<Set<MeasurementData>>() {
                        public void onSuccess(Set<MeasurementData> result) {
                            if (result == null) {
                                result = new HashSet<MeasurementData>(0);
                            }
                            ArrayList<ListGridRecord> records = new ArrayList<ListGridRecord>(result.size());
                            for (MeasurementData data : result) {
                                ListGridRecord record = new ListGridRecord();
                                record.setAttribute("name", scheduleNames.get(data.getName()));
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
        ListGridField name = new ListGridField("name", MSG.dataSource_traits_field_trait());
        ListGridField value = new ListGridField("value", MSG.common_title_value());
        liveDataGrid.setFields(name, value);

        return liveDataGrid;
    }
}
