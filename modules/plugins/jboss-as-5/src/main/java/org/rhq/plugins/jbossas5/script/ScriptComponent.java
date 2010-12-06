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
package org.rhq.plugins.jbossas5.script;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.transfer.ContentResponseResult;
import org.rhq.core.domain.content.transfer.DeployIndividualPackageResponse;
import org.rhq.core.domain.content.transfer.DeployPackageStep;
import org.rhq.core.domain.content.transfer.DeployPackagesResponse;
import org.rhq.core.domain.content.transfer.RemovePackagesResponse;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.content.ContentFacet;
import org.rhq.core.pluginapi.content.ContentServices;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.pluginapi.util.ProcessExecutionUtility;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.plugins.jbossas5.ApplicationServerComponent;
import org.rhq.plugins.jbossas5.ApplicationServerPluginConfigurationProperties;
import org.rhq.plugins.jbossas5.connection.ProfileServiceConnection;
import org.rhq.plugins.jbossas5.deploy.Deployer;
import org.rhq.plugins.jbossas5.deploy.RemoteDownloader;
import org.rhq.plugins.jbossas5.deploy.ScriptDeployer;

/**
 * A JON service that provides the ability to execute a script.
 * 
 * @author Ian Springer
 * @author Lukas Krejci
 */
public class ScriptComponent implements ResourceComponent<ApplicationServerComponent>, OperationFacet, DeleteResourceFacet, ContentFacet {
    public static final String TYPE_NAME = "Script";
    public static final String PACKAGE_TYPE = "script";
    
    public static final String PATH_CONFIG_PROP = "path";
    public static final String ENVIRONMENT_VARIABLES_CONFIG_PROP = "environmentVariables";
    
    public static final String EXECUTE_OPERATION = "execute";

    public static final String COMMAND_LINE_ARGUMENTS_PARAM_PROP = "commandLineArguments";

    private static final String EXIT_CODE_RESULT_PROP = "exitCode";
    private static final String OUTPUT_RESULT_PROP = "output";

    private static final String PACKAGE_VERSION = "none";
    private static final String PACKAGE_ARCHITECTURE = "noarch";
    
    private final Log log = LogFactory.getLog(this.getClass());

    private ResourceContext<ApplicationServerComponent> resourceContext;

    public void start(ResourceContext<ApplicationServerComponent> resourceContext) {
        this.resourceContext = resourceContext;
    }

    public void stop() {
        this.resourceContext = null;
    }

    public AvailabilityType getAvailability() {
        File scriptFile = getScriptFile();
        return (scriptFile.exists()) ? AvailabilityType.UP : AvailabilityType.DOWN;
    }

    public OperationResult invokeOperation(String name, Configuration params) throws Exception {
        if (name.equals(EXECUTE_OPERATION)) {
            OperationResult operationResult = new OperationResult();

            File scriptFile = getScriptFile();
            SystemInfo systemInfo = this.resourceContext.getSystemInformation();
            ProcessExecution processExecution = ProcessExecutionUtility.createProcessExecution(scriptFile);

            processExecution.setWaitForCompletion(1000L * 60 * 60); // 1 hour
            processExecution.setCaptureOutput(true);

            // TODO: Make the script's cwd configurable, but default it to the
            // directory containing the script.
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
            String output = processExecutionResults.getCapturedOutput(); // NOTE:
            // this
            // is
            // stdout
            // +
            // stderr

            Configuration complexResults = operationResult.getComplexResults();
            complexResults.put(new PropertySimple(EXIT_CODE_RESULT_PROP, exitCode));
            complexResults.put(new PropertySimple(OUTPUT_RESULT_PROP, output));
            if (exitCode != null && exitCode != 0) {
                operationResult.setErrorMessage("Exit code was '" + exitCode + "', see operation results for details");
            }

            return operationResult;
        } else {
            throw new IllegalArgumentException("Unsupported operation: " + name);
        }
    }

    public void deleteResource() throws Exception {
        String path = resourceContext.getPluginConfiguration().getSimpleValue(PATH_CONFIG_PROP, null);
        
        File f = new File(path);
        
        if (!f.delete()) {
            throw new IOException("Failed to delete the file on the configured path: " + path);
        }
    }
    
    public List<DeployPackageStep> generateInstallationSteps(ResourcePackageDetails packageDetails) {
        return null;
    }

