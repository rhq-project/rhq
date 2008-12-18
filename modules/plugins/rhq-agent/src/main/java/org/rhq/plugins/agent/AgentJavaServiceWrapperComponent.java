/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.plugins.agent;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.util.exception.ExceptionPackage;
import org.rhq.core.util.exception.Severity;
import org.rhq.enterprise.agent.EnvironmentScriptFileUpdate;
import org.rhq.enterprise.agent.EnvironmentScriptFileUpdate.NameValuePair;

/**
 * The component that represents the agent's Java Service Wrapper (JSW).
 *
 * @author John Mazzitelli
 */
public class AgentJavaServiceWrapperComponent implements ResourceComponent<AgentServerComponent>, ConfigurationFacet,
    OperationFacet {

    private Log log = LogFactory.getLog(AgentJavaServiceWrapperComponent.class);

    private ResourceContext<AgentServerComponent> resourceContext;

    private File launcherScript;
    private File configFile;
    private File environmentFile;
    private File includeFile;

    public void start(ResourceContext<AgentServerComponent> rc) throws Exception {

        this.resourceContext = rc;

        Configuration pc = this.resourceContext.getPluginConfiguration();

        PropertySimple prop;

        prop = pc.getSimple(AgentJavaServiceWrapperDiscoveryComponent.PLUGINCONFIG_LAUNCHER_SCRIPT);
        if (prop == null) {
            throw new InvalidPluginConfigurationException("Missing Launcher Script");
        }
        if (prop.getStringValue() == null) {
            throw new InvalidPluginConfigurationException("Launcher Script property value is null");
        }

        launcherScript = new File(prop.getStringValue());
        if (!launcherScript.exists()) {
            throw new InvalidPluginConfigurationException("Launcher Script [" + launcherScript + "] does not exist");
        }

        prop = pc.getSimple(AgentJavaServiceWrapperDiscoveryComponent.PLUGINCONFIG_CONF_FILE);
        if (prop == null) {
            throw new InvalidPluginConfigurationException("Missing Configuration File");
        }
        if (prop.getStringValue() == null) {
            throw new InvalidPluginConfigurationException("Configuration File property value is null");
        }

        configFile = new File(prop.getStringValue());
        if (!configFile.exists()) {
            throw new InvalidPluginConfigurationException("Config file [" + configFile + "] does not exist");
        }

        log.debug("Starting agent JSW component: " + configFile);

        // get the optional files (these may remain null if the paths were left undefined)
        prop = pc.getSimple(AgentJavaServiceWrapperDiscoveryComponent.PLUGINCONFIG_ENV_FILE);
        if (prop != null && prop.getStringValue() != null) {
            environmentFile = new File(prop.getStringValue());
        }

        prop = pc.getSimple(AgentJavaServiceWrapperDiscoveryComponent.PLUGINCONFIG_INC_FILE);
        if (prop != null && prop.getStringValue() != null) {
            includeFile = new File(prop.getStringValue());
        }

        return;
    }

    public void stop() {
        // nothing to do
        return;
    }

    public AvailabilityType getAvailability() {

        return (launcherScript.exists() && configFile.exists()) ? AvailabilityType.UP : AvailabilityType.DOWN;

        // I would like to do this but:
        // 1. I don't like executing the script like this every 60 seconds; not very efficient and,
        // 2. I don't think executing this script and processing it output will always be faster than
        //    the 5 seconds the plugin container will give us. 
        //        try {
        //            String output = executeLauncherScript("status");
        //            return (output.contains("is installed")) ? AvailabilityType.UP : AvailabilityType.DOWN;
        //        } catch (Throwable t) {
        //            return AvailabilityType.DOWN;
        //        }
    }

    public OperationResult invokeOperation(String name, Configuration params) throws Exception {

        OperationResult result = null;
        try {
            if (name.equals("Status")) {
                Map<Integer, String> output = executeLauncherScript("status");
                result = new OperationResult();
                Integer exitCode = output.keySet().iterator().next();
                determineServiceStatus(exitCode, result);
                result.getComplexResults().put(new PropertySimple("exitCode", exitCode));
                result.getComplexResults().put(new PropertySimple("output", output.values().toArray()[0]));
            } else if (name.equals("Install")) {
                Map<Integer, String> output = executeLauncherScript("install");
                result = new OperationResult();
                Integer exitCode = output.keySet().iterator().next();
                result.getComplexResults().put(new PropertySimple("exitCode", exitCode));
                result.getComplexResults().put(new PropertySimple("output", output.values().toArray()[0]));
            } else if (name.equals("Restart")) {
                executeLauncherScriptInThread("restart");
            } else if (name.equals("Stop")) {
                executeLauncherScriptInThread("stop");
            } else if (name.equals("Remove")) {
                executeLauncherScriptInThread("remove");
            } else {
                throw new UnsupportedOperationException("Invalid operation name: " + name);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke operation [" + name + "]", e);
        }
        return result;
    }

    private void determineServiceStatus(Integer exitCode, OperationResult result) {
        if (exitCode == null || exitCode.intValue() < 0) {
            return;
        }

        int bitmask = exitCode.intValue();
        Configuration map = result.getComplexResults();
        map.put(new PropertySimple("disabled", ((bitmask & 32) == 32) ? Boolean.TRUE : Boolean.FALSE));
        map.put(new PropertySimple("requiresManualStart", ((bitmask & 16) == 16) ? Boolean.TRUE : Boolean.FALSE));
        map.put(new PropertySimple("willAutomaticallyStart", ((bitmask & 8) == 8) ? Boolean.TRUE : Boolean.FALSE));
        map.put(new PropertySimple("hasInteractiveConsole", ((bitmask & 4) == 4) ? Boolean.TRUE : Boolean.FALSE));
        map.put(new PropertySimple("isRunning", ((bitmask & 2) == 2) ? Boolean.TRUE : Boolean.FALSE));
        map.put(new PropertySimple("isInstalled", ((bitmask & 1) == 1) ? Boolean.TRUE : Boolean.FALSE));
        return;
    }

    private Map<Integer, String> executeLauncherScript(String arg) throws Exception {
        if (!this.launcherScript.exists()) {
            throw new Exception("Launcher script [" + this.launcherScript + "] does not exist");
        }

        Map<String, String> envvars = new HashMap<String, String>(System.getenv());
        envvars.put("RHQ_AGENT_DEBUG", "false"); // we don't want all that debug output in the beginning

        ProcessExecution exe = new ProcessExecution(this.launcherScript.getAbsolutePath());
        exe.setArguments(new String[] { arg });
        exe.setWorkingDirectory(this.launcherScript.getParent());
        exe.setCaptureOutput(true);
        exe.setWaitForCompletion(30000L);
        exe.setEnvironmentVariables(envvars);
        ProcessExecutionResults results = this.resourceContext.getSystemInformation().executeProcess(exe);
        Throwable error = results.getError();
        if (error != null) {
            throw new Exception("Failed to invoke [" + this.launcherScript + ' ' + arg + "]", error);
        }

        HashMap<Integer, String> map = new HashMap<Integer, String>();
        Integer exitCode = results.getExitCode();
        String output = results.getCapturedOutput();
        map.put((exitCode != null) ? exitCode : Integer.valueOf(-1), (output != null) ? output : "");
        return map;
    }

    /**
     * This will execute the launcher script in a separate thread. This separate thread
     * will sleep for a few seconds before executing to give the caller enough time to
     * return itself. This is used when the launcher script being executed will quickly
     * kill the agent VM process in which we are running.
     * 
     * @param arg the command to pass to the launcher script
     *
     * @throws Exception if failed to even get a chance to spawn the thread and execute the launcher
     */
    private void executeLauncherScriptInThread(final String arg) throws Exception {
        if (!this.launcherScript.exists()) {
            throw new Exception("Launcher script [" + this.launcherScript + "] does not exist");
        }

        final File script = this.launcherScript;
        final SystemInfo sysInfo = this.resourceContext.getSystemInformation();

        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(10000L); // this should be enough to return our operation results back
                    ProcessExecution exe = new ProcessExecution(script.getAbsolutePath());
                    exe.setArguments(new String[] { arg });
                    exe.setWorkingDirectory(script.getParent());
                    ProcessExecutionResults results = sysInfo.executeProcess(exe);
                    if (results != null && results.getError() != null) {
                        throw results.getError();
                    }
                } catch (Throwable t) {
                    log.error("Failed to invoke [" + script + ' ' + arg + "] in a thread", t);
                }
            }
        }, "RHQ Agent Plugin JSW Launcher Thread");
        thread.setDaemon(true);

        // after we start, do not linger; return fast so we can return our operation results before we die
        thread.start();
        return;
    }

    public Configuration loadResourceConfiguration() throws Exception {
        Configuration config = new Configuration();
        PropertyList conf = loadConfigurationFileConfiguration();
        PropertyList env = loadEnvironmentFileConfiguration();
        PropertyList inc = loadIncludeFileConfiguration();
        if (conf != null) {
            config.put(conf);
        }
        if (env != null) {
            config.put(env);
        }
        if (inc != null) {
            config.put(inc);
        }
        return config;
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport request) {
        try {
            updateConfigurationFileConfiguration(request);
            updateEnvironmentFileConfiguration(request);
            updateIncludeFileConfiguration(request);
        } catch (Exception e) {
            request.setErrorMessage(new ExceptionPackage(Severity.Severe, e).toString());
        }

        return;
    }

    private PropertyList loadConfigurationFileConfiguration() throws Exception {
        if (configFile == null || !configFile.exists()) {
            return null;
        }

        // read in the file and get all the settings it defines
        EnvironmentScriptFileUpdate updater = EnvironmentScriptFileUpdate.create(configFile.getAbsolutePath());
        List<NameValuePair> properties = updater.loadExisting();

        // put the env var definitions in a config object
        PropertyList list = new PropertyList("mainConfigurationSettings");

        for (NameValuePair prop : properties) {
            PropertyMap map = new PropertyMap("mainConfigurationSetting");
            map.put(new PropertySimple("name", prop.name));
            map.put(new PropertySimple("value", prop.value));
            list.add(map);
        }

        return list;
    }

    private PropertyList loadEnvironmentFileConfiguration() throws Exception {
        if (environmentFile == null || !environmentFile.exists()) {
            return null;
        }

        // read in the file and get all the settings it defines
        EnvironmentScriptFileUpdate updater = EnvironmentScriptFileUpdate.create(environmentFile.getAbsolutePath());
        List<NameValuePair> properties = updater.loadExisting();

        // put the env var definitions in a config object
        PropertyList list = new PropertyList("environmentSettings");

        for (NameValuePair prop : properties) {
            PropertyMap map = new PropertyMap("environmentSetting");
            map.put(new PropertySimple("name", prop.name));
            map.put(new PropertySimple("value", prop.value));
            list.add(map);
        }

        return list;
    }

    private PropertyList loadIncludeFileConfiguration() throws Exception {
        if (includeFile == null || !includeFile.exists()) {
            return null;
        }

        // read in the file and get all the settings it defines
        EnvironmentScriptFileUpdate updater = EnvironmentScriptFileUpdate.create(includeFile.getAbsolutePath());
        List<NameValuePair> properties = updater.loadExisting();

        // put the env var definitions in a config object
        PropertyList list = new PropertyList("includeSettings");

        for (NameValuePair prop : properties) {
            PropertyMap map = new PropertyMap("includeSetting");
            map.put(new PropertySimple("name", prop.name));
            map.put(new PropertySimple("value", prop.value));
            list.add(map);
        }

        return list;
    }

    private void updateConfigurationFileConfiguration(ConfigurationUpdateReport request) {
        try {
            List<NameValuePair> newSettings = new ArrayList<NameValuePair>();

            Configuration configuration = request.getConfiguration();
            PropertyList list = configuration.getList("mainConfigurationSettings");

            if (list == null) {
                throw new Exception("Missing main config");
            }

            for (Property item : list.getList()) {
                PropertyMap map = (PropertyMap) item;
                PropertySimple name = map.getSimple("name");
                PropertySimple value = map.getSimple("value");

                if (name == null || name.getStringValue() == null) {
                    log.error("Missing a config name: " + configuration.toString(true));
                    throw new IllegalArgumentException("Missing the name of a main config setting");
                }

                if (value != null && value.getStringValue() != null) {
                    newSettings.add(new NameValuePair(name.getStringValue(), value.getStringValue()));
                }
            }

            // update the env script file so it includes the new settings.
            // note that we require the request to contain ALL settings, not a subset; any settings
            // missing in the request config that currently exist in the script will be removed from the script,
            // which would be bad - but that should never occur unless something bad happens in the UI
            EnvironmentScriptFileUpdate updater = EnvironmentScriptFileUpdate.create(configFile.getAbsolutePath());
            updater.update(newSettings, true);

            request.setStatus(ConfigurationUpdateStatus.SUCCESS);
        } catch (Exception e) {
            request.setErrorMessage(new ExceptionPackage(Severity.Severe, e).toString());
        }

        return;
    }

    private void updateEnvironmentFileConfiguration(ConfigurationUpdateReport request) {
        try {
            List<NameValuePair> newSettings = new ArrayList<NameValuePair>();

            Configuration configuration = request.getConfiguration();
            PropertyList list = configuration.getList("environmentSettings");

            // if there is no config, the file should be deleted
            if (list == null || list.getList() == null || list.getList().isEmpty()) {
                if (environmentFile.exists()) {
                    if (!environmentFile.delete()) {
                        throw new Exception("Failed to remove the env file: " + environmentFile);
                    }
                }
                return;
            }

            for (Property item : list.getList()) {
                PropertyMap map = (PropertyMap) item;
                PropertySimple name = map.getSimple("name");
                PropertySimple value = map.getSimple("value");

                if (name == null || name.getStringValue() == null) {
                    log.error("Missing a env name: " + configuration.toString(true));
                    throw new IllegalArgumentException("Missing the name of a env setting");
                }

                if (value != null && value.getStringValue() != null) {
                    newSettings.add(new NameValuePair(name.getStringValue(), value.getStringValue()));
                }
            }

            // update the env script file so it includes the new settings.
            // note that we require the request to contain ALL settings, not a subset; any settings
            // missing in the request config that currently exist in the script will be removed from the script,
            // which would be bad - but that should never occur unless something bad happens in the UI
            EnvironmentScriptFileUpdate updater = EnvironmentScriptFileUpdate.create(environmentFile.getAbsolutePath());
            updater.update(newSettings, true);

            request.setStatus(ConfigurationUpdateStatus.SUCCESS);
        } catch (Exception e) {
            request.setErrorMessage(new ExceptionPackage(Severity.Severe, e).toString());
        }

        return;
    }

    private void updateIncludeFileConfiguration(ConfigurationUpdateReport request) {
        try {
            List<NameValuePair> newSettings = new ArrayList<NameValuePair>();

            Configuration configuration = request.getConfiguration();
            PropertyList list = configuration.getList("includeSettings");

            // if there is no config, the file should be deleted
            if (list == null || list.getList() == null || list.getList().isEmpty()) {
                if (includeFile.exists()) {
                    if (!includeFile.delete()) {
                        throw new Exception("Failed to remove the include file: " + includeFile);
                    }
                }
                return;
            }

            for (Property item : list.getList()) {
                PropertyMap map = (PropertyMap) item;
                PropertySimple name = map.getSimple("name");
                PropertySimple value = map.getSimple("value");

                if (name == null || name.getStringValue() == null) {
                    log.error("Missing a inc name: " + configuration.toString(true));
                    throw new IllegalArgumentException("Missing the name of a include setting");
                }

                if (value != null && value.getStringValue() != null) {
                    newSettings.add(new NameValuePair(name.getStringValue(), value.getStringValue()));
                }
            }

            // update the env script file so it includes the new settings.
            // note that we require the request to contain ALL settings, not a subset; any settings
            // missing in the request config that currently exist in the script will be removed from the script,
            // which would be bad - but that should never occur unless something bad happens in the UI
            EnvironmentScriptFileUpdate updater = EnvironmentScriptFileUpdate.create(includeFile.getAbsolutePath());
            updater.update(newSettings, true);

            request.setStatus(ConfigurationUpdateStatus.SUCCESS);
        } catch (Exception e) {
            request.setErrorMessage(new ExceptionPackage(Severity.Severe, e).toString());
        }

        return;
    }
}