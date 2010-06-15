/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.plugins.filetemplate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.bundle.filetemplate.recipe.RecipeContext;
import org.rhq.bundle.filetemplate.recipe.RecipeParser;
import org.rhq.core.domain.bundle.BundleResourceDeployment;
import org.rhq.core.domain.bundle.BundleResourceDeploymentHistory;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.pluginapi.bundle.BundleManagerProvider;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.util.file.FileUtil;

/**
 * A recipe context that processes the recipe as it adds stuff to the context
 * 
 * @author John Mazzitelli
 */
public class ProcessingRecipeContext extends RecipeContext {

    private final Log log = LogFactory.getLog(this.getClass());

    private final SystemInfo systemInfo;
    private final String baseWorkingDirectory;
    private final BundleResourceDeployment bundleResourceDeployment;
    private final BundleManagerProvider bundleManagerProvider;

    public ProcessingRecipeContext(String recipe, Map<PackageVersion, File> packageVersionFiles, SystemInfo systemInfo,
        String baseWorkingDirectory, BundleResourceDeployment bundleResourceDeployment,
        BundleManagerProvider bundleManagerProvider) {

        super(recipe);
        this.systemInfo = systemInfo;
        this.baseWorkingDirectory = baseWorkingDirectory; // the directory that existing bundle filenames will be relative to
        this.bundleResourceDeployment = bundleResourceDeployment;
        this.bundleManagerProvider = bundleManagerProvider;
    }

    @Override
    public void addDeployFile(String filename, String directory) {
        super.addDeployFile(filename, directory);

        String msg;
        File existingFile = new File(this.baseWorkingDirectory, filename);
        ProcessExecution pe = getUnzipExecution(existingFile, directory);

        if (pe != null) {
            ProcessExecutionResults results = this.systemInfo.executeProcess(pe);
            if (results.getError() != null) {
                msg = "Could not unbundle file [" + pe + "]: " + results;
                audit("deploy", BundleResourceDeploymentHistory.Status.FAILURE, msg);
                throw new RuntimeException(msg, results.getError());
            } else if (results.getExitCode() == null || results.getExitCode().intValue() > 0) {
                msg = "Failed to unbundle file [" + pe + "]: " + results;
                audit("deploy", BundleResourceDeploymentHistory.Status.FAILURE, msg);
                throw new RuntimeException(msg);
            } else {
                msg = "extracted files from [" + existingFile + "] to [" + directory + "]";
            }
            // existingFile.delete(); WOULD WE WANT TO REMOVE THE COMPRESSED FILE?
            audit("deploy", BundleResourceDeploymentHistory.Status.SUCCESS, msg);
        } else {
            // not a zipped format - just move the file to the directory as-is
            File newFile = new File(directory, filename);
            if (!existingFile.renameTo(newFile)) {
                msg = "Failed to move [" + existingFile + "] to [" + newFile + "]";
                audit("deploy", BundleResourceDeploymentHistory.Status.FAILURE, msg);
                throw new RuntimeException(msg);
            }
            audit("deploy", BundleResourceDeploymentHistory.Status.SUCCESS, "renamed [" + existingFile + "] to ["
                + newFile + "]");
        }
    }

    @Override
    public void addFile(String source, String destination) {
        super.addFile(source, destination);

        File sourceFile = new File(this.baseWorkingDirectory, source);
        File destinationFile = new File(destination);

        try {
            destinationFile.getParentFile().mkdirs();
            FileUtil.copyFile(sourceFile, destinationFile);
            audit("file", BundleResourceDeploymentHistory.Status.SUCCESS, "copied file [" + sourceFile + "] to ["
                + destinationFile + "]");
        } catch (Exception e) {
            String msg = "Failed to copy file [" + sourceFile + "] to [" + destinationFile + "]";
            audit("file", BundleResourceDeploymentHistory.Status.FAILURE, msg);
            throw new RuntimeException(msg, e);
        }

        return;
    }

    @Override
    public void addRealizedFile(String file) {
        super.addRealizedFile(file);

        String msg;
        File trueFile = new File(file);
        RecipeParser parser = getParser();
        File realizedTmpFile = null;
        FileWriter realizedTmpFileWriter = null;
        BufferedReader reader = null;

        try {

            realizedTmpFile = File.createTempFile("rhq-realize-", ".tmp", trueFile.getParentFile());
            realizedTmpFileWriter = new FileWriter(realizedTmpFile);

            reader = new BufferedReader(new FileReader(trueFile));
            String line = reader.readLine();
            while (line != null) {
                line = parser.replaceReplacementVariables(this, line);
                realizedTmpFileWriter.write(line);
                realizedTmpFileWriter.write("\n");
                line = reader.readLine();
            }

            realizedTmpFileWriter.close();
            realizedTmpFileWriter = null;
            reader.close();
            reader = null;

            audit("realize", BundleResourceDeploymentHistory.Status.SUCCESS, "realized [" + file + "]");

            trueFile.delete(); // remove the one with the replacement variables in it
            if (!realizedTmpFile.renameTo(trueFile)) {
                msg = "Failed to rename realized tmp file [" + realizedTmpFile + "] to [" + trueFile + "]";
                audit("realize", BundleResourceDeploymentHistory.Status.FAILURE, msg);
                throw new RuntimeException(msg);
            }

            audit("realize", BundleResourceDeploymentHistory.Status.SUCCESS, "renamed realized file ["
                + realizedTmpFile + "] to [" + trueFile + "]");
        } catch (Exception e) {
            msg = "Cannot realize file [" + file + "]";
            audit("realize", BundleResourceDeploymentHistory.Status.FAILURE, msg);
            throw new RuntimeException(msg, e);
        } finally {
            if (realizedTmpFileWriter != null) {
                try {
                    realizedTmpFileWriter.close();
                } catch (Exception e) {
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                }
            }
            if (realizedTmpFile != null && realizedTmpFile.exists()) {
                realizedTmpFile.delete();
            }
        }
    }

