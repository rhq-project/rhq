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

import org.rhq.enterprise.agent.AgentMain;

/**
 * This is the superclass to all the agent prompt commands - these are simple commands that are executed when the user
 * asks to execute them from the agent prompt. These are different than the remote commands defined by the command
 * framework - prompt commands are local only and can only be executed if the user specifies it from the agent prompt.
 * You can not remotely execute these prompt commands.
 *
 * @author John Mazzitelli
 */
public interface AgentPromptCommand {
    /**
     * All implementations must indicate what the prompt command is that will trigger its execution. This method returns
     * the prompt command name.
     *
     * @return the prompt command string - if the first prompt argument is this value, then this prompt command will be
     *         executed.
     */
    String getPromptCommandString();

    /**
     * A command can optionally have one or more aliases to allow for a more flexible user experience. Aliases may
     * include abbreviations ("quit" -> "q") as well as synonyms ("exit" <--> "quit")
     *
     * @return the set of prompt command string aliases - if the first prompt argument is not the value returned from
     *         AgentPromptCommand#getPromptCommandString() , but matches any of the values in this set, then this prompt
     *         command will be executed.
     */
    //Set<String> getPromptCommandStringAliases();
    /**
     * Executes the agent prompt command with the given arguments.
     *
     * @param  agent the agent itself
     * @param  args  the arguments passed to the agent on the agent prompt
     *
     * @return <code>true</code> if the agent can continue accepting prompt commands; <code>false</code> if the agent
     *         should die
     */
    boolean execute(AgentMain agent, String[] args);

    /**
     * Returns a one-line usage string to describe the syntax of the command including all optional and required
     * arguments.
     *
     * @return syntax string
     */
    String getSyntax();

    /**
     * Returns a help summary to describe to the user what the prompt command does and its purpose. It is usually a
     * short one line help summary.
     *
     * @return help string
     */
    String getHelp();

    /**
     * Returns a detailed help message to describe to the user what the command's syntax is and any detailed information
     * that is useful to the user that wants to use this command.
     *
     * @return detailed help string
     */
    String getDetailedHelp();
}