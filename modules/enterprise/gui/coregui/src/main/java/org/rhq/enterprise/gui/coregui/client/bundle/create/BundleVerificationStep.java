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
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.layout.HLayout;

import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;
import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;

/**
 * @author Greg Hinkle
 */
public class BundleVerificationStep implements WizardStep {

    private final BundleCreationWizard wizard;

    private final BundleGWTServiceAsync bundleServer = GWTServiceLookup.getBundleService();

    public BundleVerificationStep(BundleCreationWizard bundleCreationWizard) {
        this.wizard = bundleCreationWizard;
    }

    public Canvas getCanvas() {
        HLayout hlayout = new HLayout();
        hlayout.setWidth100();
        hlayout.setHeight100();

        final Img verifyingImage = new Img("/images/status-bar.gif");
        verifyingImage.setWidth100();
        verifyingImage.setHeight(15);

        hlayout.addChild(verifyingImage);

        bundleServer.createBundleAndBundleVersion(this.wizard.getBundleName(), this.wizard.getBundleType().getId(),
            this.wizard.getBundleName(), this.wizard.getBundleVersionString(), this.wizard.getRecipe(),
            new AsyncCallback<BundleVersion>() {
                public void onSuccess(BundleVersion result) {
                    verifyingImage.setSrc("/images/status_complete.gif");
                    wizard.setBundleVersion(result);
                }

                public void onFailure(Throwable caught) {
                    verifyingImage.setSrc("/images/status_error.gif");
                    CoreGUI.getErrorHandler().handleError("Failed to create bundle: " + caught.getMessage(), caught);
                }
            });
        return hlayout;
    }

    public boolean valid() {
        return false; // TODO: Implement this method.
    }

    public String getName() {
        return "Verify Recipe";
    }

}