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
package org.rhq.enterprise.gui.coregui.client.components.wizard;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.events.CloseClientEvent;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;

/**
 * @author Greg Hinkle
 */
public class WizardView extends VLayout {

    static private final String CANCEL = "Cancel";
    static private final String NEXT = "Next";
    static private final String PREVIOUS = "Previous";

    private Window wizardWindow;
    private Wizard wizard;
    private int currentStep;

    HLayout titleBar;
    HTMLFlow titleLabel;
    Label stepLabel;

    Label stepTitleLabel;
    HLayout contentLayout;

    HLayout messageBar;
    HTMLFlow messageLabel;

    ToolStrip buttonBar;

    IButton cancelButton;
    IButton previousButton;
    IButton nextButton;

    ArrayList<IButton> customButtons = new ArrayList<IButton>();
    Canvas currentCanvas;

    HashSet<Canvas> createdCanvases = new HashSet<Canvas>();

    public WizardView(Wizard wizard) {
        super(10);
        this.wizard = wizard;
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        titleBar = new HLayout(30);
        titleBar.setHeight(50);
        titleBar.setPadding(10);
        titleBar.setBackgroundColor("#F0F0F0");
        titleLabel = new HTMLFlow();
        refreshTitleLabelContents();
        titleLabel.setWidth("*");

        stepLabel = new Label();
        stepLabel.setWidth(60);
        titleBar.addMember(titleLabel);
        titleBar.addMember(stepLabel);

        addMember(titleBar);

        stepTitleLabel = new Label();
        stepTitleLabel.setWidth100();
        stepTitleLabel.setHeight(40);
        stepTitleLabel.setPadding(20);
        stepTitleLabel.setStyleName("HeaderLabel");

        addMember(stepTitleLabel);

        contentLayout = new HLayout();
        contentLayout.setHeight("*");
        contentLayout.setWidth100();
        contentLayout.setPadding(10);

        addMember(contentLayout);

        messageBar = new HLayout();
        messageBar.setHeight(20);
        messageBar.setPadding(2);
        messageBar.setBackgroundColor("#F0F0F0");
        messageLabel = new HTMLFlow();
        messageLabel.setWidth("*");
        messageLabel.setLeft(20);
        messageBar.addMember(messageLabel);
        messageBar.setVisible(false);

        addMember(messageBar);

        buttonBar = new ToolStrip();
        buttonBar.setPadding(5);
        buttonBar.setWidth100();
        buttonBar.setMembersMargin(15);

        setupButtons();

        addMember(buttonBar);

        setStep(0);
    }

    /**
     * You can call this if you ever change the wizard's title or subtitle and you want to see it
     * reflected in the wizard UI.
     */
    public void refreshTitleLabelContents() {
        this.titleLabel.setContents("<span class=\"HeaderLabel\">" + wizard.getTitle() + "</span><br/>"
            + (wizard.getSubtitle() != null ? wizard.getSubtitle() : ""));
    }

    public void showMessage(String message) {
        if (null == message) {
            hideMessage();
        } else {
            this.messageLabel.setContents("<span style='color:red'>" + message + "</span>");
            this.messageBar.show();
        }
    }

    public void hideMessage() {
        this.messageBar.hide();
    }

    private void setupButtons() {
        cancelButton = new LocatableIButton("Cancel", CANCEL);
        cancelButton.setDisabled(false);
        cancelButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                wizard.cancel();
                closeDialog();
            }
        });
        previousButton = new LocatableIButton("Previous", PREVIOUS);
        previousButton.setDisabled(true);
        previousButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {

                WizardStep step = wizard.getSteps().get(currentStep);
                if (step.previousPage()) {
                    decrementStep();
                }
            }
        });
        nextButton = new LocatableIButton("Next", NEXT);
        nextButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {

                WizardStep step = wizard.getSteps().get(currentStep);
                if (step.nextPage()) {
                    incrementStep();
                }
            }
        });

        buttonBar.addMember(cancelButton);
        buttonBar.addMember(new LayoutSpacer());
        buttonBar.addMember(previousButton);
        buttonBar.addMember(nextButton);

    }

    private void setStep(int stepIndex) {
        if (stepIndex < 0) {
            this.previousButton.setDisabled(true);
            return;
        }

        // determine if we are "finished" - that is, going past our last step
        List<WizardStep> wizardSteps = wizard.getSteps();
        if (stepIndex >= wizardSteps.size()) {
            closeDialog();
            return;
        }

        // a valid step, continue 
        currentStep = stepIndex;

        stepLabel.setContents("Step " + (stepIndex + 1) + " of " + wizardSteps.size());
        stepLabel.setWrap(false);

        WizardStep step = wizardSteps.get(currentStep);

        stepTitleLabel.setContents(step.getName());

        previousButton.setDisabled(stepIndex == 0);

        boolean last = (stepIndex == (wizardSteps.size() - 1));
        if (last) {
            nextButton.setTitle("Finish");
        } else {
            nextButton.setTitle("Next");
        }

        for (IButton button : customButtons) {
            buttonBar.removeMember(button);
        }
        customButtons.clear();
        List<IButton> cbs = wizard.getCustomButtons(currentStep);
        if (cbs != null) {
            for (IButton button : cbs) {
                buttonBar.addMember(button);
                customButtons.add(button);
            }
        }

        if (currentCanvas != null) {
            contentLayout.removeMember(currentCanvas);
        }
        currentCanvas = wizardSteps.get(currentStep).getCanvas();
        createdCanvases.add(currentCanvas);

        contentLayout.addMember(currentCanvas);

        // clean any message from a previous step
        hideMessage();

        markForRedraw();
    }

    public void displayDialog() {
        wizardWindow = new Window();
        wizardWindow.setTitle(wizard.getWindowTitle());
        wizardWindow.setWidth(800);
        wizardWindow.setHeight(600);
        wizardWindow.setIsModal(true);
        wizardWindow.setShowModalMask(true);
        wizardWindow.setCanDragResize(true);
        wizardWindow.setShowResizer(true);
        wizardWindow.centerInPage();
        wizardWindow.addCloseClickHandler(new CloseClickHandler() {
            public void onCloseClick(CloseClientEvent closeClientEvent) {
                wizard.cancel();
                closeDialog();
            }
        });
        wizardWindow.addItem(this);
        wizardWindow.show();

    }

    public void closeDialog() {

        // Attempt to clean up canvases created in the steps
        for (Canvas canvas : createdCanvases) {
            canvas.destroy();
        }

        wizardWindow.destroy();
    }

    public IButton getCancelButton() {
        return cancelButton;
    }

    public IButton getPreviousButton() {
        return previousButton;
    }

    public IButton getNextButton() {
        return nextButton;
    }

    public ArrayList<IButton> getCustomButtons() {
        return customButtons;
    }

    public void incrementStep() {
        setStep(currentStep + 1);
    }

    public void decrementStep() {
        setStep(currentStep - 1);
    }
}
