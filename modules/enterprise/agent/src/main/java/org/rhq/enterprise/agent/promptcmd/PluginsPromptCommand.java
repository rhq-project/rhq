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

import java.io.File;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Date;
import java.util.List;

import mazz.i18n.Msg;

import org.rhq.core.clientapi.descriptor.AgentPluginDescriptorUtil;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.clientapi.server.core.CoreServerService;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.util.MD5Generator;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.agent.AgentMain;
import org.rhq.enterprise.agent.PluginUpdate;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;
import org.rhq.enterprise.communications.command.client.ClientCommandSender;
import org.rhq.enterprise.communications.command.client.ClientRemotePojoFactory;

/**
 * Allows you to manually update the plugins.
 *
 * @author John Mazzitelli
 */
public class PluginsPromptCommand implements AgentPromptCommand {
    private static final Msg MSG = AgentI18NFactory.getMsg();

    /**
     * @see AgentPromptCommand#getPromptCommandString()
     */
    public String getPromptCommandString() {
        return MSG.getMsg(AgentI18NResourceKeys.PLUGINS);
    }

    /**
     * @see AgentPromptCommand#execute(AgentMain, String[])
     */
    public boolean execute(AgentMain agent, String[] args) {
        PrintWriter out = agent.getOut();

        if (args.length != 2) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
            return true;
        }

