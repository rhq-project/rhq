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
import java.io.Serializable;
import java.util.Date;

import mazz.i18n.Msg;

import org.rhq.enterprise.agent.AgentMain;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;
import org.rhq.enterprise.communications.Ping;
import org.rhq.enterprise.communications.command.CommandResponse;
import org.rhq.enterprise.communications.command.client.ClientCommandSender;
import org.rhq.enterprise.communications.command.client.ClientRemotePojoFactory;
import org.rhq.enterprise.communications.command.client.CommandResponseCallback;

/**
 * Sends a remote ping request to the server. This utilizes the remote POJO functionality.
 *
 * @author John Mazzitelli
 */
public class PingPromptCommand implements AgentPromptCommand, Serializable {
    /**
     * the UID to identify the serializable version of this class
     */
    private static final long serialVersionUID = 1L;

    /**
     * The object used to get I18N messages suitable for displaying to the user.
     */
    private static final Msg MSG = AgentI18NFactory.getMsg();

    /**
     * @see AgentPromptCommand#getPromptCommandString()
     */
    public String getPromptCommandString() {
        return MSG.getMsg(AgentI18NResourceKeys.PING);
    }

    /**
     * @see AgentPromptCommand#execute(AgentMain, String[])
     */
    public boolean execute(final AgentMain agent, String[] args) {
        int numberOfTimes = getTestBlastCount(agent);
        ClientRemotePojoFactory.GuaranteedDelivery guaranteed = ClientRemotePojoFactory.GuaranteedDelivery.NO;

        if (args.length > 1) {
            if ((args.length > 2) || !args[1].equals(MSG.getMsg(AgentI18NResourceKeys.PING_GUARANTEED))) {
                agent.getOut().println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
                return true;
            }

            if (numberOfTimes == 1) {
                agent.getOut().println(MSG.getMsg(AgentI18NResourceKeys.PING_GUARANTEED_FOR_ASYNC_ONLY));
            }

            guaranteed = ClientRemotePojoFactory.GuaranteedDelivery.YES;
        }

        ClientCommandSender sender = agent.getClientCommandSender();

        if (sender != null) {
            if (numberOfTimes == 1) {
                // if executing it just one time, then do so synchronously
                Ping ping = sender.getClientRemotePojoFactory().getRemotePojo(Ping.class);
                agent.getOut().println(MSG.getMsg(AgentI18NResourceKeys.PING_PINGING));
                String ping_results = ping.ping("PING", "Ack: ");
                agent.getOut().println(MSG.getMsg(AgentI18NResourceKeys.PING_PING_RESULTS, ping_results));
            } else {
                ClientRemotePojoFactory factory = sender.getClientRemotePojoFactory();
                factory.setDeliveryGuaranteed(guaranteed);
                factory.setAsynch(true, new Callback(agent));

                Ping ping = factory.getRemotePojo(Ping.class);

                for (int i = 0; i < numberOfTimes; i++) {
                    agent.getOut().println(MSG.getMsg(AgentI18NResourceKeys.PING_ASYNC_PING, i, guaranteed));
                    ping.ping("PING", "Ack #"
                        + i
                        + ((ClientRemotePojoFactory.GuaranteedDelivery.YES == guaranteed) ? (" ("
                            + MSG.getMsg(AgentI18NResourceKeys.PING_GUARANTEED) + ")") : "") + ": ");
                }
            }
        } else {
            agent.getOut().println(MSG.getMsg(AgentI18NResourceKeys.PING_AGENT_NOT_STARTED));
        }

        return true;
    }

    /**
     * @see AgentPromptCommand#getSyntax()
     */
    public String getSyntax() {
        return MSG.getMsg(AgentI18NResourceKeys.PING_SYNTAX);
    }

    /**
     * @see AgentPromptCommand#getHelp()
     */
    public String getHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.PING_HELP);
    }

    /**
     * @see AgentPromptCommand#getDetailedHelp()
     */
    public String getDetailedHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.PING_DETAILED_HELP);
    }

    /**
     * To facilitate testing, if a preference named "rhq.agent.test.blast-count" is set, that's the number of async
     * calls that are made. Default is 1.
     *
     * @param  agent
     *
     * @return blast count
     */
    private int getTestBlastCount(AgentMain agent) {
        int value = agent.getConfiguration().getPreferences().getInt("rhq.agent.test.blast-count", 1);

        return value;
    }
}

/**
 * A callback that will output the results of the asynchronous ping.
 */
class Callback implements CommandResponseCallback, Serializable {
    private static final long serialVersionUID = 1L;
    private static final Msg MSG = AgentI18NFactory.getMsg();
    private transient AgentMain m_agent = null;

    /**
     * Constructor for {@link Callback}.
     *
     * @param agent
     */
    Callback(AgentMain agent) {
        m_agent = agent;
    }

    /**
     * Receives the ping ACK.
     *
     * @param response
     */
    public void commandSent(CommandResponse response) {
        PrintWriter out;

        if (m_agent == null) {
            out = new PrintWriter(System.out);
        } else {
            out = m_agent.getOut();
        }

        if (response.isSuccessful()) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.PING_TIMESTAMPED_PING_RESULTS, new Date(), response
                .getResults()));
        } else {
            out.println(MSG.getMsg(AgentI18NResourceKeys.PING_PING_FAILED, response.getException()));
            if (response.getException() != null) {
                response.getException().printStackTrace(out);
            }
        }

        out.flush(); // seems we need to flush if the out wraps System.out

        return;
    }
}