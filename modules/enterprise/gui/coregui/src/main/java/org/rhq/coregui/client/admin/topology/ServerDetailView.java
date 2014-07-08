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

import static org.rhq.coregui.client.admin.topology.AgentDatasourceField.FIELD_AFFINITY_GROUP;
import static org.rhq.coregui.client.admin.topology.ServerDatasourceField.FIELD_ADDRESS;
import static org.rhq.coregui.client.admin.topology.ServerDatasourceField.FIELD_CTIME;
import static org.rhq.coregui.client.admin.topology.ServerDatasourceField.FIELD_MTIME;
import static org.rhq.coregui.client.admin.topology.ServerDatasourceField.FIELD_NAME;
import static org.rhq.coregui.client.admin.topology.ServerDatasourceField.FIELD_OPERATION_MODE;
import static org.rhq.coregui.client.admin.topology.ServerDatasourceField.FIELD_PORT;
import static org.rhq.coregui.client.admin.topology.ServerDatasourceField.FIELD_SECURE_PORT;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.validator.IntegerRangeValidator;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;

import org.rhq.core.domain.cloud.AffinityGroup;
import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.cloud.Server.OperationMode;
import org.rhq.core.domain.criteria.ServerCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.BookmarkableView;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.ViewPath;
import org.rhq.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.StringUtility;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton;
import org.rhq.coregui.client.util.enhanced.EnhancedToolStrip;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton.ButtonColor;
import org.rhq.coregui.client.util.message.Message;

/**
 * Shows details of a server.
 * 
 * @author Jirka Kremser
 */
public class ServerDetailView extends EnhancedVLayout implements BookmarkableView {

    private final int serverId;

    private static final int SECTION_COUNT = 2;
    private final SectionStack sectionStack;
    private SectionStackSection detailsSection = null;
    private SectionStackSection agentSection = null;

    private volatile int initSectionCount = 0;

