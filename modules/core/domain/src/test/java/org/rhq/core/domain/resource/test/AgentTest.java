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
package org.rhq.core.domain.resource.test;

import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import org.testng.annotations.Test;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.composite.AgentLastAvailabilityReportComposite;
import org.rhq.core.domain.test.AbstractEJB3Test;

@Test
public class AgentTest extends AbstractEJB3Test {
    private long now = System.currentTimeMillis();

    public void testSuspectAgentQuery() throws Exception {
        List<AgentLastAvailabilityReportComposite> reports;
        int i;

        getTransactionManager().begin();
        try {
            EntityManager em = getEntityManager();

            // get our baseline, in case you already have data in your DB
            Query q = em.createNamedQuery(Agent.QUERY_FIND_ALL_SUSPECT_AGENTS);
            q.setParameter("dateThreshold", new Date(now - 2500), TemporalType.TIMESTAMP);
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
            q.setParameter("dateThreshold", new Date(now - 2500), TemporalType.TIMESTAMP);
            reports = q.getResultList();
            assert reports.size() == 2 + baselineSize : reports;

            i = reports.indexOf(new AgentLastAvailabilityReportComposite(agent3.getId(), null, null, null));
            assert reports.get(i).getAgentId() == agent3.getId();
            assert reports.get(i).getAgentName().equals(agent3.getName());
            assert reports.get(i).getRemoteEndpoint().equals(agent3.getRemoteEndpoint());
            assert reports.get(i).getLastAvailabilityReport().getTime() == agent3.getLastAvailabilityReport().getTime();

            i = reports.indexOf(new AgentLastAvailabilityReportComposite(agent4.getId(), null, null, null));
            assert reports.get(i).getAgentId() == agent4.getId();
            assert reports.get(i).getAgentName().equals(agent4.getName());
            assert reports.get(i).getRemoteEndpoint().equals(agent4.getRemoteEndpoint());
            assert reports.get(i).getLastAvailabilityReport().getTime() == agent4.getLastAvailabilityReport().getTime();

            assert -1 == reports.indexOf(new AgentLastAvailabilityReportComposite(agent2.getId(), null, null, null));
            assert -1 == reports.indexOf(new AgentLastAvailabilityReportComposite(agent1.getId(), null, null, null));

            // add an agent with a null in the date column
            Agent agent0 = createAgent(em, 0, -1);
            assert em.find(Agent.class, agent0.getId()) != null;
            reports = q.getResultList();
            assert reports.size() == 2 + baselineSize : reports;

            i = reports.indexOf(new AgentLastAvailabilityReportComposite(agent3.getId(), null, null, null));
            assert reports.get(i).getAgentId() == agent3.getId();
            assert reports.get(i).getAgentName().equals(agent3.getName());
            assert reports.get(i).getRemoteEndpoint().equals(agent3.getRemoteEndpoint());
            assert reports.get(i).getLastAvailabilityReport().getTime() == agent3.getLastAvailabilityReport().getTime();

            i = reports.indexOf(new AgentLastAvailabilityReportComposite(agent4.getId(), null, null, null));
            assert reports.get(i).getAgentId() == agent4.getId();
            assert reports.get(i).getAgentName().equals(agent4.getName());
            assert reports.get(i).getRemoteEndpoint().equals(agent4.getRemoteEndpoint());
            assert reports.get(i).getLastAvailabilityReport().getTime() == agent4.getLastAvailabilityReport().getTime();

            assert -1 == reports.indexOf(new AgentLastAvailabilityReportComposite(agent2.getId(), null, null, null));
            assert -1 == reports.indexOf(new AgentLastAvailabilityReportComposite(agent1.getId(), null, null, null));

            // get all of them, except the one with the null
            q.setParameter("dateThreshold", new Date(now - 1), TemporalType.TIMESTAMP);
            reports = q.getResultList();
            assert reports.size() == 4 + baselineSize : reports;

            // get none of them
            q.setParameter("dateThreshold", new Date(now - 10000), TemporalType.TIMESTAMP);
            reports = q.getResultList();
            assert reports.size() == 0 + baselineSize : reports;
        } finally {
            getTransactionManager().rollback();
        }
    }

    private Agent createAgent(EntityManager em, int num, long availDateOffsetFromNow) {
        Agent agent = new Agent("agent" + num, "address" + num, num, "remoteaddr" + num, "token" + num);
        if (availDateOffsetFromNow > 0) {
            agent.setLastAvailabilityReport(new Date(now - availDateOffsetFromNow));
        } else {
            agent.setLastAvailabilityReport(null);
        }

        em.persist(agent);
        return agent;
    }
}