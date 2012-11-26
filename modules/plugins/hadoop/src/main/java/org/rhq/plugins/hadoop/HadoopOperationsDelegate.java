/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.plugins.hadoop;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.SystemInfo;

/**
 * Handles performing operations on Hadoop node instance.
 * 
 * @author Jirka Kremser
 */
public class HadoopOperationsDelegate {

    private static final Log LOG = LogFactory.getLog(HadoopOperationsDelegate.class);
    private static final long MAX_WAIT = 1000 * 60 * 5;
    private static final int MAX_OUTPUT = 2048;

    private ResourceContext<? extends ResourceComponent<?>> resourceContext;

    public HadoopOperationsDelegate(ResourceContext<? extends ResourceComponent<?>> resourceContext) {
        this.resourceContext = resourceContext;
    }

    public OperationResult invoke(@NotNull
    HadoopSupportedOperations operation, Configuration parameters, String serverType) throws InterruptedException {

        ProcessExecutionResults results = null;
        switch (operation) {
        case FORMAT:
            throw new UnsupportedOperationException("This operation requires user interaction.");
            //            results = format(operation);
            //            break;
        case FSCK:
            results = fsck(operation);
            break;
        case LS:
            results = ls(operation);
            break;
        case START:
            results = start(operation, serverType);
            break;
        case STOP:
            results = stop(operation, serverType);
            break;
        case QUEUE_LIST:
            results = queueList(operation);
            break;
        case JOB_LIST_RUNNING:
            results = invokeGeneralOperation(operation);
            break;
        case JOB_LIST_ALL:
            results = invokeGeneralOperation(operation);
            break;
        case REBALANCE_DFS:
            results = invokeGeneralOperation(operation);
            break;
        case KILL:
            results = invokeGeneralOperation(operation, parameters, null);
            break;
        case JAR:
            results = invokeGeneralOperation(operation, parameters, null);
            break;
        default:
            throw new UnsupportedOperationException(operation.toString());
        }

        String message = truncateString(results.getCapturedOutput());
        if (LOG.isDebugEnabled()) {
            LOG.debug("CLI results: exitcode=[" + results.getExitCode() + "]; error=[" + results.getError()
                + "]; output=" + message);
        }

        OperationResult result = new OperationResult(message);
        return result;
    }

    /**
     * @param operation supported Hadoop operation @see HadoopSupportedOperations
     * @return the object encapsulating the exit code, err output and std output
     */
    private ProcessExecutionResults queueList(HadoopSupportedOperations operation) {
        return invokeGeneralOperation(operation);
    }

    /**
     * @param operation supported Hadoop operation @see HadoopSupportedOperations
     * @param serverType {Name|Data|SecondaryName}Node / {Job|Task}Tracker
     * @return the object encapsulating the exit code, err output and std output
     */
    private ProcessExecutionResults stop(HadoopSupportedOperations operation, String serverType) {
        return invokeGeneralOperation(operation, null, serverType);
    }

    /**
     * @param operation supported Hadoop operation @see HadoopSupportedOperations
     * @param serverType {Name|Data|SecondaryName}Node / {Job|Task}Tracker
     * @return the object encapsulating the exit code, err output and std output
     */
    private ProcessExecutionResults start(HadoopSupportedOperations operation, String serverType) {
        return invokeGeneralOperation(operation, null, serverType);
    }

    /**
     * Format a new distributed filesystem
     * by running $bin/hadoop namenode -format
     * 
     * @return the object encapsulating the exit code, err output and std output
     */
    private ProcessExecutionResults format(HadoopSupportedOperations operation) {
        return invokeGeneralOperation(operation);
    }

    /**
     * @param operation supported Hadoop operation @see HadoopSupportedOperations
     * @return the object encapsulating the exit code, err output and std output
     */
    private ProcessExecutionResults ls(HadoopSupportedOperations operation) {
        return invokeGeneralOperation(operation);
    }

    /**
     * @param operation supported Hadoop operation @see HadoopSupportedOperations
     * @return the object encapsulating the exit code, err output and std output
     */
    private ProcessExecutionResults fsck(HadoopSupportedOperations operation) {
        return invokeGeneralOperation(operation);
    }

    /**
     * Executes the process and returns the exit code, err output and std output
     * 
     * @param sysInfo instance of SystemInfo
     * @param executable String with path to executable file
     * @param args String with arguments passed to the executable file
     * @param wait Max time to wait in milliseconds
     * @param captureOutput Whether or not to capture the output
     * @param killOnTimeout Whether or not to kill the process after the timeout
     * @return the object encapsulating the exit code, err output and std output
     * @throws InvalidPluginConfigurationException
     */
    private static ProcessExecutionResults executeExecutable(@NotNull
    SystemInfo sysInfo, String executable, String args, long wait, boolean captureOutput, boolean killOnTimeout)
        throws InvalidPluginConfigurationException {

        ProcessExecution processExecution = new ProcessExecution(executable);
        if (args != null) {
            processExecution.setArguments(args.split("[ \\t\\n]+"));
        }
        processExecution.setWaitForCompletion(wait);
        processExecution.setCaptureOutput(captureOutput);
        processExecution.setKillOnTimeout(killOnTimeout);

        ProcessExecutionResults results = sysInfo.executeProcess(processExecution);

        return results;
    }

    /**
     * Truncate a string so it is short, usually for display or logging purposes.
     * 
     * @param output the output to trim
     * @return the trimmed output
     */
    private String truncateString(String output) {
        String outputToLog = output;
        if (outputToLog != null && outputToLog.length() > MAX_OUTPUT) {
            outputToLog = outputToLog.substring(0, MAX_OUTPUT) + "...";
        }
        return outputToLog;
    }

    private ProcessExecutionResults invokeGeneralOperation(HadoopSupportedOperations operation) {
        return invokeGeneralOperation(operation, null, null);
    }

    private ProcessExecutionResults invokeGeneralOperation(HadoopSupportedOperations operation, Configuration parameters, String serverType) {
        String hadoopHome = resourceContext.getPluginConfiguration()
            .getSimple(HadoopServerDiscovery.HOME_DIR_PROPERTY).getStringValue();
        String executable = hadoopHome + operation.getRelativePathToExecutable();
        String args = operation.getArgs() + (serverType == null ? "" : serverType.toLowerCase());
        if (operation.getClass() != null) {
            for (String paramName : operation.getParamsNames()) {
                args += " " + parameters.getSimpleValue(paramName);
            }
        }

        ProcessExecutionResults results = executeExecutable(resourceContext.getSystemInformation(), executable, args,
            MAX_WAIT, true, operation.isKillOnTimeout());
        return results;
    }
}
