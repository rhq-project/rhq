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

import static org.rhq.enterprise.gui.coregui.client.admin.topology.AgentDatasourceField.FIELD_AFFINITY_GROUP;
import static org.rhq.enterprise.gui.coregui.client.admin.topology.AgentDatasourceField.FIELD_AFFINITY_GROUP_ID;
import static org.rhq.enterprise.gui.coregui.client.admin.topology.ServerDatasourceField.FIELD_ORDINAL;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
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
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.IconEnum;
import org.rhq.enterprise.gui.coregui.client.admin.AdministrationView;
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
 * @author Jirka Kremser
 * 
 */
public class ServerTableView extends
    TableSection<AbstractServerNodeDatasource<? extends Serializable, ? extends BaseCriteria>> implements HasViewName {

    public static final ViewName VIEW_ID = new ViewName("Servers(GWT)", MSG.view_adminTopology_servers() + "(GWT)",
        IconEnum.SERVERS);

    public static final String VIEW_PATH = AdministrationView.VIEW_ID + "/"
        + AdministrationView.SECTION_TOPOLOGY_VIEW_ID + "/" + VIEW_ID;

    private final boolean showActions;

    private final boolean isAffinityGroupId;

    private final Integer id;

    public ServerTableView(String locatorId, Integer id, boolean isAffinityGroupId) {
        super(locatorId, null);
        this.showActions = id == null && !isAffinityGroupId;
        this.isAffinityGroupId = isAffinityGroupId;
        this.id = id;
        setHeight100();
        setWidth100();
        if (isAffinityGroupId) {
            setDataSource(new ServerDatasource(id));
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
            if (field.getName() == FIELD_NAME) {
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
            } else if (field.getName() == FIELD_AFFINITY_GROUP.propertyName()) {
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
                        return SeleniumUtility.getLocatableHref(detailsUrl, formattedValue, null);
                    }
                });
            }
        }
    }

    @Override
    public Canvas getDetailsView(Integer id) {
        return new ServerDetailView(extendLocatorId("detailsView"), id);
    }

    private void showCommonActions() {
        addChangeOperationModeAction(OperationMode.NORMAL, MSG.view_adminTopology_server_setNormal());
        addChangeOperationModeAction(OperationMode.MAINTENANCE, MSG.view_adminTopology_server_setMaintenance());

        addTableAction(extendLocatorId("removeSelected"), MSG.view_adminTopology_server_removeSelected(), null,
            new AuthorizedTableAction(this, TableActionEnablement.ANY, Permission.MANAGE_SETTINGS) {
                public void executeAction(final ListGridRecord[] selections, Object actionValue) {
                    List<String> selectedNames = getSelectedNames(selections);
                    String message = MSG.view_adminTopology_message_removeServerConfirm(selectedNames.toString());
                    SC.ask(message, new BooleanCallback() {
                        public void execute(Boolean confirmed) {
                            if (confirmed) {
                                int[] selectedIds = getSelectedIds(selections);
                                GWTServiceLookup.getCloudService().deleteServers(selectedIds,
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
        addTableAction(extendLocatorId("set" + mode), label, null, new AuthorizedTableAction(this,
            TableActionEnablement.ANY, Permission.MANAGE_SETTINGS) {
            public void executeAction(final ListGridRecord[] selections, Object actionValue) {
                List<String> selectedNames = getSelectedNames(selections);
                String message = MSG.view_adminTopology_message_setModeConfirm(selectedNames.toString(), mode.name());
                SC.ask(message, new BooleanCallback() {
                    public void execute(Boolean confirmed) {
                        if (confirmed) {
                            int[] selectedIds = getSelectedIds(selections);
                            GWTServiceLookup.getCloudService().updateServerMode(selectedIds, mode,
                                new AsyncCallback<Void>() {
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
                        } else {
                            refreshTableInfo();
                        }
                    }
                });
            }
        });
    }

    private void showUpdateMembersAction() {
        addTableAction(extendLocatorId("editGroupServers"), MSG.view_groupInventoryMembers_button_updateMembership(),
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

}
