/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2013 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.server.control;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.util.stream.StreamUtil;

/**
 * @author John Sanda
 */
public abstract class ControlCommand {

    public static final String SERVER_OPTION = "server";
    public static final String STORAGE_OPTION = "storage";
    public static final String AGENT_OPTION = "agent";
    public static final String HELP_OPTION_1 = "--help";
    public static final String HELP_OPTION_2 = "-h";
    public static final String RHQ_AGENT_BASEDIR_PROP = "rhq.agent.basedir";

    protected static final String STORAGE_BASEDIR_NAME = "rhq-storage";
    protected static final String AGENT_BASEDIR_NAME = "rhq-agent";

    private final File defaultStorageBasedir;
    private final File defaultAgentBasedir;

    protected final Log log = LogFactory.getLog(getClass().getName());

    private File basedir;
    private File binDir; // where the internal startup scripts are

    private PropertiesConfiguration rhqctlConfig = null; // the (optional) settings found in rhqctl.properties

    private ArrayList<Runnable> undoTasks = new ArrayList<Runnable>();

    protected void undo() {
        // perform the undo on the snapshot of undoTasks, because of possible ConcurrentModificationException
        List<Runnable> undoTasksCopy = new ArrayList<Runnable>(undoTasks);
        Collections.reverse(undoTasksCopy); // do the undo tasks in the reverse order in which they were added to the list
        for (Runnable undoTask : undoTasksCopy) {
            try {
                undoTask.run();
            } catch (Throwable t) {
                log.error("Failed to invoke undo task [" + undoTask
                    + "], will keep going but system may be in an indeterminate state");
            }
        }
        return;
    }

    protected void addUndoTask(UndoTask r) {
        undoTasks.add(r);
    }

    protected abstract class UndoTask implements Runnable {
        private final String message;

        public UndoTask() {
            this(null);
        }

        public UndoTask(String msg) {
            this.message = msg;
        }

        @Override
        public void run() {
            if (this.message != null) {
                log.warn("UNDO: " + this.message);
            }

            try {
                performUndoWork();
            } catch (Exception e) {
                throw new RuntimeException("Undo task failed: " + ((this.message == null) ? "" : this.message), e);
            }
        }

        protected abstract void performUndoWork() throws Exception;
    }

    public ControlCommand() {
        basedir = new File(System.getProperty("rhq.server.basedir"));
        binDir = new File(basedir, "bin/internal");

        File rhqctlPropertiesFile = getRhqCtlProperties();
        if (rhqctlPropertiesFile != null) {
            try {
                rhqctlConfig = new PropertiesConfiguration(rhqctlPropertiesFile);
            } catch (ConfigurationException e) {
                throw new RHQControlException("Failed to load configuration", e);
            }
        }

        defaultStorageBasedir = new File(getBaseDir(), STORAGE_BASEDIR_NAME);
        defaultAgentBasedir = new File(getBaseDir().getParent(), AGENT_BASEDIR_NAME);
    }

    public abstract String getName();

    public abstract String getDescription();

    public abstract Options getOptions();

    protected abstract int exec(CommandLine commandLine);

    public int exec(String[] args) {
        Options options = getOptions();
        int rValue = RHQControl.EXIT_CODE_OK;
        try {
            CommandLineParser parser = new RHQPosixParser(false);
            CommandLine cmdLine = parser.parse(options, args, true);
            if (!cmdLine.getArgList().isEmpty()) {
                // there were some unrecognized args
                System.out.println("Unrecognized arguments: " + cmdLine.getArgList());
                printUsage();
                return RHQControl.EXIT_CODE_INVALID_ARGUMENT;
            }
            rValue = exec(cmdLine);

            if (rhqctlConfig != null) {
                rhqctlConfig.save();
            }
        } catch (ParseException e) {
            printUsage();
            rValue = RHQControl.EXIT_CODE_INVALID_ARGUMENT;
        } catch (ConfigurationException e) {
            throw new RHQControlException("Failed to update " + getRhqCtlProperties(), e);
        }
        return rValue;
    }

    public void printUsage() {
        Options options = getOptions();
        HelpFormatter helpFormatter = new HelpFormatter();
        String header = "\n" + getDescription() + "\n\n";
        String syntax;

        if (options.getOptions().isEmpty()) {
            syntax = "rhqctl " + getName();
        } else {
            syntax = "rhqctl " + getName() + " [options]";
        }

        helpFormatter.setNewLine("\n");
        helpFormatter.printHelp(syntax, header, options, null);

        // we can't pass this as the last argument to printHelp above
        // because it throws an exception if the string is too long.
        String readmeContent = getReadmeContent();
        if (readmeContent != null) {
            System.out.println(readmeContent);
        }
    }

    /**
     * Subclasses can override this and return a non-null name of its readme file.
     * If non-null is returned, the content of that file will be dumped at the bottom of the
     * --help output.
     * @return readme help file name, or null if the command doesn't have one
     */
    abstract protected String getReadmeFilename();

