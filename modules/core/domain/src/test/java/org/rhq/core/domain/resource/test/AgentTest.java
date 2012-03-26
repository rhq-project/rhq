/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.domain.resource.test;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.testng.annotations.Test;

import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.composite.AgentLastAvailabilityPingComposite;
import org.rhq.core.domain.test.AbstractEJB3Test;

@Test(groups = "integration.ejb3")
public class AgentTest extends AbstractEJB3Test {
    private long now = System.currentTimeMillis();

    public void testSuspectAgentQuery() throws Exception {
        List<AgentLastAvailabilityPingComposite> pings;
        int i;

        getTransactionManager().begin();
        try {
            EntityManager em = getEntityManager();

            // get our baseline, in case you already have data in your DB
            Query q = em.createNamedQuery(Agent.QUERY_FIND_ALL_SUSPECT_AGENTS);
            q.setParameter("dateThreshold", now - 2500);
            int baselineSize = q.getResultList().size();

            Agent agent1 = createAgent(em, 1, 1000);
            Agent agent2 = createAgent(em, 2, 2000);
            Agent agent3 = createAgent(em, 3, 3000);
            Agent agent4 = createAgent(em, 4, 4000);

            // make sure they were persisted
            em.flush();
            assert em.find(Agent.class, agent1.getId()) != null;
            assert em.find(Agent.class, agent2.getId()) != null;
            assert em.find(Agent.class, agent3.getId()) != null;
            assert em.find(Agent.class, agent4.getId()) != null;

            q = em.createNamedQuery(Agent.QUERY_FIND_ALL_SUSPECT_AGENTS);
            q.setParameter("dateThreshold", now - 2500);
            pings = q.getResultList();
            assert pings.size() == 2 + baselineSize : pings;

            i = pings.indexOf(new AgentLastAvailabilityPingComposite(agent3.getId(), null, null, null, false));
            assert pings.get(i).getAgentId() == agent3.getId();
            assert pings.get(i).getAgentName().equals(agent3.getName());
            assert pings.get(i).getRemoteEndpoint().equals(agent3.getRemoteEndpoint());
            assert pings.get(i).getLastAvailabilityPing().equals(agent3.getLastAvailabilityPing());

            i = pings.indexOf(new AgentLastAvailabilityPingComposite(agent4.getId(), null, null, null, false));
            assert pings.get(i).getAgentId() == agent4.getId();
            assert pings.get(i).getAgentName().equals(agent4.getName());
            assert pings.get(i).getRemoteEndpoint().equals(agent4.getRemoteEndpoint());
            assert pings.get(i).getLastAvailabilityPing().equals(agent4.getLastAvailabilityPing());

            assert -1 == pings.indexOf(new AgentLastAvailabilityPingComposite(agent2.getId(), null, null, null, false));
            assert -1 == pings.indexOf(new AgentLastAvailabilityPingComposite(agent1.getId(), null, null, null, false));

            // add an agent with a null in the date column
            Agent agent0 = createAgent(em, 0, -1);
            assert em.find(Agent.class, agent0.getId()) != null;
            pings = q.getResultList();
            assert pings.size() == 2 + baselineSize : pings;

            i = pings.indexOf(new AgentLastAvailabilityPingComposite(agent3.getId(), null, null, null, false));
            assert pings.get(i).getAgentId() == agent3.getId();
            assert pings.get(i).getAgentName().equals(agent3.getName());
            assert pings.get(i).getRemoteEndpoint().equals(agent3.getRemoteEndpoint());
            assert pings.get(i).getLastAvailabilityPing().equals(agent3.getLastAvailabilityPing());

            i = pings.indexOf(new AgentLastAvailabilityPingComposite(agent4.getId(), null, null, null, false));
            assert pings.get(i).getAgentId() == agent4.getId();
            assert pings.get(i).getAgentName().equals(agent4.getName());
            assert pings.get(i).getRemoteEndpoint().equals(agent4.getRemoteEndpoint());
            assert pings.get(i).getLastAvailabilityPing().equals(agent4.getLastAvailabilityPing());

            assert -1 == pings.indexOf(new AgentLastAvailabilityPingComposite(agent2.getId(), null, null, null, false));
            assert -1 == pings.indexOf(new AgentLastAvailabilityPingComposite(agent1.getId(), null, null, null, false));

            // get all of them, except the one with the null
            q.setParameter("dateThreshold", now - 1);
            pings = q.getResultList();
            assert pings.size() == 4 + baselineSize : pings;

            // get none of them
            q.setParameter("dateThreshold", now - 10000);
            pings = q.getResultList();
            assert pings.size() == 0 + baselineSize : pings;
        } finally {
            getTransactionManager().rollback();
        }
    }

    private Agent createAgent(EntityManager em, int num, long availDateOffsetFromNow) {
        Agent agent = new Agent("agent" + num, "address" + num, num, "remoteaddr" + num, "token" + num);
        if (availDateOffsetFromNow > 0) {
            agent.setLastAvailabilityPing(now - availDateOffsetFromNow);
        } else {
            agent.setLastAvailabilityPing(null);
        }

        em.persist(agent);
        return agent;
    }
}