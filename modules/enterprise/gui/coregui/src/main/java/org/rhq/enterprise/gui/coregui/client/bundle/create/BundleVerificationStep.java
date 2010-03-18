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
package org.rhq.enterprise.gui.coregui.client.bundle.create;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;
import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;

public class BundleVerificationStep implements WizardStep {

    private final BundleCreationWizard wizard;

    private final BundleGWTServiceAsync bundleServer = GWTServiceLookup.getBundleService();
    private VLayout canvas;

    public BundleVerificationStep(BundleCreationWizard bundleCreationWizard) {
        this.wizard = bundleCreationWizard;
    }

    public Canvas getCanvas() {
        canvas = new VLayout();
        canvas.setWidth100();
        canvas.setHeight100();
        canvas.setAlign(Alignment.CENTER);

        final Img verifyingImage = new Img("/images/status-bar.gif");
        verifyingImage.setLayoutAlign(Alignment.CENTER);
        verifyingImage.setWidth(50);
        verifyingImage.setHeight(15);

        final Label verifiedMessage = new Label("Verifying...");
        verifiedMessage.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        canvas.addMember(verifyingImage);
        canvas.addMember(verifiedMessage);

        bundleServer.createBundleAndBundleVersion(this.wizard.getBundleName(), this.wizard.getBundleType().getId(),
            this.wizard.getBundleName(), this.wizard.getBundleVersionString(), this.wizard.getBundleDescription(),
            this.wizard.getRecipe(), new AsyncCallback<BundleVersion>() {
                public void onSuccess(BundleVersion result) {
                    verifyingImage.setSrc("/images/status_complete.gif");
                    verifiedMessage.setText("Verified!");
                    CoreGUI.getMessageCenter().notify(
                        new Message("Created bundle [" + result.getName() + "] version [" + result.getVersion() + "]",
                            Severity.Info));
                    wizard.setBundleVersion(result);
                    enableNextButtonWhenAppropriate();
                }

                public void onFailure(Throwable caught) {
                    verifyingImage.setSrc("/images/status_error.gif");
                    verifiedMessage.setText("Failed!");
                    CoreGUI.getErrorHandler().handleError("Failed to create bundle: " + caught.getMessage(), caught);
                    wizard.setBundleVersion(null);
                    enableNextButtonWhenAppropriate();
                }
            });

        return canvas;
    }

    public boolean nextPage() {
        return this.wizard.getBundleVersion() != null;
    }

    public String getName() {
        return "Verify Recipe";
    }

    public boolean isNextEnabled() {
        return this.wizard.getBundleVersion() != null;
    }

    public boolean isPreviousEnabled() {
        return true;
    }

    private void enableNextButtonWhenAppropriate() {
        this.wizard.getView().getNextButton().setDisabled(!isNextEnabled());
    }
}