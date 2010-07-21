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

import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleDestination;
import org.rhq.enterprise.gui.coregui.client.components.wizard.AbstractWizard;

/**
 * @author Jay Shaughnessy
 *
 */
public abstract class AbstractBundleRevertWizard extends AbstractWizard {

    // the things we build up in the wizard
    private BundleDestination destination;
    private String deploymentDescription;
    private BundleDeployment deployment;
    private boolean isCleanDeployment = false;
    private BundleDeployment liveDeployment;
    private BundleDeployment previousDeployment;

    public String getDeploymentDescription() {
        return deploymentDescription;
    }

    public void setDeploymentDescription(String deploymentDescription) {
        this.deploymentDescription = deploymentDescription;
    }

    public BundleDeployment getDeployment() {
        return deployment;
    }

    public void setDeployment(BundleDeployment deployment) {
        this.deployment = deployment;
    }

    public BundleDeployment getLiveDeployment() {
        return liveDeployment;
    }

    public void setLiveDeployment(BundleDeployment liveDeployment) {
        this.liveDeployment = liveDeployment;
    }

    public BundleDeployment getPreviousDeployment() {
        return previousDeployment;
    }

    public void setPreviousDeployment(BundleDeployment previousDeployment) {
        this.previousDeployment = previousDeployment;
    }

    public BundleDestination getDestination() {
        return destination;
    }

    public void setDestination(BundleDestination destination) {
        this.destination = destination;
    }

    public boolean isCleanDeployment() {
        return isCleanDeployment;
    }

    public void setCleanDeployment(boolean isCleanDeployment) {
        this.isCleanDeployment = isCleanDeployment;
    }
}
