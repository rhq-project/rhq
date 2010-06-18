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
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.bundle.ant.type;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Chmod;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.optional.unix.Symlink;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * An Ant task that installs a system startup/shutdown service. Currently only Red Hat Linux versions are supported.
 *
 * @author Ian Springer
 */
public class SystemServiceType extends AbstractBundleType {
    private static final String OS_NAME = System.getProperty("os.name");
    private static final File REDHAT_RELEASE_FILE = new File("/etc/redhat-release");
    private static final Set<Character> REDHAT_RUN_LEVELS = new HashSet<Character>();
    static {
        for (char c = '0'; c <= '6'; c++) {
            REDHAT_RUN_LEVELS.add(c);
        }
        // TODO: Add 's' and/or 'S' depending on the flavor of UNIX.
    }
    private static final File INIT_DIR = new File("/etc/init.d");
    private static final File SYSCONFIG_DIR = new File("/etc/sysconfig");
    private static final File DEFAULT_ROOT = new File("/");

    private String name;
    private File scriptFile;
    private File configFile;
    private boolean overwriteScript;
    private boolean overwriteConfig;
    private boolean overwriteLinks = true;
    private File root = DEFAULT_ROOT;
    private String startLevels;

    /**
     * An integer from 0-99 indicating the service's start order - services with a lower priority number are started
     * before services with a higher priority number.
     */
    private Byte startPriority;
    
    /**
     * An integer from 0-99 indicating the service's stop order - services with a lower priority number are stopped
     * before services with a higher priority number.
     */
    private Byte stopPriority;

    private Set<Character> startLevelChars;
    private Set<Character> stopLevelChars;

    private File scriptDestFile;
    private File configDestFile;

    public void validate() throws BuildException {
        validateAttributes();
        
        this.scriptDestFile = new File(getInitDir(), this.name);
        this.configDestFile = new File(getSysConfigDir(), this.name);
    }

    public void init() throws BuildException {
        if (!OS_NAME.equals("Linux") || !REDHAT_RELEASE_FILE.exists() ) {
            throw new BuildException("The system-service element is only supported on Red Hat Linux systems.");
        }

        if (!this.scriptFile.exists() || this.scriptFile.isDirectory()) {
            throw new BuildException("The 'scriptFile' attribute must be set to the path of an existing regular file.");
        }
        if (this.configFile != null && !this.configFile.exists() || this.configFile.isDirectory()) {
            throw new BuildException("The 'configFile' attribute must be set to the path of an existing regular file.");
        }
    }

    public void install() throws BuildException {
        // Install the config file if one was provided (e.g. /etc/sysconfig/named).
        if (this.configFile != null) {
            File sysconfigDir = getSysConfigDir();
            if (!sysconfigDir.exists()) {
                sysconfigDir.mkdirs();
            }
            if (!sysconfigDir.canWrite()) {
                throw new BuildException(sysconfigDir + " directory is not writeable.");
            }
            // Don't copy the file ourselves - let our parent DeploymentUnitType handle it, so the deployment metadata
            // (i.e. MD5) can be calculated and saved.
            //copyFile(this.configFile, this.configDestFile, this.overwriteConfig);
            setPermissions(this.configDestFile, "644");
        }

        // Install the script itself (e.g. /etc/init.d/named).
        File initDir = getInitDir();
        if (!initDir.exists()) {
           initDir.mkdirs();
        }
        if (!initDir.canWrite()) {
            throw new BuildException(initDir + " directory is not writeable.");
        }
        getProject().log("Installing service script " + this.scriptDestFile + "...");
        // Don't copy the file ourselves - let our parent DeploymentUnitType handle it, so the deployment metadata
        // (i.e. MD5) can be calculated and saved.
        //copyFile(this.scriptFile, scriptDestFile, this.overwriteScript);
        setPermissions(this.scriptDestFile, "755");

        // Create the symlinks in the rcX.d dirs (e.g. /etc/rc3.d/S24named -> ../init.d/named)
        createScriptSymlinks(this.scriptDestFile, this.startPriority, this.startLevelChars, 'S');
        createScriptSymlinks(this.scriptDestFile, this.stopPriority, this.stopLevelChars, 'K');
    }

