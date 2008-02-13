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
package org.rhq.core.util.exec;

/**
 * The results of a process execution.
 *
 * @author John Mazzitelli
 * @see    ProcessExecutor
 * @see    ProcessToStart
 */
public class ProcessExecutorResults {
    private Integer exitCode;
    private Throwable error;

    /**
     * The exit code of the process. Note that if the {@link ProcessToStart} did not want to wait for the process to
     * exit, the returned value will be <code>null</code>. The returned value will also be <code>null</code> if the
     * process failed to start; in which case, you can get the {@link #getError()} that caused the failure.
     *
     * @return the exit code of the process; <code>null</code> if the process failed to start or the we didn't wait for
     *         the process to exit
     */
    public Integer getExitCode() {
        return exitCode;
    }

    public void setExitCode(Integer code) {
        exitCode = code;
    }

    /**
     * If the process failed to start, this will indicate the error that occurred.
     *
     * @return start error (may be <code>null</code> if the process started successfully)
     */
    public Throwable getError() {
        return error;
    }

    public void setError(Throwable t) {
        error = t;
    }

    public String toString() {
        return "ProcessExecResults: exit-code=[" + exitCode + "], error=[" + error + "]";
    }
}