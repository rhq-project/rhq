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
import static org.rhq.enterprise.gui.coregui.client.admin.topology.AgentDatasourceField.FIELD_SERVER;
import static org.rhq.enterprise.gui.coregui.client.admin.topology.AgentDatasourceField.FIELD_SERVER_ID;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.resource.Agent;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.IconEnum;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.admin.AdministrationView;
import org.rhq.enterprise.gui.coregui.client.components.table.AuthorizedTableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableActionEnablement;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection;
import org.rhq.enterprise.gui.coregui.client.components.view.HasViewName;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.StringUtility;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * Shows the table of all agents.
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
            if (field.getName() == FIELD_NAME) {
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
            } else if (field.getName() == FIELD_SERVER.propertyName()) {
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
                        return LinkManager.getHref(detailsUrl, formattedValue);
                    }
                });
            }
        }

        setupDeleteButton();

        if (isAffinityGroupId) {
            showUpdateMembersAction();
        }
    }

    private void setupDeleteButton() {
        addTableAction(MSG.common_button_delete(), MSG.view_adminTopology_agent_delete_confirm(),
            new AuthorizedTableAction(this, TableActionEnablement.ANY, Permission.MANAGE_INVENTORY) {
                public void executeAction(final ListGridRecord[] selections, Object actionValue) {
                    if (selections == null || selections.length == 0) {
                        return; // do nothing since nothing is selected (we really shouldn't get here)
                    }

                    final ResourceGWTServiceAsync resourceManager = GWTServiceLookup.getResourceService();
                    final Agent[] agents = new Agent[selections.length];
                    int i = 0;
                    for (ListGridRecord selection : selections) {
                        final int agentId = selection.getAttributeAsInt(FIELD_ID);
                        final String agentName = selection.getAttribute(FIELD_NAME);
                        final Agent agent = new Agent();
                        agent.setId(agentId);
                        agent.setName(agentName);
                        agents[i++] = agent;
                    }

                    resourceManager.uninventoryAllResourcesByAgent(agents, new AsyncCallback<Void>() {
                        public void onSuccess(Void result) {
                            CoreGUI.getMessageCenter().notify(
                                new Message(MSG.view_adminTopology_agent_delete_submitted(Integer
                                    .toString(agents.length))));
                            refresh();
                        }

                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError(MSG.view_adminTopology_agent_delete_error(), caught);
                            refresh();
                        }
                    });
                }
            });
    }

    private void showUpdateMembersAction() {
        addTableAction(MSG.view_groupInventoryMembers_button_updateMembership(), new AuthorizedTableAction(this,
            TableActionEnablement.ALWAYS, Permission.MANAGE_SETTINGS) {
            public void executeAction(final ListGridRecord[] selections, Object actionValue) {
                AffinityGroupAgentsSelector.show(id, AgentTableView.this);
            }
        });
    }

    @Override
    public Canvas getDetailsView(Integer id) {
        return new AgentDetailView(id);
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
