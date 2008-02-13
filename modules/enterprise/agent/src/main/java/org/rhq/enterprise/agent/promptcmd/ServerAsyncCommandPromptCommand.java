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
import org.rhq.enterprise.communications.command.client.CommandResponseCallback;

/**
 * This builds a command based on the prompt command's arguments and sends a remote command request to the JON Server
 * asynchronously.
 *
 * @author John Mazzitelli
 */
public class ServerAsyncCommandPromptCommand implements AgentPromptCommand {
    private static final Msg MSG = AgentI18NFactory.getMsg();

    /**
     * @see AgentPromptCommand#getPromptCommandString()
     */
    public String getPromptCommandString() {
        return MSG.getMsg(AgentI18NResourceKeys.SERVERASYNC);
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

            int blast_count = getTestBlastCount(agent);

            for (int i = 0; i < blast_count; i++) {
                agent.getClientCommandSender().sendAsynch(command, new Callback(agent));
                agent.getOut().println(MSG.getMsg(AgentI18NResourceKeys.SERVERASYNC_QUEUED, i));
            }
        } catch (Exception e) {
            throw new RuntimeException(MSG.getMsg(AgentI18NResourceKeys.SERVERASYNC_FAILURE), e);
        }

        return true;
    }

    /**
     * @see AgentPromptCommand#getSyntax()
     */
    public String getSyntax() {
        return MSG.getMsg(AgentI18NResourceKeys.SERVERASYNC_SYNTAX);
    }

    /**
     * @see AgentPromptCommand#getHelp()
     */
    public String getHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.SERVERASYNC_HELP);
    }

    /**
     * @see AgentPromptCommand#getDetailedHelp()
     */
    public String getDetailedHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.SERVERASYNC_DETAILED_HELP, new CmdlineClient().getUsage());
    }

    /**
     * To facilitate testing, if a preference named "rhq.agent.test.blast-count" is set, that's the number of async
     * messages that are sent. Default is 1.
     *
     * @param  agent
     *
     * @return blast count
     */
    private int getTestBlastCount(AgentMain agent) {
        int value = agent.getConfiguration().getPreferences().getInt("rhq.agent.test.blast-count", 1);

        return value;
    }

    /**
     * The command response callback that will receive the notification when the command has been executed.
     */
    private class Callback implements CommandResponseCallback {
        private static final long serialVersionUID = 1L;
        private transient AgentMain m_agent;

        /**
         * Creates a new {@link Callback} object.
         *
         * @param agent
         */
        public Callback(AgentMain agent) {
            m_agent = agent;
        }

        /**
         * @see CommandResponseCallback#commandSent(CommandResponse)
         */
        public void commandSent(CommandResponse response) {
            if (m_agent != null) {
                m_agent.getOut().println(MSG.getMsg(AgentI18NResourceKeys.SERVERASYNC_FINISHED, response));
            } else {
                System.out.println(MSG.getMsg(AgentI18NResourceKeys.SERVERASYNC_RECOVERED, response));
            }
        }
    }
}