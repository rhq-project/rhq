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

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import mazz.i18n.Msg;

import org.rhq.enterprise.agent.AgentMain;
import org.rhq.enterprise.agent.Version;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;

/**
 * Displays the version information of the agent.
 *
 * @author John Mazzitelli
 */
public class VersionPromptCommand implements AgentPromptCommand {
    private static final Msg MSG = AgentI18NFactory.getMsg();

    /**
     * @see AgentPromptCommand#getPromptCommandString()
     */
    public String getPromptCommandString() {
        return MSG.getMsg(AgentI18NResourceKeys.VERSION);
    }

    /**
     * @see AgentPromptCommand#execute(AgentMain, String[])
     */
    public boolean execute(AgentMain agent, String[] args) {
        PrintWriter out = agent.getOut();
        String versionString = Version.getVersionPropertiesAsString();

        if (args.length <= 1) {
            out.println(versionString);
            return true;
        }

        String sopts = "s::e::";
        LongOpt[] lopts = { new LongOpt("sysprops", LongOpt.OPTIONAL_ARGUMENT, null, 's'),
            new LongOpt("env", LongOpt.OPTIONAL_ARGUMENT, null, 'e') };

        Getopt getopt = new Getopt(getPromptCommandString(), args, sopts, lopts);
        int code;

        while ((code = getopt.getopt()) != -1) {
            switch (code) {
            case ':':
            case '?':
            case 1: {
                out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
                break;
            }

            case 's': {
                out.println(versionString);
                out.println(MSG.getMsg(AgentI18NResourceKeys.VERSION_SYSPROPS_LABEL));
                out.println();

                String opt = getopt.getOptarg();

                Properties sysprops = System.getProperties();
                for (Map.Entry<Object, Object> sysprop : sysprops.entrySet()) {
                    if (opt == null || sysprop.getKey().toString().startsWith(opt)) {
                        out.println(sysprop.getKey() + "=" + sysprop.getValue());
                    }
                }
                break;
            }

            case 'e': {
                out.println(versionString);
                out.println(MSG.getMsg(AgentI18NResourceKeys.VERSION_ENV_LABEL));
                out.println();

                String opt = getopt.getOptarg();

                Map<String, String> envvars = System.getenv();
                if (envvars == null) {
                    envvars = new HashMap<String, String>();
                }
                for (Map.Entry<String, String> envvar : envvars.entrySet()) {
                    if (opt == null || envvar.getKey().toString().startsWith(opt)) {
                        out.println(envvar.getKey() + "=" + envvar.getValue());
                    }
                }
                break;
            }
            }
        }

        if ((getopt.getOptind() + 1) < args.length) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
        }

        return true;
    }

    /**
     * @see AgentPromptCommand#getSyntax()
     */
    public String getSyntax() {
        return MSG.getMsg(AgentI18NResourceKeys.VERSION_SYNTAX);
    }

    /**
     * @see AgentPromptCommand#getHelp()
     */
    public String getHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.VERSION_HELP);
    }

    /**
     * @see AgentPromptCommand#getDetailedHelp()
     */
    public String getDetailedHelp() {
        return getHelp();
    }
}