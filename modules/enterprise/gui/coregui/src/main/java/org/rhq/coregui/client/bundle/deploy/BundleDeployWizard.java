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
package org.rhq.coregui.client.bundle.deploy;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleDeploymentStatus;
import org.rhq.core.domain.bundle.BundleDestination;
import org.rhq.core.domain.criteria.BundleDeploymentCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.components.wizard.WizardStep;
import org.rhq.coregui.client.gwt.GWTServiceLookup;

/**
 * @author Jay Shaughnessy
 *
 */
public class BundleDeployWizard extends AbstractBundleDeployWizard {

    private Bundle bundle;

    // Deployment of specified bundle
    public BundleDeployWizard(Bundle bundle) {
        this.setInitialDeployment(true);
        this.setBundleId(bundle.getId());
        this.bundle = bundle;

        List<WizardStep> steps = init();
        steps.add(new GetDestinationStep(this));
        steps.add(new SelectBundleVersionStep(this));
        steps.add(new GetDeploymentConfigStep(this));
        steps.add(new GetDeploymentInfoStep(this));
        steps.add(new DeployStep(this));
    }

    // Redeploy to existing destination
    public BundleDeployWizard(BundleDestination destination) {
        if (null == destination) {
            throw new IllegalArgumentException("destination is null");
        }

        this.setInitialDeployment(false);
        this.setBundleId(destination.getBundle().getId());
        this.setDestination(destination);

        List<WizardStep> steps = init();
        steps.add(new SelectBundleVersionStep(this));
        steps.add(new GetDeploymentConfigStep(this));
        steps.add(new GetDeploymentInfoStep(this));
        steps.add(new DeployStep(this));
    }

    public Bundle getBundle() {
        return bundle;
    }

    private List<WizardStep> init() {
        setWindowTitle(MSG.view_bundle_deployWizard_title());
        setTitle(MSG.view_bundle_bundleDeployment());

        ArrayList<WizardStep> steps = new ArrayList<WizardStep>();
        setSteps(steps);
        return steps;
    }

    public void cancel() {
        // delete a newly created deployment but only if it's status is failure and it has
        // no deployments.  This should be rare, or maybe impossible, but in an odd case that
        // the deployment fails and they user wants to Cancel as opposed to Finish, let's
        // clean up as best as possible.
        if ((null != getNewDeployment()) && (0 < getNewDeployment().getId())) {
            BundleDeploymentCriteria c = new BundleDeploymentCriteria();
            c.addFilterId(getNewDeployment().getId());
            c.fetchResourceDeployments(true);
            GWTServiceLookup.getBundleService().findBundleDeploymentsByCriteria(c, //
                new AsyncCallback<PageList<BundleDeployment>>() {
                    public void onSuccess(PageList<BundleDeployment> newDeploymentList) {
                        if (!newDeploymentList.isEmpty()) {
                            BundleDeployment newDeployment = newDeploymentList.get(0);
                            boolean isFailedToLaunch = BundleDeploymentStatus.FAILURE.equals(newDeployment.getStatus())
                                || BundleDeploymentStatus.PENDING.equals(newDeployment.getStatus());
                            boolean hasNoResourceDeployments = ((null == newDeployment.getResourceDeployments()) || newDeployment
                                .getResourceDeployments().isEmpty());

                            // go ahead and delete it if it hasn't really done anything but get created.
                            // otherwise, let folks inspect via the ui and take further action.
                            // if the deployment can't be deleted then don't try to delete the destination,
                            // it's now in use by the deployment
                            if (isFailedToLaunch && hasNoResourceDeployments) {
                                GWTServiceLookup.getBundleService().deleteBundleDeployment(newDeployment.getId(), //
                                    new AsyncCallback<Void>() {
                                        public void onSuccess(Void voidReturn) {
                                            deleteNewDestination();
                                        }

                                        public void onFailure(Throwable caught) {
                                            CoreGUI.getErrorHandler().handleError(
                                                MSG.view_bundle_deployWizard_error_1(), caught);
                                        }
                                    });
                            }
                        }
                    }

                    public void onFailure(Throwable caught) {
                        // should not really get here
                        CoreGUI.getErrorHandler().handleError(MSG.view_bundle_deployWizard_error_1(), caught);
                        deleteNewDestination();
                    }
                });
        } else {
            deleteNewDestination();
        }
    }

    private void deleteNewDestination() {
        if (this.isNewDestination() && (null != this.getDestination())) {
            GWTServiceLookup.getBundleService().deleteBundleDestination(this.getDestination().getId(), //
                new AsyncCallback<Void>() {
                    public void onSuccess(Void voidReturn) {
                        CoreGUI.refresh();
                    }

                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_bundle_deployWizard_error_2(), caught);
                    }
                });
        }
    }
}
