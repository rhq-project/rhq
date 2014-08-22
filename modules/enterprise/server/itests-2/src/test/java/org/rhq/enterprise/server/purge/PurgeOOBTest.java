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

package org.rhq.enterprise.server.purge;

import static java.lang.Math.random;
import static org.rhq.core.domain.measurement.DataType.MEASUREMENT;
import static org.rhq.core.domain.measurement.NumericType.DYNAMIC;
import static org.rhq.core.domain.resource.ResourceCategory.SERVER;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.MeasurementBaseline;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementOOB;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.drift.DriftServerPluginService;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TransactionCallback;
import org.rhq.enterprise.server.test.TransactionCallbackReturnable;
import org.rhq.enterprise.server.util.Overlord;
import org.rhq.enterprise.server.util.ResourceTreeHelper;

public class PurgeOOBTest extends AbstractEJB3Test {

    private final String RESOURCE_TYPE = getClass().getName() + "_TYPE";
    private final String PLUGIN = getClass().getName() + "_PLUGIN";
    private final String AGENT_NAME = getClass().getName() + "_AGENT";
    private final String DYNAMIC_DEF_NAME = getClass().getName() + "_DYNAMIC";
    private final String RESOURCE_KEY = getClass().getName() + "_RESOURCE_KEY";
    private final String RESOURCE_NAME = getClass().getName() + "_NAME";
    private final String RESOURCE_UUID = getClass().getSimpleName() + "_UUID";

    private ResourceType resourceType;
    private Agent agent;
    private Resource resource;
    private List<MeasurementDefinition> measurementDefs;
    private List<MeasurementSchedule> schedules;

    @Inject
    @Overlord
    private Subject overlord;

    @EJB
    private ResourceManagerLocal resourceManager;

    @EJB
    private PurgeManagerLocal purgeManager;

    @Override
    protected void beforeMethod() throws Exception {
        // MeasurementDataManagerUtility looks up config settings from SystemManagerBean.
        // SystemManagerBean.getDriftServerPluginManager method requires drift server plugin.
        DriftServerPluginService driftServerPluginService = new DriftServerPluginService(getTempDir());
        prepareCustomServerPluginService(driftServerPluginService);
        driftServerPluginService.masterConfig.getPluginDirectory().mkdirs();

        measurementDefs = new ArrayList<MeasurementDefinition>();
        schedules = new ArrayList<MeasurementSchedule>();
        createInventory();
    }

    @Override
    protected void afterMethod() throws Exception {
        purgeDB();
    }

    @Test
    public void testRemoveOutdatedOOBs() throws Exception {

        long now = System.currentTimeMillis();
        long anHourAgo = now - TimeUnit.HOURS.toMillis(1);
        long halfAnHourAgo = now - TimeUnit.MINUTES.toMillis(30);

        for (int i = 0; i < 100; i++) {
            MeasurementSchedule schedule = createSchedule();
            insertBaseline(baseline(schedule, random(), random(), random()));
            if (i % 2 == 0) {
                moveBaselineBackInPast(schedule, anHourAgo);
            }
            insertOOB(oob(schedule, (int) (1000 * random()), now));
        }

        purgeManager.removeOutdatedOOBs(halfAnHourAgo);

        for (int i = 0; i < 100; i++) {
            MeasurementSchedule schedule = schedules.get(i);
            long oobCount = countOOBsFor(schedule).longValue();
            if (i % 2 == 0) {
                assertEquals(1, oobCount);
            } else {
                assertEquals(0, oobCount);
            }
        }
    }

