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
package org.rhq.enterprise.gui.coregui.client.menu;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.TextBox;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.FormLayoutType;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.TextMatchStyle;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.FormItemIfFunction;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuButton;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.MenuItemSeparator;

import java.util.LinkedHashMap;

import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupsDataSource;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDatasource;

/**
 * @author Greg Hinkle
 */
public class SearchBarPane extends HLayout {

    public SearchBarPane() {
        super();
        setWidth100();
        setHeight(28);
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        final DynamicForm form = new DynamicForm();
        form.setNumCols(6);
        form.setColWidths("120", "140", "400");

        final SelectItem searchType = new SelectItem("searchType", "Search");
        searchType.setWidth(120);
        searchType.setValueMap("Resources", "Resource Groups", "Bundles", "Packages", "Users", "Roles");
        searchType.setValue("Resources");

        ComboBoxItem resourceSearch = getResourceComboBox();
        resourceSearch.setShowIfCondition(new FormItemIfFunction() {
            public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
                return form.getValueAsString("searchType").equals("Resources");
            }
        });

        TextItem query = new TextItem("query");
        query.setShowTitle(false);

        ButtonItem search = new ButtonItem("Search", "Search");
        search.setStartRow(false);
        search.setEndRow(false);
        search.setShowTitle(false);
        search.setIcon(Window.getImgURL("[SKIN]/actions/view.png"));

        form.setItems(searchType, resourceSearch, search, new SpacerItem());

        addMember(form);
    }


    private ComboBoxItem getResourceComboBox() {

        final ComboBoxItem comboBox = new ComboBoxItem("query", "Query");
        comboBox.setWidth(400);
        comboBox.setShowTitle(false);
        comboBox.setHint("resource search");
        comboBox.setShowHintInField(true);

        comboBox.setOptionDataSource(new ResourceDatasource());

        ListGridField nameField = new ListGridField("name", "Name", 250);
        ListGridField descriptionField = new ListGridField("description", "Description");
        ListGridField typeNameField = new ListGridField("typeName", "Type", 130);
        ListGridField pluginNameField = new ListGridField("pluginName", "Plugin", 100);
        ListGridField categoryField = new ListGridField("category", "Category", 60);
        ListGridField availabilityField = new ListGridField("currentAvailability", "Availability", 55);
        availabilityField.setAlign(Alignment.CENTER);

        comboBox.setPickListFields(nameField, descriptionField, typeNameField, pluginNameField, categoryField, availabilityField);

        comboBox.setValueField("id");
        comboBox.setDisplayField("name");
        comboBox.setPickListWidth(800);
        comboBox.setTextMatchStyle(TextMatchStyle.SUBSTRING);
        comboBox.setCompleteOnTab(true);

        comboBox.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent changedEvent) {
                try {
                    Integer resourceId = (Integer) changedEvent.getValue();
                    comboBox.setValue("");

                    String link = LinkManager.getResourceLink(resourceId);
                    if (!link.contains("#")) {
                        com.google.gwt.user.client.Window.Location.assign(link);
                    } else {
                        History.newItem(link.substring(1));
                    }
                } catch (Exception e) {
                }
            }
        });

        return comboBox;
    }


    private ComboBoxItem getGroupComboBox() {
        ComboBoxItem comboBox = new ComboBoxItem("query", "Query");
        comboBox.setWidth(400);
        comboBox.setShowTitle(false);


        comboBox.setOptionDataSource(new ResourceGroupsDataSource());
        ListGridField nameField = new ListGridField("name");
        ListGridField descriptionField = new ListGridField("description");
        comboBox.setPickListFields(nameField, descriptionField);


        comboBox.setValueField("id");
        comboBox.setDisplayField("name");
        comboBox.setPickListWidth(600);
        comboBox.setTextMatchStyle(TextMatchStyle.SUBSTRING);

        return comboBox;
    }

}
