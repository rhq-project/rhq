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
package org.rhq.coregui.client.bundle.revert;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.criteria.BundleDeploymentCriteria;
import org.rhq.core.domain.criteria.BundleVersionCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.components.wizard.AbstractWizardStep;
import org.rhq.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.message.Message;

/**
 * @author Jay Shaughnessy
 *
 */
public class ConfirmationStep extends AbstractWizardStep {

    private VLayout layout;
    private boolean nextPage = true;
    private final BundleRevertWizard wizard;
    private final BundleGWTServiceAsync bundleServer = GWTServiceLookup.getBundleService();

    public ConfirmationStep(BundleRevertWizard wizard) {
        this.wizard = wizard;
    }

    public String getName() {
        return MSG.view_bundle_revertWizard_confirmStep_name();
    }

    public Canvas getCanvas() {
        if (layout == null) {
            layout = new EnhancedVLayout();
            layout.setMembersMargin(15);

            // Get the Live Deployment
            BundleDeploymentCriteria c = new BundleDeploymentCriteria();
            c.addFilterDestinationId(this.wizard.getDestination().getId());
            c.addFilterIsLive(true);
            c.fetchBundleVersion(true);
            bundleServer.findBundleDeploymentsByCriteria(c, //
                new AsyncCallback<PageList<BundleDeployment>>() {

                    public void onSuccess(PageList<BundleDeployment> liveDeployments) {
                        if (1 != liveDeployments.size()) {
                            nextPage = false;
                            String messageConcise = MSG.view_bundle_revertWizard_confirmStep_noLiveDeployment_concise();
                            String message = MSG.view_bundle_revertWizard_confirmStep_noLiveDeployment(wizard
                                .getDestination().toString());
                            wizard.getView().showMessage(message);
                            CoreGUI.getMessageCenter().notify(
                                new Message(messageConcise, message, Message.Severity.Warning));
                        }

                        wizard.setLiveDeployment(liveDeployments.get(0));
                        Integer replacedBundleDeploymentId = wizard.getLiveDeployment().getReplacedBundleDeploymentId();

                        if (null == replacedBundleDeploymentId) {
                            nextPage = false;
                            String messageConcise = MSG
                                .view_bundle_revertWizard_confirmStep_noPriorDeployment_concise();
                            String message = MSG.view_bundle_revertWizard_confirmStep_noPriorDeployment(wizard
                                .getLiveDeployment().toString(), wizard.getDestination().toString());
                            wizard.getView().showMessage(message);
                            CoreGUI.getMessageCenter().notify(
                                new Message(messageConcise, message, Message.Severity.Warning));
                        }

                        // Get the Replaced Deployment (the one we want to revert to_
                        BundleDeploymentCriteria c = new BundleDeploymentCriteria();
                        c.addFilterId(replacedBundleDeploymentId);
                        bundleServer.findBundleDeploymentsByCriteria(c, //
                            new AsyncCallback<PageList<BundleDeployment>>() {

                                public void onSuccess(PageList<BundleDeployment> replacedBundleDeployments) {
                                    if (1 != replacedBundleDeployments.size()) {
                                        nextPage = false;
                                        String messageConcise = MSG
                                            .view_bundle_revertWizard_confirmStep_noPriorDeployment_concise();
                                        String message = MSG.view_bundle_revertWizard_confirmStep_noPriorDeployment(
                                            wizard.getLiveDeployment().toString(), wizard.getDestination().toString());
                                        wizard.getView().showMessage(message);
                                        CoreGUI.getMessageCenter().notify(
                                            new Message(messageConcise, message, Message.Severity.Warning));
                                    }

                                    wizard.setPreviousDeployment(replacedBundleDeployments.get(0));
                                    setLayout();
                                }

                                public void onFailure(Throwable caught) {
                                    nextPage = false;
                                    String messageConcise = MSG
                                        .view_bundle_revertWizard_confirmStep_noPriorDeployment_concise();
                                    String message = MSG.view_bundle_revertWizard_confirmStep_noPriorDeployment(wizard
                                        .getLiveDeployment().toString(), wizard.getDestination().toString());
                                    wizard.getView().showMessage(message);
                                    CoreGUI.getMessageCenter().notify(
                                        new Message(messageConcise, message, Message.Severity.Warning));
                                }
                            });
                    }

                    public void onFailure(Throwable caught) {
                        nextPage = false;
                        String message = MSG.view_bundle_revertWizard_confirmStep_failedToFindLiveDeployment() + ": "
                            + caught.getMessage();
                        wizard.getView().showMessage(message);
                        CoreGUI.getErrorHandler().handleError(
                            MSG.view_bundle_revertWizard_confirmStep_failedToFindLiveDeployment(), caught);
                    }
                });
        }

        return layout;
    }

