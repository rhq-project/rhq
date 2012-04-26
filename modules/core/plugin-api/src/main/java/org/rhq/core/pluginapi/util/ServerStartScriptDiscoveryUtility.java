/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hyperic.sigar.ProcExe;

import org.rhq.core.system.ProcessInfo;

/**
 * A set of utility methods for initializing the three plugin configuration properties used by a plugin for starting the
 * managed server - "startScript", "startScriptEnv", and "startScriptArgs". See also {@link StartScriptConfiguration}.
 *
 * @author Ian Springer
 */
public class ServerStartScriptDiscoveryUtility {

    // Generic OS-level env vars that should be in every process's environment.
    private static final Set<String> CORE_ENV_VAR_NAME_INCLUDES = new HashSet<String>(Arrays.asList(
        "PATH",
        "LD_LIBRARY_PATH"
    ));
    static {
        if (File.separatorChar == '\\') {
            CORE_ENV_VAR_NAME_INCLUDES.add("OS"); // many batch files use this to figure out if the OS type is NT or 9x
            CORE_ENV_VAR_NAME_INCLUDES.add("SYSTEMROOT"); // required on Windows to avoid winsock create errors
        }
    }


    private ServerStartScriptDiscoveryUtility() {
    }

    public static File getStartScript(ProcessInfo serverParentProcess) {
        // e.g. UNIX:    "/bin/sh ./standalone.sh --server-config=standalone-full.xml"
        //      Windows: "cmd.exe [options] standalone.bat --server-config=standalone-full.xml"
        int startScriptIndex = 1;
        String[] serverParentProcessCommandLine = serverParentProcess.getCommandLine();

        // advance past cmd.exe options. options start with '/'       
        if (File.separatorChar == '\\') {
            for (; (startScriptIndex < serverParentProcessCommandLine.length); ++startScriptIndex) {
                if (!serverParentProcessCommandLine[startScriptIndex].startsWith("/")) {
                    break;
                }
            }
        }

        String startScript = (serverParentProcessCommandLine.length > startScriptIndex) ? serverParentProcessCommandLine[startScriptIndex]
            : null;

        File startScriptFile;

        if (isScript(startScript)) {
            // The parent process is a script - excellent!
            startScriptFile = new File(startScript);
            if (!startScriptFile.isAbsolute()) {
                ProcExe parentProcessExe = serverParentProcess.getExecutable();
                if (parentProcessExe == null) {
                    startScriptFile = new File("bin", startScriptFile.getName());
                } else {
                    String cwd = parentProcessExe.getCwd();
                    startScriptFile = new File(cwd, startScriptFile.getPath());
                    startScriptFile = new File(FileUtils.getCanonicalPath(startScriptFile.getPath()));
                }
            }
        } else {
            // The parent process is not a script - either the user started the server via some other mechanism, or the
            // script process got killed.
            startScriptFile = null;
        }

        return startScriptFile;
    }

    public static List<String> getStartScriptArgs(ProcessInfo serverParentProcess, List<String> serverArgs,
        Set<CommandLineOption> optionExcludes) {
        // e.g. UNIX:    "/bin/sh ./standalone.sh --server-config=standalone-full.xml"
        //      Windows: "standalone.bat --server-config=standalone-full.xml"
        int startScriptIndex = (File.separatorChar == '/') ? 1 : 0;
        String[] startScriptCommandLine = serverParentProcess.getCommandLine();
        String startScript = (startScriptCommandLine.length > startScriptIndex) ? startScriptCommandLine[startScriptIndex]
            : null;

        List<String> startScriptArgs = new ArrayList<String>();
        if (isScript(startScript)) {
            // Skip past the script to get the arguments that were passed to the script.
            for (int i = (startScriptIndex + 1); i < startScriptCommandLine.length; i++) {
                startScriptArgs.add(startScriptCommandLine[i]);
            }
        } else {
            for (int i = 0, serverArgsSize = serverArgs.size(); i < serverArgsSize; i++) {
                String serverArg = serverArgs.get(i);
                // Skip any options that the start script will take care of specifying.
                CommandLineOption option = null;
                for (CommandLineOption optionExclude : optionExcludes) {
                    if ((optionExclude.getShortName() != null && (serverArg.equals('-' + optionExclude.getShortName()) || serverArg
                        .startsWith('-' + optionExclude.getShortName() + "=")))
                        || ((optionExclude.getLongName() != null) && (serverArg.equals("--"
                            + optionExclude.getLongName()) || serverArg.startsWith("--" + optionExclude.getLongName()
                            + "=")))) {
                        option = optionExclude;
                        break;
                    }
                }
                if (option != null) {
                    if (option.isExpectsValue()
                        && ((i + 1) < serverArgsSize)
                        && (((option.getShortName() != null) && serverArg.equals('-' + option.getShortName())) || (option
                            .getLongName() != null) && serverArg.equals("--" + option.getLongName()))) {
                        // If the option expects a value and the delimiter is a space, skip the next argument too.
                        i++;
                    }
                } else {
                    startScriptArgs.add(serverArg);
                }
            }
        }
        return startScriptArgs;
    }

    public static Map<String, String> getStartScriptEnv(ProcessInfo serverProcess, ProcessInfo serverParentProcess,
        Set<String> envVarNameIncludes) {
        Map<String, String> processEnvVars;
        if (getStartScript(serverParentProcess) != null) {
            processEnvVars = serverParentProcess.getEnvironmentVariables();
        } else {
            processEnvVars = serverProcess.getEnvironmentVariables();
        }

        List<String> fullEnvVarNameIncludes = new ArrayList<String>(envVarNameIncludes);
        // Add the core includes at the end of the list, since end users will probably be more interested in the
        // app-specific env vars.
        fullEnvVarNameIncludes.addAll(CORE_ENV_VAR_NAME_INCLUDES);

        // Use a linked hashmap to maintain the order of the includes list.
        Map<String, String> startScriptEnv = new LinkedHashMap<String, String>();
        for (String envVarName : fullEnvVarNameIncludes) {
            String envVarValue = processEnvVars.get(envVarName);
            if (envVarValue != null) {
                startScriptEnv.put(envVarName, envVarValue);
            }
        }
        return startScriptEnv;
    }

    private static boolean isScript(String startScript) {
        // TODO: What if CygWin was used to start AS7 on Windows via a shell script?
        return (startScript != null) && (startScript.endsWith(".sh") || startScript.endsWith(".bat"));
    }

}
