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
package org.rhq.enterprise.agent.promptcmd;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import mazz.i18n.Msg;
import org.rhq.core.system.ProcessInfo;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.enterprise.agent.AgentMain;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;

/**
 * Provides access to the native system.
 *
 * @author John Mazzitelli
 */
public class NativePromptCommand implements AgentPromptCommand {
    private static final Msg MSG = AgentI18NFactory.getMsg();

    /**
     * @see AgentPromptCommand#getPromptCommandString()
     */
    public String getPromptCommandString() {
        return MSG.getMsg(AgentI18NResourceKeys.NATIVE);
    }

    /**
     * @see AgentPromptCommand#execute(AgentMain, String[])
     */
    public boolean execute(AgentMain agent, String[] args) {
        PrintWriter out = agent.getOut();

        if (args.length <= 1) {
            out.println(MSG
                .getMsg(SystemInfoFactory.isNativeSystemInfoAvailable() ? AgentI18NResourceKeys.NATIVE_IS_AVAILABLE
                    : AgentI18NResourceKeys.NATIVE_IS_NOT_AVAILABLE));

            out.println(MSG
                .getMsg(SystemInfoFactory.isNativeSystemInfoDisabled() ? AgentI18NResourceKeys.NATIVE_IS_DISABLED
                    : AgentI18NResourceKeys.NATIVE_IS_NOT_DISABLED));

            out.println(MSG
                .getMsg(SystemInfoFactory.isNativeSystemInfoInitialized() ? AgentI18NResourceKeys.NATIVE_IS_INITIALIZED
                    : AgentI18NResourceKeys.NATIVE_IS_NOT_INITIALIZED));

            return true;
        }

        processArguments(agent, args);

        return true;
    }

    /**
     * @see AgentPromptCommand#getSyntax()
     */
    public String getSyntax() {
        return MSG.getMsg(AgentI18NResourceKeys.NATIVE_SYNTAX);
    }

    /**
     * @see AgentPromptCommand#getHelp()
     */
    public String getHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.NATIVE_HELP);
    }

    /**
     * @see AgentPromptCommand#getDetailedHelp()
     */
    public String getDetailedHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.NATIVE_DETAILED_HELP);
    }

    private void processArguments(AgentMain agent, String[] args) {
        PrintWriter out = agent.getOut();

        String sopts = "deop::sv";
        LongOpt[] lopts = { new LongOpt("disable", LongOpt.NO_ARGUMENT, null, 'd'),
            new LongOpt("enable", LongOpt.NO_ARGUMENT, null, 'e'), new LongOpt("os", LongOpt.NO_ARGUMENT, null, 'o'),
            new LongOpt("ps", LongOpt.OPTIONAL_ARGUMENT, null, 'p'),
            new LongOpt("shutdown", LongOpt.NO_ARGUMENT, null, 's'),
            new LongOpt("version", LongOpt.NO_ARGUMENT, null, 'v') };

        Getopt getopt = new Getopt("native", args, sopts, lopts);
        int code;

        while ((code = getopt.getopt()) != -1) {
            switch (code) {
            case ':':
            case '?':
            case 1: {
                out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
                break;
            }

            case 'd': {
                SystemInfoFactory.disableNativeSystemInfo();
                out.println(MSG.getMsg(AgentI18NResourceKeys.NATIVE_DISABLE_DONE));
                break;
            }

            case 'e': {
                SystemInfoFactory.enableNativeSystemInfo();
                out.println(MSG.getMsg(AgentI18NResourceKeys.NATIVE_ENABLE_DONE));
                break;
            }

            case 'o': {
                SystemInfo sysInfo = SystemInfoFactory.createSystemInfo();

                // careful - I chose to only output things that I know the non-native Java sysinfo can support
                out.println(MSG.getMsg(AgentI18NResourceKeys.NATIVE_OS_OUTPUT, sysInfo.getOperatingSystemName(),
                    sysInfo.getOperatingSystemVersion(), sysInfo.getHostname()));

                break;
            }

            case 'p': {
                SystemInfo sysInfo = SystemInfoFactory.createSystemInfo();
                String verboseOpt = getopt.getOptarg();
                boolean verbose = (verboseOpt != null)
                    && verboseOpt.equals(MSG.getMsg(AgentI18NResourceKeys.NATIVE_VERBOSE));

                try {
                    List<ProcessInfo> processes = sysInfo.getAllProcesses();
                    if (verbose) {
                        out.println(MSG.getMsg(AgentI18NResourceKeys.NATIVE_PS_OUTPUT_VERBOSE_HEADER));
                    } else {
                        out.println(MSG.getMsg(AgentI18NResourceKeys.NATIVE_PS_OUTPUT_SHORT_HEADER));
                    }

                    for (ProcessInfo p : processes) {
                        if (verbose) {
                            out.println(MSG.getMsg(AgentI18NResourceKeys.NATIVE_PS_OUTPUT_VERBOSE, p.getPid(), p
                                .getParentPid(), p.getBaseName(), Arrays.toString(p.getCommandLine())));
                        } else {
                            out.println(MSG.getMsg(AgentI18NResourceKeys.NATIVE_PS_OUTPUT_SHORT, p.getPid(), p
                                .getName()));
                        }
                    }
                } catch (Exception e) {
                    out.println(MSG.getMsg(AgentI18NResourceKeys.NATIVE_NOT_SUPPORTED));
                }

                break;
            }

            case 's': {
                if (!agent.isStarted()) {
                    SystemInfoFactory.shutdown();
                    SystemInfoFactory.disableNativeSystemInfo();
                    out.println(MSG.getMsg(AgentI18NResourceKeys.NATIVE_SHUTDOWN_DONE));
                } else {
                    out.println(MSG.getMsg(AgentI18NResourceKeys.NATIVE_SHUTDOWN_FAILED_AGENT_STARTED));
                }

                break;
            }

            case 'v': {
                out.println(SystemInfoFactory.getNativeSystemInfoVersion());
                break;
            }
            }
        }

        if ((getopt.getOptind() + 1) < args.length) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
        }

        return;
    }
}