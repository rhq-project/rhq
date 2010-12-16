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
package org.rhq.enterprise.server.measurement.test;

import java.util.Date;
import java.util.Random;

import javax.persistence.EntityManager;
import javax.transaction.Status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementCategory;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.calltime.CallTimeData;
import org.rhq.core.domain.measurement.calltime.CallTimeDataComposite;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.measurement.CallTimeDataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Test some measurement subsystem functionality
 * @author Heiko W. Rupp
 */

public class MeasurementDataManagerTest extends AbstractEJB3Test {

    private final Log log = LogFactory.getLog(MeasurementDataManagerTest.class);

    private ResourceManagerLocal resourceManager;
    private MeasurementDataManagerLocal measurementDataManager;
    private CallTimeDataManagerLocal callTimeDataManager;

    private Subject overlord;

    private Resource resource1,resource2;
    private ResourceType theResourceType;
    private Agent theAgent;
    private MeasurementDefinition theDefinition;

    @BeforeMethod
    public void beforeMethod() {
        try {
            this.resourceManager = LookupUtil.getResourceManager();
            this.measurementDataManager = LookupUtil.getMeasurementDataManager();
            this.callTimeDataManager = LookupUtil.getCallTimeDataManager();
            this.overlord = LookupUtil.getSubjectManager().getOverlord();
        } catch (Throwable t) {
            // Catch RuntimeExceptions and Errors and dump their stack trace, because Surefire will completely swallow them
            // and throw a cryptic NPE (see http://jira.codehaus.org/browse/SUREFIRE-157)!
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }


    @Test
    public void bz658491() throws Exception {
        EntityManager em = beginTx();

        try {
            setupResources(em);

            MeasurementSchedule schedule1 = resource1.getSchedules().iterator().next();
            MeasurementSchedule schedule2 = resource2.getSchedules().iterator().next();
            MeasurementScheduleRequest request1 = new MeasurementScheduleRequest(schedule1);
            MeasurementScheduleRequest request2 = new MeasurementScheduleRequest(schedule2);

            CallTimeData data1 = new CallTimeData(request1);
            CallTimeData data2 = new CallTimeData(request2);

            data1.addCallData("/foo", new Date(),100);
            data2.addCallData("/bar", new Date(),200);

            MeasurementReport report = new MeasurementReport();
            report.addData(data1);
            report.addData(data2);

            measurementDataManager.mergeMeasurementReport(report);


            PageList<CallTimeDataComposite> list1 = callTimeDataManager.findCallTimeDataForResource(overlord,schedule1.getId(),
                0,System.currentTimeMillis(),new PageControl());
            PageList<CallTimeDataComposite> list2 = callTimeDataManager.findCallTimeDataForResource(overlord,schedule2.getId(),
                0,System.currentTimeMillis(),new PageControl());

            assert list1 != null;
            assert list2 != null;

            assert list1.size() == 1 : "List 1 returned " + list1.size() + " entries, expected was 1";
            assert list2.size() == 1 : "List 2 returned " + list2.size() + " entries, expected was 1";


        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (em != null) {
                getTransactionManager().rollback();
                em.close();
            }
        }
    }


    /**
     * Just set up a resource where we can attach the availabilities to
     *
     * @param  em The EntityManager to use
     *
     * @return A Resource ready to use
     */
    private void setupResources(EntityManager em) {
        theAgent = new Agent("testagent", "localhost", 1234, "", "randomToken");
        em.persist(theAgent);

        theResourceType = new ResourceType("test-plat", "test-plugin", ResourceCategory.PLATFORM, null);
        em.persist(theResourceType);

        theDefinition = new MeasurementDefinition("CT-Def", MeasurementCategory.PERFORMANCE,
            MeasurementUnits.MILLISECONDS, DataType.CALLTIME,true,60000, DisplayType.SUMMARY);
        theDefinition.setResourceType(theResourceType);
        em.persist(theDefinition);


        resource1 = new Resource("test-platform-key1", "test-platform-name", theResourceType);
        resource1.setUuid("" + new Random().nextInt());
        resource1.setAgent(theAgent);
        em.persist(resource1);
        resource2 = new Resource("test-platform-key2", "test-platform-name", theResourceType);
        resource2.setUuid("" + new Random().nextInt());
        resource2.setAgent(theAgent);
        em.persist(resource2);

        MeasurementSchedule schedule1 = new MeasurementSchedule(theDefinition,resource1);
        em.persist(schedule1);
        theDefinition.addSchedule(schedule1);
        resource1.addSchedule(schedule1);

        MeasurementSchedule schedule2 = new MeasurementSchedule(theDefinition,resource2);
        em.persist(schedule2);
        theDefinition.addSchedule(schedule2);
        resource2.addSchedule(schedule2);

        em.flush();
    }

    private EntityManager beginTx() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        return em;
    }

}
