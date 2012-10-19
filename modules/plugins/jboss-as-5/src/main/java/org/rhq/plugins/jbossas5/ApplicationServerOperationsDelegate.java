/*
 * Jopr Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.jbossas5;

import java.io.File;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.operation.EmsOperation;
import org.mc4j.ems.connection.bean.parameter.EmsParameter;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.pluginapi.util.ProcessExecutionUtility;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.SystemInfo;

/**
 * Handles performing operations (Start, Shut Down, and Restart) on a JBoss AS 5.x instance.
 * 
 * @author Ian Springer
 * @author Jason Dobies
 * @author Jay Shaughnessy
 */
public class ApplicationServerOperationsDelegate {
    /**
     * max amount of time to wait for server to show as unavailable after
     * executing stop - in milliseconds
     */
    private static long STOP_WAIT_MAX = 1000L * 150; // 2.5 minutes

    /**
     * amount of time to wait between availability checks when performing a stop
     * - in milliseconds
     */
    private static final long STOP_WAIT_INTERVAL = 1000L * 5; // 5 seconds

    /**
     * amount of time to wait for stop to complete after the loop that checks
     * for DOWN availability terminates - in milliseconds
     */
    private static final long STOP_WAIT_FINAL = 1000L * 30; // 30 seconds

    /** max amount of time to wait for start to complete - in milliseconds */
    private static long START_WAIT_MAX = 1000L * 300; // 5 minutes

    /**
     * amount of time to wait between availability checks when performing a
     * start - in milliseconds
     */
    private static final long START_WAIT_INTERVAL = 1000L * 5; // 5 seconds

    private final Log log = LogFactory.getLog(ApplicationServerOperationsDelegate.class);

    private static final String SEPARATOR = "\n-----------------------\n";

    static final String DEFAULT_START_SCRIPT = "bin" + File.separator + "run."
        + ((File.separatorChar == '/') ? "sh" : "bat");
    static final String DEFAULT_SHUTDOWN_SCRIPT = "bin" + File.separator + "shutdown."
        + ((File.separatorChar == '/') ? "sh" : "bat");

    /**
     * Server component against which the operations are being performed.
     */
    private ApplicationServerComponent serverComponent;

    private File configPath;

    // Constructors --------------------------------------------

    public ApplicationServerOperationsDelegate(ApplicationServerComponent serverComponent) {
        this.serverComponent = serverComponent;
    }

    // Public --------------------------------------------

    /**
     * Performs the specified operation. The result of the operation will be
     * indicated in the return. If there is an error, an
     * <code>RuntimeException</code> will be thrown.
     * 
     * @param operation
     *            the operation to perform
     * @param parameters
     *            parameters to the operation call
     * 
     * @return if successful, the result object will contain a success message
     * 
     * @throws RuntimeException
     *             if any errors occur while trying to perform the operation
     */
    public OperationResult invoke(ApplicationServerSupportedOperations operation, Configuration parameters)
        throws InterruptedException {
        OperationResult result = null;

        switch (operation) {
        case START: {
            result = start();
            break;
        }
        case SHUTDOWN: {
            result = shutDown();
            break;
        }
        case RESTART: {
            result = restart();
            break;
        }
        }

        return result;
    }

    // Private --------------------------------------------

