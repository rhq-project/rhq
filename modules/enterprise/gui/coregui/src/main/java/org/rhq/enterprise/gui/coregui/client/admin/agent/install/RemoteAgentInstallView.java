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
import com.smartgwt.client.types.ExpansionMode;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
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
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.RemoteInstallGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 */
public class RemoteAgentInstallView extends LocatableVLayout {
    public static final ViewName VIEW_ID = new ViewName("RemoteAgentInstall", MSG
        .view_adminTopology_remoteAgentInstall());

    private RemoteInstallGWTServiceAsync remoteInstallService = GWTServiceLookup.getRemoteInstallService();

    private DynamicForm connectionForm;
    private DynamicForm buttonsForm;
    private ButtonItem installButton;
    private ButtonItem startButton;
    private ButtonItem stopButton;
    private VLayout agentInfoLayout;

    public RemoteAgentInstallView(String locatorId) {
        super(locatorId);
        setMembersMargin(1);
        setWidth100();
        setHeight100();
    }

    @Override
    protected void onInit() {
        super.onInit();

        addMember(getConnectionForm());
        addMember(getButtons());

        agentInfoLayout = new VLayout();
        agentInfoLayout.setWidth100();
        agentInfoLayout.setHeight100();
        agentInfoLayout.setMembersMargin(1);
        addMember(agentInfoLayout);

    }

