/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.tools.ant.Project;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.util.updater.DeployDifferences;
import org.rhq.core.util.updater.DestinationComplianceMode;

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
 * Also provides a common method for any task to invoke to send an audit message.
 * 
 * @author John Mazzitelli
 * @author Ian Springer
 */
public class BundleAntProject extends Project {
    // these statuses should match those of see BundleResourceDeploymentHistory.Status
    public enum AuditStatus {
        SUCCESS, FAILURE, WARN, INFO
    }

    // Bundle-level attributes
    private boolean parseOnly;

    private String bundleName;
    private String bundleVersion;
    private String bundleDescription;

    // Deployment-level attributes
    private ConfigurationDefinition configDef;
    private Configuration config;
    private File deployDir;
    private final Set<String> bundleFileNames = new HashSet<String>();
    private int deploymentId;
    private DeploymentPhase deploymentPhase;
    private boolean dryRun;

    //note that this will have to change once we start supporting multiple deployment units.
    private DestinationComplianceMode destinationCompliance;

    private HandoverTarget handoverTarget;

    // results of project execution
    private DeployDifferences deployDiffs = new DeployDifferences();
    private Set<File> downloadedFiles = new HashSet<File>();

    public BundleAntProject() {
        this(false);
    }

    public BundleAntProject(boolean parseOnly) {
        this.parseOnly = parseOnly;
    }

    public boolean isParseOnly() {
        return parseOnly;
    }

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

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public DeployDifferences getDeployDifferences() {
        return deployDiffs;
    }

    public DestinationComplianceMode getDestinationCompliance() {
        return destinationCompliance;
    }

    public void setDestinationCompliance(DestinationComplianceMode destinationCompliance) {
        this.destinationCompliance = destinationCompliance;
    }

    public HandoverTarget getHandoverTarget() {
        return handoverTarget;
    }

    public void setHandoverTarget(HandoverTarget handoverTarget) {
        this.handoverTarget = handoverTarget;
    }

    /**
     * If there were url-file or url-archives, this returns the set of files
     * that were downloaded from the URLs.
     * 
     * @return downloaded files from remote URLs that contain our bundle content
     */
    public Set<File> getDownloadedFiles() {
        return downloadedFiles;
    }

    /**
     * Logs a message in a format that our audit task/agent-side audit log listener knows about.
     * When running in the agent, this audit log will be sent to the server.
     * It is always logged at part of the normal Ant logger mechanism.
     * 
     * @param status SUCCESS, FAILURE, WARN, INFO
     * @param action audit action, a short summary easily displayed (e.g "File Download")
     * @param info information about the action target, easily displayed (e.g. "myfile.zip")
     * @param message Optional, brief (one or two lines) information message
     * @param details Optional, verbose data, such as full file text or long error messages/stack traces  
     */
    public void auditLog(AuditStatus status, String action, String info, String message, String details) {
        if (status == null) {
            status = AuditStatus.SUCCESS;
        }

        // this will log a message with a very specific format that is understood
        // by the agent-side build listener's messageLogged method:
        // org.rhq.plugins.ant.DeploymentAuditorBuildListener.messageLogged(BuildEvent)
        // RHQ_AUDIT_MESSAGE___<status>___<action>___<info>___<message>___<details>
        StringBuilder str = new StringBuilder("RHQ_AUDIT_MESSAGE___");
        str.append(status.name());
        str.append("___");
        str.append((action != null) ? action : "Audit Message");
        str.append("___");
        if (info != null) {
            str.append(info);
        } else {
            str.append("Timestamp: ").append(new Date());
        }
        str.append("___");
        str.append((message != null) ? message : "");
        str.append("___");
        str.append((details != null) ? details : "");
        this.log(str.toString(), Project.MSG_INFO);
    }
}
