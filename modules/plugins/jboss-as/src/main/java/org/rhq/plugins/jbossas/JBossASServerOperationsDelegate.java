/*
 * Jopr Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.plugins.jbossas;

import java.io.File;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.operation.EmsOperation;
import org.mc4j.ems.connection.bean.parameter.EmsParameter;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.pluginapi.util.ProcessExecutionUtility;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.SystemInfo;

/**
 * Handles performing operations on a JBossAS instance.
 *
 * @author Ian Springer
 * @author Jason Dobies
 * @author Jay Shaughnessy
 */
public class JBossASServerOperationsDelegate {

    /** max amount of time to wait for server to show as unavailable after executing stop - in milliseconds */
    private static long STOP_WAIT_MAX = 1000L * 150; // 2.5 minutes

    /** amount of time to wait between availability checks when performing a stop - in milliseconds */
    private static final long STOP_WAIT_INTERVAL = 1000L * 10; // 10 seconds

    /** amount of time to wait for stop to complete after the loop that checks for DOWN availability terminates -
     *  in milliseconds */
    private static final long STOP_WAIT_FINAL = 1000L * 30; // 30 seconds

    /** max amount of time to wait for start to complete - in milliseconds */
    private static long START_WAIT_MAX = 1000L * 300; // 5 minutes

    /** amount of time to wait between availability checks when performing a start - in milliseconds */
    private static final long START_WAIT_INTERVAL = 1000L * 10; // 10 seconds

    private final Log log = LogFactory.getLog(JBossASServerOperationsDelegate.class);

    private static final String SEPARATOR = "\n-----------------------\n";
    // Attributes  --------------------------------------------

    /**
     * Server component against which the operations are being performed.
     */
    private JBossASServerComponent serverComponent;

    /**
     * Passed in from the resource context for making process calls.
     */
    private SystemInfo systemInfo;

    // Constructors  --------------------------------------------

    public JBossASServerOperationsDelegate(JBossASServerComponent serverComponent, SystemInfo systemInfo) {
        this.serverComponent = serverComponent;
        this.systemInfo = systemInfo;
    }

    // Public  --------------------------------------------

    /**
     * Performs the specified operation. The result of the operation will be indicated in the return. If there is an
     * error, an <code>RuntimeException</code> will be thrown.
     *
     * @param  operation  the operation to perform
     * @param  parameters parameters to the operation call
     *
     * @return if successful, the result object will contain a success message
     *
     * @throws RuntimeException if any errors occur while trying to perform the operation
     */
    public OperationResult invoke(JBossASServerSupportedOperations operation, Configuration parameters)
        throws InterruptedException {
        String message = null;

        switch (operation) {
        case RESTART: {
            message = restart();
            break;
        }

        case SHUTDOWN: {
            message = shutdown();
            break;
        }

        case START: {
            message = start();
            break;
        }
        }

        OperationResult result = new OperationResult(message);
        return result;
    }

    // Private  --------------------------------------------

    /**
     * Starts the underlying AS server.
     *
     * @return success message if no errors are encountered
     * @throws InterruptedException if the plugin container stops this operation while its executing
     */
    private String start() throws InterruptedException {
        Configuration pluginConfiguration = this.serverComponent.getPluginConfiguration();
        File startScriptFile = this.serverComponent.getStartScriptPath();
        validateScriptFile(startScriptFile, JBossASServerComponent.START_SCRIPT_CONFIG_PROP);

        String prefix = pluginConfiguration.getSimple(JBossASServerComponent.SCRIPT_PREFIX_CONFIG_PROP)
            .getStringValue();
        String configName = this.serverComponent.getConfigurationSet();
        String bindingAddress = pluginConfiguration.getSimple(JBossASServerComponent.BINDING_ADDRESS_CONFIG_PROP)
            .getStringValue();

        String configArgument = "-c" + configName;
        String bindingAddressArgument = null;
        if (bindingAddress != null)
            bindingAddressArgument = "-b" + bindingAddress;

        ProcessExecution processExecution;

        // prefix is either null or contains ONLY whitespace characters
        if (prefix == null || prefix.replaceAll("\\s", "").equals("")) {
            processExecution = ProcessExecutionUtility.createProcessExecution(startScriptFile);

            processExecution.getArguments().add("-c");
            processExecution.getArguments().add(configName);

            if (bindingAddressArgument != null) {
                processExecution.getArguments().add("-b");
                processExecution.getArguments().add(bindingAddress);
            }
        } else {
            // The process execution should be tied to the process represented as the prefix. If there are any other
            // tokens in the prefix, consider them arguments to the prefix process.
            StringTokenizer prefixTokenizer = new StringTokenizer(prefix);
            String processName = prefixTokenizer.nextToken();
            File prefixProcess = new File(processName);

            processExecution = ProcessExecutionUtility.createProcessExecution(prefixProcess);

            while (prefixTokenizer.hasMoreTokens()) {
                String prefixArgument = prefixTokenizer.nextToken();
                processExecution.getArguments().add(prefixArgument);
            }

            // Assemble the AS start script and its prefixes as one argument to the prefix
            String startScriptArgument = startScriptFile.getAbsolutePath();
            startScriptArgument += " " + configArgument;

            if (bindingAddressArgument != null) {
                startScriptArgument += " " + bindingAddressArgument;
            }

            processExecution.getArguments().add(startScriptArgument);
        }

        initProcessExecution(processExecution, startScriptFile);

        long start = System.currentTimeMillis();
        if (log.isDebugEnabled()) {
            log.debug("About to execute the following process: [" + processExecution + "]");
        }
        ProcessExecutionResults results = this.systemInfo.executeProcess(processExecution);
        logExecutionResults(results);

        AvailabilityType avail;
        if (results.getError() == null) {
            avail = waitForServerToStart(start);
        } else {
            log.error("Error from process execution while starting the AS instance. Exit code ["
                + results.getExitCode() + "]", results.getError());
            avail = this.serverComponent.getAvailability();
        }

        // If, after the loop, the Server is still down, consider the start to be a failure.
        if (avail == AvailabilityType.DOWN) {
            throw new RuntimeException("Server failed to start: " + results.getCapturedOutput());
        } else {
            return "Server has been started.";
        }
    }

