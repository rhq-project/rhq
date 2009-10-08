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
package org.rhq.enterprise.communications.command.impl.start.server;

import org.rhq.core.util.exec.ProcessExecutorResults;
import org.rhq.core.util.exec.ProcessToStart;
import org.rhq.enterprise.communications.command.impl.start.StartCommand;
import org.rhq.enterprise.communications.command.impl.start.StartCommandResponse;

/**
 * Used to execute a process. Uses {@link StartCommand} as the object used to encapsulate the metadata about the process
 * to start.
 *
 * <p><b>Warning: caution should be exercised when using this class - it allows any process to be started with no
 * security restrictions.</b></p>
 *
 * @author John Mazzitelli
 */
public class ProcessExec {
    /**
     * This executes any operating system process as described in the given start command. When this method returns, it
     * can be assumed that the process was launched but not necessarily finished. The caller can ask this method to
     * block until process exits by setting {@link StartCommand#getWaitForExit()}. On error, the exception will be
     * returned in the returned response.
     *
     * <p>Subclasses may override this method to accept different types of commands; they just need to ensure that if
     * they call this super method, that the command is of type {@link StartCommand}.</p>
     *
     * @param  command the command that contains metadata about the process to start
     *
     * @return the results of the command execution
     */
    public StartCommandResponse execute(StartCommand command) {
        org.rhq.core.util.exec.ProcessExecutor exec = new org.rhq.core.util.exec.ProcessExecutor();
        StartCommandResponse response;

        try {
            StartCommand startCommand = new StartCommand(command);
            ProcessToStart process = new ProcessToStart();

            process.setArguments(command.getArguments());
            process.setBackupOutputFile(command.isBackupOutputFile());
            process.setCaptureOutput(command.isCaptureOutput());
            process.setEnvironment(command.getEnvironment());
            process.setInputDirectory(command.getInputDirectory());
            process.setInputFile(command.getInputFile());
            process.setOutputDirectory(command.getOutputDirectory());
            process.setOutputFile(command.getOutputFile());
            process.setProgramDirectory(command.getProgramDirectory());
            process.setProgramExecutable(command.getProgramExecutable());
            process.setProgramTitle(command.getProgramTitle());
            process.setWaitForExit(command.getWaitForExit());
            process.setWorkingDirectory(command.getWorkingDirectory());

            ProcessExecutorResults results = exec.execute(process);
            Integer exitCode = results.getExitCode();
            Throwable error = results.getError();

            if (error == null) {
                response = new StartCommandResponse(startCommand, exitCode);
            } else {
                response = new StartCommandResponse(startCommand, error);
            }
        } catch (Exception e) {
            response = new StartCommandResponse(command, e);
        }

        return response;
    }
}