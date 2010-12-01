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
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.FormItemIcon;
import com.smartgwt.client.widgets.form.fields.HeaderItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;

import org.rhq.core.domain.resource.Agent;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Simeon Pinder
 */
public class ResourceResourceAgentView extends LocatableVLayout {

    private int resourceId;
    private StaticTextItem nameValue;
    private StaticTextItem addressValue;
    private StaticTextItem portValue;
    private StaticTextItem agentStatus;
    private FormItemIcon agentStatusIcon;
    private StaticTextItem lastAvailReportValue;
    private StaticTextItem endpointValue;

    public ResourceResourceAgentView(String locatorId, int resourceId) {
        super(locatorId);

        this.resourceId = resourceId;
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        build();
    }

    public void build() {
        LocatableDynamicForm currentAgentInfo = new LocatableDynamicForm(extendLocatorId("Agent_Info"));
        populateAgentInfo(this.resourceId, currentAgentInfo);
        this.addMember(currentAgentInfo);
    }

    private void populateAgentInfo(final int id, final LocatableDynamicForm currentAgentInfo) {
        if (currentAgentInfo != null) {
            setLeft("10%");
            setWidth("80%");
            final List<FormItem> formItems = new ArrayList<FormItem>();
            HeaderItem headerItem = new HeaderItem("header", MSG.view_inventory_summary_agent_title());
            headerItem.setValue(MSG.view_inventory_summary_agent_title());
            formItems.add(headerItem);
            formItems.add(new SpacerItem());
            //populate remaining details
            GWTServiceLookup.getAgentService().getAgentForResource(id, new AsyncCallback<Agent>() {
                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_inventory_summary_agent_error1() + id + ".", caught);
                }

                @Override
                public void onSuccess(Agent agent) {
                    //name
                    String name = "name";
                    nameValue = new StaticTextItem(name, MSG.common_title_name());
                    nameValue.setValue(agent.getName());
                    formItems.add(nameValue);
                    //address
                    String address = "address";
                    addressValue = new StaticTextItem(address, MSG.common_title_address());
                    addressValue.setValue(agent.getAddress());
                    formItems.add(addressValue);
                    //port
                    String port = "port";
                    portValue = new StaticTextItem(port, MSG.common_title_port());
                    portValue.setValue(agent.getPort());
                    formItems.add(portValue);

                    //agent-comm-status
                    String agentComStatus = "agent-comm-status";
                    agentStatusIcon = new FormItemIcon();
                    agentStatusIcon.setSrc(ImageManager.getAvailabilityLargeIcon(null));
                    agentStatus = new StaticTextItem(agentComStatus, MSG.view_inventory_summary_agent_status_title());
                    agentStatus.setIcons(agentStatusIcon);
                    agentStatus.setWrapTitle(false);
                    formItems.add(agentStatus);
                    GWTServiceLookup.getAgentService().pingAgentForResource(id, new AsyncCallback<Boolean>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError(MSG.view_inventory_summary_agent_error2() + id + ".",
                                caught);
                        }

                        @Override
                        public void onSuccess(Boolean result) {
                            //update icon with correct status
                            agentStatusIcon.setSrc(ImageManager.getAvailabilityLargeIcon(result));
                            currentAgentInfo.markForRedraw();
                        }
                    });

                    //Last Received Avail report
                    String lastAvailReport = "last-avail-report";
                    lastAvailReportValue = new StaticTextItem(lastAvailReport, MSG
                        .view_inventory_summary_agent_last_title());
                    lastAvailReportValue.setWrapTitle(false);
                    lastAvailReportValue.setValue(new Date(agent.getLastAvailabilityReport()));
                    formItems.add(lastAvailReportValue);

                    //Full Endpoint
                    String fullEndpoint = "full-endpoint";
                    endpointValue = new StaticTextItem(fullEndpoint, MSG.view_inventory_summary_agent_fullEnpoint());
                    String remoteEndpoint = agent.getRemoteEndpoint();
                    if (remoteEndpoint != null) {
                        // some browsers (firefox in particular) won't wrap unless you put breaks in the string
                        remoteEndpoint = remoteEndpoint.replaceAll("&", " &");
                    } else {
                        remoteEndpoint = MSG.view_inventory_summary_agent_fullEnpoint_err1();
                    }
                    endpointValue.setValue(remoteEndpoint);
                    formItems.add(endpointValue);

                    currentAgentInfo.setItems(formItems.toArray(new FormItem[formItems.size()]));
                    currentAgentInfo.markForRedraw();
                }
            });

            //final form population 
            currentAgentInfo.setItems(formItems.toArray(new FormItem[formItems.size()]));
        }
    }
}