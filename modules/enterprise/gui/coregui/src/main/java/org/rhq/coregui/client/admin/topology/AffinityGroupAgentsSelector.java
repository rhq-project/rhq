/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.coregui.client.admin.topology;

import static org.rhq.coregui.client.admin.topology.AgentDatasourceField.FIELD_NAME;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.core.domain.criteria.AgentCriteria;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.IconEnum;
import org.rhq.coregui.client.components.selector.AbstractSelector;
import org.rhq.coregui.client.components.table.TableSection;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.RPCDataSource;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton.ButtonColor;

/**
 * Component for assigning the agents into affinity group.
 * 
 * @author Jirka Kremser
 */
public class AffinityGroupAgentsSelector extends AbstractSelector<Agent, AgentCriteria> {

    private final Integer affinityGroupId;
    private static RPCDataSource<Agent, AgentCriteria> datasource = null;
    private static final int MAX_AVAIL_AGENTS = 3000;
    private static Window modalWindow;
    private static boolean shouldBeClosed;
    private static VLayout layout;
    private List<Integer> originallyAssignedIds;

    private AffinityGroupAgentsSelector() {
        super();
        affinityGroupId = -1;
    }

    private AffinityGroupAgentsSelector(Integer affinityGroupId) {
        super(false);
        this.affinityGroupId = affinityGroupId;
        prepareMembers(this);
    }

    @Override
    protected DynamicForm getAvailableFilterForm() {
        if (availableFilterForm == null) {
            availableFilterForm = new DynamicForm();
            availableFilterForm.setWidth("75%");
            final TextItem search = new TextItem(FIELD_NAME.propertyName(), MSG.common_title_search());
            availableFilterForm.setItems(search);
        }
        return availableFilterForm;
    }

    private void prepareMembers(final AffinityGroupAgentsSelector selector) {
        AgentCriteria criteria = new AgentCriteria();
        criteria.addFilterAffinityGroupId(affinityGroupId);
        GWTServiceLookup.getTopologyService().findAgentsByCriteria(criteria, new AsyncCallback<PageList<Agent>>() {
            public void onSuccess(PageList<Agent> result) {
                ListGridRecord[] records = getDataSource().buildRecords(result);
                originallyAssignedIds = getIdList(records);
                setAssigned(records);
                modalWindow.addItem(layout);
                modalWindow.show();
                selector.reset();
            }

            public void onFailure(Throwable t) {
                CoreGUI.getErrorHandler().handleError(
                    MSG.view_adminTopology_message_fetchAgentsFail(String.valueOf(affinityGroupId)), t);
            }
        });
    }

    @Override
    protected RPCDataSource<Agent, AgentCriteria> getDataSource() {
        if (datasource == null) {
            // fetch all available agents without an affinity group
            datasource = new AgentDatasource(null, false) {
                @Override
                protected void executeFetch(final DSRequest request, final DSResponse response, AgentCriteria criteria) {
                    criteria.fetchAffinityGroup(true);
                    GWTServiceLookup.getTopologyService().findAgentsByCriteria(criteria,
                        new AsyncCallback<PageList<Agent>>() {
                            public void onSuccess(PageList<Agent> result) {
                                Iterator<Agent> it = result.iterator();
                                while (it.hasNext()) {
                                    Agent agent = it.next();
                                    if (agent.getAffinityGroup() != null) {
                                        it.remove();
                                    }
                                }
                                response.setData(buildRecords(result));
                                response.setTotalRows(result.size());
                                processResponse(request.getRequestId(), response);
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                CoreGUI.getErrorHandler().handleError(
                                    MSG.view_adminTopology_message_fetchAgents2Fail(), t);
                                response.setStatus(DSResponse.STATUS_FAILURE);
                                processResponse(request.getRequestId(), response);
                            }
                        });
                }
            };
        }
        return datasource;
    }

    @Override
    protected int getMaxAvailableRecords() {
        return MAX_AVAIL_AGENTS;
    }

    @Override
    protected Criteria getLatestCriteria(DynamicForm availableFilterForm) {
        return availableFilterForm.getValuesAsCriteria();
    }

    @Override
    protected String getItemTitle() {
        return MSG.view_adminTopology_agents();
    }

