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
package org.rhq.bundle.ant.task;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;

import org.rhq.bundle.ant.AntLauncher;
import org.rhq.bundle.ant.type.ArchiveType;
import org.rhq.bundle.ant.type.FileSet;
import org.rhq.bundle.ant.type.FileType;
import org.rhq.bundle.ant.type.IgnoreType;
import org.rhq.bundle.ant.type.ReplaceType;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.template.TemplateEngine;
import org.rhq.core.util.updater.DeployDifferences;
import org.rhq.core.util.updater.Deployer;
import org.rhq.core.util.updater.DeploymentProperties;

/**
 * An Ant task for deploying a bundle or previewing the deployment.
 *
 * @author Ian Springer
 */
public class DeployTask extends AbstractBundleTask {
    private Map<File, File> files = new LinkedHashMap<File, File>();
    private Set<File> archives = new LinkedHashSet<File>();
    private Pattern ignorePattern;
    private Pattern replacePattern;
    private boolean preview;

    @Override
    public void maybeConfigure() throws BuildException {
        // The below call will init the attribute fields.
        super.maybeConfigure();
    }

    @Override
    public void execute() throws BuildException {
        int deploymentId = getDeploymentId();
        DeploymentProperties deploymentProps = new DeploymentProperties(deploymentId, getProject().getBundleName(),
            getProject().getBundleVersion(), getProject().getBundleDescription());
        File deployDir = getProject().getDeployDir();
        TemplateEngine templateEngine = createTemplateEngine();
        log("Deploying files " + this.files + "...");
        log("Deploying archives " + this.archives + "...");
        Deployer deployer = new Deployer(deploymentProps, this.archives, this.files, deployDir, this.replacePattern,
            templateEngine, this.ignorePattern);
        try {
            DeployDifferences diff = null;
            deployer.deploy(diff);
        } catch (Exception e) {
            throw new BuildException("Failed to deploy bundle '" + getProject().getBundleName() + "' version "
                + getProject().getBundleVersion() + ".", e);
        }
        return;
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

    private int getDeploymentId() {
        String deploymentIdStr = getProject().getProperty(AntLauncher.DEPLOY_ID_PROP);
        if (deploymentIdStr == null) {
            return 0;
        }
        return Integer.parseInt(deploymentIdStr);
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