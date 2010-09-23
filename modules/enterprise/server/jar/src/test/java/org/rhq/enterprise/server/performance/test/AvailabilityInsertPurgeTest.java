/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.server.performance.test;

import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.measurement.AvailabilityManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3PerformanceTest;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.helpers.perftest.support.testng.DatabaseSetupInterceptor;
import org.rhq.helpers.perftest.support.testng.DatabaseState;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/**
 * Performance test the availabilities subsystem
 *
 * @author Heiko W. Rupp
 * @author Lukas Krejci
 */
@Test(groups = "PERF")
@Listeners({ DatabaseSetupInterceptor.class })
public class AvailabilityInsertPurgeTest extends AbstractEJB3PerformanceTest {

    ResourceManagerLocal resourceManager;
    AvailabilityManagerLocal availabilityManager;
    AgentManagerLocal agentManager;
    private static final int MILLIS_APART = 2000;
    private static final String ROUND__FORMAT = "Round %6d";

    @BeforeMethod
    public void beforeMethod() {
        try {
            this.availabilityManager = LookupUtil.getAvailabilityManager();
            this.resourceManager = LookupUtil.getResourceManager();
            this.agentManager = LookupUtil.getAgentManager();
        } catch (Throwable t) {
            // Catch RuntimeExceptions and Errors and dump their stack trace, because Surefire will completely swallow them
            // and throw a cryptic NPE (see http://jira.codehaus.org/browse/SUREFIRE-157)!
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    @DatabaseState(url = "perftest/AvailabilityInsertPurgeTest-testOne-data.xml.zip", dbVersion="2.94")
    public void testOne() throws Exception {

        final int[] ROUNDS = {500,1000,1500,2000,2500,3000};

        EntityManager em = getEntityManager();
        Query q = em.createQuery("SELECT r FROM Resource r");
        List<Resource> resources = q.getResultList();
        Resource res = resources.get(0);
        Agent agent = agentManager.getAgentByResourceId(res.getId());

        q = em.createQuery("SELECT COUNT(a) FROM Availability a ");
        Object o = q.getSingleResult();
        Long l = (Long)o;
        if (l!=0) {
            throw new IllegalStateException("Availabilities table is not empty");
        }

        for ( int j = 0; j < ROUNDS.length; j++) {
            int MULTI = ROUNDS[j];
            String round = String.format(ROUND__FORMAT,MULTI);

            long t1 = System.currentTimeMillis() - (MULTI * MILLIS_APART);
            for ( int i = 0 ; i < MULTI ; i++ ) {

                AvailabilityReport report = new AvailabilityReport(agent.getName());
                for (Resource r : resources) {
                    AvailabilityType at = (i%2==0) ? AvailabilityType.UP : AvailabilityType.DOWN;
                    Availability a = new Availability(r,new Date(t1 + i * MILLIS_APART), at);
                    report.addAvailability(a);
                }
                startTiming(round);
                availabilityManager.mergeAvailabilityReport(report);
                endTiming(round);
            }




        }

        long timing1000 = getTiming(String.format(ROUND__FORMAT,1000));
        long timing2000 = getTiming(String.format(ROUND__FORMAT,2000));
        long timing3000 = getTiming(String.format(ROUND__FORMAT,3000));

        assertCirca(timing1000,timing2000,2);
        assertCirca(timing1000,timing3000,3);


        commitTimings();

    }
}
