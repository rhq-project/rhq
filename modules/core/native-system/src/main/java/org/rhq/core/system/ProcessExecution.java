/*
 * RHQ Management Platform
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
package org.rhq.core.system;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.rhq.core.util.exec.ProcessExecutionOutputStream;

/**
 * Provides information on what process to execute and how to execute it.
 * <p/>
 * Agent plugin developers should also see the ProcessExecutionUtility class in the plugin-api module, which provides
 * handy methods for creating <code>ProcessExecution</code>s.
 *
 * @author John Mazzitelli
 * @author Ian Springer
 * @author Jay Shaughnessy
 *
 * @see org.rhq.core.system.JavaSystemInfo#executeProcess(ProcessExecution)
 */
public class ProcessExecution {

    private String executable;
    private List<String> arguments;
    private Map<String, String> environmentVariables;
    private String workingDirectory;
    private long waitForCompletion = 30000L;
    private boolean killOnTimeout = false;
    private boolean checkExecutableExists = true;
    private CaptureMode captureMode = CaptureMode.none();

    /**
     * Constructor for {@link ProcessExecution} that defines the full path to the executable that will be run. See the
     * other setter methods in this class for the additional things you can set when executing a process.
     *
     * @param executable the full path to the executable that will be run
     */
    public ProcessExecution(String executable) {
        if (executable == null)
            throw new IllegalArgumentException("executable cannot be null");

        setExecutable(executable);
    }

    @NotNull
    public String getExecutable() {
        return executable;
    }

    /**
     * Sets the full path to the executable that will be run.
     *
     * @param executable the full path to the executable that will be run
     */
    public void setExecutable(@NotNull
    String executable) {
        this.executable = executable;
    }

    /**
     * Obtain the optional set of arguments to the executable as List.
     * @return List of arguments or null if no arguments are set.
     */
    @Nullable
    public List<String> getArguments() {
        return arguments;
    }

    /**
     * Obtain the optional set of arguments to the executable as String array
     * @return Array of arguments or null if no arguments are set.
     */
    @Nullable
    public String[] getArgumentsAsArray() {
        String[] argArray;
        if (this.arguments == null) {
            argArray = null;
        } else {
            argArray = this.arguments.toArray(new String[this.arguments.size()]);
        }

        return argArray;
    }

    /**
     * Sets an optional set of arguments to pass to the executable.
     * <p/>
     * Windows Note! This will overwrite internal arguments set by the constructor. Use {@link #addArguments} on Windows.
     *  
     * @param arguments an optional set of arguments to pass to the executable
     */
    public void setArguments(@Nullable
    List<String> arguments) {
        this.arguments = arguments;
    }

    /**
     * Adds an optional set of arguments to the current arguments passed to the executable.
     *  
     * @param arguments an optional set of arguments to pass to the executable. Not null.
     */
    public void addArguments(List<String> arguments) {
        if (null == this.arguments) {
            this.arguments = arguments;
        } else {
            this.arguments.addAll(arguments);
        }
    }

    /**
     * Sets an optional set of arguments to pass to the executable.
     *
     * @param arguments an optional set of arguments to pass to the executable
     */
    public void setArguments(@Nullable
    String[] arguments) {
        this.arguments = new ArrayList<String>(Arrays.asList(arguments));
    }

