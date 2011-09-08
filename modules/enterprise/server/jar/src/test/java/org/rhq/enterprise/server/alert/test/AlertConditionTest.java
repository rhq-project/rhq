/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.enterprise.server.alert.test;

import java.util.HashSet;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.transaction.TransactionManager;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.AlertDampening;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.alert.BooleanExpression;
import org.rhq.core.domain.alert.AlertDampening.Category;
import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.resource.metadata.test.UpdatePluginMetadataTestBase;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.test.JPAUtils;
import org.rhq.test.TransactionCallbackWithContext;

@Test
public class AlertConditionTest extends UpdatePluginMetadataTestBase {
    private Resource resource;
    private Server server;

    @Override
    protected String getSubsystemDirectory() {
        return "alerts";
    }

    @AfterMethod(alwaysRun = true)
    public void removePersistedResource() throws Exception {
        if (resource != null) {
            deleteNewResource(resource);
            resource = null;
        }

        deleteServerIdentity();

        return;
    }

    public void testBZ736685() throws Exception {
        // TODO: note that once BZ 735262 is fixed, THIS test will need to be rewritten
        // TODO: since this assumes the bad behavior of 735262 to test 736685

        // create our resource with alert definition
        MeasurementDefinition metricDef = createResourceWithMetricSchedule();
        createAlertDefinitionWithTwoConditionsALL(metricDef);

        // re-load the resource so we get the measurement schedule
        Resource resourceWithSchedules = loadResourceWithSchedules();
        MeasurementSchedule schedule = resourceWithSchedules.getSchedules().iterator().next();

        // simulate some measurement reports coming from the agent
        MeasurementScheduleRequest request = new MeasurementScheduleRequest(schedule);
        MeasurementReport report = new MeasurementReport();
        report.addData(new MeasurementDataNumeric(getTimestamp(60), request, Double.valueOf(20.0))); // 20 < 60
        report.addData(new MeasurementDataNumeric(getTimestamp(30), request, Double.valueOf(50.0))); // 50 > 40, 50 < 60
        MeasurementDataManagerLocal dataManager = LookupUtil.getMeasurementDataManager();
        dataManager.mergeMeasurementReport(report);

        // wait for our JMS messages to process and see if we get any alerts
        Thread.sleep(5000);

        AlertManagerLocal alertManager = LookupUtil.getAlertManager();
        AlertCriteria alertCriteria = new AlertCriteria();
        alertCriteria.addFilterResourceIds(resourceWithSchedules.getId());
        PageList<Alert> alerts = alertManager.findAlertsByCriteria(getOverlord(), alertCriteria);
        assert alerts.size() == 1 : "1 alert should have fired: " + alerts;

        int resourceId = resource.getId();
        deleteNewResource(resource);
        resource = null;

        AlertDefinitionManagerLocal alertDefManager = LookupUtil.getAlertDefinitionManager();
        PageList<AlertDefinition> defs = alertDefManager.findAlertDefinitions(getOverlord(), resourceId, PageControl
            .getUnlimitedInstance());
        assert defs.isEmpty() : "failed to delete the alert definition - are condition logs still around?";

        return;
    }

    private Resource loadResourceWithSchedules() {
        ResourceCriteria resourceCriteria = new ResourceCriteria();
        resourceCriteria.addFilterId(resource.getId());
        resourceCriteria.fetchSchedules(true);
        Resource resourceWithSchedules = getResource(resourceCriteria);
        assert resourceWithSchedules != null : "could not obtain resource from DB";
        assert resourceWithSchedules.getSchedules() != null && resourceWithSchedules.getSchedules().size() == 1 : "missing schedule";
        return resourceWithSchedules;
    }

