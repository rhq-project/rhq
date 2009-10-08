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
package org.rhq.core.pluginapi.util;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.List;

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
        return createProcessExecution((String) null, file);
    }

    /**
     * Creates a ProcessExecution for the specified file for the current platform. If the current platform is Windows
     * and the file name ends with ".bat" or ".cmd", the file will be assumed to be a Windows batch file, and the
     * process execution will be initialized accordingly. Note, if the file is a UNIX script, its first line must be a
     * valid #! reference to a script interpreter (e.g. #!/bin/sh), otherwise it will fail to execute. The returned
     * ProcessExecution will have a non-null arguments list, an environment map that is a copy of the current process's
     * environment, and a working directory set to its executable's parent directory.
     *
     * @param  prefix a command prefix applied prior to <code>file</code> execution. Typically a <code>sudo</code>
     *                command. Ignored if null.
     * @param  file an executable or a batch file
     *
     * @return a process execution
     */
    public static ProcessExecution createProcessExecution(String prefix, File file) {
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
            if (null == prefix) {
                processExecution.setArguments(new String[] { "/C", file.getPath(), });
            } else {
                processExecution.setArguments(new String[] { "/C", prefix, file.getPath(), });
            }
        } else {
            if (null == prefix) {
                processExecution = new ProcessExecution(file.getPath());
                processExecution.setArguments(new ArrayList<String>());
            } else {
                List<String> arguments = new ArrayList<String>();

                StringTokenizer prefixTokenizer = new StringTokenizer(prefix);
                String processName = prefixTokenizer.nextToken();

                while (prefixTokenizer.hasMoreTokens()) {
                    String prefixArgument = prefixTokenizer.nextToken();
                    arguments.add(prefixArgument);
                }

                arguments.add(file.getPath());

                processExecution = new ProcessExecution(processName);
                processExecution.setArguments(arguments);
            }
        }

        // Start out with a copy of our own environment, since Windows needs
        // certain system environment variables to find DLLs, etc., and even
        // on UNIX, many scripts will require certain environment variables
        // (PATH, JAVA_HOME, etc.).
        Map<String, String> envVars = new LinkedHashMap<String, String>(System.getenv());
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