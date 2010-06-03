/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.bundle.ant;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.tools.ant.Project;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.util.updater.DeployDifferences;

/**
 * This is the Ant project object that is used when processing bundle Ant scripts
 * (aka "bundle recipes").
 * 
 * It extends the normal Ant project object by providing additional methods that help
 * collect additional information about the Ant script.
 * 
 * This project object is to be used by either the bundle {@link AntLauncher} or custom
 * bundle Ant tasks. The launcher or tasks can inform this project object of things that
 * are happening as the Ant script is being parsed and/or executed.
 * 
 * @author John Mazzitelli
 * @author Ian Springer
 */
public class BundleAntProject extends Project {
    // Bundle-level attributes
    private String bundleName;
    private String bundleVersion;
    private String bundleDescription;
    private ConfigurationDefinition configDef;

    // Deployment-level attributes
    private Configuration config;
    private File deployDir;
    private final Set<String> bundleFileNames = new HashSet<String>();
    private int deploymentId;
    private DeploymentPhase deploymentPhase;
    private DeployDifferences deployDiffs = new DeployDifferences();
    private boolean dryRun;

    public Set<String> getBundleFileNames() {
        return bundleFileNames;
    }

    public ConfigurationDefinition getConfigurationDefinition() {
        if (configDef == null) {
            configDef = new ConfigurationDefinition("Ant Bundle Deployment", null);
        }
        return configDef;
    }

    public Configuration getConfiguration() {
        if (config == null) {
            config = new Configuration();
        }
        return config;
    }

    public String getBundleName() {
        return bundleName;
    }

    public void setBundleName(String bundleName) {
        this.bundleName = bundleName;
    }

    public String getBundleVersion() {
        return bundleVersion;
    }

    public void setBundleVersion(String bundleVersion) {
        this.bundleVersion = bundleVersion;
    }

    public String getBundleDescription() {
        return bundleDescription;
    }

    public void setBundleDescription(String bundleDescription) {
        this.bundleDescription = bundleDescription;
    }

    public File getDeployDir() {
        return deployDir;
    }

    public void setDeployDir(File deployDir) {
        this.deployDir = deployDir;
    }

    public int getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(int deploymentId) {
        this.deploymentId = deploymentId;
    }

    public DeploymentPhase getDeploymentPhase() {
        return deploymentPhase;
    }

    public void setDeploymentPhase(DeploymentPhase deploymentPhase) {
        this.deploymentPhase = deploymentPhase;
    }

    public DeployDifferences getDeployDifferences() {
        return deployDiffs;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public boolean isDryRun() {
        return dryRun;
    }
}
