/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.table;

import java.util.ArrayList;
import java.util.List;

import com.smartgwt.client.data.Record;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.events.CellClickEvent;
import com.smartgwt.client.widgets.grid.events.CellClickHandler;

import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.enterprise.gui.coregui.client.components.FullHTMLPane;
import org.rhq.enterprise.gui.coregui.client.components.measurement.UserPreferencesMeasurementRangeEditor;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.summary.AbstractActivityView.ChartViewWindow;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.D3GraphListView;
import org.rhq.enterprise.gui.coregui.client.util.Log;

/**
 * Views a resource's measurements in a tabular view.
 *
 * @author John Mazzitelli
 * @author Simeon Pinder
 */
public class GroupMeasurementTableView extends Table<GroupMeasurementTableDataSource> {

    private final int groupId;


    public GroupMeasurementTableView(String locatorId, ResourceGroupComposite groupComposite, int groupId) {
        super(locatorId);
        this.groupId = groupId;
        setDataSource(new GroupMeasurementTableDataSource(groupComposite, groupId));
        //disable fields used when is full screen
        setShowFooterRefresh(true);
        setTitle(MSG.common_title_numeric_metrics());
    }

    @Override
    protected void configureTable() {
        ArrayList<ListGridField> fields = getDataSource().getListGridFields();

        //add cell click handler to execute on Table data entries.
        getListGrid().addCellClickHandler(new CellClickHandler() {
            @Override
            public void onCellClick(CellClickEvent event) {
                Record record = event.getRecord();
                String title = record.getAttribute(GroupMeasurementTableDataSource.FIELD_METRIC_LABEL);
                ChartViewWindow window = new ChartViewWindow("MeasurementTableFrame", title);
                int defId = record.getAttributeAsInt(GroupMeasurementTableDataSource.FIELD_METRIC_DEF_ID);
                Log.debug(" *** Mike you clicked here and defId is: "+ defId + ", groupId: "+groupId);

                CompositeGroupD3GraphListView graph = new CompositeGroupD3GraphListView("CompositeD3GraphView",
                        groupId, defId);
                window.addItem(graph);
                graph.populateData();
                window.show();
            }
        });
        setListGridFields(fields.toArray(new ListGridField[getDataSource().getListGridFields().size()]));
        addExtraWidget(new UserPreferencesMeasurementRangeEditor(extendLocatorId("range")), true);
    }


}
