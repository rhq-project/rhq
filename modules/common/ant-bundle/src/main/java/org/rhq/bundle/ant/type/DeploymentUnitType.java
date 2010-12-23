/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
 * * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.bundle.ant.type;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;

import org.rhq.bundle.ant.DeployPropertyNames;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.template.TemplateEngine;
import org.rhq.core.util.updater.DeployDifferences;
import org.rhq.core.util.updater.Deployer;
import org.rhq.core.util.updater.DeploymentData;
import org.rhq.core.util.updater.DeploymentProperties;

/**
 * An Ant task for deploying a bundle or previewing the deployment.
 *
 * @author Ian Springer
 */
public class DeploymentUnitType extends AbstractBundleType {
    private String name;
    private String manageRootDir = Boolean.TRUE.toString();
    private Map<File, File> files = new LinkedHashMap<File, File>();
    private Set<File> rawFilesToReplace = new LinkedHashSet<File>();
    private Set<File> archives = new LinkedHashSet<File>();
    private Map<File, Boolean> archivesExploded = new HashMap<File, Boolean>();
    private Map<File, Pattern> archiveReplacePatterns = new HashMap<File, Pattern>();
    private SystemServiceType systemService;
    private Pattern ignorePattern;
    private String preinstallTarget;
    private String postinstallTarget;

    public void init() throws BuildException {
        if (this.systemService != null) {
            this.systemService.init();
        }
    }

    public void install(boolean revert, boolean clean) throws BuildException {
        if (this.preinstallTarget != null) {
            Target target = (Target) getProject().getTargets().get(this.preinstallTarget);
            if (target == null) {
                throw new BuildException("Specified preinstall target (" + this.preinstallTarget + ") does not exist.");
            }
            target.performTasks();
        }

        int deploymentId = getProject().getDeploymentId();
        DeploymentProperties deploymentProps = new DeploymentProperties(deploymentId, getProject().getBundleName(),
            getProject().getBundleVersion(), getProject().getBundleDescription());
        File deployDir = getProject().getDeployDir();
        TemplateEngine templateEngine = createTemplateEngine();

        if (this.files.isEmpty() && this.archives.isEmpty()) {
            throw new BuildException(
                "You must specify at least one file to deploy via nested rhq:file, rhq:archive, and/or rhq:system-service elements.");
        }
        if (!this.files.isEmpty()) {
            log("Deploying files " + this.files + "...", Project.MSG_VERBOSE);
        }
        if (!this.archives.isEmpty()) {
            log("Deploying archives " + this.archives + "...", Project.MSG_VERBOSE);
        }

        boolean willManageRootDir = Boolean.parseBoolean(this.manageRootDir);
        if (willManageRootDir) {
            log("Managing the root directory of this deployment unit - unrelated files found will be removed",
                Project.MSG_VERBOSE);
        } else {
            log("Not managing the root directory of this deployment unit - unrelated files will remain intact",
                Project.MSG_VERBOSE);
        }

        DeploymentData deploymentData = new DeploymentData(deploymentProps, this.archives, this.files, getProject()
            .getBaseDir(), deployDir, this.archiveReplacePatterns, this.rawFilesToReplace, templateEngine,
            this.ignorePattern, willManageRootDir, this.archivesExploded);
        Deployer deployer = new Deployer(deploymentData);
        try {
            DeployDifferences diffs = getProject().getDeployDifferences();
            boolean dryRun = getProject().isDryRun();
            if (revert) {
                deployer.redeployAndRestoreBackupFiles(diffs, clean, dryRun);
            } else {
                deployer.deploy(diffs, clean, dryRun);
            }
            getProject().log("Results:\n" + diffs + "\n");
        } catch (Exception e) {
            throw new BuildException("Failed to deploy bundle '" + getProject().getBundleName() + "' version "
                + getProject().getBundleVersion() + ": " + e, e);
        }

        if (this.systemService != null) {
            this.systemService.install();
        }

        if (this.postinstallTarget != null) {
            Target target = (Target) getProject().getTargets().get(this.postinstallTarget);
            if (target == null) {
                throw new BuildException("Specified postinstall target (" + this.postinstallTarget
                    + ") does not exist.");
            }
            target.performTasks();
        }

        return;
    }

