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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.rhq.bundle.filetemplate.recipe.RecipeContext;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.SystemInfo;

/**
 * A recipe context that processes the recipe as it adds stuff to the context
 * 
 * @author John Mazzitelli
 */
public class ProcessingRecipeContext extends RecipeContext {

    private final Map<PackageVersion, File> packageVersionFiles;
    private final SystemInfo systemInfo;
    private String baseWorkingDirectory;

    public ProcessingRecipeContext(String recipe, Map<PackageVersion, File> packageVersionFiles, SystemInfo systemInfo,
        String baseWorkingDirectory) {

        super(recipe);
        this.packageVersionFiles = packageVersionFiles;
        this.systemInfo = systemInfo;
        this.baseWorkingDirectory = baseWorkingDirectory; // the directory that existing bundle filenames will be relative to
    }

    @Override
    public void addDeployFile(String filename, String directory) {
        super.addDeployFile(filename, directory);

        File existingFile = new File(this.baseWorkingDirectory, filename);

        ProcessExecution pe = getUnzipExecution(filename, directory);
        if (pe != null) {
            ProcessExecutionResults results = this.systemInfo.executeProcess(pe);
            if (results.getError() != null) {
                throw new RuntimeException("Could not unbundle file [" + pe + "]: " + results, results.getError());
            } else if (results.getExitCode() == null || results.getExitCode().intValue() > 0) {
                throw new RuntimeException("Failed to unbundle file [" + pe + "]: " + results);
            }
            existingFile.delete();
        } else {
            // not a zipped format - just move the file to the directory as-is
            File newFile = new File(directory, filename);
            if (!existingFile.renameTo(newFile)) {
                throw new RuntimeException("Failed to move [" + existingFile + "] to [" + newFile + "]");
            }
        }
    }

    @Override
    public void addFile(String filename, String directory) {
        super.addDeployFile(filename, directory);

        File existingFile = new File(this.baseWorkingDirectory, filename);
        File newFile = new File(directory, filename);
        if (!existingFile.renameTo(newFile)) {
            throw new RuntimeException("Failed to move file [" + existingFile + "] to [" + newFile + "]");
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

        ProcessExecution pe = new ProcessExecution(scriptFile.getAbsolutePath());
        pe.setArguments(exeArgs);
        pe.setWaitForCompletion(30 * 60 * 1000L);
        pe.setWorkingDirectory(scriptFile.getParent());

        ProcessExecutionResults results = this.systemInfo.executeProcess(pe);
        if (results.getError() != null) {
            throw new RuntimeException("Could not execute script [" + pe + "]: " + results, results.getError());
        }
    }

    private ProcessExecution getUnzipExecution(String filename, String directory) {
        String exe;
        List<String> args = null;

        if (filename.endsWith(".tar")) {
            exe = "tar";
            args = new ArrayList<String>();
            args.add("xf");
            args.add(filename);
            args.add("-C");
            args.add(directory);
        } else if (filename.endsWith(".tar.bz2") || filename.endsWith(".tbz2") || filename.endsWith(".tbz")) {
            exe = "tar";
            args = new ArrayList<String>();
            args.add("xfj");
            args.add(filename);
            args.add("-C");
            args.add(directory);
        } else if (filename.endsWith(".tar.gz") || filename.endsWith(".tgz")) {
            exe = "tar";
            args = new ArrayList<String>();
            args.add("xfz");
            args.add(filename);
            args.add("-C");
            args.add(directory);
        } else if (filename.endsWith(".zip")) {
            exe = "unzip";
            args = new ArrayList<String>();
            args.add(filename);
            args.add("-d");
            args.add(directory);
        } else if (filename.endsWith(".rpm")) {
            exe = "rpm";
            args = new ArrayList<String>();
            args.add("-i");
            args.add(filename);
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
        pe.setWorkingDirectory(this.baseWorkingDirectory);
        return pe;
    }
}
