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
package org.rhq.coregui.client.admin.topology;

import java.util.ArrayList;
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
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.IconEnum;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.admin.AdministrationView;
import org.rhq.coregui.client.components.form.EnumSelectItem;
import org.rhq.coregui.client.components.table.AuthorizedTableAction;
import org.rhq.coregui.client.components.table.TableActionEnablement;
import org.rhq.coregui.client.components.table.TableSection;
import org.rhq.coregui.client.components.view.HasViewName;
import org.rhq.coregui.client.components.view.ViewName;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.StringUtility;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton.ButtonColor;
import org.rhq.coregui.client.util.message.Message;

/**
 * Shows the table of all partition events.
 * 
 * @author Jirka Kremser
 */
public class PartitionEventTableView extends TableSection<PartitionEventDatasource> implements HasViewName {

    public static final ViewName VIEW_ID = new ViewName("PartitionEvents", MSG.view_adminTopology_partitionEvents(),
        IconEnum.EVENTS);

    public static final String VIEW_PATH = AdministrationView.VIEW_ID + "/"
        + AdministrationView.SECTION_TOPOLOGY_VIEW_ID + "/" + VIEW_ID;

    private static final Criteria INITIAL_CRITERIA = new Criteria();

    private static final SortSpecifier DEFAULT_SORT_SPECIFIER = new SortSpecifier(
        PartitionEventCriteria.SORT_FIELD_CTIME, SortDirection.DESCENDING);

    private enum TableAction {
        REMOVE_SELECTED(MSG.view_adminTopology_server_removeSelected(), TableActionEnablement.ANY, ButtonColor.RED), PURGE_ALL(
            MSG.view_adminTopology_partitionEvents_purgeAll(), TableActionEnablement.ALWAYS, ButtonColor.RED), FORCE_REPARTITION(
            MSG.view_adminTopology_partitionEvents_forceRepartition(), TableActionEnablement.ALWAYS, ButtonColor.GRAY);

        private String title;
        private TableActionEnablement enablement;
        private ButtonColor buttonColor;

        private TableAction(String title, TableActionEnablement enablement, ButtonColor buttonColor) {
            this.title = title;
            this.enablement = enablement;
            this.buttonColor = buttonColor;
        }
    }

    public PartitionEventTableView(String tableTitle) {
        super(tableTitle, INITIAL_CRITERIA, new SortSpecifier[] { DEFAULT_SORT_SPECIFIER });
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
            if (PartitionEventDatasourceField.FIELD_EVENT_TYPE.propertyName().equals(field.getName())) {
                field.setCellFormatter(new CellFormatter() {
                    @Override
                    public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                        if (value == null) {
                            return "";
                        }
                        String detailsUrl = "#" + VIEW_PATH + "/" + getId(record);
                        String formattedValue = StringUtility.escapeHtml(value.toString());
                        return LinkManager.getHref(detailsUrl, formattedValue);
                    }
                });
            }
        }
    }

    @Override
    public Canvas getDetailsView(Integer id) {
        return new PartitionEventDetailView(id);
    }

    private void showActions() {
        addTableAction(TableAction.REMOVE_SELECTED);
        addTableAction(TableAction.PURGE_ALL);
        addTableAction(TableAction.FORCE_REPARTITION);
    }

    private void addTableAction(final TableAction action) {
        addTableAction(action.title, null, action.buttonColor, new AuthorizedTableAction(this, action.enablement,
            Permission.MANAGE_SETTINGS) {
            public void executeAction(final ListGridRecord[] selections, Object actionValue) {
                String eventTypes = getSelectedEventTypes(selections).toString();
                String message = null;
                switch (action) {
                case REMOVE_SELECTED:
                    message = MSG.view_adminTopology_message_removePEventConfirm(eventTypes);
                    break;
                case PURGE_ALL:
                    message = MSG.view_adminTopology_message_removeAllPEventConfirm();
                    break;
                case FORCE_REPARTITION:
                    message = MSG.view_adminTopology_message_forceRepartition();
                    break;
                default:
                    throw new IllegalArgumentException("unknown table action type");
                }
                SC.ask(message, new BooleanCallback() {
                    public void execute(Boolean confirmed) {
                        if (confirmed) {
                            int[] selectedIds = getSelectedIds(selections);

                            AsyncCallback<Void> callback = new AsyncCallback<Void>() {
                                public void onSuccess(Void arg0) {
                                    String msgString = null;
                                    switch (action) {
                                    case REMOVE_SELECTED:
                                        msgString = MSG.view_adminTopology_message_removedPEvent(String
                                            .valueOf(selections.length));
                                        break;
                                    case PURGE_ALL:
                                        msgString = MSG.view_adminTopology_message_removedAllPEvent();
                                        break;
                                    case FORCE_REPARTITION:
                                        msgString = MSG.view_adminTopology_message_repartitioned();
                                    }
                                    Message msg = new Message(msgString, Message.Severity.Info);
                                    CoreGUI.getMessageCenter().notify(msg);
                                    refresh();
                                }

                                public void onFailure(Throwable caught) {
                                    String msgString = null;
                                    switch (action) {
                                    case REMOVE_SELECTED:
                                        msgString = MSG.view_adminTopology_message_removePEventFail(String
                                            .valueOf(selections.length));
                                        break;
                                    case PURGE_ALL:
                                        msgString = MSG.view_adminTopology_message_removedAllPEventFail();
                                        break;
                                    case FORCE_REPARTITION:
                                        msgString = MSG.view_adminTopology_message_forceRepartitionFail();
                                    }
                                    CoreGUI.getErrorHandler()
                                        .handleError(msgString + " " + caught.getMessage(), caught);
                                    refreshTableInfo();
                                }

                            };
                            switch (action) {
                            case REMOVE_SELECTED:
                                GWTServiceLookup.getTopologyService().deletePartitionEvents(selectedIds, callback);
                                break;
                            case PURGE_ALL:
                                GWTServiceLookup.getTopologyService().purgeAllEvents(callback);
                                break;
                            case FORCE_REPARTITION:
                                GWTServiceLookup.getTopologyService().cloudPartitionEventRequest(callback);
                            }

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

    private List<String> getSelectedEventTypes(ListGridRecord[] selections) {
        if (selections == null) {
            return new ArrayList<String>(0);
        }
        List<String> ids = new ArrayList<String>(selections.length);
        for (ListGridRecord selection : selections) {
            ids.add(selection.getAttributeAsString(PartitionEventDatasourceField.FIELD_EVENT_TYPE.propertyName()));
        }
        return ids;
    }

    @Override
    public ViewName getViewName() {
        return VIEW_ID;
    }

}
