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
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import mazz.i18n.Msg;

import org.apache.log4j.LogManager;

import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.agent.AgentMain;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;
import org.rhq.enterprise.communications.util.CommandTraceUtil;

/**
 * Performs various things to help debug the agent.
 * 
 * @author John Mazzitelli
 */
public class DebugPromptCommand implements AgentPromptCommand {

    private static final Msg MSG = AgentI18NFactory.getMsg();

    /**
     * @see AgentPromptCommand#getPromptCommandString()
     */
    public String getPromptCommandString() {
        return MSG.getMsg(AgentI18NResourceKeys.DEBUG);
    }

    /**
     * @see AgentPromptCommand#execute(AgentMain, String[])
     */
    public boolean execute(AgentMain agent, String[] args) {
        processArguments(agent, args);
        return true;
    }

    /**
     * @see AgentPromptCommand#getSyntax()
     */
    public String getSyntax() {
        return MSG.getMsg(AgentI18NResourceKeys.DEBUG_SYNTAX);
    }

    /**
     * @see AgentPromptCommand#getHelp()
     */
    public String getHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.DEBUG_HELP);
    }

    /**
     * @see AgentPromptCommand#getDetailedHelp()
     */
    public String getDetailedHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.DEBUG_DETAILED_HELP);
    }

    private void processArguments(AgentMain agent, String[] args) {
        PrintWriter out = agent.getOut();

        if (args.length <= 1) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
            return;
        }

        String sopts = "tc:f:";
        LongOpt[] lopts = { new LongOpt("comm", LongOpt.REQUIRED_ARGUMENT, null, 'c'), // trace comm messages
            new LongOpt("threaddump", LongOpt.NO_ARGUMENT, null, 't'), // dump thread stacks
            new LongOpt("file", LongOpt.REQUIRED_ARGUMENT, null, 'f') }; // reconfigure with new log file

        Getopt getopt = new Getopt(getPromptCommandString(), args, sopts, lopts);
        int code;

        while ((code = getopt.getopt()) != -1) {
            switch (code) {
            case ':':
            case '?':
            case 1: {
                out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
                break;
            }

            case 'c': {
                String flag = getopt.getOptarg();
                boolean flagBoolean = Boolean.parseBoolean(flag);
                agent.agentServerCommunicationsTrace(flagBoolean);
                if (flagBoolean) {
                    out.println(MSG.getMsg(AgentI18NResourceKeys.DEBUG_CMD_TRACING_ENABLED, //
                        CommandTraceUtil.getSettingTraceCommandConfig(), //
                        CommandTraceUtil.getSettingTraceCommandResponseResults(), //
                        CommandTraceUtil.getSettingTraceCommandSizeThreshold(), //
                        CommandTraceUtil.getSettingTraceCommandResponseSizeThreshold()));
                } else {
                    out.println(MSG.getMsg(AgentI18NResourceKeys.DEBUG_CMD_TRACING_DISABLED));
                }
                break;
            }

            case 'f': {
                String file = getopt.getOptarg();
                try {
                    agent.hotDeployLogConfigurationFile(file);
                    out.println(MSG.getMsg(AgentI18NResourceKeys.DEBUG_LOG_FILE_LOADED, file, LogManager
                        .getRootLogger().getLevel()));
                } catch (Exception e) {
                    out.println(MSG.getMsg(AgentI18NResourceKeys.DEBUG_CANNOT_LOAD_LOG_FILE, file, ThrowableUtil
                        .getAllMessages(e)));
                }
                break;
            }

            case 't': {
                try {
                    // TODO: Once we drop support for Java 5, we can use ThreadMXBean.dumpAllThreads(false, false)
                    //       (introduced in Java 6) here instead.
                    ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
                    long[] allIds = threadMXBean.getAllThreadIds();
                    ThreadInfo[] allInfo = threadMXBean.getThreadInfo(allIds, 256);
                    for (ThreadInfo threadInfo : allInfo) {
                        if (threadInfo != null) {
                            out.println(threadInfo);
                        }
                    }
                } catch (Exception e) {
                    out.println(MSG.getMsg(AgentI18NResourceKeys.DEBUG_CANNOT_DUMP_THREADS, ThrowableUtil
                        .getAllMessages(e)));
                }
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
