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

import mazz.i18n.Logger;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.client.ClientCommandSender;
import org.rhq.enterprise.communications.command.client.CommandPreprocessor;

/**
 * This command preprocessor is intended to be installed on the agent's {@link ClientCommandSender} so that all commands
 * will get a security token inserted into their configuration.
 *
 * @author John Mazzitelli
 */
public class SecurityTokenCommandPreprocessor implements CommandPreprocessor {
    /**
     * Logger
     */
    private static final Logger LOG = AgentI18NFactory.getLogger(SecurityTokenCommandPreprocessor.class);

    /**
     * This is the name of the command configuration property that will get assigned the security token string.
     */
    private static final String CMDCONFIG_PROP_SECURITY_TOKEN = "rhq.security-token";

    /**
     * This is the name of the command configuration property that will get assigned the agent name.
     */
    private static final String CMDCONFIG_PROP_AGENT_NAME = "rhq.agent-name";

    /**
     * The agent configuration that will be used to persist the security token.
     */
    private AgentConfiguration m_agentConfiguration = null;

    /**
     * The name of the agent sending commands.
     */
    private String m_agentName = "unknown";

    /**
     * If the agent has a security token established, this will insert that security token in the command's
     * {@link Command#getConfiguration() configuration}.
     *
     * @see CommandPreprocessor#preprocess(Command, ClientCommandSender)
     */
    public void preprocess(Command command, ClientCommandSender sender) {
        String token = getAgentSecurityToken();

        if (token != null) {
            command.getConfiguration().setProperty(CMDCONFIG_PROP_SECURITY_TOKEN, token);
        }

        // as of 10/9/2008, this is here solely to be able to debug where a command was sent from
        command.getConfiguration().setProperty(CMDCONFIG_PROP_AGENT_NAME, m_agentName);

        return;
    }

    /**
     * This method is called during the startup of the agent so this preprocessor knows where to find and persist the
     * token.
     *
     * @param config the agent's configuration where the token is found/stored
     */
    public void setAgentConfiguration(AgentConfiguration config) {
        m_agentConfiguration = config;
        m_agentName = config.getAgentName();
    }

    /**
     * Gets the agent's security token and returns it.
     *
     * @return the agent's security token
     */
    private String getAgentSecurityToken() {
        String token = null;

        if (m_agentConfiguration != null) {
            token = m_agentConfiguration.getAgentSecurityToken();
        }

        if (token == null) {
            // ignore, we just don't have a security token yet - we'll end up returning null
            LOG.debug(AgentI18NResourceKeys.NO_SECURITY_TOKEN_YET);
        }

        return token;
    }
}