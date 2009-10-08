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

import mazz.i18n.Logger;
import mazz.i18n.LoggerLocale;
import mazz.i18n.Msg;
import org.rhq.enterprise.agent.AgentMain;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;

/**
 * Command to configure some of the log settings.
 *
 * @author John Mazzitelli
 */
public class LogPromptCommand implements AgentPromptCommand {
    private static final Msg MSG = AgentI18NFactory.getMsg();

    /**
     * @see AgentPromptCommand#getPromptCommandString()
     */
    public String getPromptCommandString() {
        return MSG.getMsg(AgentI18NResourceKeys.LOG);
    }

    /**
     * @see AgentPromptCommand#execute(AgentMain, String[])
     */
    public boolean execute(AgentMain agent, String[] args) {
        boolean show_current_settings = false;

        try {
            if (args.length == 1) {
                show_current_settings = true;
            } else if (args.length != 3) {
                agent.getOut().println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
            } else {
                String subcommand = args[1];
                String subcommand_arg = args[2];

                if (subcommand.equals(MSG.getMsg(AgentI18NResourceKeys.LOG_LOCALE))) {
                    LoggerLocale.setLogLocale(subcommand_arg);
                    show_current_settings = true;
                } else if (subcommand.equals(MSG.getMsg(AgentI18NResourceKeys.LOG_DUMPSTACKS))) {
                    boolean flag = subcommand_arg.equalsIgnoreCase(MSG.getMsg(AgentI18NResourceKeys.LOG_TRUE));
                    Logger.setDumpStackTraces(flag);
                    show_current_settings = true;
                } else if (subcommand.equals(MSG.getMsg(AgentI18NResourceKeys.LOG_DUMPKEYS))) {
                    boolean flag = subcommand_arg.equalsIgnoreCase(MSG.getMsg(AgentI18NResourceKeys.LOG_TRUE));
                    Logger.setDumpLogKeys(flag);
                    show_current_settings = true;
                } else {
                    agent.getOut().println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
                }
            }
        } catch (Exception e) {
            agent.getOut().println(MSG.getMsg(AgentI18NResourceKeys.LOG_FAILURE));
            e.printStackTrace(agent.getOut());
        }

        // if our commands were successful, we are going to show the settings
        if (show_current_settings) {
            agent.getOut().println(
                MSG.getMsg(AgentI18NResourceKeys.LOG_SHOW_CURRENT_SETTINGS, LoggerLocale.getLogLocale(), Logger
                    .getDumpStackTraces(), Logger.getDumpLogKeys()));
        }

        return true;
    }

    /**
     * @see AgentPromptCommand#getSyntax()
     */
    public String getSyntax() {
        return MSG.getMsg(AgentI18NResourceKeys.LOG_SYNTAX);
    }

    /**
     * @see AgentPromptCommand#getHelp()
     */
    public String getHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.LOG_HELP);
    }

    /**
     * @see AgentPromptCommand#getDetailedHelp()
     */
    public String getDetailedHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.LOG_DETAILED_HELP);
    }
}