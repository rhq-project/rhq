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
package org.rhq.coregui.client.inventory.resource;

import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.AVAILABILITY;
import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.CATEGORY;
import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.CTIME;
import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.DESCRIPTION;
import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.INVENTORY_STATUS;
import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.ITIME;
import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.KEY;
import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.LOCATION;
import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.MODIFIER;
import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.MTIME;
import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.NAME;
import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.PLUGIN;
import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.TYPE;
import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.VERSION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.events.DoubleClickEvent;
import com.smartgwt.client.widgets.events.DoubleClickHandler;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.search.SearchSubsystem;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.components.ReportExporter;
import org.rhq.coregui.client.components.table.EscapedHtmlCellFormatter;
import org.rhq.coregui.client.components.table.IconField;
import org.rhq.coregui.client.components.table.RecordExtractor;
import org.rhq.coregui.client.components.table.ResourceAuthorizedTableAction;
import org.rhq.coregui.client.components.table.ResourceCategoryCellFormatter;
import org.rhq.coregui.client.components.table.Table;
import org.rhq.coregui.client.components.table.TableAction;
import org.rhq.coregui.client.components.table.TableActionEnablement;
import org.rhq.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.coregui.client.report.DriftComplianceReportResourceSearchView;
import org.rhq.coregui.client.util.RPCDataSource;
import org.rhq.coregui.client.util.StringUtility;
import org.rhq.coregui.client.util.TableUtility;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.message.Message.Severity;

/**
 * The list view for {@link Resource}s. If not specified a default title is assigned.  If not specified the list will
 * be initially sorted by resource name, ascending.
 *
 * @author Jay Shaughnessy
 * @author Greg Hinkle
 */
@SuppressWarnings("unchecked")
public class ResourceSearchView extends Table {

    private static final String DEFAULT_TITLE = MSG.common_title_resources();
    private static final SortSpecifier[] DEFAULT_SORT_SPECIFIER = new SortSpecifier[] { new SortSpecifier("name",
        SortDirection.ASCENDING) };

    private List<ResourceSelectListener> selectListeners = new ArrayList<ResourceSelectListener>();

    private boolean exportable;

    /**
     * A list of all Resources in the system.
     */
    public ResourceSearchView() {
        this(null, null, null, null, false, null);
    }

    /**
     * A Resource list filtered by a given criteria.
     */
    public ResourceSearchView(Criteria criteria) {
        this(criteria, null, null, null, false, null);
    }

    /**
     * A Resource list filtered by a given criteria and optionally exportable
     */
    public ResourceSearchView(Criteria criteria, boolean exportable) {
        this(criteria, null, null, null, exportable, null);
    }

    /**
     * A Resource list filtered by a given criteria with the given title.
     *
     * @param headerIcon
     */
    public ResourceSearchView(Criteria criteria, String title, String headerIcon) {
        this(criteria, title, null, null, false, headerIcon);
    }

    /**
     * A Resource list filtered by a given criteria with the given title.
     *
     * @param headerIcon
     */
    public ResourceSearchView(Criteria criteria, String title, SortSpecifier[] sortSpecifier, String[] excludeFields,
        String headerIcon) {

        this(criteria, title, sortSpecifier, excludeFields, false, headerIcon);
    }

    /**
     * A Resource list filtered by a given criteria with the given title and optionally exportable.
     *
     * @param headerIcon
     */
    public ResourceSearchView(Criteria criteria, String title, SortSpecifier[] sortSpecifier, String[] excludeFields,
        boolean exportable, String headerIcon) {

        super((null == title) ? DEFAULT_TITLE : title, criteria, (null == sortSpecifier) ? DEFAULT_SORT_SPECIFIER
            : sortSpecifier, excludeFields);

        this.exportable = exportable;

        if (headerIcon != null) {
            setTitleIcon(headerIcon);
        }

        final RPCDataSource<Resource, ResourceCriteria> datasource = getDataSourceInstance();
        setDataSource(datasource);
        setStyleName("resourcesearchlist");
    }

