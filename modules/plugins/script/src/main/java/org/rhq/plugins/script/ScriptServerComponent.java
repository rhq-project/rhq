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

package org.rhq.plugins.script;

import static java.lang.Boolean.TRUE;
import static org.rhq.core.util.StringUtil.isBlank;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.util.exception.ThrowableUtil;

/**
 * Represents a managed resource whose management interface is a command line executable or script,
 * sometimes referred to as the "CLI" (command line interface).
 *
 * @author John Mazzitelli
 */
public class ScriptServerComponent implements ResourceComponent, MeasurementFacet, OperationFacet {
    private static final Log LOG = LogFactory.getLog(ScriptServerComponent.class);

    private static final long DEFAULT_MAX_WAIT_TIME = 3600000L;

    protected static final String PLUGINCONFIG_EXECUTABLE = "executable";
    protected static final String PLUGINCONFIG_WORKINGDIR = "workingDirectory";
    protected static final String PLUGINCONFIG_ENVVARS = "environmentVariables";
    protected static final String PLUGINCONFIG_ENVVAR_NAME = "name";
    protected static final String PLUGINCONFIG_ENVVAR_VALUE = "value";
    protected static final String PLUGINCONFIG_AVAIL_EXECUTE_CHECK = "availabilityExecuteCheck";
    protected static final String PLUGINCONFIG_AVAIL_EXITCODE_REGEX = "availabilityExitCodeRegex";
    protected static final String PLUGINCONFIG_AVAIL_OUTPUT_REGEX = "availabilityOutputRegex";
    protected static final String PLUGINCONFIG_AVAIL_ARGS = "availabilityArguments";
    protected static final String PLUGINCONFIG_VERSION_ARGS = "versionArguments";
    protected static final String PLUGINCONFIG_VERSION_REGEX = "versionRegex";
    protected static final String PLUGINCONFIG_FIXED_VERSION = "fixedVersion";
    protected static final String PLUGINCONFIG_DESC_ARGS = "descriptionArguments";
    protected static final String PLUGINCONFIG_DESC_REGEX = "descriptionRegex";
    protected static final String PLUGINCONFIG_FIXED_DESC = "fixedDescription";
    protected static final String PLUGINCONFIG_QUOTING_ENABLED = "quotingEnabled";
    protected static final String PLUGINCONFIG_ESCAPE_CHARACTER = "escapeCharacter";

    protected static final String OPERATION_PARAM_ARGUMENTS = "arguments";
    protected static final String OPERATION_PARAM_WAIT_TIME = "waitTime";
    protected static final String OPERATION_PARAM_CAPTURE_OUTPUT = "captureOutput";
    protected static final String OPERATION_PARAM_KILL_ON_TIMEOUT = "killOnTimeout";
    protected static final String OPERATION_RESULT_EXITCODE = "exitCode";
    protected static final String OPERATION_RESULT_OUTPUT = "output";

    protected static final String METRIC_PROPERTY_ARGUMENTS = "arguments";
    protected static final String METRIC_PROPERTY_REGEX = "regex";
    protected static final String METRIC_PROPERTY_EXITCODE = "exitcode";

    protected static final char DISABLING_ESCAPE_CHARACTER = '\u0000';

    private char escapeChar = DISABLING_ESCAPE_CHARACTER;
    private ResourceContext resourceContext;

    @Override
    public void start(ResourceContext context) {
        resourceContext = context;
        escapeChar = getConfiguredEscapeCharacter(resourceContext.getPluginConfiguration());
        if (isBlank(resourceContext.getPluginConfiguration().getSimpleValue(PLUGINCONFIG_QUOTING_ENABLED))) {
            LOG.warn(resourceContext.getResourceDetails() + ": the attribute enabling argument quoting is not set"
                + " in plugin config. Defaulting to enabled.");
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Script Server started: " + context.getPluginConfiguration());
        }
    }

