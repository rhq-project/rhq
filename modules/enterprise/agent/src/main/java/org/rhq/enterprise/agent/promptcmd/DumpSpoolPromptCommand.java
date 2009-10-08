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
import mazz.i18n.Msg;
import org.rhq.enterprise.agent.AgentConfiguration;
import org.rhq.enterprise.agent.AgentMain;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;
import org.rhq.enterprise.communications.command.client.ClientCommandSenderConfiguration;
import org.rhq.enterprise.communications.command.client.PersistentFifo;
import org.rhq.enterprise.communications.util.DumpBytes;

/**
 * Dumps the contents of the persisted command spool file. This is probably a rarely used command mainly for testing and
 * debugging. It allows you to see what commands are currently stored in the command spool file in case you are
 * interested to know what commands marked for guaranteed delivery have been persisted but not yet sent.
 *
 * @author John Mazzitelli
 */
public class DumpSpoolPromptCommand implements AgentPromptCommand {
    private static final Msg MSG = AgentI18NFactory.getMsg();

    /**
     * @see AgentPromptCommand#getPromptCommandString()
     */
    public String getPromptCommandString() {
        return MSG.getMsg(AgentI18NResourceKeys.DUMPSPOOL);
    }

    /**
     * @see AgentPromptCommand#execute(AgentMain, String[])
     */
    public boolean execute(AgentMain agent, String[] args) {
        AgentConfiguration agent_config = agent.getConfiguration();
        ClientCommandSenderConfiguration sender_config = agent_config.getClientCommandSenderConfiguration();

        if (sender_config.commandSpoolFileName == null) {
            agent.getOut().println(MSG.getMsg(AgentI18NResourceKeys.DUMPSPOOL_NO_FILE));
            return true;
        }

        int raw_byte_base = -2; // -2 will indicate an error

        if (args.length > 1) {
            if (args.length == 2) {
                if (args[1].equals(MSG.getMsg(AgentI18NResourceKeys.DUMPSPOOL_OBJ))) {
                    raw_byte_base = 0;
                } else if (args[1].equals(MSG.getMsg(AgentI18NResourceKeys.DUMPSPOOL_HEX))) {
                    raw_byte_base = DumpBytes.BASE_HEX;
                }
            }
        } else {
            raw_byte_base = -1;
        }

        if (raw_byte_base == -2) {
            // wrong number of arguments or arguments were invalid
            agent.getOut().println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
            return true;
        }

        File spool_file = new File(sender_config.dataDirectory, sender_config.commandSpoolFileName);

        try {
            PersistentFifo.dumpContents(agent.getOut(), spool_file, agent_config
                .isClientSenderCommandSpoolFileCompressed(), raw_byte_base);
        } catch (Exception e) {
            agent.getOut().println(MSG.getMsg(AgentI18NResourceKeys.DUMPSPOOL_ERROR, spool_file, e));
        }

        return true;
    }

    /**
     * @see AgentPromptCommand#getSyntax()
     */
    public String getSyntax() {
        return MSG.getMsg(AgentI18NResourceKeys.DUMPSPOOL_SYNTAX);
    }

    /**
     * @see AgentPromptCommand#getHelp()
     */
    public String getHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.DUMPSPOOL_HELP);
    }

    /**
     * @see AgentPromptCommand#getDetailedHelp()
     */
    public String getDetailedHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.DUMPSPOOL_DETAILED_HELP);
    }
}