    private File getInitDir() {
        return new File(this.root, INIT_DIR.getPath().substring(1));
    }

    public File getScriptDestFile() {
        return this.scriptDestFile;
    }

    private File getSysConfigDir() {
        return new File(this.root, SYSCONFIG_DIR.getPath().substring(1));
    }

    public File getConfigDestFile() {
        return this.configDestFile;
    }

    public void start() throws BuildException {
        File scriptFile = getScriptDestFile();
        String[] commandLine = {scriptFile.getAbsolutePath(), "start"};
        try {
            executeCommand(commandLine);
        } catch (IOException e) {
            throw new BuildException("Failed to start " + this.name + " system service via command [" + Arrays.toString(commandLine)
                    + "].", e);
        }
    }

    public void stop() throws BuildException {
        File scriptFile = getScriptDestFile();
        String[] commandLine = {scriptFile.getAbsolutePath(), "stop"};
        try {
            executeCommand(commandLine);
        } catch (IOException e) {
            throw new BuildException("Failed to stop " + this.name + " system service via command [" + Arrays.toString(commandLine)
                    + "].", e);
        }
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

    public File getScriptFile() {
        return scriptFile;
    }

    public void setScriptFile(String scriptFile) {
        File file = new File(scriptFile);
        if (file.isAbsolute()) {
            throw new BuildException("Path specified by 'scriptFile' attribute (" + scriptFile
                + ") is not relative - it must be a relative path, relative to the Ant basedir.");
        }
        this.scriptFile = getProject().resolveFile(scriptFile);
    }

    public File getConfigFile() {
        return configFile;
    }

    public void setConfigFile(String configFile) {
        File file = new File(configFile);
        if (file.isAbsolute()) {
            throw new BuildException("Path specified by 'configFile' attribute (" + configFile
                + ") is not relative - it must be a relative path, relative to the Ant basedir.");
        }
        this.configFile = getProject().resolveFile(configFile);
    }

    public boolean isOverwriteScript() {
        return overwriteScript;
    }

    public void setOverwriteScript(boolean overwriteScript) {
        this.overwriteScript = overwriteScript;
    }

    public boolean isOverwriteConfig() {
        return overwriteConfig;
    }

    public void setOverwriteConfig(boolean overwriteConfig) {
        this.overwriteConfig = overwriteConfig;
    }

    public boolean isOverwriteLinks() {
        return overwriteLinks;
    }

    public void setOverwriteLinks(boolean overwriteLinks) {
        this.overwriteLinks = overwriteLinks;
    }

    public String getStartLevels() {
        return startLevels;
    }

    public void setStartLevels(String startLevels) {
        this.startLevels = startLevels;
    }

    public byte getStartPriority() {
        return startPriority;
    }

    public void setStartPriority(byte startPriority) {
        this.startPriority = startPriority;
    }

    public byte getStopPriority() {
        return stopPriority;
    }

    public void setStopPriority(byte stopPriority) {
        this.stopPriority = stopPriority;
    }

    public File getRoot() {
        return root;
    }

    public void setRoot(File root) {
        this.root = root;
    }

    /**
     * Ensure we have a consistent and legal set of attributes, and set
     * any internal flags necessary based on different combinations
     * of attributes.
     *
     * @throws BuildException if an error occurs
     */
    protected void validateAttributes() throws BuildException {
        if (this.name == null) {
            throw new BuildException("The 'name' attribute is required.");
        }
        if (this.name.length() == 0) {
            throw new BuildException("The 'name' attribute must have a non-empty value.");
        }

        if (this.scriptFile == null) {
            throw new BuildException("The 'scriptFile' attribute is required.");
        }

        if (this.startLevels == null) {
            throw new BuildException("The 'startLevels' attribute is required.");
        }
        if (this.startLevels.length() == 0) {
            throw new BuildException("The 'startLevels' attribute must have a non-empty value.");
        }
        this.startLevelChars = parseLevels(this.startLevels);
        this.stopLevelChars = new TreeSet<Character>(); 
        for (char level : REDHAT_RUN_LEVELS) {
            if (!this.startLevelChars.contains(level)) {
                this.stopLevelChars.add(level);
            }
        }

        if (this.startPriority == null) {
            throw new BuildException("The 'startPriority' attribute is required.");
        }
        if (this.startPriority < 0 || this.startPriority > 99) {
            throw new BuildException("The 'startPriority' attribute must be >=0 and <= 99.");
        }
        if (this.stopPriority == null) {
            throw new BuildException("The 'stopPriority' attribute is required.");
        }
        if (this.stopPriority < 0 || this.stopPriority > 99) {
            throw new BuildException("The 'startPriority' attribute must be >=0 and <= 99.");
        }

        if (!this.root.exists()) {
            this.root.mkdirs();
            if (!this.root.exists()) {
                throw new BuildException("Failed to create root directory " + this.root
                        + " as specified by 'root' attribute.");
            }
        }
        if (!this.root.isDirectory()) {
            throw new BuildException("The 'root' attribute must be set to the path of a directory.");
        }
        if (!this.root.equals(DEFAULT_ROOT)) {
            getProject().log("Using root " + this.root + ".");
        }
    }

    private static Set<Character> parseLevels(String levels) {
        Set<Character> levelChars = new TreeSet<Character>();
        String[] tokens = levels.split("[ ]*,[ ]*");
        for (String token : tokens) {
            if (!token.equals("")) {
                Character level;
                try {
                    if (token.length() != 1) {
                        throw new Exception();
                    }
                    level = token.charAt(0);
                    if (!REDHAT_RUN_LEVELS.contains(level)) {
                        throw new Exception();
                    }

                } catch (Exception e) {
                    throw new BuildException("Invalid run level: " + token
                            + " - the 'startLevels' attribute must be a comma-separated list of run levels - the valid levels are "
                            + REDHAT_RUN_LEVELS + ".");
                }
                if (levelChars.contains(level)) {
                    throw new BuildException("The 'startLevels' attribute defines run level " + level + " more than once.");
                }
                levelChars.add(level);
            }
        }
        return levelChars;
    }

    private void createScriptSymlinks(File scriptFile, byte priority, Set<Character> levels, char fileNamePrefix) {
        String priorityString = String.format("%02d", priority);
        for (char level : levels) {
            File rcDir = new File(this.root, "etc/rc" + level + ".d");
            if (!rcDir.exists()) {
                rcDir.mkdirs();
            }
            if (!rcDir.exists()) {
                throw new BuildException(rcDir + " does not exist.");
            }
            if (!rcDir.isDirectory()) {
                throw new BuildException(rcDir + " exists but is not a directory.");
            }
            if (!rcDir.isDirectory()) {
                throw new BuildException(rcDir + " directory is not writeable.");
            }
            File link = new File(rcDir, fileNamePrefix + priorityString + this.name);
            getProject().log("Creating symbolic link " + link + " referencing " + scriptFile + "...");

            createSymlink(scriptFile, link, this.overwriteLinks);
        }
    }

    private void copyFile(File sourceFile, File destFile, boolean overwrite) {
        Copy copyTask = new Copy();
        copyTask.setProject(getProject());
        copyTask.init();
        copyTask.setFile(sourceFile);
        copyTask.setTofile(destFile);
        copyTask.setOverwrite(overwrite);
        copyTask.execute();
    }

    private void createSymlink(File targetFile, File linkFile, boolean overwrite) {
        Symlink symlinkTask = new Symlink();
        symlinkTask.setProject(getProject());
        symlinkTask.init();
        symlinkTask.setResource(targetFile.getAbsolutePath());
        symlinkTask.setLink(linkFile.getAbsolutePath());
        symlinkTask.setOverwrite(overwrite);
        symlinkTask.execute();
    }

    private int executeCommand(String[] commandLine) throws IOException {
        Execute executeTask = new Execute();
        executeTask.setCommandline(commandLine);        
        return executeTask.execute();
    }

    private void setPermissions(File file, String perms) {
        Chmod chmodTask = new Chmod();
        chmodTask.setProject(getProject());
        chmodTask.init();
        chmodTask.setFile(file);
        chmodTask.setPerm(perms);
        chmodTask.execute();
    }
}
