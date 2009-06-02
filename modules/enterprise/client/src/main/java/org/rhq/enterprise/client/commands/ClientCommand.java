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
package org.rhq.enterprise.client.commands;

import org.rhq.enterprise.client.ClientMain;

/**
 * @author Greg Hinkle, Simeon Pinder
 */
public interface ClientCommand {


    public static Class[] COMMANDS = new Class[] {
            FindResourcesCommand.class,
//            FindGroupsCommand.class,
            HelpCommand.class,
            LoginCommand.class,
            LogoutCommand.class,
            CreateUserCommand.class,
            FindUserAccountsCommand.class,
            DeleteUserCommand.class,
            QuitCommand.class,
            ScriptCommand.class
    };


    /**
     * All implementations must indicate what the prompt command is that will trigger its execution. This method returns
     * the prompt command name.
     *
     * @return the prompt command string - if the first prompt argument is this value, then this prompt command will be
     *         executed.
     */
    String getPromptCommandString();

    /**
     * Executes the agent prompt command with the given arguments.
     *
     * @param client the ClientMain class itself
     * @param args  the arguments passed to the agent on the agent prompt
     * @return <code>true</code> if the agent can continue accepting prompt commands; <code>false</code> if the agent
     *         should die
     */
    boolean execute(ClientMain client, String[] args);

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