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
package org.rhq.coregui.client.bundle.create;

import java.util.HashMap;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;

import org.rhq.core.domain.bundle.BundleGroup;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.bundle.composite.BundleGroupAssignmentComposite;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.components.wizard.AbstractWizard;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.message.Message.Severity;

public abstract class AbstractBundleCreateWizard extends AbstractWizard {

    // the things we build up in the wizard
    private String recipe;
    private BundleVersion bundleVersion;
    private String createInitialBundleVersionToken;
    private String createInitialBundleVersionRecipe;
    private HashMap<String, Boolean> allBundleFilesStatus; // bundle file names with their upload status (true=they were uploaded)
    private BundleGroupAssignmentComposite bundleGroupAssignmentComposite;
    private Set<BundleGroup> initialBundleGroups;

    public String getRecipe() {
        return recipe;
    }

    public void setRecipe(String recipe) {
        this.recipe = recipe;
    }

    public BundleVersion getBundleVersion() {
        return bundleVersion;
    }

    public void setBundleVersion(BundleVersion bv) {
        this.bundleVersion = bv;
        if (bv != null) {
            setSubtitle(bv.getName() + " (" + bv.getVersion() + ")");
        }
    }

    public HashMap<String, Boolean> getAllBundleFilesStatus() {
        return allBundleFilesStatus;
    }

    public void setAllBundleFilesStatus(HashMap<String, Boolean> allBundleFilesStatus) {
        this.allBundleFilesStatus = allBundleFilesStatus;
    }

    public String getCreateInitialBundleVersionToken() {
        return createInitialBundleVersionToken;
    }

    public void setCreateInitialBundleVersionToken(String createInitialBundleVersionToken) {
        this.createInitialBundleVersionToken = createInitialBundleVersionToken;
    }

    public String getCreateInitialBundleVersionRecipe() {
        return createInitialBundleVersionRecipe;
    }

    public void setCreateInitialBundleVersionRecipe(String createInitialBundleVersionRecipe) {
        this.createInitialBundleVersionRecipe = createInitialBundleVersionRecipe;
    }

    public boolean isInitialVersion() {
        return ((null != createInitialBundleVersionToken) || (null != createInitialBundleVersionRecipe));
    }

    public BundleGroupAssignmentComposite getBundleGroupAssignmentComposite() {
        return bundleGroupAssignmentComposite;
    }

    public void setBundleGroupAssignmentComposite(BundleGroupAssignmentComposite bundleGroupAssignmentComposite) {
        this.bundleGroupAssignmentComposite = bundleGroupAssignmentComposite;
    }

    public Set<BundleGroup> getInitialBundleGroups() {
        return initialBundleGroups;
    }

    public void setInitialBundleGroups(Set<BundleGroup> initialBundleGroups) {
        this.initialBundleGroups = initialBundleGroups;
    }

    public void cancel() {
        final BundleVersion bv = getBundleVersion();
        if (bv != null) {
            // the user must have created it already after verification step, delete it, if possible 
            GWTServiceLookup.getBundleService().deleteBundleVersion(bv.getId(), true, new AsyncCallback<Void>() {
                public void onSuccess(Void result) {
                    CoreGUI.getMessageCenter().notify(
                        new Message(MSG.view_bundle_createWizard_cancelSuccessful(bv.getName(), bv.getVersion()),
                            Severity.Info));
                }

                public void onFailure(Throwable caught) {
                    String msg = MSG.view_bundle_createWizard_cancelFailure(bv.getName(), bv.getVersion());
                    // provide a more specific message if the cancel failed due to the user not having delete permission
                    if (caught.getMessage().contains("PermissionException")) {
                        msg = MSG.view_bundle_createWizard_cancelFailurePerm(bv.getName(), bv.getVersion());
                    }
                    CoreGUI.getErrorHandler().handleError(msg, caught);
                }
            });
        }
    }
}
