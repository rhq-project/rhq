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
package org.rhq.enterprise.server.core.comm;

import java.util.Hashtable;
import java.util.Map;

import org.rhq.core.clientapi.server.core.CoreServerService;
import org.rhq.core.domain.resource.Agent;
import org.rhq.enterprise.communications.ServiceContainer;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.impl.remotepojo.RemotePojoInvocationCommand;
import org.rhq.enterprise.communications.command.server.CommandAuthenticator;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This command authenticator implementation is designed to be installed on the JON Server so it can authenticate JON
 * Agents that are sending commands. It performs the necessary checks on the security token found within the command in
 * order to ensure the authenticity of the agent sending the command.
 *
 * @author John Mazzitelli
 */
public class SecurityTokenCommandAuthenticator implements CommandAuthenticator {
    /**
     * This is the name of the command configuration property whose value (if it exists) is the security token string.
     * Each incoming command should contain a security token within the command's
     * {@link Command#getConfiguration() configuration} unless it is a command that is designed to initially establish a
     * security token.
     */
    private static final String CMDCONFIG_PROP_SECURITY_TOKEN = "rhq.security-token";

    /**
     * A cache keyed on security token strings whose values are the timestamps when the token entries were stored in the
     * cache.
     */
    private static final Map<String, Long> TOKENS_CACHE = new Hashtable<String, Long>();

    /**
     * Items in the tokens cache will be valid for this amount of milliseconds. After an token grows this old in the
     * cache, that token's authenticity will again be checked with the database.
     */
    private static final long TOKENS_CACHE_TIMEOUT = 5 * 60 * 1000L;

    /**
     * This is the server service interface that contains the "register agent" API.
     */
    private static final String REGISTER_SERVICE_INTERFACE = CoreServerService.class.getName();

    /**
     * This is the name of the command type that will house all "register agent" commands.
     */
    private static final String REGISTER_COMMAND_TYPE_NAME = RemotePojoInvocationCommand.COMMAND_TYPE.getName();

    /**
     * EJB that is to be used to authenticate security tokens.
     */
    private AgentManagerLocal m_agentManager;

    /**
     * @see CommandAuthenticator#isAuthenticated(Command)
     */
    @Override
    public boolean isAuthenticated(Command command) {
        String security_token = command.getConfiguration().getProperty(CMDCONFIG_PROP_SECURITY_TOKEN);

        // if no security token is in the command, reject it unless this command is asking for a token.
        // if registering - allow it to be considered authenticated since its asking to be registered for the first time
        // if asking for additional properties - allow it, it might need this info for updating
        if (security_token == null) {
            return isRegisterCommand(command) || isGetPublicAgentUpdateEndpointAddressCommand(command);
        }

        // check the validity of the security token
        long now = System.currentTimeMillis();
        Long timestamp = TOKENS_CACHE.get(security_token);

        if ((timestamp == null) || ((timestamp.longValue() + TOKENS_CACHE_TIMEOUT) < now)) {
            try {
                AgentManagerLocal agentManager = getAgentManager();

                // see if the agent token is valid - note that we do not know if the token is coming
                // from the particular agent it is assigned to - for true security, SSL certs should be used
                Agent agent = agentManager.getAgentByAgentToken(security_token);
                if (agent == null) {
                    throw new NullPointerException();
                }

                TOKENS_CACHE.put(security_token, new Long(now));

                // let's take the opportunity to tell the server that this agent is up!
                agentManager.agentIsAlive(agent);
            } catch (Exception e) {
                TOKENS_CACHE.remove(security_token);
                return false;
            }
        }

        return true;
    }

    @Override
    public void setServiceContainer(ServiceContainer serviceContainer) {
        // don't need this, no need to store it
    }

    /**
     * This examines the given command and determines if it is a command asking to register an agent.
     *
     * @param  command
     *
     * @return <code>true</code> if the given command is asking to register an agent; any other command results in this
     *         method returning <code>false</code>
     */
    private boolean isRegisterCommand(Command command) {
        if (REGISTER_COMMAND_TYPE_NAME.equals(command.getCommandType().getName())) {
            RemotePojoInvocationCommand remote_cmd = (RemotePojoInvocationCommand) command;
            String iface_name = remote_cmd.getTargetInterfaceName();

            if (REGISTER_SERVICE_INTERFACE.equals(iface_name)) {
                String method_name = remote_cmd.getNameBasedInvocation().getMethodName();

                if ("registerAgent".equals(method_name)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isGetPublicAgentUpdateEndpointAddressCommand(Command command) {
        if (REGISTER_COMMAND_TYPE_NAME.equals(command.getCommandType().getName())) {
            RemotePojoInvocationCommand remote_cmd = (RemotePojoInvocationCommand) command;
            String iface_name = remote_cmd.getTargetInterfaceName();

            if (REGISTER_SERVICE_INTERFACE.equals(iface_name)) {
                String method_name = remote_cmd.getNameBasedInvocation().getMethodName();

                if ("getPublicAgentUpdateEndpointAddress".equals(method_name)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the EJB used to authenticate security tokens.
     *
     * @return EJB used to authenticate tokens
     *
     * @throws Exception
     */
    private AgentManagerLocal getAgentManager() throws Exception {
        if (m_agentManager == null) {
            m_agentManager = LookupUtil.getAgentManager();
        }

        return m_agentManager;
    }
}