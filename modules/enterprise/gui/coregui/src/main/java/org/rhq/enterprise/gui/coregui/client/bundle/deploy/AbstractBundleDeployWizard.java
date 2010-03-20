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

import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleDeployDefinition;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.enterprise.gui.coregui.client.bundle.AbstractBundleWizard;

public abstract class AbstractBundleDeployWizard extends AbstractBundleWizard {

    // the things we build up in the wizard
    private Bundle bundle;
    private BundleVersion bundleVersion;
    private BundleDeployDefinition bundleDeployDefinition;
    private BundleDeployment bundleDeployment;
    private String name;
    private String description;
    private ConfigurationTemplate template;
    private Configuration config;
    private Integer platformGroupId;
    private Integer platformId;
    private Boolean newDefinition = Boolean.TRUE;
    private Boolean deployNow = null;

    public Bundle getBundle() {
        return bundle;
    }

    public void setBundle(Bundle bundle) {
        this.bundle = bundle;
    }

    public BundleVersion getBundleVersion() {
        return bundleVersion;
    }

    public void setBundleVersion(BundleVersion bundleVersion) {
        this.bundleVersion = bundleVersion;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Configuration getConfig() {
        return config;
    }

    public ConfigurationTemplate getTemplate() {
        return template;
    }

    public void setTemplate(ConfigurationTemplate template) {
        this.template = template;
    }

    public void setConfig(Configuration config) {
        this.config = config;
    }

    public BundleDeployDefinition getBundleDeployDefinition() {
        return bundleDeployDefinition;
    }

    public void setBundleDeployDefinition(BundleDeployDefinition bundleDeployDefinition) {
        this.bundleDeployDefinition = bundleDeployDefinition;
    }

    public boolean isNewDefinition() {
        return Boolean.TRUE.equals(newDefinition);
    }

    public Boolean getNewDefinition() {
        return newDefinition;
    }

    public void setNewDefinition(Boolean newDefinition) {
        this.newDefinition = newDefinition;
    }

    public boolean isDeployNow() {
        return Boolean.TRUE.equals(deployNow);
    }

    public Boolean getDeployNow() {
        return deployNow;
    }

    public void setDeployNow(Boolean deployNow) {
        this.deployNow = deployNow;
    }

    public Integer getPlatformGroupId() {
        return platformGroupId;
    }

    public void setPlatformGroupId(Integer platformGroupId) {
        this.platformGroupId = platformGroupId;
    }

    public Integer getPlatformId() {
        return platformId;
    }

    public void setPlatformId(Integer platformId) {
        this.platformId = platformId;
    }

    public BundleDeployment getBundleDeployment() {
        return bundleDeployment;
    }

    public void setBundleDeployment(BundleDeployment bundleDeployment) {
        this.bundleDeployment = bundleDeployment;
    }

}
