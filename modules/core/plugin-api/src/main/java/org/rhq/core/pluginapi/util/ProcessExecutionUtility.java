/*
  * RHQ Management Platform
  * Copyright (C) 2005-2012 Red Hat, Inc.
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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

import org.rhq.core.system.ProcessExecution;

/**
 * A set of utility methods for creating {@link ProcessExecution}s.
 *
 * @author Ian Springer
 */
public class ProcessExecutionUtility {

    private static final boolean OS_IS_WINDOWS = (File.separatorChar == '\\');

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
     * @param  prefix a prefix command line that should be prepended to the executable's command line
     *                (e.g. "/usr/bin/nohup /usr/bin/sudo -u jboss -g jboss"). any files on the
     *                command line should be absolute paths. if null, no prefix command line will be
     *                prepended
     * @param  file an executable or a batch file
     *
     * @return a process execution
     */
    public static ProcessExecution createProcessExecution(String prefix, File file) {
        ProcessExecution processExecution;

        List<String> prefixArgs;
        if (prefix != null) {
            // TODO (ips, 04/27/10): Ideally, the prefix should be a String[], not a String.
            prefixArgs = Arrays.asList(prefix.split("[ \t]+"));
        } else {
            prefixArgs = Collections.emptyList();
        }
        String executable;
        List<String> args = new ArrayList<String>();
        if (OS_IS_WINDOWS && isBatchFile(file)) {
            // Windows batch files cannot be executed directly - they must be passed as arguments to cmd.exe, e.g.
            // "C:\Windows\System32\cmd.exe /c C:\opt\jboss-as\bin\run.bat".
            executable = getCmdExeFile().getPath();
            args.add("/c");
            args.addAll(prefixArgs);
            args.add(file.getPath());
        } else {
            // UNIX
            if (prefixArgs.isEmpty()) {
                executable = file.getPath();
            } else {
                executable = prefixArgs.get(0);
                if (prefixArgs.size() > 1) {
                    args.addAll(prefixArgs.subList(1, prefixArgs.size()));
                }
                args.add(file.getPath());
            }
        }

        processExecution = new ProcessExecution(executable);
        processExecution.setArguments(args);

        // Start out with a copy of our own environment, since Windows needs
        // certain system environment variables to find DLLs, etc., and even
        // on UNIX, many scripts will require certain environment variables
        // (PATH, JAVA_HOME, etc.).
        // TODO (ips, 04/27/12): We probably should not just do this by default.
        Map<String, String> envVars = new LinkedHashMap<String, String>(System.getenv());
        processExecution.setEnvironmentVariables(envVars);

        // Many scripts (e.g. JBossAS scripts) assume their working directory is the directory containing the script.
        processExecution.setWorkingDirectory(file.getParent());

        return processExecution;
    }

    private static boolean isBatchFile(File file) {
        return file.getName().matches(".*\\.((bat)|(cmd))$(?i)");
    }

    private static File getCmdExeFile() {
        String cmdExe = System.getenv("COMSPEC");
        if (cmdExe == null) {
            throw new RuntimeException("COMSPEC environment variable is not defined.");
            // TODO: Try to find cmd.exe by checking the various usual locations.
        }

        File cmdExeFile = new File(cmdExe);
        if (!cmdExeFile.exists()) {
            throw new RuntimeException("COMSPEC environment variable specifies a non-existent path: " + cmdExe);
        }

        return cmdExeFile;
    }

}