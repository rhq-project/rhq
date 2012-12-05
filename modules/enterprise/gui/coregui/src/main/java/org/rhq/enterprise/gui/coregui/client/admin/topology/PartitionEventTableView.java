/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.admin.topology;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.cloud.PartitionEvent.ExecutionStatus;
import org.rhq.core.domain.cloud.PartitionEventType;
import org.rhq.core.domain.criteria.PartitionEventCriteria;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.IconEnum;
import org.rhq.enterprise.gui.coregui.client.admin.AdministrationView;
import org.rhq.enterprise.gui.coregui.client.components.form.EnumSelectItem;
import org.rhq.enterprise.gui.coregui.client.components.table.AuthorizedTableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableActionEnablement;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection;
import org.rhq.enterprise.gui.coregui.client.components.view.HasViewName;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.StringUtility;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * @author Jiri Kremser
 * 
 */
public class PartitionEventTableView extends TableSection<PartitionEventDatasource> implements HasViewName {

    public static final ViewName VIEW_ID = new ViewName("PartitionEvents(GWT)",
        MSG.view_adminTopology_partitionEvents() + "(GWT)", IconEnum.EVENTS);

    public static final String VIEW_PATH = AdministrationView.VIEW_ID + "/"
        + AdministrationView.SECTION_TOPOLOGY_VIEW_ID + "/" + VIEW_ID;


    private static final Criteria INITIAL_CRITERIA = new Criteria();

    private static SortSpecifier DEFAULT_SORT_SPECIFIER = new SortSpecifier(PartitionEventCriteria.SORT_FIELD_CTIME,
        SortDirection.DESCENDING);

    public PartitionEventTableView(String locatorId, String tableTitle) {
        super(locatorId, tableTitle, INITIAL_CRITERIA, new SortSpecifier[] { DEFAULT_SORT_SPECIFIER });

        setHeight100();
        setWidth100();

        setInitialCriteriaFixed(false);
        setDataSource(new PartitionEventDatasource());
    }

    @Override
    protected void configureTableFilters() {
        final EnumSelectItem statusFilter = new EnumSelectItem(PartitionEventDatasource.FILTER_EXECUTION_STATUS,
            MSG.view_adminTopology_partitionEvents_execStatusFilter(), ExecutionStatus.class, null, null);

        final EnumSelectItem typeFilter = new EnumSelectItem(PartitionEventDatasource.FILTER_EVENT_TYPE,
            MSG.view_adminTopology_partitionEvents_typeFilter(), PartitionEventType.class, null, null);

        final TextItem detail = new TextItem(PartitionEventDatasource.FILTER_EVENT_DETAIL,
            MSG.view_adminTopology_partitionEvents_detailsFilter());
        //        detail.setValue("");

        //        startDateFilter = new DateFilterItem(DateFilterItem.START_DATE_FILTER, MSG.filter_from_date());
        //        endDateFilter = new DateFilterItem(DateFilterItem.END_DATE_FILTER, MSG.filter_to_date());

//        SpacerItem spacerItem = new SpacerItem();
//        spacerItem.setColSpan(1);
//        final ButtonItem showAll = new ButtonItem("showAll", "Show All");
//        showAll.addClickHandler(new ClickHandler() {
//            public void onClick(ClickEvent event) {
//                statusFilter.init(ExecutionStatus.class, null, null);
//                typeFilter.init(PartitionEventType.class, null, null);
//                detail.setValue("");
//                refresh();
//            }
//        });

        if (isShowFilterForm()) {
            setFilterFormItems(statusFilter, detail, typeFilter);
        }
    }