    @Override
    protected String getItemIcon() {
        return IconEnum.AGENT.getIcon16x16Path();
    }

    public static void show(final Integer affinityGroupId, final TableSection<?> parrent) {
        modalWindow = new Window();
        modalWindow.addCloseClickHandler(new CloseClickHandler() {
            public void onCloseClick(CloseClickEvent event) {
                closeAndRefresh(parrent, false);
            }
        });
        modalWindow
            .setTitle(MSG.view_adminTopology_agents() + ": " + MSG.view_adminTopology_affinityGroups_createNew());
        modalWindow.setOverflow(Overflow.VISIBLE);
        modalWindow.setWidth(800);
        modalWindow.setHeight(410);
        modalWindow.setAutoCenter(true);

        layout = new VLayout();
        layout.setWidth100();
        layout.setHeight100();

        final AffinityGroupAgentsSelector selector = new AffinityGroupAgentsSelector(affinityGroupId);
        selector.setMargin(10);
        layout.addMember(selector);

        IButton cancel = new EnhancedIButton(MSG.common_button_cancel());
        cancel.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                closeAndRefresh(parrent, false);
            }
        });
        IButton save = new EnhancedIButton(MSG.common_button_save(), ButtonColor.BLUE);
        save.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                List<Integer> actuallySelected = getIdList(selector.getSelectedRecords());
                List<Integer> originallySelected = selector.getOriginallyAssignedIds();
                originallySelected.removeAll(actuallySelected);
                actuallySelected.removeAll(selector.getOriginallyAssignedIds());
                shouldBeClosed = true;
                if (!originallySelected.isEmpty()) {
                    shouldBeClosed = false;
                    GWTServiceLookup.getTopologyService().removeAgentsFromGroup(
                        originallySelected.toArray(new Integer[originallySelected.size()]), new AsyncCallback<Void>() {
                            public void onSuccess(Void result) {
                                closeAndRefresh(parrent, true);
                            }

                            public void onFailure(Throwable t) {
                                CoreGUI.getErrorHandler().handleError(
                                    MSG.view_adminTopology_message_agroupAssingAgentsFail(String
                                        .valueOf(affinityGroupId)), t);
                            }
                        });
                }
                if (!actuallySelected.isEmpty()) {
                    shouldBeClosed = false;
                    GWTServiceLookup.getTopologyService().addAgentsToGroup(selector.getAffinityGroupId(),
                        actuallySelected.toArray(new Integer[actuallySelected.size()]), new AsyncCallback<Void>() {
                            public void onSuccess(Void result) {
                                closeAndRefresh(parrent, true);
                            }

                            public void onFailure(Throwable t) {
                                CoreGUI.getErrorHandler().handleError(
                                    MSG.view_adminTopology_message_agroupRemovingAgentsFail(String
                                        .valueOf(affinityGroupId)), t);
                            }
                        });
                }
                if (shouldBeClosed) {
                    closeAndRefresh(parrent, false);
                }
            }
        });
        ToolStrip buttonBar = new ToolStrip();
        buttonBar.setPadding(5);
        buttonBar.setWidth100();
        buttonBar.setMembersMargin(15);
        buttonBar.addMember(save);
        buttonBar.addMember(cancel);
        layout.addMember(buttonBar);
    }

    private static void closeAndRefresh(TableSection<?> parrent, boolean fullRefresh) {
        if (modalWindow != null) {
            modalWindow.destroy();
        }
        if (fullRefresh) {
            parrent.refresh();
        } else {
            parrent.refreshTableInfo();
        }
    }

    private static List<Integer> getIdList(ListGridRecord[] records) {
        if (records == null) {
            return null;
        }
        List<Integer> ids = new ArrayList<Integer>(records.length);
        for (ListGridRecord record : records) {
            ids.add(record.getAttributeAsInt(AgentDatasourceField.FIELD_ID.propertyName()));
        }
        return ids;
    }

    public List<Integer> getOriginallyAssignedIds() {
        return new ArrayList<Integer>(originallyAssignedIds);
    }

    public Integer getAffinityGroupId() {
        return affinityGroupId;
    }
}
