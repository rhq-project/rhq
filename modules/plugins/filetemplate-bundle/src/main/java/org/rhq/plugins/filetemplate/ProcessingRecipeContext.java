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

import org.rhq.bundle.filetemplate.recipe.RecipeContext;
import org.rhq.bundle.filetemplate.recipe.RecipeParser;
import org.rhq.core.domain.content.PackageVersion;
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

        ProcessExecution pe = getUnzipExecution(existingFile, directory);
        if (pe != null) {
            ProcessExecutionResults results = this.systemInfo.executeProcess(pe);
            if (results.getError() != null) {
                throw new RuntimeException("Could not unbundle file [" + pe + "]: " + results, results.getError());
            } else if (results.getExitCode() == null || results.getExitCode().intValue() > 0) {
                throw new RuntimeException("Failed to unbundle file [" + pe + "]: " + results);
            }
            // existingFile.delete(); WOULD WE WANT TO REMOVE THE COMPRESSED FILE?
        } else {
            // not a zipped format - just move the file to the directory as-is
            File newFile = new File(directory, filename);
            if (!existingFile.renameTo(newFile)) {
                throw new RuntimeException("Failed to move [" + existingFile + "] to [" + newFile + "]");
            }
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
        } catch (Exception e) {
            throw new RuntimeException("Failed to copy file [" + sourceFile + "] to [" + destinationFile + "]", e);
        }

        return;
    }

    @Override
    public void addRealizedFile(String file) {
        super.addRealizedFile(file);

        File trueFile = new File(this.baseWorkingDirectory, file);
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

            trueFile.delete(); // remove the one with the replacement variables in it
            if (!realizedTmpFile.renameTo(trueFile)) {
                throw new RuntimeException("Failed to rename realized tmp file [" + realizedTmpFile + "]");
            }
        } catch (Exception e) {
            throw new RuntimeException("Cannot realize file [" + file + "]", e);
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

        ProcessExecution pe = new ProcessExecution(scriptFile.getAbsolutePath());
        pe.setArguments(exeArgs);
        pe.setWaitForCompletion(30 * 60 * 1000L);
        pe.setWorkingDirectory(scriptFile.getParent());

        ProcessExecutionResults results = this.systemInfo.executeProcess(pe);
        if (results.getError() != null) {
            throw new RuntimeException("Could not execute script [" + pe + "]: " + results, results.getError());
        }
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
