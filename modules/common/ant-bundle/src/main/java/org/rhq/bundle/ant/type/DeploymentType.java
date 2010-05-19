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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.template.TemplateEngine;
import org.rhq.core.util.updater.DeployDifferences;
import org.rhq.core.util.updater.Deployer;
import org.rhq.core.util.updater.DeploymentData;
import org.rhq.core.util.updater.DeploymentProperties;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * An Ant task for deploying a bundle or previewing the deployment.
 *
 * @author Ian Springer
 */
public class DeploymentType extends AbstractBundleType {
    private String name;
    private Map<File, File> files = new LinkedHashMap<File, File>();
    private Set<File> archives = new LinkedHashSet<File>();
    private SystemServiceType systemService;
    private Pattern ignorePattern;
    private Pattern replacePattern;
    private boolean preview;
    private String preinstallTarget;
    private String postinstallTarget;

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
            throw new BuildException("You must specify at least one file to deploy via nested rhq:file and/or rhq:archive elements.");
        }
        if (!this.files.isEmpty()) {
            log("Deploying files " + this.files + "...", Project.MSG_VERBOSE);
        }
        if (!this.archives.isEmpty()) {
            log("Deploying archives " + this.archives + "...", Project.MSG_VERBOSE);
        }

        // for now, apply the pattern to all files in the deployment
        Map<File, Pattern> archiveReplacePatterns = new HashMap<File, Pattern>();
        for (File file : this.archives) {
            archiveReplacePatterns.put(file, this.replacePattern);
        }
        Set<File> rawFilesToReplace = this.files.keySet(); // TODO: CHANGE ME! only replace those raw files marked as "replace=true"
        DeploymentData dd = new DeploymentData(deploymentProps, this.archives, this.files, deployDir,
            archiveReplacePatterns, rawFilesToReplace, templateEngine, this.ignorePattern);
        Deployer deployer = new Deployer(dd);
        try {
            DeployDifferences diffs = getProject().getDeployDifferences();
            boolean dryRun = getProject().isDryRun();
            if (revert) {
                deployer.redeployAndRestoreBackupFiles(diffs, clean, dryRun);
            } else {
                deployer.deploy(diffs, clean, dryRun);
            }
            deployer.deploy(diffs);
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
                throw new BuildException("Specified postinstall target (" + this.postinstallTarget + ") does not exist.");
            }
            target.performTasks();
        }

        return;
    }

    public void start() throws BuildException {

    }

    public void stop() throws BuildException {

    }

    public void upgrade() throws BuildException {

    }

    public void uninstall() throws BuildException {
        // TODO
    }
        
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<File, File> getFiles() {
        return files;
    }

    public Set<File> getArchives() {
        return archives;
    }

    public boolean isPreview() {
        return this.preview;
    }

    public void setPreview(boolean preview) {
        this.preview = preview;
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
            throw new IllegalStateException("A deployment can only have one system-service child element.");
        }
        this.systemService = systemService;
    }

    public void addConfigured(FileType file) {
        File destFile = file.getDestinationFile();
        if (destFile == null) {
            File destDir = file.getDestinationDir();
            destFile = new File(destDir, file.getSource().getName());
        }
        this.files.put(file.getSource(), destFile);
    }

    public void addConfigured(ArchiveType archive) {
        this.archives.add(archive.getSource());
    }

    public void addConfigured(IgnoreType ignore) {
        List<FileSet> fileSets = ignore.getFileSets();
        this.ignorePattern = getPattern(fileSets);
    }

    public void addConfigured(ReplaceType replace) {
        List<FileSet> fileSets = replace.getFileSets();
        this.replacePattern = getPattern(fileSets);
    }

    private TemplateEngine createTemplateEngine() {
        TemplateEngine templateEngine = SystemInfoFactory.fetchTemplateEngine();
        // Add the deployment props to the template engine's tokens.
        Configuration config = getProject().getConfiguration();
        for (PropertySimple prop : config.getSimpleProperties().values()) {
            templateEngine.getTokens().put(prop.getName(), prop.getStringValue());
        }
        return templateEngine;
    }

    private static Pattern getPattern(List<FileSet> fileSets) {
        boolean first = true;
        StringBuilder regex = new StringBuilder();
        for (FileSet fileSet : fileSets) {
            if (!first) {
                regex.append("|");
            } else {
                first = false;
            }
            regex.append("(");
            File dir = fileSet.getDir();
            if (dir != null) {
                regex.append(dir);
                regex.append('/');
            }
            if (fileSet.getIncludePatterns().length == 0) {
                regex.append(".*");
            } else {
                boolean firstIncludePattern = true;
                for (String includePattern : fileSet.getIncludePatterns()) {
                    if (!firstIncludePattern) {
                        regex.append("|");
                    } else {
                        firstIncludePattern = false;
                    }
                    regex.append("(");
                    for (int i = 0; i < includePattern.length(); i++) {
                        char c = includePattern.charAt(i);
                        if (c == '?') {
                            regex.append('.');
                        } else if (c == '*') {
                            if (i + 1 < includePattern.length()) {
                                char c2 = includePattern.charAt(++i);
                                if (c2 == '*') {
                                    regex.append(".*");
                                    i++;
                                    continue;
                                }
                            }
                            regex.append("[^/]*");
                        } else {
                            regex.append(c);
                        }
                        // TODO: Escape backslashes.
                    }
                    regex.append(")");
                }
            }
            regex.append(")");
        }
        return Pattern.compile(regex.toString());
    }
}