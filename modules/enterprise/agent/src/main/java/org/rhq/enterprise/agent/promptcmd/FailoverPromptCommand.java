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

import mazz.i18n.Msg;

import org.rhq.core.domain.cluster.composite.FailoverListComposite;
import org.rhq.enterprise.agent.AgentMain;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;

/**
 * Performs various interactions with this agent's {@link FailoverListComposite}
 * 
 * @author Joseph Marques
 */
public class FailoverPromptCommand implements AgentPromptCommand {

    private static final Msg MSG = AgentI18NFactory.getMsg();

    /**
     * @see AgentPromptCommand#getPromptCommandString()
     */
    public String getPromptCommandString() {
        return MSG.getMsg(AgentI18NResourceKeys.FAILOVER);
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
        return MSG.getMsg(AgentI18NResourceKeys.FAILOVER_SYNTAX);
    }

    /**
     * @see AgentPromptCommand#getHelp()
     */
    public String getHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.FAILOVER_HELP);
    }

    /**
     * @see AgentPromptCommand#getDetailedHelp()
     */
    public String getDetailedHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.FAILOVER_DETAILED_HELP);
    }

    private void processArguments(AgentMain agent, String[] args) {
        PrintWriter out = agent.getOut();

        if (args.length <= 1) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
            return;
        }

        String sopts = "clr";
        LongOpt[] lopts = { new LongOpt("check", LongOpt.NO_ARGUMENT, null, 'c'), // check if using primary server
            new LongOpt("list", LongOpt.NO_ARGUMENT, null, 'l'), // show the failover list
            new LongOpt("reset", LongOpt.NO_ARGUMENT, null, 'r') }; // reset the failover list iterator

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
                out.println(MSG.getMsg(AgentI18NResourceKeys.FAILOVER_CHECK_NOW));
                agent.performPrimaryServerSwitchoverCheck();
                break;
            }

            case 'l': {
                showFailoverList(agent, out);
                break;
            }

            case 'r': {
                agent.getServerFailoverList().resetIndex();
                out.println(MSG.getMsg(AgentI18NResourceKeys.FAILOVER_RESET_DONE));
                out.println();
                showFailoverList(agent, out);
                break;
            }
            }
        }

        if ((getopt.getOptind() + 1) < args.length) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
        }

        return;
    }

    private void showFailoverList(AgentMain agent, PrintWriter out) {
        FailoverListComposite failoverList = agent.getServerFailoverList();
        if (failoverList != null && failoverList.size() > 0) {
            failoverList.print(out);
        } else {
            out.println("<>");
        }
    }
}
