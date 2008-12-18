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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.SystemInfo;

/**
 * The component that represents the agent's launcher script.
 *
 * @author John Mazzitelli
 */
public class AgentLauncherScriptComponent implements ResourceComponent<AgentServerComponent>, OperationFacet {

    private Log log = LogFactory.getLog(AgentLauncherScriptComponent.class);

    private ResourceContext<AgentServerComponent> resourceContext;

    private File launcherScript;

    public void start(ResourceContext<AgentServerComponent> rc) throws Exception {

        this.resourceContext = rc;
        Configuration pc = this.resourceContext.getPluginConfiguration();
        PropertySimple prop;

        prop = pc.getSimple(AgentLauncherScriptDiscoveryComponent.PLUGINCONFIG_PATHNAME);
        if (prop == null) {
            throw new InvalidPluginConfigurationException("Missing path to launcher script");
        }
        if (prop.getStringValue() == null) {
            throw new InvalidPluginConfigurationException("Launcher script pathname property value is null");
        }

        launcherScript = new File(prop.getStringValue());
        if (!launcherScript.exists()) {
            throw new InvalidPluginConfigurationException("Launcher Script [" + launcherScript + "] does not exist");
        }

        log.debug("Starting agent launcher script component: " + launcherScript);
        return;
    }

    public void stop() {
        // nothing to do
        return;
    }

    public AvailabilityType getAvailability() {
        return (launcherScript.exists()) ? AvailabilityType.UP : AvailabilityType.DOWN;
    }

    public OperationResult invokeOperation(String name, Configuration params) throws Exception {

        OperationResult result = null;
        try {
            if (name.equals("Status")) {
                Map<Integer, String> output = executeLauncherScript("status");
                result = new OperationResult();
                Integer exitCode = output.keySet().iterator().next();
                result.getComplexResults().put(new PropertySimple("exitCode", exitCode));
                result.getComplexResults().put(new PropertySimple("output", output.values().toArray()[0]));
            } else if (name.equals("Restart")) {
                executeLauncherScriptInThread("restart");
            } else if (name.equals("Stop")) {
                executeLauncherScriptInThread("stop");
            } else if (name.equals("Kill")) {
                executeLauncherScriptInThread("kill");
            } else {
                throw new UnsupportedOperationException("Invalid operation name: " + name);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke operation [" + name + "]", e);
        }
        return result;
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
        }, "RHQ Agent Plugin Launcher Script Thread");
        thread.setDaemon(true);

        // after we start, do not linger; return fast so we can return our operation results before we die
        thread.start();
        return;
    }
}