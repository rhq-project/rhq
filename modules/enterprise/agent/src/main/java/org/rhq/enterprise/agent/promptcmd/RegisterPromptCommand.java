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
import org.rhq.core.clientapi.server.core.AgentRegistrationResults;
import org.rhq.enterprise.agent.AgentMain;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;

/**
 * Attempts to register the agent with the server which assigns a security token to this agent.
 *
 * @author John Mazzitelli
 */
public class RegisterPromptCommand implements AgentPromptCommand {
    private static final Msg MSG = AgentI18NFactory.getMsg();

    /**
     * @see AgentPromptCommand#getPromptCommandString()
     */
    public String getPromptCommandString() {
        return MSG.getMsg(AgentI18NResourceKeys.REGISTER);
    }

    /**
     * @see AgentPromptCommand#execute(AgentMain, String[])
     */
    public boolean execute(AgentMain agent, String[] args) {
        if (!agent.isStarted()) {
            agent.getOut().println(MSG.getMsg(AgentI18NResourceKeys.REGISTER_MUST_BE_STARTED));
            return true;
        }

        PrintWriter out = agent.getOut();
        long wait = 30L;
        boolean regenerate = false;

        if (args.length > 3) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
            return true;
        } else if ((args.length == 2) || (args.length == 3)) {
            try {
                // this code is weird because the first or second arg can be wait or regenerate
                Long wait_arg = getWait(args[1]);
                regenerate = (wait_arg == null);

                if (args.length == 3) {
                    if (wait_arg == null) {
                        wait_arg = getWait(args[2]);
                    } else {
                        regenerate = getRegenerate(args[2]);
                    }
                }

                if (wait_arg != null) {
                    wait = wait_arg.longValue();
                }
            } catch (NumberFormatException nfe) {
                out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
                return true;
            }
        }

        out.println(MSG.getMsg(AgentI18NResourceKeys.REGISTER_SENT_REQUEST, regenerate));
        out.println(MSG.getMsg(AgentI18NResourceKeys.REGISTER_WAITING, wait));

        agent.registerWithServer(wait * 1000L, regenerate);
        AgentRegistrationResults registration = agent.getAgentRegistration();

        if (registration == null) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.REGISTER_FAILED));
        } else {
            out.println(MSG.getMsg(AgentI18NResourceKeys.REGISTER_REGISTRATION, registration));
        }

        return true;
    }

    /**
     * @see AgentPromptCommand#getSyntax()
     */
    public String getSyntax() {
        return MSG.getMsg(AgentI18NResourceKeys.REGISTER_SYNTAX);
    }

    /**
     * @see AgentPromptCommand#getHelp()
     */
    public String getHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.REGISTER_HELP);
    }

    /**
     * @see AgentPromptCommand#getDetailedHelp()
     */
    public String getDetailedHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.REGISTER_DETAILED_HELP);
    }

    /**
     * Returns <code>true</code> if the given argument is the regenerate argument.
     *
     * @param  arg
     *
     * @return <code>true</code> if arg was the regenerate arg; <code>false</code> otherwise
     */
    private boolean getRegenerate(String arg) {
        return arg.equalsIgnoreCase(MSG.getMsg(AgentI18NResourceKeys.REGISTER_REGENERATE));
    }

    /**
     * Returns the wait time if the given argument is a number. If it is not, but is the
     * {@link #getRegenerate(String) regenerate} flag, null is returned. Otherwise, a NumberFormatException is thrown.
     *
     * @param  arg
     *
     * @return the wait time if the arg is a number; <code>null</code> if it is not but is the regenerate flag
     *
     * @throws NumberFormatException if the arg is neither a valid value nor the regenerate flag
     */
    private Long getWait(String arg) throws NumberFormatException {
        try {
            Long wait = Long.valueOf(arg);

            // note that if the wait time specified is 0 or less, the wait time will remain 30 seconds
            if (wait.longValue() <= 0L) {
                wait = Long.valueOf(30L);
            }

            return wait;
        } catch (NumberFormatException nfe) {
            if (!getRegenerate(arg)) {
                throw nfe;
            }

            return null;
        }
    }
}