    // suppress unchecked warnings because the subclasses may have different generic types for the datasource
    protected RPCDataSource getDataSourceInstance() {
        return ResourceDatasource.getInstance();
    }

    @Override
    protected void configureTable() {
        List<ListGridField> fields = createFields();
        setListGridFields(fields.toArray(new ListGridField[fields.size()]));

        addTableAction(MSG.common_button_uninventory(), MSG.view_inventory_resources_uninventoryConfirm(),
            new ResourceAuthorizedTableAction(ResourceSearchView.this, TableActionEnablement.ANY,
                Permission.DELETE_RESOURCE, new RecordExtractor<Integer>() {

                    public Collection<Integer> extract(Record[] records) {
                        List<Integer> result = new ArrayList<Integer>(records.length);
                        for (Record record : records) {
                            result.add(record.getAttributeAsInt("id"));
                        }

                        return result;
                    }
                }) {

                public void executeAction(final ListGridRecord[] selection, Object actionValue) {
                    if (containsStorageNodeOrItsResource(selection)) {
                        // ask again if we are going to remove platform, storage node or its child
                        SC.confirm(MSG.view_inventory_resources_uninventoryStorageConfirm(),
                            new BooleanCallback() {
                            public void execute(Boolean test) {
                                if (test) uninventoryItems(selection);
                            }
                        });
                    } else {
                        uninventoryItems(selection);
                    }
                }

                private void uninventoryItems(ListGridRecord[] selection) {
                    int[] resourceIds = TableUtility.getIds(selection);
                    ResourceGWTServiceAsync resourceManager = GWTServiceLookup.getResourceService();

                    resourceManager.uninventoryResources(resourceIds, new AsyncCallback<List<Integer>>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError(MSG.view_inventory_resources_uninventoryFailed(),
                                caught);
                            refreshTableInfo();
                        }

                        public void onSuccess(List<Integer> result) {
                            CoreGUI.getMessageCenter().notify(
                                new Message(MSG.view_inventory_resources_uninventorySuccessful(), Severity.Info));
                            onActionSuccess();
                        }
                    });
                }

                private boolean containsStorageNodeOrItsResource(ListGridRecord[] selection) {
                    for (ListGridRecord record : selection) {
                        if (record.getAttribute(AncestryUtil.RESOURCE_ANCESTRY) == null
                            || "RHQStorage".equals(record.getAttribute(PLUGIN.propertyName()))) {
                         // is a platform, storage node or child resource of storage node
                            return true;
                        }
                    }
                    return false;
                }
            });

        addTableAction(MSG.common_button_disable(), MSG.view_inventory_resources_disableConfirm(),
            new AvailabilityTypeResourceAuthorizedTableAction(ResourceSearchView.this, TableActionEnablement.ANY,
                EnumSet.complementOf(EnumSet.of(AvailabilityType.DISABLED)), Permission.DELETE_RESOURCE,
                new RecordExtractor<AvailabilityType>() {

                    public Collection<AvailabilityType> extract(Record[] records) {
                        List<AvailabilityType> result = new ArrayList<AvailabilityType>(records.length);
                        for (Record record : records) {
                            result.add(((Resource) record.getAttributeAsObject("resource")).getCurrentAvailability()
                                .getAvailabilityType());
                        }

                        return result;
                    }
                }, //
                new RecordExtractor<Integer>() {

                    public Collection<Integer> extract(Record[] records) {
                        List<Integer> result = new ArrayList<Integer>(records.length);
                        for (Record record : records) {
                            result.add(record.getAttributeAsInt("id"));
                        }

                        return result;
                    }
                }) {

                @Override
                public boolean isEnabled(ListGridRecord[] records) {
                    boolean result = super.isEnabled(records);

                    if (result) {
                        for (Record record : records) {
                            if (record.getAttribute(ResourceDataSourceField.CATEGORY.propertyName()).equals(
                                ResourceCategory.PLATFORM.name())) {
                                result = false;
                                break;
                            }
                        }
                    }

                    return result;
                }

                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    int[] resourceIds = TableUtility.getIds(selection);
                    ResourceGWTServiceAsync resourceManager = GWTServiceLookup.getResourceService();

                    resourceManager.disableResources(resourceIds, new AsyncCallback<List<Integer>>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError(MSG.view_inventory_resources_disableFailed(), caught);
                            refreshTableInfo();
                        }

                        public void onSuccess(List<Integer> result) {
                            CoreGUI.getMessageCenter().notify(
                                new Message(
                                    MSG.view_inventory_resources_disableSuccessful(String.valueOf(result.size())),
                                    Severity.Info));
                            onActionSuccess();
                        }
                    });
                }
            });

        addTableAction(MSG.common_button_enable(), MSG.view_inventory_resources_enableConfirm(),
            new AvailabilityTypeResourceAuthorizedTableAction(ResourceSearchView.this, TableActionEnablement.ANY,
                EnumSet.of(AvailabilityType.DISABLED), Permission.DELETE_RESOURCE,
                new RecordExtractor<AvailabilityType>() {

                    public Collection<AvailabilityType> extract(Record[] records) {
                        List<AvailabilityType> result = new ArrayList<AvailabilityType>(records.length);
                        for (Record record : records) {
                            result.add(((Resource) record.getAttributeAsObject("resource")).getCurrentAvailability()
                                .getAvailabilityType());
                        }

                        return result;
                    }
                }, //
                new RecordExtractor<Integer>() {

                    public Collection<Integer> extract(Record[] records) {
                        List<Integer> result = new ArrayList<Integer>(records.length);
                        for (Record record : records) {
                            result.add(record.getAttributeAsInt("id"));
                        }

                        return result;
                    }
                }) {

                @Override
                public boolean isEnabled(ListGridRecord[] records) {
                    boolean result = super.isEnabled(records);

                    if (result) {
                        for (Record record : records) {
                            if (record.getAttribute(ResourceDataSourceField.CATEGORY.propertyName()).equals(
                                ResourceCategory.PLATFORM.name())) {
                                result = false;
                                break;
                            }
                        }
                    }

                    return result;
                }

                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    int[] resourceIds = TableUtility.getIds(selection);
                    ResourceGWTServiceAsync resourceManager = GWTServiceLookup.getResourceService();

                    resourceManager.enableResources(resourceIds, new AsyncCallback<List<Integer>>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError(MSG.view_inventory_resources_enableFailed(), caught);
                            refreshTableInfo();
                        }

                        public void onSuccess(List<Integer> result) {
                            CoreGUI.getMessageCenter().notify(
                                new Message(
                                    MSG.view_inventory_resources_enableSuccessful(String.valueOf(result.size())),
                                    Severity.Info));
                            onActionSuccess();
                        }
                    });
                }
            });

        if (shouldShowIgnoreButton()) {
            addIgnoreButton();
        }

        if (shouldShowUnignoreButton()) {
            addUnignoreButton();
        }

        if (exportable) {
            addExportAction();
        }

        setListGridDoubleClickHandler(new DoubleClickHandler() {
            public void onDoubleClick(DoubleClickEvent event) {
                ListGrid listGrid = (ListGrid) event.getSource();
                ListGridRecord[] selectedRows = listGrid.getSelectedRecords();
                if (selectedRows != null && selectedRows.length == 1) {
                    String invStatus = selectedRows[0].getAttribute(INVENTORY_STATUS.propertyName());
                    if (InventoryStatus.COMMITTED == InventoryStatus.valueOf(invStatus)) {
                        String selectedId = selectedRows[0].getAttribute("id");
                        CoreGUI.goToView(LinkManager.getResourceLink(Integer.valueOf(selectedId)));
                    }
                }
            }
        });
    }

    /**
     * If returns true, the ignore button will be added to the list of table buttons.
     * Subclasses can remove the ignore button by overriding this and returning false.
     * The only time you normally do not want an ignore button is if you know your
     * criteria will only ever show ignored resources (in which case, its useless
     * showing an ignore button since everything will already have been ignored).
     */
    protected boolean shouldShowIgnoreButton() {
        return true; // by default, show the ignore button
    }

    /**
     * If returns true, the unignore button will be added to the list of table buttons.
     * Subclasses can add the unignore button by overriding this and returning true.
     * The only time you normally want an unignore button is if you know your
     * criteria will only ever show ignored resources (so it makes sense to allow someone
     * to unignore those resources).
     */
    protected boolean shouldShowUnignoreButton() {
        return false; // by default, do not show the unignore button
    }

    /**
     * Adds an ignore button to the list of table buttons. Subclasses normally do not have to override
     * this - instead, look at {@link #shouldShowIgnoreButton()}.
     */
    protected void addIgnoreButton() {
        addTableAction(MSG.common_button_ignore(), MSG.view_inventory_resources_ignoreConfirm(),
            new ResourceAuthorizedTableAction(ResourceSearchView.this, TableActionEnablement.ANY,
                Permission.DELETE_RESOURCE, new RecordExtractor<Integer>() {
                    public Collection<Integer> extract(Record[] records) {
                        List<Integer> result = new ArrayList<Integer>(records.length);
                        for (Record record : records) {
                            // we only care about those that are not platforms since you cannot ignore platforms outside of discovery queue anyway
                            String resCat = record.getAttribute(CATEGORY.propertyName());
                            if (ResourceCategory.valueOf(resCat) != ResourceCategory.PLATFORM) {
                                result.add(record.getAttributeAsInt("id"));
                            }
                        }
                        return result;
                    }
                }) {

                public boolean isEnabled(ListGridRecord[] selection) {
                    if (selection == null || selection.length == 0) {
                        return false;
                    }

                    // do not enable the ignore button if everything selected is a platform
                    boolean nonPlatformSelected = false;
                    for (Record record : selection) {
                        // we only care about those that are not platforms since you cannot ignore platforms outside of discovery queue anyway
                        String resCat = record.getAttribute(CATEGORY.propertyName());
                        if (ResourceCategory.valueOf(resCat) != ResourceCategory.PLATFORM) {
                            nonPlatformSelected = true;
                            break;
                        }
                    }

                    if (!nonPlatformSelected) {
                        return false; // everything selected is a platform - you can't ignore them
                    }

                    // at least one non-platform was selected, let's ask what our superclass thinks
                    return super.isEnabled(selection);
                }

                public void executeAction(final ListGridRecord[] selection, Object actionValue) {
                    if (selection == null || selection.length == 0) {
                        return; // should never happen, but ignore it if it does
                    }

                    final int[] numberOfPlatformsSelected = new int[] { 0 };
                    final ArrayList<Integer> resourceIdsList = new ArrayList<Integer>(selection.length);
                    for (ListGridRecord selectedRecord : selection) {
                        // we only include those that are not platforms (you cannot ignore platforms outside of discovery queue)
                        String resCat = selectedRecord.getAttribute(CATEGORY.propertyName());
                        if (ResourceCategory.valueOf(resCat) != ResourceCategory.PLATFORM) {
                            resourceIdsList.add(selectedRecord.getAttributeAsInt("id"));
                        } else {
                            numberOfPlatformsSelected[0]++;
                        }
                    }

                    if (resourceIdsList.isEmpty()) {
                        CoreGUI.getMessageCenter().notify(
                            new Message(MSG.view_inventory_resources_ignoreSkipAllPlatforms(), Severity.Warning));
                        return;
                    } else if (numberOfPlatformsSelected[0] > 0) {
                        String n = Integer.toString(numberOfPlatformsSelected[0]);
                        CoreGUI.getMessageCenter().notify(
                            new Message(MSG.view_inventory_resources_ignoreSkipSomePlatforms(n), Severity.Warning));
                    }

                    // the remote API requires int[]
                    int[] resourceIds = new int[resourceIdsList.size()];
                    int i = 0;
                    for (Integer id : resourceIdsList) {
                        resourceIds[i++] = id;
                    }

                    // ask the server to ignore the selected non-platform resources
                    ResourceGWTServiceAsync resourceManager = GWTServiceLookup.getResourceService();
                    resourceManager.ignoreResources(resourceIds, new AsyncCallback<Void>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError(MSG.view_inventory_resources_ignoreFailed(), caught);
                            refreshTableInfo();
                        }

                        public void onSuccess(Void result) {
                            String msg;
                            if (numberOfPlatformsSelected[0] > 0) {
                                String n = Integer.toString(numberOfPlatformsSelected[0]);
                                msg = MSG.view_inventory_resources_ignoreSuccessfulSkipPlatforms(n);
                            } else {
                                msg = MSG.view_inventory_resources_ignoreSuccessful();
                            }
                            CoreGUI.getMessageCenter().notify(new Message(msg, Severity.Info));
                            onActionSuccess(); // do the same thing as if we uninventoried the resource
                        }
                    });

                    return;
                }
            });
    }

    /**
     * Adds an unignore button to the list of table buttons. Subclasses normally do not have to override
     * this - instead, look at {@link #shouldShowUnignoreButton()}.
     */
    protected void addUnignoreButton() {
        addTableAction(MSG.common_button_unignore(), MSG.view_inventory_resources_unignoreConfirm(),
            new ResourceAuthorizedTableAction(ResourceSearchView.this, TableActionEnablement.ANY,
                Permission.DELETE_RESOURCE, new RecordExtractor<Integer>() {
                    public Collection<Integer> extract(Record[] records) {
                        List<Integer> result = new ArrayList<Integer>(records.length);
                        for (Record record : records) {
                            result.add(record.getAttributeAsInt("id"));
                        }

                        return result;
                    }
                }) {

                public void executeAction(final ListGridRecord[] selection, Object actionValue) {
                    if (selection == null || selection.length == 0) {
                        return; // should never happen, but ignore it if it does
                    }

                    int[] resourceIds = TableUtility.getIds(selection);

                    // ask the server to unignore the selected resources and immediately commit them to inventory again
                    ResourceGWTServiceAsync resourceManager = GWTServiceLookup.getResourceService();
                    resourceManager.unignoreAndImportResources(resourceIds, new AsyncCallback<Void>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler()
                                .handleError(MSG.view_inventory_resources_unignoreFailed(), caught);
                            refreshTableInfo();
                        }

                        public void onSuccess(Void result) {
                            String msg = MSG.view_inventory_resources_unignoreSuccessful();
                            CoreGUI.getMessageCenter().notify(new Message(msg, Severity.Info));
                            onActionSuccess();
                        }
                    });

                    return;
                }
            });
    }

    private void addExportAction() {
        addTableAction("Export", MSG.common_button_reports_export(), new TableAction() {
            @Override
            public boolean isEnabled(ListGridRecord[] selection) {
                return true;
            }

            @Override
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                Criteria criteria = getInitialCriteria();
                String resourceTypeId = criteria.getAttribute(ResourceDataSourceField.TYPE.propertyName());
                String version = criteria.getAttribute("version");
                String reportType;

                if (ResourceSearchView.this instanceof DriftComplianceReportResourceSearchView) {
                    reportType = "driftCompliance";
                } else {
                    reportType = "inventorySummary";
                }

                ReportExporter exportModalWindow = ReportExporter.createExporterForInventorySummary(reportType,
                    resourceTypeId, version);
                exportModalWindow.export();
                refreshTableInfo();
            }
        });
    }

    protected void onActionSuccess() {
        refresh(true);
        // the group type may have changed
        CoreGUI.refresh();
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
                String invStatus = record.getAttribute(INVENTORY_STATUS.propertyName());
                if (InventoryStatus.COMMITTED == InventoryStatus.valueOf(invStatus)) {
                    String url = LinkManager.getResourceLink(record.getAttributeAsInt("id"));
                    String name = StringUtility.escapeHtml(value.toString());
                    return LinkManager.getHref(url, name);
                } else {
                    return value.toString();
                }
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
        keyField.setCellFormatter(new EscapedHtmlCellFormatter());
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

        ListGridField inventoryStatusField = new ListGridField(INVENTORY_STATUS.propertyName(),
            INVENTORY_STATUS.title(), 100);
        fields.add(inventoryStatusField);

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