    /**
     * Starts the underlying AS server.
     * 
     * @return success message if no errors are encountered
     * @throws InterruptedException
     *             if the plugin container stops this operation while its
     *             executing
     */
    private OperationResult start() throws InterruptedException {
        AvailabilityType avail = this.serverComponent.getAvailability();
        if (avail == AvailabilityType.UP) {
            OperationResult result = new OperationResult();
            result.setErrorMessage("The server is already started.");
            return result;
        }
        Configuration pluginConfig = serverComponent.getResourceContext().getPluginConfiguration();
        File startScriptFile = getStartScriptPath();
        validateScriptFile(startScriptFile, ApplicationServerPluginConfigurationProperties.START_SCRIPT_CONFIG_PROP);

        // The optional command prefix (e.g. sudo or nohup).
        String prefix = pluginConfig
            .getSimple(ApplicationServerPluginConfigurationProperties.SCRIPT_PREFIX_CONFIG_PROP).getStringValue();
        String configName = getConfigurationSet();
        String bindAddress = pluginConfig.getSimpleValue(ApplicationServerPluginConfigurationProperties.BIND_ADDRESS,
            null);

        ProcessExecution processExecution;
        if (prefix == null || prefix.replaceAll("\\s", "").equals("")) {
            // Prefix is either null or contains ONLY whitespace characters.

            processExecution = ProcessExecutionUtility.createProcessExecution(startScriptFile);

            processExecution.getArguments().add("-c");
            processExecution.getArguments().add(configName);

            if (bindAddress != null) {
                processExecution.getArguments().add("-b");
                processExecution.getArguments().add(bindAddress);
            }
        } else {
            // The process execution should be tied to the process represented
            // as the prefix. If there are any other
            // tokens in the prefix, consider them arguments to the prefix
            // process.
            StringTokenizer prefixTokenizer = new StringTokenizer(prefix);
            String processName = prefixTokenizer.nextToken();
            File prefixProcess = new File(processName);

            processExecution = ProcessExecutionUtility.createProcessExecution(prefixProcess);

            while (prefixTokenizer.hasMoreTokens()) {
                String prefixArgument = prefixTokenizer.nextToken();
                processExecution.getArguments().add(prefixArgument);
            }

            // Add the AS start script and its options as a single argument to the prefix command.
            String startScriptArgument = startScriptFile.getAbsolutePath();
            startScriptArgument += " -c " + configName;
            if (bindAddress != null) {
                startScriptArgument += " -b " + bindAddress;
            }
            processExecution.getArguments().add(startScriptArgument);
        }

        initProcessExecution(processExecution, startScriptFile);

        long start = System.currentTimeMillis();
        if (log.isDebugEnabled()) {
            log.debug("About to execute the following process: [" + processExecution + "]");
        }
        SystemInfo systemInfo = serverComponent.getResourceContext().getSystemInformation();
        ProcessExecutionResults results = systemInfo.executeProcess(processExecution);
        logExecutionResults(results);

        if (results.getError() == null) {
            avail = waitForServerToStart(start);
        } else {
            log.error("Error from process execution while starting the AS instance. Exit code ["
                + results.getExitCode() + "]", results.getError());
            avail = this.serverComponent.getAvailability();
        }

        // If, after the loop, the Server is still down, consider the start to be a failure.
        OperationResult result;
        if (avail == AvailabilityType.DOWN) {
            result = new OperationResult();
            result.setErrorMessage("The server failed to start: " + results.getCapturedOutput());
        } else {
            result = new OperationResult("The server has been started.");
        }
        return result;
    }

    private String getConfigurationSet() {
        Configuration pluginConfig = serverComponent.getResourceContext().getPluginConfiguration();
        configPath = resolvePathRelativeToHomeDir(getRequiredPropertyValue(pluginConfig,
            ApplicationServerPluginConfigurationProperties.SERVER_HOME_DIR));

        if (!configPath.exists()) {
            throw new InvalidPluginConfigurationException("Configuration path '" + configPath + "' does not exist.");
        }
        return pluginConfig.getSimpleValue(ApplicationServerPluginConfigurationProperties.SERVER_NAME, configPath
            .getName());
    }

    private void initProcessExecution(ProcessExecution processExecution, File scriptFile) {
        // NOTE: For both run.bat and shutdown.bat, the current working dir must
        // be set to the script's parent dir
        // (e.g. ${JBOSS_HOME}/bin) for the script to work.
        processExecution.setWorkingDirectory(scriptFile.getParent());

        // Both scripts require the JAVA_HOME env var to be set.
        File javaHomeDir = getJavaHomePath();
        if (javaHomeDir == null) {
            throw new IllegalStateException(
                "The '"
                    + ApplicationServerPluginConfigurationProperties.JAVA_HOME
                    + "' connection property must be set in order to start the application server or to stop it via script.");
        }

        validateJavaHomePathProperty();
        processExecution.getEnvironmentVariables().put("JAVA_HOME", javaHomeDir.getPath());

        processExecution.setCaptureOutput(true);
        processExecution.setWaitForCompletion(1000L); // 1 second // TODO:
        // Should we wait longer than one second?
        processExecution.setKillOnTimeout(false);
    }

