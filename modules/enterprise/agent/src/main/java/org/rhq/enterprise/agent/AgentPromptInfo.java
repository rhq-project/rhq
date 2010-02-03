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
 * Provides a {@link PromptInput} implementation that the agent can use to obtain input from the native system. Note
 * that if the native libraries are not available or have been disabled, this will effectively fallback to using the
 * Java-only mechanism to read input and will therefore always echo the input (i.e. calling {@link #readLineNoEcho()}
 * will do the same as {@link #readLine()}).
 *
 * @author John Mazzitelli
 */
public class AgentPromptInfo implements PromptInput {

    private final AgentMain agent;

    public AgentPromptInfo(AgentMain agent) {
        this.agent = agent;
    }

    public String readLine() throws IOException {
        AgentInputReader input = this.agent.getIn();
        if (input != null) {
            return input.readLine();
        } else {
            throw new IOException("Agent not accepting input");
        }
    }

    public String readLineNoEcho() throws IOException {
        AgentInputReader input = this.agent.getIn();
        if (input != null) {
            return input.readLineNoEcho();
        } else {
            throw new IOException("Agent not accepting input");
        }
    }
}