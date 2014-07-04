/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.plugins.jbossas7;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.rhq.core.domain.measurement.AvailabilityType.DOWN;
import static org.rhq.core.domain.measurement.AvailabilityType.UP;
import static org.rhq.core.pluginapi.bundle.BundleHandoverResponse.FailureType.EXECUTION;
import static org.rhq.core.pluginapi.bundle.BundleHandoverResponse.FailureType.INVALID_PARAMETER;
import static org.rhq.modules.plugins.jbossas7.JBossProductType.AS;
import static org.rhq.modules.plugins.jbossas7.JBossProductType.WILDFLY8;
import static org.rhq.modules.plugins.jbossas7.util.ProcessExecutionLogger.logExecutionResults;
import static org.rhq.modules.plugins.jbossas7.util.PropertyReplacer.replacePropertyPatterns;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;

import org.jboss.sasl.util.UsernamePasswordHashUtil;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.bundle.BundleHandoverFacet;
import org.rhq.core.pluginapi.bundle.BundleHandoverRequest;
import org.rhq.core.pluginapi.bundle.BundleHandoverResponse;
import org.rhq.core.pluginapi.event.log.LogFileEventResourceComponentHelper;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.pluginapi.util.ProcessExecutionUtility;
import org.rhq.core.pluginapi.util.StartScriptConfiguration;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.ProcessInfo;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.util.PropertiesFileUpdate;
import org.rhq.core.util.StringUtil;
import org.rhq.core.util.file.FileUtil;
import org.rhq.modules.plugins.jbossas7.helper.HostConfiguration;
import org.rhq.modules.plugins.jbossas7.helper.HostPort;
import org.rhq.modules.plugins.jbossas7.helper.ServerPluginConfiguration;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.ComplexResult;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;
import org.rhq.modules.plugins.jbossas7.json.ReadResource;
import org.rhq.modules.plugins.jbossas7.json.Result;
import org.rhq.modules.plugins.jbossas7.json.ResultFailedException;

/**
 * Base component for functionality that is common to Standalone Servers and Host Controllers.
 *
 * @author Heiko W. Rupp
 * @author Ian Springer
 */
