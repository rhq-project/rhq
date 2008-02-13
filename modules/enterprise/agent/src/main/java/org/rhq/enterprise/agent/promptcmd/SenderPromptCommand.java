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
import mazz.i18n.Msg;
import org.rhq.enterprise.agent.AgentMain;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;
import org.rhq.enterprise.communications.command.client.ClientCommandSender;
import org.rhq.enterprise.communications.command.client.ClientCommandSenderMetrics;

/**
 * Command to forcibly start or stop the agent's {@link AgentMain#getClientCommandSender() client command sender}. This
 * allows you to manually tell the sender to start sending commands or stop sending commands even if auto-discovery is
 * enabled and wants to do the opposite. Note that if auto-discovery is enabled (this includes server polling as well)
 * the sender will probably be set back to its original state when the JON Server's state is auto-detected. This means
 * that even if this command is used to start or stop the sender, when auto-discovery detects the server is down it will
 * stop the sender again - if the server comes up, it will start the sender again.
 *
 * @author John Mazzitelli
 */
public class SenderPromptCommand implements AgentPromptCommand {
    private static final Msg MSG = AgentI18NFactory.getMsg();

    /**
     * @see AgentPromptCommand#getPromptCommandString()
     */
    public String getPromptCommandString() {
        return MSG.getMsg(AgentI18NResourceKeys.SENDER);
    }

    /**
     * @see AgentPromptCommand#execute(AgentMain, String[])
     */
    public boolean execute(AgentMain agent, String[] args) {
        PrintWriter out = agent.getOut();

        try {
            if (args.length != 2) {
                out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
            } else {
                String subcommand = args[1];
                ClientCommandSender sender = agent.getClientCommandSender();

                if (sender != null) {
                    if (subcommand.equals(MSG.getMsg(AgentI18NResourceKeys.SENDER_START))) {
                        out.println(MSG.getMsg(AgentI18NResourceKeys.SENDER_STARTING));
                        sender.startSending();
                    } else if (subcommand.equals(MSG.getMsg(AgentI18NResourceKeys.SENDER_STOP))) {
                        out.println(MSG.getMsg(AgentI18NResourceKeys.SENDER_STOPPING));
                        sender.stopSending(true);
                    } else if (subcommand.equals(MSG.getMsg(AgentI18NResourceKeys.SENDER_METRICS))) {
                        ClientCommandSenderMetrics metrics = sender.getMetrics();

                        out.println(MSG.getMsg(AgentI18NResourceKeys.SENDER_METRICS_OUTPUT, metrics));
                    } else if (!subcommand.equals(MSG.getMsg(AgentI18NResourceKeys.SENDER_STATUS))) {
                        out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
                    }

                    if (sender.isSending()) {
                        out.println(MSG.getMsg(AgentI18NResourceKeys.SENDER_IS_SENDING));
                    } else {
                        out.println(MSG.getMsg(AgentI18NResourceKeys.SENDER_IS_NOT_SENDING));
                    }
                } else {
                    out.println(MSG.getMsg(AgentI18NResourceKeys.SENDER_AGENT_NOT_STARTED));
                }
            }
        } catch (Exception e) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.SENDER_FAILURE));
            e.printStackTrace(out);
        }

        return true;
    }

    /**
     * @see AgentPromptCommand#getSyntax()
     */
    public String getSyntax() {
        return MSG.getMsg(AgentI18NResourceKeys.SENDER_SYNTAX);
    }

    /**
     * @see AgentPromptCommand#getHelp()
     */
    public String getHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.SENDER_HELP);
    }

    /**
     * @see AgentPromptCommand#getDetailedHelp()
     */
    public String getDetailedHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.SENDER_DETAILED_HELP);
    }
}