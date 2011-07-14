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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.persistence.EntityManager;
import javax.persistence.Query;

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
import org.rhq.core.domain.measurement.calltime.CallTimeDataKey;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.measurement.CallTimeDataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Test some measurement subsystem functionality
 * @author Heiko W. Rupp
 */

public class MeasurementDataManagerTest extends AbstractEJB3Test {

    private static final int DELTA = 20;
    private MeasurementDataManagerLocal measurementDataManager;
    private CallTimeDataManagerLocal callTimeDataManager;

    private Subject overlord;

    private Resource resource1,resource2;
    private MeasurementDefinition definitionCt1;
    private MeasurementDefinition definitionCt2;
    private ResourceType theResourceType;
    private Agent theAgent;

    @BeforeMethod
    public void beforeMethod() {
        try {
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


    @AfterMethod
    public void afterMethod() {

        try {
            // delete values
            callTimeDataManager.purgeCallTimeData(new Date());

            EntityManager em = beginTx();
            // delete keys
            List<Integer> resourceIds = new ArrayList<Integer>();
            resourceIds.add(resource1.getId());
            resourceIds.add(resource2.getId());
            Query q = em.createNamedQuery(CallTimeDataKey.QUERY_DELETE_BY_RESOURCES);
            q.setParameter("resourceIds",resourceIds);
            q.executeUpdate();

            resource1 = em.merge(resource1);
            for (MeasurementSchedule sched : resource1.getSchedules()) {
                em.remove(sched);
            }
            em.remove(resource1);

            resource2 = em.merge(resource2);
            for (MeasurementSchedule sched : resource2.getSchedules()) {
                em.remove(sched);
            }
            em.remove(resource2);

            definitionCt1 = em.merge(definitionCt1);
            em.remove(definitionCt1);
            definitionCt2 = em.merge(definitionCt2);
            em.remove(definitionCt2);

            theResourceType = em.merge(theResourceType);
            em.remove(theResourceType);

            theAgent = em.merge(theAgent);
            em.remove(theAgent);

            commitAndClose(em);
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


    @Test
    public void bz658491() throws Exception {

        try {
            EntityManager em = beginTx();

            setupResources(em);

            MeasurementSchedule schedule1 = new MeasurementSchedule(definitionCt1,resource1);
            em.persist(schedule1);
            definitionCt1.addSchedule(schedule1);
            resource1.addSchedule(schedule1);

            MeasurementSchedule schedule2 = new MeasurementSchedule(definitionCt1,resource2);
            em.persist(schedule2);
            definitionCt1.addSchedule(schedule2);
            resource2.addSchedule(schedule2);

            em.flush();
            long now = System.currentTimeMillis();

            MeasurementScheduleRequest request1 = new MeasurementScheduleRequest(schedule1);
            MeasurementScheduleRequest request2 = new MeasurementScheduleRequest(schedule2);

            CallTimeData data1 = new CallTimeData(request1);
            CallTimeData data2 = new CallTimeData(request2);

            data1.addCallData("/foo", new Date(),100);
            data2.addCallData("/bar", new Date(),200);

            MeasurementReport report = new MeasurementReport();
            report.addData(data1);
            report.addData(data2);

            commitAndClose(em);

            measurementDataManager.mergeMeasurementReport(report);

            // Do not remove this sleep -- the previous is is asynchronous
            // and the sleep "guarantees" that data is actually hitting the db
            Thread.sleep(1000);

            PageList<CallTimeDataComposite> list1 = callTimeDataManager.findCallTimeDataForResource(overlord,schedule1.getId(),
                now-DELTA,System.currentTimeMillis()+DELTA,new PageControl());
            PageList<CallTimeDataComposite> list2 = callTimeDataManager.findCallTimeDataForResource(overlord,schedule2.getId(),
                now-DELTA,System.currentTimeMillis()+DELTA,new PageControl());

            assert list1 != null;
            assert list2 != null;

            assert list1.size() == 1 : "List 1 returned " + list1.size() + " entries, expected was 1";
            assert list2.size() == 1 : "List 2 returned " + list2.size() + " entries, expected was 1";


        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void bz658491OneResource() throws Exception {

        try {
            EntityManager em = beginTx();

            setupResources(em);

            MeasurementSchedule schedule1 = new MeasurementSchedule(definitionCt1,resource1);
            em.persist(schedule1);
            definitionCt1.addSchedule(schedule1);
            resource1.addSchedule(schedule1);

            MeasurementSchedule schedule2 = new MeasurementSchedule(definitionCt2,resource1);
            em.persist(schedule2);
            definitionCt1.addSchedule(schedule2);
            resource2.addSchedule(schedule2);

            em.flush();
            long now = System.currentTimeMillis();

            MeasurementScheduleRequest request1 = new MeasurementScheduleRequest(schedule1);
            MeasurementScheduleRequest request2 = new MeasurementScheduleRequest(schedule2);

            CallTimeData data1 = new CallTimeData(request1);
            CallTimeData data2 = new CallTimeData(request2);

            data1.addCallData("/foo", new Date(),100);
            data2.addCallData("/bar", new Date(),200);

            MeasurementReport report = new MeasurementReport();
            report.addData(data1);
            report.addData(data2);

            commitAndClose(em);

            measurementDataManager.mergeMeasurementReport(report);

            // Do not remove this sleep -- the previous is is asynchronous
            // and the sleep "guarantees" that data is actually hitting the db
            Thread.sleep(1000);

            PageList<CallTimeDataComposite> list1 = callTimeDataManager.findCallTimeDataForResource(overlord,schedule1.getId(),
                now- DELTA,System.currentTimeMillis()+ DELTA,new PageControl());
            PageList<CallTimeDataComposite> list2 = callTimeDataManager.findCallTimeDataForResource(overlord,schedule2.getId(),
                now- DELTA,System.currentTimeMillis()+ DELTA,new PageControl());

            assert list1 != null;
            assert list2 != null;

            assert list1.size() == 1 : "List 1 returned " + list1.size() + " entries, expected was 1";
            assert list2.size() == 1 : "List 2 returned " + list2.size() + " entries, expected was 1";


        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }


    /**
     * Just set up two resources plus measurement definitions
     *
     * @param  em The EntityManager to use
     *
     */
    private void setupResources(EntityManager em) {
        theAgent = new Agent("testagent", "localhost", 1234, "", "randomToken");
        em.persist(theAgent);

        theResourceType = new ResourceType("test-plat", "test-plugin", ResourceCategory.PLATFORM, null);
        em.persist(theResourceType);

        definitionCt1 = new MeasurementDefinition("CT-Def1", MeasurementCategory.PERFORMANCE,
            MeasurementUnits.MILLISECONDS, DataType.CALLTIME,true,60000, DisplayType.SUMMARY);
        definitionCt1.setResourceType(theResourceType);
        em.persist(definitionCt1);

        definitionCt2 = new MeasurementDefinition("CT-Def2", MeasurementCategory.PERFORMANCE,
            MeasurementUnits.MILLISECONDS, DataType.CALLTIME,true,60000, DisplayType.SUMMARY);
        definitionCt2.setResourceType(theResourceType);
        em.persist(definitionCt2);


        resource1 = new Resource("test-platform-key1", "test-platform-name", theResourceType);
        resource1.setUuid("" + new Random().nextInt());
        resource1.setAgent(theAgent);
        em.persist(resource1);
        resource2 = new Resource("test-platform-key2", "test-platform-name", theResourceType);
        resource2.setUuid("" + new Random().nextInt());
        resource2.setAgent(theAgent);
        em.persist(resource2);
    }

    private EntityManager beginTx() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        return em;
    }

    private void commitAndClose(EntityManager em) throws Exception {
        em.flush();
        getTransactionManager().commit();
        em.close();
    }
}
