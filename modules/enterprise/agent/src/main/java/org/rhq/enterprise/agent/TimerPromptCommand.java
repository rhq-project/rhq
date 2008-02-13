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
package org.rhq.enterprise.agent;

import mazz.i18n.Msg;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;
import org.rhq.enterprise.agent.promptcmd.AgentPromptCommand;

/**
 * A special prompt command that simply times the execution of another prompt command.
 *
 * @author John Mazzitelli
 */
public class TimerPromptCommand implements AgentPromptCommand {
    private static final Msg MSG = AgentI18NFactory.getMsg();

    /**
     * @see AgentPromptCommand#getPromptCommandString()
     */
    public String getPromptCommandString() {
        return MSG.getMsg(AgentI18NResourceKeys.TIMER);
    }

    /**
     * @see AgentPromptCommand#execute(AgentMain, String[])
     */
    public boolean execute(AgentMain agent, String[] args) {
        if (args.length == 1) {
            agent.getOut().println(MSG.getMsg(AgentI18NResourceKeys.TIMER_MISSING_COMMAND));
            return true;
        }

        boolean ret_val = true;
        String[] cmd_to_execute = new String[args.length - 1];
        System.arraycopy(args, 1, cmd_to_execute, 0, cmd_to_execute.length);

        long free_mem_before = Runtime.getRuntime().freeMemory();
        long total_mem_before = Runtime.getRuntime().totalMemory();
        long time_before = System.currentTimeMillis();

        try {
            ret_val = agent.executePromptCommand(cmd_to_execute);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        long time_after = System.currentTimeMillis();
        long free_mem_after = Runtime.getRuntime().freeMemory();
        long total_mem_after = Runtime.getRuntime().totalMemory();

        // show time in seconds and memory in megabytes
        agent.getOut().println(
            MSG.getMsg(AgentI18NResourceKeys.TIMER_RESULTS, (time_after - time_before) / 1000f, args[1],
                free_mem_before / 1048576f, total_mem_before / 1048576f, free_mem_after / 1048576f,
                total_mem_after / 1048576f));

        return ret_val;
    }

    /**
     * @see AgentPromptCommand#getSyntax()
     */
    public String getSyntax() {
        return MSG.getMsg(AgentI18NResourceKeys.TIMER_SYNTAX);
    }

    /**
     * @see AgentPromptCommand#getHelp()
     */
    public String getHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.TIMER_HELP);
    }

    /**
     * @see AgentPromptCommand#getDetailedHelp()
     */
    public String getDetailedHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.TIMER_DETAILED_HELP);
    }
}