public abstract class BaseServerComponent<T extends ResourceComponent<?>> extends BaseComponent<T> implements
    MeasurementFacet, BundleHandoverFacet {

    private static final Log LOG = LogFactory.getLog(BaseServerComponent.class);

    protected static final long MAX_TIMEOUT_FAILURE_WAIT = 5 * 60 * 1000L;

    private ASConnection connection;
    private LogFileEventResourceComponentHelper logFileEventDelegate;
    private StartScriptConfiguration startScriptConfig;
    private ServerPluginConfiguration serverPluginConfig;
    private AvailabilityType previousAvailabilityType;
    private String releaseVersion;
    private String aSHostName;
    private DocumentBuilderFactory docBuilderFactory;
    private long lastManagementInterfaceReply = 0;

    @Override
    public void start(ResourceContext<T> resourceContext) throws InvalidPluginConfigurationException, Exception {
        super.start(resourceContext);
        docBuilderFactory = DocumentBuilderFactory.newInstance();
        serverPluginConfig = new ServerPluginConfiguration(pluginConfiguration);
        serverPluginConfig.validate();
        connection = new ASConnection(ASConnectionParams.createFrom(serverPluginConfig));
        setASHostName(findASDomainHostName());
        getAvailability();
        logFileEventDelegate = new LogFileEventResourceComponentHelper(context);
        logFileEventDelegate.startLogFileEventPollers();
        startScriptConfig = new StartScriptConfiguration(pluginConfiguration);
    }

    @Override
    public void stop() {
        connection.shutdown();
        logFileEventDelegate.stopLogFileEventPollers();
        previousAvailabilityType = null;
    }

    @Override
    public AvailabilityType getAvailability() {
        AvailabilityType availabilityType;
        try {
            try {
                readAttribute(getHostAddress(), "name", AVAIL_OP_TIMEOUT_SECONDS);
                availabilityType = UP;
                lastManagementInterfaceReply = new Date().getTime();
            } catch (ResultFailedException e) {
                LOG.warn("Domain host name seems to be changed, re-reading from  "
                    + getServerPluginConfiguration().getHostConfigFile());
                setASHostName(findASDomainHostName());
                LOG.info("Detected domain host name [" + getASHostName() + "]");
                try {
                    readAttribute(getHostAddress(), "name");
                    availabilityType = UP;
                } catch (Exception ex) {
                    throw ex;
                }
            } catch (Exception ex) {
                throw ex;
            }
        } catch (TimeoutException e) {
            long now = new Date().getTime();

            if (now - lastManagementInterfaceReply > MAX_TIMEOUT_FAILURE_WAIT) {
                availabilityType = DOWN;
            } else {
                ProcessInfo processInfo = context.getNativeProcess();
                if (processInfo != null && processInfo.priorSnaphot().isRunning()) {
                    availabilityType = previousAvailabilityType;
                } else {
                    availabilityType = DOWN;
                }
            }
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(getResourceDescription() + ": exception while checking availability", e);
            }
            availabilityType = DOWN;
        }

        if (availabilityType == DOWN) {
            releaseVersion = null;
        } else if (previousAvailabilityType != UP) {
            onAvailGoesUp();
        }
        previousAvailabilityType = availabilityType;
        return availabilityType;
    }

    /**
     * this method get's called when availability was DOWN and now is UP.
     * When overriding, don't forget to call super impl.
     */
    protected void onAvailGoesUp() {
        validateServerAttributes();
        if (LOG.isDebugEnabled()) {
            LOG.debug(getResourceDescription() + " has just come UP.");
        }
    }

    private void validateServerAttributes() throws InvalidPluginConfigurationException {
        // Validate the base dir (e.g. /opt/jboss-as-7.1.1.Final/standalone).
        File runtimeBaseDir;
        File baseDir = null;
        try {
            String runtimeBaseDirString = readAttribute(getEnvironmentAddress(), getBaseDirAttributeName());
            // Canonicalize both paths before comparing them!
            runtimeBaseDir = new File(runtimeBaseDirString).getCanonicalFile();
            File baseDirTmp = serverPluginConfig.getBaseDir();
            if (baseDirTmp != null) { // may be null for manually added servers
                baseDir = baseDirTmp.getCanonicalFile();
            }
        } catch (Exception e) {
            runtimeBaseDir = null;
            baseDir = null;
            LOG.error("Failed to validate base dir for " + getResourceDescription() + ".", e);
        }
        if ((runtimeBaseDir != null) && (baseDir != null)) {
            if (!runtimeBaseDir.equals(baseDir)) {
                throw new InvalidPluginConfigurationException("The server listening on "
                    + serverPluginConfig.getHostname() + ":" + serverPluginConfig.getPort() + " has base dir ["
                    + runtimeBaseDir + "], but the base dir we expected was [" + baseDir
                    + "]. Perhaps the management hostname or port has been changed for the server with base dir ["
                    + baseDir + "].");
            }
        }

        // Validate the mode (e.g. STANDALONE or DOMAIN).
        String runtimeMode;
        try {
            runtimeMode = readAttribute("launch-type");
        } catch (Exception e) {
            runtimeMode = null;
            LOG.error("Failed to validate mode for " + getResourceDescription() + ".", e);
        }
        if (runtimeMode != null) {
            String mode = getMode().name();
            if (!runtimeMode.equals(mode)) {
                throw new InvalidPluginConfigurationException("The original mode discovered for this AS7 server was "
                    + getMode() + ", but the server is now reporting its mode is [" + runtimeMode + "].");
            }
        }

        // Validate the product type (e.g. AS or EAP).
        String expectedRuntimeProductName = pluginConfiguration.getSimpleValue("expectedRuntimeProductName");
        String runtimeProductName;
        try {
            runtimeProductName = readAttribute(getHostAddress(), "product-name");
        } catch (Exception e) {
            throw new InvalidPluginConfigurationException("Failed to validate product type for "
                + getResourceDescription(), e);
        }
        if (runtimeProductName == null || runtimeProductName.trim().isEmpty()) {
            String releaseVersionNumber;
            try {
                releaseVersionNumber = readAttribute(getHostAddress(), "release-version");
            } catch (Exception e) {
                throw new InvalidPluginConfigurationException("Failed to validate product type for "
                    + getResourceDescription(), e);
            }
            if (releaseVersionNumber.startsWith("8.")) {
                runtimeProductName = WILDFLY8.PRODUCT_NAME;
            } else {
                runtimeProductName = AS.PRODUCT_NAME;
            }
        }
        if (!runtimeProductName.equals(expectedRuntimeProductName)) {
            throw new InvalidPluginConfigurationException("The original product type discovered for this server was "
                + expectedRuntimeProductName + ", but the server is now reporting its product type is ["
                + runtimeProductName + "]");
        }
    }

    public ServerPluginConfiguration getServerPluginConfiguration() {
        return serverPluginConfig;
    }

    public StartScriptConfiguration getStartScriptConfiguration() {
        return startScriptConfig;
    }

    @Override
    public ASConnection getASConnection() {
        return connection;
    }

    // TODO: Refactor this - we should be able to mock the ResourceContext passed to start() instead.
    @Override
    public void setConnection(ASConnection connection) {
        this.connection = connection;
    }

    @NotNull
    protected abstract AS7Mode getMode();

    /**
     * Restart the server by first executing a 'shutdown' operation via the management API and then calling
     * the {@link #startServer} method to start it again.
     *
     * @param parameters Parameters to pass to the (recursive) invocation of #invokeOperation
     * @return State of execution
     * @throws Exception If anything goes wrong
     */
    protected OperationResult restartServer(Configuration parameters) throws Exception {

        OperationResult operationResult = new OperationResult();
        if (isManuallyAddedServer(operationResult, "Restarting")) {
            return operationResult;
        }

        List<String> errors = validateStartScriptPluginConfigProps();
        if (!errors.isEmpty()) {
            OperationResult result = new OperationResult();
            setErrorMessage(result, errors);
            return result;
        }

        OperationResult tmp = invokeOperation("shutdown", parameters);

        if (tmp.getErrorMessage() != null) {
            tmp.setErrorMessage("Restart failed while attempting to shut down: " + tmp.getErrorMessage());
            return tmp;
        }

        context.getAvailabilityContext().requestAvailabilityCheck();

        return startServer();
    }

    protected boolean waitUntilDown() throws InterruptedException {
        boolean notAnswering = false;
        while (!notAnswering) {
            Operation op = new ReadAttribute(new Address(), "release-version");

            try {
                Result res = getASConnection().execute(op);
                if (!res.isSuccess()) { // If op succeeds, server is not down
                    notAnswering = true;
                }
            } catch (Exception e) {
                notAnswering = true;
            }

            if (!notAnswering) {
                if (context.getComponentInvocationContext().isInterrupted()) {
                    // Operation canceled or timed out
                    throw new InterruptedException();
                }
                Thread.sleep(SECONDS.toMillis(1));
            }
        }

        // BZ 893802: wait until server (the process) is really down
        HostConfiguration hostConfig = getHostConfig();
        // commandLine instance is not important for determining whether the HostPort is local or not
        AS7CommandLine commandLine = new AS7CommandLine(new String[] { "java", "foo.Main",
            "org.jboss.as.host-controller" });
        HostPort hostPort = hostConfig.getDomainControllerHostPort(commandLine);

        if (hostPort.isLocal) {
            // lets be paranoid here
            for (ProcessInfo processInfo = context.getNativeProcess();; processInfo = context.getNativeProcess()) {
                if (processInfo == null) {
                    // Process not found, so it died, that's fine
                    break;
                }
                if (!processInfo.priorSnaphot().isRunning()) {
                    // Process info says process is no longer running, that's fine
                    break;
                }
                if (context.getComponentInvocationContext().isInterrupted()) {
                    // Operation canceled or timed out
                    throw new InterruptedException();
                }
                // Process is still running, wait a second and check again
                Thread.sleep(SECONDS.toMillis(1));
            }
        }
        return notAnswering;
    }

    /**
     * Start the server by calling the start script defined in the plugin configuration.
     *
     * @return the result of the operation
     */
    protected OperationResult startServer() throws InterruptedException {
        OperationResult operationResult = new OperationResult();
        if (isManuallyAddedServer(operationResult, "Starting")) {
            return operationResult;
        }

        List<String> errors = validateStartScriptPluginConfigProps();
        if (!errors.isEmpty()) {
            setErrorMessage(operationResult, errors);
            return operationResult;
        }

        String startScriptPrefix = startScriptConfig.getStartScriptPrefix();
        File startScriptFile = getStartScriptFile();
        ProcessExecution processExecution = ProcessExecutionUtility.createProcessExecution(startScriptPrefix,
            startScriptFile);

        List<String> arguments = processExecution.getArguments();
        if (arguments == null) {
            arguments = new ArrayList<String>();
            processExecution.setArguments(arguments);
        }

        List<String> startScriptArgs = startScriptConfig.getStartScriptArgs();
        for (String startScriptArg : startScriptArgs) {
            startScriptArg = replacePropertyPatterns(startScriptArg, pluginConfiguration);
            arguments.add(startScriptArg);
        }

        Map<String, String> startScriptEnv = startScriptConfig.getStartScriptEnv();
        for (String envVarName : startScriptEnv.keySet()) {
            String envVarValue = startScriptEnv.get(envVarName);
            envVarValue = replacePropertyPatterns(envVarValue, pluginConfiguration);
            startScriptEnv.put(envVarName, envVarValue);
        }
        processExecution.setEnvironmentVariables(startScriptEnv);

        // When running on Windows 9x, standalone.bat and domain.bat need the cwd to be the AS bin dir in order to find
        // standalone.bat.conf and domain.bat.conf respectively.
        processExecution.setWorkingDirectory(startScriptFile.getParent());
        processExecution.setCaptureOutput(true);
        processExecution.setWaitForCompletion(0);

        if (LOG.isDebugEnabled()) {
            LOG.debug("About to execute the following process: [" + processExecution + "]");
        }
        SystemInfo systemInfo = context.getSystemInformation();
        ProcessExecutionResults results = systemInfo.executeProcess(processExecution);
        logExecutionResults(results);
        if (results.getError() != null) {
            operationResult.setErrorMessage(results.getError().getMessage());
        } else if (results.getExitCode() != null && results.getExitCode() != 0) {
            operationResult.setErrorMessage("Start failed with error code " + results.getExitCode() + ":\n"
                + results.getCapturedOutput());
        } else {
            // Try to connect to the server - ping once per second, timing out after 20s.
            boolean up = waitForServerToStart();
            if (up) {
                operationResult.setSimpleResult("Success");
            } else {
                operationResult.setErrorMessage("Was not able to start the server");
            }
        }
        context.getAvailabilityContext().requestAvailabilityCheck();

        return operationResult;
    }

    /**
     * runs jboss-cli executable and returns its output
     * @param parameters input configuration (either commands or file sipmle-property is expected)
     * @return the result of the operation
     * @throws InterruptedException
     */
    protected OperationResult runCliCommand(Configuration parameters) throws InterruptedException {
        OperationResult result = new OperationResult();

        if (isManuallyAddedServer(result, "Executing jboss-cli")) {
            return result;
        }

        long waitTime;
        PropertySimple waitTimeProp = parameters.getSimple("waitTime");
        Long waitTimePropLongValue = waitTimeProp == null ? null : waitTimeProp.getLongValue();
        if (waitTimePropLongValue == null) {
            waitTime = HOURS.toMillis(1);
        } else {
            waitTime = waitTimePropLongValue * 1000L;
            if (waitTime <= 0) {
                result.setErrorMessage("waitTime parameter must be positive integer");
                return result;
            }
        }

        boolean killOnTimeout = false;
        if (Boolean.parseBoolean(parameters.getSimpleValue("killOnTimeout", "false"))) {
            killOnTimeout = true;
        }

        CliExecutor cliExecutor = new CliExecutor(getMode(), serverPluginConfig, startScriptConfig,
            context.getSystemInformation());

        ProcessExecutionResults results;
        String commands = parameters.getSimpleValue("commands");
        if (commands != null) {
            results = cliExecutor.executeCliCommand(commands, waitTime, killOnTimeout);
        } else {
            File script = new File(parameters.getSimpleValue("file"));
            if (!script.isAbsolute()) {
                script = new File(serverPluginConfig.getHomeDir(), parameters.getSimpleValue("file"));
            }
            results = cliExecutor.executeCliScript(script.getAbsolutePath(), waitTime, killOnTimeout);
        }

        if (results.getError() != null) {
            result.setErrorMessage(results.getError().getMessage());
        } else if (results.getExitCode() == null) {
            result.setErrorMessage("jboss-cli execution timed out, captured output:\n" + results.getCapturedOutput());
            return result;
        } else if (results.getExitCode() != 0) {
            result.setErrorMessage("jboss-cli execution failed with error code " + results.getExitCode() + ":\n"
                + results.getCapturedOutput());
        } else {
            result.setSimpleResult(results.getCapturedOutput());
        }
        if (result.getErrorMessage() == null) {
            PropertySimple avail = parameters.getSimple("triggerAvailability");
            if (avail != null && avail.getBooleanValue()) {
                context.getAvailabilityContext().requestAvailabilityCheck();
            }
            PropertySimple disc = parameters.getSimple("triggerDiscovery");
            if (disc != null && disc.getBooleanValue()) {
                context.getInventoryContext().requestDeferredChildResourcesDiscovery();
            }
        }
        return result;
    }

    public boolean isManuallyAddedServer() {
        if (pluginConfiguration.get("manuallyAdded") != null) {
            return true;
        }
        return false;
    }

    private boolean isManuallyAddedServer(OperationResult operationResult, String operation) {
        if (isManuallyAddedServer()) {
            operationResult.setErrorMessage(operation + " is not enabled for manually added servers");
            return true;
        }
        return false;
    }

    private void setErrorMessage(OperationResult operationResult, List<String> errors) {
        StringBuilder buffer = new StringBuilder("This Resource's connection properties contain errors: ");
        for (int i = 0, errorsSize = errors.size(); i < errorsSize; i++) {
            if (i != 0) {
                buffer.append(", ");
            }
            String error = errors.get(i);
            buffer.append('[').append(error).append(']');
        }
        operationResult.setErrorMessage(buffer.toString());
    }

    private List<String> validateStartScriptPluginConfigProps() {
        List<String> errors = new ArrayList<String>();

        File startScriptFile = getStartScriptFile();

        if (!startScriptFile.exists()) {
            errors.add("Start script '" + startScriptFile + "' does not exist.");
        } else {
            if (!startScriptFile.isFile()) {
                errors.add("Start script '" + startScriptFile + "' is not a regular file.");
            } else {
                if (!startScriptFile.canRead()) {
                    errors.add("Start script '" + startScriptFile + "' is not readable.");
                }
                if (!startScriptFile.canExecute()) {
                    errors.add("Start script '" + startScriptFile + "' is not executable.");
                }
            }
        }

        return errors;
    }

    private File getStartScriptFile() {
        File startScriptFile = startScriptConfig.getStartScript();
        File homeDir = serverPluginConfig.getHomeDir();
        if (startScriptFile != null) {
            if (!startScriptFile.isAbsolute()) {
                startScriptFile = new File(homeDir, startScriptFile.getPath());
            }
        } else {
            // Use the default start script.
            String startScriptFileName = getMode().getStartScriptFileName();
            File binDir = new File(homeDir, "bin");
            startScriptFile = new File(binDir, startScriptFileName);
        }
        return startScriptFile;
    }

    private boolean waitForServerToStart() throws InterruptedException {
        boolean up = false;
        while (!up) {
            Operation op = new ReadAttribute(new Address(), "release-version");
            try {
                Result res = getASConnection().execute(op);
                if (res.isSuccess()) { // If op succeeds, server is not down
                    up = true;
                }
            } catch (Exception e) {
                //do absolutely nothing
                //if an exception is thrown that means the server is still down, so consider this
                //a single failed attempt, equivalent to res.isSuccess == false
            }

            if (!up) {
                if (context.getComponentInvocationContext().isInterrupted()) {
                    // Operation canceled or timed out
                    throw new InterruptedException();
                }
                Thread.sleep(SECONDS.toMillis(1));
            }
        }
        return up;
    }

    /**
     * Do some post processing of the Result - especially the 'shutdown' operation needs a special
     * treatment.
     * @param name Name of the operation
     * @param res Result of the operation vs. AS7
     * @return OperationResult filled in from values of res
     */
    protected OperationResult postProcessResult(String name, Result res) {
        OperationResult operationResult = new OperationResult();
        if (res == null) {
            operationResult.setErrorMessage("No result received from server");
            return operationResult;
        }

        if (name.equals("shutdown") || name.equals("restart") || name.equals("reload")) {
            /*
             * Shutdown, restart and reload need a special treatment, because after sending the operation, event if
             * it succeeds, the server connection is sometimes closed and we can't read from it.
             */
            if (!res.isSuccess()) {
                if (StringUtil.isNotBlank(res.getFailureDescription())
                    && res.getFailureDescription().startsWith(ASConnection.FAILURE_NO_RESPONSE)) {
                    operationResult.setSimpleResult("Success");
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Got no response for operation '" + name + "'. "
                            + "This is considered ok, as the remote server sometimes closes the communications "
                            + "channel before sending a reply");
                    }
                } else {
                    operationResult.setErrorMessage(res.getFailureDescription());
                }
            } else {
                operationResult.setSimpleResult("Success");
            }
        } else {
            if (res.isSuccess()) {
                if (res.getResult() != null)
                    operationResult.setSimpleResult(res.getResult().toString());
                else
                    operationResult.setSimpleResult("-None provided by server-");
            } else
                operationResult.setErrorMessage(res.getFailureDescription());
        }
        return operationResult;
    }

    protected OperationResult installManagementUser(Configuration parameters, Configuration pluginConfig) {
        String user = parameters.getSimpleValue("user", "");
        String password = parameters.getSimpleValue("password", "");

        OperationResult result = new OperationResult();

        PropertySimple remoteProp = pluginConfig.getSimple("manuallyAdded");
        if (remoteProp != null && remoteProp.getBooleanValue() != null && remoteProp.getBooleanValue()) {
            result
                .setErrorMessage("This is a manually added server. This operation can not be used to install a management user. Use the server's 'bin/add-user.sh'");
            return result;
        }

        if (user.isEmpty() || password.isEmpty()) {
            result.setErrorMessage("User and Password must not be empty");
            return result;
        }

        File baseDir = serverPluginConfig.getBaseDir();
        if (baseDir == null) {
            result.setErrorMessage("'" + ServerPluginConfiguration.Property.BASE_DIR
                + "' plugin config prop is not set.");
            return result;
        }

        HostConfiguration hostConfig = getHostConfig();
        String realm = pluginConfig.getSimpleValue("realm", "ManagementRealm");
        File propertiesFile = hostConfig.getSecurityPropertyFile(baseDir, getMode(), realm);
        if (!propertiesFile.canWrite()) {
            result.setErrorMessage("Management users properties file [" + propertiesFile + "] is not writable.");
            return result;
        }

        String encryptedPassword;
        try {
            UsernamePasswordHashUtil hashUtil = new UsernamePasswordHashUtil();
            encryptedPassword = hashUtil.generateHashedHexURP(user, realm, password.toCharArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt password.", e);
        }

        boolean userAlreadyExisted;
        try {
            PropertiesFileUpdate propsFileUpdate = new PropertiesFileUpdate(propertiesFile.getPath());
            userAlreadyExisted = propsFileUpdate.update(user, encryptedPassword);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update management users properties file [" + propertiesFile + "].", e);
        }

        String verb = (userAlreadyExisted) ? "updated" : "added";
        result.setSimpleResult("Management user [" + user + "] " + verb + ".");
        LOG.info("Management user [" + user + "] " + verb + " for " + context.getResourceType().getName()
            + " server with key [" + context.getResourceKey() + "].");

        context.getAvailabilityContext().requestAvailabilityCheck();
        context.getInventoryContext().requestDeferredChildResourcesDiscovery();

        return result;
    }

    /**
     * Requests a deferred child resource discovery for sub-resources of this server.
     */
    public void requestDeferredChildResourcesDiscovery() {
        this.context.getInventoryContext().requestDeferredChildResourcesDiscovery();
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) throws Exception {
        Set<MeasurementScheduleRequest> skmRequests = new HashSet<MeasurementScheduleRequest>(requests.size());
        Set<MeasurementScheduleRequest> leftovers = new HashSet<MeasurementScheduleRequest>(requests.size());
        String tempDirAttributeName = getTempDirAttributeName();
        for (MeasurementScheduleRequest request : requests) {
            String requestName = request.getName();
            if (requestName.equals("startTime")) {
                collectStartTimeTrait(report, request);
            } else {
                if (tempDirAttributeName != null && requestName.equals(tempDirAttributeName)) {
                    collectEnvironmentTrait(report, request);
                } else if (requestName.startsWith("_skm:")) { // handled below
                    skmRequests.add(request);
                } else {
                    leftovers.add(request); // handled below
                }
            }
        }

        // Now handle the server kind traits.
        if (skmRequests.size() > 0) {
            collectServerKindTraits(report, skmRequests);
        }

        // Finally let our superclass handle the leftovers.
        super.getValues(report, leftovers);
    }

    /**
     * The release version as returned by the "release-version" attribute of the root node in the management model.
     */
    public String getReleaseVersion() {
        if (releaseVersion == null) {
            releaseVersion = (String) getASConnection().execute(new ReadAttribute(new Address(), "release-version"))
                .getResult();
        }

        return releaseVersion;
    }

    private void collectStartTimeTrait(MeasurementReport report, MeasurementScheduleRequest request) {
        Address address = new Address(getHostAddress());
        address.add("core-service", "platform-mbean");
        address.add("type", "runtime");
        Long startTime;
        try {
            startTime = readAttribute(address, "start-time", Long.class);
        } catch (Exception e) {
            startTime = null;
        }

        if (startTime != null) {
            MeasurementDataTrait data = new MeasurementDataTrait(request, new Date(startTime).toString());
            report.addData(data);
        }
    }

    protected BundleHandoverResponse handleExecuteScript(BundleHandoverRequest handoverRequest) throws IOException {
        Map<String, String> params = handoverRequest.getParams();

        long waitTime;
        String waitTimeParam = params.get("waitTime");
        if (waitTimeParam != null) {
            try {
                waitTime = Long.parseLong(waitTimeParam);
                if (waitTime <= 0) {
                    return BundleHandoverResponse.failure(INVALID_PARAMETER, "waitTime must greater than 0");
                }
            } catch (NumberFormatException e) {
                return BundleHandoverResponse.failure(INVALID_PARAMETER, "waitTime is not a number");
            }
        } else {
            waitTime = HOURS.toMillis(1);
        }

        boolean killOnTimeout = Boolean.parseBoolean(params.get("killOnTimeout"));

        File scriptFile = null;
        try {
            scriptFile = File.createTempFile(handoverRequest.getFilename(), ".tmp", context.getTemporaryDirectory());
            FileUtil.writeFile(handoverRequest.getContent(), scriptFile);

            CliExecutor cliExecutor = new CliExecutor(getMode(), getServerPluginConfiguration(),
                getStartScriptConfiguration(), context.getSystemInformation());
            ProcessExecutionResults results = cliExecutor.executeCliScript(scriptFile.getAbsolutePath(), waitTime,
                killOnTimeout);

            Throwable error = results.getError();
            if (error != null) {
                return BundleHandoverResponse.failure(EXECUTION, error.getMessage(), error);
            }
            Integer exitCode = results.getExitCode();
            if (exitCode == null) {
                return BundleHandoverResponse.failure(EXECUTION, "Timeout waiting for completion of the CLI process");
            }
            if (exitCode != 0) {
                return BundleHandoverResponse.failure(EXECUTION, "CLI process exit code is " + exitCode);
            }
            return BundleHandoverResponse.success();

        } finally {
            if (scriptFile != null) {
                scriptFile.delete();
            }
        }
    }

    @NotNull
    protected abstract Address getEnvironmentAddress();

    @NotNull
    protected abstract Address getHostAddress();

    @NotNull
    protected abstract String getBaseDirAttributeName();

    /**
     * Default implentation. Override in concrete subclasses and return the the temporary directory attribute name,
     * found on the node at {@link #getEnvironmentAddress()}.
     *
     * @return the temp dir attribute name or null if no such attribute exists
     */
    protected String getTempDirAttributeName() {
        return null;
    }

    protected void collectConfigTrait(MeasurementReport report, MeasurementScheduleRequest request) {
        String value = readEnvironmentAttribute(request);
        if (value != null) {
            MeasurementDataTrait data = new MeasurementDataTrait(request, new File(value).getName());
            report.addData(data);
        }
    }

    protected void collectEnvironmentTrait(MeasurementReport report, MeasurementScheduleRequest request) {
        String value = readEnvironmentAttribute(request);
        if (value != null) {
            MeasurementDataTrait data = new MeasurementDataTrait(request, value);
            report.addData(data);
        }
    }

    private String readEnvironmentAttribute(MeasurementScheduleRequest request) {
        try {
            return readAttribute(getEnvironmentAddress(), request.getName(), String.class);
        } catch (Exception e) {
            LOG.error("Failed to read attribute [" + request.getName() + "]: " + e, e);
            return null;
        }
    }

    private synchronized void setASHostName(String aSHostName) {
        this.aSHostName = aSHostName;
    }

    /**
     * gets AS domain host name (defult is master for HC) null for standalone;
     * @return AS domain host name
     */
    protected synchronized String getASHostName() {
        return aSHostName;
    }

    /**
     * Reads <host name= attribute from host.xml file.
     *
     * @return name attribute from host.xml file
     */
    protected String findASDomainHostName() {
        File hostXmlFile = getServerPluginConfiguration().getHostConfigFile();
        if (hostXmlFile == null) {
            return null;
        }
        String hostName = null;
        try {
            DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
            InputStream is = new FileInputStream(hostXmlFile);
            try {
                Document document = builder.parse(is);
                hostName = document.getDocumentElement().getAttribute("name");
            } finally {
                is.close();
            }
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        if (hostName == null)
            hostName = "local"; // Fallback to the installation default
        return hostName;
    }

    private HostConfiguration getHostConfig() {
        File configFile;
        HostConfiguration hostConfig;
        try {
            String config = readAttribute(getEnvironmentAddress(), getMode().getHostConfigAttributeName());
            configFile = new File(config);
        } catch (Exception e) {
            // This probably means the server is not running and/or authentication is not set up. Fallback to the
            // host config file set in the plugin config during discovery.
            // TODO (ips, 05/05/12): This is not ideal, because the user could have restarted the server with a
            //                       different config file, since the time it was imported into inventory. The better
            //                       thing to do here is to find the current server process and parse its command line
            //                       to find the current config file name.
            configFile = serverPluginConfig.getHostConfigFile();
            if (configFile == null) {
                throw new RuntimeException("Failed to determine config file path.", e);
            }
        }
        try {
            hostConfig = new HostConfiguration(configFile);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse configuration file [" + configFile + "].", e);
        }
        return hostConfig;
    }

    private void collectServerKindTraits(MeasurementReport report, Set<MeasurementScheduleRequest> skmRequests) {
        Address address = new Address();

        ReadResource op = new ReadResource(address);
        op.includeRuntime(true);

        ComplexResult res = getASConnection().executeComplex(op);
        if (res.isSuccess()) {
            Map<String, Object> props = res.getResult();

            for (MeasurementScheduleRequest request : skmRequests) {
                String requestName = request.getName();
                String realName = requestName.substring(requestName.indexOf(':') + 1);
                String val = null;
                if (props.containsKey(realName)) {
                    val = getStringValue(props.get(realName));
                }

                if ("null".equals(val)) {
                    if (realName.equals("product-name"))
                        val = "JBoss AS";
                    else if (realName.equals("product-version"))
                        val = getStringValue(props.get("release-version"));
                    else if (LOG.isDebugEnabled()) {
                        LOG.debug("Value for " + realName + " was 'null' and no replacement found");
                    }
                }
                MeasurementDataTrait data = new MeasurementDataTrait(request, val);
                report.addData(data);
            }

        } else if (LOG.isDebugEnabled()) {
            LOG.debug("getSKMRequests failed: " + res.getFailureDescription());
        }
    }
}
