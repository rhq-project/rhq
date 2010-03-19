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

import java.util.LinkedHashMap;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;

import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.criteria.BundleVersionCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;
import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;

/** Temporary step pending creation of BundleVersion navigation screen */

public class SelectBundleVersionStep implements WizardStep {

    private DynamicForm form;
    private final BundleDeployWizard wizard;

    private final BundleGWTServiceAsync bundleServer = GWTServiceLookup.getBundleService();

    public SelectBundleVersionStep(BundleDeployWizard bundleDeployWizard) {
        this.wizard = bundleDeployWizard;
    }

    public String getName() {
        return "Select Version of Bundle to Deploy";
    }

    public Canvas getCanvas() {
        if (form == null) {
            form = new DynamicForm();
            form.setWidth100();
            form.setNumCols(2);
            form.setColWidths("50%", "*");

            // for now, get all the bundle versions for the bundle
            final BundleVersionCriteria criteria = new BundleVersionCriteria();
            criteria.addFilterBundleId(wizard.getBundle().getId());
            criteria.fetchConfigurationDefinition(true);
            final LinkedHashMap<String, BundleVersion> bundleVersions = new LinkedHashMap<String, BundleVersion>();

            bundleServer.findBundleVersionsByCriteria(criteria, new AsyncCallback<PageList<BundleVersion>>() {
                public void onSuccess(PageList<BundleVersion> result) {
                    if (result.isEmpty()) {
                        onFailure(new IllegalArgumentException("No bundle versions defined for bundle."));
                    }
                    for (BundleVersion bundleVersion : result) {
                        bundleVersions.put(bundleVersion.getVersion(), bundleVersion);
                    }
                }

                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Failed to find defined bundles.", caught);
                }
            });

            final SelectItem selectBundle = new SelectItem("selectBundle", "Deployment Bundle");
            selectBundle.setRequired(true);
            selectBundle.setWidth(150);
            selectBundle.setValueMap(bundleVersions);
            selectBundle.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent event) {
                    wizard.setBundleVersion((BundleVersion) event.getValue());
                    enableNextButtonWhenAppropriate();
                }
            });

            form.setItems(selectBundle);
        }

        return form;
    }

    public boolean nextPage() {
        return form.validate();
    }

    public boolean isNextEnabled() {
        return (null != this.wizard.getBundle());
    }

    public boolean isPreviousEnabled() {
        return true;
    }

    private void enableNextButtonWhenAppropriate() {
        this.wizard.getView().getNextButton().setDisabled(!isNextEnabled());
    }
}