    public ServerDetailView(int serverId) {
        super();
        this.serverId = serverId;
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
        ServerCriteria criteria = new ServerCriteria();
        criteria.addFilterId(serverId);
        GWTServiceLookup.getTopologyService().findServersByCriteria(criteria, new AsyncCallback<PageList<Server>>() {
            public void onSuccess(final PageList<Server> servers) {
                if (servers == null || servers.isEmpty() || servers.size() != 1) {
                    CoreGUI.getErrorHandler().handleError(
                        MSG.view_adminTopology_message_fetchServerFail(String.valueOf(serverId)));
                    initSectionCount = SECTION_COUNT;
                    return;
                }
                prepareDetailsSection(sectionStack, servers.get(0));
                prepareAgentSection(sectionStack, servers.get(0));
            }

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(
                    MSG.view_adminTopology_message_fetchServerFail(String.valueOf(serverId)) + " "
                        + caught.getMessage(), caught);
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
                    if (null != agentSection) {
                        sectionStack.addSection(agentSection);
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

    private void prepareAgentSection(SectionStack stack, Server server) {
        SectionStackSection section = new SectionStackSection(MSG.view_adminTopology_serverDetail_connectedAgents());
        section.setExpanded(true);
        AgentTableView agentsTable = new AgentTableView(serverId, false);
        section.setItems(agentsTable);

        agentSection = section;
        initSectionCount++;
        return;
    }

    private void prepareDetailsSection(SectionStack stack, final Server server) {
        final DynamicForm form = new DynamicForm();
        form.setMargin(10);
        form.setWidth100();
        form.setWrapItemTitles(false);
        form.setNumCols(2);

        StaticTextItem nameItem = new StaticTextItem(FIELD_NAME.propertyName(), FIELD_NAME.title());
        nameItem.setValue("<b>" + server.getName() + "</b>");

        final TextItem addressItem = new TextItem(FIELD_ADDRESS.propertyName(), FIELD_ADDRESS.title());
        addressItem.setRequired(true);
        addressItem.setValue(server.getAddress());

        IntegerRangeValidator portValidator = new IntegerRangeValidator();
        portValidator.setMin(0);
        portValidator.setMax(65535);
        final TextItem portItem = new TextItem(FIELD_PORT.propertyName(), FIELD_PORT.title());
        portItem.setRequired(true);
        portItem.setValidators(portValidator);
        portItem.setValue(server.getPort());

        final TextItem securePortItem = new TextItem(FIELD_SECURE_PORT.propertyName(), FIELD_SECURE_PORT.title());
        securePortItem.setRequired(true);
        securePortItem.setValidators(portValidator);
        securePortItem.setValue(server.getSecurePort());

        final SelectItem operationModeItem = new SelectItem(FIELD_OPERATION_MODE.propertyName(),
            MSG.view_adminTopology_serverDetail_operationMode());
        operationModeItem.setValueMap("NORMAL", "MAINTENANCE");
        operationModeItem.setValue(server.getOperationMode());

        // make clickable link for affinity group
        StaticTextItem affinityGroupItem = new StaticTextItem(FIELD_AFFINITY_GROUP.propertyName(),
            FIELD_AFFINITY_GROUP.title());
        String affinityGroupItemText = "";
        AffinityGroup ag = server.getAffinityGroup();
        if (ag != null && ag.getName() != null && !ag.getName().isEmpty()) {
            String detailsUrl = "#" + AffinityGroupTableView.VIEW_PATH + "/" + ag.getId();
            String formattedValue = StringUtility.escapeHtml(ag.getName());
            affinityGroupItemText = LinkManager.getHref(detailsUrl, formattedValue);
        }
        affinityGroupItem.setValue(affinityGroupItemText);

        StaticTextItem installationDateItem = new StaticTextItem(FIELD_CTIME.propertyName(), FIELD_CTIME.title());
        installationDateItem.setValue(TimestampCellFormatter.format(Long.valueOf(server.getCtime()),
            TimestampCellFormatter.DATE_TIME_FORMAT_LONG));

        StaticTextItem lastUpdateItem = new StaticTextItem(FIELD_MTIME.propertyName(), FIELD_MTIME.title());
        lastUpdateItem.setValue(TimestampCellFormatter.format(Long.valueOf(server.getMtime()),
            TimestampCellFormatter.DATE_TIME_FORMAT_LONG));

        IButton saveButton = new EnhancedIButton(MSG.common_button_save(), ButtonColor.BLUE);
        saveButton.setOverflow(Overflow.VISIBLE);
        saveButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                if (form.validate()) {
                    server.setAddress(addressItem.getValueAsString());
                    server.setPort(Integer.parseInt(portItem.getValueAsString()));
                    server.setSecurePort(Integer.parseInt(securePortItem.getValueAsString()));
                    server.setOperationMode(OperationMode.valueOf(operationModeItem.getValueAsString()));
                    GWTServiceLookup.getTopologyService().updateServer(server, new AsyncCallback<Void>() {
                        public void onSuccess(Void result) {
                            Message msg = new Message(MSG.view_adminTopology_message_serverUpdated(server.getName()),
                                Message.Severity.Info);
                            CoreGUI.getMessageCenter().notify(msg);

                        }

                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError(
                                MSG.view_adminTopology_message_serverUpdateFail(server.getName()) + " "
                                    + caught.getMessage(), caught);
                        }
                    });
                }
            }
        });

        form.setItems(nameItem, addressItem, portItem, securePortItem, operationModeItem, affinityGroupItem,
            installationDateItem, lastUpdateItem);

        EnhancedToolStrip footer = new EnhancedToolStrip();
        footer.setPadding(5);
        footer.setWidth100();
        footer.setMembersMargin(15);
        footer.addMember(saveButton);

        SectionStackSection section = new SectionStackSection(MSG.common_title_details());
        section.setExpanded(true);
        section.setItems(form, footer);

        detailsSection = section;
        initSectionCount++;
    }

    @Override
    public void renderView(ViewPath viewPath) {
        Log.debug("ServerDetailView: " + viewPath);
    }

}
