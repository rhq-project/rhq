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
package org.rhq.enterprise.gui.coregui.client.inventory.groups;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGroupGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.wizard.GroupCreateWizard;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;

/**
 * @author Greg Hinkle
 */
public class ResourceGroupListView extends Table {

    public ResourceGroupListView() {
        super("Resource Groups");
        setWidth100();
        setHeight100();
    }

    public ResourceGroupListView(Criteria criteria) {
        super("Resource Groups", criteria);
    }

    @Override
    protected void onInit() {
        super.onInit();

        // setHeaderIcon("?_24.png");

        final ResourceGroupsDataSource datasource = ResourceGroupsDataSource.getInstance();
        setDataSource(datasource);

        getListGrid().setSelectionType(SelectionStyle.SIMPLE);
        //table.getListGrid().setSelectionAppearance(SelectionAppearance.CHECKBOX);
        getListGrid().setResizeFieldsInRealTime(true);

        ListGridField idField = new ListGridField("id", "Id", 55);
        idField.setType(ListGridFieldType.INTEGER);
        ListGridField nameField = new ListGridField("name", "Name", 250);
        nameField.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                return "<a href=\"#ResourceGroup/" + listGridRecord.getAttribute("id") + "\">" + o + "</a>";
            }
        });

        ListGridField descriptionField = new ListGridField("description", "Description");
        ListGridField typeNameField = new ListGridField("typeName", "Type", 130);
        ListGridField pluginNameField = new ListGridField("pluginName", "Plugin", 100);
        ListGridField categoryField = new ListGridField("category", "Category", 60);

        ListGridField availabilityField = new ListGridField("currentAvailability", "Availability", 55);

        availabilityField.setAlign(Alignment.CENTER);
        getListGrid().setFields(idField, nameField, descriptionField, typeNameField, pluginNameField, categoryField,
            availabilityField);

        addTableAction("Delete", Table.SelectionEnablement.ANY, "Delete the selected resource groups?",
            new TableAction() {
                public void executeAction(ListGridRecord[] selections) {
                    ResourceGroupsDataSource ds = (ResourceGroupsDataSource) getDataSource();
                    for (ListGridRecord selection : selections) {
                        ResourceGroupGWTServiceAsync resourceGroupManager = GWTServiceLookup.getResourceGroupService();
                        final ResourceGroup object = ds.copyValues(selection);
                        resourceGroupManager.deleteResourceGroup(object.getId(), new AsyncCallback<Void>() {
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError(
                                    "Failed to delete resource group [" + object.getName() + "]", caught);
                            }

                            public void onSuccess(Void result) {
                                CoreGUI.getMessageCenter().notify(
                                    new Message("Deleted resource group [" + object.getName() + "]", Severity.Info));

                                CoreGUI.refresh();
                            }
                        });
                    }
                }
            });

        addTableAction("New", new TableAction() {
            public void executeAction(ListGridRecord[] selection) {
                new GroupCreateWizard().startBundleWizard();
            }
        });
    }

}