/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.core.domain.bundle.composite;

import java.io.Serializable;

public class BundleWithLatestVersionComposite implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer bundleId;
    private String bundleName;
    private String bundleDescription;
    private String latestVersion;
    private Long deploymentCount;

    public BundleWithLatestVersionComposite(Integer bundleId, String bundleName, String bundleDescription,
        String latestVersion, Long deploymentCount) {

        this.bundleId = bundleId;
        this.bundleName = bundleName;
        this.bundleDescription = bundleDescription;
        this.latestVersion = latestVersion;
        this.deploymentCount = deploymentCount;
    }

    public Integer getBundleId() {
        return bundleId;
    }

    public void setBundleId(Integer bundleId) {
        this.bundleId = bundleId;
    }

    public String getBundleName() {
        return bundleName;
    }

    public void setBundleName(String bundleName) {
        this.bundleName = bundleName;
    }

    public String getBundleDescription() {
        return bundleDescription;
    }

    public void setBundleDescription(String bundleDescription) {
        this.bundleDescription = bundleDescription;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public void setLatestVersion(String latestVersion) {
        this.latestVersion = latestVersion;
    }

    public Long getDeploymentCount() {
        return deploymentCount;
    }

    public void setDeploymentCount(Long deploymentCount) {
        this.deploymentCount = deploymentCount;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BundleWithLatestVersionComposite [bundleId=").append(bundleId);
        builder.append(", bundleName=").append(bundleName);
        builder.append(", bundleDescription=").append(bundleDescription);
        builder.append(", latestVersion=").append(latestVersion);
        builder.append(", deploymentCount=").append(deploymentCount);
        builder.append("]");
        return builder.toString();
    }

}
