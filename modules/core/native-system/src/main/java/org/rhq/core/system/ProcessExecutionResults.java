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

import java.io.ByteArrayOutputStream;

/**
 * Encapsulates the results of a process that was executed via {@link SystemInfo#executeProcess(ProcessExecution)}.
 *
 * @author John Mazzitelli
 */
public class ProcessExecutionResults {
    private Integer exitCode;
    private Throwable error;
    private ByteArrayOutputStream output;

    /**
     * If the process finished, this is its exit code. Its numeric value has specific meaning that is custom to the
     * operating system and the program that was executed.
     *
     * <p>This will be <code>null</code> if the process was never waited on or if the wait time expired before the
     * process exited.</p>
     *
     * @return process exit code or <code>null</code> if it could not be determined
     *
     * @see    SystemInfo#executeProcess(ProcessExecution)
     */
    public Integer getExitCode() {
        return exitCode;
    }

    void setExitCode(Integer exitCode) {
        this.exitCode = exitCode;
    }

    /**
     * An error that occurred, typically due to a startup failure.
     *
     * @return error that occurred while starting the process; <code>null</code> if no known error occurred
     */
    public Throwable getError() {
        return error;
    }

    void setError(Throwable error) {
        this.error = error;
    }

    /**
     * Returns the full output of the process as a <code>String</code>. This returns <code>null</code> if the process's
     * output was not captured.
     *
     * @return the full output of the process or <code>null</code>
     */
    public String getCapturedOutput() {
        if (output == null) {
            return null;
        }

        return output.toString();
    }

    void setCapturedOutputStream(ByteArrayOutputStream output) {
        this.output = output;
    }

    @Override
    public String toString() {
        return "ProcessExecutionResults: exit-code=[" + exitCode + "], error=[" + error + "]";
    }
}