    private void initProcessExecution(ProcessExecution processExecution, File scriptFile) {
        // NOTE: For both run.bat and shutdown.bat, the current working dir must be set to the script's parent dir
        //       (e.g. ${JBOSS_HOME}/bin) for the script to work.
        processExecution.setWorkingDirectory(scriptFile.getParent());

        // Both scripts require the JAVA_HOME env var to be set.
        File javaHomeDir = this.serverComponent.getJavaHomePath();
        if (javaHomeDir == null) {
            throw new IllegalStateException("The '" + JBossASServerComponent.JAVA_HOME_PATH_CONFIG_PROP
                + "' connection property must be set in order to start or stop JBossAS via script.");
        }

        this.serverComponent.validateJavaHomePathProperty();
        processExecution.getEnvironmentVariables().put("JAVA_HOME", javaHomeDir.getPath());

        processExecution.setCaptureOutput(true);
        processExecution.setWaitForCompletion(1000L); // 1 second // TODO: Should we wait longer than one second?
        processExecution.setKillOnTimeout(false);
    }

    /**
     * Shuts down the server by dispatching to shutdown via script or JMX. Waits until the server is down.
     * @return The result of the shutdown operation - is successful
     */
    private String shutdown() {
        JBossASServerShutdownMethod shutdownMethod = Enum.valueOf(JBossASServerShutdownMethod.class,
            this.serverComponent.getPluginConfiguration().getSimple(JBossASServerComponent.SHUTDOWN_METHOD_CONFIG_PROP)
                .getStringValue());
        String result = JBossASServerShutdownMethod.JMX.equals(shutdownMethod) ? shutdownViaJmx() : shutdownViaScript();
        AvailabilityType avail = waitForServerToShutdown();
        if (avail == AvailabilityType.UP) {
            throw new RuntimeException("Server failed to shutdown");
        } else {
            return result;
        }
    }

    /**
     * Shuts down the AS server using a shutdown script.
     *
     * @return success message if no errors are encountered
     */
    private String shutdownViaScript() {
        Configuration pluginConfiguration = this.serverComponent.getPluginConfiguration();
        File shutdownScriptFile = this.serverComponent.getShutdownScriptPath();
        validateScriptFile(shutdownScriptFile, JBossASServerComponent.SHUTDOWN_SCRIPT_CONFIG_PROP);
        String prefix = pluginConfiguration.getSimple(JBossASServerComponent.SCRIPT_PREFIX_CONFIG_PROP)
            .getStringValue();
        ProcessExecution processExecution = ProcessExecutionUtility.createProcessExecution(prefix, shutdownScriptFile);

        initProcessExecution(processExecution, shutdownScriptFile);

        String server = pluginConfiguration.getSimple(JBossASServerComponent.NAMING_URL_CONFIG_PROP).getStringValue();
        if (server != null) {
            processExecution.getArguments().add("--server=" + server);
        }

        String user = pluginConfiguration.getSimple(JBossASServerComponent.PRINCIPAL_CONFIG_PROP).getStringValue();
        if (user != null) {
            processExecution.getArguments().add("--user=" + user);
        }

        String password = pluginConfiguration.getSimple(JBossASServerComponent.CREDENTIALS_CONFIG_PROP)
            .getStringValue();
        if (password != null) {
            processExecution.getArguments().add("--password=" + password);
        }

        processExecution.getArguments().add("--shutdown");

        /* This tells shutdown.bat not to call the Windows PAUSE command, which
         * would cause the script to hang indefinitely waiting for input.
         * noinspection ConstantConditions
         */
        processExecution.getEnvironmentVariables().put("NOPAUSE", "1");

        if (log.isDebugEnabled()) {
            log.debug("About to execute the following process: [" + processExecution + "]");
        }
        ProcessExecutionResults results = this.systemInfo.executeProcess(processExecution);
        logExecutionResults(results);

        if (results.getError() != null) {
            throw new RuntimeException("Error executing shutdown script while stopping AS instance. Exit code ["
                + results.getExitCode() + "]", results.getError());
        }

        return "Server has been shut down.";
    }

