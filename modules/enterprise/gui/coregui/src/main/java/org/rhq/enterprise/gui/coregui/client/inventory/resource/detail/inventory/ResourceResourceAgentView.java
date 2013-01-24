/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.inventory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.FormItemIcon;
import com.smartgwt.client.widgets.form.fields.HeaderItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;

import org.rhq.core.domain.resource.Agent;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.RefreshableView;
import org.rhq.enterprise.gui.coregui.client.admin.topology.AgentTableView;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.StringUtility;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * The content pane for the Resource Inventory>Agent subtab.
 *
 * @author Simeon Pinder
 */
public class ResourceResourceAgentView extends LocatableVLayout implements RefreshableView {

    private int resourceId;
    private LocatableDynamicForm form;
    private StaticTextItem nameValue;
    private StaticTextItem addressValue;
    private StaticTextItem portValue;
    private FormItemIcon agentStatusIcon;
    private StaticTextItem lastAvailReportValue;
    private StaticTextItem lastAvailPingValue;
    private StaticTextItem endpointValue;
    private StaticTextItem topologyViewItem;

    private boolean loading = false; // will be true if loadData is currently waiting for responses

    public ResourceResourceAgentView(String locatorId, int resourceId) {
        super(locatorId);

        this.resourceId = resourceId;
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        setLeft("10%");
        setWidth("80%");

        this.form = new LocatableDynamicForm(extendLocatorId("Agent_Info"));
        final List<FormItem> formItems = createFormItems();
        this.form.setItems(formItems.toArray(new FormItem[formItems.size()]));
        loadData();
        this.addMember(this.form);
    }

    @Override
    public void refresh() {
        loadData();
    }

    private List<FormItem> createFormItems() {
        final List<FormItem> formItems = new ArrayList<FormItem>();
        HeaderItem headerItem = new HeaderItem("header", MSG.view_inventory_summary_agent_title());
        headerItem.setValue(MSG.view_inventory_summary_agent_title());
        formItems.add(headerItem);
        formItems.add(new SpacerItem());

        // Name
        nameValue = new StaticTextItem("name", MSG.common_title_name());
        formItems.add(nameValue);

        // Address
        String address = "address";
        addressValue = new StaticTextItem(address, MSG.common_title_address());
        formItems.add(addressValue);

        // Port
        String port = "port";
        portValue = new StaticTextItem(port, MSG.common_title_port());
        formItems.add(portValue);

        // Agent Status
        agentStatusIcon = new FormItemIcon();
        agentStatusIcon.setSrc(ImageManager.getAvailabilityLargeIcon(null));
        StaticTextItem agentStatus = new StaticTextItem("agent-comm-status", MSG
            .view_inventory_summary_agent_status_title());
        agentStatus.setIcons(agentStatusIcon);
        agentStatus.setWrapTitle(false);
        formItems.add(agentStatus);

        // Last Received Avail report
        String lastAvailReport = "last-avail-report";
        lastAvailReportValue = new StaticTextItem(lastAvailReport, MSG.view_inventory_summary_agent_last_title());
        lastAvailReportValue.setWrapTitle(false);
        formItems.add(lastAvailReportValue);
        
        // Last Received Avail ping
        lastAvailPingValue = new StaticTextItem("last-avail-ping", MSG.view_adminTopology_agent_lastAvailabilityPing());
        lastAvailPingValue.setWrapTitle(false);
        formItems.add(lastAvailPingValue);

        // Full Endpoint
        String fullEndpoint = "full-endpoint";
        endpointValue = new StaticTextItem(fullEndpoint, MSG.view_inventory_summary_agent_fullEnpoint());
        formItems.add(endpointValue);
        
        // link to agent topology view
        String topologyView = "topology-view";
        topologyViewItem = new StaticTextItem("topology-view", MSG.view_admin_topology());
        formItems.add(topologyViewItem);

        return formItems;
    }

    private void loadData() {
        if (loading) {
            return; // we are already loading and waiting for data, no need to re-issue the same queries
        }
        loading = true;

        GWTServiceLookup.getAgentService().getAgentForResource(this.resourceId, new AsyncCallback<Agent>() {
            @Override
            public void onFailure(Throwable caught) {
                loading = false;
                //Permissions failure, generate message to that effect
                for (Canvas child : form.getChildren()) {
                    child.destroy();
                }

                HeaderItem headerItem = new HeaderItem("header", MSG.view_inventory_summary_agent_title());
                headerItem.setValue(MSG.view_inventory_summary_agent_title());
                StaticTextItem permissionsMessage = new StaticTextItem("permissions", "permissionsFailure");
                permissionsMessage.setShowTitle(false);
                permissionsMessage.setValue(MSG.view_inventory_summary_agent_error3());
                permissionsMessage.setWrap(false);
                form.setFields(headerItem, new SpacerItem(), permissionsMessage);
                form.markForRedraw();
            }

            @Override
            public void onSuccess(Agent agent) {
                GWTServiceLookup.getAgentService().pingAgentForResource(resourceId, new AsyncCallback<Boolean>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        loading = false;
                        CoreGUI.getErrorHandler().handleError(
                            MSG.view_inventory_summary_agent_error2() + " " + resourceId + ".", caught);
                        agentStatusIcon.setSrc(ImageManager.getAvailabilityLargeIcon(null));
                        form.markForRedraw();
                    }

                    @Override
                    public void onSuccess(Boolean isUp) {
                        loading = false;
                        //update icon with correct status
                        agentStatusIcon.setSrc(ImageManager.getAvailabilityLargeIcon(isUp));
                        form.markForRedraw();
                    }
                });
                nameValue.setValue(agent.getName());
                addressValue.setValue(agent.getAddress());
                portValue.setValue(agent.getPort());
                Long lastAvailReport = agent.getLastAvailabilityReport();
                lastAvailReportValue.setValue((null != lastAvailReport) ? new Date(lastAvailReport) : MSG.common_val_none());
                Long lastAvailPing = agent.getLastAvailabilityPing();
                lastAvailPingValue.setValue((null != lastAvailPing) ? new Date(lastAvailPing) : MSG.common_val_none());
                String remoteEndpoint = agent.getRemoteEndpoint();
                if (remoteEndpoint != null) {
                    // some browsers (firefox in particular) won't wrap unless you put breaks in the string
                    // (EDIT: in FF 16 the wrapping works even without a space, so this could be possibly ommited)
                    remoteEndpoint = remoteEndpoint.replaceAll("&", " &");
                } else {
                    remoteEndpoint = MSG.view_inventory_summary_agent_fullEnpoint_err1();
                }
                endpointValue.setValue(remoteEndpoint);
                
                // make clickable link for agent topology view
                String detailsUrl = "#" + AgentTableView.VIEW_PATH + "/" + agent.getId();
                String formattedValue = StringUtility.escapeHtml(MSG.common_label_link());
                String topologyViewItemText = SeleniumUtility.getLocatableHref(detailsUrl, formattedValue, null);
                topologyViewItem.setValue(topologyViewItemText);

                form.markForRedraw();
            }
        });
    }

}