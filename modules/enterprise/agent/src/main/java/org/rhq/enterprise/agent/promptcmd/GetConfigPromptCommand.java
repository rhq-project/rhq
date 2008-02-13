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

import java.util.Map;
import java.util.TreeMap;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import mazz.i18n.Msg;
import org.rhq.enterprise.agent.AgentMain;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;

/**
 * Gets an agent configuration preference and displays it. If no arguments are given, then all preferences are
 * displayed.
 *
 * @author John Mazzitelli
 */
public class GetConfigPromptCommand implements AgentPromptCommand {
    private static final Msg MSG = AgentI18NFactory.getMsg();

    /**
     * @see AgentPromptCommand#getPromptCommandString()
     */
    public String getPromptCommandString() {
        return MSG.getMsg(AgentI18NResourceKeys.GETCONFIG);
    }

    /**
     * @see AgentPromptCommand#execute(AgentMain, String[])
     */
    public boolean execute(AgentMain agent, String[] args) {
        Preferences preferences = agent.getConfiguration().getPreferences();

        if (args.length == 1) {
            try {
                TreeMap<String, String> sortedMap = new TreeMap<String, String>();
                String[] keys = preferences.keys();
                for (int i = 0; i < keys.length; i++) {
                    sortedMap.put(keys[i], preferences.get(keys[i], MSG
                        .getMsg(AgentI18NResourceKeys.GETCONFIG_UNKNOWN_VALUE)));
                }

                for (Map.Entry<String, String> entry : sortedMap.entrySet()) {
                    agent.getOut().println(entry.getKey() + "=" + entry.getValue());
                }
            } catch (BackingStoreException e) {
                agent.getOut().println(MSG.getMsg(AgentI18NResourceKeys.GETCONFIG_CANNOT_GET, e));
            }
        } else {
            for (int i = 1; i < args.length; i++) {
                String prop = args[i];
                String prop_value = preferences.get(prop, MSG.getMsg(AgentI18NResourceKeys.GETCONFIG_UNKNOWN_VALUE));
                agent.getOut().println(prop + "=" + prop_value);
            }
        }

        return true;
    }

    /**
     * @see AgentPromptCommand#getSyntax()
     */
    public String getSyntax() {
        return MSG.getMsg(AgentI18NResourceKeys.GETCONFIG_SYNTAX);
    }

    /**
     * @see AgentPromptCommand#getHelp()
     */
    public String getHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.GETCONFIG_HELP);
    }

    /**
     * @see AgentPromptCommand#getDetailedHelp()
     */
    public String getDetailedHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.GETCONFIG_DETAILED_HELP);
    }
}