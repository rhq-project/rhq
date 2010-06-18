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

import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleDestination;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.enterprise.gui.coregui.client.bundle.AbstractBundleWizard;

/**
 * @author Jay Shaughnessy
 *
 */
public abstract class AbstractBundleDeployWizard extends AbstractBundleWizard {

    // the things we build up in the wizard
    private Integer bundleId;
    private BundleDestination destination;
    private boolean isNewDestination = false;
    private BundleVersion bundleVersion;
    private boolean initialDeployment = false;
    private Configuration newDeploymentConfig;
    private String newDeploymentDescription;
    private BundleDeployment newDeployment;
    private boolean isCleanDeployment = false;
    private BundleDeployment liveDeployment;

    private boolean deployNow = true;

    public Integer getBundleId() {
        return bundleId;
    }

    public void setBundleId(Integer bundleId) {
        this.bundleId = bundleId;
    }

    public BundleVersion getBundleVersion() {
        return bundleVersion;
    }

    public void setBundleVersion(BundleVersion bundleVersion) {
        this.bundleVersion = bundleVersion;
    }

    public String getNewDeploymentDescription() {
        return newDeploymentDescription;
    }

    public void setNewDeploymentDescription(String newDeploymentDescription) {
        this.newDeploymentDescription = newDeploymentDescription;
    }

    public Configuration getNewDeploymentConfig() {
        return newDeploymentConfig;
    }

    public void setNewDeploymentConfig(Configuration newDeploymentConfig) {
        this.newDeploymentConfig = newDeploymentConfig;
    }

    public BundleDeployment getNewDeployment() {
        return newDeployment;
    }

    public void setNewDeployment(BundleDeployment newDeployment) {
        this.newDeployment = newDeployment;
    }

    public BundleDeployment getLiveDeployment() {
        return liveDeployment;
    }

    public void setLiveDeployment(BundleDeployment liveDeployment) {
        this.liveDeployment = liveDeployment;
    }

    public boolean isInitialDeployment() {
        return initialDeployment;
    }

    public void setInitialDeployment(boolean initialDeployment) {
        this.initialDeployment = initialDeployment;
    }

    public BundleDestination getDestination() {
        return destination;
    }

    public void setDestination(BundleDestination destination) {
        this.destination = destination;
    }

    public Boolean isDeployNow() {
        return deployNow;
    }

    public void setDeployNow(Boolean deployNow) {
        this.deployNow = deployNow;
    }

    public boolean isCleanDeployment() {
        return isCleanDeployment;
    }

    public void setCleanDeployment(boolean isCleanDeployment) {
        this.isCleanDeployment = isCleanDeployment;
    }

    public boolean isNewDestination() {
        return isNewDestination;
    }

    public void setNewDestination(boolean isNewDestination) {
        this.isNewDestination = isNewDestination;
    }

}
