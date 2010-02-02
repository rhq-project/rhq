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
package org.rhq.enterprise.agent;

import java.io.IOException;
import org.rhq.core.system.SystemInfo;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;
import org.rhq.enterprise.communications.util.prefs.PromptInput;

/**
 * Provides a {@link PromptInput} implementation that the agent can use to obtain input from the jline
 * command prompt in the agent.
 */
public class JlinePromptInfo implements PromptInput {
    private final AgentMain agent;

    /**
     * Constructor for {@link AgentNativePromptInfo} that is given the agent main to retrieve
     * the jline command prompt from.
     */
    public JlinePromptInfo(AgentMain agent) {
        this.agent = agent;
    }

    public String readLine() throws IOException {

        return agent.getIn().readLine();
    }

    public String readLineNoEcho() throws IOException {
        String input = null;

        while (true) {
            // get the answer the first time
            // A (char)0 will blank it out. I like stars
            input = agent.getIn().readLine('*');

            // get the answer a second time
            agent.getOut().write(agent.getI18NMsg().getMsg(AgentI18NResourceKeys.PROMPT_CONFIRM)) ;
            agent.getOut().println() ;
            String confirmation = agent.getIn().readLine('*');
            agent.getOut().println() ;        

            // make sure the first and second answers match; otherwise, ask again
            if (input.equals(confirmation)) {
                break;
            }

            agent.getOut().write(agent.getI18NMsg().getMsg(AgentI18NResourceKeys.PROMPT_CONFIRM_FAILED));
            agent.getOut().println() ;         
        }

        return input;
    }
}