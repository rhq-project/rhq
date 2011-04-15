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
package org.rhq.enterprise.gui.coregui.client.inventory.resource;

import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.widgets.events.DoubleClickEvent;
import com.smartgwt.client.widgets.events.DoubleClickHandler;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.search.SearchSubsystem;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.components.table.EscapedHtmlCellFormatter;
import org.rhq.enterprise.gui.coregui.client.components.table.RecordExtractor;
import org.rhq.enterprise.gui.coregui.client.components.table.ResourceAuthorizedTableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.IconField;
import org.rhq.enterprise.gui.coregui.client.components.table.ResourceCategoryCellFormatter;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableActionEnablement;
import org.rhq.enterprise.gui.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.StringUtility;
import org.rhq.enterprise.gui.coregui.client.util.TableUtility;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * The list view for {@link Resource}s.
 *
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
    @SuppressWarnings("unchecked")
    public ResourceSearchView(String locatorId, Criteria criteria, String title, SortSpecifier[] sortSpecifier,
        String[] excludeFields, String... headerIcons) {
        super(locatorId, title, criteria, sortSpecifier, excludeFields);

        for (String headerIcon : headerIcons) {
            addHeaderIcon(headerIcon);
        }

        final RPCDataSource<Resource, ResourceCriteria> datasource = getDataSourceInstance();
        setDataSource(datasource);
    }

    // surpress unchecked warnings because the subclasses may have different generic types for the datasource
    @SuppressWarnings("unchecked")
    protected RPCDataSource getDataSourceInstance() {
        return ResourceDatasource.getInstance();
    }

    @Override
    protected void configureTable() {
        List<ListGridField> fields = createFields();
        setListGridFields(fields.toArray(new ListGridField[fields.size()]));

        addTableAction(extendLocatorId("Uninventory"), MSG.common_button_uninventory(), MSG
            .view_inventory_resources_uninventoryConfirm(), new ResourceAuthorizedTableAction(ResourceSearchView.this,
            TableActionEnablement.ANY, Permission.DELETE_RESOURCE, new RecordExtractor<Integer>() {

                public Collection<Integer> extract(Record[] records) {
                    List<Integer> result = new ArrayList<Integer>(records.length);
                    for (Record record : records) {
                        result.add(record.getAttributeAsInt("id"));
                    }

                    return result;
                }
            }) {

            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                int[] resourceIds = TableUtility.getIds(selection);
                ResourceGWTServiceAsync resourceManager = GWTServiceLookup.getResourceService();

                resourceManager.uninventoryResources(resourceIds, new AsyncCallback<List<Integer>>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_inventory_resources_uninventoryFailed(), caught);
                    }

                    public void onSuccess(List<Integer> result) {
                        CoreGUI.getMessageCenter().notify(
                            new Message(MSG.view_inventory_resources_uninventorySuccessful(), Severity.Info));

                        ResourceSearchView.this.refresh();
                    }
                });
            }
        });

        setListGridDoubleClickHandler(new DoubleClickHandler() {
            public void onDoubleClick(DoubleClickEvent event) {
                ListGrid listGrid = (ListGrid) event.getSource();
                ListGridRecord[] selectedRows = listGrid.getSelection();
                if (selectedRows != null && selectedRows.length == 1) {
                    String selectedId = selectedRows[0].getAttribute("id");
                    CoreGUI.goToView(LinkManager.getResourceLink(Integer.valueOf(selectedId)));
                }
            }
        });
    }

    protected List<ListGridField> createFields() {
        List<ListGridField> fields = new ArrayList<ListGridField>();

        IconField iconField = new IconField();
        iconField.setShowHover(true);
        iconField.setHoverCustomizer(new HoverCustomizer() {
            public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                String resCat = record.getAttribute(CATEGORY.propertyName());
                switch (ResourceCategory.valueOf(resCat)) {
                    case PLATFORM:
                        return MSG.common_title_platform();
                    case SERVER:
                        return MSG.common_title_server();
                    case SERVICE:
                        return MSG.common_title_service();
                }
                return null;
            }
        });
        fields.add(iconField);

        ListGridField nameField = new ListGridField(NAME.propertyName(), NAME.title(), 250);
        nameField.setCellFormatter(new CellFormatter() {
            public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                String url = LinkManager.getResourceLink(record.getAttributeAsInt("id"));
                String name = StringUtility.escapeHtml(value.toString());
                return SeleniumUtility.getLocatableHref(url, name, null);
            }
        });
        nameField.setShowHover(true);
        nameField.setHoverCustomizer(new HoverCustomizer() {
            public String hoverHTML(Object value, ListGridRecord listGridRecord, int rowNum, int colNum) {
                return AncestryUtil.getResourceHoverHTML(listGridRecord, 0);
            }
        });
        fields.add(nameField);

        ListGridField keyField = new ListGridField(KEY.propertyName(), KEY.title(), 170);
        fields.add(keyField);

        ListGridField ancestryField = AncestryUtil.setupAncestryListGridField();
        fields.add(ancestryField);

        ListGridField descriptionField = new ListGridField(DESCRIPTION.propertyName(), DESCRIPTION.title());
        descriptionField.setCellFormatter(new EscapedHtmlCellFormatter());
        fields.add(descriptionField);

        ListGridField locationField = new ListGridField(LOCATION.propertyName(), LOCATION.title(), 180);
        locationField.setCellFormatter(new EscapedHtmlCellFormatter());
        fields.add(locationField);

        ListGridField typeNameField = new ListGridField(TYPE.propertyName(), TYPE.title(), 130);
        fields.add(typeNameField);

        ListGridField pluginNameField = new ListGridField(PLUGIN.propertyName(), PLUGIN.title(), 100);
        fields.add(pluginNameField);

        ListGridField versionField = new ListGridField(VERSION.propertyName(), VERSION.title(), 60);
        fields.add(versionField);

        ListGridField categoryField = new ListGridField(CATEGORY.propertyName(), CATEGORY.title(), 60);
        categoryField.setCellFormatter(new ResourceCategoryCellFormatter());
        fields.add(categoryField);

        IconField availabilityField = new IconField(AVAILABILITY.propertyName(), AVAILABILITY.title(), 70);
        fields.add(availabilityField);

        ListGridField ctimeField = new ListGridField(CTIME.propertyName(), CTIME.title(), 120);
        ctimeField.setCellFormatter(new TimestampCellFormatter());
        fields.add(ctimeField);

        ListGridField itimeField = new ListGridField(ITIME.propertyName(), ITIME.title(), 120);
        itimeField.setCellFormatter(new TimestampCellFormatter());
        fields.add(itimeField);

        ListGridField mtimeField = new ListGridField(MTIME.propertyName(), MTIME.title(), 120);
        mtimeField.setCellFormatter(new TimestampCellFormatter());
        fields.add(mtimeField);

        ListGridField modifiedByField = new ListGridField(MODIFIER.propertyName(), MODIFIER.title(), 100);
        fields.add(modifiedByField);

        return fields;
    }

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
