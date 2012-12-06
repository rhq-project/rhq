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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.rhq.core.system.ProcessInfo;

/**
 * A set of utility methods that server discovery components can use to discover the values required to later restart
 * the server process via a start script. The values can then be stored in or retrieved from the server's plugin
 * configuration using the {@link StartScriptConfiguration} plugin configuration wrapper.
 *
 * @author Ian Springer
 */
public class ServerStartScriptDiscoveryUtility {

    private static final boolean OS_IS_WINDOWS = (File.separatorChar == '\\');
    private static final char OPTION_PREFIX = (OS_IS_WINDOWS) ? '/' : '-';

    // Generic OS-level PATH setting for LINUX. For Windows the PATH must be generated when we have
    // the system env vars. It will be of the form %SystemRoot%\\system32;%SystemRoot%; 
    private static final String CORE_ENV_VAR_PATH_UNIX = "/bin:/usr/bin";

    // Generic OS-level env vars that should be in every process's environment.
    private static final Set<String> CORE_ENV_VAR_NAME_INCLUDES = new HashSet<String>(Arrays.asList("PATH",
        "LD_LIBRARY_PATH"));
    private static final String NOHUP_PATH = "/usr/bin/nohup";
    private static final String SUDO_PATH = "/usr/bin/sudo";

    static {
        if (OS_IS_WINDOWS) {
            CORE_ENV_VAR_NAME_INCLUDES.add("OS"); // many batch files use this to figure out if the OS type is NT or 9x
            CORE_ENV_VAR_NAME_INCLUDES.add("SYSTEMROOT"); // required on Windows to avoid winsock create errors
        }
    }

    private ServerStartScriptDiscoveryUtility() {
    }