    public void start() throws BuildException {
        if (this.systemService != null) {
            this.systemService.start();
        }
    }

    public void stop() throws BuildException {
        if (this.systemService != null) {
            this.systemService.stop();
        }
    }

    public void upgrade(boolean revert, boolean clean) throws BuildException {
        install(revert, clean);
    }

    public void uninstall() throws BuildException {
        if (this.systemService != null) {
            this.systemService.uninstall();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getManageRootDir() {
        return manageRootDir;
    }

    public void setManageRootDir(String booleanString) {
        if (!Boolean.TRUE.toString().equalsIgnoreCase(booleanString)
            && !Boolean.FALSE.toString().equalsIgnoreCase(booleanString)) {
            throw new BuildException("manageRootDir attribute must be 'true' or 'false': " + booleanString);
        }
        this.manageRootDir = booleanString;
    }

    public Map<File, File> getFiles() {
        return files;
    }

    public Set<File> getArchives() {
        return archives;
    }

    /**
     * Returns a map keyed on {@link #getArchives() archive names} whose values
     * are either true or false, where true means the archive is to be deployed exploded
     * and false means the archive should be deployed in compressed form.
     * 
     * @return map showing how an archive should be deployed in its final form
     */
    public Map<File, Boolean> getArchivesExploded() {
        return archivesExploded;
    }

    public String getPreinstallTarget() {
        return preinstallTarget;
    }

    public void setPreinstallTarget(String preinstallTarget) {
        this.preinstallTarget = preinstallTarget;
    }

    public String getPostinstallTarget() {
        return postinstallTarget;
    }

    public void setPostinstallTarget(String postinstallTarget) {
        this.postinstallTarget = postinstallTarget;
    }

    public void addConfigured(SystemServiceType systemService) {
        if (this.systemService != null) {
            throw new IllegalStateException(
                "A rhq:deployment-unit element can only have one rhq:system-service child element.");
        }
        this.systemService = systemService;
        this.systemService.validate();

        // Add the init script and its config file to the list of bundle files.
        this.files.put(this.systemService.getScriptFile(), this.systemService.getScriptDestFile());
        if (this.systemService.getConfigFile() != null) {
            this.files.put(this.systemService.getConfigFile(), this.systemService.getConfigDestFile());
            this.rawFilesToReplace.add(this.systemService.getConfigFile());
        }
    }

    public void addConfigured(FileType file) {
        File destFile = file.getDestinationFile();
        if (destFile == null) {
            File destDir = file.getDestinationDir();
            destFile = new File(destDir, file.getSource().getName());
        }
        this.files.put(file.getSource(), destFile);
        if (file.isReplace()) {
            this.rawFilesToReplace.add(file.getSource());
        }
    }

    public void addConfigured(ArchiveType archive) {
        this.archives.add(archive.getSource());
        Pattern replacePattern = archive.getReplacePattern();
        if (replacePattern != null) {
            this.archiveReplacePatterns.put(archive.getSource(), replacePattern);
        }
        Boolean exploded = Boolean.valueOf(archive.getExploded());
        this.archivesExploded.put(archive.getSource(), exploded);
    }

    public void addConfigured(IgnoreType ignore) {
        List<FileSet> fileSets = ignore.getFileSets();
        this.ignorePattern = getPattern(fileSets);
    }

    private TemplateEngine createTemplateEngine() {
        TemplateEngine templateEngine = SystemInfoFactory.fetchTemplateEngine();
        // Add the deployment props to the template engine's tokens.
        Configuration config = getProject().getConfiguration();
        for (PropertySimple prop : config.getSimpleProperties().values()) {
            templateEngine.getTokens().put(prop.getName(), prop.getStringValue());
        }
        // And add the special rhq.deploy.dir prop.
        templateEngine.getTokens().put(DeployPropertyNames.DEPLOY_DIR,
            getProject().getProperty(DeployPropertyNames.DEPLOY_DIR));
        return templateEngine;
    }
}