    private void createAlertDefinitionWithTwoConditionsALL(MeasurementDefinition metricDef) {
        // create alert definition with the conditions "metric value > 40 AND metric value < 60"
        HashSet<AlertCondition> conditions = new HashSet<AlertCondition>(2);
        AlertCondition cond1 = new AlertCondition();
        cond1.setCategory(AlertConditionCategory.THRESHOLD);
        cond1.setName(metricDef.getDisplayName());
        cond1.setComparator(">");
        cond1.setThreshold(Double.valueOf(40.0)); // value > 40 threshold
        cond1.setOption(null);
        cond1.setMeasurementDefinition(metricDef);
        conditions.add(cond1);

        AlertCondition cond2 = new AlertCondition();
        cond2.setCategory(AlertConditionCategory.THRESHOLD);
        cond2.setName(metricDef.getDisplayName());
        cond2.setComparator("<");
        cond2.setThreshold(Double.valueOf(60.0)); // value < 60 threshold
        cond2.setOption(null);
        cond2.setMeasurementDefinition(metricDef);
        conditions.add(cond2);

        AlertDefinition alertDefinition = new AlertDefinition();
        alertDefinition.setName("two condition ALL alert");
        alertDefinition.setEnabled(true);
        alertDefinition.setPriority(AlertPriority.HIGH);
        alertDefinition.setAlertDampening(new AlertDampening(Category.NONE));
        alertDefinition.setRecoveryId(Integer.valueOf(0));
        alertDefinition.setConditionExpression(BooleanExpression.ALL);
        alertDefinition.setConditions(conditions);

        AlertDefinitionManagerLocal alertDefManager = LookupUtil.getAlertDefinitionManager();
        int defId = alertDefManager.createAlertDefinition(getOverlord(), alertDefinition, resource.getId());
        alertDefinition.setId(defId);

        // now that we created an alert def, we have to reload the alert condition cache
        reloadAllAlertConditionCaches();
    }

    private MeasurementDefinition createResourceWithMetricSchedule() throws Exception {
        registerPlugin("type-with-metric.xml");
        ResourceType resourceType = getResourceType("TypeWithMetrics");
        assert resourceType != null : "failed to deploy resource type";
        assert resourceType.getMetricDefinitions() != null : "failed to create metric defs";
        assert resourceType.getMetricDefinitions().size() == 1 : "do not have the expected number of metric defs";

        final MeasurementDefinition metricDef = resourceType.getMetricDefinitions().iterator().next();

        resource = persistNewResource(resourceType.getName());
        assert resource != null && resource.getId() > 0 : "failed to create test resource";

        JPAUtils.executeInTransaction(new TransactionCallbackWithContext<Object>() {
            @Override
            public Object execute(TransactionManager tm, EntityManager em) throws Exception {
                MeasurementSchedule schedule = new MeasurementSchedule(metricDef, resource);
                em.persist(schedule);
                return null;
            }
        });

        // create a server which attaches our agent to it - we need this for the alert subsystem to do its thing
        createServerIdentity();
        return metricDef;
    }

    /**
     * Returns a epoch millis timestamp that is the current time minus the given number of seconds.
     * In other words, this returns a time in the past - how far in the past is determined by the
     * number of seconds parameter.
     * @param secondsAgo
     */
    private long getTimestamp(long secondsAgo) {
        return System.currentTimeMillis() - (secondsAgo * 1000);
    }

    private void reloadAllAlertConditionCaches() {
        LookupUtil.getAlertConditionCacheManager().reloadAllCaches();
    }

    private void createServerIdentity() {
        server = new Server();
        server.setName("localhost"); // this is the default assumed in the SLSB code, we must match it
        server.setAddress("localhost");
        server.setPort(7080);
        server.setSecurePort(7443);
        server.setComputePower(1);
        server.setOperationMode(Server.OperationMode.MAINTENANCE);
        int serverId = LookupUtil.getServerManager().create(server);
        assert serverId > 0 : "could not create our server identity in the DB";

        // simulate the agent being "connected" to the server
        try {
            Agent agent = getAgent(getEntityManager());
            agent.setServer(server);
            LookupUtil.getAgentManager().updateAgent(agent);
        } catch (NoResultException nre) {
            // no agent to attach
        }
    }

    private void deleteServerIdentity() throws Exception {
        if (server != null) {
            cleanupAgent(); // can't remove the server before we purge the agent
            LookupUtil.getCloudManager().deleteServer(server.getId());
            server = null;
        }
    }
}
