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

import static org.rhq.coregui.client.admin.topology.AgentDatasourceField.FIELD_ADDRESS;
import static org.rhq.coregui.client.admin.topology.AgentDatasourceField.FIELD_AFFINITY_GROUP;
import static org.rhq.coregui.client.admin.topology.AgentDatasourceField.FIELD_AFFINITY_GROUP_ID;
import static org.rhq.coregui.client.admin.topology.AgentDatasourceField.FIELD_SERVER;
import static org.rhq.coregui.client.admin.topology.AgentDatasourceField.FIELD_SERVER_ID;

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
import org.rhq.core.domain.install.remote.AgentInstall;
import org.rhq.core.domain.resource.Agent;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.IconEnum;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.PopupWindow;
import org.rhq.coregui.client.admin.AdministrationView;
import org.rhq.coregui.client.admin.agent.install.RemoteAgentInstallView;
import org.rhq.coregui.client.admin.agent.install.RemoteAgentInstallView.Type;
import org.rhq.coregui.client.components.table.AuthorizedTableAction;
import org.rhq.coregui.client.components.table.TableActionEnablement;
import org.rhq.coregui.client.components.table.TableSection;
import org.rhq.coregui.client.components.table.Table.TableActionInfo.ButtonColor;
import org.rhq.coregui.client.components.view.HasViewName;
import org.rhq.coregui.client.components.view.ViewName;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.coregui.client.util.StringUtility;
import org.rhq.coregui.client.util.message.Message;

/**
 * Shows the table of all agents.
 *
 * This component is used from three various contexts:
 * 1) simple list of all available agents (url fragment - #Administration/Topology/Agents)
 * 2) list of agents connected to the server on server detail page (#Administration/Topology/Servers/{serverId})
 * 3) list of agents assigned to a affinity group (#Administration/Topology/AffinityGroups/{aGroupId})
 *
 * @author Jirka Kremser
 */
public class AgentTableView extends TableSection<AgentDatasource> implements HasViewName {

    public static final ViewName VIEW_ID = new ViewName("Agents", MSG.view_adminTopology_agents(), IconEnum.AGENT);

    public static final String VIEW_PATH = AdministrationView.VIEW_ID + "/"
        + AdministrationView.SECTION_TOPOLOGY_VIEW_ID + "/" + VIEW_ID;

    private final boolean isAffinityGroupId;
    private final Integer id;

    public AgentTableView(Integer id, boolean isAffinityGroupId) {
        super(null);
        this.isAffinityGroupId = isAffinityGroupId;
        this.id = id;
        setHeight100();
        setWidth100();
        setDataSource(new AgentDatasource(id, isAffinityGroupId));
    }

