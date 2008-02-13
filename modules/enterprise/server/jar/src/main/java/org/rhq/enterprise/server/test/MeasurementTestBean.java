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

import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.rhq.core.clientapi.server.measurement.MeasurementServerService;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataPK;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.ResourceMeasurementScheduleRequest;
import org.rhq.core.domain.measurement.oob.MeasurementOutOfBounds;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementServerServiceImpl;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * An EJB for testing the measurement subsystem - used by TestControl.jsp.
 */
@Stateless
public class MeasurementTestBean implements MeasurementTestLocal {
    private static final String FAKE_PLATFORM_NAME = "Fake Platform";

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    EntityManager entityManager;

    private MeasurementServerService measurementServerService = new MeasurementServerServiceImpl();

    @EJB
    MeasurementScheduleManagerLocal measurementScheduleManager;

    /**
     * Send a test measurement report, full of lots of fake metrics, to the server.
     */
    public void sendTestMeasurementReport() {
        Resource res = setupFakePlatformIfNeeded();
        Set<ResourceMeasurementScheduleRequest> scheds;
        scheds = measurementScheduleManager.getSchedulesForResourceAndItsDescendants(res.getId(), false);

        Date now = new Date();
        MeasurementReport report = new MeasurementReport();
        for (ResourceMeasurementScheduleRequest sched : scheds) {
            for (MeasurementScheduleRequest s : sched.getMeasurementSchedules()) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(now);

                // send 10 data points per schedule
                for (int i = 0; i < 10; i++) {
                    long dat = cal.getTimeInMillis();
                    MeasurementDataNumeric mdn = new MeasurementDataNumeric(new MeasurementDataPK(dat, s
                        .getScheduleId()), (double) i);
                    report.addData(mdn);
                    cal.add(Calendar.SECOND, -1);
                }
            }
        }

        measurementServerService.mergeMeasurementReport(report);
    }

    public void addProblemResource() {
        Resource res = setupFakePlatformIfNeeded();
        Availability ava = new Availability(res, new Date(), AvailabilityType.UP);
        entityManager.persist(ava);

        if ((res.getSchedules().size() == 0) && (res.getResourceType().getMetricDefinitions().size() > 0)) {
            MeasurementDefinition def = res.getResourceType().getMetricDefinitions().iterator().next();
            MeasurementSchedule schedule = new MeasurementSchedule(def, res);
            entityManager.persist(schedule);
            res.addSchedule(schedule);
        }

        MeasurementOutOfBounds oob = new MeasurementOutOfBounds(res.getSchedules().iterator().next(), System
            .currentTimeMillis(), 1L);
        entityManager.persist(oob);
        entityManager.flush();
    }

    /**
     * Set up a fake platform and a MeasurementDefinition for it for several usages. This method will check if the fake
     * platform already exists first.
     *
     * @return
     */
    private Resource setupFakePlatformIfNeeded() {
        Resource res;
        try {
            Query q = entityManager.createNamedQuery("Resource.findByName");
            q.setParameter("name", FAKE_PLATFORM_NAME);
            res = (Resource) q.getSingleResult();
        } catch (NoResultException nre) {
            ResourceType resourceType = new ResourceType("fake platform", "", ResourceCategory.PLATFORM, null);
            resourceType.setPlugin("Platforms");
            entityManager.persist(resourceType);
            MeasurementDefinition def = new MeasurementDefinition(resourceType, "Fake Definition");
            def.setUnits(MeasurementUnits.SECONDS);
            def.setResourceType(resourceType);
            entityManager.persist(def);
            resourceType.addMetricDefinition(def);
            res = new Resource("org.jboss.on.TestPlatfor", FAKE_PLATFORM_NAME, resourceType);
            entityManager.persist(res);
            entityManager.flush();
        }

        return res;
    }

    @SuppressWarnings("deprecation")
    public void setAgentCurrentlyScheduledMetrics(double value) {
        String scheduleString = "SELECT schedule " + "FROM MeasurementSchedule schedule "
            + "WHERE schedule.definition.name = 'CurrentlyScheduleMeasurements' "
            + "AND schedule.resource.name LIKE '%Agent%'";

        Query scheduleQuery = entityManager.createQuery(scheduleString);
        MeasurementSchedule schedule = (MeasurementSchedule) scheduleQuery.getSingleResult();

        MeasurementDataNumeric numericData = new MeasurementDataNumeric(new MeasurementDataPK(schedule.getId()), value);

        entityManager.persist(numericData);

        LookupUtil.getAlertConditionCacheManager().checkConditions(numericData);
    }
}