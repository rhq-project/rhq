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
package org.rhq.enterprise.gui.coregui.client.inventory.common.detail.operation.schedule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.wizard.AbstractWizard;
import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;
import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardView;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * @author Greg Hinkle
 */
public class OperationCreateWizard extends AbstractWizard {

    private Resource resource;
    private OperationDefinition operationDefinition;

    private OperationParametersStep parametersStep;
    private OperationSchedulingStep schedulingStep;

    private WizardView view;

    private IButton executeNowButton;
    private IButton executeButton;

    public OperationCreateWizard(Resource resource, OperationDefinition operationDefinition) {
        this.resource = resource;
        this.operationDefinition = operationDefinition;
    }

    public void startOperationWizard() {

        parametersStep = new OperationParametersStep(operationDefinition);
        schedulingStep = new OperationSchedulingStep();

        ArrayList<WizardStep> steps = new ArrayList<WizardStep>();
        steps.add(parametersStep);
        steps.add(schedulingStep);
        setSteps(steps);

        executeNowButton = new IButton(MSG.view_operationCreateWizard_button_executeImmediately());
        executeNowButton.setAutoFit(true);
        executeNowButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                execute();
            }
        });

        executeButton = new IButton(MSG.view_operationCreateWizard_button_execute());
        executeButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                execute();
            }
        });

        view = new WizardView(this);
        view.displayDialog();
    }

    public String getWindowTitle() {
        return MSG.view_operationCreateWizard_title();
    }

    public String getTitle() {
        return MSG.view_operationCreateWizard_header(operationDefinition.getDisplayName(), resource.getName());
    }

    public String getSubtitle() {
        return operationDefinition.getDescription();
    }

    public List<IButton> getCustomButtons(int step) {
        switch (step) {
        case 0:
            return Arrays.asList(executeNowButton);
        case 1:
            return Arrays.asList(executeButton);
        default:
            return Collections.emptyList();
        }
    }

    private void execute() {
        Configuration parameters = parametersStep.getParameterConfiguration();
        final ExecutionSchedule schedule = schedulingStep.getExecutionSchedule();

        // TODO: get the description and timeout from user input
        String description = "";
        int timeout = 0;

        AsyncCallback<Void> operationCallback = new AsyncCallback<Void>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_operationCreateWizard_error_scheduleOperationFailure(),
                    caught);
            }

            public void onSuccess(Void result) {
                String opName = operationDefinition.getDisplayName();
                String cron = schedule.getCronString();
                if (cron == null) {
                    cron = MSG.common_val_na();
                }
                String concise = MSG.view_operationCreateWizard_message_scheduleOperationSuccess_short(opName);
                String message = MSG.view_operationCreateWizard_message_scheduleOperationSuccess(opName, String
                    .valueOf(resource.getId()), cron);

                CoreGUI.getMessageCenter().notify(new Message(concise, message, Message.Severity.Info));
            }
        };

        if (schedule.getStart() == ExecutionSchedule.Start.Immediately) {
            GWTServiceLookup.getOperationService().invokeResourceOperation(resource.getId(),
                operationDefinition.getName(), parameters, description, timeout, operationCallback);
        } else {
            GWTServiceLookup.getOperationService().scheduleResourceOperation(resource.getId(),
                operationDefinition.getName(), parameters, description, timeout, schedule.getCronString(),
                operationCallback);
        }

        view.closeDialog();
    }

    public void cancel() {
        // TODO: revert back to original state
    }
}
