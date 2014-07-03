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

import static org.rhq.modules.plugins.jbossas7.util.ProcessExecutionLogger.logExecutionResults;
import static org.rhq.modules.plugins.jbossas7.util.PropertyReplacer.replacePropertyPatterns;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.pluginapi.util.ProcessExecutionUtility;
import org.rhq.core.pluginapi.util.StartScriptConfiguration;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.SystemInfo;
import org.rhq.modules.plugins.jbossas7.helper.ServerPluginConfiguration;

/**
 * @author Thomas Segismont
 */
class CliExecutor {
    private static final Log LOG = LogFactory.getLog(CliExecutor.class);

    private final AS7Mode serverMode;
    private final ServerPluginConfiguration serverPluginConfig;
    private final StartScriptConfiguration startScriptConfig;
    private final SystemInfo systemInfo;

    CliExecutor(AS7Mode serverMode, ServerPluginConfiguration serverPluginConfig,
        StartScriptConfiguration startScriptConfig, SystemInfo systemInfo) {
        this.serverMode = serverMode;
        this.serverPluginConfig = serverPluginConfig;
        this.startScriptConfig = startScriptConfig;
        this.systemInfo = systemInfo;
    }

    ProcessExecutionResults executeCliCommand(String commands, long waitTime, boolean killOnTimeout) {
        StringBuilder additionalArg = new StringBuilder(commands.length());
        for (Scanner scanner = new Scanner(commands); scanner.hasNextLine();) {
            additionalArg.append(scanner.nextLine());
            if (scanner.hasNextLine()) {
                additionalArg.append(",");
            }
        }
        return executeCli(Arrays.asList(additionalArg.toString()), waitTime, killOnTimeout);
    }

    ProcessExecutionResults executeCliScript(String scriptFile, long waitTime, boolean killOnTimeout) {
        List<String> additionalArgs = Arrays.asList("--file=" + scriptFile);
        return executeCli(additionalArgs, waitTime, killOnTimeout);
    }

    ProcessExecutionResults executeCli(List<String> additionalArgs, long waitTime, boolean killOnTimeout) {
        File startScriptFile = new File(new File(serverPluginConfig.getHomeDir(), "bin"),
            serverMode.getCliScriptFileName());

        ProcessExecution processExecution = ProcessExecutionUtility.createProcessExecution(startScriptFile);

        processExecution.setArguments(new ArrayList<String>(15));
        List<String> arguments = processExecution.getArguments();
        arguments.add("--connect");
        arguments.add("--user=" + serverPluginConfig.getUser());
        arguments.add("--password=" + serverPluginConfig.getPassword());
        arguments.add("--controller=" + serverPluginConfig.getNativeHost() + ":" + serverPluginConfig.getNativePort());

        arguments.addAll(additionalArgs);

        Map<String, String> startScriptEnv = startScriptConfig.getStartScriptEnv();
        for (String envVarName : startScriptEnv.keySet()) {
            String envVarValue = startScriptEnv.get(envVarName);
            envVarValue = replacePropertyPatterns(envVarValue, serverPluginConfig.getPluginConfig());
            startScriptEnv.put(envVarName, envVarValue);
        }
        processExecution.setEnvironmentVariables(startScriptEnv);

        // When running on Windows 9x, standalone.bat and domain.bat need the cwd to be the AS bin dir in order to find
        // standalone.bat.conf and domain.bat.conf respectively.
        processExecution.setWorkingDirectory(startScriptFile.getParent());
        processExecution.setCaptureOutput(true);

        processExecution.setWaitForCompletion(waitTime);
        processExecution.setKillOnTimeout(killOnTimeout);

        if (LOG.isDebugEnabled()) {
            LOG.debug("About to execute the following process: [" + processExecution + "]");
        }

        ProcessExecutionResults results = systemInfo.executeProcess(processExecution);
        logExecutionResults(results);
        return results;
    }

}
