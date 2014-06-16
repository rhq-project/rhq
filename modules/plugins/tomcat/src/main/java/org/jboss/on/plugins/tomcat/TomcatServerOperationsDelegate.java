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
package org.jboss.on.plugins.tomcat;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.operation.EmsOperation;

import org.jboss.on.plugins.tomcat.TomcatServerComponent.ControlMethod;
import org.jboss.on.plugins.tomcat.TomcatServerComponent.SupportedOperations;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.pluginapi.util.ProcessExecutionUtility;
import org.rhq.core.system.OperatingSystemType;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.SystemInfo;

/**
 * Handles performing operations on a Tomcat Server
 *
 * @author Jay Shaughnessy
 * @author Ian Springer
 * @author Jason Dobies
 * @author Lukas Krejci
 */
public class TomcatServerOperationsDelegate {

    public static final String SHUTDOWN_SCRIPT_ENVIRONMENT_PROPERTY = "shutdownScriptEnvironment";
    public static final String START_SCRIPT_ENVIRONMENT_PROPERTY = "startScriptEnvironment";

    private static final String SERVER_MBEAN_NAME = "Catalina:type=Server";

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

    private final Log log = LogFactory.getLog(this.getClass());

    private static final String SEPARATOR = "\n-----------------------\n";
    // Attributes  --------------------------------------------

    /**
     * Server component against which the operations are being performed.
     */
    private TomcatServerComponent serverComponent;

    /**
     * Passed in from the resource context for making process calls.
     */
    private SystemInfo systemInfo;

    // Constructors  --------------------------------------------

