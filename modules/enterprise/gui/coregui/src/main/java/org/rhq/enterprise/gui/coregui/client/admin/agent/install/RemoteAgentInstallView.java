/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.admin.agent.install;

import java.util.ArrayList;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Autofit;
import com.smartgwt.client.types.ExpansionMode;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.HeaderItem;
import com.smartgwt.client.widgets.form.fields.PasswordItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.install.remote.AgentInstallInfo;
import org.rhq.core.domain.install.remote.AgentInstallStep;
import org.rhq.core.domain.install.remote.RemoteAccessInfo;
import org.rhq.core.domain.measurement.MeasurementConverterClient;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.RemoteInstallGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * @author Greg Hinkle
 */
public class RemoteAgentInstallView extends VLayout {

    private RemoteInstallGWTServiceAsync remoteInstallService = GWTServiceLookup.getRemoteInstallService();

    private DynamicForm form;
    private VLayout agentInfoLayout;

    public RemoteAgentInstallView() {
        setMembersMargin(10);
    }

    @Override
    protected void onInit() {
        super.onInit();

        addMember(getConnectionForm());

        agentInfoLayout = new VLayout();
        agentInfoLayout.setWidth100();
        addMember(agentInfoLayout);

    }

    DynamicForm getConnectionForm() {

        form = new DynamicForm();
        form.setWidth100();

        HeaderItem connectionHeader = new HeaderItem();
        connectionHeader.setValue("Connection Information");

        TextItem host = new TextItem("host", "Hostname");
        host.setRequired(true);

        TextItem username = new TextItem("username", "Username");
        username.setRequired(true);

        PasswordItem password = new PasswordItem("password", "Password");
        //        password.setRequired(true);

        TextItem agentInstallPath = new TextItem("agentInstallPath", "Agent Install Path");
        agentInstallPath.setRequired(true);

        ButtonItem statusCheck = new ButtonItem("updateStatus", "Update Status");

        final StaticTextItem agentStatus = new StaticTextItem("agentStatus", "Agent Status");
        agentStatus.setRedrawOnChange(true);

        statusCheck.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                if (form.validate()) {
                    agentStatusCheck();
                }
            }
        });

        ButtonItem startButton = new ButtonItem("start", "Start Agent");
        startButton.setEndRow(false);
        //        startButton.setShowIfCondition(new FormItemIfFunction() {
        //            public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
        //                return form.getValue("agentStatus") != null && !"Agent Not Installed".equals(form.getValue("agentStatus"));
        //            }
        //        });
        startButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                remoteInstallService.startAgent(getRemoteAccessInfo(), getAgentInstallPath(),
                    new AsyncCallback<String>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError("Failed to start agent", caught);
                        }

                        public void onSuccess(String result) {
                            CoreGUI.getMessageCenter().notify(
                                new Message("Agent successfully started.", Message.Severity.Info));
                            agentStatusCheck();
                        }
                    });
            }
        });

        ButtonItem stopButton = new ButtonItem("stop", "Stop Agent");
        stopButton.setStartRow(false);
        //        stopButton.setShowIfCondition(new FormItemIfFunction() {
        //            public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
        //                return form.getValue("agentStatus") != null && !"Agent Not Installed".equals(form.getValue("agentStatus"));
        //            }
        //        });
        stopButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                remoteInstallService.stopAgent(getRemoteAccessInfo(), getAgentInstallPath(),
                    new AsyncCallback<String>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError("Failed to stop agent", caught);
                        }

                        public void onSuccess(String result) {
                            CoreGUI.getMessageCenter().notify(
                                new Message("Agent successfully stopped.", Message.Severity.Info));
                            agentStatusCheck();
                        }
                    });
            }
        });

        ButtonItem installButton = new ButtonItem("install", "Install Agent");
        installButton.setRedrawOnChange(true);
        //        installButton.setShowIfCondition(new FormItemIfFunction() {
        //            public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
        //                return form.getValue("agentStatus") != null && "Agent Not Installed".equals(form.getValue("agentStatus"));
        //            }
        //        });

        installButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                remoteInstallService.installAgent(getRemoteAccessInfo(), getAgentInstallPath(),
                    new AsyncCallback<AgentInstallInfo>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError("Failed to install agent", caught);
                        }

                        public void onSuccess(AgentInstallInfo result) {
                            CoreGUI.getMessageCenter().notify(
                                new Message("Agent successfully installed.", Message.Severity.Info));

                            for (Canvas child : agentInfoLayout.getChildren()) {
                                child.destroy();
                            }
                            agentInfoLayout.addMember(buildInstallInfoCanvas(result));
                            agentInfoLayout.markForRedraw();
                            agentStatusCheck();
                        }
                    });
            }
        });

        form.setFields(connectionHeader, host, username, password, agentInstallPath, statusCheck, agentStatus,
            new SpacerItem(), startButton, stopButton, new SpacerItem(), installButton, new SpacerItem());

        return form;
    }

    private void agentStatusCheck() {
        remoteInstallService.agentStatus(getRemoteAccessInfo(), getAgentInstallPath(), new AsyncCallback<String>() {
            public void onFailure(Throwable caught) {
                form.setValue("agentStatus", caught.getMessage());
            }

            public void onSuccess(String result) {
                form.setValue("agentStatus", result);
            }
        });
    }

    private Canvas buildInstallInfoCanvas(AgentInstallInfo info) {

        VLayout installInfo = new VLayout();
        installInfo.setWidth100();

        DynamicForm infoForm = new DynamicForm();
        infoForm.setWidth100();
        HeaderItem infoHeader = new HeaderItem();
        infoHeader.setValue("Agent Installation Information");
        StaticTextItem version = new StaticTextItem("version", "Version");
        version.setValue(info.getVersion());

        StaticTextItem path = new StaticTextItem("path", "Path");
        path.setValue(info.getPath());

        StaticTextItem owner = new StaticTextItem("owner", "Owner");
        owner.setValue(info.getOwner());

        StaticTextItem config = new StaticTextItem("config", "Configuration");
        config.setValue(info.getConfigurationStartString());

        infoForm.setFields(infoHeader, version, path, owner, config);

        installInfo.addMember(infoForm);

        ListGrid listGrid = new ListGrid() {
            @Override
            protected Canvas getExpansionComponent(ListGridRecord record) {
                Canvas canvas = super.getExpansionComponent(record);
                canvas.setPadding(5);
                return canvas;
            }
        };
        listGrid.setWidth(800);
        listGrid.setHeight(500);
        listGrid.setCanExpandRecords(true);
        listGrid.setExpansionMode(ExpansionMode.DETAIL_FIELD);
        listGrid.setDetailField("result");

        listGrid.setAutoFitData(Autofit.VERTICAL);
        ListGridField step = new ListGridField("description", "Step");
        ListGridField resultCode = new ListGridField("resultCode", "Result Code", 90);
        ListGridField duration = new ListGridField("duration", "Duration", 90);

        listGrid.setFields(step, resultCode, duration);

        listGrid.setData(getStepRecords(info));

        installInfo.addMember(listGrid);

        for (ListGridRecord rec : listGrid.getRecords()) {
            listGrid.expandRecord(rec);
        }

        return installInfo;

    }

    private ListGridRecord[] getStepRecords(AgentInstallInfo info) {
        ArrayList<ListGridRecord> steps = new ArrayList<ListGridRecord>();

        for (AgentInstallStep step : info.getSteps()) {
            ListGridRecord rec = new ListGridRecord();
            rec.setAttribute("description", step.getDescription());
            rec.setAttribute("result", step.getResult());
            rec.setAttribute("resultCode", "" + step.getResultCode());
            rec.setAttribute("duration", MeasurementConverterClient.format((double) step.getDuration(),
                MeasurementUnits.MILLISECONDS, true));
            steps.add(rec);
        }

        return steps.toArray(new ListGridRecord[steps.size()]);
    }

    private RemoteAccessInfo getRemoteAccessInfo() {
        String host = form.getValueAsString("host");
        String username = form.getValueAsString("username");
        String password = form.getValueAsString("password");
        RemoteAccessInfo info = new RemoteAccessInfo(host, username, password);
        return info;
    }

    private String getAgentInstallPath() {
        return form.getValueAsString("agentInstallPath");
    }
}
