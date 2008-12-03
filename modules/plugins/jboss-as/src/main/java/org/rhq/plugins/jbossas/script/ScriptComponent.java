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
package org.rhq.plugins.jbossas.script;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.pluginapi.util.ProcessExecutionUtility;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.SystemInfo;
import org.rhq.plugins.jbossas.JBossASServerComponent;

/**
 * A JON service that provides the ability to execute a script.
 *
 * @author Ian Springer
 */
public class ScriptComponent implements ResourceComponent<JBossASServerComponent>, OperationFacet {
    public static final String PATH_CONFIG_PROP = "path";
    public static final String ENVIRONMENT_VARIABLES_CONFIG_PROP = "environmentVariables";

    public static final String EXECUTE_OPERATION = "execute";

    public static final String COMMAND_LINE_ARGUMENTS_PARAM_PROP = "commandLineArguments";

    private final Log log = LogFactory.getLog(this.getClass());

    private ResourceContext<JBossASServerComponent> resourceContext;

    public void start(ResourceContext<JBossASServerComponent> resourceContext) {
        this.resourceContext = resourceContext;
    }

    public void stop() {
        this.resourceContext = null;
    }

    public AvailabilityType getAvailability() {
        File scriptFile = getScriptFile();
        return (scriptFile.exists()) ? AvailabilityType.UP : AvailabilityType.DOWN;
    }

    @SuppressWarnings( { "ConstantConditions" })
    public OperationResult invokeOperation(String name, Configuration params) throws Exception {
        if (name.equals(EXECUTE_OPERATION)) {
            OperationResult operationResult = new OperationResult();

            File scriptFile = getScriptFile();
            SystemInfo systemInfo = this.resourceContext.getSystemInformation();
            ProcessExecution processExecution = ProcessExecutionUtility.createProcessExecution(scriptFile);

            processExecution.setWaitForCompletion(1000L * 60 * 60); // 1 hour
            processExecution.setCaptureOutput(true);

            // TODO: Make the script's cwd configurable, but default it to the directory containing the script.
            processExecution.setWorkingDirectory(scriptFile.getParent());

            setEnvironmentVariables(processExecution);
            setCommandLineArguments(params, processExecution);

            if (log.isDebugEnabled()) {
                log.debug(processExecution);
            }

            ProcessExecutionResults processExecutionResults = systemInfo.executeProcess(processExecution);
            if (processExecutionResults.getError() != null) {
                throw new Exception(processExecutionResults.getError());
            }

            Integer exitCode = processExecutionResults.getExitCode();
            operationResult.setExitCode(exitCode);
            String output = processExecutionResults.getCapturedOutput(); // NOTE: this is stdout + stderr
            operationResult.setOutput(output);

            return operationResult;
        } else {
            throw new IllegalArgumentException("Unsupported operation: " + name);
        }
    }

    private void setCommandLineArguments(Configuration params, ProcessExecution processExecution) {
        String cmdLineArgsString = params.getSimpleValue(COMMAND_LINE_ARGUMENTS_PARAM_PROP, null);
        List<String> cmdLineArgs = createCommandLineArgumentList(cmdLineArgsString);
        List<String> processExecutionArguments = processExecution.getArguments();
        if (processExecutionArguments == null) {
            processExecutionArguments = new ArrayList<String>();
            processExecution.setArguments(processExecutionArguments);
        }

        processExecutionArguments.addAll(cmdLineArgs);
    }

    private void setEnvironmentVariables(ProcessExecution processExecution) {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        String envVars = pluginConfig.getSimpleValue(ENVIRONMENT_VARIABLES_CONFIG_PROP, null);
        Map<String, String> envVarsMap = createEnvironmentVariableMap(envVars);
        Map<String, String> processExecutionEnvironmentVariables = processExecution.getEnvironmentVariables();
        if (processExecutionEnvironmentVariables == null) {
            processExecutionEnvironmentVariables = new LinkedHashMap<String, String>();
            processExecution.setEnvironmentVariables(processExecutionEnvironmentVariables);
        }

        processExecutionEnvironmentVariables.putAll(envVarsMap);
    }

    @NotNull
    private List<String> createCommandLineArgumentList(String cmdLineArgsString) {
        if (cmdLineArgsString == null) {
            return new ArrayList<String>();
        }

        StringTokenizer tokenizer = new StringTokenizer(cmdLineArgsString, "\n");
        List<String> cmdLineArgs = new ArrayList<String>(tokenizer.countTokens());
        while (tokenizer.hasMoreTokens()) {
            String cmdLineArg = tokenizer.nextToken().trim();
            cmdLineArg = replacePropertyPatterns(cmdLineArg);
            cmdLineArgs.add(cmdLineArg);
        }

        return cmdLineArgs;
    }

    private Map<String, String> createEnvironmentVariableMap(String envVarsString) {
        if (envVarsString == null) {
            return null;
        }

        StringTokenizer tokenizer = new StringTokenizer(envVarsString, "\n");
        Map<String, String> envVars = new LinkedHashMap<String, String>(tokenizer.countTokens());
        while (tokenizer.hasMoreTokens()) {
            String var = tokenizer.nextToken().trim();
            int equalsIndex = var.indexOf('=');
            if (equalsIndex == -1) {
                throw new IllegalStateException("Malformed environment entry: " + var);
            }

            String varName = var.substring(0, equalsIndex);
            String varValue = var.substring(equalsIndex + 1);
            varValue = replacePropertyPatterns(varValue);
            envVars.put(varName, varValue);
        }

        return envVars;
    }

    private String replacePropertyPatterns(String envVars) {
        Pattern pattern = Pattern.compile("(%([^%]*)%)");
        Matcher matcher = pattern.matcher(envVars);
        Configuration parentPluginConfig = this.resourceContext.getParentResourceComponent().getPluginConfiguration();
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String propName = matcher.group(2);
            PropertySimple prop = parentPluginConfig.getSimple(propName);
            String propPattern = matcher.group(1);
            String replacement = (prop != null) ? prop.getStringValue() : propPattern;
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private File getScriptFile() {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        String scriptFilePath = pluginConfig.getSimple(PATH_CONFIG_PROP).getStringValue();
        return new File(scriptFilePath);
    }
}