    private void setLayout() {

        final BundleDeployment live = this.wizard.getLiveDeployment();
        final BundleDeployment prev = this.wizard.getPreviousDeployment();

        DynamicForm liveForm = new DynamicForm();
        liveForm.setNumCols(2);
        liveForm.setIsGroup(true);
        liveForm.setGroupTitle("<b>" + MSG.view_bundle_revertWizard_confirmStep_liveDeployment() + "<b>");

        StaticTextItem liveNameItem = new StaticTextItem("liveName", MSG.common_title_name());
        liveNameItem.setTitleAlign(Alignment.LEFT);
        liveNameItem.setAlign(Alignment.LEFT);
        liveNameItem.setWrap(false);
        liveNameItem.setWrapTitle(false);
        liveNameItem.setValue(live.getName());
        liveNameItem.setLeft(20);

        StaticTextItem liveDescItem = new StaticTextItem("liveDesc", MSG.common_title_description());
        liveDescItem.setTitleAlign(Alignment.LEFT);
        liveDescItem.setAlign(Alignment.LEFT);
        liveDescItem.setWrap(true);
        liveDescItem.setWrapTitle(false);
        liveDescItem.setVAlign(VerticalAlignment.TOP);
        liveDescItem.setTitleVAlign(VerticalAlignment.TOP);
        liveDescItem.setValue((null != live.getDescription()) ? live.getDescription() : MSG.common_val_none());

        StaticTextItem liveVersionItem = new StaticTextItem("liveVersion", MSG.view_bundle_bundleVersion());
        liveVersionItem.setTitleAlign(Alignment.LEFT);
        liveVersionItem.setAlign(Alignment.LEFT);
        liveVersionItem.setWrap(false);
        liveVersionItem.setWrapTitle(false);
        liveVersionItem.setValue(live.getBundleVersion().getVersion());

        liveForm.setItems(liveNameItem, liveVersionItem, liveDescItem);
        layout.addMember(liveForm);

        if (prev != null) {
            final DynamicForm prevForm = new DynamicForm();
            prevForm.setNumCols(2);
            prevForm.setIsGroup(true);
            prevForm.setGroupTitle("<b>" + MSG.view_bundle_revertWizard_confirmStep_prevDeployment() + "<b>");

            StaticTextItem prevNameItem = new StaticTextItem("prevName", MSG.common_title_name());
            prevNameItem.setTitleAlign(Alignment.LEFT);
            prevNameItem.setAlign(Alignment.LEFT);
            prevNameItem.setWrap(false);
            prevNameItem.setWrapTitle(false);
            prevNameItem.setValue(prev.getName());

            StaticTextItem prevDescItem = new StaticTextItem("prevDesc", MSG.common_title_description());
            prevDescItem.setTitleAlign(Alignment.LEFT);
            prevDescItem.setAlign(Alignment.LEFT);
            prevDescItem.setWrap(true);
            prevDescItem.setWrapTitle(false);
            prevDescItem.setTitleVAlign(VerticalAlignment.TOP);
            prevDescItem.setVAlign(VerticalAlignment.TOP);
            prevDescItem.setValue((null != prev.getDescription()) ? prev.getDescription() : MSG.common_val_none());

            final StaticTextItem prevVersionItem = new StaticTextItem("prevVersion", MSG.view_bundle_bundleVersion());
            prevVersionItem.setTitleAlign(Alignment.LEFT);
            prevVersionItem.setAlign(Alignment.LEFT);
            prevVersionItem.setWrap(false);
            prevVersionItem.setWrapTitle(false);
            if (prev.getBundleVersion().getVersion() == null) {
                BundleVersionCriteria c = new BundleVersionCriteria();
                c.addFilterId(prev.getBundleVersion().getId());
                bundleServer.findBundleVersionsByCriteria(c, new AsyncCallback<PageList<BundleVersion>>() {

                    @Override
                    public void onSuccess(PageList<BundleVersion> result) {
                        if (result != null && result.size() == 1) {
                            prevVersionItem.setValue(result.get(0).getVersion());
                            prevForm.markForRedraw();
                        }
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        prevVersionItem.setValue("?");
                        prevForm.markForRedraw();
                    }
                });
            } else {
                prevVersionItem.setValue(prev.getBundleVersion().getVersion());
            }

            prevForm.setItems(prevNameItem, prevVersionItem, prevDescItem);
            layout.addMember(prevForm);
        }

        Label confirmation = new Label();
        confirmation.setContents("<b>" + MSG.view_bundle_revertWizard_confirmStep_confirmation() + "</b>");
        confirmation.setMargin(20);
        confirmation.setWidth100();
        layout.addMember(confirmation);

    }

    public boolean nextPage() {
        return nextPage;
    }
}
