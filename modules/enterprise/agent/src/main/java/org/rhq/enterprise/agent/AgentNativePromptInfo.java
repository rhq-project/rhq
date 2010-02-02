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
public class AgentNativePromptInfo implements PromptInput {
    private final SystemInfo systemInfo;
    private final AgentMain agent;

    /**
     * Constructor for {@link AgentNativePromptInfo} that is given the system info used to obtain the console input
     * natively. The agent is provided so it can give us a fallback stream where input can be read in case either <code>
     * sysinfo</code> is <code>null</code> or it is not a native implementation (as will be the case if the platform
     * where this object is running does not have native libraries for it).
     *
     * @param sysinfo the object where native console input can be read (may be <code>null</code>)
     * @param agent   the agent that will be doing the prompting and will provide us with a fallback input stream if
     *                <code>sysinfo</code> is <code>null</code> or not a native implementation
     */
    public AgentNativePromptInfo(SystemInfo sysinfo, AgentMain agent) {
        this.systemInfo = sysinfo;
        this.agent = agent;
    }

    public String readLine() throws IOException {
        String input;

        if ((systemInfo == null) || !systemInfo.isNative()) {
            input = agent.getIn().readLine();
        } else {
            input = systemInfo.readLineFromConsole(false);
        }

        return input;
    }

    public String readLineNoEcho() throws IOException {
        String input = null;

        if ((systemInfo == null) || !systemInfo.isNative()) {
            input = agent.getIn().readLine();
        } else {
            while (true) {
                // get the answer the first time
                input = systemInfo.readLineFromConsole(true);

                // get the answer a second time
                systemInfo.writeLineToConsole(agent.getI18NMsg().getMsg(AgentI18NResourceKeys.PROMPT_CONFIRM));
                String confirmation = systemInfo.readLineFromConsole(true);
                systemInfo.writeLineToConsole("\n");

                // make sure the first and second answers match; otherwise, ask again
                if (input.equals(confirmation)) {
                    break;
                }

                systemInfo.writeLineToConsole(agent.getI18NMsg().getMsg(AgentI18NResourceKeys.PROMPT_CONFIRM_FAILED));
            }
        }

        return input;
    }
}