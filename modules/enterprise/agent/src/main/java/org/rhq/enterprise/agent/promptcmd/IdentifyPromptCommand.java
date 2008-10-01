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

import java.io.PrintWriter;
import java.net.MalformedURLException;

import mazz.i18n.Msg;

import org.rhq.enterprise.agent.AgentMain;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandResponse;
import org.rhq.enterprise.communications.command.client.ClientCommandSender;
import org.rhq.enterprise.communications.command.client.JBossRemotingRemoteCommunicator;
import org.rhq.enterprise.communications.command.client.RemoteCommunicator;
import org.rhq.enterprise.communications.command.impl.generic.GenericCommandClient;
import org.rhq.enterprise.communications.command.impl.identify.IdentifyCommand;
import org.rhq.enterprise.communications.command.impl.identify.IdentifyCommandResponse;

/**
 * Asks to identify a remote server.
 *
 * @author John Mazzitelli
 */
public class IdentifyPromptCommand implements AgentPromptCommand {
    private static final Msg MSG = AgentI18NFactory.getMsg();

    /**
     * @see AgentPromptCommand#getPromptCommandString()
     */
    public String getPromptCommandString() {
        return MSG.getMsg(AgentI18NResourceKeys.IDENTIFY);
    }

    /**
     * @see AgentPromptCommand#execute(AgentMain, String[])
     */
    public boolean execute(AgentMain agent, String[] args) {
        Command command = new IdentifyCommand();
        PrintWriter out = agent.getOut();

        try {
            if (args.length <= 1) {
                // the user didn't specify a locator URI, by default, we'll send the command to our configured server
                ClientCommandSender sender = agent.getClientCommandSender();
                if (agent.isStarted() && (sender != null)) {
                    out.println(MSG.getMsg(AgentI18NResourceKeys.IDENTIFY_ASK_SERVER_FOR_ID));
                    CommandResponse response = sender.sendSynch(command);
                    out.println(response);

                    // let the server know about the time of the server
                    if (response instanceof IdentifyCommandResponse && response.isSuccessful()) {
                        long serverTime = ((IdentifyCommandResponse) response).getIdentification().getTimestamp();
                        agent.serverClockNotification(serverTime);
                    }
                } else {
                    out.println(MSG.getMsg(AgentI18NResourceKeys.IDENTIFY_NOT_SENDING));
                }
            } else if (args.length > 2) {
                out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
            } else {
                // use the generic client utility to issue the command
                RemoteCommunicator rc = new JBossRemotingRemoteCommunicator(args[1]);
                GenericCommandClient client = new GenericCommandClient(rc);

                out.println(MSG.getMsg(AgentI18NResourceKeys.IDENTIFY_ASK_REMOTE_SERVER_FOR_ID, args[1]));
                CommandResponse response = client.invoke(command);

                client.disconnectRemoteCommunicator();

                out.println(response);
            }
        } catch (MalformedURLException e) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.IDENTIFY_INVALID_LOCATOR_URI, args[1]));
        } catch (Throwable e) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.IDENTIFY_REMOTE_FAILURE));
            e.printStackTrace(out);
        }

        return true;
    }

    /**
     * @see AgentPromptCommand#getSyntax()
     */
    public String getSyntax() {
        return MSG.getMsg(AgentI18NResourceKeys.IDENTIFY_SYNTAX);
    }

    /**
     * @see AgentPromptCommand#getHelp()
     */
    public String getHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.IDENTIFY_HELP);
    }

    /**
     * @see AgentPromptCommand#getDetailedHelp()
     */
    public String getDetailedHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.IDENTIFY_DETAILED_HELP);
    }
}