    @Override
    protected void configureTable() {
        super.configureTable();
        List<ListGridField> fields = getDataSource().getListGridFields();
        ListGrid listGrid = getListGrid();
        listGrid.setFields(fields.toArray(new ListGridField[fields.size()]));
        listGrid.sort(FIELD_NAME, SortDirection.ASCENDING);

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
            } else if (FIELD_SERVER.propertyName().equals(field.getName())) {
                // adding the cell formatter for server field (clickable link)
                field.setCellFormatter(new CellFormatter() {
                    @Override
                    public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                        if (value == null || value.toString().isEmpty()) {
                            return "";
                        }
                        String detailsUrl = "#" + ServerTableView.VIEW_PATH + "/"
                            + record.getAttributeAsString(FIELD_SERVER_ID.propertyName());
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

        // list of all agents (context #1 see the class JavaDoc)
        if (id == null) {
            setupNewButton();
            setupDeleteButton();
            setupStartButton();
            setupStopButton();
        }

        // list of agents assigned to affinity group (context #3)
        if (isAffinityGroupId) {
            showUpdateMembersAction();
        }
    }

    private void setupNewButton() {
        addTableAction(MSG.common_button_new(), null, ButtonColor.BLUE, new AuthorizedTableAction(this,
            TableActionEnablement.ALWAYS, Permission.MANAGE_INVENTORY) {
            public void executeAction(final ListGridRecord[] selections, Object actionValue) {
                newDetails();
            }
        });
    }

    private void setupStartButton() {
        addTableAction(MSG.common_button_start(), null, ButtonColor.GRAY, new AuthorizedTableAction(this,
            TableActionEnablement.SINGLE, Permission.MANAGE_INVENTORY) {
            public void executeAction(final ListGridRecord[] selections, Object actionValue) {
                if (selections == null || selections.length == 0) {
                    return; // do nothing since nothing is selected (we really shouldn't get here)
                }

                final String agentName = selections[0].getAttributeAsString(FIELD_NAME);
                final String agentAddress = selections[0].getAttributeAsString(FIELD_ADDRESS.propertyName());

                GWTServiceLookup.getAgentService().getAgentInstallByAgentName(agentName,
                    new AsyncCallback<AgentInstall>() {
                        @Override
                        public void onSuccess(AgentInstall result) {
                            showRemoteAgentInstallView(result);
                        }

                        @Override
                        public void onFailure(Throwable caught) {
                            showRemoteAgentInstallView(null); // can't get any info on the agent - the user will have to provide it all
                        }

                        private void showRemoteAgentInstallView(AgentInstall ai) {
                            // if no hostname is in agent install info, help out the user by suggesting the agent endpoint hostname
                            if (ai != null && ai.getSshHost() == null) {
                                ai.setSshHost(agentAddress);
                            }
                            RemoteAgentInstallView remoteAgentView = new RemoteAgentInstallView(ai, Type.START);
                            PopupWindow window = new PopupWindow(remoteAgentView);
                            window.setTitle(MSG.view_adminTopology_agent_start());
                            window.setHeight(350);
                            window.setWidth(850);
                            window.show();
                            refreshTableInfo();
                        }
                    });
            }
        });
    }

    private void setupStopButton() {
        addTableAction(MSG.common_button_stop(), null, ButtonColor.GRAY, new AuthorizedTableAction(this,
            TableActionEnablement.SINGLE, Permission.MANAGE_INVENTORY) {
            public void executeAction(final ListGridRecord[] selections, Object actionValue) {
                if (selections == null || selections.length == 0) {
                    return; // do nothing since nothing is selected (we really shouldn't get here)
                }

                final String agentName = selections[0].getAttributeAsString(FIELD_NAME);
                final String agentAddress = selections[0].getAttributeAsString(FIELD_ADDRESS.propertyName());

                GWTServiceLookup.getAgentService().getAgentInstallByAgentName(agentName,
                    new AsyncCallback<AgentInstall>() {
                        @Override
                        public void onSuccess(AgentInstall result) {
                            showRemoteAgentInstallView(result);
                        }

                        @Override
                        public void onFailure(Throwable caught) {
                            showRemoteAgentInstallView(null); // can't get any info on the agent - the user will have to provide it all
                        }

                        private void showRemoteAgentInstallView(AgentInstall ai) {
                            // if no hostname is in agent install info, help out the user by suggesting the agent endpoint hostname
                            if (ai != null && ai.getSshHost() == null) {
                                ai.setSshHost(agentAddress);
                            }
                            RemoteAgentInstallView remoteAgentView = new RemoteAgentInstallView(ai, Type.STOP);
                            PopupWindow window = new PopupWindow(remoteAgentView);
                            window.setTitle(MSG.view_adminTopology_agent_stop());
                            window.setHeight(350);
                            window.setWidth(850);
                            window.show();
                            refreshTableInfo();
                        }
                    });
            }
        });
    }

    private void setupDeleteButton() {
        addTableAction(MSG.common_button_delete(), MSG.view_adminTopology_agent_delete_confirm(), ButtonColor.RED,
            new AuthorizedTableAction(this, TableActionEnablement.SINGLE, Permission.MANAGE_INVENTORY) {
                public void executeAction(final ListGridRecord[] selections, Object actionValue) {
                    if (selections == null || selections.length == 0) {
                        refresh();
                        return; // do nothing since nothing is selected (we really shouldn't get here)
                    }

                    // Get the selected agents.
                    // Some of the code below supports removing multiple agents - however, I had to change the UI to only
                    // allow the user to only be able to select one at a time for deletion because we need to support
                    // the ability to uninstall it. And as of the time of me writing this, we can only uninstall via
                    // the dialog box to uninstall which would get ugly if we popped up multiple ones. We could uninstall
                    // multiple agents without asking the user via the dialog however it would mean we must have the SSH
                    // credentials and the install location in the DB for all of them and I'm not sure this is always going
                    // to be available for all agents.

                    final Agent[] agents = new Agent[selections.length];
                    int i = 0;
                    for (ListGridRecord selection : selections) {
                        final int agentId = selection.getAttributeAsInt(FIELD_ID);
                        final String agentName = selection.getAttribute(FIELD_NAME);
                        final String agentHost = selection.getAttribute(FIELD_ADDRESS.propertyName()); // this might not be needed
                        final Agent agent = new Agent();
                        agent.setId(agentId);
                        agent.setName(agentName);
                        agent.setAddress(agentHost);
                        agents[i++] = agent;
                    }

                    // ask if we want to uninstall it
                    SC.ask(MSG.view_adminTopology_agent_uninstallConfirm(), new BooleanCallback() {
                        public void execute(final Boolean uninstall) {
                            // if uninstall is true the user wants us to try to remove the agent installation files on the remote box
                            if (uninstall) {
                                GWTServiceLookup.getAgentService().getAgentInstallByAgentName(agents[0].getName(),
                                    new AsyncCallback<AgentInstall>() {
                                        @Override
                                        public void onSuccess(AgentInstall result) {
                                            RemoteAgentInstallView remoteAgentView = new RemoteAgentInstallView(result,
                                                Type.UNINSTALL);

                                            final PopupWindow window = new PopupWindow(remoteAgentView);
                                            window.setTitle(MSG.view_adminTopology_agent_uninstall());
                                            window.setHeight(350);
                                            window.setWidth(850);
                                            window.show();

                                            remoteAgentView
                                                .setSuccessHandler(new RemoteAgentInstallView.SuccessHandler() {
                                                    @Override
                                                    public void onSuccess(RemoteAgentInstallView.Type type) {
                                                        if (type == Type.UNINSTALL) {
                                                            removeResources();
                                                        }
                                                    }
                                                });
                                        }

                                        @Override
                                        public void onFailure(Throwable caught) {
                                            CoreGUI.getErrorHandler().handleError(
                                                MSG.view_adminTopology_agent_delete_error(), caught);
                                            refreshTableInfo();
                                        }
                                    });
                            } else {
                                // user doesn't want us to uninstall, just to remove the agent resources
                                removeResources();
                            }
                        }

                        private void removeResources() {
                            final ResourceGWTServiceAsync resourceManager = GWTServiceLookup.getResourceService();

                            resourceManager.uninventoryAllResourcesByAgent(agents, new AsyncCallback<Void>() {
                                public void onSuccess(Void result) {
                                    CoreGUI.getMessageCenter().notify(
                                        new Message(MSG.view_adminTopology_agent_delete_submitted(Integer
                                            .toString(agents.length))));
                                    refresh();
                                }

                                public void onFailure(Throwable caught) {
                                    CoreGUI.getErrorHandler().handleError(MSG.view_adminTopology_agent_delete_error(),
                                        caught);
                                    refreshTableInfo();
                                }
                            });
                        }
                    });
                }
            });
    }

    private void showUpdateMembersAction() {
        addTableAction(MSG.view_groupInventoryMembers_button_updateMembership(), ButtonColor.BLUE, new AuthorizedTableAction(this,
            TableActionEnablement.ALWAYS, Permission.MANAGE_SETTINGS) {
            public void executeAction(final ListGridRecord[] selections, Object actionValue) {
                AffinityGroupAgentsSelector.show(id, AgentTableView.this);
            }
        });
    }

    @Override
    public Canvas getDetailsView(Integer id) {
        if (id != null && id.intValue() > 0) {
            return new AgentDetailView(id);
        } else {
            return new RemoteAgentInstallView(null, Type.INSTALL);
        }
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
