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
package org.rhq.enterprise.gui.coregui.client.bundle.deploy;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.components.wizard.AbstractWizardStep;
import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.ErrorHandler;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;
import org.rhq.enterprise.gui.coregui.client.util.selenium.Locatable;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Jay Shaughnessy
 *
 */
public class DeployStep extends AbstractWizardStep {

    private VLayout canvas;
    private final BundleDeployWizard wizard;

    private final BundleGWTServiceAsync bundleServer = GWTServiceLookup.getBundleService();

    public DeployStep(BundleDeployWizard wizard) {
        this.wizard = wizard;
    }

    public String getName() {
        return MSG.view_bundle_deployWizard_deployStep();
    }

    public Canvas getCanvas(Locatable parent) {
        if (canvas == null) {
            if (parent != null) {
                canvas = new LocatableVLayout(parent.extendLocatorId("BundleDeployDeploy"));
            } else {
                canvas = new LocatableVLayout("BundleDeployDeploy");
            }
            canvas.setWidth100();
            canvas.setHeight100();
            canvas.setAlign(Alignment.CENTER);

            final Img deployingImage = new Img(ImageManager.getLoadingIcon());
            deployingImage.setLayoutAlign(Alignment.CENTER);
            deployingImage.setWidth(50);
            deployingImage.setHeight(15);

            final Label deployingMessage = new Label(MSG.view_bundle_deployWizard_deploying());
            deployingMessage.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

            canvas.addMember(deployingImage);
            canvas.addMember(deployingMessage);

            bundleServer.createBundleDeployment(wizard.getBundleVersion().getId(), wizard.getDestination().getId(),
                wizard.getNewDeploymentDescription(), wizard.getNewDeploymentConfig(), false, -1, false, //
                new AsyncCallback<BundleDeployment>() {
                    public void onSuccess(BundleDeployment result) {
                        deployingImage.setSrc("/images/status_complete.gif");
                        deployingMessage.setText(MSG.view_bundle_deployWizard_deploymentCreated());
                        CoreGUI.getMessageCenter().notify(
                            new Message(MSG.view_bundle_deployWizard_deploymentCreatedDetail_concise(result.getName()),
                                MSG.view_bundle_deployWizard_deploymentCreatedDetail(result.getName(), result
                                    .getDescription()), Severity.Info));
                        wizard.setNewDeployment(result);

                        bundleServer.scheduleBundleDeployment(wizard.getNewDeployment().getId(), wizard
                            .isCleanDeployment(), //
                            new AsyncCallback<BundleDeployment>() {
                                public void onSuccess(BundleDeployment result) {
                                    deployingImage.setSrc("/images/status_complete.gif");
                                    deployingMessage.setText(MSG.view_bundle_deployWizard_deploymentScheduled());
                                    CoreGUI.getMessageCenter().notify(
                                        new Message(MSG.view_bundle_deployWizard_deploymentScheduledDetail_concise(),
                                            MSG.view_bundle_deployWizard_deploymentScheduledDetail(result.getName(),
                                                result.getDestination().getGroup().getName()), Severity.Info));
                                    wizard.getView().hideMessage();
                                    CoreGUI.refresh();
                                    wizard.setNewDeployment(result);
                                }

                                public void onFailure(Throwable caught) {
                                    deployingImage.setSrc("/images/status_error.gif");
                                    deployingMessage.setText(MSG.view_bundle_deployWizard_error_3());
                                    String errMsg = MSG.view_bundle_deployWizard_error_4(ErrorHandler
                                        .getAllMessages(caught));
                                    wizard.getView().showMessage(errMsg);
                                    Message msg = new Message(MSG.view_bundle_deployWizard_error_3(), errMsg,
                                        Severity.Error);
                                    CoreGUI.getMessageCenter().notify(msg);
                                }
                            });
                    }

                    public void onFailure(Throwable caught) {
                        deployingImage.setSrc("/images/status_error.gif");
                        deployingMessage.setText(MSG.view_bundle_deployWizard_error_5());
                        String errMsg = MSG.view_bundle_deployWizard_error_6(ErrorHandler.getAllMessages(caught));
                        wizard.getView().showMessage(errMsg);
                        Message msg = new Message(MSG.view_bundle_deployWizard_error_5(), errMsg, Severity.Error);
                        CoreGUI.getMessageCenter().notify(msg);
                    }
                });
        }

        return canvas;
    }

    public boolean nextPage() {
        return true;
    }
}
