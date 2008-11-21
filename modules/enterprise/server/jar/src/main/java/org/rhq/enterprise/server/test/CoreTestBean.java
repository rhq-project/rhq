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
package org.rhq.enterprise.server.test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Properties;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.rhq.core.clientapi.server.core.AgentNotSupportedException;
import org.rhq.core.clientapi.server.core.AgentRegistrationException;
import org.rhq.core.clientapi.server.core.AgentRegistrationRequest;
import org.rhq.core.clientapi.server.core.AgentRegistrationResults;
import org.rhq.core.clientapi.server.core.CoreServerService;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.core.CoreServerServiceImpl;

/**
 * An EJB for testing the core subsystem - used by TestControl.jsp.
 */
@Stateless
public class CoreTestBean implements CoreTestLocal {
    static final String TEST_AGENT_ADDRESS = "127.0.0.2";
    static final int TEST_AGENT_PORT = 2145;
    static final String TEST_AGENT_REMOTE_ENDPOINT = "socket://" + TEST_AGENT_ADDRESS + ":" + TEST_AGENT_PORT
        + "/?rhqtype=agent";

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    private CoreServerService coreServerService = new CoreServerServiceImpl();

    public boolean isTestAgentReported() {
        try {
            getTestAgent();
            return true;
        } catch (NoResultException nre) {
            return false;
        }
    }

    public void enableHibernateStatistics() {
        PersistenceUtility.enableHibernateStatistics(entityManager, ManagementFactory.getPlatformMBeanServer());
    }

    /**
     * Build a fake agent and register it
     */
    public AgentRegistrationResults registerTestAgent() {
        AgentRegistrationRequest registrationRequest = new AgentRegistrationRequest(TEST_AGENT_ADDRESS,
            TEST_AGENT_ADDRESS, TEST_AGENT_PORT, TEST_AGENT_REMOTE_ENDPOINT, true, null, null);
        try {
            return this.coreServerService.registerAgent(registrationRequest);
        } catch (AgentRegistrationException e) {
            throw new RuntimeException(e);
        } catch (AgentNotSupportedException e2) {
            throw new RuntimeException(e2);
        }
    }

    public Properties getLatestConfiguration() {
        return new Properties(); // TODO: Implement this method.
    }

    public List<Plugin> getLatestPlugins() {
        // TODO Auto-generated method stub
        return null;
    }

    public InputStream getPluginArchive(String pluginName) {
        // TODO Auto-generated method stub
        return null;
    }

    public InputStream getFileContents(String file) {
        // TODO: Implement me
        return new ByteArrayInputStream(("Test contents of " + file).getBytes());
    }

    public Agent getTestAgent() {
        return (Agent) entityManager.createNamedQuery(Agent.QUERY_FIND_BY_ADDRESS_AND_PORT).setParameter("address",
            TEST_AGENT_ADDRESS).setParameter("port", TEST_AGENT_PORT).getSingleResult();
    }
}