    private void logExecutionResults(ProcessExecutionResults results) {
        // Always log the output at info level. On Unix we could switch depending on a exitCode being !=0, but ...
        log.info("Exit code from process execution: " + results.getExitCode());
        log.info("Output from process execution: " + SEPARATOR + results.getCapturedOutput() + SEPARATOR);
    }

    /**
     * Shuts down the AS server via a JMX call.
     *
     * @return success message if no errors are encountered
     */
    private String shutdownViaJmx() {
        Configuration pluginConfiguration = this.serverComponent.getPluginConfiguration();
        String mbeanName = pluginConfiguration.getSimple(JBossASServerComponent.SHUTDOWN_MBEAN_CONFIG_PROP)
            .getStringValue();
        String operationName = pluginConfiguration.getSimple(
            JBossASServerComponent.SHUTDOWN_MBEAN_OPERATION_CONFIG_PROP).getStringValue();

        EmsConnection connection = this.serverComponent.getEmsConnection();
        if (connection == null) {
            throw new RuntimeException("Can not connect to the server");
        }
        EmsBean bean = connection.getBean(mbeanName);
        EmsOperation operation = bean.getOperation(operationName);
        /*
         *  Now see if we got the 'real' method (the one with no param) or the overloaded one.
         *  This is a workaround for a bug in EMS that prevents finding operations with same name
         *  and different signature.
         *  http://sourceforge.net/tracker/index.php?func=detail&aid=2007692&group_id=60228&atid=493495
         *
         *   In addition, as we offer the user to specify any MBean and any method, we'd need a
         *   clever way for the user to specify parameters anyway.
         */
        List<EmsParameter> params = operation.getParameters();
        int count = params.size();
        if (count == 0)
            operation.invoke(new Object[0]);
        else { // overloaded operation
            operation.invoke(new Object[] { 0 }); // return code of 0
        }

        return "Server has been shut down.";
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
     * Restart the server by first trying a shutdown and then a start. This is fail fast.
     * @return A success message on success
     */
    private String restart() {

        try {
            shutdown();
        } catch (Exception e) {
            throw new RuntimeException("Shutdown may have failed: " + e);
        }

        try {
            // Perform the restart.
            start();

        } catch (Exception e) {
            throw new RuntimeException("Re-Startup may have failed: " + e);
        }

        return "Server has been restarted.";

    }

    private AvailabilityType waitForServerToStart(long start) throws InterruptedException {
        AvailabilityType avail;
        //detect whether startWaitMax property has been set.
        Configuration pluginConfig = serverComponent.getPluginConfiguration();
        PropertySimple property = pluginConfig.getSimple(JBossASServerComponent.START_WAIT_MAX_PROP);
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
        //detect whether stopWaitMax property has been set.
        Configuration pluginConfig = serverComponent.getPluginConfiguration();
        PropertySimple property = pluginConfig.getSimple(JBossASServerComponent.STOP_WAIT_MAX_PROP);
        //if set and valid, update startWaitMax value
        if ((property != null) && (property.getIntegerValue() != null)) {
            int newValue = property.getIntegerValue();
            if (newValue >= 1) {
                STOP_WAIT_MAX = 1000L * 60 * newValue;
            }
        }

        for (long wait = 0L; (wait < STOP_WAIT_MAX) && (AvailabilityType.UP == this.serverComponent.getAvailability()); wait += STOP_WAIT_INTERVAL) {
            try {
                Thread.sleep(STOP_WAIT_INTERVAL);
            } catch (InterruptedException e) {
                // ignore
            }
        }

        // After the server shows unavailable, wait a little longer to hopefully ensure shutdown is complete.
        try {
            Thread.sleep(STOP_WAIT_FINAL);
        } catch (InterruptedException e) {
            // ignore
        }

        return this.serverComponent.getAvailability();
    }

}