/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.enterprise.server.performance.test;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Query;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventDefinition;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.event.EventSource;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.event.EventManagerLocal;
import org.rhq.enterprise.server.measurement.AvailabilityManagerLocal;
import org.rhq.enterprise.server.purge.PurgeManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3PerformanceTest;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.helpers.perftest.support.reporting.ExcelExporter;
import org.rhq.helpers.perftest.support.testng.DatabaseSetupInterceptor;
import org.rhq.helpers.perftest.support.testng.DatabaseState;
import org.rhq.helpers.perftest.support.testng.PerformanceReporting;

/**
 * Performance test the events subsystem
 *
 * @author Heiko W. Rupp
 */
@Test(groups = "PERF")
@Listeners({ DatabaseSetupInterceptor.class })
@PerformanceReporting(exporter = ExcelExporter.class)
@DatabaseState(url = "perftest/AvailabilityInsertPurgeTest-testOne-data.xml.zip", dbVersion = "2.125")
public class EventsInsertPurgeTest extends AbstractEJB3PerformanceTest {

    private static final int ROUNDS = 20000;
    private static final int NUM_SOURCES = 10;
    private static final int LINES_PER_REPORT = 50;

    ResourceManagerLocal resourceManager;
    AvailabilityManagerLocal availabilityManager;
    AgentManagerLocal agentManager;
    SystemManagerLocal systemManager;
    AlertDefinitionManagerLocal alertDefinitionManager;
    EventManagerLocal eventManager;
    PurgeManagerLocal purgeManager;

    @Override
    protected void beforeMethod(Method method) {
        super.setupTimings(method);
        try {
            this.availabilityManager = LookupUtil.getAvailabilityManager();
            this.resourceManager = LookupUtil.getResourceManager();
            this.agentManager = LookupUtil.getAgentManager();
            this.systemManager = LookupUtil.getSystemManager();
            this.alertDefinitionManager = LookupUtil.getAlertDefinitionManager();
            this.eventManager = LookupUtil.getEventManager();
            this.purgeManager = LookupUtil.getPurgeManager();
            /*
             * NOTE: do not try to get Subjects or other DB stuff in here, as they will only
             * be available after this method has finished and the DatabaseSetupInterceptor
             * has initialized the database.
             */
        } catch (Throwable t) {
            // Catch RuntimeExceptions and Errors and dump their stack trace, because Surefire will completely swallow them
            // and throw a cryptic NPE (see http://jira.codehaus.org/browse/SUREFIRE-157)!
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    /**
     * This test insertsevents from NUM_SOURCES sources into the
     * event subsystem, where each set of events is LINES_PER_REPORT long.
     * In total ROUNDS are inserted.
     * After this, events are purged in two steps: first the first half of them
     * and then the remaining ones.
     * @throws Exception
     */
    public void testSimpleInserts() throws Exception {
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();

        Query q = em.createQuery("SELECT r FROM Resource r");
        List<Resource> resources = q.getResultList();
        Resource res = resources.get(0);
        if (!(res.getResourceType().getCategory() == ResourceCategory.PLATFORM))
            res = resourceManager.getPlaformOfResource(overlord, res.getId());
        ResourceType type = res.getResourceType();

        EventDefinition def;
        Set<EventDefinition> eventDefs = type.getEventDefinitions();
        if (eventDefs != null && !eventDefs.isEmpty()) {
            def = eventDefs.iterator().next();
        } else {
            throw new RuntimeException("No event definition found, should not happen");
        }

        EventSource[] evSrc = new EventSource[NUM_SOURCES];
        for (int i = 0; i < NUM_SOURCES; i++) {
            evSrc[i] = new EventSource("ESource" + 1, def, res);
        }
        long now = new Date().getTime() - ROUNDS * LINES_PER_REPORT;

        for (int round = 1; round < ROUNDS; round++) {

            Map<EventSource, Set<Event>> events = new HashMap<EventSource, Set<Event>>();
            for (int sourceNum = 0; sourceNum < NUM_SOURCES; sourceNum++) {
                Set<Event> eventSet = new HashSet<Event>();
                for (int i = 0; i < LINES_PER_REPORT; i++) {
                    Event ev = new Event("EType", "ESource" + sourceNum, now + round * i, EventSeverity.INFO,
                        "This is a 2nd test", evSrc[sourceNum]);
                    eventSet.add(ev);
                }
                events.put(evSrc[sourceNum], eventSet);
            }
            startTiming("add");
            eventManager.addEventData(events);
            endTiming("add");
        }

        startTiming("purge_half");
        purgeManager.purgeEventData(now + (ROUNDS / 2) * LINES_PER_REPORT);
        endTiming("purge_half");
        startTiming("purge_final");
        purgeManager.purgeEventData(System.currentTimeMillis());
        endTiming("purge_final");
    }

}