    public DeployPackagesResponse deployPackages(Set<ResourcePackageDetails> packages, ContentServices contentServices) {
        DeployPackagesResponse response = new DeployPackagesResponse();
        if (packages.size() != 1) {
            log.warn("Request to deploy a script containing multiple files, which is obivously illegal: " + packages);
            response.setOverallRequestResult(ContentResponseResult.FAILURE);
            response.setOverallRequestErrorMessage("Only one script can be updated at a time.");
            return response;
        }
        
        try {
            String jbossHomeDir = resourceContext.getParentResourceComponent().getResourceContext().getPluginConfiguration().getSimpleValue(ApplicationServerPluginConfigurationProperties.HOME_DIR, null);
            SystemInfo systemInfo = resourceContext.getSystemInformation();
            ProfileServiceConnection profileServiceConnection = resourceContext.getParentResourceComponent().getConnection();
            
            ScriptDeployer deployer = new ScriptDeployer(jbossHomeDir, systemInfo, new RemoteDownloader(resourceContext, true, profileServiceConnection));
            ResourcePackageDetails packageDetails = packages.iterator().next();
            
            DeployIndividualPackageResponse scriptUpdateResult = deployer.update(packageDetails, resourceContext.getResourceType());
            
            response.setOverallRequestResult(scriptUpdateResult.getResult());            
            response.addPackageResponse(scriptUpdateResult);            
        } catch (Exception e) {
            response.setOverallRequestErrorMessage(e.getMessage());
            response.setOverallRequestResult(ContentResponseResult.FAILURE);
        }
        
        return response;
    }

    public RemovePackagesResponse removePackages(Set<ResourcePackageDetails> packages) {
        throw new UnsupportedOperationException("Cannot remove a package backing a script.");
    }

    public Set<ResourcePackageDetails> discoverDeployedPackages(PackageType type) {
        Set<ResourcePackageDetails> results = new HashSet<ResourcePackageDetails>();
        if (PACKAGE_TYPE.equals(type.getName())) {
            File scriptFile = new File(resourceContext.getResourceKey());
            
            PackageDetailsKey key = new PackageDetailsKey(scriptFile.getName(), PACKAGE_VERSION, PACKAGE_TYPE, PACKAGE_ARCHITECTURE);
            ResourcePackageDetails details = new ResourcePackageDetails(key);
            details.setDisplayName(scriptFile.getName());
            details.setFileName(scriptFile.getAbsolutePath());
            details.setFileSize(scriptFile.length());
            details.setLocation(scriptFile.getAbsolutePath());
            details.setFileCreatedDate(scriptFile.lastModified());
            details.setInstallationTimestamp(System.currentTimeMillis());
            try {
                details.setSHA256(new MessageDigestGenerator(MessageDigestGenerator.SHA_256).calcDigestString(scriptFile));
            } catch (IOException e) {
                log.warn("Failed to compute the SHA256 digest of the script: " + scriptFile.getAbsolutePath(), e);
            }
            results.add(details);
        }
        
        return results;
    }

    public InputStream retrievePackageBits(ResourcePackageDetails packageDetails) {
        String jbossHomeDir = resourceContext.getParentResourceComponent().getResourceContext().getPluginConfiguration().getSimpleValue(ApplicationServerPluginConfigurationProperties.HOME_DIR, null);
        File binDir = new File(jbossHomeDir, "bin");

        String scriptName = packageDetails.getKey().getName();
        
        File script = new File(binDir, scriptName);
        
        try {
            return new FileInputStream(script);
        } catch (FileNotFoundException e) {
            log.warn("Failed to retrieve package bits for script " + packageDetails, e);
            return null;
        }
    }

    private void setCommandLineArguments(Configuration params, ProcessExecution processExecution) {
        List<String> processExecutionArguments = processExecution.getArguments();
        if (null == processExecutionArguments) {
            processExecutionArguments = new ArrayList<String>();
            processExecution.setArguments(processExecutionArguments);
        }

        String cmdLineArgsString = params.getSimpleValue(COMMAND_LINE_ARGUMENTS_PARAM_PROP, null);
        List<String> cmdLineArgs = createCommandLineArgumentList(cmdLineArgsString);
        if (null != cmdLineArgs) {
            processExecutionArguments.addAll(cmdLineArgs);
        }
    }

    private void setEnvironmentVariables(ProcessExecution processExecution) {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        Map<String, String> processExecutionEnvironmentVariables = processExecution.getEnvironmentVariables();
        if (null == processExecutionEnvironmentVariables) {
            processExecutionEnvironmentVariables = new LinkedHashMap<String, String>();
            processExecution.setEnvironmentVariables(processExecutionEnvironmentVariables);
        }

        String envVars = pluginConfig.getSimpleValue(ENVIRONMENT_VARIABLES_CONFIG_PROP, null);
        Map<String, String> envVarsMap = createEnvironmentVariableMap(envVars);
        if (null != envVarsMap) {
            processExecutionEnvironmentVariables.putAll(envVarsMap);
        }
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
        Configuration parentPluginConfig = this.resourceContext.getParentResourceComponent().getResourceContext()
            .getPluginConfiguration();
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
