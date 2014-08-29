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

import static org.rhq.coregui.client.admin.topology.ServerDatasourceField.FIELD_AFFINITY_GROUP;
import static org.rhq.coregui.client.admin.topology.ServerDatasourceField.FIELD_AFFINITY_GROUP_ID;
import static org.rhq.coregui.client.admin.topology.ServerDatasourceField.FIELD_ORDINAL;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.cloud.Server.OperationMode;
import org.rhq.core.domain.criteria.BaseCriteria;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.IconEnum;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.admin.AdministrationView;
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
 * Shows the table of all servers.
 *
 * This component is used from three various contexts:
 * 1) simple list of all available servers (url fragment - #Administration/Topology/Servers)
 * 2) list of servers in the agent's failover list (#Administration/Topology/Agents/{agentId})
 * 3) list of servers assigned to a affinity group (#Administration/Topology/AffinityGroups/{aGroupId})
 *
 * @author Jirka Kremser
 */
public class ServerTableView extends
    TableSection<AbstractServerNodeDatasource<? extends Serializable, ? extends BaseCriteria>> implements HasViewName {

    public static final ViewName VIEW_ID = new ViewName("Servers", MSG.view_adminTopology_servers(), IconEnum.SERVERS);

    public static final String VIEW_PATH = AdministrationView.VIEW_ID + "/"
        + AdministrationView.SECTION_TOPOLOGY_VIEW_ID + "/" + VIEW_ID;

    private final boolean showActions;
    private final boolean isAffinityGroupId;
    private final Integer id;

    public ServerTableView(Integer id, boolean isAffinityGroupId) {
        super(null);
        this.showActions = id == null && !isAffinityGroupId;
        this.isAffinityGroupId = isAffinityGroupId;
        this.id = id;
        setHeight100();
        setWidth100();
        if (isAffinityGroupId) {
            setDataSource(new ServerDatasource(id));
            Criteria criteria = new Criteria();
            String[] modes = new String[OperationMode.values().length];
            int i = 0;
            for (OperationMode value : OperationMode.values()) {
                modes[i++] = value.name();
            }
            criteria.addCriteria(ServerDatasource.FILTER_OPERATION_MODE, modes);
            setInitialCriteria(criteria);
        } else {
            setDataSource(showActions ? new ServerWithAgentCountDatasource() : new FailoverListItemDatasource(id));
        }

    }

    @Override
    protected void configureTable() {
        super.configureTable();
        List<ListGridField> fields = getDataSource().getListGridFields();
        ListGrid listGrid = getListGrid();
        listGrid.setFields(fields.toArray(new ListGridField[fields.size()]));
        if (showActions) {
            listGrid.sort(FIELD_NAME, SortDirection.ASCENDING);
            showCommonActions();
        } else if (isAffinityGroupId) {
            listGrid.sort(FIELD_NAME, SortDirection.ASCENDING);
            // displayed from AffinityGroupDetailView
            showUpdateMembersAction();
        } else {
            // sorting by order field (displayed from AgentDetailView)
            listGrid.sort(FIELD_ORDINAL.propertyName(), SortDirection.ASCENDING);
        }
        for (ListGridField field : fields) {
            // adding the cell formatter for name field (clickable link)
            if (FIELD_NAME.equals(field.getName())) {
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
            } else if (FIELD_AFFINITY_GROUP.propertyName().equals(field.getName())) {
                // adding the cell formatter for affinity group field (clickable link)
                field.setCellFormatter(new CellFormatter() {
                    @Override
                    public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                        if (value == null || value.toString().isEmpty()) {
                            return "";
                        }
                        String detailsUrl = "#" + AffinityGroupTableView.VIEW_PATH + "/"
                            + record.getAttributeAsString(FIELD_AFFINITY_GROUP_ID.propertyName());
                        String formattedValue = StringUtility.escapeHtml(value.toString());
                        return LinkManager.getHref(detailsUrl, formattedValue);
                    }
                });
            }
        }
    }

    @Override
    public Canvas getDetailsView(Integer id) {
        return new ServerDetailView(id);
    }

    private void showCommonActions() {
        addChangeOperationModeAction(OperationMode.NORMAL, MSG.view_adminTopology_server_setNormal());
        addChangeOperationModeAction(OperationMode.MAINTENANCE, MSG.view_adminTopology_server_setMaintenance());

        addTableAction(MSG.view_adminTopology_server_removeSelected(), null, ButtonColor.RED,
            new AuthorizedTableAction(this, TableActionEnablement.ANY, Permission.MANAGE_SETTINGS) {
                public void executeAction(final ListGridRecord[] selections, Object actionValue) {
                    List<String> selectedNames = getSelectedNames(selections);
                    String message = MSG.view_adminTopology_message_removeServerConfirm(selectedNames.toString());
                    SC.ask(message, new BooleanCallback() {
                        public void execute(Boolean confirmed) {
                            if (null == confirmed || !confirmed) { // clicked "No" or closed the dialog
                                refreshTableInfo();
                            } else {
                                int[] selectedIds = getSelectedIds(selections);
                                GWTServiceLookup.getTopologyService().deleteServers(selectedIds,
                                    new AsyncCallback<Void>() {
                                        public void onSuccess(Void arg0) {
                                            Message msg = new Message(MSG
                                                .view_adminTopology_message_removedServer(String
                                                    .valueOf(selections.length)), Message.Severity.Info);
                                            CoreGUI.getMessageCenter().notify(msg);
                                            refresh();
                                        }

                                        public void onFailure(Throwable caught) {
                                            CoreGUI.getErrorHandler().handleError(
                                                MSG.view_adminTopology_message_removeServerFail(String
                                                    .valueOf(selections.length)) + " " + caught.getMessage(), caught);
                                            refreshTableInfo();
                                        }

                                    });
                            }
                        }
                    });
                }
            });
    }

    private void addChangeOperationModeAction(final OperationMode mode, String label) {
        addTableAction(label, null, ButtonColor.BLUE, new AuthorizedTableAction(this, TableActionEnablement.ANY,
            Permission.MANAGE_SETTINGS) {
            public void executeAction(final ListGridRecord[] selections, Object actionValue) {
                List<String> selectedNames = getSelectedNames(selections);
                String message = MSG.view_adminTopology_message_setModeConfirm(selectedNames.toString(), mode.name());
                SC.ask(message, new BooleanCallback() {
                    public void execute(Boolean confirmed) {
                        if (null == confirmed || !confirmed) { // clicked "No" or closed the dialog
                            refreshTableInfo();
                        } else {
                            int[] selectedIds = getSelectedIds(selections);
                            boolean manualMaintenance = mode == OperationMode.MAINTENANCE;
                            GWTServiceLookup.getTopologyService().updateServerManualMaintenance(selectedIds,
                                manualMaintenance, new AsyncCallback<Void>() {
                                    public void onSuccess(Void result) {
                                        Message msg = new Message(MSG.view_adminTopology_message_setMode(
                                            String.valueOf(selections.length), mode.name()), Message.Severity.Info);
                                        CoreGUI.getMessageCenter().notify(msg);
                                        refresh();
                                    }

                                    public void onFailure(Throwable caught) {
                                        CoreGUI.getErrorHandler().handleError(
                                            MSG.view_adminTopology_message_setModeFail(
                                                String.valueOf(selections.length), mode.name())
                                                + " " + caught.getMessage(), caught);
                                        refreshTableInfo();
                                    }

                                });
                        }
                    }
                });
            }
        });
    }

    private void showUpdateMembersAction() {
        addTableAction(MSG.view_groupInventoryMembers_button_updateMembership(), ButtonColor.BLUE,
            new AuthorizedTableAction(this, TableActionEnablement.ALWAYS, Permission.MANAGE_SETTINGS) {
                public void executeAction(final ListGridRecord[] selections, Object actionValue) {
                    AffinityGroupServersSelector.show(id, ServerTableView.this);
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

    private List<String> getSelectedNames(ListGridRecord[] selections) {
        if (selections == null) {
            return new ArrayList<String>(0);
        }
        List<String> ids = new ArrayList<String>(selections.length);
        for (ListGridRecord selection : selections) {
            ids.add(selection.getAttributeAsString(FIELD_NAME));
        }
        return ids;
    }

    @Override
    public ViewName getViewName() {
        return VIEW_ID;
    }

    @Override
    protected String getBasePath() {
        return VIEW_PATH;
    }
}
