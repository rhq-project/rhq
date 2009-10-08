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

import mazz.i18n.Msg;
import org.rhq.enterprise.agent.AgentMain;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandResponse;
import org.rhq.enterprise.communications.command.client.CmdlineClient;

/**
 * This builds a command based on the prompt command's arguments and sends a remote command request to the JON Server.
 *
 * @author John Mazzitelli
 */
public class ServerCommandPromptCommand implements AgentPromptCommand {
    private static final Msg MSG = AgentI18NFactory.getMsg();

    /**
     * @see AgentPromptCommand#getPromptCommandString()
     */
    public String getPromptCommandString() {
        return MSG.getMsg(AgentI18NResourceKeys.SERVER);
    }

    /**
     * @see AgentPromptCommand#execute(AgentMain, String[])
     */
    public boolean execute(AgentMain agent, String[] args) {
        try {
            // strip the first argument, its our prompt command string and can't be passed to the cmdline client
            String[] remoteCmdArgs = new String[args.length - 1];
            System.arraycopy(args, 1, remoteCmdArgs, 0, remoteCmdArgs.length);

            // use the command line client utility to build the command, but send the command via the agent's client sender
            CmdlineClient client = new CmdlineClient();
            Command command = client.buildCommand(remoteCmdArgs);

            agent.getOut().println(MSG.getMsg(AgentI18NResourceKeys.SERVER_SENDING));

            CommandResponse response = agent.getClientCommandSender().sendSynch(command);

            agent.getOut().println(response);
        } catch (Exception e) {
            throw new RuntimeException(MSG.getMsg(AgentI18NResourceKeys.SERVER_FAILURE), e);
        }

        return true;
    }

    /**
     * @see AgentPromptCommand#getSyntax()
     */
    public String getSyntax() {
        return MSG.getMsg(AgentI18NResourceKeys.SERVER_SYNTAX);
    }

    /**
     * @see AgentPromptCommand#getHelp()
     */
    public String getHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.SERVER_HELP);
    }

    /**
     * @see AgentPromptCommand#getDetailedHelp()
     */
    public String getDetailedHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.SERVER_DETAILED_HELP, new CmdlineClient().getUsage());
    }
}