    @Nullable
    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }

    /**
     * Returns a copy of this ProcessExecution's environment variables as an array of "name=value" Strings. Note, since
     * the array is only a copy of the environmentVariables property, modifications made to it will have no effect on
     * this ProcessExecution.
     *
     * @return a copy of this ProcessExecution's environment variables as an array of "name=value" Strings
     */
    @Nullable
    public String[] getEnvironmentVariablesAsArray() {
        String[] envVarArray;
        if (this.environmentVariables == null) {
            envVarArray = null;
        } else {
            envVarArray = new String[this.environmentVariables.size()];
            int i = 0;
            for (String varName : this.environmentVariables.keySet()) {
                envVarArray[i++] = varName + "=" + this.environmentVariables.get(varName);
            }
        }

        return envVarArray;
    }

    /**
     * Sets an optional set of environment variables to pass to the process. If <code>null</code>, the new process will
     * inherit the environment of the caller.
     *
     * @param environmentVariables an optional set of environment variables to pass to the process
     */
    public void setEnvironmentVariables(@Nullable
    Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    @Nullable
    public String getWorkingDirectory() {
        return workingDirectory;
    }

    /**
     * If not <code>null</code>, will be the working directory of the new process (if <code>null</code>, the new
     * process's working directory will be the current working directory of caller).
     *
     * @param workingDirectory The directory the process should get as working directory.
     */
    public void setWorkingDirectory(@Nullable
    String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public long getWaitForCompletion() {
        return waitForCompletion;
    }

    /**
     * The time, in milliseconds, to wait for the process to exit (will not wait if 0 or less).
     *
     * @param waitForCompletion The wait time in ms.
     */
    public void setWaitForCompletion(long waitForCompletion) {
        this.waitForCompletion = waitForCompletion;
    }

    /**
     * 
     * @return whether capture process output
     * @deprecated
     */
    public boolean isCaptureOutput() {
        return this.captureMode.isCapture();
    }

    /**
     * If <code>true</code>, the process's output will be captured and returned in the results. This may be ignored if
     * <code>waitForCompletion</code> is 0 or less. Be careful setting this to <code>true</code>, you must ensure that
     * the process will not write a lot of output - you might run out of memory if the process is a long-lived daemon
     * process that outputs a lot of log messages, for example. By default, output is *not* captured.
     * @deprecated @see {@link #setCaptureMode(CaptureMode)}
     * @param captureOutput whether or not this process's output (stdout+stderr) should be captured and returned in the
     *                      results
     */
    public void setCaptureOutput(boolean captureOutput) {
        this.captureMode = captureOutput ? CaptureMode.memory() : CaptureMode.none();
    }

    /**
     * get process output capture mode
     * @return process output capture mode
     */
    public CaptureMode getCaptureMode() {
        return captureMode;
    }

    public void setCaptureMode(CaptureMode captureMode) {
        this.captureMode = captureMode;
    }

    public boolean isKillOnTimeout() {
        return killOnTimeout;
    }

    /**
     * If <code>true</code>, then the process will be forcibly killed if it doesn't exit within the
     * {@link #getWaitForCompletion() wait time}. If <code>false</code>, the process will be allowed to continue to run
     * for as long as it needs - {@link #getWaitForCompletion()} will only force the caller to "wake up" and not block
     * waiting for the process to finish.
     *
     * @param killOnTimeout Should the process be killed after the timeout timed out?
     */
    public void setKillOnTimeout(boolean killOnTimeout) {
        this.killOnTimeout = killOnTimeout;
    }

    /**
     * If <code>true</code>, then the executable should first be checked for its existence.
     * If the executable does not exist, the execution should fail-fast. If <code>false</code>,
     * the process will attempt to be executed no matter what. This will allow the operating
     * system to check its executable PATH to find the executable as necessary.
     *
     * @return check flag (default is <code>true</code>)
     */
    public boolean isCheckExecutableExists() {
        return checkExecutableExists;
    }

    public void setCheckExecutableExists(boolean checkExecutableExists) {
        this.checkExecutableExists = checkExecutableExists;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("ProcessExecution: ");

        buf.append("executable=[").append(this.executable);
        buf.append("], args=[").append(this.arguments);
        buf.append("], env-vars=[").append(this.environmentVariables);
        buf.append("], working-dir=[").append(this.workingDirectory);
        buf.append("], wait=[").append(this.waitForCompletion);
        buf.append("], capture-mode=[").append(this.captureMode);
        buf.append("], kill-on-timeout=[").append(this.killOnTimeout);
        buf.append("], executable-is-command=[").append(this.checkExecutableExists);
        buf.append("]");

        return buf.toString();
    }

    /**
     * Process output capture mode.
     * 
     * @author lzoubek@redhat.com
     *
     */
    public static class CaptureMode {

        /**
         * The process's output is *not* captured, this is the default.
         * @return captureMode
         */
        public static CaptureMode none() {
            return new CaptureMode(false);
        }

        /**
         * The process's output will be captured and returned in the results. This may be ignored if
         * <code>waitForCompletion</code> is 0 or less. By default capturing to memory is limited to 2MB of
         * process output. If the process writes more output, it will be ignored.
         * @return captureMode
         */
        public static CaptureMode memory() {
            return new CaptureMode(true);
        }

        /**
         * The process's output will be captured and returned in the results. This may be ignored if
         * <code>waitForCompletion</code> is 0 or less. With <code>limit</code> parameter you can set maximum captured output size. 
         * If the process writes more output, it will be ignored.
         * 
         * @param limit in Bytes (if given value < 0, it's ignored and default 2MB is used instead)
         * @return captureMode
         */
        public static CaptureMode memory(int limit) {
            return new CaptureMode(true, limit);
        }

        /**
         * The process's output will be captured and returned in the results. This may be ignored if
         * <code>waitForCompletion</code> is 0 or less. Process output will be redirected to agent.log and at the same time
         * captured into memory. By default capturing to memory is limited to 2MB of process output. If the process writes more output,
         * it will only be redirected to agent.log.
         * 
         * @return captureMode
         */
        public static CaptureMode agentLog() {
            return new CaptureMode(true, true, -1);
        }

        /**
         * The process's output will be captured and returned in the results. This may be ignored if
         * <code>waitForCompletion</code> is 0 or less. Process output will be logged into agent.log and at the same time
         * captured into memory. With <code>limit</code> parameter you can set maximum memory buffer to be captured (and possibly returned)
         * captured output size. If the process writes more output, it will only be redirected to agent.log.
         * 
         * @param limit in Bytes (if given value < 0, it's ignored and default 2MB is used instead)
         * @return captureMode
         */
        public static CaptureMode agentLog(int limit) {
            return new CaptureMode(true, true, limit);
        }

        private final boolean capture;
        private final int limit;
        private final boolean log;

        private CaptureMode(boolean capture) {
            this(capture, -1);
        }

        private CaptureMode(boolean capture, int limit) {
            this(capture, false, limit);
        }

        private CaptureMode(boolean capture, boolean log, int limit) {
            this.capture = capture;
            this.log = log;
            this.limit = limit;
        }

        /**
         * 
         * @return true if capturing is enabled
         */
        public boolean isCapture() {
            return capture;
        }

        /**
         * 
         * @return captured output size limit in Bytes, -1 if default should be used 
         */
        public int getLimit() {
            return limit;
        }

        /**
         * 
         * @return true if output should be forwarded to logging subsystem
         */
        public boolean isLog() {
            return log;
        }

        ProcessExecutionOutputStream createOutputStream() {
            if (!this.capture) {
                // capturing is disabled still return some output stream (this instance ignores everything)
                return new ProcessExecutionOutputStream(0, this.log);
            }
            if (this.limit > 0) {
                return new ProcessExecutionOutputStream(this.limit, this.log);
            }
            return new ProcessExecutionOutputStream(this.log);
        }

        @Override
        public String toString() {
            return new StringBuilder("CaptureMode: ")
            .append(" [capture="+isCapture())
            .append("], [memory-limit=" + getLimit() / 1024 + "kB")
            .append("], [log="+isLog())
            .append("]")
            .toString();
        }
    }

}