    /**
     * Shuts down the server by dispatching to shutdown via script or JMX. Waits
     * until the server is down.
     * 
     * @return The result of the shutdown operation - is successful
     */
    private OperationResult shutDown() {
        AvailabilityType avail = this.serverComponent.getAvailability();
        if (avail == AvailabilityType.DOWN) {
            OperationResult result = new OperationResult();
            result.setErrorMessage("The server is already shut down.");
            return result;
        }

        Configuration pluginConfig = serverComponent.getResourceContext().getPluginConfiguration();
        ApplicationServerShutdownMethod shutdownMethod = Enum.valueOf(ApplicationServerShutdownMethod.class,
            pluginConfig.getSimple(ApplicationServerPluginConfigurationProperties.SHUTDOWN_METHOD_CONFIG_PROP)
                .getStringValue());
        String resultMessage = ApplicationServerShutdownMethod.JMX.equals(shutdownMethod) ? shutdownViaJmx()
            : shutdownViaScript();

        avail = waitForServerToShutdown();
        OperationResult result;
        if (avail == AvailabilityType.UP) {
            result = new OperationResult();
            result.setErrorMessage("The server failed to shut down.");
        } else {
            return new OperationResult(resultMessage);
        }
        return result;
    }

    /**
     * Shuts down the AS server using a shutdown script.
     * 
     * @return success message if no errors are encountered
     */
    private String shutdownViaScript() {
        File shutdownScriptFile = getShutdownScriptPath();
        validateScriptFile(shutdownScriptFile,
            ApplicationServerPluginConfigurationProperties.SHUTDOWN_SCRIPT_CONFIG_PROP);
        Configuration pluginConfig = serverComponent.getResourceContext().getPluginConfiguration();
        String prefix = pluginConfig
            .getSimple(ApplicationServerPluginConfigurationProperties.SCRIPT_PREFIX_CONFIG_PROP).getStringValue();
        ProcessExecution processExecution = ProcessExecutionUtility.createProcessExecution(prefix, shutdownScriptFile);

        initProcessExecution(processExecution, shutdownScriptFile);

        String server = pluginConfig.getSimple(ApplicationServerPluginConfigurationProperties.NAMING_URL)
            .getStringValue();
        if (server != null) {
            processExecution.getArguments().add("--server=" + server);
        }

        String user = pluginConfig.getSimple(ApplicationServerComponent.PRINCIPAL_CONFIG_PROP).getStringValue();
        if (user != null) {
            processExecution.getArguments().add("--user=" + user);
        }

        String password = pluginConfig.getSimple(ApplicationServerComponent.CREDENTIALS_CONFIG_PROP).getStringValue();
        if (password != null) {
            processExecution.getArguments().add("--password=" + password);
        }

        processExecution.getArguments().add("--shutdown");

        /*
         * This tells shutdown.bat not to call the Windows PAUSE command, which
         * would cause the script to hang indefinitely waiting for input.
         * noinspection ConstantConditions
         */
        processExecution.getEnvironmentVariables().put("NOPAUSE", "1");

        if (log.isDebugEnabled()) {
            log.debug("About to execute the following process: [" + processExecution + "]");
        }
        SystemInfo systemInfo = serverComponent.getResourceContext().getSystemInformation();
        ProcessExecutionResults results = systemInfo.executeProcess(processExecution);
        logExecutionResults(results);

        if (results.getError() != null) {
            throw new RuntimeException("Error executing shutdown script while stopping AS instance. Exit code ["
                + results.getExitCode() + "]", results.getError());
        }

        return "The server has been shut down.";
    }

    private void logExecutionResults(ProcessExecutionResults results) {
        // Always log the output at info level. On Unix we could switch
        // depending on a exitCode being !=0, but ...
        log.info("Exit code from process execution: " + results.getExitCode());
        log.info("Output from process execution: " + SEPARATOR + results.getCapturedOutput() + SEPARATOR);
    }

