/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2015 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */
package org.rhq.server.control.util;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;

import org.rhq.core.util.exec.ProcessExecutionOutputStream;

@SuppressWarnings("rawtypes")
public class ExecutorAssist {

    private static ExecutorAssist defaultAssit;
    private static ExecutorAssist silentAssist;
    /**
     * @return default executor assist
     */
    public static ExecutorAssist getDefault() {
        if (defaultAssit == null) {
            defaultAssit = new ExecutorAssist();
        }
        return defaultAssit;
    }

    /**
     * @return silent executor assist (does not capture child process output streams)
     */
    public static ExecutorAssist getSilent() {
        if (silentAssist == null) {
            silentAssist = new ExecutorAssist();
            silentAssist.executor.setStreamHandler(
                new PumpStreamHandler(
                    new ProcessExecutionOutputStream(0, false),
                    new ProcessExecutionOutputStream(0, false)));
        }
        return silentAssist;
    }

    private final Executor executor;

    private ExecutorAssist() {
        executor = new DefaultExecutor();
        executor.setStreamHandler(new PumpStreamHandler());
    }

    public int exec(final File workingDir, final CommandLine commandLine) throws IOException {
        return exec(workingDir, commandLine, null);
    }

    public int exec(final File workingDir, final CommandLine commandLine, final Map environment) throws IOException {
        int rValue = 0;
        try {
            // Synchronized to prevent two threads from setting different workingDirectory at the same time..
            synchronized(executor) {
                executor.setWorkingDirectory(workingDir);
                // null environment is fine in both cases, DefaultExecutor will use default environment in that case
                rValue = executor.execute(commandLine, environment);
            }
        } catch(ExecuteException e) {
            // DefaultExecutor has no detailed exception text, safe to ignore
            rValue = Math.max(e.getExitValue(), rValue);
        }
        return rValue;
    }

    // static methods for compatibility (using default ExecutorAssist)

    public Future<Integer> execAsync(final File workingDir, final CommandLine commandLine, final Map environment) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        return executorService.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return exec(workingDir, commandLine, environment);
            }
        });
    }

    public static int execute(final File workingDir, final CommandLine commandLine, final Map environment)
        throws IOException {
        return getDefault().exec(workingDir, commandLine, environment);
    }

    public static Future<Integer> executeAsync(final File workingDir, final CommandLine commandLine,
        final Map environment) {
        return getDefault().execAsync(workingDir, commandLine, environment);
    }

    public static int execute(final File workingDir, final CommandLine commandLine) throws IOException {
        return getDefault().exec(workingDir, commandLine);
    }

}
