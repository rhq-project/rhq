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

import com.google.gwt.user.client.History;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.TextMatchStyle;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.FormItemIfFunction;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.grid.ListGridField;

import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupDataSourceField;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupsDataSource;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDatasource;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;

/**
 * @author Greg Hinkle
 * @author Joseph Marques
 */
public class SearchBarPane extends LocatableHLayout {

    public SearchBarPane(String locatorId) {
        super(locatorId);

        setWidth100();
        setHeight(28);
    }

    public enum SearchType {
        RESOURCE("Resources"), //
        GROUP("Resource Groups");

        private String displayName;

        private SearchType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static String[] getValueMap() {
            SearchType[] searchTypes = SearchType.values();
            String[] results = new String[searchTypes.length];
            int i = 0;
            for (SearchType nextType : searchTypes) {
                results[i++] = nextType.getDisplayName();
            }
            return results;
        }
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        final DynamicForm form = new LocatableDynamicForm(this.getLocatorId());
        form.setNumCols(6);
        form.setColWidths("120", "140", "400");

        final SelectItem searchType = new SelectItem("searchType", "Search");
        String[] valueMap = SearchType.getValueMap();
        searchType.setValueMap(valueMap);
        searchType.setValue(valueMap[0]);
        searchType.setWidth(120);
        searchType.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                markForRedraw();
            }
        });

        ComboBoxItem resourceSearch = getResourceComboBox();
        ComboBoxItem groupSearch = getGroupComboBox();

        ButtonItem search = new ButtonItem("Search", "Search");
        search.setStartRow(false);
        search.setEndRow(false);
        search.setShowTitle(false);
        search.setIcon(Window.getImgURL("[SKIN]/actions/view.png"));

        form.setItems(searchType, resourceSearch, groupSearch, search, new SpacerItem());

        addMember(form);
    }

    private ComboBoxItem getResourceComboBox() {
        final ComboBoxItem comboBox = getBaseComboBox(SearchType.RESOURCE);

        ListGridField nameField = ResourceDataSourceField.NAME.getListGridField(250);
        ListGridField descriptionField = ResourceDataSourceField.DESCRIPTION.getListGridField();
        ListGridField typeNameField = ResourceDataSourceField.TYPE.getListGridField(130);
        ListGridField pluginNameField = ResourceDataSourceField.PLUGIN.getListGridField(100);
        ListGridField categoryField = ResourceDataSourceField.CATEGORY.getListGridField(60);
        ListGridField availabilityField = ResourceDataSourceField.AVAILABILITY.getListGridField(55);
        availabilityField.setAlign(Alignment.CENTER);

        comboBox.setPickListFields(nameField, descriptionField, typeNameField, pluginNameField, categoryField,
            availabilityField);

        comboBox.setValueField("id");
        comboBox.setDisplayField(ResourceDataSourceField.NAME.propertyName());
        comboBox.setOptionDataSource(new ResourceDatasource());

        return comboBox;
    }

    private ComboBoxItem getGroupComboBox() {
        final ComboBoxItem comboBox = getBaseComboBox(SearchType.GROUP);

        ListGridField nameField = ResourceGroupDataSourceField.NAME.getListGridField(250);
        ListGridField descriptionField = ResourceGroupDataSourceField.DESCRIPTION.getListGridField();
        ListGridField typeNameField = ResourceGroupDataSourceField.TYPE.getListGridField(130);
        ListGridField pluginNameField = ResourceGroupDataSourceField.PLUGIN.getListGridField(100);
        ListGridField categoryField = ResourceGroupDataSourceField.CATEGORY.getListGridField(105);

        comboBox.setPickListFields(nameField, descriptionField, typeNameField, pluginNameField, categoryField);

        comboBox.setValueField("id");
        comboBox.setDisplayField(ResourceGroupDataSourceField.NAME.propertyName());
        comboBox.setOptionDataSource(new ResourceGroupsDataSource());

        return comboBox;
    }

    private ComboBoxItem getBaseComboBox(final SearchType searchType) {
        final ComboBoxItem comboBox = new ComboBoxItem("query", "Query");
        comboBox.setWidth(400);
        comboBox.setShowTitle(false);
        comboBox.setHint("search");
        comboBox.setShowHintInField(true);

        comboBox.setPickListWidth(800);
        comboBox.setTextMatchStyle(TextMatchStyle.SUBSTRING);
        comboBox.setCompleteOnTab(true);

        comboBox.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent changedEvent) {
                //System.out.println("ChangedEvent: " + changedEvent.getValue());

                Object intermediate = changedEvent.getValue();
                if (!(intermediate instanceof Integer)) {
                    return;
                }

                Integer id = (Integer) changedEvent.getValue();
                comboBox.setValue("");

                String link = null;
                if (searchType == SearchType.RESOURCE) {
                    link = LinkManager.getResourceLink(id);
                } else if (searchType == SearchType.GROUP) {
                    link = LinkManager.getResourceGroupLink(id);
                } else {
                    throw new IllegalArgumentException("There is no global search type for " + searchType);
                }

                if (!link.contains("#")) {
                    com.google.gwt.user.client.Window.Location.assign(link);
                } else {
                    History.newItem(link.substring(1));
                }
            }
        });

        comboBox.setShowIfCondition(new FormItemIfFunction() {
            public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
                return dynamicForm.getValueAsString("searchType").equals(searchType.getDisplayName());
            }
        });

        return comboBox;
    }

}
