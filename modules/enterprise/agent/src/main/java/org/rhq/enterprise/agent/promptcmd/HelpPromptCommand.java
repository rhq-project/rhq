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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import mazz.i18n.Msg;
import org.rhq.enterprise.agent.AgentMain;
import org.rhq.enterprise.agent.StringUtil;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;

/**
 * Displays help on a prompt command.
 *
 * @author John Mazzitelli
 */
public class HelpPromptCommand implements AgentPromptCommand {
    private static final Msg MSG = AgentI18NFactory.getMsg();

    /**
     * @see AgentPromptCommand#getPromptCommandString()
     */
    public String getPromptCommandString() {
        return MSG.getMsg(AgentI18NResourceKeys.HELP);
    }

    /**
     * @see AgentPromptCommand#execute(AgentMain, String[])
     */
    public boolean execute(AgentMain agent, String[] args) {
        if (args.length <= 1) {
            Map<String, String> command_and_help = new HashMap<String, String>();

            for (Iterator<String> iter = agent.getPromptCommands().keySet().iterator(); iter.hasNext();) {
                String command_name = iter.next();
                String help = getHelpForCommand(agent, command_name, false);
                command_and_help.put(command_name, help);
            }

            agent.getOut().println(StringUtil.justifyKeyValueStrings(command_and_help));
        } else {
            if (args.length == 2) {
                String command_name = args[1];
                String help = getHelpForCommand(agent, command_name, true);
                agent.getOut().println(help);
            } else {
                agent.getOut().println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
            }
        }

        return true;
    }

    /**
     * Gets the help text for the given command.
     *
     * @param  agent               agent
     * @param  prompt_command_name the prompt command name
     * @param  detailed            if <code>true</code>, gets the detailed help; <code>false</code> gets the short help
     *                             summary
     *
     * @return the help text for the command
     */
    private String getHelpForCommand(AgentMain agent, String prompt_command_name, boolean detailed) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(out);

        try {
            Class clazz = agent.getPromptCommands().get(prompt_command_name);
            AgentPromptCommand cmd_instance = (AgentPromptCommand) clazz.newInstance();

            if (detailed) {
                ps.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, cmd_instance.getSyntax()));
                ps.println();
                ps.println(cmd_instance.getDetailedHelp());
            } else {
                ps.print(cmd_instance.getHelp());
            }
        } catch (Exception e) {
            ps.print(MSG.getMsg(AgentI18NResourceKeys.HELP_UNKNOWN, prompt_command_name));
        }

        return out.toString();
    }

    /**
     * @see AgentPromptCommand#getSyntax()
     */
    public String getSyntax() {
        return MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX);
    }

    /**
     * @see AgentPromptCommand#getHelp()
     */
    public String getHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.HELP_HELP);
    }

    /**
     * @see AgentPromptCommand#getDetailedHelp()
     */
    public String getDetailedHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.HELP_DETAILED_HELP);
    }
}