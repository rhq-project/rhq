/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.core.clientapi.agent.bundle;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;

import org.rhq.core.domain.bundle.BundleResourceDeployment;

/**
 * The request that the server sends down to the agent to schedule a bundle deployment.
 * 
 * @author John Mazzitelli
 */
public class BundleScheduleRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private BundleResourceDeployment resourceDeployment;
    private long requestedDeployTime = System.currentTimeMillis();
    private boolean isCleanDeployment = false;
    private boolean isRevert = false;

    public BundleScheduleRequest(BundleResourceDeployment resourceDeployment) {
        this.resourceDeployment = resourceDeployment;
    }

    /**
     * @return Data describing what needs to get deployed.
     */
    public BundleResourceDeployment getBundleResourceDeployment() {
        return resourceDeployment;
    }

    /**
     * @return the time the deployment should be scheduled to happen, in epoch millis.
     */
    public Long getRequestedDeployTime() {
        return requestedDeployTime;
    }

    public String getRequestedDeployTimeAsString() {
        return DateFormat.getInstance().format(new Date(requestedDeployTime));
    }

    /**
     * @return flag to indicate if the deployment directory should have all of its files deleted
     *         prior to deploying the new files. All files in the deployment directory will be cleaned,
     *         including files/directories that are marked to be "ignored".
     */
    public boolean isCleanDeployment() {
        return isCleanDeployment;
    }

    public void setCleanDeployment(boolean isCleanDeployment) {
        this.isCleanDeployment = isCleanDeployment;
    }

    /**
     * @return flag to indicate if this bundle deployment request is reverting a deployment back to
     *         a previous state. Reverting means that any files backed up from the last deployment
     *         are reverted to their original state.
     */
    public boolean isRevert() {
        return isRevert;
    }

    public void setRevert(boolean isRevert) {
        this.isRevert = isRevert;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder(this.getClass() + ": ");
        str.append("deployment=[").append(resourceDeployment.toString()).append("], ");
        str.append("clean=[").append(isCleanDeployment).append("], ");
        str.append("revert=[").append(isRevert).append("]");
        return str.toString();
    }
}