    @Override
    public void addReplacementVariables(Set<String> replacementVariables) {
        super.addReplacementVariables(replacementVariables);
    }

    @Override
    public void addScript(String exe, List<String> exeArgs) {
        super.addScript(exe, exeArgs);

        File scriptFile = new File(this.baseWorkingDirectory, exe);

        ensureExecutable(scriptFile);

        ProcessExecution pe = new ProcessExecution(scriptFile.getAbsolutePath());
        pe.setArguments(exeArgs);
        pe.setWaitForCompletion(30 * 60 * 1000L);
        pe.setWorkingDirectory(scriptFile.getParent());

        String msg;
        ProcessExecutionResults results = this.systemInfo.executeProcess(pe);
        if (results.getError() != null) {
            msg = "Could not execute script [" + pe + "]: " + results;
            audit("script", BundleResourceDeploymentHistory.Status.FAILURE, msg);
            throw new RuntimeException(msg, results.getError());
        } else {
            msg = "Executed script [" + pe + "]";
            audit("script", BundleResourceDeploymentHistory.Status.SUCCESS, msg);
        }

        return;
    }

    @Override
    public void addCommand(String exe, List<String> exeArgs) {
        super.addCommand(exe, exeArgs);

        ProcessExecution pe = new ProcessExecution(exe);
        pe.setArguments(exeArgs);
        pe.setWaitForCompletion(30 * 60 * 1000L);
        pe.setCheckExecutableExists(false);
        pe.setWorkingDirectory(this.baseWorkingDirectory);

        String msg;
        ProcessExecutionResults results = this.systemInfo.executeProcess(pe);
        if (results.getError() != null) {
            msg = "Could not execute command [" + pe + "]: " + results;
            audit("command", BundleResourceDeploymentHistory.Status.FAILURE, msg);
            throw new RuntimeException(msg, results.getError());
        } else {
            msg = "Executed command [" + pe + "]";
            audit("command", BundleResourceDeploymentHistory.Status.SUCCESS, msg);
        }

        return;
    }

    private void audit(String action, BundleResourceDeploymentHistory.Status status, String message) {
        try {
            bundleManagerProvider.auditDeployment(bundleResourceDeployment, action, "recipe", null, status, message,
                null);
            if (log.isDebugEnabled()) {
                log.debug("Deployment [" + bundleResourceDeployment.getBundleDeployment().getBundleVersion()
                    + "] audit: action=[" + action + "], status=[" + status + "], message: " + message);
            }
        } catch (Exception e) {
            log.warn("Failed to send audit message for deployment of ["
                + bundleResourceDeployment.getBundleDeployment().getBundleVersion() + "]. audit action=[" + action
                + "], status=[" + status + "], message: " + message);
        }
    }

    private void ensureExecutable(File scriptFile) {
        boolean success = scriptFile.setExecutable(true, true);
        if (!success) {
            String msg = "Cannot ensure that script [" + scriptFile + "] is executable";
            audit("ensureExecutable", BundleResourceDeploymentHistory.Status.FAILURE, msg);
            throw new RuntimeException(msg);
        }
        return;
    }

    private ProcessExecution getUnzipExecution(File file, String directory) {
        String exe;
        List<String> args = null;

        String filepath = file.getAbsolutePath();

        if (filepath.endsWith(".tar")) {
            exe = "tar";
            args = new ArrayList<String>();
            args.add("xf");
            args.add(filepath);
            args.add("-C");
            args.add(directory);
        } else if (filepath.endsWith(".tar.bz2") || filepath.endsWith(".tbz2") || filepath.endsWith(".tbz")) {
            exe = "tar";
            args = new ArrayList<String>();
            args.add("xfj");
            args.add(filepath);
            args.add("-C");
            args.add(directory);
        } else if (filepath.endsWith(".tar.gz") || filepath.endsWith(".tgz")) {
            exe = "tar";
            args = new ArrayList<String>();
            args.add("xfz");
            args.add(filepath);
            args.add("-C");
            args.add(directory);
        } else if (filepath.endsWith(".zip")) {
            exe = "unzip";
            args = new ArrayList<String>();
            args.add("-o");
            args.add(filepath);
            args.add("-d");
            args.add(directory);
        } else if (filepath.endsWith(".rpm")) {
            exe = "rpm";
            args = new ArrayList<String>();
            args.add("-i");
            args.add(filepath);
            if (directory != null && directory.length() > 0) {
                args.add("--prefix");
                args.add(directory);
            }
        } else {
            return null; // isn't a unzippable package, we'll just copy it as is
        }

        ProcessExecution pe = new ProcessExecution(exe);
        pe.setArguments(args);
        pe.setWaitForCompletion(30 * 60 * 1000L);
        pe.setCheckExecutableExists(false);
        pe.setWorkingDirectory(this.baseWorkingDirectory);
        return pe;
    }
}