    @Override
    public void stop() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Script Server stopped: " + this.resourceContext.getPluginConfiguration());
        }
    }

    @Override
    public AvailabilityType getAvailability() {
        boolean result = checkAvailability();
        return result ? AvailabilityType.UP : AvailabilityType.DOWN;
    }

    /**
     * Executes the CLI and based on the measurement property, collects the appropriate measurement value.
     * This supports measurement data and traits.
     * A metric property can be a string that is assumed the arguments to pass to the CLI. However, if
     * the property is in the form "{arguments}|regex", the arguments are passed to the CLI and the metric
     * value is taken from the regex match. Examples of metric properties:
     *  
     * <ul>
     * <li>"-a --arg2=123" - passes that string as the arguments list, the metric value is the output of the CLI</li> 
     * <li>"{}|exitcode" - no arguments are passed to the CLI and the metric value is the exit code of the CLI</li>
     * <li>"{-a}|[lL]abel(\p{Digit}+)[\r\n]*" - "-a" is the argument passed to the CLI and the metric value is the digits following the label of the output</li>
     * <li>"{}|[ABC]+" - no arguments are passed and the value is the output of the CLI assuming it matches the regex</li>
     * <li>"{--foobar}|foobar (.*) blah" - passes "--foobar" as the argument and the metric value is the string that matches the regex group</li>
     * </ul>
     * 
     * @see MeasurementFacet#getValues(MeasurementReport, Set)
     */
    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) {
        Map<String, ProcessExecutionResults> exeResultsCache = new HashMap<String, ProcessExecutionResults>();

        for (MeasurementScheduleRequest request : requests) {
            String metricPropertyName = request.getName();
            boolean dataMustBeNumeric;

            if (request.getDataType() == DataType.MEASUREMENT) {
                dataMustBeNumeric = true;
            } else if (request.getDataType() == DataType.TRAIT) {
                dataMustBeNumeric = false;
            } else {
                LOG.error("Plugin does not support metric [" + metricPropertyName + "] of type ["
                    + request.getDataType() + "]");
                continue;
            }

            try {

                // determine how to execute the CLI for this metric
                Map<String, String> argsRegex = parseMetricProperty(metricPropertyName);
                Object dataValue;

                if (argsRegex == null) {
                    continue; // this metric's property was invalid, skip this one and move on to the next
                }

                String arguments = argsRegex.get(METRIC_PROPERTY_ARGUMENTS);
                String regex = argsRegex.get(METRIC_PROPERTY_REGEX);
                boolean valueIsExitCode = METRIC_PROPERTY_EXITCODE.equals(regex);

                // if we already executed it with the same arguments, don't bother doing it again
                ProcessExecutionResults exeResults = exeResultsCache.get((arguments == null) ? "" : arguments);
                if (exeResults == null) {
                    boolean captureOutput = !valueIsExitCode; // don't need output if we need to just check exit code
                    exeResults = executeExecutable(arguments, DEFAULT_MAX_WAIT_TIME, captureOutput);
                    exeResultsCache.put((arguments == null) ? "" : arguments, exeResults);
                }

                // don't report a metric value if the CLI failed to execute
                if (exeResults.getError() != null) {
                    LOG.error("Cannot collect CLI metric [" + metricPropertyName + "]. Cause: "
                        + ThrowableUtil.getAllMessages(exeResults.getError()));
                    continue;
                }

                // set dataValue to the appropriate value based on how the metric property defined it
                if (valueIsExitCode) {
                    dataValue = exeResults.getExitCode();
                    if (dataValue == null) {
                        LOG.error("Could not determine exit code for metric property [" + metricPropertyName
                            + "] - metric will not be collected");
                        continue;
                    }
                } else if (regex != null) {
                    String output = exeResults.getCapturedOutput();
                    if (output == null) {
                        LOG.error("Could not get output for metric property [" + metricPropertyName
                            + "] -- metric will not be collected");
                        continue;
                    } else {
                        output = output.trim();
                    }

                    Pattern pattern = Pattern.compile(regex);
                    Matcher match = pattern.matcher(output);
                    if (match.find()) {
                        if (match.groupCount() > 0) {
                            dataValue = match.group(1);
                        } else {
                            dataValue = output;
                        }
                    } else {
                        LOG.error("Output did not match metric property [" + metricPropertyName
                            + "] - metric will not be collected: " + truncateString(output));
                        continue;
                    }
                } else {
                    dataValue = exeResults.getCapturedOutput();
                    if (dataValue == null) {
                        LOG.error("Could not get output for metric property [" + metricPropertyName
                            + "] - metric will not be collected");
                        continue;
                    }
                }

                // add the metric value to the measurement report
                if (dataMustBeNumeric) {
                    Double numeric = Double.parseDouble(dataValue.toString().trim());
                    report.addData(new MeasurementDataNumeric(request, numeric));
                } else {
                    report.addData(new MeasurementDataTrait(request, dataValue.toString().trim()));
                }
            } catch (Exception e) {
                LOG.error("Failed to obtain measurement [" + metricPropertyName + "]. Cause: " + e);
            }
        }
    }

    /**
     * Given a metric property name, this parses it into its different pieces and returns a
     * map of the different tokens. The format of the metric property one of the following:
     * <pre>
     * arguments
     * {arguments}|regex
     * {arguments}|exitcode
     * </pre>
     *
     * where "arguments" is the empty or non-empty string of the arguments to pass to the CLI executable,
     * regex is a empty or non-empty regular expresssion string to match the output (if there is a matching
     * group in the regex, its value will be used as the metric value, not the full output of the executable),
     * exitcode is the literal string "exitcode" to indicate that the exit code value is to be used as the metric value.
     *
     * @param metricPropertyName the name of the property in the metric descriptor
     * @return map containing the pieces that have been parsed out - the keys are
     *             either {@link #METRIC_PROPERTY_ARGUMENTS} or {@link #METRIC_PROPERTY_REGEX}.
     *             The map will be <code>null</code> if the property name is invalid for some reason.
     *             A map entry will not exist if it was not specified in the property name.
     */
    protected Map<String, String> parseMetricProperty(String metricPropertyName) {
        HashMap<String, String> map = new HashMap<String, String>();

        if (metricPropertyName != null && metricPropertyName.length() > 0) {
            if (!metricPropertyName.startsWith("{")) {
                map.put(METRIC_PROPERTY_ARGUMENTS, metricPropertyName); // the property is entirely the arguments 
            } else {
                String[] argsRegex = metricPropertyName.substring(1).split("\\}\\|", 2);
                if (argsRegex.length != 2) {
                    LOG.error("Invalid metric property [" + metricPropertyName + "] - metric will not be collected");
                    return null;
                }
                if (!isValidRegularExpression(argsRegex[1])) {
                    LOG.error("Invalid regex [" + argsRegex[1] + "] for metric property [" + metricPropertyName
                        + "] - metric will not be collected");
                    return null;
                }
                if (argsRegex[0].length() > 0) {
                    map.put(METRIC_PROPERTY_ARGUMENTS, argsRegex[0]);
                }
                if (argsRegex[1].length() > 0) {
                    map.put(METRIC_PROPERTY_REGEX, argsRegex[1]);
                }
            }
        }

        return map;
    }

    /**
     * Invokes the CLI executable. User can tell us what arguments to pass to the executable.
     * The result includes the output of the process as well as the exit code.
     * 
     * @see OperationFacet#invokeOperation(String, Configuration)
     */
    @Override
    public OperationResult invokeOperation(String name, Configuration configuration) throws Exception {

        OperationResult result = new OperationResult();

        String arguments = configuration.getSimpleValue(OPERATION_PARAM_ARGUMENTS, null);
        String waitTimeStr = configuration.getSimpleValue(OPERATION_PARAM_WAIT_TIME, null);
        String captureOutputStr = configuration.getSimpleValue(OPERATION_PARAM_CAPTURE_OUTPUT, null);
        String killOnTimeoutStr = configuration.getSimpleValue(OPERATION_PARAM_KILL_ON_TIMEOUT, null);

        long waitTime;
        boolean captureOutput;
        boolean killOnTimeout;

        if (waitTimeStr != null) {
            try {
                waitTime = Long.parseLong(waitTimeStr);
                waitTime *= 1000L; // the parameter is specified in seconds, but we need it in milliseconds
            } catch (NumberFormatException e) {
                throw new NumberFormatException("Wait time parameter value is invalid: " + waitTimeStr);
            }
        } else {
            waitTime = DEFAULT_MAX_WAIT_TIME;
        }

        captureOutput = captureOutputStr == null || Boolean.parseBoolean(captureOutputStr);
        killOnTimeout = killOnTimeoutStr == null || Boolean.parseBoolean(killOnTimeoutStr);

        ProcessExecutionResults exeResults = executeExecutable(arguments, waitTime, captureOutput, killOnTimeout);
        Integer exitcode = exeResults.getExitCode();
        String output = exeResults.getCapturedOutput();
        Throwable error = exeResults.getError();

        if (error != null) {
            result.setErrorMessage(ThrowableUtil.getAllMessages(error));
        }

        Configuration resultsConfig = result.getComplexResults();
        if (exitcode != null) {
            resultsConfig.put(new PropertySimple(OPERATION_RESULT_EXITCODE, exitcode));
        }
        if (output != null) {
            resultsConfig.put(new PropertySimple(OPERATION_RESULT_OUTPUT, output.trim()));
        }

        return result;
    }

    protected ResourceContext getResourcContext() {
        return this.resourceContext;
    }

    /**
     * Executes the CLI executable with the given arguments.
     * 
     * Same as {@link #executeExecutable(String, long, boolean, boolean)} with 'killOnTimeout' being true.
     *
     * @return the results of the execution
     *
     * @throws InvalidPluginConfigurationException
     */
    protected ProcessExecutionResults executeExecutable(String args, long wait, boolean captureOutput) {
        return executeExecutable(args, wait, captureOutput, true);
    }

    /**
     * Executes the CLI executable with the given arguments.
     * 
     * @param args the arguments to send to the executable (may be <code>null</code>)
     * @param wait the maximum time in milliseconds to wait for the process to execute; 0 means do not wait 
     * @param captureOutput if <code>true</code>, the executables output will be captured and returned
     * @param killOnTimeout if <code>true</code> and if 'wait' is greater than 0, the process will be killed if it times out
     *
     * @return the results of the execution
     *
     * @throws InvalidPluginConfigurationException
     */
    protected ProcessExecutionResults executeExecutable(String args, long wait, boolean captureOutput,
        boolean killOnTimeout) throws InvalidPluginConfigurationException {

        SystemInfo sysInfo = this.resourceContext.getSystemInformation();
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        ProcessExecutionResults results = executeExecutable(sysInfo, pluginConfig, args, wait, captureOutput,
            killOnTimeout, escapeChar);

        if (LOG.isDebugEnabled()) {
            logDebug("CLI results: exitcode=[" + results.getExitCode() + "]; error=[" + results.getError()
                + "]; output=" + truncateString(results.getCapturedOutput()));
        }

        return results;
    }

    protected static char getConfiguredEscapeCharacter(Configuration pluginConfiguration) {
        char escapeChar = DISABLING_ESCAPE_CHARACTER;
        String attributeString = pluginConfiguration.getSimpleValue(PLUGINCONFIG_QUOTING_ENABLED);
        if (isBlank(attributeString) || TRUE.toString().equalsIgnoreCase(attributeString)) {
            String ec = pluginConfiguration.getSimpleValue(PLUGINCONFIG_ESCAPE_CHARACTER, "\\");
            escapeChar = ec.charAt(0);
        }
        return escapeChar;
    }

    // This is protected static so the discovery component can use it.
    protected static ProcessExecutionResults executeExecutable(SystemInfo sysInfo, Configuration pluginConfig,
        String args, long wait, boolean captureOutput, char escapeChar) throws InvalidPluginConfigurationException {

        return executeExecutable(sysInfo, pluginConfig, args, wait, captureOutput, true, escapeChar);
    }

    private static ProcessExecutionResults executeExecutable(SystemInfo sysInfo, Configuration pluginConfig,
        String args, long wait, boolean captureOutput, boolean killOnTimeout, char escapeChar)
        throws InvalidPluginConfigurationException {

        ProcessExecution processExecution = getProcessExecutionInfo(pluginConfig);
        if (args != null) {
            if (isQuotingEnabled(escapeChar)) {
                processExecution.setArguments(ScriptArgumentParser.parse(args, escapeChar));
            } else {
                processExecution.setArguments(args.split(" "));
            }
        }
        processExecution.setCaptureOutput(captureOutput);
        processExecution.setWaitForCompletion(wait);
        processExecution.setKillOnTimeout(killOnTimeout);

        ProcessExecutionResults results = sysInfo.executeProcess(processExecution);

        return results;
    }

    private boolean checkAvailability() throws InvalidPluginConfigurationException {

        String executable;
        boolean availExecuteCheck = false;
        String availArgs = null;
        String availExitCodeRegex = null;
        Pattern availOutputRegex = null;

        // determine how we are to consider the CLI available by looking at the plugin configuration
        try {
            Configuration pc = this.resourceContext.getPluginConfiguration();

            executable = pc.getSimpleValue(PLUGINCONFIG_EXECUTABLE, null);
            if (executable == null) {
                throw new Exception("Missing executable in plugin configuraton");
            }

            PropertySimple availExecuteCheckProp = pc.getSimple(PLUGINCONFIG_AVAIL_EXECUTE_CHECK);
            PropertySimple availArgsProp = pc.getSimple(PLUGINCONFIG_AVAIL_ARGS);
            PropertySimple availExitCodeRegexProp = pc.getSimple(PLUGINCONFIG_AVAIL_EXITCODE_REGEX);
            PropertySimple availOutputRegexProp = pc.getSimple(PLUGINCONFIG_AVAIL_OUTPUT_REGEX);

            if (availExecuteCheckProp != null && availExecuteCheckProp.getBooleanValue() != null) {
                availExecuteCheck = availExecuteCheckProp.getBooleanValue().booleanValue();
            }

            if (availArgsProp != null && availArgsProp.getStringValue() != null) {
                availArgs = availArgsProp.getStringValue();
            }

            if (availExitCodeRegexProp != null && availExitCodeRegexProp.getStringValue() != null) {
                availExitCodeRegex = availExitCodeRegexProp.getStringValue();
            }

            if (availOutputRegexProp != null && availOutputRegexProp.getStringValue() != null) {
                availOutputRegex = Pattern.compile(availOutputRegexProp.getStringValue());
            }
        } catch (Exception e) {
            throw new InvalidPluginConfigurationException("Cannot get avail plugin config. Cause: " + e);
        }

        // first, make sure the executable actually exists
        File executableFile = new File(executable);
        if (!executableFile.exists()) {
            if (LOG.isDebugEnabled()) {
                logDebug("The executable [" + executable + "] does not exist - resource is considered DOWN");
            }
            return false;
        }

        // if we need to check the exit code or output, execute the CLI now
        if (availExecuteCheck || availExitCodeRegex != null || availOutputRegex != null) {
            ProcessExecutionResults results = executeExecutable(availArgs, 3000L, (availOutputRegex != null));

            // if we get some error while trying to run the executable, immediately consider the resource down
            if (results.getError() != null) {
                if (LOG.isDebugEnabled()) {
                    logDebug("CLI execution encountered an error, resource is considered DOWN: "
                        + ThrowableUtil.getAllMessages(results.getError()));
                }
                return false;
            }

            // if the exit code is used to determine availability, check it now
            if (availExitCodeRegex != null) {
                if (results.getExitCode() == null) {
                    if (LOG.isDebugEnabled()) {
                        logDebug("Cannot get exit code, resource is considered DOWN");
                    }
                    return false;
                }

                boolean exitcodeMatches = results.getExitCode().toString().matches(availExitCodeRegex);
                if (!exitcodeMatches) {
                    if (LOG.isDebugEnabled()) {
                        logDebug("CLI exit code=[" + results.getExitCode() + "] != [" + availExitCodeRegex + "]. DOWN");
                    }
                    return false;
                }
            }

            // if the output is used to determine availability, check it now
            if (availOutputRegex != null) {
                String output = results.getCapturedOutput();
                if (output == null) {
                    output = "";
                } else {
                    output = output.trim();
                }

                boolean outputMatches = availOutputRegex.matcher(output).find();
                if (!outputMatches) {
                    if (LOG.isDebugEnabled()) {
                        logDebug("CLI output [" + truncateString(output) + "] did not match regex [" + availOutputRegex
                            + "], resource is considered DOWN");
                    }
                    return false;
                }
            }
        }

        // everything passes, resource is UP
        return true;
    }

    /**
     * Truncate a string so it is short, usually for display or logging purposes.
     * 
     * @param output the output to trim
     * @return the trimmed output
     */
    private String truncateString(String output) {
        String outputToLog = output;
        if (outputToLog != null && outputToLog.length() > 100) {
            outputToLog = outputToLog.substring(0, 100) + "...";
        }
        return outputToLog;
    }

    private static ProcessExecution getProcessExecutionInfo(Configuration pluginConfig)
        throws InvalidPluginConfigurationException {

        PropertySimple executableProp = pluginConfig.getSimple(PLUGINCONFIG_EXECUTABLE);
        PropertySimple workingDirProp = pluginConfig.getSimple(PLUGINCONFIG_WORKINGDIR);
        PropertyList envvarsProp = pluginConfig.getList(PLUGINCONFIG_ENVVARS);

        String executable;
        String workingDir = null;
        Map<String, String> envvars = null;

        if (executableProp == null) {
            throw new InvalidPluginConfigurationException("Missing required plugin config: " + PLUGINCONFIG_EXECUTABLE);
        } else {
            executable = executableProp.getStringValue();
            if (executable == null || executable.length() == 0) {
                throw new InvalidPluginConfigurationException("Bad plugin config: " + PLUGINCONFIG_EXECUTABLE);
            }
        }

        if (workingDirProp != null) {
            workingDir = workingDirProp.getStringValue();
            if (workingDir != null && workingDir.length() == 0) {
                workingDir = null; // empty string is as good as unsetting it (i.e. making it null)
            }
        }

        if (envvarsProp != null) {
            try {
                // we want envvars to be null if there are no envvars defined, so the agent env is passed
                // but if there are 1 or more envvars in our config, then we define our envvars list
                List<Property> listOfMaps = envvarsProp.getList();
                if (listOfMaps.size() > 0) {
                    envvars = new HashMap<String, String>();
                    for (Property envvarMap : listOfMaps) {
                        PropertySimple name = (PropertySimple) ((PropertyMap) envvarMap).get(PLUGINCONFIG_ENVVAR_NAME);
                        PropertySimple value = (PropertySimple) ((PropertyMap) envvarMap)
                            .get(PLUGINCONFIG_ENVVAR_VALUE);
                        envvars.put(name.getStringValue(), value.getStringValue());
                    }
                }
            } catch (Exception e) {
                throw new InvalidPluginConfigurationException("Bad plugin config: " + PLUGINCONFIG_ENVVARS
                    + ". Cause: " + e);
            }
        }

        ProcessExecution processExecution = new ProcessExecution(executable);
        processExecution.setEnvironmentVariables(envvars);
        processExecution.setWorkingDirectory(workingDir);

        return processExecution;
    }

    private boolean isValidRegularExpression(String regex) {
        try {
            Pattern.compile(regex);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void logDebug(String msg) {
        LOG.debug("[" + this.resourceContext.getResourceKey() + "]: " + msg);
    }

    private static boolean isQuotingEnabled(char escapeChar) {
        return escapeChar != DISABLING_ESCAPE_CHARACTER;
    }
}