    private DynamicForm getConnectionForm() {
        connectionForm = new LocatableDynamicForm(this.extendLocatorId("Connection"));
        connectionForm.setWidth100();
        connectionForm.setNumCols(3);
        connectionForm.setWrapItemTitles(false);
        connectionForm.setColWidths("25%", "50%", "25%");
        connectionForm.setMargin(20);

        HeaderItem connectionHeader = new HeaderItem();
        connectionHeader.setDefaultValue(MSG.view_remoteAgentInstall_connInfo());

        TextItem host = new TextItem("host", MSG.common_title_host());
        host.setRequired(true);
        host.setWidth("100%");
        host.setPrompt(MSG.view_remoteAgentInstall_promptHost());
        host.setColSpan(2);

        TextItem port = new TextItem("port", MSG.common_title_port());
        port.setRequired(false);
        port.setWidth("90");
        port.setPrompt(MSG.view_remoteAgentInstall_promptPort());
        port.setColSpan(1);

        TextItem username = new TextItem("username", MSG.common_title_user());
        username.setRequired(true);
        username.setWidth("100%");
        username.setPrompt(MSG.view_remoteAgentInstall_promptUser());
        username.setColSpan(2);

        PasswordItem password = new PasswordItem("password", MSG.common_title_password());
        password.setRequired(false);
        password.setWidth("100%");
        password.setPrompt(MSG.view_remoteAgentInstall_promptPassword());
        password.setColSpan(2);

        TextItem agentInstallPath = new TextItem("agentInstallPath", MSG.view_remoteAgentInstall_installPath());
        agentInstallPath.setRequired(true);
        agentInstallPath.setWidth("100%");
        agentInstallPath.setPrompt(MSG.view_remoteAgentInstall_promptInstallPath());
        agentInstallPath.setStartRow(true);
        agentInstallPath.setEndRow(false);

        ButtonItem findAgentInstallPathButton = new ButtonItem("findAgentInstallPathButton", MSG
            .view_remoteAgentInstall_buttonFindAgent());
        findAgentInstallPathButton.setStartRow(false);
        findAgentInstallPathButton.setEndRow(true);
        findAgentInstallPathButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                if (connectionForm.validate()) {
                    findAgentInstallPath();
                }
            }
        });

        StaticTextItem agentStatus = new StaticTextItem("agentStatus", MSG.view_remoteAgentInstall_agentStatus());
        agentStatus.setDefaultValue(MSG.view_remoteAgentInstall_agentStatusDefault());
        agentStatus.setRedrawOnChange(true);
        agentStatus.setRedrawOnChange(true);
        agentStatus.setWidth("100%");
        agentStatus.setStartRow(true);
        agentStatus.setEndRow(false);

        ButtonItem statusCheckButton = new ButtonItem("updateStatus", MSG.view_remoteAgentInstall_updateStatus());
        statusCheckButton.setStartRow(false);
        statusCheckButton.setEndRow(true);
        statusCheckButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                if (connectionForm.validate()) {
                    agentStatusCheck();
                }
            }
        });

        connectionForm.setFields(connectionHeader, host, port, username, password, agentInstallPath,
            findAgentInstallPathButton, agentStatus, statusCheckButton);

        return connectionForm;
    }

    private DynamicForm getButtons() {
        buttonsForm = new LocatableDynamicForm(this.extendLocatorId("ButtonForm"));
        buttonsForm.setWidth("75%");
        buttonsForm.setNumCols(4);
        buttonsForm.setMargin(20);
        buttonsForm.setColWidths("10%", "30%", "30%", "30%");

        HeaderItem buttonsHeader = new HeaderItem();
        buttonsHeader.setDefaultValue(MSG.common_title_operations());

        SpacerItem spacerItem = new SpacerItem();
        spacerItem.setStartRow(true);
        spacerItem.setEndRow(false);

        installButton = new ButtonItem("install", MSG.view_remoteAgentInstall_installAgent());
        installButton.setStartRow(false);
        installButton.setEndRow(false);
        installButton.setRedrawOnChange(true);
        installButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                if (connectionForm.validate()) {
                    installAgent();
                }
            }
        });

        startButton = new ButtonItem("start", MSG.view_remoteAgentInstall_startAgent());
        startButton.setStartRow(false);
        startButton.setEndRow(false);
        // startButton.setShowIfCondition(new FormItemIfFunction() {
        //     public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
        //         return form.getValue("agentStatus") != null && !"Agent Not Installed".equals(form.getValue("agentStatus"));
        //     }
        // });
        startButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                if (connectionForm.validate()) {
                    startAgent();
                }
            }
        });

        stopButton = new ButtonItem("stop", MSG.view_remoteAgentInstall_stopAgent());
        stopButton.setStartRow(false);
        stopButton.setEndRow(true);
        stopButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                if (connectionForm.validate()) {
                    stopAgent();
                }
            }
        });

        buttonsForm.setFields(buttonsHeader, spacerItem, installButton, startButton, stopButton);
        return buttonsForm;
    }

    private void findAgentInstallPath() {
        disableButtons(true);

        final String parentPath = getAgentInstallPath();

        remoteInstallService.findAgentInstallPath(getRemoteAccessInfo(), parentPath, new AsyncCallback<String>() {
            public void onFailure(Throwable caught) {
                disableButtons(false);
                CoreGUI.getErrorHandler().handleError(MSG.view_remoteAgentInstall_error_1(), caught);
            }

            public void onSuccess(String result) {
                disableButtons(false);
                if (result != null) {
                    connectionForm.setValue("agentInstallPath", result);
                } else {
                    String err;
                    if (parentPath == null || parentPath.length() == 0) {
                        err = MSG.view_remoteAgentInstall_error_2();
                    } else {
                        err = MSG.view_remoteAgentInstall_error_3(parentPath);
                    }
                    CoreGUI.getErrorHandler().handleError(err);
                }
                agentStatusCheck();
            }
        });
    }

    private void agentStatusCheck() {
        disableButtons(true);
        remoteInstallService.agentStatus(getRemoteAccessInfo(), getAgentInstallPath(), new AsyncCallback<String>() {
            public void onFailure(Throwable caught) {
                disableButtons(false);
                connectionForm.setValue("agentStatus", caught.getMessage());
            }

            public void onSuccess(String result) {
                disableButtons(false);
                connectionForm.setValue("agentStatus", result);
            }
        });
    }

    private void installAgent() {
        disableButtons(true);

        // FOR TESTING WITHOUT DOING A REAL INSTALL - START
        //        AgentInstallInfo result = new AgentInstallInfo("mypath", "myown", "1.1", "localHOST", "serverHOST");
        //        for (int i = 1; i < 20; i++)
        //            result.addStep(new AgentInstallStep("cmd" + i, "desc" + i, i, "result" + i, i * 10));
        //        for (Canvas child : agentInfoLayout.getChildren())
        //            child.destroy();
        //        buildInstallInfoCanvas(agentInfoLayout, result);
        //        agentInfoLayout.markForRedraw();
        //        disableButtons(false);
        //        if (true)
        //            return;
        // FOR TESTING WITHOUT DOING A REAL INSTALL - END

        remoteInstallService.installAgent(getRemoteAccessInfo(), getAgentInstallPath(),
            new AsyncCallback<AgentInstallInfo>() {
                public void onFailure(Throwable caught) {
                    disableButtons(false);
                    CoreGUI.getErrorHandler().handleError(MSG.view_remoteAgentInstall_error_4(), caught);
                }

                public void onSuccess(AgentInstallInfo result) {
                    disableButtons(false);
                    CoreGUI.getMessageCenter().notify(
                        new Message(MSG.view_remoteAgentInstall_success(), Message.Severity.Info));

                    for (Canvas child : agentInfoLayout.getChildren()) {
                        child.destroy();
                    }
                    buildInstallInfoCanvas(agentInfoLayout, result);
                    agentInfoLayout.markForRedraw();
                    agentStatusCheck();
                }
            });
    }

    private void startAgent() {
        disableButtons(true);
        remoteInstallService.startAgent(getRemoteAccessInfo(), getAgentInstallPath(), new AsyncCallback<String>() {
            public void onFailure(Throwable caught) {
                disableButtons(false);
                CoreGUI.getErrorHandler().handleError(MSG.view_remoteAgentInstall_error_5(), caught);
            }

            public void onSuccess(String result) {
                disableButtons(false);
                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.view_remoteAgentInstall_startAgentResults(result), Message.Severity.Info));
                agentStatusCheck();
            }
        });
    }

    private void stopAgent() {
        disableButtons(true);
        remoteInstallService.stopAgent(getRemoteAccessInfo(), getAgentInstallPath(), new AsyncCallback<String>() {
            public void onFailure(Throwable caught) {
                disableButtons(false);
                CoreGUI.getErrorHandler().handleError(MSG.view_remoteAgentInstall_error_6(), caught);
            }

            public void onSuccess(String result) {
                disableButtons(false);
                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.view_remoteAgentInstall_stopAgentResults(result), Message.Severity.Info));
                agentStatusCheck();
            }
        });
    }

    private void buildInstallInfoCanvas(VLayout installInfo, AgentInstallInfo info) {
        DynamicForm infoForm = new DynamicForm();
        infoForm.setMargin(20);
        infoForm.setWidth100();
        infoForm.setHeight100();

        HeaderItem infoHeader = new HeaderItem();
        infoHeader.setValue(MSG.view_remoteAgentInstall_installInfo());

        StaticTextItem version = new StaticTextItem("version", MSG.common_title_version());
        version.setValue(info.getVersion());

        StaticTextItem path = new StaticTextItem("path", MSG.common_title_path());
        path.setValue(info.getPath());

        StaticTextItem owner = new StaticTextItem("owner", MSG.view_remoteAgentInstall_owner());
        owner.setValue(info.getOwner());

        StaticTextItem config = new StaticTextItem("config", MSG.common_title_configuration());
        config.setValue(info.getConfigurationStartString());

        CanvasItem listCanvas = new CanvasItem();
        listCanvas.setShowTitle(false);
        listCanvas.setColSpan(2);

        VLayout listLayout = new VLayout(0);
        listLayout.setWidth100();
        listLayout.setHeight100();

        ListGrid listGrid = new ListGrid() {
            @Override
            protected Canvas getExpansionComponent(ListGridRecord record) {
                Canvas canvas = super.getExpansionComponent(record);
                canvas.setPadding(5);
                return canvas;
            }
        };
        listGrid.setWidth100();
        listGrid.setHeight100();
        listGrid.setCanExpandRecords(true);
        listGrid.setExpansionMode(ExpansionMode.DETAIL_FIELD);
        listGrid.setDetailField("result");
        ListGridField step = new ListGridField("description", MSG.view_remoteAgentInstall_step());
        ListGridField result = new ListGridField("result", MSG.view_remoteAgentInstall_result());
        ListGridField resultCode = new ListGridField("resultCode", MSG.view_remoteAgentInstall_resultCode(), 90);
        ListGridField duration = new ListGridField("duration", MSG.common_title_duration(), 90);
        listGrid.setFields(step, result, resultCode, duration);
        listGrid.setData(getStepRecords(info));

        listLayout.addMember(listGrid);
        listCanvas.setCanvas(listLayout);

        infoForm.setFields(infoHeader, version, path, owner, config, listCanvas);
        installInfo.addMember(infoForm);

        return;
    }

    private ListGridRecord[] getStepRecords(AgentInstallInfo info) {
        ArrayList<ListGridRecord> steps = new ArrayList<ListGridRecord>();

        for (AgentInstallStep step : info.getSteps()) {
            ListGridRecord rec = new ListGridRecord();
            rec.setAttribute("description", step.getDescription());
            String result = step.getResult();
            if (result == null || result.trim().length() == 0) {
                result = MSG.view_remoteAgentInstall_resultCode() + "=" + step.getResultCode();
            }
            rec.setAttribute("result", result);
            rec.setAttribute("resultCode", "" + step.getResultCode());
            rec.setAttribute("duration", MeasurementConverterClient.format((double) step.getDuration(),
                MeasurementUnits.MILLISECONDS, true));
            steps.add(rec);
        }

        return steps.toArray(new ListGridRecord[steps.size()]);
    }

    private void disableButtons(boolean disabled) {
        installButton.setDisabled(disabled);
        startButton.setDisabled(disabled);
        stopButton.setDisabled(disabled);
        buttonsForm.setDisabled(disabled);
    }

    private RemoteAccessInfo getRemoteAccessInfo() {
        String host = connectionForm.getValueAsString("host");
        String port = connectionForm.getValueAsString("port");
        String username = connectionForm.getValueAsString("username");
        String password = connectionForm.getValueAsString("password");

        int portInt;
        try {
            portInt = Integer.parseInt(port);
        } catch (NumberFormatException e) {
            portInt = 22;
        }
        connectionForm.setValue("port", portInt);

        RemoteAccessInfo info = new RemoteAccessInfo(host, portInt, username, password);
        return info;
    }

    private String getAgentInstallPath() {
        return connectionForm.getValueAsString("agentInstallPath");
    }
}