    /**
     * Shuts down the AS server via a JMX call.
     * 
     * @return success message if no errors are encountered
     */
    private String shutdownViaJmx() {
        Configuration pluginConfig = serverComponent.getResourceContext().getPluginConfiguration();
        String mbeanName = pluginConfig.getSimple(
            ApplicationServerPluginConfigurationProperties.SHUTDOWN_MBEAN_CONFIG_PROP).getStringValue();
        String operationName = pluginConfig.getSimple(
            ApplicationServerPluginConfigurationProperties.SHUTDOWN_MBEAN_OPERATION_CONFIG_PROP).getStringValue();

        EmsConnection connection = this.serverComponent.getEmsConnection();
        if (connection == null) {
            throw new RuntimeException("Can not connect to the server");
        }
        EmsBean bean = connection.getBean(mbeanName);
        EmsOperation operation = bean.getOperation(operationName);
        /*
         * Now see if we got the 'real' method (the one with no param) or the
         * overloaded one. This is a workaround for a bug in EMS that prevents
         * finding operations with same name and different signature.
         * http://sourceforge
         * .net/tracker/index.php?func=detail&aid=2007692&group_id
         * =60228&atid=493495
         * 
         * In addition, as we offer the user to specify any MBean and any
         * method, we'd need a clever way for the user to specify parameters
         * anyway.
         */
        List<EmsParameter> params = operation.getParameters();
        int count = params.size();
        if (count == 0)
            operation.invoke(new Object[0]);
        else { // overloaded operation
            operation.invoke(new Object[] { 0 }); // return code of 0
        }

        return "The server has been shut down.";
    }

    private void validateScriptFile(File scriptFile, String scriptPropertyName) {
        if (!scriptFile.exists()) {
            throw new RuntimeException("Script (" + scriptFile + ") specified via '" + scriptPropertyName
                + "' connection property does not exist.");
        }

        if (scriptFile.isDirectory()) {
            throw new RuntimeException("Script (" + scriptFile + ") specified via '" + scriptPropertyName
                + "' connection property is a directory, not a file.");
        }
    }

    /**
     * Restart the server by first trying a shutdown and then a start. This is
     * fail fast.
     * 
     * @return A success message on success
     */
    private OperationResult restart() {
        try {
            shutDown();
        } catch (Exception e) {
            throw new RuntimeException("Shutdown may have failed: " + e);
        }

        try {
            start();
        } catch (Exception e) {
            throw new RuntimeException("Start following shutdown may have failed: " + e);
        }

        return new OperationResult("Server has been restarted.");

    }

