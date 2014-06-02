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
import static org.rhq.coregui.client.admin.topology.AgentDatasourceField.FIELD_AGENT_TOKEN;
import static org.rhq.coregui.client.admin.topology.AgentDatasourceField.FIELD_LAST_AVAILABILITY_PING;
import static org.rhq.coregui.client.admin.topology.AgentDatasourceField.FIELD_LAST_AVAILABILITY_REPORT;
import static org.rhq.coregui.client.admin.topology.AgentDatasourceField.FIELD_NAME;
import static org.rhq.coregui.client.admin.topology.AgentDatasourceField.FIELD_PORT;
import static org.rhq.coregui.client.admin.topology.AgentDatasourceField.FIELD_REMOTE_ENDPOINT;
import static org.rhq.coregui.client.admin.topology.AgentDatasourceField.FIELD_SERVER;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;

import org.rhq.core.domain.cloud.AffinityGroup;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.criteria.AgentCriteria;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.StringUtility;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * Shows details of an agent.
 *
 * @author Jirka Kremser
 */
public class AgentDetailView extends EnhancedVLayout {

    private final int agentId;

    private static final int SECTION_COUNT = 3;
    private static final String LOADING_ICON = "ajax-loader.gif";
    private final SectionStack sectionStack;
    private SectionStackSection detailsSection = null;
    private SectionStackSection failoverListSection = null;
    private SectionStackSection agentPluginsSection = null;
    private volatile boolean waitingForPlugins = false;
    private EnhancedVLayout pluginsSection;
    private Img loading;

    private volatile int initSectionCount = 0;

    public AgentDetailView(int agentId) {
        super();
        this.agentId = agentId;
        setHeight100();
        setWidth100();
        setOverflow(Overflow.AUTO);

        sectionStack = new SectionStack();
        sectionStack.setVisibilityMode(VisibilityMode.MULTIPLE);
        sectionStack.setWidth100();
        sectionStack.setHeight100();
        sectionStack.setMargin(5);
        sectionStack.setOverflow(Overflow.VISIBLE);
    }