        try {
            if (args[1].equals(MSG.getMsg(AgentI18NResourceKeys.PLUGINS_ARG_UPDATE))) {
                doUpdate(agent);
            } else if (args[1].equals(MSG.getMsg(AgentI18NResourceKeys.PLUGINS_ARG_INFO))) {
                doInfo(agent);
            } else {
                out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
            }
        } catch (Exception e) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.PLUGINS_ERROR_UPDATING, ThrowableUtil.getAllMessages(e)));
        }

        return true;
    }

    /**
     * @see AgentPromptCommand#getSyntax()
     */
    public String getSyntax() {
        return MSG.getMsg(AgentI18NResourceKeys.PLUGINS_SYNTAX);
    }

    /**
     * @see AgentPromptCommand#getHelp()
     */
    public String getHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.PLUGINS_HELP);
    }

    /**
     * @see AgentPromptCommand#getDetailedHelp()
     */
    public String getDetailedHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.PLUGINS_DETAILED_HELP);
    }

    /**
     * Performs the info command which will output information on the currently installed plugins to the agent's output.
     *
     * @param  agent
     *
     * @throws Exception
     */
    private void doInfo(AgentMain agent) throws Exception {
        PrintWriter out = agent.getOut();
        PluginUpdate plugin_update = getPluginUpdateObject(agent);
        List<File> current_plugins = plugin_update.getCurrentPluginFiles();

        if (current_plugins.size() > 0) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.PLUGINS_LISTING_PLUGINS));

            for (File current_plugin : current_plugins) {
                String plugin_name;
                try {
                    URL url = current_plugin.toURI().toURL();
                    PluginDescriptor descriptor = AgentPluginDescriptorUtil.loadPluginDescriptorFromUrl(url);
                    plugin_name = descriptor.getName();
                } catch (Throwable t) {
                    plugin_name = "?cannot-parse-descriptor?";
                }

                String filename = current_plugin.getName();
                Date last_mod = new Date(current_plugin.lastModified());
                long filesize = current_plugin.length();
                String md5 = MD5Generator.getDigestString(current_plugin);

                out.println();
                out.println(MSG.getMsg(AgentI18NResourceKeys.PLUGINS_PLUGINS_INFO_FILENAME, filename));
                out.print('\t');
                out.println(MSG.getMsg(AgentI18NResourceKeys.PLUGINS_PLUGINS_INFO_NAME, plugin_name));
                out.print('\t');
                out.println(MSG.getMsg(AgentI18NResourceKeys.PLUGINS_PLUGINS_INFO_LASTMOD, last_mod));
                out.print('\t');
                out.println(MSG.getMsg(AgentI18NResourceKeys.PLUGINS_PLUGINS_INFO_FILESIZE, filesize));
                out.print('\t');
                out.println(MSG.getMsg(AgentI18NResourceKeys.PLUGINS_PLUGINS_INFO_MD5, md5));
            }

            out.println();
            out.println(MSG.getMsg(AgentI18NResourceKeys.PLUGINS_NUM_CURRENT_PLUGINS, current_plugins.size()));
        } else {
            out.println(MSG.getMsg(AgentI18NResourceKeys.PLUGINS_NO_CURRENT_PLUGINS));
        }

        return;
    }

    /**
     * Performs the update command which will update all plugins and output to the agent's <code>out</code> stream what
     * was updated.
     *
     * @param  agent
     *
     * @throws Exception
     */
    private void doUpdate(AgentMain agent) throws Exception {
        // if the PC is already started, we need to shut it down and restart it after we get the new plugins
        boolean recyclePC = agent.isPluginContainerStarted();

        PrintWriter out = agent.getOut();

        // make sure our agent is currently in communications with the server
        ClientCommandSender clientCommandSender = agent.getClientCommandSender();
        if (clientCommandSender == null || !clientCommandSender.isSending()) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.PLUGINS_ERROR_NOT_SENDING));
            return;
        }

        // use the PC prompt command to stop the PC - needed because the PC cannot hot-deploy plugins
        if (recyclePC) {
            executePCCommand(agent, "stop");
        }

        // catch any exceptions and keep going so we can restart the PC, even if plugins failed to update
        try {
            out.println(MSG.getMsg(AgentI18NResourceKeys.UPDATING_PLUGINS));

            List<Plugin> updated_plugins = updatePlugins(agent);

            if ((updated_plugins != null) && (updated_plugins.size() > 0)) {
                for (Plugin plugin : updated_plugins) {
                    out.println(MSG.getMsg(AgentI18NResourceKeys.DOWNLOADING_PLUGIN_COMPLETE, plugin.getPath()));
                }
            } else {
                out.println(MSG.getMsg(AgentI18NResourceKeys.UPDATING_PLUGINS_ALREADY_UPTODATE));
            }

            out.println(MSG.getMsg(AgentI18NResourceKeys.UPDATING_PLUGINS_COMPLETE));
        } catch (Throwable t) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.UPDATING_PLUGINS_FAILURE));
            out.println(ThrowableUtil.getAllMessages(t));
        }

        if (recyclePC) {
            executePCCommand(agent, "start");
        }

        return;
    }

    /**
     * Updates plugins with their latest versions. If our plugins are already up-to-date, this leaves them as-is.
     *
     * @param  agent
     *
     * @return the list of plugins that were updated (those not listed were already up-to-date)
     *
     * @throws Exception if failed to update the plugins
     */
    private List<Plugin> updatePlugins(AgentMain agent) throws Exception {
        return getPluginUpdateObject(agent).updatePlugins();
    }

    /**
     * Builds a {@link PluginUpdate} object that can be used to update the plugins.
     *
     * @param  agent
     *
     * @return plugin updater
     */
    private PluginUpdate getPluginUpdateObject(AgentMain agent) {
        ClientCommandSender sender = agent.getClientCommandSender();
        CoreServerService server = null;

        if (sender != null) {
            ClientRemotePojoFactory factory = sender.getClientRemotePojoFactory();
            server = factory.getRemotePojo(CoreServerService.class);
        }

        PluginContainerConfiguration pc_config = agent.getConfiguration().getPluginContainerConfiguration();
        PluginUpdate plugin_update = new PluginUpdate(server, pc_config);

        return plugin_update;
    }

    private void executePCCommand(AgentMain agent, String startOrStop) {
        new PluginContainerPromptCommand().execute(agent, new String[] { "pc", startOrStop });
    }
}