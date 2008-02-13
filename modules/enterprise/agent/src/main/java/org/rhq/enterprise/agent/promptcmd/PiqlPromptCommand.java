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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import mazz.i18n.Msg;
import org.rhq.core.system.ProcessInfo;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.system.pquery.ProcessInfoQuery;
import org.rhq.enterprise.agent.AgentMain;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;

public class PiqlPromptCommand implements AgentPromptCommand {
    /**
     * The object used to get I18N messages suitable for displaying to the user.
     */
    private static final Msg MSG = AgentI18NFactory.getMsg();

    public String getPromptCommandString() {
        return MSG.getMsg(AgentI18NResourceKeys.PIQL);
    }

    public boolean execute(AgentMain agent, String[] args) {
        PrintWriter out = agent.getOut();
        boolean verbose = false;
        String piql;

        if (args.length == 2) {
            piql = args[1];
        } else if ((args.length == 3) && args[1].equals(MSG.getMsg(AgentI18NResourceKeys.PIQL_VERBOSE))) {
            piql = args[2];
            verbose = true;
        } else {
            out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
            return true;
        }

        try {
            SystemInfo info = SystemInfoFactory.createSystemInfo();
            List<ProcessInfo> processes = info.getAllProcesses();
            ProcessInfoQuery query = new ProcessInfoQuery(processes);
            List<ProcessInfo> results = query.query(piql);

            out.println(MSG.getMsg(AgentI18NResourceKeys.PIQL_RESULTS_HEADER, piql));

            for (ProcessInfo process : results) {
                if (verbose) {
                    out.println(MSG.getMsg(AgentI18NResourceKeys.PIQL_RESULTS_FULL, process.getPid(), process
                        .getParentPid(), process.getName(), new Date(process.getTime().getStartTime()), process
                        .getTime().getSys(), process.getTime().getUser(), Arrays.toString(process.getCommandLine())));
                } else {
                    out.println(MSG.getMsg(AgentI18NResourceKeys.PIQL_RESULTS_SHORT, process.getPid(), process
                        .getName()));
                }
            }
        } catch (UnsupportedOperationException uoe) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.PIQL_NO_NATIVE_SUPPORT, uoe.getMessage()));
        } catch (Exception e) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.PIQL_FAILURE, e.getMessage()));
        }

        return true;
    }

    public String getSyntax() {
        return MSG.getMsg(AgentI18NResourceKeys.PIQL_SYNTAX);
    }

    public String getHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.PIQL_HELP);
    }

    public String getDetailedHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.PIQL_DETAILED_HELP);
    }
}