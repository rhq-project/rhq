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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.summary;

import java.util.ArrayList;
import java.util.List;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.HTMLPane;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.HeaderItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.CellClickEvent;
import com.smartgwt.client.widgets.grid.events.CellClickHandler;

import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * The Resource Summary>Overview tab.
 *
 * @author Lukas Krejci
 */
public class OverviewView extends LocatableVLayout {

    private Table errorsGrid;
    private Img availabilityImage;

    public OverviewView(String locatorId, ResourceComposite resourceComposite) {
        super(locatorId);
        OverviewForm form = new OverviewForm(extendLocatorId("form"), resourceComposite);
        LocatableDynamicForm currentAgentInfo = new LocatableDynamicForm(extendLocatorId("Agent_Info"));
        populateAgentInfo(resourceComposite.getResource().getId(), currentAgentInfo);
        errorsGrid = new Table(extendLocatorId("errors"), MSG.view_summaryOverview_header_detectedErrors(), null, null,
            new String[] { ResourceErrorsDataSource.Field.DETAIL });

        Resource resource = resourceComposite.getResource();
        ResourceErrorsDataSource errors = new ResourceErrorsDataSource(resource.getId());

        errorsGrid.setShowFooter(false);
        errorsGrid.setDataSource(errors);

        //        form.setHeight("*");
        form.setHeight("200");
        errorsGrid.setHeight(200); //this should be just enough to fit the maximum of 3 rows in this table (there's at most 1 error per type)

        addMember(form);
        addMember(currentAgentInfo);
        addMember(errorsGrid);
    }

    private void populateAgentInfo(final int id, final LocatableDynamicForm currentAgentInfo) {
        if (currentAgentInfo != null) {
            setLeft("10%");
            setWidth("80%");
            //            currentAgentInfo.setBorder("1px dashed black");
            final List<FormItem> formItems = new ArrayList<FormItem>();
            HeaderItem headerItem = new HeaderItem("header", "Agent Managing this Resource");
            headerItem.setValue("Agent Managing this Resource");
            formItems.add(headerItem);
            //populate remaining details
            GWTServiceLookup.getAgentService().getAgentForResource(id, new AsyncCallback<Agent>() {
                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Failed to locate agent managing resource id " + id + ".",
                        caught);
                }

                @Override
                public void onSuccess(Agent agent) {
                    Log.debug("############ Debug: agent:" + agent);
                    //name
                    String name = "name";
                    StaticTextItem nameValue = new StaticTextItem(name, "Name");
                    nameValue.setValue(agent.getName());
                    formItems.add(nameValue);
                    //address
                    String address = "address";
                    StaticTextItem addressValue = new StaticTextItem(address, "Address");
                    addressValue.setValue(agent.getAddress());
                    formItems.add(addressValue);
                    //port
                    String port = "port";
                    StaticTextItem portValue = new StaticTextItem(port, "Port");
                    portValue.setValue(agent.getPort());
                    formItems.add(portValue);

                    //                    AgentClient client = agentManager.getAgentClient(agent);
                    //                    pingResults = client.ping(5000L);
                    //
                    //                    pingResults = agent.ping(5000L);
                    //                    //Agent Communiation status
                    //                    availabilityImage.setSrc("resources/availability_"
                    //                        + (resource.getCurrentAvailability().getAvailabilityType() == AvailabilityType.UP ? "green" : "red")
                    //                        + "_24.png");

                    //Last Received Avail report
                    //Full Endpoint
                    currentAgentInfo.setItems(formItems.toArray(new FormItem[formItems.size()]));
                    currentAgentInfo.markForRedraw();
                }
            });

            //final form population 
            currentAgentInfo.setItems(formItems.toArray(new FormItem[formItems.size()]));
        }
    }

    @Override
    public void onInit() {
        super.onInit();
        initErrorsGrid();
    }

    private void initErrorsGrid() {
        errorsGrid.setTooltip(MSG.view_summaryOverview_tooltip_detectedErrors());
        errorsGrid.getListGrid().addCellClickHandler(new CellClickHandler() {
            public void onCellClick(CellClickEvent event) {
                ListGridRecord record = event.getRecord();
                final Window w = new Window();
                w.setTitle(MSG.view_summaryOverview_title_errorDetailsWindow());
                w.setIsModal(true);
                w.setShowMinimizeButton(false);
                w.setShowModalMask(true);
                w.setWidth(640);
                w.setHeight(480);
                w.centerInPage();
                w.setCanDragResize(true);

                LocatableVLayout layout = new LocatableVLayout(errorsGrid.extendLocatorId("dialogLayout"), 10);
                layout.setDefaultLayoutAlign(Alignment.CENTER);
                layout.setLayoutMargin(10);

                w.addItem(layout);

                HTMLPane details = new HTMLPane();
                details.setContents("<pre>" + record.getAttribute(ResourceErrorsDataSource.Field.DETAIL) + "</pre>");
                layout.addMember(details);

                IButton ok = new IButton(MSG.common_button_ok());
                ok.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        w.destroy();
                    }
                });

                layout.addMember(ok);

                w.show();
            }
        });
    }

}