    protected String getReadmeContent() {
        // see if the command has a readme file whose content we will print; if not, just abort
        String readmeFilename = getReadmeFilename();
        if (readmeFilename == null) {
            return null;
        }
        File readmeFile = new File(getBaseDir(), readmeFilename);
        if (!readmeFile.canRead()) {
            return null;
        }

        // dump the readme
        try {
            FileReader fileReader = new FileReader(readmeFile);
            String readmeContent = StreamUtil.slurp(fileReader);
            return readmeContent;
        } catch (Exception e) {
            // just quietly abort, don't be noisy about this, though this should never happen
            return null;
        }
    }

    protected List<Integer> toIntList(String s) {
        String[] args = s.split(",");
        List<Integer> list = new ArrayList<Integer>(args.length);
        for (String arg : args) {
            list.add(Integer.parseInt(arg));
        }

        return list;
    }

    protected File getBaseDir() {
        return this.basedir;
    }

    protected File getBinDir() {
        return this.binDir;
    }

    protected File getLogDir() {
        return new File(getBaseDir(), "logs");
    }

    protected File getStorageBasedir() {
        return defaultStorageBasedir; // the storage location is always in our expected default location, not configurable
    }

    protected File getAgentBasedir() {
        File retval;
        String agentBasedirProperty = getProperty(RHQ_AGENT_BASEDIR_PROP, null);
        if (agentBasedirProperty == null) {
            retval = defaultAgentBasedir; // we want to return the exact defaultAgentBasedir object here to ensure equals() works later
        } else {
            retval = new File(agentBasedirProperty);
        }
        return retval;
    }

    protected void setAgentBasedir(File agentBasedir) {
        // We only want to create this in rhqctl.properties if it is different than the default location.
        // This allows us to avoid creating an empty/unused rhqctl.properties - we only want that file
        // if we actually need it to override our defaults.
        if (agentBasedir != null && !agentBasedir.equals(defaultAgentBasedir)) {
            putProperty(RHQ_AGENT_BASEDIR_PROP, agentBasedir.getAbsolutePath());
        }
    }

    protected boolean isServerInstalled() {
        return isServerInstalled(getBaseDir());
    }

    protected File getServerInstalledMarkerFile(File baseDir) {
        return new File(baseDir, "jbossas/standalone/data/rhq.installed");
    }

    protected File getServerPropertiesFile() {
        return new File(getBaseDir(), "bin/rhq-server.properties");
    }

    protected boolean isServerInstalled(File baseDir) {
        File markerFile = getServerInstalledMarkerFile(baseDir);
        return markerFile.exists();
    }

    protected boolean isAgentInstalled() {
        return getAgentBasedir().exists();
    }

    protected boolean isStorageInstalled() {
        return getStorageBasedir().exists();
    }

    protected File getStoragePidFile() {
        File storageBasedir = getStorageBasedir();
        File storageBinDir = new File(storageBasedir, "bin");
        File pidFile = new File(storageBinDir, "cassandra.pid");
        return pidFile;
    }

    protected String getStoragePid() throws IOException {
        File pidFile = getStoragePidFile();

        if (pidFile.exists()) {
            return StreamUtil.slurp(new FileReader(pidFile));
        }
        return null;
    }

    protected String getServerPid() throws IOException {
        File pidFile = new File(binDir, "rhq-server.pid");
        if (pidFile.exists()) {
            return StreamUtil.slurp(new FileReader(pidFile));
        }
        return null;
    }

    protected String getAgentPid() throws IOException {
        File agentBinDir = new File(getAgentBasedir(), "bin");
        File pidFile = new File(agentBinDir, "rhq-agent.pid");

        if (pidFile.exists()) {
            return StreamUtil.slurp(new FileReader(pidFile));
        }
        return null;
    }

    /**
     * Returns a property from the rhqctl.properties file. If that optional file doesn't
     * exist, the default value is returned. If the file exists but the key isn't found
     * in there, the default value is returned.
     * @param key the property name to find in rhqctl.properties
     * @param defaultValue the value to return if the property doesn't exist
     * @return the value of the property or its default value if property does not exist
     */
    private String getProperty(String key, String defaultValue) {
        return (rhqctlConfig != null) ? rhqctlConfig.getString(key, defaultValue) : defaultValue;
    }

    /**
     * Sets a property to rhqctl.properties. Note that calling this will eventually cause
     * rhqctl.properties file to be created later.
     * @param key
     * @param value
     */
    private void putProperty(String key, String value) {
        if (rhqctlConfig == null) {
            rhqctlConfig = new PropertiesConfiguration();
            rhqctlConfig.setPath(getRhqCtlPropertiesPath());
        }

        rhqctlConfig.setProperty(key, value);
    }

    /**
     * Returns the file of the rhqctl.properties file if it exists. Since this is
     * an optional file, this can return null to indicate it doesn't yet exist.
     *
     * @return rhqctl.properties file if it exists; null if it doesn't exist
     */
    protected File getRhqCtlProperties() {
        String filename = getRhqCtlPropertiesPath();
        File file = new File(filename);
        if (!file.isFile()) {
            file = null;
        }
        return file;
    }