    @Override
    protected void onInit() {
        super.onInit();
        AgentCriteria criteria = new AgentCriteria();
        criteria.addFilterId(agentId);
        GWTServiceLookup.getTopologyService().findAgentsByCriteria(criteria, new AsyncCallback<PageList<Agent>>() {
            public void onSuccess(final PageList<Agent> agents) {
                if (agents == null || agents.isEmpty() || agents.size() != 1) {
                    CoreGUI.getErrorHandler().handleError(
                        MSG.view_adminTopology_message_fetchAgentFail(String.valueOf(agentId)));
                    initSectionCount = SECTION_COUNT;
                    return;
                }
                prepareDetailsSection(agents.get(0));
                prepareFailoverListSection(agents.get(0));

                GWTServiceLookup.getTopologyService().getResourceIdOfAgent(agentId, new AsyncCallback<Integer>() {
                    public void onSuccess(final Integer resourceId) {
                        if (resourceId != null) {
                            prepareAgentPluginsSection(sectionStack, resourceId);
                            loadPlugins(resourceId);
                        } else {
                            initSectionCount = SECTION_COUNT;
                        }
                    }

                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(
                            MSG.view_adminTopology_message_fetchAgentFail(String.valueOf(agentId)) + " "
                                + caught.getMessage(), caught);
                        initSectionCount = SECTION_COUNT;
                    }
                });
            }

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(
                    MSG.view_adminTopology_message_fetchAgentFail(String.valueOf(agentId)) + " " + caught.getMessage(),
                    caught);
                initSectionCount = SECTION_COUNT;
            }
        });
    }

    public boolean isInitialized() {
        return initSectionCount >= SECTION_COUNT;
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        // wait until we have all of the sections before we show them. We don't use InitializableView because,
        // it seems they are not supported (in the applicable renderView()) at this level.
        new Timer() {
            final long startTime = System.currentTimeMillis();

            public void run() {
                if (isInitialized()) {
                    if (null != detailsSection) {
                        sectionStack.addSection(detailsSection);
                    }
                    if (null != failoverListSection) {
                        sectionStack.addSection(failoverListSection);
                    }
                    if (null != agentPluginsSection) {
                        sectionStack.addSection(agentPluginsSection);
                    }

                    addMember(sectionStack);
                    markForRedraw();

                } else {
                    // don't wait forever, give up after 20s and show what we have
                    long elapsedMillis = System.currentTimeMillis() - startTime;
                    if (elapsedMillis > 20000) {
                        initSectionCount = SECTION_COUNT;
                    }
                    schedule(100); // Reschedule the timer.
                }
            }
        }.run(); // fire the timer immediately
    }

    private void prepareFailoverListSection(Agent agent) {
        SectionStackSection section = new SectionStackSection(MSG.view_adminTopology_agentDetail_agentFailoverList());
        section.setExpanded(true);
        ServerTableView agentsTable = new ServerTableView(agent.getId(), false);
        section.setItems(agentsTable);

        failoverListSection = section;
        ++initSectionCount;
        return;
    }

    private void prepareAgentPluginsSection(SectionStack stack, int resourceId) {
        SectionStackSection section = new SectionStackSection(MSG.view_adminConfig_agentPlugins());
        section.setExpanded(false);
        loading = new Img(LOADING_ICON, 16, 16);
        loading.setValign(VerticalAlignment.CENTER);
        loading.setAlign(Alignment.CENTER);
        pluginsSection = new EnhancedVLayout();
        pluginsSection.addMember(loading);
        section.setItems(pluginsSection);
        agentPluginsSection = section;
        ++initSectionCount;
        return;
    }

    protected Canvas buildResultsSection(ResourceOperationHistory operationHistory) {
        OperationRequestStatus status = operationHistory.getStatus();
        if (status == OperationRequestStatus.SUCCESS || status == OperationRequestStatus.FAILURE) {
            EnhancedVLayout resultsSection = new EnhancedVLayout();

            OperationDefinition operationDefinition = operationHistory.getOperationDefinition();
            ConfigurationDefinition resultsConfigurationDefinition = operationDefinition
                .getResultsConfigurationDefinition();
            if (resultsConfigurationDefinition != null
                && !resultsConfigurationDefinition.getPropertyDefinitions().isEmpty()
                && operationHistory.getResults() != null) {
                ConfigurationEditor editor = new ConfigurationEditor(
                    operationDefinition.getResultsConfigurationDefinition(), operationHistory.getResults());
                editor.setPreserveTextFormatting(true);
                editor.setReadOnly(true);
                resultsSection.addMember(editor);
            } else {
                Label noResultsLabel = new Label(MSG.view_operationHistoryDetails_noResults());
                noResultsLabel.setHeight(17);
                resultsSection.addMember(noResultsLabel);
            }

            return resultsSection;
        } else {
            return null;
        }
    }

    private void prepareDetailsSection(Agent agent) {
        final DynamicForm form = new DynamicForm();
        form.setMargin(10);
        form.setWidth100();
        form.setWrapItemTitles(false);
        form.setNumCols(2);

        StaticTextItem nameItem = new StaticTextItem(FIELD_NAME.propertyName(), FIELD_NAME.title());
        nameItem.setValue("<b>" + agent.getName() + "</b>");

        StaticTextItem addressItem = new StaticTextItem(FIELD_ADDRESS.propertyName(), FIELD_ADDRESS.title());
        addressItem.setValue(agent.getAddress());

        StaticTextItem remoteEndpointItem = new StaticTextItem(FIELD_REMOTE_ENDPOINT.propertyName(),
            FIELD_REMOTE_ENDPOINT.title());
        remoteEndpointItem.setValue(agent.getRemoteEndpoint());

        StaticTextItem portItem = new StaticTextItem(FIELD_PORT.propertyName(), FIELD_PORT.title());
        portItem.setValue(agent.getPort());

        final StaticTextItem tokenItem = new StaticTextItem(FIELD_AGENT_TOKEN.propertyName(), FIELD_AGENT_TOKEN.title());
        tokenItem.setValue(agent.getAgentToken());

        StaticTextItem lastAvailabilityReportItem = new StaticTextItem(FIELD_LAST_AVAILABILITY_REPORT.propertyName(),
            FIELD_LAST_AVAILABILITY_REPORT.title());
        String lastReport = agent.getLastAvailabilityReport() == null ? "unknown" : TimestampCellFormatter.format(
            Long.valueOf(agent.getLastAvailabilityReport()), TimestampCellFormatter.DATE_TIME_FORMAT_LONG);
        lastAvailabilityReportItem.setValue(lastReport);

        StaticTextItem lastAvailabilityPingItem = new StaticTextItem(FIELD_LAST_AVAILABILITY_PING.propertyName(),
            FIELD_LAST_AVAILABILITY_PING.title());
        String lastPing = agent.getLastAvailabilityPing() == null ? "unknown" : TimestampCellFormatter.format(
            Long.valueOf(agent.getLastAvailabilityPing()), TimestampCellFormatter.DATE_TIME_FORMAT_LONG);
        lastAvailabilityPingItem.setValue(lastPing);

        // make clickable link for affinity group
        StaticTextItem affinityGroupItem = new StaticTextItem(FIELD_AFFINITY_GROUP.propertyName(),
            FIELD_AFFINITY_GROUP.title());
        String affinityGroupItemText = "";
        AffinityGroup ag = agent.getAffinityGroup();
        if (ag != null && ag.getName() != null && !ag.getName().isEmpty()) {
            String detailsUrl = "#" + AffinityGroupTableView.VIEW_PATH + "/" + ag.getId();
            String formattedValue = StringUtility.escapeHtml(ag.getName());
            affinityGroupItemText = LinkManager.getHref(detailsUrl, formattedValue);
        }
        affinityGroupItem.setValue(affinityGroupItemText);

        StaticTextItem currentServerItem = new StaticTextItem(FIELD_SERVER.propertyName(), FIELD_SERVER.title());
        String serverValue = null;
        if (agent.getServer() == null) {
            serverValue = "";
        } else {
            String detailsUrl = "#" + ServerTableView.VIEW_PATH + "/" + agent.getServer().getId();
            String formattedValue = StringUtility.escapeHtml(agent.getServer().getName());
            serverValue = LinkManager.getHref(detailsUrl, formattedValue);
        }
        currentServerItem.setValue(serverValue);

        form.setItems(nameItem, addressItem, remoteEndpointItem, portItem, tokenItem, lastAvailabilityReportItem,
            lastAvailabilityPingItem, affinityGroupItem, currentServerItem);

        SectionStackSection section = new SectionStackSection(MSG.common_title_details());
        section.setExpanded(true);
        section.setItems(form);

        detailsSection = section;
        ++initSectionCount;
    }

    private void loadPlugins(final int resourceId) {
        if (waitingForPlugins) {
            return;
        }
        waitingForPlugins = true;
        GWTServiceLookup.getOperationService().scheduleResourceOperation(resourceId, "retrieveAllPluginInfo", null,
            "Run by RHQ Server", 0, new AsyncCallback<Void>() {

                @Override
                public void onFailure(Throwable caught) {
                    waitingForPlugins = false;
                }

                @Override
                public void onSuccess(Void result) {
                    final ResourceOperationHistoryCriteria criteria = new ResourceOperationHistoryCriteria();
                    criteria.addFilterResourceIds(resourceId);
                    criteria.addFilterOperationName("retrieveAllPluginInfo");
                    criteria.addSortEndTime(PageOrdering.DESC);
                    criteria.fetchResults(true);
                    new Timer() {
                        @Override
                        public void run() {
                            GWTServiceLookup.getOperationService().findResourceOperationHistoriesByCriteria(criteria,
                                new AsyncCallback<PageList<ResourceOperationHistory>>() {

                                    @Override
                                    public void onFailure(Throwable caught) {
                                        waitingForPlugins = false;
                                    }

                                    @Override
                                    public void onSuccess(PageList<ResourceOperationHistory> result) {
                                        if (!result.isEmpty()) {
                                            ResourceOperationHistory opHistory = result.get(0);
                                            if (opHistory.getStatus() == OperationRequestStatus.SUCCESS
                                                || opHistory.getStatus() == OperationRequestStatus.FAILURE) {
                                                pluginsSection.removeMembers(pluginsSection.getMembers());
                                                pluginsSection.addMember(buildResultsSection(opHistory));
                                                pluginsSection.markForRedraw();
                                                waitingForPlugins = false;
                                            } else if (opHistory.getStatus() == OperationRequestStatus.INPROGRESS) {
                                                schedule(1000);
                                            }
                                        }
                                    }
                                });
                        }

                    }.schedule(700);
                }
            });
    }
}
