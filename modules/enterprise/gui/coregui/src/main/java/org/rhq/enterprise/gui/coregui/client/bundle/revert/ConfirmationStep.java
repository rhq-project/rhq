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
package org.rhq.enterprise.gui.coregui.client.bundle.revert;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.criteria.BundleDeploymentCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.wizard.AbstractWizardStep;
import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

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
        return "Revert Deployment Confirmation";
    }

    public Canvas getCanvas() {
        if (layout == null) {
            layout = new LocatableVLayout("BundleRevertConfirmation");
            layout.setMembersMargin(10);

            BundleDeploymentCriteria c = new BundleDeploymentCriteria();
            c.addFilterDestinationId(this.wizard.getDestination().getId());
            c.addFilterIsLive(true);
            c.fetchReplacedBundleDeployment(true);
            c.fetchBundleVersion(true);
            bundleServer.findBundleDeploymentsByCriteria(c, //
                new AsyncCallback<PageList<BundleDeployment>>() {
                    public void onSuccess(PageList<BundleDeployment> liveDeployments) {
                        if (1 != liveDeployments.size()) {
                            nextPage = false;
                            String message = "No live deployment found for destination [" + wizard.getDestination()
                                + "]";
                            wizard.getView().showMessage(message);
                            CoreGUI.getMessageCenter().notify(new Message(message, Message.Severity.Warning));
                        }
                        wizard.setLiveDeployment(liveDeployments.get(0));
                        wizard.setPreviousDeployment(wizard.getLiveDeployment().getReplacedBundleDeployment());
                        if (null == wizard.getPreviousDeployment()) {
                            nextPage = false;
                            String message = "Live deployment [" + wizard.getLiveDeployment()
                                + "] can not be reverted. There is no prior deployment for the destination ["
                                + wizard.getDestination() + "]";
                            wizard.getView().showMessage(message);
                            CoreGUI.getMessageCenter().notify(new Message(message, Message.Severity.Warning));
                        }

                        setLayout();
                    }

                    public void onFailure(Throwable caught) {
                        nextPage = false;
                        String message = "Failed to find live deployment, can not revert: " + caught.getMessage();
                        wizard.getView().showMessage(message);
                        CoreGUI.getErrorHandler().handleError(message, caught);
                    }
                });
        }

        return layout;
    }

    private void setLayout() {

        BundleDeployment live = this.wizard.getLiveDeployment();
        BundleDeployment prev = this.wizard.getPreviousDeployment();

        Label liveHeader = new Label();
        liveHeader.setContents("<b>Live Deployment:<b>");
        liveHeader.setHeight(10);
        liveHeader.setWidth100();
        layout.addMember(liveHeader);

        DynamicForm liveForm = new DynamicForm();
        liveForm.setNumCols(2);

        StaticTextItem liveNameItem = new StaticTextItem("liveName", "Name");
        liveNameItem.setTitleAlign(Alignment.LEFT);
        liveNameItem.setAlign(Alignment.LEFT);
        liveNameItem.setWrap(false);
        liveNameItem.setValue(live.getName());
        liveNameItem.setLeft(20);

        StaticTextItem liveDescItem = new StaticTextItem("liveDesc", "Description");
        liveDescItem.setTitleAlign(Alignment.LEFT);
        liveDescItem.setAlign(Alignment.LEFT);
        liveDescItem.setWrap(false);
        liveDescItem.setValue((null != live.getName()) ? live.getName() : "none");

        StaticTextItem liveVersionItem = new StaticTextItem("liveVersion", "Bundle Version");
        liveVersionItem.setTitleAlign(Alignment.LEFT);
        liveVersionItem.setAlign(Alignment.LEFT);
        liveVersionItem.setWrap(false);
        liveVersionItem.setValue(live.getBundleVersion().getVersion());

        liveForm.setItems(liveNameItem, liveDescItem, liveVersionItem);
        layout.addMember(liveForm);

        Label prevHeader = new Label();
        prevHeader.setContents("<b>Previous Deployment:</b>");
        prevHeader.setHeight(20);
        prevHeader.setWidth100();
        layout.addMember(prevHeader);

        DynamicForm prevForm = new DynamicForm();
        prevForm.setNumCols(2);

        StaticTextItem prevNameItem = new StaticTextItem("prevName", "Name");
        prevNameItem.setTitleAlign(Alignment.LEFT);
        prevNameItem.setAlign(Alignment.LEFT);
        prevNameItem.setWrap(false);
        prevNameItem.setValue(prev.getName());

        StaticTextItem prevDescItem = new StaticTextItem("prevDesc", "Description");
        prevDescItem.setTitleAlign(Alignment.LEFT);
        prevDescItem.setAlign(Alignment.LEFT);
        prevDescItem.setWrap(false);
        prevDescItem.setValue((null != prev.getName()) ? prev.getName() : "none");

        prevForm.setItems(prevNameItem, prevDescItem);
        layout.addMember(prevForm);

        Label confirmation = new Label();
        confirmation
            .setContents("<b>Reverting Live Deployment to Previous Deployment. Click \'Next\' to continue...</b>");
        confirmation.setMargin(20);
        confirmation.setWidth100();
        layout.addMember(confirmation);

    }

    public boolean nextPage() {
        return nextPage;
    }
}
