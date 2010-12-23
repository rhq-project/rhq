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

import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.AVAILABILITY;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.CATEGORY;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.DESCRIPTION;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.NAME;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.PLUGIN;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.TYPE;

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

import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.search.SearchSubsystem;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.components.table.AbstractTableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableActionEnablement;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.TableUtility;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;

/**
 * @author Greg Hinkle
 */
public class ResourceSearchView extends Table {

    private static final String DEFAULT_TITLE = MSG.view_inventory_resources_title();

    private List<ResourceSelectListener> selectListeners = new ArrayList<ResourceSelectListener>();

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

    /**
     * A Resource list filtered by a given criteria with the given title.
     *
     * @param headerIcons 24x24 icon(s) to be displayed in the header
     */
    public ResourceSearchView(String locatorId, Criteria criteria, String title, String... headerIcons) {
        this(locatorId, criteria, title, null, null, headerIcons);
    }

    /**
     * A Resource list filtered by a given criteria with the given title.
     *
     * @param headerIcons 24x24 icon(s) to be displayed in the header
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

        final RPCDataSource datasource = getDataSourceInstance();
        setDataSource(datasource);
    }

    protected RPCDataSource getDataSourceInstance() {
        return ResourceDatasource.getInstance();
    }

    @Override
    protected void configureTable() {
        ListGridField iconField = new ListGridField("icon", MSG.common_title_icon(), 26);
        iconField.setType(ListGridFieldType.IMAGE);
        iconField.setImageURLPrefix("types/");
        iconField.setShowDefaultContextMenu(false);
        iconField.setCanSort(false);
        iconField.setTitle("&nbsp;");

        ListGridField nameField = new ListGridField(NAME.propertyName(), NAME.title(), 250);
        nameField.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                return "<a href=\"" + LinkManager.getResourceLink(listGridRecord.getAttributeAsInt("id")) + "\">" + o
                    + "</a>";
            }
        });

        ListGridField descriptionField = new ListGridField(DESCRIPTION.propertyName(), DESCRIPTION.title());

        ListGridField typeNameField = new ListGridField(TYPE.propertyName(), TYPE.title(), 130);

        ListGridField pluginNameField = new ListGridField(PLUGIN.propertyName(), PLUGIN.title(), 100);

        ListGridField categoryField = new ListGridField(CATEGORY.propertyName(), CATEGORY.title(), 60);
        categoryField.setCellFormatter(new CellFormatter() {
            public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                String categoryName = (String) value;
                ResourceCategory category = ResourceCategory.valueOf(categoryName);
                String displayName = "";
                switch (category) {
                case PLATFORM:
                    displayName = MSG.common_title_platform();
                    break;
                case SERVER:
                    displayName = MSG.common_title_server();
                    break;
                case SERVICE:
                    displayName = MSG.common_title_service();
                    break;
                }
                return displayName;
            }
        });

        ListGridField availabilityField = new ListGridField(AVAILABILITY.propertyName(), AVAILABILITY.title(), 70);
        availabilityField.setType(ListGridFieldType.IMAGE);
        availabilityField.setAlign(Alignment.CENTER);

        setListGridFields(iconField, nameField, descriptionField, typeNameField, pluginNameField, categoryField,
            availabilityField);

        addTableAction(extendLocatorId("Uninventory"), MSG.common_button_uninventory(), MSG
            .view_inventory_resources_deleteConfirm(), new AbstractTableAction(TableActionEnablement.ANY) {
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                int[] resourceIds = TableUtility.getIds(selection);
                ResourceGWTServiceAsync resourceManager = GWTServiceLookup.getResourceService();

                resourceManager.uninventoryResources(resourceIds, new AsyncCallback<List<Integer>>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_inventory_resources_deleteFailed(), caught);
                    }

                    public void onSuccess(List<Integer> result) {
                        CoreGUI.getMessageCenter().notify(
                            new Message(MSG.view_inventory_resources_deleteSuccessful(), Severity.Info));

                        ResourceSearchView.this.refresh();
                    }
                });
            }
        });

        //        //load double click handler for this table
        //        configureDoubleClickHandler();

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

    //    /** Defines the double click handler action for ResourceSearch.  This means that on double
    //     * click a (Resource-relative) url, specifically Summary/Overview is what will happen
    //     * with this action. Override in subclasses to define alternate behavior.
    //     */
    //    protected void configureDoubleClickHandler() {
    //        //adding cell double click handler
    //        getListGrid().addCellDoubleClickHandler(new CellDoubleClickHandler() {
    //            @Override
    //            public void onCellDoubleClick(CellDoubleClickEvent event) {
    //                CoreGUI.goToView("Resource/" + event.getRecord().getAttribute("id") + "/Summary/Overview");
    //            }
    //        });
    //    }

    public int getMatches() {
        return this.getListGrid().getTotalRows();
    }

    public void addResourceSelectedListener(ResourceSelectListener listener) {
        selectListeners.add(listener);
    }

    @Override
    protected SearchSubsystem getSearchSubsystem() {
        return SearchSubsystem.RESOURCE;
    }

}
