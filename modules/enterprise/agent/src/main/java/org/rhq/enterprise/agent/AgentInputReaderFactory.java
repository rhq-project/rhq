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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;

import mazz.i18n.Logger;

import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;

/**
 * Creates implementations of the input reader depending on the need for a native console library.
 *
 * @author John Mazzitelli
 */
public class AgentInputReaderFactory {
    private static final Logger LOG = AgentI18NFactory.getLogger(AgentInputReaderFactory.class);

    public static enum ConsoleType {
        java, sigar, jline
    }

    private static ConsoleType consoleType = ConsoleType.jline;

    public static AgentInputReader create(AgentMain agent) throws IOException {
        AgentInputReader input = null;

        try {
            switch (consoleType) {
            case java:
                input = new JavaAgentInputReader();
                break;
            case sigar:
                input = new SigarAgentInputReader(agent);
                break;
            case jline:
                input = new JLineAgentInputReader(agent);
                break;
            }
        } catch (Exception e) {
            input = null;
            LOG.warn(e, AgentI18NResourceKeys.AGENT_INPUT_READER_FACTORY_ERROR, consoleType);
        }

        if (input == null) {
            input = new JavaAgentInputReader();
        }

        return input;
    }

    public static AgentInputReader create(AgentMain agent, File file) throws IOException {
        AgentInputReader input = null;

        try {
            switch (consoleType) {
            case java:
                input = new JavaAgentInputReader(new FileReader(file));
                break;
            case sigar:
                input = new SigarAgentInputReader(agent, new FileReader(file));
                break;
            case jline:
                input = new JLineAgentInputReader(agent, new FileInputStream(file));
                break;
            }
        } catch (Exception e) {
            input = null;
            LOG.warn(e, AgentI18NResourceKeys.AGENT_INPUT_READER_FACTORY_ERROR, consoleType);
        }

        if (input == null) {
            input = new JavaAgentInputReader(new FileReader(file));
        }

        return input;
    }

    public static ConsoleType getConsoleType() {
        return consoleType;
    }

    public static void setConsoleType(ConsoleType type) {
        consoleType = type;
    }

    public static void setConsoleType(String typeStr) {
        try {
            setConsoleType(ConsoleType.valueOf(typeStr));
        } catch (Exception e) {
            throw new IllegalArgumentException(LOG.getMsgString(
                AgentI18NResourceKeys.AGENT_INPUT_READER_FACTORY_BAD_TYPE, typeStr));
        }
    }
}
