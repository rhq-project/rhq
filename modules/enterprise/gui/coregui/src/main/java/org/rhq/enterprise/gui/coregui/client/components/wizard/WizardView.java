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

/**
 * @author Greg Hinkle
 */
public class WizardView extends VLayout {

    private Window wizardWindow;
    private Wizard wizard;
    private int currentStep;

    HLayout titleBar;
    HTMLFlow titleLabel;
    Label stepLabel;

    Label stepTitleLabel;
    HLayout contentLayout;
    ToolStrip buttonBar;

    IButton cancelButton;
    IButton previousButton;
    IButton nextButton;

    ArrayList<IButton> customButtons = new ArrayList<IButton>();
    Canvas currentCanvas;

    public WizardView(Wizard wizard) {
        super(10);
        this.wizard = wizard;
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        titleBar = new HLayout(30);
        titleBar.setHeight(50);
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
        stepTitleLabel.setStyleName("HeaderLabel");

        addMember(stepTitleLabel);

        contentLayout = new HLayout();
        contentLayout.setHeight("*");
        contentLayout.setWidth100();

        addMember(contentLayout);

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
            + wizard.getSubtitle());
    }

    private void setupButtons() {
        cancelButton = new IButton("Cancel");
        cancelButton.setDisabled(false);
        cancelButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                wizardWindow.destroy();
            }
        });
        previousButton = new IButton("Previous");
        previousButton.setDisabled(true);
        previousButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                setStep(currentStep - 1);
            }
        });
        nextButton = new IButton("Next");
        nextButton.setDisabled(true);
        nextButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {

                WizardStep step = wizard.getSteps().get(currentStep);
                if (step.nextPage()) {
                    setStep(currentStep + 1);
                }
            }
        });

        buttonBar.addMember(cancelButton);
        buttonBar.addMember(new LayoutSpacer());
        buttonBar.addMember(previousButton);
        buttonBar.addMember(nextButton);

    }

    public void setStep(int stepIndex) {
        currentStep = stepIndex;
        stepLabel.setContents("Step " + (stepIndex + 1) + " of " + wizard.getSteps().size());

        WizardStep step = wizard.getSteps().get(currentStep);

        stepTitleLabel.setContents(step.getName());

        if (stepIndex == 0) {
            previousButton.setDisabled(true);
        } else {
            previousButton.setDisabled(step.isNextEnabled());
        }

        boolean last = (stepIndex == (wizard.getSteps().size() - 1));
        if (last) {
            nextButton.setDisabled(true);
        } else {
            nextButton.setDisabled(step.isPreviousEnabled());
        }

        for (IButton button : customButtons) {
            buttonBar.removeMember(button);
        }
        customButtons.clear();
        for (IButton button : wizard.getCustomButtons(currentStep)) {
            buttonBar.addMember(button);
            customButtons.add(button);
        }

        if (currentCanvas != null) {
            contentLayout.removeMember(currentCanvas);
        }
        currentCanvas = wizard.getSteps().get(currentStep).getCanvas();
        contentLayout.addMember(currentCanvas);

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
                wizardWindow.destroy();
            }
        });
        wizardWindow.addItem(this);
        wizardWindow.show();

    }

    public void closeDialog() {
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
}