    private AvailabilityType waitForServerToStart(long start) throws InterruptedException {
        AvailabilityType avail;
        //detect whether startWaitMax property has been set.
        Configuration pluginConfig = serverComponent.getResourceContext().getPluginConfiguration();
        PropertySimple property = pluginConfig
            .getSimple(ApplicationServerPluginConfigurationProperties.START_WAIT_MAX_PROP);
        //if set and valid, update startWaitMax value
        if ((property != null) && (property.getIntegerValue() != null)) {
            int newValue = property.getIntegerValue();
            if (newValue >= 1) {
                START_WAIT_MAX = 1000L * 60 * newValue;
            }
        }
        while (((avail = this.serverComponent.getAvailability()) == AvailabilityType.DOWN)
            && (System.currentTimeMillis() < (start + START_WAIT_MAX))) {
            try {
                Thread.sleep(START_WAIT_INTERVAL);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        return avail;
    }

    private AvailabilityType waitForServerToShutdown() {
        long start = System.currentTimeMillis();
        AvailabilityType avail;
        //detect whether stopWaitMax property has been set.
        Configuration pluginConfig = serverComponent.getResourceContext().getPluginConfiguration();
        PropertySimple property = pluginConfig
            .getSimple(ApplicationServerPluginConfigurationProperties.STOP_WAIT_MAX_PROP);
        //if set and valid update stopWaitMax value
        if ((property != null) && (property.getIntegerValue() != null)) {
            int newValue = property.getIntegerValue();
            if (newValue >= 1) {
                STOP_WAIT_MAX = 1000L * 60 * newValue;
            }
        }

        while (((avail = this.serverComponent.getAvailability()) == AvailabilityType.UP)
            && (System.currentTimeMillis() < (start + STOP_WAIT_MAX))) {
            try {
                Thread.sleep(STOP_WAIT_INTERVAL);
            } catch (InterruptedException e) {
                // ignore
            }
        }

        // After the server becomes unavailable, wait a little longer to hopefully
        // ensure shutdown is complete.
        try {
            Thread.sleep(STOP_WAIT_FINAL);
        } catch (InterruptedException e) {
            // ignore
        }
        return avail;
    }

    /**
     * Return the absolute path of this JBoss server's start script (e.g.
     * "C:\opt\jboss-5.1.0.GA\bin\run.sh").
     * 
     * @return the absolute path of this JBoss server's start script (e.g.
     *         "C:\opt\jboss-5.1.0.GA\bin\run.sh")
     */
    @NotNull
    public File getStartScriptPath() {
        Configuration pluginConfig = serverComponent.getResourceContext().getPluginConfiguration();
        String startScript = pluginConfig.getSimpleValue(
            ApplicationServerPluginConfigurationProperties.START_SCRIPT_CONFIG_PROP, DEFAULT_START_SCRIPT);
        File startScriptFile = resolvePathRelativeToHomeDir(startScript);
        return startScriptFile;
    }

    @NotNull
    private File resolvePathRelativeToHomeDir(@NotNull String path) {
        return resolvePathRelativeToHomeDir(serverComponent.getResourceContext().getPluginConfiguration(), path);
    }

    @NotNull
    private File resolvePathRelativeToHomeDir(Configuration pluginConfig, @NotNull String path) {
        File configDir = new File(path);
        if (!configDir.isAbsolute()) {
            String jbossHomeDir = getRequiredPropertyValue(pluginConfig,
                ApplicationServerPluginConfigurationProperties.HOME_DIR);
            configDir = new File(jbossHomeDir, path);
        }

        return configDir;
    }

    @NotNull
    private String getRequiredPropertyValue(@NotNull Configuration config, @NotNull String propName) {
        String propValue = config.getSimpleValue(propName, null);
        if (propValue == null) {
            // Something's not right - neither autodiscovery, nor the config
            // edit GUI, should ever allow this.
            throw new IllegalStateException("Required property '" + propName + "' is not set.");
        }

        return propValue;
    }

    /**
     * Return the absolute path of this JBoss server's shutdown script (e.g.
     * "C:\opt\jboss-5.1.0.GA\bin\shutdown.sh").
     * 
     * @return the absolute path of this JBoss server's shutdown script (e.g.
     *         "C:\opt\jboss-5.1.0.GA\bin\shutdown.sh")
     */
    @NotNull
    public File getShutdownScriptPath() {
        Configuration pluginConfig = serverComponent.getResourceContext().getPluginConfiguration();
        String shutdownScript = pluginConfig.getSimpleValue(
            ApplicationServerPluginConfigurationProperties.SHUTDOWN_SCRIPT_CONFIG_PROP, DEFAULT_SHUTDOWN_SCRIPT);
        File shutdownScriptFile = resolvePathRelativeToHomeDir(shutdownScript);
        return shutdownScriptFile;
    }

    /**
     * Return the absolute path of this JBoss server's JAVA_HOME directory (e.g. "C:\opt\jdk1.5.0_14"), as defined by
     * the 'javaHome' plugin config prop, or null if that prop is not set.
     * 
     * @return the absolute path of this JBoss server's JAVA_HOME directory, as defined by
     *         the 'javaHome' plugin config prop, or null if that prop is not set
     */
    @Nullable
    public File getJavaHomePath() {
        Configuration pluginConfig = serverComponent.getResourceContext().getPluginConfiguration();
        String javaHomePath = pluginConfig.getSimpleValue(ApplicationServerPluginConfigurationProperties.JAVA_HOME,
            null);
        File javaHome = (javaHomePath != null) ? new File(javaHomePath) : null;
        return javaHome;
    }

    void validateJavaHomePathProperty() {
        Configuration pluginConfig = serverComponent.getResourceContext().getPluginConfiguration();
        String javaHome = pluginConfig.getSimple(ApplicationServerPluginConfigurationProperties.JAVA_HOME)
            .getStringValue();
        if (javaHome != null) {
            File javaHomeDir = new File(javaHome);
            if (!javaHomeDir.isAbsolute()) {
                throw new InvalidPluginConfigurationException(
                    ApplicationServerPluginConfigurationProperties.JAVA_HOME
                        + " connection property ('"
                        + javaHomeDir
                        + "') is not an absolute path. Note, on Windows, absolute paths must start with the drive letter (e.g. C:).");
            }

            if (!javaHomeDir.exists()) {
                throw new InvalidPluginConfigurationException(ApplicationServerPluginConfigurationProperties.JAVA_HOME
                    + " connection property ('" + javaHomeDir + "') does not exist.");
            }

            if (!javaHomeDir.isDirectory()) {
                throw new InvalidPluginConfigurationException(ApplicationServerPluginConfigurationProperties.JAVA_HOME
                    + " connection property ('" + javaHomeDir + "') is not a directory.");
            }
        }
    }
}