    public TomcatServerOperationsDelegate(TomcatServerComponent serverComponent, SystemInfo systemInfo) {
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
    public OperationResult invoke(SupportedOperations operation, Configuration parameters) throws InterruptedException {
        String message = null;

        switch (operation) {

        case RESTART: {
            message = restart(parameters);
            break;
        }

        case SHUTDOWN: {
            message = shutdown(parameters);
            break;
        }

        case START: {
            message = start(parameters);
            break;
        }

        case STORECONFIG: {
            message = storeConfig();
            break;
        }
        }

        OperationResult result = new OperationResult(message);
        return result;
    }

    // Private  --------------------------------------------

    private String start(Configuration parameters) throws InterruptedException {
        PropertyList env = parameters.getList(START_SCRIPT_ENVIRONMENT_PROPERTY);
        return start(env);
    }

    /**
     * Starts the underlying server.
     *
     * @return success message if no errors are encountered
     * @throws InterruptedException if the plugin container stops this operation while its executing
     */
    private String start(PropertyList environment) throws InterruptedException {
        Configuration pluginConfiguration = this.serverComponent.getPluginConfiguration();
        String controlMethodName = pluginConfiguration.getSimpleValue(
            TomcatServerComponent.PLUGIN_CONFIG_CONTROL_METHOD, ControlMethod.SCRIPT.name());
        ControlMethod controlMethod = ControlMethod.valueOf(controlMethodName);
        ProcessExecution processExecution = (controlMethod == ControlMethod.SCRIPT) ? getScriptStart(pluginConfiguration)
            : getRpmStart(pluginConfiguration);

        applyEnvironmentVars(environment, processExecution);

        long start = System.currentTimeMillis();
        if (log.isDebugEnabled()) {
            log.debug("About to execute the following process: [" + processExecution + "]");
        }
        ProcessExecutionResults results = this.systemInfo.executeProcess(processExecution);
        logExecutionResults(results);
        Throwable error = results.getError();
        Integer exitCode = results.getExitCode();
        AvailabilityType avail;

        if (startScriptFailed(controlMethod, error, exitCode, isWindows())) {
            String output = results.getCapturedOutput();
            String message = "Script returned error or non-zero exit code while starting the Tomcat instance - exitCode=["
                + ((exitCode != null) ? exitCode : "UNKNOWN") + "], output=[" + output + "].";
            if (error == null) {
                log.error(message);
            } else {
                log.error(message, error);
            }
            avail = this.serverComponent.getAvailability();
        } else {
            avail = waitForServerToStart(start);
        }

        // If, after the loop, the Server is still down, consider the start to be a failure.
        if (avail == AvailabilityType.DOWN) {
            throw new RuntimeException("Server failed to start: " + results.getCapturedOutput());
        } else {
            return "Server has been started.";
        }
    }

    private static boolean startScriptFailed(ControlMethod controlMethod, Throwable error, Integer exitCode, boolean isWindows) {
        if (error != null || exitCode == null) {
            return true;
        }
        if (controlMethod == ControlMethod.SCRIPT && isWindows) {
            // Believe it or not, an exit code of 1 from startup.bat does not indicate an error.
            return exitCode != 0 && exitCode != 1;
        } else {
            return exitCode != 0;
        }
    }

    private ProcessExecution getScriptStart(Configuration pluginConfiguration) {
        File startScriptFile = this.serverComponent.getStartScriptPath();
        validateScriptFile(startScriptFile, TomcatServerComponent.PLUGIN_CONFIG_START_SCRIPT);

        String prefix = pluginConfiguration.getSimple(TomcatServerComponent.PLUGIN_CONFIG_SCRIPT_PREFIX)
            .getStringValue();

        ProcessExecution processExecution;

        // prefix is either null or contains ONLY whitespace characters
        if (prefix == null || prefix.replaceAll("\\s", "").equals("")) {
            processExecution = ProcessExecutionUtility.createProcessExecution(startScriptFile);
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

            processExecution.getArguments().add(startScriptArgument);
        }

        initScriptProcessExecution(processExecution, startScriptFile);

        return processExecution;
    }

    private ProcessExecution getRpmStart(Configuration pluginConfiguration) {
        ProcessExecution processExecution;
        String rpm = getTomcatServiceNum();

        if (isWindows()) {
            processExecution = new ProcessExecution("net");
            // disable the executable existence check because it is a command on the supplied PATH
            processExecution.setCheckExecutableExists(false);
            processExecution.setArguments(new ArrayList<String>());
            processExecution.getArguments().add("start");
            processExecution.getArguments().add(rpm);
        } else {
            processExecution = new ProcessExecution("service");
            // disable the executable existence check because it is a command on the supplied PATH
            processExecution.setCheckExecutableExists(false);
            processExecution.setArguments(new ArrayList<String>());
            processExecution.getArguments().add(rpm);
            processExecution.getArguments().add("start");
        }

        Map<String, String> envVars = new LinkedHashMap<String, String>(System.getenv());
        processExecution.setEnvironmentVariables(envVars);

        initProcessExecution(processExecution);

        return processExecution;
    }

    private String shutdown(Configuration parameters) throws InterruptedException {
        PropertyList env = parameters.getList(SHUTDOWN_SCRIPT_ENVIRONMENT_PROPERTY);
        return shutdown(env);
    }

    private String shutdown(PropertyList environment) throws InterruptedException {
        String result = doShutdown(environment);
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
    private String doShutdown(PropertyList environment) {
        Configuration pluginConfiguration = this.serverComponent.getPluginConfiguration();
        // NOTE: In TomcatDiscoveryComponent.java we set TomcatServerComponent.PLUGIN_CONFIG_SHUTDOWN_SCRIPT (amongst others).
        String controlMethod = pluginConfiguration.getSimpleValue(TomcatServerComponent.PLUGIN_CONFIG_CONTROL_METHOD,
            ControlMethod.SCRIPT.name());
        ProcessExecution processExecution = (ControlMethod.SCRIPT.name().equals(controlMethod)) ? getScriptShutdown(pluginConfiguration)
            : getRpmShutdown();

        applyEnvironmentVars(environment, processExecution);

        if (log.isDebugEnabled()) {
            log.debug("About to execute the following process: [" + processExecution + "]");
        }
        ProcessExecutionResults results = this.systemInfo.executeProcess(processExecution);
        logExecutionResults(results);
        Throwable error = results.getError();
        Integer exitCode = results.getExitCode();

        if ((null != error) || ((null != exitCode) && (0 != exitCode))) {
            String message = "Script returned error or non-zero exit code while shutting down the Tomcat instance. Exit code ["
                + exitCode + "]";
            if (null == error) {
                throw new RuntimeException(message);
            } else {
                throw new RuntimeException(message, error);
            }
        }

        return "Server has been shut down.";
    }

    private ProcessExecution getScriptShutdown(Configuration pluginConfiguration) {
        File shutdownScriptFile = this.serverComponent.getShutdownScriptPath();
        validateScriptFile(shutdownScriptFile, TomcatServerComponent.PLUGIN_CONFIG_SHUTDOWN_SCRIPT);
        String prefix = pluginConfiguration.getSimple(TomcatServerComponent.PLUGIN_CONFIG_SCRIPT_PREFIX)
            .getStringValue();
        ProcessExecution processExecution = ProcessExecutionUtility.createProcessExecution(prefix, shutdownScriptFile);

        initScriptProcessExecution(processExecution, shutdownScriptFile);

        return processExecution;
    }

    private String getTomcatServiceNum() {
        String catalinaHome = this.serverComponent.getCatalinaHome().getPath();
        String serviceName = this.serverComponent.getServiceName();
        if (serviceName != null)
            return serviceName;
        String rpm = TomcatDiscoveryComponent.EWS_TOMCAT_8;
        if (TomcatDiscoveryComponent.isTomcat7(catalinaHome))
            rpm = TomcatDiscoveryComponent.EWS_TOMCAT_7;
        if (TomcatDiscoveryComponent.isTomcat6(catalinaHome))
            rpm = TomcatDiscoveryComponent.EWS_TOMCAT_6;
        if (TomcatDiscoveryComponent.isTomcat5(catalinaHome))
            rpm = TomcatDiscoveryComponent.EWS_TOMCAT_5;
        return rpm;
    }

    private ProcessExecution getRpmShutdown() {
        ProcessExecution processExecution;
        String rpm = getTomcatServiceNum();

        if (isWindows()) {
            processExecution = new ProcessExecution("net");
            // disable the executable existence check because it is a command on the supplied PATH
            processExecution.setCheckExecutableExists(false);
            processExecution.setArguments(new ArrayList<String>());
            processExecution.getArguments().add("stop");
            processExecution.getArguments().add(rpm);
        } else {
            processExecution = new ProcessExecution("service");
            // disable the executable existence check because it is a command on the supplied PATH
            processExecution.setCheckExecutableExists(false);
            processExecution.setArguments(new ArrayList<String>());
            processExecution.getArguments().add(rpm);
            processExecution.getArguments().add("stop");
        }

        Map<String, String> envVars = new LinkedHashMap<String, String>(System.getenv());
        log.info("Operation Envs: " + envVars);
        processExecution.setEnvironmentVariables(envVars);

        initProcessExecution(processExecution);

        return processExecution;
    }

    static public void setProcessExecutionEnvironment(ProcessExecution processExecution, String catalinaHome,
        String catalinaBase) {
        String jreHomeDir = System.getProperty("java.home");
        if (null == jreHomeDir) {
            throw new IllegalStateException(
                "The JAVA_HOME or JAVA_JRE environment variable must be set in order to run the Tomcat scripts.");
        }

        Map<String, String> processExecutionEnvironmentVariables = processExecution.getEnvironmentVariables();
        if (null == processExecutionEnvironmentVariables) {
            processExecutionEnvironmentVariables = new LinkedHashMap<String, String>();
            processExecution.setEnvironmentVariables(processExecutionEnvironmentVariables);
        }

        // It is important to realize that the processExecutionEnvironmentVariables may have been inheriting the
        // environment of the RHQ Agent.  The RHQ Agent allows for JAVA_HOME to be set to a JRE and does not
        // use JRE_HOME. Tomcat does not allow this, and favors the use of JRE_HOME. So, unset JAVA_HOME and
        // reset it as needed. Always set JRE_HOME.
        processExecutionEnvironmentVariables.remove("JAVA_HOME");
        processExecutionEnvironmentVariables.put("JRE_HOME", new File(jreHomeDir).getPath());
        processExecutionEnvironmentVariables.put("CATALINA_HOME", catalinaHome);
        processExecutionEnvironmentVariables.put("CATALINA_BASE", catalinaBase);
        processExecutionEnvironmentVariables.put("CATALINA_TMPDIR", catalinaBase + File.separator + "temp");

        // Tomcat, starting with 5.5, requires only a JRE to run. But, if TC is running in debug mode it
        // requires a JDK.  We always set JRE_HOME above but, if possible, set JAVA_HOME as well if 
        // in fact it looks like we have a JDK at our disposal.
        if (jreHomeDir.endsWith("jre")) {
            File jdkHomeDir = new File(jreHomeDir.substring(0, jreHomeDir.length() - 3));
            // one more check, look for a bin dir
            if (new File(jdkHomeDir, "bin").isDirectory()) {
                processExecutionEnvironmentVariables.put("JAVA_HOME", jdkHomeDir.getPath());
            }
        }
    }

    private void initScriptProcessExecution(ProcessExecution processExecution, File scriptFile) {
        // For the script to work the current working dir must be set to the script's parent dir
        processExecution.setWorkingDirectory(scriptFile.getParent());

        // Set necessary environment variables
        setProcessExecutionEnvironment(processExecution, this.serverComponent.getCatalinaHome().getPath(),
            this.serverComponent.getCatalinaBase().getPath());

        initProcessExecution(processExecution);
    }

    private void initProcessExecution(ProcessExecution processExecution) {
        processExecution.setCaptureOutput(true);
        processExecution.setWaitForCompletion(120000L); // 120 seconds - that should be safe? // TODO: make this configurable 
        processExecution.setKillOnTimeout(false);
    }

    private void logExecutionResults(ProcessExecutionResults results) {
        if (log.isDebugEnabled()) {
            log.debug("Exit code from process execution: " + results.getExitCode());
            log.debug("Output from process execution: " + SEPARATOR + results.getCapturedOutput() + SEPARATOR);
        }
    }

    private String restart(Configuration parameters) {
        StringBuffer result = new StringBuffer();
        boolean problem = false;

        try {
            shutdown(parameters);
        } catch (Exception e) {
            problem = true;
            result.append("Shutdown may have failed: ");
            result.append(e);
            result.append(", ");
        } finally {
            try {
                // Wait for server to show as unavailable, up to max wait time.
                AvailabilityType avail = waitForServerToShutdown();
                if (avail == AvailabilityType.UP) {
                    problem = true;
                    result.append("Shutdown may have failed (server appears to still be running), ");
                }
                // Perform the restart.
                start(parameters);

            } catch (Exception e) {
                problem = true;
                result.append("Startup may have failed: ");
                result.append(e);
                result.append(", ");
            }
        }

        if (problem) {
            result.append("Restart may have failed.");
        } else {
            result.append("Server has been restarted.");
        }

        return result.toString();
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

    private AvailabilityType waitForServerToStart(long start) throws InterruptedException {
        AvailabilityType avail;

        //detect whether startWaitMax property has been set.
        Configuration pluginConfig = serverComponent.getResourceContext().getPluginConfiguration();
        PropertySimple property = pluginConfig.getSimple(TomcatServerComponent.START_WAIT_MAX_PROP);
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
        Configuration pluginConfig = serverComponent.getResourceContext().getPluginConfiguration();
        PropertySimple property = pluginConfig.getSimple(TomcatServerComponent.STOP_WAIT_MAX_PROP);
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

    private String storeConfig() {
        EmsConnection connection = this.serverComponent.getEmsConnection();
        if (connection == null) {
            throw new RuntimeException("Can not connect to the server");
        }
        EmsBean bean = connection.getBean(SERVER_MBEAN_NAME);
        EmsOperation operation = bean.getOperation("storeConfig");
        operation.invoke(new Object[0]);

        return ("Tomcat configuration updated.");
    }

    private static void applyEnvironmentVars(PropertyList environment, ProcessExecution processExecution) {
        if (environment != null) {
            Map<String, String> environmentVariables = processExecution.getEnvironmentVariables();
            for (Property prop : environment.getList()) {
                PropertyMap var = (PropertyMap) prop;
                environmentVariables.put(var.getSimpleValue("name", null), var.getSimpleValue("value", null));
            }
            processExecution.setEnvironmentVariables(environmentVariables);
        }
    }

    private static boolean isWindows() {
        return File.separatorChar == '\\';
    }
}
