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

package org.rhq.core.clientapi.agent.upgrade;

import java.io.Serializable;

import org.rhq.core.domain.configuration.Configuration;

/**
 * Represents a response to a resource upgrade request.
 * The upgraded* properties contain the values of the corresponding resource properties
 * as they were stored on the server.
 *
 * @author Lukas Krejci
 */
public class ResourceUpgradeResponse implements Serializable {

    private static final long serialVersionUID = 3L;

    private int resourceId;

    private String upgradedResourceName;
    private String upgradedResourceKey;
    private String upgradedResourceDescription;
    private String upgradedResourceVersion;
    private Configuration upgradedResourcePluginConfiguration;

    public ResourceUpgradeResponse() {
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public String getUpgradedResourceName() {
        return upgradedResourceName;
    }

    public void setUpgradedResourceName(String upgradedResourceName) {
        this.upgradedResourceName = upgradedResourceName;
    }

    public String getUpgradedResourceKey() {
        return upgradedResourceKey;
    }

    public Configuration getUpgradedResourcePluginConfiguration() {
        return upgradedResourcePluginConfiguration;
    }

    public void setUpgradedResourceKey(String upgradedResourceKey) {
        this.upgradedResourceKey = upgradedResourceKey;
    }

    public String getUpgradedResourceDescription() {
        return upgradedResourceDescription;
    }

    public void setUpgradedResourceDescription(String upgradedResourceDescription) {
        this.upgradedResourceDescription = upgradedResourceDescription;
    }

    public String getUpgradedResourceVersion() {
        return upgradedResourceVersion;
    }

    public void setUpgradedResourceVersion(String upgradedResourceVersion) {
        this.upgradedResourceVersion = upgradedResourceVersion;
    }

    public void setUpgradedResourcePluginConfiguration(Configuration upgradedResourcePluginConfiguration) {
        this.upgradedResourcePluginConfiguration = upgradedResourcePluginConfiguration;
    }
}