    /**
     * If the specified process is a script, return the path to the script - the returned path will be absolute and
     * canonical if possible, or, if it is not a script, return <code>null</code>.
     *
     * @param serverParentProcess the parent process of a server (e.g. JBoss AS) process
     *
     * @return if the specified process is a script (the returned path will be absolute and
     *         canonical if possible), the path to the script, otherwise <code>null</code>
     */
    @Nullable
    public static File getStartScript(ProcessInfo serverParentProcess) {
        // e.g. UNIX:    "/bin/sh ./standalone.sh --server-config=standalone-full.xml"
        //      Windows: "cmd.exe [options] standalone.bat --server-config=standalone-full.xml"
        String[] serverParentProcessCommandLine = serverParentProcess.getCommandLine();
        Integer startScriptIndex = getStartScriptIndex(serverParentProcessCommandLine);

        File startScriptFile;
        if (startScriptIndex != null) {
            // The process is a script - excellent!
            String startScript = (serverParentProcessCommandLine.length > startScriptIndex) ? serverParentProcessCommandLine[startScriptIndex]
                        : null;
            startScriptFile = new File(startScript);
            if (!startScriptFile.isAbsolute()) {
                ProcExe parentProcessExe = serverParentProcess.getExecutable();
                if (parentProcessExe == null) {
                    // TODO: This isn't really generic
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

    /**
     * Return the command line prefix that should be used when restarting the specified server process.
     *
     * @param serverProcess a server (e.g. JBoss AS) process
     * @param thisProcess this java process
     *
     * @return the command line prefix that should be used when restarting the specified server process
     */
    @Nullable
    public static String getStartScriptPrefix(ProcessInfo serverProcess, ProcessInfo thisProcess) {
        String prefix = null;
        if (!OS_IS_WINDOWS) {
            StringBuilder buffer = new StringBuilder();
            File nohup = new File(NOHUP_PATH);
            if (nohup.canExecute()) {
                buffer.append(nohup.getPath());
            }
            File sudo = new File(SUDO_PATH);
            if (sudo.canExecute() && (serverProcess.getCredentials() != null) &&
                    (thisProcess.getCredentials() != null)) {
                long processUid = serverProcess.getCredentials().getUid();
                long processGid = serverProcess.getCredentials().getGid();
                long agentProcessUid = thisProcess.getCredentials().getUid();
                long agentProcessGid = thisProcess.getCredentials().getGid();
                boolean sudoNeededForUser = (processUid != agentProcessUid);
                boolean sudoNeededForGroup = (processGid != agentProcessGid);
                if (sudoNeededForUser || sudoNeededForGroup) {
                    if (buffer.length() > 0) {
                        buffer.append(' ');
                    }
                    buffer.append(sudo.getPath());
                    if (sudoNeededForUser) {
                        buffer.append(" -u ");
                        if (serverProcess.getCredentialsName() != null) {
                            buffer.append(serverProcess.getCredentialsName().getUser());
                        } else {
                            buffer.append(serverProcess.getCredentials().getUid());
                        }
                    }
                    if (sudoNeededForUser) {
                        buffer.append(" -g ");
                        if (serverProcess.getCredentialsName() != null) {
                            buffer.append(serverProcess.getCredentialsName().getGroup());
                        } else {
                            buffer.append(serverProcess.getCredentials().getGid());
                        }
                    }
                }
            }
            if (buffer.length() > 0) {
                prefix = buffer.toString();
            }
        }

        return prefix;
    }

    /**
     * Returns the list of arguments that should be passed to the start script for the specified server (e.g. JBoss AS)
     * process in order to start a functionally equivalent server instance.
     *
     * @param serverParentProcess the parent process of a server (e.g. JBoss AS) process
     * @param serverArgs the subset of arguments from the server (e.g. JBoss AS) process that should be used if the
     *                   parent process is not a script
     * @param optionExcludes options that should be excluded from the returned arguments if the parent process is not a
     *                       script
     *
     * @return the list of arguments that should be passed to the start script for the specified server (e.g. JBoss AS)
     *         process in order to start a functionally equivalent server instance
     */
    @NotNull
    public static List<String> getStartScriptArgs(ProcessInfo serverParentProcess, List<String> serverArgs,
        Set<CommandLineOption> optionExcludes) {
        String[] startScriptCommandLine = serverParentProcess.getCommandLine();
        Integer startScriptIndex = getStartScriptIndex(startScriptCommandLine);

        List<String> startScriptArgs = new ArrayList<String>();
        if (startScriptIndex != null) {
            // Skip past the script to get the arguments that were passed to the script.
            for (int i = (startScriptIndex + 1); i < startScriptCommandLine.length; i++) {
                startScriptArgs.add(startScriptCommandLine[i]);
            }
        } else {
            if ((optionExcludes != null) && !optionExcludes.isEmpty()) {
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
            } else {
                startScriptArgs.addAll(serverArgs);
            }
        }
        return startScriptArgs;
    }

    /**
     * Returns the set of environment variables that should be passed to the start script for the specified server
     * (e.g. JBoss AS) process in order to start a functionally equivalent server instance.
     *
     * @param serverProcess a server (e.g. JBoss AS) process
     * @param serverParentProcess the parent process of the server (e.g. JBoss AS) process
     * @param envVarNameIncludes the names of the variables that should be included in the returned map, in addition to
     *                           a core set of OS-level variables (PATH, LD_LIBRARY_PATH, etc.)
     *
     * @return the set of environment variables that should be passed to the start script for the specified server
     *         (e.g. JBoss AS) process
     */
    @NotNull
    public static Map<String, String> getStartScriptEnv(ProcessInfo serverProcess, ProcessInfo serverParentProcess,
        Set<String> envVarNameIncludes) {
        Map<String, String> processEnvVars;
        if (getStartScript(serverParentProcess) != null) {
            processEnvVars = serverParentProcess.getEnvironmentVariables();
        } else {
            processEnvVars = serverProcess.getEnvironmentVariables();
        }

        List<String> fullEnvVarNameIncludes = (envVarNameIncludes != null) ?
                new ArrayList<String>(envVarNameIncludes) : new ArrayList<String>();
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

        // Add the fixed PATH
        if (File.separatorChar == '\\') {
            String systemRoot = processEnvVars.get("SYSTEMROOT");
            systemRoot = (systemRoot == null) ? "C:\\Windows" : systemRoot;
            String path = systemRoot + "\\system32;" + systemRoot;
            startScriptEnv.put("PATH", path);
        } else {
            startScriptEnv.put("PATH", CORE_ENV_VAR_PATH_UNIX);
        }

        return startScriptEnv;
    }

    @Nullable
    private static Integer getStartScriptIndex(String[] serverParentProcessCommandLine) {
        // Assuming the specified process actually is a script, it will look something like this:
        //   UNIX:    "/bin/sh [options] ./standalone.sh --server-config=standalone-full.xml"
        //   Windows: "cmd.exe [options]  standalone.bat --server-config=standalone-full.xml"

        if (serverParentProcessCommandLine.length == 1) {
            // The command line is an executable with no arguments - there's no way it's a script, so return null.
            return null;
        }

        int startScriptIndex;
        // Advance past any shell (e.g. /bin/sh or cmd.exe) options or empty args
        for (startScriptIndex = 1; (startScriptIndex < serverParentProcessCommandLine.length); ++startScriptIndex) {
            // the arg should not be null or empty, but we've seen empty args from Sigar...            
            String arg = serverParentProcessCommandLine[startScriptIndex];

            if (arg != null && !arg.isEmpty() && arg.charAt(0) != OPTION_PREFIX) {
                break;
            }
        }

        // for whatever unanticipated reason, we advanced past all of the args
        if (startScriptIndex == serverParentProcessCommandLine.length) {
            return null;
        }

        String possibleStartScript = serverParentProcessCommandLine[startScriptIndex];
        return (isScript(possibleStartScript)) ? startScriptIndex : null;
    }

    private static boolean isScript(String filePath) {
        // TODO: What if CygWin was used to start AS7 on Windows via a shell script?
        return (filePath != null) && (filePath.endsWith(".sh") || filePath.matches(".*\\.((bat)|(cmd))$(?i)"));
    }

}
