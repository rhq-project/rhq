/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.server.plugins.alertIrc;

import java.util.Arrays;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jibble.pircbot.PircBot;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent;
import org.rhq.enterprise.server.plugin.pc.ServerPluginContext;

/**
 * Persistent component used to send alert notifications via IRC.
 * This component is responsible for establishing and maintaining the IRC
 * session as well as sending messages.
 *
 * @author Justin Harris
 */
public class IrcAlertComponent implements ServerPluginComponent {

    private final static String DEFAULT_IRC_PORT = "6667";
    private final Log log = LogFactory.getLog(IrcAlertComponent.class);
    private RhqAlertBot ircBot;
    private String nick;
    private String server;
    private int port;
    private String[] channels;
    private String cannedResponse;

    public void initialize(ServerPluginContext context) throws Exception {
        Configuration preferences = context.getPluginConfiguration();

        this.nick = preferences.getSimpleValue("nick", null);
        this.server = preferences.getSimpleValue("server", null);
        this.port = Integer.parseInt(preferences.getSimpleValue("port", DEFAULT_IRC_PORT));
        this.channels = parseChannels(preferences.getSimpleValue("initialChannels", null));
        this.cannedResponse = preferences.getSimpleValue("cannedResponse", null);
    }

    private String[] parseChannels(String channelList) {
        if (channelList != null && channelList.length() > 0) {
            return channelList.split(",");
        }

        return new String[0];
    }

    public void start() {
        this.ircBot = new RhqAlertBot(this.nick, this.cannedResponse);

        try {
            this.ircBot.connect(server, port);
            log.info("Connected to server " + server + ":" + port);
        } catch (Exception e) {
            log.error("Error joining IRC: " + e.getMessage());

            return;
        }

        for (String channel : channels) {
            this.ircBot.joinChannel(channel);
        }
    }

    public void stop() {
        this.ircBot.disconnect();
    }

    public void shutdown() {
        this.ircBot.dispose();
        this.ircBot = null;
    }

    /**
     * Sends a message to the specified channel, or all currently joined channels
     * if the channel is not supplied.
     *
     * @param channel the channel to send the message to, or <code>null</code> if
     *                the message should be sent to all currently joined channels
     * @param message
     */
    public void sendIrcMessage(String channel, String message) {
        if (this.ircBot != null && this.ircBot.isConnected()) {
            for (String sendChannel : getChannelsToSend(channel)) {
                this.ircBot.sendMessage(sendChannel, message);
            }
        } else {
            throw new IllegalStateException("IRC bot has not been initialized");
        }
    }

    private String[] getChannelsToSend(String channel) {
        if (channel != null && channel.length() > 0) {
            if (!isInChannel(channel)) {
                this.ircBot.joinChannel(channel);
            }

            return new String[]{channel};
        } else {
            return this.ircBot.getChannels();
        }
    }

    private boolean isInChannel(String channel) {
        return Arrays.asList(this.ircBot.getChannels()).contains(channel);
    }

    /**
     * Our alert-centric IRC bot - not too complicated.
     */
    private class RhqAlertBot extends PircBot {

        private String response;

        private RhqAlertBot(String name, String defaultResponse) {
            this.setName(name);
            this.response = defaultResponse;
        }

        /**
         * Responds to messages with the default response, if one has been defined.
         *
         * @param channel
         * @param sender
         * @param login
         * @param hostname
         * @param message
         */
        @Override
        public void onMessage(String channel, String sender, String login,
                String hostname, String message) {

            if (!message.contains(nick))
                return;

            if (this.response != null) {
                if (channel != null && channel.length() > 0) {
                    sendMessage(channel, sender + ":  " + this.response);
                } else {
                    sendMessage(sender, this.response);
                }
            }
        }
    }
}
