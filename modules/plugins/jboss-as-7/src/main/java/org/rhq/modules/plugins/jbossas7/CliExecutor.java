/*
 * RHQ Management Platform
 * Copyright (C) 2014 Red Hat, Inc.
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.util.ProcessExecutionUtility;
import org.rhq.core.pluginapi.util.StartScriptConfiguration;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.SystemInfo;
import org.rhq.modules.plugins.jbossas7.helper.ServerPluginConfiguration;
import org.rhq.modules.plugins.jbossas7.util.PropertyReplacer;

/**
 * @author Lukas Krejci
 * @author Thomas Segismont
 *
 * @since 4.12
 */
final class CliExecutor {

    private static final Log LOG = LogFactory.getLog(CliExecutor.class);

    private static final long MAX_PROCESS_WAIT_TIME = 3600000L;

    private final ServerPluginConfiguration serverPluginConfig;
    private final Configuration pluginConfiguration;
    private final Map<String, String> startScriptEnv;
    private final AS7Mode serverMode;
    private final SystemInfo systemInfo;

    private boolean disconnected;
    private long waitTime;
    private boolean killOnTimeout;

    private CliExecutor(Configuration pluginConfiguration, AS7Mode serverMode, SystemInfo systemInfo) {
        this.serverPluginConfig = new ServerPluginConfiguration(pluginConfiguration);
        this.pluginConfiguration = pluginConfiguration;
        this.serverMode = serverMode;
        this.systemInfo = systemInfo;
        this.startScriptEnv = new StartScriptConfiguration(pluginConfiguration).getStartScriptEnv();

        for (String envVarName : startScriptEnv.keySet()) {
            String envVarValue = startScriptEnv.get(envVarName);
            envVarValue = PropertyReplacer.replacePropertyPatterns(envVarValue, pluginConfiguration);
            startScriptEnv.put(envVarName, envVarValue);
        }
    }

    public static CliExecutor onServer(Configuration serverPluginConfig, AS7Mode serverMode,
        SystemInfo systemInfo) {

        return new CliExecutor(serverPluginConfig, serverMode, systemInfo);
    }

    /**
     * 0, the default, means waiting forever. Any positive number means waiting for given number of milliseconds
     * and timing out afterwards. Any negative value means timing out immediately.
     */
    public CliExecutor waitingFor(long milliseconds) {
        this.waitTime = milliseconds;
        return this;
    }

    public CliExecutor killingOnTimeout(boolean kill) {
        killOnTimeout = kill;
        return this;
    }

    public CliExecutor disconnected(boolean disconnected) {
        this.disconnected = disconnected;
        return this;
    }

    /**
     * Runs (a series of) CLI commands against the server.
     * The commands are separated by either a newline or a comma (or a mix thereof).
     *
     * @param commands the commands to execute in order
     * @return the execution results
     */
    public ProcessExecutionResults executeCliCommand(String commands) {
        String connect = disconnected ? null : "--connect";
        commands = commands.replace('\n', ',');
        String user = disconnected ? null : "--user=" + serverPluginConfig.getUser();
        String password = disconnected ? null : "--password=" + serverPluginConfig.getPassword();
        String controller = disconnected ? null : "--controller" + serverPluginConfig.getNativeHost() + ":"
            + serverPluginConfig.getNativePort();

        return execute(new File("bin", serverMode.getCliScriptFileName()), connect, commands, user, password,
            controller);
    }

    /**
     * Runs the provided script against the server.
     *
     * @param scriptFile the script file to run
     * @return the execution results
     */
    public ProcessExecutionResults executeCliScript(File scriptFile) {
        File homeDir = serverPluginConfig.getHomeDir();

        File script = scriptFile;
        if (!script.isAbsolute()) {
            script = new File(homeDir, scriptFile.getPath());
        }

        String connect = disconnected ? null : "--connect";
        String file = "--file=" + script.getAbsolutePath();
        String user = disconnected ? null : "--user=" + serverPluginConfig.getUser();
        String password = disconnected ? null : "--password=" + serverPluginConfig.getPassword();
        String controller = disconnected ? null : "--controller" + serverPluginConfig.getNativeHost() + ":"
            + serverPluginConfig.getNativePort();

        return execute(new File("bin", serverMode.getCliScriptFileName()), connect, file, user, password, controller);
    }

    /**
     * This command ignores the timeout set by the {@link #waitingFor(long)} method. It starts the process and returns
     * immediately. Other means have to be used to determine if the server finished starting up.
     */
    public ProcessExecutionResults startServer() {
        StartScriptConfiguration startScriptConfiguration = new StartScriptConfiguration(pluginConfiguration);
        File startScriptFile = startScriptConfiguration.getStartScript();

        if (startScriptFile == null) {
            startScriptFile = new File("bin", serverMode.getStartScriptFileName());
        }

        List<String> startScriptArgsL = startScriptConfiguration.getStartScriptArgs();
        String[] startScriptArgs = startScriptArgsL.toArray(new String[startScriptArgsL.size()]);

        for (int i = 0; i < startScriptArgs.length; ++i) {
            startScriptArgs[i] = PropertyReplacer.replacePropertyPatterns(startScriptArgs[i], pluginConfiguration);
        }

        long origWaitTime = waitTime;
        try {
            //we really don't want to wait for the server start, because, hopefully, it will keep on running ;)
            waitTime = -1;
            return execute(startScriptFile, startScriptArgs);
        } finally {
            waitTime = origWaitTime;
        }
    }

    public ProcessExecutionResults shutdownServer() {
        boolean origDisconnected = disconnected;
        try {
            disconnected = false;
            return executeCliCommand("shutdown");
        } finally {
            disconnected = origDisconnected;
        }
    }

    private ProcessExecutionResults execute(File executable, String... args) {
        File homeDir = serverPluginConfig.getHomeDir();
        File startScriptFile = executable.isAbsolute() ? executable : new File(homeDir, executable.getPath());

        ProcessExecution processExecution = ProcessExecutionUtility.createProcessExecution(null,
            startScriptFile);

        processExecution.setEnvironmentVariables(startScriptEnv);

        List<String> arguments = processExecution.getArguments();
        if (arguments == null) {
            arguments = new ArrayList<String>();
            processExecution.setArguments(arguments);
        }

        for(String arg : args) {
            if (arg != null) {
                arguments.add(arg);
            }
        }

        // When running on Windows 9x, standalone.bat and domain.bat need the cwd to be the AS bin dir in order to find
        // standalone.bat.conf and domain.bat.conf respectively.
        processExecution.setWorkingDirectory(startScriptFile.getParent());
        processExecution.setCaptureOutput(true);
        processExecution.setWaitForCompletion(MAX_PROCESS_WAIT_TIME);
        processExecution.setKillOnTimeout(false);

        if (waitTime > 0) {
            processExecution.setWaitForCompletion(waitTime);
        } else if (waitTime < 0) {
            processExecution.setWaitForCompletion(0);
        }

        processExecution.setKillOnTimeout(killOnTimeout);

        if (LOG.isDebugEnabled()) {
            LOG.debug("About to execute the following process: [" + processExecution + "]");
        }

        return systemInfo.executeProcess(processExecution);
    }
}
