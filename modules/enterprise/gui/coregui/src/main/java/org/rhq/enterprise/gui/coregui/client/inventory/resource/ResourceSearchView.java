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

import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.AVAILABILITY;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.CATEGORY;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.CTIME;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.DESCRIPTION;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.ITIME;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.KEY;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.LOCATION;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.MODIFIER;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.MTIME;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.NAME;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.PLUGIN;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.TYPE;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.VERSION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.SortDirection;
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
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.search.SearchSubsystem;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.components.ReportExporter;
import org.rhq.enterprise.gui.coregui.client.components.table.EscapedHtmlCellFormatter;
import org.rhq.enterprise.gui.coregui.client.components.table.IconField;
import org.rhq.enterprise.gui.coregui.client.components.table.RecordExtractor;
import org.rhq.enterprise.gui.coregui.client.components.table.ResourceAuthorizedTableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.ResourceCategoryCellFormatter;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableActionEnablement;
import org.rhq.enterprise.gui.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.report.DriftComplianceReportResourceSearchView;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.StringUtility;
import org.rhq.enterprise.gui.coregui.client.util.TableUtility;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

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
    public ResourceSearchView(String locatorId) {
        this(locatorId, null, null, null, null, false);
    }

    /**
     * A Resource list filtered by a given criteria.
     */
    public ResourceSearchView(String locatorId, Criteria criteria) {
        this(locatorId, criteria, null, null, null, false);
    }

    /**
     * A Resource list filtered by a given criteria and optionally exportable
     */
    public ResourceSearchView(String locatorId, Criteria criteria, boolean exportable) {
        this(locatorId, criteria, null, null, null, exportable);
    }

    /**
     * A Resource list filtered by a given criteria with the given title.
     *
     * @param headerIcons 24x24 icon(s) to be displayed in the header
     */
    public ResourceSearchView(String locatorId, Criteria criteria, String title, String... headerIcons) {
        this(locatorId, criteria, title, null, null, false, headerIcons);
    }

    /**
     * A Resource list filtered by a given criteria with the given title.
     *
     * @param headerIcons 24x24 icon(s) to be displayed in the header
     */
    public ResourceSearchView(String locatorId, Criteria criteria, String title, SortSpecifier[] sortSpecifier,
        String[] excludeFields, String... headerIcons) {

        this(locatorId, criteria, title, sortSpecifier, excludeFields, false, headerIcons);
    }

    /**
     * A Resource list filtered by a given criteria with the given title and optionally exportable.
     *
     * @param headerIcons 24x24 icon(s) to be displayed in the header
     */
    public ResourceSearchView(String locatorId, Criteria criteria, String title, SortSpecifier[] sortSpecifier,
        String[] excludeFields, boolean exportable, String... headerIcons) {

        super(locatorId, (null == title) ? DEFAULT_TITLE : title, criteria,
            (null == sortSpecifier) ? DEFAULT_SORT_SPECIFIER : sortSpecifier, excludeFields);

        this.exportable = exportable;

        for (String headerIcon : headerIcons) {
            addHeaderIcon(headerIcon);
        }

        final RPCDataSource<Resource, ResourceCriteria> datasource = getDataSourceInstance();
        setDataSource(datasource);
    }

    // suppress unchecked warnings because the subclasses may have different generic types for the datasource
    protected RPCDataSource getDataSourceInstance() {
        return ResourceDatasource.getInstance();
    }

    @Override
    protected void configureTable() {
        List<ListGridField> fields = createFields();
        setListGridFields(fields.toArray(new ListGridField[fields.size()]));

        addTableAction(extendLocatorId("Uninventory"), MSG.common_button_uninventory(),
            MSG.view_inventory_resources_uninventoryConfirm(), new ResourceAuthorizedTableAction(
                ResourceSearchView.this, TableActionEnablement.ANY, Permission.DELETE_RESOURCE,
                new RecordExtractor<Integer>() {

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
                            CoreGUI.getErrorHandler().handleError(MSG.view_inventory_resources_uninventoryFailed(),
                                caught);
                            refreshTableInfo();
                        }

                        public void onSuccess(List<Integer> result) {
                            CoreGUI.getMessageCenter().notify(
                                new Message(MSG.view_inventory_resources_uninventorySuccessful(), Severity.Info));
                            onUninventorySuccess();
                        }
                    });
                }
            });

        addTableAction(extendLocatorId("Disable"), MSG.common_button_disable(),
            MSG.view_inventory_resources_disableConfirm(),
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
                            onUninventorySuccess();
                        }
                    });
                }
            });

        addTableAction(extendLocatorId("Enable"), MSG.common_button_enable(),
            MSG.view_inventory_resources_enableConfirm(), new AvailabilityTypeResourceAuthorizedTableAction(
                ResourceSearchView.this, TableActionEnablement.ANY, EnumSet.of(AvailabilityType.DISABLED),
                Permission.DELETE_RESOURCE, new RecordExtractor<AvailabilityType>() {

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
                            onUninventorySuccess();
                        }
                    });
                }
            });

        if (exportable) {
            addExportAction();
        }

        setListGridDoubleClickHandler(new DoubleClickHandler() {
            public void onDoubleClick(DoubleClickEvent event) {
                ListGrid listGrid = (ListGrid) event.getSource();
                ListGridRecord[] selectedRows = listGrid.getSelectedRecords();
                if (selectedRows != null && selectedRows.length == 1) {
                    String selectedId = selectedRows[0].getAttribute("id");
                    CoreGUI.goToView(LinkManager.getResourceLink(Integer.valueOf(selectedId)));
                }
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

    protected void onUninventorySuccess() {
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
