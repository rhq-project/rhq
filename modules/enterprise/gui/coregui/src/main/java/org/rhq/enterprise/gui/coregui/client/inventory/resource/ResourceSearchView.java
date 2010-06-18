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

import java.util.ArrayList;

import com.google.gwt.event.dom.client.KeyCodes;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.SelectionAppearance;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.KeyPressEvent;
import com.smartgwt.client.widgets.form.fields.events.KeyPressHandler;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;

/**
 * @author Greg Hinkle
 */
public class ResourceSearchView extends Table {

    private static final String DEFAULT_TITLE = "Resources";

    private ArrayList<ResourceSelectListener> selectListeners = new ArrayList<ResourceSelectListener>();

    /**
     * A list of all Resources in the system.
     */
    public ResourceSearchView() {
        this(null);
    }

    public ResourceSearchView(String title, String[] excludeFields) {
        this(null, title, null, excludeFields);
    }

    /**
     * A Resource list filtered by a given criteria.
     */
    public ResourceSearchView(Criteria criteria) {
        this(criteria, DEFAULT_TITLE);
    }

    public ResourceSearchView(Criteria criteria, String title) {
        this(criteria, title, null, null);
    }

    /**
     * A Resource list filtered by a given criteria with the given title.
     */
    public ResourceSearchView(Criteria criteria, String title, SortSpecifier[] sortSpecifier, String[] excludeFields) {
        super(title, criteria, sortSpecifier, excludeFields);

        setHeaderIcon("types/Platform_up_24.png");

        setWidth100();
        setHeight100();

//        DynamicForm searchPanel = new DynamicForm();
//        final TextItem searchBox = new TextItem("query", "Search Resources");
//        searchBox.setValue("");
//        searchPanel.setWrapItemTitles(false);
//        searchPanel.setFields(searchBox);


        final ResourceDatasource datasource = new ResourceDatasource();
//        setTitleComponent(searchPanel);
        setDataSource(datasource);

        getListGrid().setSelectionType(SelectionStyle.SIMPLE);
//        getListGrid().setSelectionAppearance(SelectionAppearance.CHECKBOX);
        getListGrid().setResizeFieldsInRealTime(true);

        ListGridField idField = new ListGridField("id", "Id", 55);
        idField.setType(ListGridFieldType.INTEGER);
        ListGridField iconField = new ListGridField("icon","", 40);
        ListGridField nameField = new ListGridField("name", "Name", 250);
        nameField.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                return "<a href=\"#Resource/" + listGridRecord.getAttribute("id") + "\">" + o + "</a>";
            }
        });

        ListGridField descriptionField = new ListGridField("description", "Description");
        ListGridField typeNameField = new ListGridField("typeName", "Type", 130);
        ListGridField pluginNameField = new ListGridField("pluginName", "Plugin", 100);
        ListGridField categoryField = new ListGridField("category", "Category", 60);

        ListGridField availabilityField = new ListGridField("currentAvailability", "Availability", 55);
        availabilityField.setAlign(Alignment.CENTER);
        getListGrid().setFields(idField, iconField, nameField, descriptionField, typeNameField, pluginNameField,
                categoryField, availabilityField);

        addTableAction("Uninventory", Table.SelectionEnablement.ANY,
                "Are you sure you want to delete # resources?", new TableAction() {
                    public void executeAction(ListGridRecord[] selection) {
                        getListGrid().removeSelectedData();
                    }
                });


        /*searchBox.addKeyPressHandler(new KeyPressHandler() {
            public void onKeyPress(KeyPressEvent event) {
                if ((event.getCharacterValue() != null) && (event.getCharacterValue() == KeyCodes.KEY_ENTER)) {
                    datasource.setQuery((String) searchBox.getValue());

                    Criteria c = getListGrid().getCriteria();
                    if (c == null) {
                        c = new Criteria();
                    }

                    c.addCriteria("name", (String) searchBox.getValue());

                    long start = System.currentTimeMillis();
                    getListGrid().fetchData(c);
                    System.out.println("Loaded in: " + (System.currentTimeMillis() - start));
                }
            }
        });*/
    }



    public int getMatches() {
        return this.getListGrid().getTotalRows();
    }


    public void addResourceSelectedListener(ResourceSelectListener listener) {
        selectListeners.add(listener);
    }

    // -------- Static Utility loaders ------------

    public static ResourceSearchView getChildrenOf(int resourceId) {
        return new ResourceSearchView(new Criteria("parentId", String.valueOf(resourceId)), "Child Resources");
    }
}
