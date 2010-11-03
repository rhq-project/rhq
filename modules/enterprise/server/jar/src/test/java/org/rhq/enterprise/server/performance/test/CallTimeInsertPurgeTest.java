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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.calltime.CallTimeData;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.event.EventManagerLocal;
import org.rhq.enterprise.server.measurement.AvailabilityManagerLocal;
import org.rhq.enterprise.server.measurement.CallTimeDataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerBean;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3PerformanceTest;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.helpers.perftest.support.reporting.ExcelExporter;
import org.rhq.helpers.perftest.support.testng.DatabaseSetupInterceptor;
import org.rhq.helpers.perftest.support.testng.DatabaseState;
import org.rhq.helpers.perftest.support.testng.PerformanceReporting;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Test insertion of call time data and purging it
 *
 * @author Heiko W. Rupp
 */
@Test(groups = "PERF")
@Listeners({ DatabaseSetupInterceptor.class })
@PerformanceReporting(exporter=ExcelExporter.class)
@DatabaseState(url = "perftest/AvailabilityInsertPurgeTest-testOne-data.xml.zip", dbVersion="2.101")
public class CallTimeInsertPurgeTest extends AbstractEJB3PerformanceTest {

    private final Log log = LogFactory.getLog(CallTimeInsertPurgeTest.class);


    private static final int ROUNDS = 200;
    private static final int NUM_SOURCES = 10;
    private static final int LINES_PER_REPORT = 50;
    ResourceManagerLocal resourceManager;
    AvailabilityManagerLocal availabilityManager;
    AgentManagerLocal agentManager;
    SystemManagerLocal systemManager;
    AlertDefinitionManagerLocal alertDefinitionManager;
    MeasurementDataManagerLocal measurementDataManager;
    private static final int MILLIS_APART = 2000;
    private static final String ROUND__FORMAT = "Round %6d";
    private static final String PURGE__FORMAT = "Purge %6d";


    @BeforeMethod
    public void beforeMethod() {
        Date now = new Date();
        try {
            this.availabilityManager = LookupUtil.getAvailabilityManager();
            this.resourceManager = LookupUtil.getResourceManager();
            this.agentManager = LookupUtil.getAgentManager();
            this.systemManager = LookupUtil.getSystemManager();
            this.alertDefinitionManager = LookupUtil.getAlertDefinitionManager();
            this.measurementDataManager = LookupUtil.getMeasurementDataManager();
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


    public void testSimple() throws Exception {

        Subject overlord = LookupUtil.getSubjectManager().getOverlord();


        EntityManager em = getEntityManager();
        Query q = em.createQuery("SELECT r FROM Resource r");
        List<Resource> resources = q.getResultList();
        Resource res = resources.get(0);
        if (!(res.getResourceType().getCategory()== ResourceCategory.PLATFORM))
            res = resourceManager.getPlaformOfResource(overlord,res.getId());
        ResourceType type = res.getResourceType();
        MeasurementDefinition def = null;
        Set<MeasurementDefinition> definitions = type.getMetricDefinitions();
        for (MeasurementDefinition d : definitions) {
            if (d.getName().equals("myCallTime"))
                def = d;
        }



        MeasurementSchedule schedule = new MeasurementSchedule(def,res);
        MeasurementScheduleRequest request = new MeasurementScheduleRequest(schedule);


        for (int i = 0 ; i < ROUNDS ; i++) {
            CallTimeData data = new CallTimeData(request);

            MeasurementReport report = new MeasurementReport();


            for (int j = 0 ; j < NUM_SOURCES ; j++) {
                data.addCallData("/foo/" + j,new Date(),NUM_SOURCES-j);
            }
            report.addData(data);

            startTiming();
            measurementDataManager.mergeMeasurementReport(report);
            endTiming();
        }
    }

}