    /**
     * Returns the path to the optional rhqctl.properties file. This will always return non-null,
     * even if the file actually doesn't exist yet.
     * @return the path to where the rhqctl.properties file is or could be if one existed.
     */
    protected String getRhqCtlPropertiesPath() {
        String sysprop = System.getProperty("rhqctl.properties-file");
        if (sysprop == null) {
            throw new RuntimeException("The required system property [rhqctl.properties-file] is not defined.");
        }
        return sysprop;
    }

    protected org.apache.commons.exec.CommandLine getCommandLine(String scriptName, String... args) {
        return getCommandLine(true, scriptName, args);
    }

    protected org.apache.commons.exec.CommandLine getCommandLine(boolean addShExt, String scriptName, String... args) {
        org.apache.commons.exec.CommandLine result;

        if (isWindows()) {
            result = new org.apache.commons.exec.CommandLine("cmd.exe");
            result.addArgument("/C");
            result.addArgument(scriptName.replace('/', '\\') + ".bat");
        } else {
            result = new org.apache.commons.exec.CommandLine("./" + (addShExt ? scriptName + ".sh" : scriptName));
        }

        for (String arg : args) {
            result.addArgument(arg);
        }

        return result;
    }

    protected String getScript(String scriptName) {
        if (isWindows()) {
            return scriptName.replace('/', '\\') + ".bat";
        }

        return "./" + scriptName + ".sh";
    }

    protected boolean isWindows() {
        String operatingSystem = System.getProperty("os.name").toLowerCase(Locale.US);
        return operatingSystem.contains("windows");
    }

    protected boolean isPortInUse(String host, int port) {
        boolean inUse;

        try {
            Socket testSocket = new Socket(host, port);
            try {
                testSocket.close();
            } catch (Exception ignore) {
            }
            inUse = true;
        } catch (Exception expected) {
            inUse = false;
        }

        return inUse;
    }

    protected void waitForProcessToStop(String pid) throws Exception {

        if (isWindows() || pid==null) {
            // For the moment we have no better way to just wait some time
            Thread.sleep(10*1000L);
        } else {
            int tries = 5;
            while (tries > 0) {
                log.debug(".");
                if (!isUnixPidRunning(pid)) {
                    break;
                }
                Thread.sleep(2*1000L);
                tries--;
            }
            if (tries==0) {
                throw new RHQControlException("Process [" + pid + "] did not finish yet. Terminate it manually and retry.");
            }
        }

    }

    protected void killPid(String pid) throws IOException {
        Executor executor = new DefaultExecutor();
        executor.setWorkingDirectory(getBinDir());
        PumpStreamHandler streamHandler = new PumpStreamHandler(createNullOutputStream(), createNullOutputStream());
        executor.setStreamHandler(streamHandler);
        org.apache.commons.exec.CommandLine commandLine;

        commandLine = new org.apache.commons.exec.CommandLine("kill").addArgument(pid);
        executor.execute(commandLine);
    }

    protected boolean isUnixPidRunning(String pid) {

        Executor executor = new DefaultExecutor();
        executor.setWorkingDirectory(getBinDir());
        PumpStreamHandler streamHandler = new PumpStreamHandler(createNullOutputStream(), createNullOutputStream());
        executor.setStreamHandler(streamHandler);
        org.apache.commons.exec.CommandLine commandLine;
        commandLine = new org.apache.commons.exec.CommandLine("kill").addArgument("-0").addArgument(pid);

        try {
            int code = executor.execute(commandLine);
            if (code != 0) {
                return false;
            }
        } catch (ExecuteException ee ) {
            if (ee.getExitValue() == 1) {
                // return code 1 means process does not exist
                return false;
            }
        } catch (IOException e) {
            log.error("Checking for running process failed: " + e.getMessage());
        }
        return true;
    }

    protected boolean isStorageRunning() throws IOException {
        String pid = getStoragePid();
        if (pid == null) {
            return false;
        } else if (pid != null && !isUnixPidRunning(pid)) {
            // There is a phantom pidfile
            File pidFile = getStoragePidFile();
            if (!pidFile.delete()) {
                throw new RHQControlException("Could not delete storage pidfile " + pidFile.getAbsolutePath());
            }
            return false;
        } else {
            return true;
        }
    }

    /**
     * Call this method to get an output stream that throws away all data. Useful
     * when executing commands which you don't care about seeing its output on stdout.
     * Example usage:
     *    executor.setStreamHandler(new PumpStreamHandler(createNullOutputStream(), createNullOutputStream()));
     * @return a null output stream
     */
    protected OutputStream createNullOutputStream() {
        return new NullOutputStream();
    }

    private class NullOutputStream extends OutputStream {
        @Override
        public void write(byte[] b, int off, int len) {
        }

        @Override
        public void write(int b) {
        }

        @Override
        public void write(byte[] b) throws IOException {
        }
    }
}
