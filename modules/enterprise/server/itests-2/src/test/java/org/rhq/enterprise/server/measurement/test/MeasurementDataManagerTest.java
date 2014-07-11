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

package org.rhq.enterprise.server.measurement.test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.measurement.MeasurementAgentService;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.MeasurementDataTraitCriteria;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementCategory;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataPK;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
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
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.measurement.CallTimeDataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.purge.PurgeManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TestServerCommunicationsService;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.ResourceTreeHelper;

/**
 * Test some measurement subsystem functionality
 * @author Heiko W. Rupp
 */

public class MeasurementDataManagerTest extends AbstractEJB3Test {

    private static final int DELTA = 20;

    private MeasurementDataManagerLocal measurementDataManager;
    private CallTimeDataManagerLocal callTimeDataManager;
    private PurgeManagerLocal purgeManager;

    private Subject overlord;

    private Resource resource1, resource2;
    private ResourceGroup group;
    private MeasurementDefinition definitionCt1, definitionCt2, definitionCt3;
    private MeasurementSchedule schedule1, schedule2, schedule3;
    private ResourceType theResourceType;
    private Agent theAgent;
    private Set<MeasurementData> expectedResult1, expectedResult2, expectedResult3, expectedResult4;

    @Override
    protected void beforeMethod() {
        try {
            this.measurementDataManager = LookupUtil.getMeasurementDataManager();
            this.callTimeDataManager = LookupUtil.getCallTimeDataManager();
            this.purgeManager = LookupUtil.getPurgeManager();
            this.overlord = LookupUtil.getSubjectManager().getOverlord();

        } catch (Throwable t) {
            // Catch RuntimeExceptions and Errors and dump their stack trace, because Surefire will completely swallow them
            // and throw a cryptic NPE (see http://jira.codehaus.org/browse/SUREFIRE-157)!
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    @Override
    protected void afterMethod() {

        try {
            // delete values
            purgeManager.purgeCallTimeData(System.currentTimeMillis());

            beginTx();

            // delete keys
            List<Integer> resourceIds = new ArrayList<Integer>();
            resourceIds.add(resource1.getId());
            resourceIds.add(resource2.getId());
            Query q = em.createNamedQuery(CallTimeDataKey.QUERY_DELETE_BY_RESOURCES);
            q.setParameter("resourceIds", resourceIds);
            q.executeUpdate();

            resource1 = em.merge(resource1);
            for (MeasurementSchedule sched : resource1.getSchedules()) {
                em.remove(sched);
            }
            ResourceTreeHelper.deleteResource(em, resource1);

            resource2 = em.merge(resource2);
            for (MeasurementSchedule sched : resource2.getSchedules()) {
                em.remove(sched);
            }
            ResourceTreeHelper.deleteResource(em, resource2);

            definitionCt1 = em.merge(definitionCt1);
            em.remove(definitionCt1);
            definitionCt2 = em.merge(definitionCt2);
            em.remove(definitionCt2);
            if (definitionCt3 != null) {
                definitionCt3 = em.merge(definitionCt3);
                em.remove(definitionCt3);
            }

            theResourceType = em.merge(theResourceType);
            em.remove(theResourceType);

            // delete group (if exist)
            if (group != null) {
                group = em.merge(group);
                em.remove(group);
            }

            theAgent = em.merge(theAgent);
            em.remove(theAgent);

            commit();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Test
    public void bz658491() throws Exception {

        try {
            beginTx();

            setupResources(em);

            em.flush();
            long now = System.currentTimeMillis();

            MeasurementScheduleRequest request1 = new MeasurementScheduleRequest(schedule1);
            MeasurementScheduleRequest request2 = new MeasurementScheduleRequest(schedule2);

            CallTimeData data1 = new CallTimeData(request1);
            CallTimeData data2 = new CallTimeData(request2);

            data1.addCallData("/foo", new Date(), 100);
            data2.addCallData("/bar", new Date(), 200);

            MeasurementReport report = new MeasurementReport();
            report.addData(data1);
            report.addData(data2);

            commit();

            measurementDataManager.mergeMeasurementReport(report);

            // Do not remove this sleep -- the previous is is asynchronous
            // and the sleep "guarantees" that data is actually hitting the db
            Thread.sleep(10000);

            PageList<CallTimeDataComposite> list1 = callTimeDataManager.findCallTimeDataForResource(overlord,
                schedule1.getId(), now - DELTA, System.currentTimeMillis() + DELTA, new PageControl());
            PageList<CallTimeDataComposite> list2 = callTimeDataManager.findCallTimeDataForResource(overlord,
                schedule2.getId(), now - DELTA, System.currentTimeMillis() + DELTA, new PageControl());

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
            beginTx();

            setupResources(em);

            em.flush();
            long now = System.currentTimeMillis();

            MeasurementScheduleRequest request1 = new MeasurementScheduleRequest(schedule1);
            MeasurementScheduleRequest request2 = new MeasurementScheduleRequest(schedule2);

            CallTimeData data1 = new CallTimeData(request1);
            CallTimeData data2 = new CallTimeData(request2);

            data1.addCallData("/foo", new Date(), 100);
            data2.addCallData("/bar", new Date(), 200);

            MeasurementReport report = new MeasurementReport();
            report.addData(data1);
            report.addData(data2);

            commit();

            measurementDataManager.mergeMeasurementReport(report);

            // Do not remove this sleep -- the previous is is asynchronous
            // and the sleep "guarantees" that data is actually hitting the db
            Thread.sleep(10000);

            PageList<CallTimeDataComposite> list1 = callTimeDataManager.findCallTimeDataForResource(overlord,
                schedule1.getId(), now - DELTA, System.currentTimeMillis() + DELTA, new PageControl());
            PageList<CallTimeDataComposite> list2 = callTimeDataManager.findCallTimeDataForResource(overlord,
                schedule2.getId(), now - DELTA, System.currentTimeMillis() + DELTA, new PageControl());

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
    public void testFindCallTimeDataRaw() throws Exception {
        try {
            beginTx();

            setupResources(em);

            em.flush();
            long now = System.currentTimeMillis();

            MeasurementScheduleRequest request1 = new MeasurementScheduleRequest(schedule1);

            CallTimeData data1 = new CallTimeData(request1);
            CallTimeData data2 = new CallTimeData(request1);
            Date dNow = new Date();
            dNow.setTime(now);

            data1.addCallData("/1", dNow, 1);
            data1.addCallData("/1", dNow, 1);

            dNow.setTime(now + 1);
            data2.addCallData("/1", dNow, 3);

            dNow.setTime(now + 2);
            data2.addCallData("/2a", dNow, 4);

            dNow.setTime(now + 3);
            data2.addCallData("/2b", dNow, 5);

            commit();

            MeasurementReport report = new MeasurementReport();
            report.addData(data1);
            measurementDataManager.mergeMeasurementReport(report);

            report = new MeasurementReport();
            report.addData(data2);
            measurementDataManager.mergeMeasurementReport(report);

            // Do not remove this sleep -- the previous is is asynchronous
            // and the sleep "guarantees" that data is actually hitting the db
            Thread.sleep(10000);

            PageList<CallTimeDataComposite> list = callTimeDataManager.findCallTimeDataRawForResource(overlord,
                schedule1.getId(), now - DELTA, System.currentTimeMillis() + DELTA, new PageControl());

            assert list != null;

            assert list.size() == 4 : "List 1 returned " + list.size() + " entries, expected was 4";
            assert list.get(0).getTotal() == 2 : "First value must be total = 2, but was "+list.get(0).getTotal();
            assert list.get(1).getTotal() == 3 : "Second value must be total = 3, but was "+list.get(1).getTotal();
            assert list.get(2).getTotal() == 4 : "Third value must be total = 4, but was "+list.get(2).getTotal();
            assert list.get(3).getTotal() == 5 : "Fourth value must be total = 5, but was "+list.get(3).getTotal();

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
            MeasurementUnits.MILLISECONDS, DataType.CALLTIME, true, 60000, DisplayType.SUMMARY);
        definitionCt1.setResourceType(theResourceType);
        em.persist(definitionCt1);

        definitionCt2 = new MeasurementDefinition("CT-Def2", MeasurementCategory.PERFORMANCE,
            MeasurementUnits.MILLISECONDS, DataType.CALLTIME, true, 60000, DisplayType.SUMMARY);
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

        schedule1 = new MeasurementSchedule(definitionCt1, resource1);
        em.persist(schedule1);
        definitionCt1.addSchedule(schedule1);
        resource1.addSchedule(schedule1);

        schedule2 = new MeasurementSchedule(definitionCt2, resource1);
        em.persist(schedule2);
        definitionCt1.addSchedule(schedule2);
        resource2.addSchedule(schedule2);
    }

    /**
     * Just set up the group of two resources plus measurement definitions
     *
     * @param  em The EntityManager to use
     *
     */
    @SuppressWarnings("unchecked")
    private void setupGroupOfResources(EntityManager em) {
        theAgent = new Agent("testagent", "localhost", 1234, "", "randomToken");
        em.persist(theAgent);

        theResourceType = new ResourceType("test-plat", "test-plugin", ResourceCategory.PLATFORM, null);
        em.persist(theResourceType);

        definitionCt1 = new MeasurementDefinition("CT-Def1", MeasurementCategory.PERFORMANCE,
            MeasurementUnits.MILLISECONDS, DataType.CALLTIME, true, 60000, DisplayType.SUMMARY);
        definitionCt1.setResourceType(theResourceType);
        em.persist(definitionCt1);

        definitionCt2 = new MeasurementDefinition("CT-Def2", MeasurementCategory.PERFORMANCE,
            MeasurementUnits.MILLISECONDS, DataType.CALLTIME, true, 60000, DisplayType.SUMMARY);
        definitionCt2.setResourceType(theResourceType);
        em.persist(definitionCt2);

        definitionCt3 = new MeasurementDefinition("CT-Def3", MeasurementCategory.PERFORMANCE,
            MeasurementUnits.MILLISECONDS, DataType.CALLTIME, true, 60000, DisplayType.SUMMARY);
        definitionCt3.setResourceType(theResourceType);
        em.persist(definitionCt3);

        resource1 = new Resource("test-platform-key1", "test-platform-name", theResourceType);
        resource1.setUuid("" + new Random().nextInt());
        resource1.setAgent(theAgent);
        em.persist(resource1);

        resource2 = new Resource("test-platform-key2", "test-platform-name", theResourceType);
        resource2.setUuid("" + new Random().nextInt());
        resource2.setAgent(theAgent);
        em.persist(resource2);

        schedule1 = new MeasurementSchedule(definitionCt1, resource1);
        em.persist(schedule1);
        definitionCt1.addSchedule(schedule1);
        resource1.addSchedule(schedule1);

        schedule2 = new MeasurementSchedule(definitionCt2, resource2);
        em.persist(schedule2);
        definitionCt2.addSchedule(schedule2);
        resource2.addSchedule(schedule2);

        schedule3 = new MeasurementSchedule(definitionCt3, resource2);
        em.persist(schedule3);
        definitionCt3.addSchedule(schedule3);
        resource2.addSchedule(schedule3);

        group = new ResourceGroup("test-group", theResourceType);
        em.persist(group);

        // prepare return values and expected values
        long time1 = System.currentTimeMillis();
        long time2 = time1 + 1;
        long time3 = time2 + 1;
        long time4 = time3 + 1;
        String name1 = "a";
        String name2 = "b";
        String name3 = "c";
        String name4 = "d";
        String value1 = "test-value1";
        String value2 = "test-value2";
        String value3 = "test-value3";
        String value4 = "test-value4";

        // method findLiveDataForGroup adds prefix with resource id which is part of equals
        MeasurementData expectedData1 = makeMeasurement(time1, schedule1.getId(), value1, name1);
        expectedData1.setName(resource1.getId() + ":" + name1);
        MeasurementData expectedData2 = makeMeasurement(time2, schedule2.getId(), value2, name2);
        expectedData2.setName(resource2.getId() + ":" + name2);
        MeasurementData expectedData3 = makeMeasurement(time3, schedule3.getId(), value3, name3);
        expectedData3.setName(resource2.getId() + ":" + name3);
        MeasurementData expectedData4 = makeMeasurement(time4, schedule2.getId(), value4, name4);
        expectedData4.setName(resource2.getId() + ":" + name4);

        expectedResult1 = new HashSet<MeasurementData>(1);
        expectedResult1.add(expectedData1);
        expectedResult2 = new HashSet<MeasurementData>(2);
        expectedResult2.add(expectedData2);
        expectedResult2.add(expectedData3);
        expectedResult3 = new HashSet<MeasurementData>(3);
        expectedResult3.addAll(expectedResult1);
        expectedResult3.addAll(expectedResult2);
        expectedResult4 = new HashSet<MeasurementData>(2);
        expectedResult4.add(expectedData2);
        expectedResult4.add(expectedData4);

        // mock the MeasurementAgentService
        MeasurementAgentService mockedMeasurementService = mock(MeasurementAgentService.class);
        when(mockedMeasurementService.getRealTimeMeasurementValue(eq(resource1.getId()), any(Set.class))).thenReturn(
            new HashSet<MeasurementData>(Arrays.asList(makeMeasurement(time1, schedule1.getId(), value1, name1))));
        when(mockedMeasurementService.getRealTimeMeasurementValue(eq(resource2.getId()), any(Set.class))).thenReturn(
            new HashSet<MeasurementData>(Arrays.asList(makeMeasurement(time2, schedule2.getId(), value2, name2),
                makeMeasurement(time3, schedule3.getId(), value3, name3))));
        TestServerCommunicationsService agentServiceContainer = prepareForTestAgents();
        agentServiceContainer.measurementService = mockedMeasurementService;
    }

    @Test
    public void testFindLiveDataForGroup1() throws Exception {
        // prepare DB
        beginTx();
        setupGroupOfResources(em);
        commit();
        try {
            Set<MeasurementData> actualResult = measurementDataManager.findLiveDataForGroup(overlord, group.getId(),
                new int[] { resource1.getId() }, new int[] { definitionCt1.getId() });

            Set<MeasurementData> actualResultWithNewHashCodes = new HashSet<MeasurementData>(actualResult);
            assertEquals(expectedResult1, actualResultWithNewHashCodes);

            actualResult = measurementDataManager.findLiveDataForGroup(overlord, group.getId(),
                new int[] { resource2.getId() }, new int[] { definitionCt2.getId(), definitionCt3.getId() });
            actualResultWithNewHashCodes = new HashSet<MeasurementData>(actualResult);
            assertEquals(expectedResult2, actualResultWithNewHashCodes);

        } catch (Exception e) {
            e.printStackTrace();
            fail();
            throw e;
        }
    }

    @Test
    public void testFindLiveDataForGroup2() throws Exception {
        // prepare DB
        beginTx();
        setupGroupOfResources(em);
        commit();
        try {
            Set<MeasurementData> actualResult = measurementDataManager.findLiveDataForGroup(overlord, group.getId(),
                new int[] { resource1.getId(), resource2.getId() },
                new int[] { definitionCt1.getId(), definitionCt2.getId(), definitionCt3.getId() });
            Set<MeasurementData> actualResultWithNewHashCodes = new HashSet<MeasurementData>(actualResult);
            assertEquals(expectedResult3, actualResultWithNewHashCodes);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
            throw e;
        }
    }

    @Test
    public void testFindLiveDataForGroup3() throws Exception {
        // prepare DB
        beginTx();
        setupGroupOfResources(em);
        commit();
        try {

            Set<MeasurementData> actualResult = measurementDataManager.findLiveDataForGroup(overlord, group.getId(),
                null, new int[] { definitionCt1.getId() });
            assertEquals(Collections.emptySet(), actualResult);

        } catch (Exception e) {
            e.printStackTrace();
            fail();
            throw e;
        }
    }

    @Test
    public void testFindLiveDataForGroup4() throws Exception {
        // prepare DB
        beginTx();
        setupGroupOfResources(em);
        commit();
        try {
            measurementDataManager.findLiveDataForGroup(null, group.getId(), new int[] { resource1.getId() },
                new int[] { definitionCt1.getId() });
            fail();
        } catch (Exception e) {
            // ok, it was expected
        }
    }
    
    @Test
    public void testAddAndFindTrait1() throws Exception {
        // prepare DB
        beginTx();
        setupGroupOfResources(em);
        commit();
        try {
            Set<MeasurementDataTrait> expectedResult = new HashSet<MeasurementDataTrait>();
            for (MeasurementData data : expectedResult1) {
                data.setName(null);
                expectedResult.add((MeasurementDataTrait) data);
            }
            // add the trait data (it stores it in db (without name field))
            measurementDataManager.addTraitData(expectedResult);
            
            // get back the trait data
            List<MeasurementDataTrait> actualResult = measurementDataManager.findTraits(overlord, resource1.getId(), definitionCt1.getId());

            Set<MeasurementData> actualResultSet = new HashSet<MeasurementData>(actualResult);
            assertEquals(expectedResult, actualResultSet);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
            throw e;
        }
    }
    
    @Test
    public void testAddAndFindTrait2() throws Exception {
        // prepare DB
        beginTx();
        setupGroupOfResources(em);
        commit();
        try {
            Set<MeasurementDataTrait> expectedResult = new HashSet<MeasurementDataTrait>();
            for (MeasurementData data : expectedResult4) {
                data.setName(null);
                expectedResult.add((MeasurementDataTrait) data);
            }
            // add the trait data (it stores it in db (without name field))
            measurementDataManager.addTraitData(expectedResult);
            
            // get back the trait data
            List<MeasurementDataTrait> actualResult = measurementDataManager.findTraits(overlord, resource2.getId(), definitionCt2.getId());

            Set<MeasurementData> actualResultSet = new HashSet<MeasurementData>(actualResult);
            assertEquals(expectedResult, actualResultSet);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
            throw e;
        }
    }
    
    @Test
    public void testAddAndFindTrait3() throws Exception {
        // prepare DB
        beginTx();
        setupGroupOfResources(em);
        commit();
        try {
            Set<MeasurementDataTrait> traitsData = new HashSet<MeasurementDataTrait>();
            Set<MeasurementDataTrait> expectedResult = new HashSet<MeasurementDataTrait>();
            for (MeasurementData data : expectedResult2) {
                data.setName(null);
                traitsData.add((MeasurementDataTrait) data);
                if (data.getScheduleId() == schedule2.getId()){
                    expectedResult.add((MeasurementDataTrait) data);
                }
            }
            
            // add the trait data (it stores it in db (without name field))
            measurementDataManager.addTraitData(traitsData);
            
            // get back the trait data
            List<MeasurementDataTrait> actualResult = measurementDataManager.findTraits(overlord, resource2.getId(), definitionCt2.getId());

            Set<MeasurementData> actualResultSet = new HashSet<MeasurementData>(actualResult);
            assertEquals(expectedResult, actualResultSet);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
            throw e;
        }
    }
    
    @Test
    public void testFindNonExistentTraitByResourceId() throws Exception {
        // prepare DB
        beginTx();
        setupGroupOfResources(em);
        commit();
        try {            
            // get back the trait data
            List<MeasurementDataTrait> actualResult = measurementDataManager.findTraits(overlord, resource2.getId(), definitionCt2.getId());

            Set<MeasurementData> actualResultSet = new HashSet<MeasurementData>(actualResult);
            assertEquals(Collections.<MeasurementData> emptySet(), actualResultSet);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
            throw e;
        }
    }
    
    
    /**
     * This test tests more combination of criteria
     * @throws Exception
     */
    @Test
    public void testAddAndFindByCriteria() throws Exception {
        // prepare DB
        beginTx();
        setupGroupOfResources(em);
        commit();
        try {
            Set<MeasurementDataTrait> expectedResult = new HashSet<MeasurementDataTrait>();
            for (MeasurementData data : expectedResult1) {
                data.setName(null);
                expectedResult.add((MeasurementDataTrait) data);
            }
            // add the trait data (it stores it in db (without name field))
            measurementDataManager.addTraitData(expectedResult);
            
            // get back the trait data by schedule id
            MeasurementDataTraitCriteria criteria = new MeasurementDataTraitCriteria();
            criteria.addFilterScheduleId(schedule1.getId());
            List<MeasurementDataTrait> actualResult = measurementDataManager.findTraitsByCriteria(overlord, criteria);
            Set<MeasurementData> actualResultSet = new HashSet<MeasurementData>(actualResult);
            assertEquals(expectedResult, actualResultSet);
            
            // get back the trait data by resource id
            criteria = new MeasurementDataTraitCriteria();
            criteria.addFilterResourceId(resource1.getId());
            actualResult = measurementDataManager.findTraitsByCriteria(overlord, criteria);
            actualResultSet = new HashSet<MeasurementData>(actualResult);
            assertEquals(expectedResult, actualResultSet);
            
            // get back the trait data by schedule id and resource id 
            criteria = new MeasurementDataTraitCriteria();
            criteria.addFilterScheduleId(schedule1.getId());
            criteria.addFilterResourceId(resource1.getId());
            actualResult = measurementDataManager.findTraitsByCriteria(overlord, criteria);
            actualResultSet = new HashSet<MeasurementData>(actualResult);
            assertEquals(expectedResult, actualResultSet);
            
            // get back the trait data by wrong schedule id
            criteria = new MeasurementDataTraitCriteria();
            criteria.addFilterScheduleId(Integer.MIN_VALUE);
            actualResult = measurementDataManager.findTraitsByCriteria(overlord, criteria);
            assertTrue(actualResult.isEmpty());
        } catch (Exception e) {
            e.printStackTrace();
            fail();
            throw e;
        }
    }
    
    @Test
    public void testAddAndFindCurrentTraitByResourceId() throws Exception {
        // prepare DB
        beginTx();
        setupGroupOfResources(em);
        commit();
        try {
            // get back the trait data
            List<MeasurementDataTrait> actualResult = measurementDataManager.findCurrentTraitsForResource(overlord, resource1.getId(), null);
            assertTrue(actualResult.isEmpty());
        } catch (Exception e) {
            e.printStackTrace();
            fail();
            throw e;
        }
    }
    
    
    @Test
    public void testAddAndFindCurrentTraitByResourceIdAcrossMoreSchedules() throws Exception {
        // prepare DB
        beginTx();
        setupGroupOfResources(em);
        commit();
        try {
            Set<MeasurementDataTrait> expectedResult = new HashSet<MeasurementDataTrait>();
            for (MeasurementData data : expectedResult2) {
                data.setName(null);
                expectedResult.add((MeasurementDataTrait) data);
            }
            // add the trait data (it stores it in db (without name field))
            measurementDataManager.addTraitData(expectedResult);
            
            // get back the trait data
            List<MeasurementDataTrait> actualResult = measurementDataManager.findCurrentTraitsForResource(overlord, resource2.getId(), null);

            Set<MeasurementData> actualResultSet = new HashSet<MeasurementData>(actualResult);
            assertEquals(expectedResult, actualResultSet);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
            throw e;
        }
    }
    
    @Test
    public void testFindNonExistentCurrentTraitByResourceId() throws Exception {
        // prepare DB
        beginTx();
        setupGroupOfResources(em);
        commit();
        try {
            Set<MeasurementDataTrait> expectedResult = new HashSet<MeasurementDataTrait>();
            for (MeasurementData data : expectedResult2) {
                data.setName(null);
                expectedResult.add((MeasurementDataTrait) data);
            }
            // add the trait data (it stores it in db (without name field))
            measurementDataManager.addTraitData(expectedResult);
            
            // get back the trait data
            List<MeasurementDataTrait> actualResult = measurementDataManager.findCurrentTraitsForResource(overlord, resource2.getId(), null);

            Set<MeasurementData> actualResultSet = new HashSet<MeasurementData>(actualResult);
            assertEquals(expectedResult, actualResultSet);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
            throw e;
        }
    }
    

    private MeasurementData makeMeasurement(long time, int scheduleId, String value, String name) {
        MeasurementData measurement = new MeasurementDataTrait(new MeasurementDataPK(time, scheduleId), value);
        measurement.setName(name);
        return measurement;
    }

    private void beginTx() throws Exception {
        getTransactionManager().begin();
    }

    private void commit() throws Exception {
        em.flush();
        getTransactionManager().commit();
    }
}
