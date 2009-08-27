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

import org.rhq.core.domain.util.serial.ExternalizableStrategy;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.client.ClientCommandSender;
import org.rhq.enterprise.communications.command.client.CommandPreprocessor;

/**
 * This command preprocessor is intended to be installed on the agent's {@link ClientCommandSender} so that all commands
 * will get an ExternalizableStrategy inserted into their configuration.
 *
 * @author Jay Shaughnessy
 */
public class ExternalizableStrategyCommandPreprocessor implements CommandPreprocessor {
    /**
     * This is the name of the command configuration property that will direct return value serialization
     */
    private static final String CMDCONFIG_PROP_EXTERNALIZABLE_STRATEGY = "rhq.externalizable-strategy";

    /**
     * The agent configuration that will be used to persist the security token.
     */
    private AgentConfiguration m_agentConfiguration = null;

    /**
     * Ensure the AGENT strategy is employed.
     *
     * @see CommandPreprocessor#preprocess(Command, ClientCommandSender)
     */
    public void preprocess(Command command, ClientCommandSender sender) {
        // Ensures that command return values will be serialized in the correct fashion
        command.getConfiguration().setProperty(CMDCONFIG_PROP_EXTERNALIZABLE_STRATEGY,
            ExternalizableStrategy.Subsystem.AGENT.name());

        return;
    }
}