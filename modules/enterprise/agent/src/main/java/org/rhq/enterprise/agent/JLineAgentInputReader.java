/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.enterprise.agent;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;

import jline.ConsoleReader;

public class JLineAgentInputReader implements AgentInputReader {

    private final ConsoleReader jline;
    private final AgentMain agent;
    private final boolean consoleInput;

    public JLineAgentInputReader(AgentMain agent) throws IOException {
        this.jline = new ConsoleReader();
        this.agent = agent;
        this.consoleInput = true;
    }

    public JLineAgentInputReader(AgentMain agent, FileInputStream fis) throws IOException {
        this.jline = new ConsoleReader(fis, agent.getOut());
        this.agent = agent;
        this.consoleInput = false;
    }

    public boolean isConsole() {
        return this.consoleInput;
    }

    public String readLine() throws IOException {
        return this.jline.readLine();
    }

    public String readLineNoEcho() throws IOException {
        String input = null;

        while (true) {
            // get the answer the first time - the '*' is echoed back to the user
            input = jline.readLine(Character.valueOf('*'));

            if (!isConsole()) {
                break; // not reading input from a console, so we don't ask for confirmation
            }

            // get the answer a second time
            this.agent.getOut().write(this.agent.getI18NMsg().getMsg(AgentI18NResourceKeys.PROMPT_CONFIRM));
            this.agent.getOut().println();
            String confirmation = jline.readLine(Character.valueOf('*'));
            this.agent.getOut().println();

            // make sure the first and second answers match; otherwise, ask again
            if (input.equals(confirmation)) {
                break;
            }

            this.agent.getOut().write(this.agent.getI18NMsg().getMsg(AgentI18NResourceKeys.PROMPT_CONFIRM_FAILED));
            this.agent.getOut().println();
        }

        return input;
    }

    public void close() throws IOException {
        InputStream input = jline.getInput();
        if (input != null) {
            input.close();
        }
    }
}
