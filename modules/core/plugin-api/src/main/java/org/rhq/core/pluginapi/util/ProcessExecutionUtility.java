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
package org.rhq.core.pluginapi.util;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Ian Springer
 */
import org.rhq.core.system.ProcessExecution;

public class ProcessExecutionUtility {
    private ProcessExecutionUtility() {
    }

    /**
     * Creates a ProcessExecution for the specified file for the current platform. If the current platform is Windows
     * and the file name ends with ".bat" or ".cmd", the file will be assumed to be a Windows batch file, and the
     * process execution will be initialized accordingly. Note, if the file is a UNIX script, its first line must be a
     * valid #! reference to a script interpreter (e.g. #!/bin/sh), otherwise it will fail to execute. The returned
     * ProcessExecution will have a non-null arguments list, an environment map that is a copy of the current process's
     * environment, and a working directory set to its executable's parent directory.
     *
     * @param  file an executable or a batch file
     *
     * @return a process execution
     */
    public static ProcessExecution createProcessExecution(File file) {
        ProcessExecution processExecution;
        if (isWindows() && isBatchFile(file)) {
            String comSpec = System.getenv("COMSPEC");
            if (comSpec == null) {
                throw new RuntimeException("COMSPEC environment variable is not defined.");
                // TODO: Try to find cmd.exe by checking the various usual locations.
            }

            if (!new File(comSpec).exists()) {
                throw new RuntimeException("COMSPEC environment variable specifies a non-existent path: " + comSpec);
            }

            processExecution = new ProcessExecution(comSpec);
            processExecution.setArguments(new String[] { "/C", file.getPath(), });
        } else {
            processExecution = new ProcessExecution(file.getPath());
            processExecution.setArguments(new ArrayList<String>());
        }

        // Start out with a copy of our own environment, since Windows needs
        // certain system environment variables to find DLLs, etc., and even
        // on UNIX, many scripts will require certain environment variables
        // (PATH, JAVA_HOME, etc.).
        Map<String, String> envVars = new LinkedHashMap(System.getenv());
        processExecution.setEnvironmentVariables(envVars);

        // Many scripts (e.g. JBossAS scripts) assume their working directory is their parent directory.
        processExecution.setWorkingDirectory(file.getParent());

        return processExecution;
    }

    private static boolean isBatchFile(File file) {
        return file.getName().matches(".*\\.((bat)|(cmd))$(?i)");
    }

    private static boolean isWindows() {
        return File.separatorChar == '\\';
    }
}