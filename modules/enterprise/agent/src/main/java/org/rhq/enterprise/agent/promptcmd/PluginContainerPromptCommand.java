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
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.agent.AgentMain;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;

/**
 * Allows you to manually start and stop the plugin container.
 *
 * @author John Mazzitelli
 */
public class PluginContainerPromptCommand implements AgentPromptCommand {
    private static final Msg MSG = AgentI18NFactory.getMsg();

    /**
     * @see AgentPromptCommand#getPromptCommandString()
     */
    public String getPromptCommandString() {
        return MSG.getMsg(AgentI18NResourceKeys.PLUGIN_CONTAINER);
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

        if (args[1].equals(MSG.getMsg(AgentI18NResourceKeys.PLUGIN_CONTAINER_ARG_START))) {
            try {
                if (!agent.isStarted()) {
                    throw new IllegalStateException(MSG
                        .getMsg(AgentI18NResourceKeys.CANNOT_START_PLUGIN_CONTAINER_AGENT_NOT_STARTED));
                }

                boolean started = agent.startPluginContainer(60000L); // wait no longer than 60 seconds for the agent to become registered

                if (started) {
                    out.println(MSG.getMsg(AgentI18NResourceKeys.PLUGIN_CONTAINER_START_DONE));
                }
            } catch (Exception e) {
                out.println(MSG.getMsg(AgentI18NResourceKeys.PLUGIN_CONTAINER_START_ERROR, ThrowableUtil
                    .getAllMessages(e)));
            }
        } else if (args[1].equals(MSG.getMsg(AgentI18NResourceKeys.PLUGIN_CONTAINER_ARG_STOP))) {
            try {
                agent.shutdownPluginContainer();
                out.println(MSG.getMsg(AgentI18NResourceKeys.PLUGIN_CONTAINER_STOP_DONE));
            } catch (Exception e) {
                out.println(MSG.getMsg(AgentI18NResourceKeys.PLUGIN_CONTAINER_STOP_ERROR, ThrowableUtil
                    .getAllMessages(e)));
            }
        } else if (args[1].equals(MSG.getMsg(AgentI18NResourceKeys.PLUGIN_CONTAINER_ARG_STATUS))) {
            if (agent.isPluginContainerStarted()) {
                out.println(MSG.getMsg(AgentI18NResourceKeys.PLUGIN_CONTAINER_STATUS_STARTED));
            } else {
                out.println(MSG.getMsg(AgentI18NResourceKeys.PLUGIN_CONTAINER_STATUS_STOPPED));
            }

            out.println(MSG.getMsg(AgentI18NResourceKeys.PLUGIN_CONTAINER_STATUS_CONFIG, agent.getConfiguration()
                .getPluginContainerConfiguration()));
        } else {
            out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
        }

        return true;
    }

    /**
     * @see AgentPromptCommand#getSyntax()
     */
    public String getSyntax() {
        return MSG.getMsg(AgentI18NResourceKeys.PLUGIN_CONTAINER_SYNTAX);
    }

    /**
     * @see AgentPromptCommand#getHelp()
     */
    public String getHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.PLUGIN_CONTAINER_HELP);
    }

    /**
     * @see AgentPromptCommand#getDetailedHelp()
     */
    public String getDetailedHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.PLUGIN_CONTAINER_DETAILED_HELP);
    }
}