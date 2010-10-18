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
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.TableUtility;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;

/**
 * @author Greg Hinkle
 */
public class ResourceSearchView extends Table {

    private static final String DEFAULT_TITLE = "Resources";

    private ArrayList<ResourceSelectListener> selectListeners = new ArrayList<ResourceSelectListener>();

    /**
     * A list of all Resources in the system.
     */
    public ResourceSearchView(String locatorId) {
        this(locatorId, null);
    }

    public ResourceSearchView(String locatorId, String title, String[] excludeFields) {
        this(locatorId, null, title, null, excludeFields);
    }

    /**
     * A Resource list filtered by a given criteria.
     */
    public ResourceSearchView(String locatorId, Criteria criteria) {
        this(locatorId, criteria, DEFAULT_TITLE);
    }

    public ResourceSearchView(String locatorId, Criteria criteria, String title, String... headerIcons) {
        this(locatorId, criteria, title, null, null, headerIcons);
    }

    /**
     * A Resource list filtered by a given criteria with the given title.
     */
    public ResourceSearchView(String locatorId, Criteria criteria, String title, SortSpecifier[] sortSpecifier,
        String[] excludeFields, String... headerIcons) {
        super(locatorId, title, criteria, sortSpecifier, excludeFields);

        for (String headerIcon : headerIcons) {
            addHeaderIcon(headerIcon);
        }

        //        DynamicForm searchPanel = new DynamicForm();
        //        final TextItem searchBox = new TextItem("query", "Search Resources");
        //        searchBox.setValue("");
        //        searchPanel.setWrapItemTitles(false);
        //        searchPanel.setFields(searchBox);

        final ResourceDatasource datasource = new ResourceDatasource();
        setDataSource(datasource);
    }

    @Override
    protected void configureTable() {

        ListGridField iconField = new ListGridField("icon", "Icon", 40);
        iconField.setType(ListGridFieldType.IMAGE);
        iconField.setImageURLPrefix("types/");

        ListGridField nameField = new ListGridField(ResourceDataSourceField.NAME.propertyName(),
            ResourceDataSourceField.NAME.title(), 250);
        nameField.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                return "<a href=\"" + LinkManager.getResourceLink(listGridRecord.getAttributeAsInt("id")) + "\">" + o
                    + "</a>";
            }
        });

        ListGridField descriptionField = new ListGridField(ResourceDataSourceField.DESCRIPTION.propertyName(),
            ResourceDataSourceField.DESCRIPTION.title());

        ListGridField typeNameField = new ListGridField(ResourceDataSourceField.TYPE.propertyName(),
            ResourceDataSourceField.TYPE.title(), 130);

        ListGridField pluginNameField = new ListGridField(ResourceDataSourceField.PLUGIN.propertyName(),
            ResourceDataSourceField.PLUGIN.title(), 100);

        ListGridField categoryField = new ListGridField(ResourceDataSourceField.CATEGORY.propertyName(),
            ResourceDataSourceField.CATEGORY.title(), 60);

        ListGridField availabilityField = new ListGridField(ResourceDataSourceField.AVAILABILITY.propertyName(),
            ResourceDataSourceField.AVAILABILITY.title(), 55);
        availabilityField.setType(ListGridFieldType.IMAGE);
        availabilityField.setAlign(Alignment.CENTER);

        setListGridFields(iconField, nameField, descriptionField, typeNameField, pluginNameField, categoryField,
            availabilityField);

        addTableAction(extendLocatorId("Uninventory"), "Uninventory", Table.SelectionEnablement.ANY,
            "Are you sure you want to uninventory # resources?", new TableAction() {
                public void executeAction(ListGridRecord[] selection) {
                    int[] resourceIds = TableUtility.getIds(selection);
                    ResourceGWTServiceAsync resourceManager = GWTServiceLookup.getResourceService();

                    resourceManager.uninventoryResources(resourceIds, new AsyncCallback<List<Integer>>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError("Failed to uninventory selected resources", caught);
                        }

                        public void onSuccess(List<Integer> result) {
                            CoreGUI.getMessageCenter()
                                .notify(
                                    new Message("Successfully uninventoried " + result.size() + " resources",
                                        Severity.Info));

                            ResourceSearchView.this.refresh();
                        }
                    });
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
                    com.allen_sauer.gwt.log.client.Log.info("Loaded in: " + (System.currentTimeMillis() - start));
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

    public static ResourceSearchView getChildrenOf(String locatorId, int resourceId) {
        return new ResourceSearchView(locatorId, new Criteria("parentId", String.valueOf(resourceId)),
            "Child Resources");
    }

    public static ResourceSearchView getMembersOf(String locatorId, int groupId) {
        return new ResourceSearchView(locatorId, new Criteria("groupId", String.valueOf(groupId)), "Member Resources");
    }

}
