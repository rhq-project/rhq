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

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationComparisonView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDatasource;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSelectListener;

import com.google.gwt.event.dom.client.KeyCodes;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.SelectionAppearance;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.KeyPressEvent;
import com.smartgwt.client.widgets.form.fields.events.KeyPressHandler;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.DataArrivedEvent;
import com.smartgwt.client.widgets.grid.events.DataArrivedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import java.util.ArrayList;

/**
 * @author Greg Hinkle
 */
public class ConfigurationHistoryView extends VLayout {

    private ListGrid listGrid;

    /**
     * A resource list of all resources in the system
     */
    public ConfigurationHistoryView() {
        this(null);
    }

    /**
     * Resource list filtered by a given criteria
     */
    public ConfigurationHistoryView(final Criteria criteria) {

        setWidth100();
        setHeight100();


        final ConfigurationHistoryDataSource datasource = new ConfigurationHistoryDataSource();

        VLayout gridHolder = new VLayout();

        listGrid = new ListGrid();
        listGrid.setWidth100();
        listGrid.setHeight100();
        listGrid.setDataSource(datasource);
        listGrid.setAutoFetchData(true);
        listGrid.setAlternateRecordStyles(true);
        listGrid.setUseAllDataSourceFields(true);


        ListGridField idField = new ListGridField("id","ID",60);
        ListGridField createdField = new ListGridField("createdTime","Timestamp",200);
        ListGridField statusField = new ListGridField("status","Status",100);
        ListGridField userField = new ListGridField("subject","User",150);
        listGrid.setFields(idField,createdField,statusField,userField);


        if (criteria != null) {
            listGrid.setInitialCriteria(criteria);
        }

        listGrid.setSelectionType(SelectionStyle.SIMPLE);
        listGrid.setSelectionAppearance(SelectionAppearance.CHECKBOX);
        listGrid.setResizeFieldsInRealTime(true);


        gridHolder.addMember(listGrid);

        ToolStrip toolStrip = new ToolStrip();
        toolStrip.setPadding(5);
        toolStrip.setWidth100();
        toolStrip.setMembersMargin(15);

        final IButton removeButton = new IButton("Remove");
        removeButton.setDisabled(true);
        removeButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                SC.confirm("Are you sure you want to delete " + listGrid.getSelection().length + " configurations?",
                        new BooleanCallback() {
                            public void execute(Boolean aBoolean) {

                            }
                        }
                );
            }
        });


        final IButton compareButton = new IButton("Compare");
        compareButton.setDisabled(true);
        compareButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                int resourceId = Integer.parseInt((String)criteria.getValues().get("resourceId"));

                ListGridRecord[] selection = listGrid.getSelection();
                ArrayList<ResourceConfigurationUpdate> configs = new ArrayList<ResourceConfigurationUpdate>();
                for (ListGridRecord record : selection) {
                    ResourceConfigurationUpdate update = (ResourceConfigurationUpdate) record.getAttributeAsObject("entity");
                    configs.add(update);
                }


                ConfigurationComparisonView.displayComparisonDialog(configs);
            }
        });


        final Label tableInfo = new Label("Total: " + listGrid.getTotalRows());
        tableInfo.setWrap(false);

        toolStrip.addMember(removeButton);
        toolStrip.addMember(compareButton);
        toolStrip.addMember(new LayoutSpacer());
        toolStrip.addMember(tableInfo);

        gridHolder.addMember(toolStrip);


        listGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                int selectedCount = ((ListGrid) selectionEvent.getSource()).getSelection().length;
                tableInfo.setContents("Total: " + listGrid.getTotalRows() + " (" + selectedCount + " selected)");
                removeButton.setDisabled(selectedCount == 0);
                compareButton.setDisabled(selectedCount == 0);
            }
        });


        addMember(gridHolder);


        listGrid.addDataArrivedHandler(new DataArrivedHandler() {
            public void onDataArrived(DataArrivedEvent dataArrivedEvent) {
                int selectedCount = ((ListGrid) dataArrivedEvent.getSource()).getSelection().length;
                tableInfo.setContents("Total: " + listGrid.getTotalRows() + " (" + selectedCount + " selected)");
            }
        });
    }



    // -------- Static Utility loaders ------------


    public static ConfigurationHistoryView getHistoryOf(int resourceId) {
        return new ConfigurationHistoryView(new Criteria("resourceId",String.valueOf(resourceId)));
    }




}