    @Override
    protected void configureTable() {
        super.configureTable();
        List<ListGridField> fields = getDataSource().getListGridFields();
        ListGrid listGrid = getListGrid();
        listGrid.setFields(fields.toArray(new ListGridField[fields.size()]));
        showActions();
        
        for (ListGridField field : fields) {
            // adding the cell formatter for name field (clickable link)
            if (field.getName() == PartitionEventDatasourceField.FIELD_EVENT_TYPE.propertyName()) {
                field.setCellFormatter(new CellFormatter() {
                    @Override
                    public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                        if (value == null) {
                            return "";
                        }
                        String detailsUrl = "#" + VIEW_PATH + "/" + getId(record);
                        String formattedValue = StringUtility.escapeHtml(value.toString());
                        return SeleniumUtility.getLocatableHref(detailsUrl, formattedValue, null);
                    }
                });
            }
        }
    }

    //    @Override
    //    protected String getDetailsLinkColumnName() {
    //        return PartitionEventDatasourceField.FIELD_EVENT_TYPE.propertyName();
    //    }
    //    
    //    @Override
    //    protected CellFormatter getDetailsLinkColumnCellFormatter() {
    //        return new CellFormatter() {
    //            public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
    //                Integer recordId = getId(record);
    //                String detailsUrl = "#" + VIEW_PATH + "/" + recordId;
    //                return SeleniumUtility.getLocatableHref(detailsUrl, detailsUrl, null);
    //            }
    //        };
    //    }

    @Override
    public Canvas getDetailsView(Integer id) {
        return new PartitionEventDetailView(extendLocatorId("detailsView"), id);
    }

    private void showActions() {
        addTableAction(extendLocatorId("removeSelected"), MSG.view_adminTopology_server_removeSelected(),
            MSG.common_msg_areYouSure(), new AuthorizedTableAction(this, TableActionEnablement.ANY,
                Permission.MANAGE_SETTINGS) {
                public void executeAction(final ListGridRecord[] selections, Object actionValue) {
                    String message = "Really? Delete? For all I've done for you? ";
                    SC.ask(message, new BooleanCallback() {
                        public void execute(Boolean confirmed) {
                            if (confirmed) {
                                int[] selectedIds = getSelectedIds(selections);
                                SC.say("setting servers to maintenance mode, ids: " + selectedIds);
                                GWTServiceLookup.getCloudService().deletePartitionEvents(selectedIds,
                                    new AsyncCallback<Void>() {
                                        public void onSuccess(Void arg0) {
                                            // TODO: msg
                                            Message msg = new Message(MSG
                                                .view_admin_plugins_disabledServerPlugins("sdf"), Message.Severity.Info);
                                            CoreGUI.getMessageCenter().notify(msg);
                                            refresh();
                                        }

                                        public void onFailure(Throwable caught) {
                                            // TODO: msg
                                            CoreGUI.getErrorHandler().handleError(
                                                MSG.view_admin_plugins_disabledServerPluginsFailure() + " "
                                                    + caught.getMessage(), caught);
                                            refreshTableInfo();
                                        }

                                    });
                            } else {
                                refreshTableInfo();
                            }
                        }
                    });
                }
            });

        addTableAction(extendLocatorId("purgeAll"), MSG.view_adminTopology_partitionEvents_purgeAll(),
            MSG.common_msg_areYouSure(), new AuthorizedTableAction(this, TableActionEnablement.ALWAYS,
                Permission.MANAGE_SETTINGS) {
                public void executeAction(final ListGridRecord[] selections, Object actionValue) {
                    // TODO: msg
                    //                       String message = MSG.view_admin_plugins_serverDisableConfirm(selectedNames.toString());
                    String message = "Really? Normal? For all I've done for you? ";
                    SC.ask(message, new BooleanCallback() {
                        public void execute(Boolean confirmed) {
                            if (confirmed) {
                                int[] selectedIds = getSelectedIds(selections);
                                SC.say("setting servers to maintenance mode, ids: " + selectedIds);
                                GWTServiceLookup.getCloudService().purgeAllEvents(new AsyncCallback<Void>() {
                                    public void onSuccess(Void arg0) {
                                        // TODO: msg
                                        Message msg = new Message(MSG.view_admin_plugins_disabledServerPlugins("sdf"),
                                            Message.Severity.Info);
                                        CoreGUI.getMessageCenter().notify(msg);
                                        refresh();
                                    }

                                    public void onFailure(Throwable caught) {
                                        // TODO: msg
                                        CoreGUI.getErrorHandler().handleError(
                                            MSG.view_admin_plugins_disabledServerPluginsFailure() + " "
                                                + caught.getMessage(), caught);
                                        refreshTableInfo();
                                    }

                                });
                            } else {
                                refreshTableInfo();
                            }
                        }
                    });
                }
            });

        addTableAction(extendLocatorId("forceRepartition"), MSG.view_adminTopology_partitionEvents_forceRepartition(),
            new AuthorizedTableAction(this, TableActionEnablement.ALWAYS, Permission.MANAGE_SETTINGS) {
                public void executeAction(final ListGridRecord[] selections, Object actionValue) {
                    //                    List<String> selectedNames = getSelectedNames(selections);
                    // TODO: msg
                    //                String message = MSG.view_admin_plugins_serverDisableConfirm(selectedNames.toString());
                    String message = "Really? Repartition? For all I've done for you? ";// + selectedNames;
                    SC.ask(message, new BooleanCallback() {
                        public void execute(Boolean confirmed) {
                            if (confirmed) {
                                SC.say("repartition is now forced");
                                GWTServiceLookup.getCloudService().cloudPartitionEventRequest(
                                    new AsyncCallback<Void>() {
                                        public void onSuccess(Void arg0) {
                                            // TODO: msg
                                            Message msg = new Message(MSG
                                                .view_admin_plugins_disabledServerPlugins("sdf"), Message.Severity.Info);
                                            CoreGUI.getMessageCenter().notify(msg);
                                            refresh();
                                        }

                                        public void onFailure(Throwable caught) {
                                            // TODO: msg
                                            CoreGUI.getErrorHandler().handleError(
                                                MSG.view_admin_plugins_disabledServerPluginsFailure() + " "
                                                    + caught.getMessage(), caught);
                                            refreshTableInfo();
                                        }

                                    });
                            } else {
                                refreshTableInfo();
                            }
                        }
                    });
                }
            });
    }

    private int[] getSelectedIds(ListGridRecord[] selections) {
        if (selections == null) {
            return new int[0];
        }
        int[] ids = new int[selections.length];
        int i = 0;
        for (ListGridRecord selection : selections) {
            ids[i++] = selection.getAttributeAsInt(FIELD_ID);
        }
        return ids;
    }

    //    private List<String> getSelectedNames(ListGridRecord[] selections) {
    //        if (selections == null) {
    //            return new ArrayList<String>(0);
    //        }
    //        List<String> ids = new ArrayList<String>(selections.length);
    //        for (ListGridRecord selection : selections) {
    //            ids.add(selection.getAttributeAsString(FIELD_NAME));
    //        }
    //        return ids;
    //    }

    @Override
    public ViewName getViewName() {
        return VIEW_ID;
    }

}
