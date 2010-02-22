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
package org.rhq.enterprise.gui.coregui.client.inventory.resource;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.SelectionAppearance;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
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
import com.smartgwt.client.widgets.grid.events.RecordClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordClickHandler;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import java.util.ArrayList;

/**
 * @author Greg Hinkle
 */
public class ResourceSearchView extends VLayout {

    private ListGrid listGrid;

    private ArrayList<ResourceSelectListener> selectListeners = new ArrayList<ResourceSelectListener>();

    public ResourceSearchView() {

        setWidth100();
        setHeight100();

        DynamicForm searchPanel = new DynamicForm();
        final TextItem searchBox = new TextItem("query", "Search Resources");
        searchBox.setValue("");
        searchPanel.setWrapItemTitles(false);
        searchPanel.setFields(searchBox);


        addMember(searchPanel);


        final ResourceDatasource datasource = new ResourceDatasource();

        VLayout gridHolder = new VLayout();

        listGrid = new ListGrid();
        listGrid.setWidth100();
        listGrid.setHeight100();
        listGrid.setDataSource(datasource);
        listGrid.setAutoFetchData(true);
        listGrid.setAlternateRecordStyles(true);
//        listGrid.setAutoFitData(Autofit.HORIZONTAL);
        listGrid.setCriteria(new Criteria("name", searchPanel.getValueAsString("query")));
        listGrid.setSelectionType(SelectionStyle.SIMPLE);
        listGrid.setSelectionAppearance(SelectionAppearance.CHECKBOX);



        ListGridField idField = new ListGridField("id", "Id", 55);
        idField.setType(ListGridFieldType.INTEGER);
        ListGridField nameField = new ListGridField("name", "Name", 250);
        nameField.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                return "<a href=\"#Resource/" +  listGridRecord.getAttribute("id")  +"\">" + o + "</a>";
            }
        });

        /* TODO: Safe to remove this, we're now using links in the records
        nameField.addRecordClickHandler(new RecordClickHandler() {
            public void onRecordClick(final RecordClickEvent recordClickEvent) {
                for (final ResourceSelectListener l : selectListeners) {
                    // TODO GH: This doesn't work
                    final int resourceId = recordClickEvent.getRecord().getAttributeAsInt("id");
                    ResourceCriteria c = new ResourceCriteria();
                    c.addFilterId(resourceId);
                    ResourceGWTServiceAsync.Util.getInstance().findResourcesByCriteria(CoreGUI.getSessionSubject(), c, new AsyncCallback<PageList<Resource>>() {
                        public void onFailure(Throwable caught) {
                            System.out.println("Failed ehre");
                        }

                        public void onSuccess(PageList<Resource> result) {

                            l.onResourceSelected(result.get(0));

                        }
                    });
                }
            }
        });
        */

        ListGridField descriptionField = new ListGridField("description", "Description");
        ListGridField availabilityField = new ListGridField("currentAvailability", "Availability", 55);
        availabilityField.setAlign(Alignment.CENTER);
        listGrid.setFields(idField, nameField, descriptionField, availabilityField);


        gridHolder.addMember(listGrid);

        ToolStrip toolStrip = new ToolStrip();
        toolStrip.setMembersMargin(15);

        final IButton removeButton = new IButton("Remove");
        removeButton.setDisabled(true);
        removeButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                SC.confirm("Are you sure you want to delete " + listGrid.getSelection().length + " resources?",
                        new BooleanCallback() {
                            public void execute(Boolean aBoolean) {

                            }
                        }
                );
            }
        });



        final Label tableInfo = new Label("Total: " + listGrid.getTotalRows());
        tableInfo.setWrap(false);

        toolStrip.addMember(removeButton);
        toolStrip.addMember(new LayoutSpacer());
        toolStrip.addMember(tableInfo);

        gridHolder.addMember(toolStrip);


        listGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                int selectedCount = ((ListGrid)selectionEvent.getSource()).getSelection().length;
                tableInfo.setContents("Total: " + listGrid.getTotalRows() + " (" + selectedCount + " selected)");
                removeButton.setDisabled(selectedCount == 0);
            }
        });



        addMember(gridHolder);


        listGrid.addDataArrivedHandler(new DataArrivedHandler() {
            public void onDataArrived(DataArrivedEvent dataArrivedEvent) {
                int selectedCount = ((ListGrid)dataArrivedEvent.getSource()).getSelection().length;
                tableInfo.setContents("Total: " + listGrid.getTotalRows() + " (" + selectedCount + " selected)");
            }
        });


        searchBox.addKeyPressHandler(new KeyPressHandler() {
            public void onKeyPress(KeyPressEvent event) {
                if (event.getCharacterValue() == KeyCodes.KEY_ENTER) {
                    datasource.setQuery((String) searchBox.getValue());
                    Criteria c = new Criteria("name", (String) searchBox.getValue());
                    long start = System.currentTimeMillis();
                    listGrid.fetchData(c);
                    System.out.println("Loaded in: " + (System.currentTimeMillis() - start));
                }
            }
        });
    }


    public void addResourceSelectedListener(ResourceSelectListener listener) {
        selectListeners.add(listener);
    }

}