    private void moveBaselineBackInPast(final MeasurementSchedule schedule, final long anHourAgo) {
        executeInTransaction(false, new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                String query = "update MeasurementBaseline set computeTime = :computeTime " //
                    + "where scheduleId = :scheduleId";
                getEntityManager().createQuery(query) //
                    .setParameter("scheduleId", schedule.getId()) //
                    .setParameter("computeTime", anHourAgo) //
                    .executeUpdate();
            }
        });
    }

    private Long countOOBsFor(final MeasurementSchedule schedule) {
        return executeInTransaction(new TransactionCallbackReturnable<Long>() {
            @Override
            public Long execute() throws Exception {
                String query = "select count(oob) from MeasurementOOB oob " //
                    + "where oob.id = :scheduleId";
                return getEntityManager().createQuery(query, Long.class) //
                    .setParameter("scheduleId", schedule.getId()) //
                    .getSingleResult();
            }
        });
    }

    private void insertOOB(final MeasurementOOB oob) {
        executeInTransaction(false, new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                EntityManager em = getEntityManager();
                em.persist(oob);
            }
        });
    }

    private MeasurementOOB oob(MeasurementSchedule schedule, int factor, long timestamp) {
        MeasurementOOB oob = new MeasurementOOB();
        oob.setScheduleId(schedule.getId());
        oob.setOobFactor(factor);
        oob.setTimestamp(timestamp);
        return oob;
    }

    private void insertBaseline(final MeasurementBaseline baseline) {
        executeInTransaction(false, new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                EntityManager em = getEntityManager();
                em.persist(baseline);
            }
        });
    }

    private MeasurementBaseline baseline(MeasurementSchedule schedule, double avg, double min, double max) {
        MeasurementBaseline baseline = new MeasurementBaseline();
        baseline.setSchedule(schedule);
        baseline.setMean(avg);
        baseline.setMax(max);
        baseline.setMin(min);
        return baseline;
    }

    private void createInventory() throws Exception {
        purgeDB();
        executeInTransaction(false, new TransactionCallback() {
            @Override
            public void execute() throws Exception {

                resourceType = new ResourceType(RESOURCE_TYPE, PLUGIN, SERVER, null);
                em.persist(resourceType);

                agent = new Agent(AGENT_NAME, "localhost", 9999, "", "randomToken");
                em.persist(agent);

                resource = new Resource(RESOURCE_KEY, RESOURCE_NAME, resourceType);
                resource.setUuid(RESOURCE_UUID);
                resource.setAgent(agent);

                em.persist(resource);
            }
        });
    }

    private void purgeDB() {
        purgeBaselines();
        purgeOOBs();

        executeInTransaction(false, new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                ResourceCriteria c = new ResourceCriteria();
                c.addFilterInventoryStatus(null);
                c.addFilterResourceKey(RESOURCE_KEY);
                c.fetchSchedules(true);
                List<Resource> r = resourceManager.findResourcesByCriteria(overlord, c);

                // Note that the order of deletes is important due to FK
                // constraints.
                if (!r.isEmpty()) {
                    assertTrue("Should be only 1 resource", r.size() == 1);
                    Resource doomedResource = r.get(0);
                    deleteMeasurementSchedules();
                    deleteResource(doomedResource);
                }
                deleteAgent();
                deleteDynamicMeasurementDef();
                deleteResourceType();
            }
        });
    }

    private void deleteDynamicMeasurementDef() {
        if (!measurementDefs.isEmpty()) {
            em.createQuery("delete from MeasurementDefinition d where d in :defs")
                .setParameter("defs", measurementDefs).executeUpdate();
        }
    }

    private void deleteAgent() {
        em.createQuery("delete from Agent where name = :name").setParameter("name", AGENT_NAME).executeUpdate();
    }

    private void deleteResourceType() {
        em.createQuery("delete from ResourceType where name = :name and plugin = :plugin")
            .setParameter("name", RESOURCE_TYPE).setParameter("plugin", PLUGIN).executeUpdate();
    }

    private void deleteResource(Resource doomedResource) {
        ResourceTreeHelper.deleteResource(em, doomedResource);
        em.flush();
    }

    private void deleteMeasurementSchedules() {
        for (MeasurementSchedule schedule : schedules) {
            em.createQuery("delete from MeasurementSchedule where id = :id").setParameter("id", schedule.getId())
                .executeUpdate();
        }
        em.flush();
    }

    private void purgeBaselines() {
        purgeTables("rhq_measurement_bline");
    }

    private void purgeOOBs() {
        purgeTables("rhq_measurement_oob");
    }

    private void purgeTables(final String... tables) {
        executeInTransaction(false, new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                for (String table : tables) {
                    getEntityManager().createNativeQuery("delete from " + table).executeUpdate();
                }
            }
        });
    }

    private MeasurementSchedule createSchedule() {
        return executeInTransaction(false, new TransactionCallbackReturnable<MeasurementSchedule>() {
            @Override
            public MeasurementSchedule execute() throws Exception {
                EntityManager em = getEntityManager();

                MeasurementDefinition definition = new MeasurementDefinition(resourceType, DYNAMIC_DEF_NAME
                    + measurementDefs.size());
                definition.setDefaultOn(true);
                definition.setDataType(MEASUREMENT);
                definition.setMeasurementType(DYNAMIC);
                em.persist(definition);

                MeasurementSchedule schedule = new MeasurementSchedule(definition, resource);
                schedule.setEnabled(true);
                resource.addSchedule(schedule);
                em.persist(schedule);

                schedules.add(schedule);
                measurementDefs.add(definition);

                return schedule;
            }
        });
    }
}
