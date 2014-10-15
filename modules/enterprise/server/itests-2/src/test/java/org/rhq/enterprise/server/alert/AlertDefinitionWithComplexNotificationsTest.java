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

package org.rhq.enterprise.server.alert;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.Test;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.AlertDampening;
import org.rhq.core.domain.alert.AlertDampening.TimeUnits;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.alert.BooleanExpression;
import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.cloud.Server.OperationMode;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.AlertDefinitionCriteria;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementCategory;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.plugin.ServerPlugin;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.shared.ResourceBuilder;
import org.rhq.core.domain.shared.ResourceTypeBuilder;
import org.rhq.enterprise.server.TestServerPluginService;
import org.rhq.enterprise.server.auth.SessionManager;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TestServerCommunicationsService;
import org.rhq.enterprise.server.test.TransactionCallback;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.ResourceTreeHelper;
import org.rhq.enterprise.server.util.ServerFactory;

/**
 * !!! README
 * !!! The original version of this class is nicely written and is maintained at the bottom of this file in comments.
 * !!! The issue is that Arquillian (1.0.2) does not honor the testNg lifecycle, on which the original
 * !!! implementation heavily relies (i.e. Before/AfterClass and instance variables that span all tests).
 * !!! The work needed to get it to work in a similar fashion was large, and Arquillian 2 promises to perhaps
 * !!! honor the testNg lifecycle. So, for now, I've basically condensed this into one large test to
 * !!! get it running. Sorry Lukas :(
 *
 *
 * @author Lukas Krejci
 */
@Test(groups = "alert")
public class AlertDefinitionWithComplexNotificationsTest extends AbstractEJB3Test {

    private static final Log LOG = LogFactory.getLog(AlertDefinitionWithComplexNotificationsTest.class);

    private enum ParentType {
        GROUP, TEMPLATE
    }

    private final String universalName = getClass().getSimpleName();

    private Server server;
    private Agent agent;
    private Subject subject;
    private Role role;
    private ResourceType resourceType;
    private ResourceGroup resourceGroup;
    private Set<Resource> resources;
    private AlertDefinition templateAlertDefinition;
    private AlertDefinition groupAlertDefinition;
    private AlertDefinition resourceAlertDefinition;
    private ServerPlugin serverPlugin;
    private Set<Object> junk = new LinkedHashSet<Object>();

    private int resourceLevelAlertDefinitionId;
    private int groupLevelAlertDefinitionId;
    private int templateLevelAlertDefinitionId;
    private Resource copyTestsResource;

    private TestAlertSenderPluginService alertSenderService;
    private TestServerCommunicationsService agentService;

    @Test
    public void singleMergedTest() throws Exception {
        try {
            prepareDB();
            containerSetup();
            login();

            System.out.println("Running test: testNotificationsCopiedOnAlertTemplateApplication");
            testNotificationsCopiedOnAlertTemplateApplication();

            System.out.println("Running test: testNotificationsCopiedOnGroupMemberAddition");
            testNotificationsCopiedOnGroupMemberAddition();

            System.out.println("Running test: testCorrectSubjectPassedOnResourceLevelAlertDefinitionCreation");
            testCorrectSubjectPassedOnResourceLevelAlertDefCreation();

            System.out.println("Running test: testCorrectSubjectPassedOnGroupLevelAlertDefinitionCreation");
            testCorrectSubjectPassedOnGroupLevelAlertDefCreation();

            System.out.println("Running test: testCorrectSubjectPassedOnTemplateLevelAlertDefinitionCreation");
            testCorrectSubjectPassedOnTemplateLevelAlertDefCreation();

            System.out.println("Running test: testNoValidationWhenNoNotificationUpdateOnResourceLevel");
            testNoValidationWhenNoNotificationUpdateOnResourceLevel();

            System.out.println("Running test: testNoValidationWhenNoNotificationUpdateOnGroupLevel");
            testNoValidationWhenNoNotificationUpdateOnGroupLevel();

            System.out.println("Running test: testNoValidationWhenNoNotificationUpdateOnTemplateLevel");
            testNoValidationWhenNoNotificationUpdateOnTemplateLevel();

            System.out.println("Running test: testCorrectSubjectPassedOnResourceLevelAlertDefinitionUpdate");
            testCorrectSubjectPassedOnResourceLevelAlertDefinitionUpdate();

            System.out.println("Running test: testCorrectSubjectPassedOnGroupLevelAlertDefinitionUpdate");
            testCorrectSubjectPassedOnGroupLevelAlertDefinitionUpdate();

            System.out.println("Running test: testCorrectSubjectPassedOnTemplateLevelAlertDefinitionUpdate");
            testCorrectSubjectPassedOnTemplateLevelAlertDefinitionUpdate();

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            logout();
            cleanDB();
            containerTearDown();
        }
    }

    //@BeforeClass
    private void prepareDB() {
        executeInTransaction(false, new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                EntityManager em = getEntityManager();

                agent = new Agent(universalName, "localhost", 0, "foo", "bar");

                server = ServerFactory.newInstance();
                server.setAddress("localhost");
                server.setName(universalName);
                server.setOperationMode(OperationMode.NORMAL);

                server.setAgents(Collections.singletonList(agent));

                role = new Role(universalName);
                role.addPermission(Permission.MANAGE_INVENTORY);
                role.addPermission(Permission.MANAGE_SETTINGS);

                subject = new Subject(universalName, true, false);
                subject.addRole(role);

                resourceType = new ResourceTypeBuilder().createPlatformResourceType().withId(0).withName(universalName)
                    .withPlugin(universalName).build();
                MeasurementDefinition md = new MeasurementDefinition(universalName, MeasurementCategory.PERFORMANCE,
                    MeasurementUnits.PERCENTAGE, DataType.MEASUREMENT, false, 100000, DisplayType.DETAIL);
                resourceType.addMetricDefinition(md);

                resourceGroup = new ResourceGroup(universalName, resourceType);

                resources = new LinkedHashSet<Resource>();
                for (int i = 0; i < 10; ++i) {
                    Resource res = createResourceForTest(universalName + i);

                    resources.add(res);

                    resourceGroup.addExplicitResource(res);
                }

                templateAlertDefinition = createDefinitionForTest("template", true);
                templateAlertDefinition.setResourceType(resourceType);

                groupAlertDefinition = createDefinitionForTest("group", true);
                groupAlertDefinition.setGroup(resourceGroup);

                resourceAlertDefinition = createDefinitionForTest("resource", true);
                resourceAlertDefinition.setResource(resources.iterator().next());

                em.persist(agent);
                em.persist(server);
                em.persist(role);
                em.persist(subject);
                em.persist(resourceType);
                em.persist(resourceGroup);
                for (Resource r : resources) {
                    em.persist(r);
                }
                em.persist(templateAlertDefinition);
                em.persist(groupAlertDefinition);
                em.persist(resourceAlertDefinition);

                //only need this for a short time now, so that we can precreate the plugin structure
                alertSenderService = new TestAlertSenderPluginService(getTempDir());
                prepareCustomServerPluginService(alertSenderService);
                alertSenderService.masterConfig.getPluginDirectory().mkdirs();
                unprepareServerPluginService();

                JavaArchive archive = ShrinkWrap.create(JavaArchive.class);
                archive.addClass(TestAlertSender.class);
                URL res = this.getClass().getClassLoader().getResource("test-alert-sender-serverplugin.xml");
                archive.addAsResource(res, "META-INF/rhq-serverplugin.xml");

                File pluginFile = new File(alertSenderService.masterConfig.getPluginDirectory(), "test-aler-plugin.jar");

                archive.as(ZipExporter.class).exportTo(pluginFile, true);

                //the alert sender plugin manager needs the plugins in the database...
                serverPlugin = TestServerPluginService.getPlugin(pluginFile.toURI().toURL());
                em.persist(serverPlugin);

                em.flush();
            }
        });
    }

    //@BeforeMethod
    private void containerSetup() {
        prepareScheduler();

        alertSenderService = new TestAlertSenderPluginService(getTempDir());
        prepareCustomServerPluginService(alertSenderService);
        alertSenderService.masterConfig.getPluginDirectory().mkdirs();

        alertSenderService.startMasterPluginContainer();

        agentService = prepareForTestAgents();
    }

    //@AfterMethod
    private void containerTearDown() throws Exception {
        unprepareServerPluginService();
        unprepareForTestAgents();
        unprepareScheduler();
    }

    //@AfterClass(alwaysRun = true)
    private void cleanDB() throws Exception {
        for (Object o : junk) {
            removeNoExceptions(o);
        }

        removeNoExceptions(resourceAlertDefinition);
        removeNoExceptions(groupAlertDefinition);
        removeNoExceptions(templateAlertDefinition);

        LookupUtil.getResourceGroupManager().deleteResourceGroup(LookupUtil.getSubjectManager().getOverlord(),
            resourceGroup.getId());

        executeInTransaction(false, new TransactionCallback() {
            public void execute() throws Exception {
                em.createQuery(
                    "delete from AlertNotification an where an.senderName like '" + TestAlertSender.NAME + "%'")
                    .executeUpdate();
                em.createQuery("delete from AlertCondition ac where ac.name like '" + universalName + "%'")
                    .executeUpdate();
                em.createQuery("delete from AlertDefinition ad where ad.name like '" + universalName + "%'")
                    .executeUpdate();
            }
        });

        executeInTransaction(false, new TransactionCallback() {
            public void execute() throws Exception {
                em.clear();
                for (Resource r : resources) {
                    r = em.find(Resource.class, r.getId());
                    try {
                        ResourceTreeHelper.deleteResource(em, r);
                    } catch (Exception e) {
                        LOG.error("Failed to DELETE Resource from database: " + r, e);
                    }
                }
            }
        });

        removeNoExceptions(resourceType);
        removeNoExceptions(subject);
        removeNoExceptions(role);
        removeNoExceptions(server);
        removeNoExceptions(agent);

        removeNoExceptions(serverPlugin);
    }

    //@BeforeMethod
    private void login() throws Exception {
        //the embedded server cannot do a full-blown login
        //so we hack our way in
        subject = SessionManager.getInstance().put(subject);
    }

    //@AfterMethod(alwaysRun = true)
    private void logout() throws Exception {
        if (subject != null) {
            SessionManager.getInstance().invalidate(subject.getSessionId());
        } else {
            System.err
                .println("Empty subject, the setup failed horribly. Not throwing an exception to allow the database clean.");
        }
    }

    private Resource getCopyTestsResource() throws Exception {
        if (copyTestsResource == null) {
            final String keyAndName = universalName + "-copyTests";

            LookupUtil.getResourceManager()
                .createResource(subject, createResourceForTest(keyAndName), Resource.ROOT_ID);

            //ok, now the new resource should contain the alert definition defined by the template
            ResourceCriteria crit = new ResourceCriteria();
            crit.addFilterResourceKey(keyAndName);
            crit.fetchExplicitGroups(true); //so that cleanup works
            crit.fetchAlertDefinitions(true); //so that cleanup works

            List<Resource> foundResources = LookupUtil.getResourceManager().findResourcesByCriteria(subject, crit);

            assertEquals("A new resource should have been created", 1, foundResources.size());

            Resource res = foundResources.get(0);
            resources.add(res);

            copyTestsResource = res;
        }

        return copyTestsResource;
    }

    private void testNotificationsCopiedOnAlertTemplateApplication() throws Exception {
        TestAlertSender.setExpectedSubject(null);
        TestAlertSender.resetValidateMethodCallCount();

        final Resource res = getCopyTestsResource();

        //apply the template manually - this is done in server-agent back-and-forth that we
        //don't test here and which is complex to mock out.
        //this method has to be called using the overlord subject
        LookupUtil.getAlertTemplateManager().updateAlertDefinitionsForResource(
            LookupUtil.getSubjectManager().getOverlord(), res.getId());

        assertEquals("No validation should occur on the copied notifications", 0,
            TestAlertSender.getValidateMethodCallCount());

        AlertDefinitionManagerLocal adm = LookupUtil.getAlertDefinitionManager();
        AlertDefinitionCriteria adCrit = new AlertDefinitionCriteria();
        adCrit.addFilterResourceIds(res.getId());
        adCrit.fetchAlertNotifications(true);

        List<AlertDefinition> foundAlertDefs = adm.findAlertDefinitionsByCriteria(subject, adCrit);
        junk.addAll(foundAlertDefs);

        assertEquals("The new resource should have an alert definition obtained from the template.", 1,
            foundAlertDefs.size());

        AlertDefinition defWithNotifications = foundAlertDefs.get(0);

        testSingleDependentAlertDefinition(defWithNotifications, ParentType.TEMPLATE,
            defWithNotifications.getParentId());
    }

    //@Test(dependsOnMethods = "testNotificationsCopiedOnAlertTemplateApplication")
    private void testNotificationsCopiedOnGroupMemberAddition() throws Exception {
        TestAlertSender.setExpectedSubject(null);
        TestAlertSender.resetValidateMethodCallCount();

        Resource res = getCopyTestsResource();

        LookupUtil.getResourceGroupManager().addResourcesToGroup(subject, resourceGroup.getId(),
            new int[] { res.getId() });

        assertEquals("No validation should occur on the copied notifications", 0,
            TestAlertSender.getValidateMethodCallCount());

        AlertDefinitionManagerLocal adm = LookupUtil.getAlertDefinitionManager();
        AlertDefinitionCriteria adCrit = new AlertDefinitionCriteria();
        adCrit.addFilterResourceIds(res.getId());
        adCrit.fetchAlertNotifications(true);

        List<AlertDefinition> foundAlertDefs = adm.findAlertDefinitionsByCriteria(subject, adCrit);
        junk.addAll(foundAlertDefs);

        //1 from the group, 1 from the template
        assertEquals("The new resource should have an alert definition obtained from the group.", 2,
            foundAlertDefs.size());

        AlertDefinition groupOriginatingDef = null;
        for (AlertDefinition d : foundAlertDefs) {
            if ((universalName + ":group").equals(d.getName())) {
                groupOriginatingDef = d;
                break;
            }
        }

        assertNotNull("The alert definition originating from the group not present on the resource.",
            groupOriginatingDef);

        testSingleDependentAlertDefinition(groupOriginatingDef, ParentType.GROUP, groupOriginatingDef
            .getGroupAlertDefinition().getId());
    }

    private void testCorrectSubjectPassedOnResourceLevelAlertDefCreation() throws Exception {
        TestAlertSender.setExpectedSubject(subject);

        AlertDefinitionManagerLocal adm = LookupUtil.getAlertDefinitionManager();

        Resource res = resources.iterator().next();

        AlertDefinition def = createDefinitionForTest("testCorrectSubjectPassedOnResourceLevelAlertDef", false);
        def.setResource(res);

        def = adm.createAlertDefinitionInNewTransaction(subject, def, res.getId(), true);

        resourceLevelAlertDefinitionId = def.getId();

        junk.add(def);

        testMainAlertDefinition(resourceLevelAlertDefinitionId);
    }

    //@Test(dependsOnMethods = { "testNotificationsCopiedOnAlertTemplateApplication",
    //    "testNotificationsCopiedOnGroupMemberAddition" })
    private void testCorrectSubjectPassedOnGroupLevelAlertDefCreation() throws Exception {
        TestAlertSender.setExpectedSubject(subject);

        GroupAlertDefinitionManagerLocal gadm = LookupUtil.getGroupAlertDefinitionManager();

        AlertDefinition def = createDefinitionForTest("testCorrectSubjectPassedOnGroupLevelAlertDef", false);
        def.setGroup(resourceGroup);

        int id = gadm.createGroupAlertDefinitions(subject, def, resourceGroup.getId());
        def.setId(id);

        groupLevelAlertDefinitionId = id;

        junk.add(def);

        testMainAlertDefinition(id);
        List<AlertDefinition> deps = testDependentAlertDefinitions(id, ParentType.GROUP);

        junk.addAll(deps);
    }

    //@Test(dependsOnMethods = { "testNotificationsCopiedOnAlertTemplateApplication",
    //    "testNotificationsCopiedOnGroupMemberAddition" })
    private void testCorrectSubjectPassedOnTemplateLevelAlertDefCreation() throws Exception {
        TestAlertSender.setExpectedSubject(subject);

        AlertTemplateManagerLocal atm = LookupUtil.getAlertTemplateManager();

        AlertDefinition def = createDefinitionForTest("testCorrectSubjectPassedOnTemplateLevelAlertDef", false);
        def.setGroup(resourceGroup);

        int id = atm.createAlertTemplate(subject, def, resourceType.getId());
        def.setId(id);

        templateLevelAlertDefinitionId = id;

        junk.add(def);

        testMainAlertDefinition(id);
        List<AlertDefinition> deps = testDependentAlertDefinitions(id, ParentType.TEMPLATE);

        junk.addAll(deps);
    }

    //@Test(dependsOnMethods = "testCorrectSubjectPassedOnResourceLevelAlertDefinitionCreation")
    private void testNoValidationWhenNoNotificationUpdateOnResourceLevel() throws Exception {
        TestAlertSender.setExpectedSubject(subject);
        TestAlertSender.resetValidateMethodCallCount();

        AlertDefinitionManagerLocal adm = LookupUtil.getAlertDefinitionManager();

        AlertDefinitionCriteria crit = new AlertDefinitionCriteria();
        crit.addFilterId(resourceLevelAlertDefinitionId);
        crit.fetchAlertNotifications(true);
        crit.fetchConditions(true);

        List<AlertDefinition> foundDefs = adm.findAlertDefinitionsByCriteria(subject, crit);

        assertEquals("Failed to find the previously created resource level alert definition.", 1, foundDefs.size());

        AlertDefinition foundDef = foundDefs.get(0);

        foundDef.setEnabled(true);

        adm.updateAlertDefinition(subject, resourceLevelAlertDefinitionId, foundDef, false);

        assertEquals("The notification validation method shouldn't have been called", 0,
            TestAlertSender.getValidateMethodCallCount());
    }

    //@Test(dependsOnMethods = "testCorrectSubjectPassedOnGroupLevelAlertDefinitionCreation")
    private void testNoValidationWhenNoNotificationUpdateOnGroupLevel() throws Exception {
        TestAlertSender.setExpectedSubject(subject);
        TestAlertSender.resetValidateMethodCallCount();

        AlertDefinitionManagerLocal adm = LookupUtil.getAlertDefinitionManager();

        AlertDefinitionCriteria crit = new AlertDefinitionCriteria();
        crit.addFilterId(groupLevelAlertDefinitionId);
        crit.fetchAlertNotifications(true);
        crit.fetchConditions(true);

        List<AlertDefinition> foundDefs = adm.findAlertDefinitionsByCriteria(subject, crit);

        assertEquals("Failed to find the previously created group level alert definition.", 1, foundDefs.size());

        AlertDefinition foundDef = foundDefs.get(0);

        foundDef.setEnabled(true);

        GroupAlertDefinitionManagerLocal gadm = LookupUtil.getGroupAlertDefinitionManager();
        gadm.updateGroupAlertDefinitions(subject, foundDef, true);

        assertEquals("The notification validation method shouldn't have been called", 0,
            TestAlertSender.getValidateMethodCallCount());
    }

    //@Test(dependsOnMethods = "testCorrectSubjectPassedOnTemplateLevelAlertDefinitionCreation")
    private void testNoValidationWhenNoNotificationUpdateOnTemplateLevel() throws Exception {
        TestAlertSender.setExpectedSubject(subject);
        TestAlertSender.resetValidateMethodCallCount();

        AlertDefinitionManagerLocal adm = LookupUtil.getAlertDefinitionManager();

        AlertDefinitionCriteria crit = new AlertDefinitionCriteria();
        crit.addFilterId(templateLevelAlertDefinitionId);
        crit.fetchAlertNotifications(true);
        crit.fetchConditions(true);

        List<AlertDefinition> foundDefs = adm.findAlertDefinitionsByCriteria(subject, crit);

        assertEquals("Failed to find the previously created resource level alert definition.", 1, foundDefs.size());

        AlertDefinition foundDef = foundDefs.get(0);

        foundDef.setEnabled(true);

        AlertTemplateManagerLocal atm = LookupUtil.getAlertTemplateManager();

        atm.updateAlertTemplate(subject, foundDef, true);

        assertEquals("The notification validation method shouldn't have been called", 0,
            TestAlertSender.getValidateMethodCallCount());
    }

    //@Test(dependsOnMethods = "testNoValidationWhenNoNotificationUpdateOnResourceLevel")
    private void testCorrectSubjectPassedOnResourceLevelAlertDefinitionUpdate() throws Exception {
        TestAlertSender.setExpectedSubject(subject);
        TestAlertSender.resetValidateMethodCallCount();

        AlertDefinitionManagerLocal adm = LookupUtil.getAlertDefinitionManager();

        AlertDefinitionCriteria crit = new AlertDefinitionCriteria();
        crit.addFilterId(resourceLevelAlertDefinitionId);
        crit.fetchAlertNotifications(true);
        crit.fetchConditions(true);

        List<AlertDefinition> foundDefs = adm.findAlertDefinitionsByCriteria(subject, crit);

        assertEquals("Failed to find the previously created resource level alert definition.", 1, foundDefs.size());

        AlertDefinition foundDef = foundDefs.get(0);

        AlertNotification newNotif = createAlertNotificationForTest(foundDef, false);

        adm.updateAlertDefinition(subject, resourceLevelAlertDefinitionId, foundDef, false);

        assertEquals("Validation should have been called for a new notification during alert def update", 1,
            TestAlertSender.getValidateMethodCallCount());
    }

    //@Test(dependsOnMethods = "testCorrectSubjectPassedOnGroupLevelAlertDefinitionCreation")
    private void testCorrectSubjectPassedOnGroupLevelAlertDefinitionUpdate() throws Exception {
        TestAlertSender.setExpectedSubject(subject);
        TestAlertSender.resetValidateMethodCallCount();

        AlertDefinitionManagerLocal adm = LookupUtil.getAlertDefinitionManager();

        AlertDefinitionCriteria crit = new AlertDefinitionCriteria();
        crit.addFilterId(groupLevelAlertDefinitionId);
        crit.fetchAlertNotifications(true);
        crit.fetchConditions(true);

        List<AlertDefinition> foundDefs = adm.findAlertDefinitionsByCriteria(subject, crit);

        assertEquals("Failed to find the previously created group level alert definition.", 1, foundDefs.size());

        AlertDefinition foundDef = foundDefs.get(0);

        AlertNotification newNotif = createAlertNotificationForTest(foundDef, false);

        GroupAlertDefinitionManagerLocal gadm = LookupUtil.getGroupAlertDefinitionManager();
        gadm.updateGroupAlertDefinitions(subject, foundDef, true);

        //notice that the validation should be called just once, even though in effect we're creating 11 notifs
        //1 for group and 10 for its members.
        assertEquals("Validation should have been called for a new notification during alert def update", 1,
            TestAlertSender.getValidateMethodCallCount());
    }

    //@Test(dependsOnMethods = "testCorrectSubjectPassedOnTemplateLevelAlertDefinitionCreation")
    private void testCorrectSubjectPassedOnTemplateLevelAlertDefinitionUpdate() throws Exception {
        TestAlertSender.setExpectedSubject(subject);
        TestAlertSender.resetValidateMethodCallCount();

        AlertDefinitionManagerLocal adm = LookupUtil.getAlertDefinitionManager();

        AlertDefinitionCriteria crit = new AlertDefinitionCriteria();
        crit.addFilterId(templateLevelAlertDefinitionId);
        crit.fetchAlertNotifications(true);
        crit.fetchConditions(true);

        List<AlertDefinition> foundDefs = adm.findAlertDefinitionsByCriteria(subject, crit);

        assertEquals("Failed to find the previously created template level alert definition.", 1, foundDefs.size());

        AlertDefinition foundDef = foundDefs.get(0);

        AlertNotification newNotif = createAlertNotificationForTest(foundDef, false);

        AlertTemplateManagerLocal atm = LookupUtil.getAlertTemplateManager();
        atm.updateAlertTemplate(subject, foundDef, true);

        //notice that the validation should be called just once, even though in effect we're creating 11 notifs
        //1 for template and 10 for its members.
        assertEquals("Validation should have been called for a new notification during alert def update", 1,
            TestAlertSender.getValidateMethodCallCount());
    }

    private void removeNoExceptions(final Object o) {
        try {
            executeInTransaction(false, new TransactionCallback() {
                public void execute() {
                    EntityManager em = getEntityManager();
                    Object o2 = em.merge(o);

                    if (o2 instanceof Resource) {
                        ResourceTreeHelper.deleteResource(em, (Resource) o2);
                    } else {
                        em.remove(o2);
                    }

                    em.flush();
                }
            });
        } catch (Exception e) {
            LOG.error("Failed to DELETE an object from database: " + o, e);
        }
    }

    private AlertDefinition createDefinition(String name) {
        AlertDefinition ret = new AlertDefinition();
        ret.setName(universalName + ":" + name);
        ret.setPriority(AlertPriority.MEDIUM);
        ret.setConditionExpression(BooleanExpression.ANY);
        ret.setRecoveryId(0);

        AlertCondition ac = new AlertCondition();
        ac.setName(universalName + ":" + name);
        ac.setCategory(AlertConditionCategory.THRESHOLD);
        ac.setComparator(">");
        ac.setThreshold(0.75D);
        //for (MeasurementDefinition d : resourceType.getMetricDefinitions()) {
        //    if ("Calculated.HeapUsagePercentage".equals(d.getName())) {
        //        ac.setMeasurementDefinition(d);
        //        ac.setName(d.getDisplayName());
        //        break;
        //    }
        // }
        //assert null != ac.getMeasurementDefinition() : "Did not find expected measurement definition [Calculated.HeapUsagePercentage] for "
        //    + resourceType;
        ret.addCondition(ac);

        AlertDampening dampener = new AlertDampening(AlertDampening.Category.PARTIAL_COUNT);
        dampener.setPeriod(15);
        dampener.setPeriodUnits(TimeUnits.MINUTES);
        dampener.setValue(10);
        ret.setAlertDampening(dampener);

        return ret;
    }

    private AlertDefinition createDefinitionForTest(String name, boolean precanned) {
        AlertDefinition def = createDefinition(name);
        createAlertNotificationForTest(def, precanned);

        return def;
    }

    private AlertNotification createAlertNotificationForTest(AlertDefinition alertDefinition, boolean precanned) {
        AlertNotification notif = new AlertNotification(TestAlertSender.NAME);

        Configuration alertConfig = new Configuration();

        //generate random property so that the notifications are distinguishable from each other
        //and are saved separately
        Random randomGenerator = new Random();
        String randomValue = randomGenerator.nextInt(100) + " - " + randomGenerator.nextInt(200);
        alertConfig.put(new PropertySimple(randomValue, randomValue));

        if (precanned) {
            alertConfig.put(new PropertySimple(TestAlertSender.PERSISTENT_PROPERTY_NAME,
                TestAlertSender.PERSISTEN_PROPERTY_EXPECTED_VALUE));
        } else {
            alertConfig.put(new PropertySimple(TestAlertSender.PERSISTENT_PROPERTY_NAME, "persistent"));
            alertConfig.put(new PropertySimple(TestAlertSender.EPHEMERAL_PROPERTY_NAME, "ephemeral"));
        }

        Configuration extraConfig = new Configuration();

        if (precanned) {
            extraConfig.put(new PropertySimple(TestAlertSender.PERSISTENT_PROPERTY_NAME,
                TestAlertSender.PERSISTEN_PROPERTY_EXPECTED_VALUE));
        } else {
            extraConfig.put(new PropertySimple(TestAlertSender.PERSISTENT_PROPERTY_NAME, "persistent"));
            extraConfig.put(new PropertySimple(TestAlertSender.EPHEMERAL_PROPERTY_NAME, "ephemeral"));
        }

        notif.setConfiguration(alertConfig);
        notif.setExtraConfiguration(extraConfig);

        alertDefinition.addAlertNotification(notif);
        notif.setAlertDefinition(alertDefinition);

        return notif;
    }

    private void testMainAlertDefinition(int id) {
        AlertDefinitionCriteria crit = new AlertDefinitionCriteria();
        crit.addFilterId(id);
        crit.fetchAlertNotifications(true);

        List<AlertDefinition> checkList = LookupUtil.getAlertDefinitionManager().findAlertDefinitionsByCriteria(
            subject, crit);

        assertNotNull("Failed to retrieve the save alert definition", checkList);
        assertEquals("The alert definition should have been saved.", 1, checkList.size());

        AlertDefinition check = checkList.get(0);

        assertEquals("There should be exactly 1 notification on the definition", 1, check.getAlertNotifications()
            .size());

        Configuration config = check.getAlertNotifications().get(0).getConfiguration();
        assertEquals("Unexpected persistent value in notif config", TestAlertSender.PERSISTEN_PROPERTY_EXPECTED_VALUE,
            config.getSimpleValue(TestAlertSender.PERSISTENT_PROPERTY_NAME, null));
        assertNull("Ephemeral property seems to have been saved",
            config.getSimpleValue(TestAlertSender.EPHEMERAL_PROPERTY_NAME, null));
    }

    private List<AlertDefinition> testDependentAlertDefinitions(int expectedParentId, ParentType parentType) {
        AlertDefinitionCriteria crit = new AlertDefinitionCriteria();

        Set<Integer> resourceIds = new HashSet<Integer>(resources.size());
        for (Resource r : resources) {
            resourceIds.add(r.getId());
        }

        if (parentType == ParentType.TEMPLATE) {
            crit.addFilterAlertTemplateParentId(expectedParentId);
        } else if (parentType == ParentType.GROUP) {
            crit.addFilterGroupAlertDefinitionId(expectedParentId);
        }

        crit.fetchAlertNotifications(true);

        List<AlertDefinition> checkList = LookupUtil.getAlertDefinitionManager().findAlertDefinitionsByCriteria(
            subject, crit);

        assertNotNull("Failed to retrieve the save alert definition", checkList);
        assertEquals("The dependent alert definitions should have been saved.", resources.size(), checkList.size());

        for (AlertDefinition check : checkList) {
            testSingleDependentAlertDefinition(check, parentType, expectedParentId);
        }

        return checkList;
    }

    private void testSingleDependentAlertDefinition(AlertDefinition alertDef, ParentType parentType,
        int expectedParentId) {
        assertEquals("There should be exactly 1 notification on the definition " + alertDef, 1, alertDef
            .getAlertNotifications().size());

        Configuration config = alertDef.getAlertNotifications().get(0).getConfiguration();
        assertEquals("Unexpected persistent value in notif config " + alertDef,
            TestAlertSender.PERSISTEN_PROPERTY_EXPECTED_VALUE,
            config.getSimpleValue(TestAlertSender.PERSISTENT_PROPERTY_NAME, null));
        assertNull("Ephemeral property seems to have been saved " + alertDef,
            config.getSimpleValue(TestAlertSender.EPHEMERAL_PROPERTY_NAME, null));

        if (parentType == ParentType.GROUP) {
            assertEquals("The group parent id has unexpected value", expectedParentId, alertDef
                .getGroupAlertDefinition().getId());
        } else if (parentType == ParentType.TEMPLATE) {
            assertEquals("The parent id has unexpected value", Integer.valueOf(expectedParentId),
                alertDef.getParentId());
        }
    }

    private Resource createResourceForTest(String resourceKey) {
        Resource res = new ResourceBuilder().createPlatform().withRandomUuid().withResourceKey(resourceKey)
            .withResourceType(resourceType).withName(resourceKey).withInventoryStatus(InventoryStatus.COMMITTED)
            .build();
        res.setAgent(agent);

        return res;
    }
}

///**
// *
// *
// * @author Lukas Krejci
// */
//@Test(groups = "alert")
//public class AlertDefinitionWithComplexNotificationsTest extends AbstractEJB3Test {
//
//    private static final Log LOG = LogFactory.getLog(AlertDefinitionWithComplexNotificationsTest.class);
//
//    private enum ParentType {
//        GROUP, TEMPLATE
//    }
//
//    private String universalName;
//
//    private Server server;
//    private Agent agent;
//    private Subject subject;
//    private Role role;
//    private ResourceType resourceType;
//    private ResourceGroup resourceGroup;
//    private Set<Resource> resources;
//    private AlertDefinition templateAlertDefinition;
//    private AlertDefinition groupAlertDefinition;
//    private AlertDefinition resourceAlertDefinition;
//    private ServerPlugin serverPlugin;
//    private Set<Object> junk = new LinkedHashSet<Object>();
//
//    private int resourceLevelAlertDefinitionId;
//    private int groupLevelAlertDefinitionId;
//    private int templateLevelAlertDefinitionId;
//    private Resource copyTestsResource;
//
//    private TestAlertSenderPluginService alertSenderService;
//    private TestServerCommunicationsService agentService;
//
//    @BeforeClass
//    public void prepareDB() {
//        executeInTransaction(new TransactionCallback() {
//            @Override
//            public void execute() throws Exception {
//                EntityManager em = getEntityManager();
//
//                universalName = getClass().getName();
//
//                agent = new Agent("localhost", "localhost", 0, "foo", "bar");
//
//                server = ServerFactory.newInstance();
//                server.setAddress("localhost");
//                server.setName("localhost");
//                server.setOperationMode(OperationMode.NORMAL);
//
//                server.setAgents(Collections.singletonList(agent));
//
//                role = new Role(universalName);
//                role.addPermission(Permission.MANAGE_INVENTORY);
//                role.addPermission(Permission.MANAGE_SETTINGS);
//
//                subject = new Subject(universalName, true, false);
//                subject.addRole(role);
//
//                resourceType =
//                    new ResourceTypeBuilder().createPlatformResourceType().withId(0).withName(universalName)
//                        .withPlugin(universalName).build();
//
//                resourceGroup = new ResourceGroup(universalName, resourceType);
//
//                resources = new LinkedHashSet<Resource>();
//                for (int i = 0; i < 10; ++i) {
//                    Resource res = createResourceForTest(universalName + i);
//
//                    resources.add(res);
//
//                    resourceGroup.addExplicitResource(res);
//                }
//
//                templateAlertDefinition = createDefinitionForTest(universalName + " template", true);
//                templateAlertDefinition.setResourceType(resourceType);
//
//                groupAlertDefinition = createDefinitionForTest(universalName + " group", true);
//                groupAlertDefinition.setResourceGroup(resourceGroup);
//
//                resourceAlertDefinition = createDefinitionForTest(universalName + " resource", true);
//                resourceAlertDefinition.setResource(resources.iterator().next());
//
//                em.persist(agent);
//                em.persist(server);
//                em.persist(role);
//                em.persist(subject);
//                em.persist(resourceType);
//                em.persist(resourceGroup);
//                for (Resource r : resources) {
//                    em.persist(r);
//                }
//                em.persist(templateAlertDefinition);
//                em.persist(groupAlertDefinition);
//                em.persist(resourceAlertDefinition);
//
//                //only need this for a short time now, so that we can precreate the plugin structure
//                alertSenderService = new TestAlertSenderPluginService();
//                prepareCustomServerPluginService(alertSenderService);
//                alertSenderService.masterConfig.getPluginDirectory().mkdirs();
//                unprepareServerPluginService();
//
//                JavaArchive archive =
//                    ShrinkWrap.create(JavaArchive.class).addClass(TestAlertSender.class)
//                        .addAsResource("test-alert-sender-serverplugin.xml", "META-INF/rhq-serverplugin.xml");
//
//                File pluginFile =
//                    new File(alertSenderService.masterConfig.getPluginDirectory(), "test-aler-plugin.jar");
//
//                archive.as(ZipExporter.class).exportTo(pluginFile, true);
//
//                //the alert sender plugin manager needs the plugins in the database...
//                serverPlugin = TestServerPluginService.getPlugin(pluginFile.toURI().toURL());
//                em.persist(serverPlugin);
//            }
//        });
//    }
//
//    @BeforeMethod
//    public void containerSetup() {
//        alertSenderService = new TestAlertSenderPluginService();
//        prepareCustomServerPluginService(alertSenderService);
//        alertSenderService.masterConfig.getPluginDirectory().mkdirs();
//
//        alertSenderService.startMasterPluginContainer();
//
//        agentService = prepareForTestAgents();
//    }
//
//    @AfterMethod
//    public void containerTearDown() throws Exception {
//        unprepareServerPluginService();
//        unprepareForTestAgents();
//    }
//
//    @AfterClass(alwaysRun = true)
//    public void cleanDB() throws Exception {
//        for (Object o : junk) {
//            removeNoExceptions(o);
//        }
//
//        removeNoExceptions(resourceAlertDefinition);
//        removeNoExceptions(groupAlertDefinition);
//        removeNoExceptions(templateAlertDefinition);
//        removeNoExceptions(resourceGroup);
//        for (Resource r : resources) {
//            r.removeExplicitGroup(resourceGroup);
//            r.getAlertDefinitions().clear();
//            removeNoExceptions(r);
//        }
//        removeNoExceptions(resourceType);
//        removeNoExceptions(subject);
//        removeNoExceptions(role);
//        removeNoExceptions(server);
//        removeNoExceptions(agent);
//
//        removeNoExceptions(serverPlugin);
//    }
//
//    @BeforeMethod
//    public void login() throws Exception {
//        //the embedded server cannot do a full-blown login
//        //so we hack our way in
//        subject = SessionManager.getInstance().put(subject);
//    }
//
//    @AfterMethod(alwaysRun = true)
//    public void logout() throws Exception {
//        SessionManager.getInstance().invalidate(subject.getSessionId());
//    }
//
//    private Resource getCopyTestsResource() throws Exception {
//        if (copyTestsResource == null) {
//            final String keyAndName = universalName + "-copyTests";
//
//            LookupUtil.getResourceManager().createResource(subject, createResourceForTest(keyAndName), Resource.ROOT_ID);
//
//            //ok, now the new resource should contain the alert definition defined by the template
//            ResourceCriteria crit = new ResourceCriteria();
//            crit.addFilterResourceKey(keyAndName);
//            crit.fetchExplicitGroups(true); //so that cleanup works
//            crit.fetchAlertDefinitions(true); //so that cleanup works
//
//            List<Resource> foundResources = LookupUtil.getResourceManager().findResourcesByCriteria(subject, crit);
//
//            assertEquals("A new resource should have been created", 1, foundResources.size());
//
//            Resource res = foundResources.get(0);
//            resources.add(res);
//
//            copyTestsResource = res;
//        }
//
//        return copyTestsResource;
//    }
//
//    public void testNotificationsCopiedOnAlertTemplateApplication() throws Exception {
//        TestAlertSender.setExpectedSubject(null);
//        TestAlertSender.resetValidateMethodCallCount();
//
//        Resource res = getCopyTestsResource();
//
//        //apply the template manually - this is done in server-agent back-and-forth that we
//        //don't test here and which is complex to mock out.
//        //this method has to be called using the overlord subject
//        LookupUtil.getAlertTemplateManager().updateAlertDefinitionsForResource(LookupUtil.getSubjectManager().getOverlord(), res.getId());
//
//        assertEquals("No validation should occur on the copied notifications", 0, TestAlertSender.getValidateMethodCallCount());
//
//        AlertDefinitionManagerLocal adm = LookupUtil.getAlertDefinitionManager();
//        AlertDefinitionCriteria adCrit = new AlertDefinitionCriteria();
//        adCrit.addFilterResourceIds(res.getId());
//        adCrit.fetchAlertNotifications(true);
//
//        List<AlertDefinition> foundAlertDefs = adm.findAlertDefinitionsByCriteria(subject, adCrit);
//        junk.addAll(foundAlertDefs);
//
//        assertEquals("The new resource should have an alert definition obtained from the template.", 1, foundAlertDefs.size());
//
//        AlertDefinition defWithNotifications = foundAlertDefs.get(0);
//
//        testSingleDependentAlertDefinition(defWithNotifications, ParentType.TEMPLATE, defWithNotifications.getParentId());
//    }
//
//    @Test(dependsOnMethods = "testNotificationsCopiedOnAlertTemplateApplication")
//    public void testNotificationsCopiedOnGroupMemberAddition() throws Exception {
//        TestAlertSender.setExpectedSubject(null);
//        TestAlertSender.resetValidateMethodCallCount();
//
//        Resource res = getCopyTestsResource();
//
//        LookupUtil.getResourceGroupManager().addResourcesToGroup(subject, resourceGroup.getId(), new int[] { res.getId() });
//
//        assertEquals("No validation should occur on the copied notifications", 0, TestAlertSender.getValidateMethodCallCount());
//
//        AlertDefinitionManagerLocal adm = LookupUtil.getAlertDefinitionManager();
//        AlertDefinitionCriteria adCrit = new AlertDefinitionCriteria();
//        adCrit.addFilterResourceIds(res.getId());
//        adCrit.fetchAlertNotifications(true);
//
//        List<AlertDefinition> foundAlertDefs = adm.findAlertDefinitionsByCriteria(subject, adCrit);
//        junk.addAll(foundAlertDefs);
//
//        //1 from the group, 1 from the template
//        assertEquals("The new resource should have an alert definition obtained from the group.", 2, foundAlertDefs.size());
//
//        AlertDefinition groupOriginatingDef = null;
//        for(AlertDefinition d : foundAlertDefs) {
//            if ((universalName + " group").equals(d.getName())) {
//                groupOriginatingDef = d;
//                break;
//            }
//        }
//
//        assertNotNull("The alert definition originating from the group not present on the resource.", groupOriginatingDef);
//
//        testSingleDependentAlertDefinition(groupOriginatingDef, ParentType.GROUP, groupOriginatingDef.getGroupAlertDefinition().getId());
//    }
//
//    public void testCorrectSubjectPassedOnResourceLevelAlertDefinitionCreation() throws Exception {
//        TestAlertSender.setExpectedSubject(subject);
//
//        AlertDefinitionManagerLocal adm = LookupUtil.getAlertDefinitionManager();
//
//        Resource res = resources.iterator().next();
//
//        AlertDefinition def = createDefinitionForTest("testCorrectSubjectPassedOnResourceLevelAlertDefinitionCreation", false);
//        def.setResource(resources.iterator().next());
//
//        int id = adm.createAlertDefinition(subject, def, res.getId(), true);
//        def.setId(id);
//
//        resourceLevelAlertDefinitionId = id;
//
//        junk.add(def);
//
//        testMainAlertDefinition(id);
//    }
//
//    @Test(dependsOnMethods = { "testNotificationsCopiedOnAlertTemplateApplication", "testNotificationsCopiedOnGroupMemberAddition" })
//    public void testCorrectSubjectPassedOnGroupLevelAlertDefinitionCreation() throws Exception {
//        TestAlertSender.setExpectedSubject(subject);
//
//        GroupAlertDefinitionManagerLocal gadm = LookupUtil.getGroupAlertDefinitionManager();
//
//        AlertDefinition def = createDefinitionForTest("testCorrectSubjectPassedOnGroupLevelAlertDefinitionCreation", false);
//        def.setResourceGroup(resourceGroup);
//
//        int id = gadm.createGroupAlertDefinitions(subject, def, resourceGroup.getId());
//        def.setId(id);
//
//        groupLevelAlertDefinitionId = id;
//
//        junk.add(def);
//
//        testMainAlertDefinition(id);
//        List<AlertDefinition> deps = testDependentAlertDefinitions(id, ParentType.GROUP);
//
//        junk.addAll(deps);
//    }
//
//    @Test(dependsOnMethods = { "testNotificationsCopiedOnAlertTemplateApplication", "testNotificationsCopiedOnGroupMemberAddition" })
//    public void testCorrectSubjectPassedOnTemplateLevelAlertDefinitionCreation() throws Exception {
//        TestAlertSender.setExpectedSubject(subject);
//
//        AlertTemplateManagerLocal atm = LookupUtil.getAlertTemplateManager();
//
//        AlertDefinition def = createDefinitionForTest("testCorrectSubjectPassedOnTemplateLevelAlertDefinitionCreation", false);
//        def.setResourceGroup(resourceGroup);
//
//        int id = atm.createAlertTemplate(subject, def, resourceType.getId());
//        def.setId(id);
//
//        templateLevelAlertDefinitionId = id;
//
//        junk.add(def);
//
//        testMainAlertDefinition(id);
//        List<AlertDefinition> deps = testDependentAlertDefinitions(id, ParentType.TEMPLATE);
//
//        junk.addAll(deps);
//    }
//
//    @Test(dependsOnMethods = "testCorrectSubjectPassedOnResourceLevelAlertDefinitionCreation")
//    public void testNoValidationWhenNoNotificationUpdateOnResourceLevel() throws Exception {
//        TestAlertSender.setExpectedSubject(subject);
//        TestAlertSender.resetValidateMethodCallCount();
//
//        AlertDefinitionManagerLocal adm = LookupUtil.getAlertDefinitionManager();
//
//        AlertDefinitionCriteria crit = new AlertDefinitionCriteria();
//        crit.addFilterId(resourceLevelAlertDefinitionId);
//        crit.fetchAlertNotifications(true);
//        crit.fetchConditions(true);
//
//        List<AlertDefinition> foundDefs = adm.findAlertDefinitionsByCriteria(subject, crit);
//
//        assertEquals("Failed to find the previously created resource level alert definition.", 1, foundDefs.size());
//
//        AlertDefinition foundDef = foundDefs.get(0);
//
//        foundDef.setEnabled(true);
//
//        adm.updateAlertDefinition(subject, resourceLevelAlertDefinitionId, foundDef, false);
//
//        assertEquals("The notification validation method shouldn't have been called", 0, TestAlertSender.getValidateMethodCallCount());
//    }
//
//    @Test(dependsOnMethods = "testCorrectSubjectPassedOnGroupLevelAlertDefinitionCreation")
//    public void testNoValidationWhenNoNotificationUpdateOnGroupLevel() throws Exception {
//        TestAlertSender.setExpectedSubject(subject);
//        TestAlertSender.resetValidateMethodCallCount();
//
//        AlertDefinitionManagerLocal adm = LookupUtil.getAlertDefinitionManager();
//
//        AlertDefinitionCriteria crit = new AlertDefinitionCriteria();
//        crit.addFilterId(groupLevelAlertDefinitionId);
//        crit.fetchAlertNotifications(true);
//        crit.fetchConditions(true);
//
//        List<AlertDefinition> foundDefs = adm.findAlertDefinitionsByCriteria(subject, crit);
//
//        assertEquals("Failed to find the previously created group level alert definition.", 1, foundDefs.size());
//
//        AlertDefinition foundDef = foundDefs.get(0);
//
//        foundDef.setEnabled(true);
//
//        GroupAlertDefinitionManagerLocal gadm = LookupUtil.getGroupAlertDefinitionManager();
//        gadm.updateGroupAlertDefinitions(subject, foundDef, true);
//
//        assertEquals("The notification validation method shouldn't have been called", 0, TestAlertSender.getValidateMethodCallCount());
//    }
//
//    @Test(dependsOnMethods = "testCorrectSubjectPassedOnTemplateLevelAlertDefinitionCreation")
//    public void testNoValidationWhenNoNotificationUpdateOnTemplateLevel() throws Exception {
//        TestAlertSender.setExpectedSubject(subject);
//        TestAlertSender.resetValidateMethodCallCount();
//
//        AlertDefinitionManagerLocal adm = LookupUtil.getAlertDefinitionManager();
//
//        AlertDefinitionCriteria crit = new AlertDefinitionCriteria();
//        crit.addFilterId(templateLevelAlertDefinitionId);
//        crit.fetchAlertNotifications(true);
//        crit.fetchConditions(true);
//
//        List<AlertDefinition> foundDefs = adm.findAlertDefinitionsByCriteria(subject, crit);
//
//        assertEquals("Failed to find the previously created resource level alert definition.", 1, foundDefs.size());
//
//        AlertDefinition foundDef = foundDefs.get(0);
//
//        foundDef.setEnabled(true);
//
//        AlertTemplateManagerLocal atm = LookupUtil.getAlertTemplateManager();
//
//        atm.updateAlertTemplate(subject, foundDef, true);
//
//        assertEquals("The notification validation method shouldn't have been called", 0, TestAlertSender.getValidateMethodCallCount());
//    }
//
//    @Test(dependsOnMethods = "testNoValidationWhenNoNotificationUpdateOnResourceLevel")
//    public void testCorrectSubjectPassedOnResourceLevelAlertDefinitionUpdate() throws Exception {
//        TestAlertSender.setExpectedSubject(subject);
//        TestAlertSender.resetValidateMethodCallCount();
//
//        AlertDefinitionManagerLocal adm = LookupUtil.getAlertDefinitionManager();
//
//        AlertDefinitionCriteria crit = new AlertDefinitionCriteria();
//        crit.addFilterId(resourceLevelAlertDefinitionId);
//        crit.fetchAlertNotifications(true);
//        crit.fetchConditions(true);
//
//        List<AlertDefinition> foundDefs = adm.findAlertDefinitionsByCriteria(subject, crit);
//
//        assertEquals("Failed to find the previously created resource level alert definition.", 1, foundDefs.size());
//
//        AlertDefinition foundDef = foundDefs.get(0);
//
//        AlertNotification newNotif = createAlertNotificationForTest(foundDef, false);
//        //just add some dummy config property so that the 2 notifs are distinguishable from each other
//        //and are saved separately
//        newNotif.getConfiguration().put(new PropertySimple("foo-resource", "bar"));
//
//        adm.updateAlertDefinition(subject, resourceLevelAlertDefinitionId, foundDef, false);
//
//        assertEquals("Validation should have been called for a new notification during alert def update", 1, TestAlertSender.getValidateMethodCallCount());
//    }
//
//    @Test(dependsOnMethods = "testCorrectSubjectPassedOnGroupLevelAlertDefinitionCreation")
//    public void testCorrectSubjectPassedOnGroupLevelAlertDefinitionUpdate() throws Exception {
//        TestAlertSender.setExpectedSubject(subject);
//        TestAlertSender.resetValidateMethodCallCount();
//
//        AlertDefinitionManagerLocal adm = LookupUtil.getAlertDefinitionManager();
//
//        AlertDefinitionCriteria crit = new AlertDefinitionCriteria();
//        crit.addFilterId(groupLevelAlertDefinitionId);
//        crit.fetchAlertNotifications(true);
//        crit.fetchConditions(true);
//
//        List<AlertDefinition> foundDefs = adm.findAlertDefinitionsByCriteria(subject, crit);
//
//        assertEquals("Failed to find the previously created group level alert definition.", 1, foundDefs.size());
//
//        AlertDefinition foundDef = foundDefs.get(0);
//
//        AlertNotification newNotif = createAlertNotificationForTest(foundDef, false);
//        //just add some dummy config property so that the 2 notifs are distinguishable from each other
//        //and are saved separately
//        newNotif.getConfiguration().put(new PropertySimple("foo-group", "bar"));
//
//        GroupAlertDefinitionManagerLocal gadm = LookupUtil.getGroupAlertDefinitionManager();
//        gadm.updateGroupAlertDefinitions(subject, foundDef, true);
//
//        //notice that the validation should be called just once, even though in effect we're creating 11 notifs
//        //1 for group and 10 for its members.
//        assertEquals("Validation should have been called for a new notification during alert def update", 1, TestAlertSender.getValidateMethodCallCount());
//    }
//
//    @Test(dependsOnMethods = "testCorrectSubjectPassedOnTemplateLevelAlertDefinitionCreation")
//    public void testCorrectSubjectPassedOnTemplateLevelAlertDefinitionUpdate() throws Exception {
//        TestAlertSender.setExpectedSubject(subject);
//        TestAlertSender.resetValidateMethodCallCount();
//
//        AlertDefinitionManagerLocal adm = LookupUtil.getAlertDefinitionManager();
//
//        AlertDefinitionCriteria crit = new AlertDefinitionCriteria();
//        crit.addFilterId(templateLevelAlertDefinitionId);
//        crit.fetchAlertNotifications(true);
//        crit.fetchConditions(true);
//
//        List<AlertDefinition> foundDefs = adm.findAlertDefinitionsByCriteria(subject, crit);
//
//        assertEquals("Failed to find the previously created template level alert definition.", 1, foundDefs.size());
//
//        AlertDefinition foundDef = foundDefs.get(0);
//
//        AlertNotification newNotif = createAlertNotificationForTest(foundDef, false);
//        //just add some dummy config property so that the 2 notifs are distinguishable from each other
//        //and are saved separately
//        newNotif.getConfiguration().put(new PropertySimple("foo-template", "bar"));
//
//        AlertTemplateManagerLocal atm = LookupUtil.getAlertTemplateManager();
//        atm.updateAlertTemplate(subject, foundDef, true);
//
//        //notice that the validation should be called just once, even though in effect we're creating 11 notifs
//        //1 for template and 10 for its members.
//        assertEquals("Validation should have been called for a new notification during alert def update", 1, TestAlertSender.getValidateMethodCallCount());
//    }
//
//    private void removeNoExceptions(final Object o) {
//        try {
//            executeInTransaction(new TransactionCallback() {
//                public void execute() {
//                    EntityManager em = getEntityManager();
//                    Object o2 = em.merge(o);
//
//                    if (o2 instanceof Resource) {
//                        ResourceTreeHelper.deleteResource(em, (Resource) o2);
//                    } else {
//                        em.remove(o2);
//                    }
//                }
//            });
//        } catch (Exception e) {
//            LOG.error("Failed to DELETE an object from database: " + o, e);
//        }
//    }
//
//    private AlertDefinition createDefinition(String name) {
//        AlertDefinition ret = new AlertDefinition();
//        ret.setName(name);
//        ret.setPriority(AlertPriority.MEDIUM);
//        ret.setAlertDampening(new AlertDampening(AlertDampening.Category.NONE));
//        ret.setConditionExpression(BooleanExpression.ANY);
//        ret.setRecoveryId(0);
//
//        return ret;
//    }
//
//    private AlertDefinition createDefinitionForTest(String name, boolean precanned) {
//        AlertDefinition def = createDefinition(name);
//        createAlertNotificationForTest(def, precanned);
//
//        return def;
//    }
//
//    private AlertNotification createAlertNotificationForTest(AlertDefinition alertDefinition, boolean precanned) {
//        AlertNotification notif = new AlertNotification("Test Alert Sender");
//
//        Configuration alertConfig = new Configuration();
//
//        if (precanned) {
//            alertConfig.put(new PropertySimple(TestAlertSender.PERSISTENT_PROPERTY_NAME, TestAlertSender.PERSISTEN_PROPERTY_EXPECTED_VALUE));
//        } else {
//            alertConfig.put(new PropertySimple(TestAlertSender.PERSISTENT_PROPERTY_NAME, "persistent"));
//            alertConfig.put(new PropertySimple(TestAlertSender.EPHEMERAL_PROPERTY_NAME, "ephemeral"));
//        }
//
//        Configuration extraConfig = new Configuration();
//
//        if (precanned) {
//            extraConfig.put(new PropertySimple(TestAlertSender.PERSISTENT_PROPERTY_NAME, TestAlertSender.PERSISTEN_PROPERTY_EXPECTED_VALUE));
//        } else {
//            extraConfig.put(new PropertySimple(TestAlertSender.PERSISTENT_PROPERTY_NAME, "persistent"));
//            extraConfig.put(new PropertySimple(TestAlertSender.EPHEMERAL_PROPERTY_NAME, "ephemeral"));
//        }
//
//        notif.setConfiguration(alertConfig);
//        notif.setExtraConfiguration(extraConfig);
//
//        alertDefinition.addAlertNotification(notif);
//        notif.setAlertDefinition(alertDefinition);
//
//        return notif;
//    }
//
//    private void testMainAlertDefinition(int id) {
//        AlertDefinitionCriteria crit = new AlertDefinitionCriteria();
//        crit.addFilterId(id);
//        crit.fetchAlertNotifications(true);
//
//        List<AlertDefinition> checkList =
//            LookupUtil.getAlertDefinitionManager().findAlertDefinitionsByCriteria(subject, crit);
//
//        assertNotNull("Failed to retrieve the save alert definition", checkList);
//        assertEquals("The alert definition should have been saved.", 1, checkList.size());
//
//        AlertDefinition check = checkList.get(0);
//
//        assertEquals("There should be exactly 1 notification on the definition", 1, check.getAlertNotifications()
//            .size());
//
//        Configuration config = check.getAlertNotifications().get(0).getConfiguration();
//        assertEquals("Unexpected persistent value in notif config", TestAlertSender.PERSISTEN_PROPERTY_EXPECTED_VALUE,
//            config.getSimpleValue(TestAlertSender.PERSISTENT_PROPERTY_NAME, null));
//        assertNull("Ephemeral property seems to have been saved",
//            config.getSimpleValue(TestAlertSender.EPHEMERAL_PROPERTY_NAME, null));
//    }
//
//    private List<AlertDefinition> testDependentAlertDefinitions(int expectedParentId, ParentType parentType) {
//        AlertDefinitionCriteria crit = new AlertDefinitionCriteria();
//
//        Set<Integer> resourceIds = new HashSet<Integer>(resources.size());
//        for (Resource r : resources) {
//            resourceIds.add(r.getId());
//        }
//
//        if (parentType == ParentType.TEMPLATE) {
//            crit.addFilterAlertTemplateParentId(expectedParentId);
//        } else if (parentType == ParentType.GROUP) {
//            crit.addFilterGroupAlertDefinitionId(expectedParentId);
//        }
//
//        crit.fetchAlertNotifications(true);
//
//        List<AlertDefinition> checkList =
//            LookupUtil.getAlertDefinitionManager().findAlertDefinitionsByCriteria(subject, crit);
//
//        assertNotNull("Failed to retrieve the save alert definition", checkList);
//        assertEquals("The dependent alert definitions should have been saved.", resources.size(), checkList.size());
//
//        for (AlertDefinition check : checkList) {
//            testSingleDependentAlertDefinition(check, parentType, expectedParentId);
//        }
//
//        return checkList;
//    }
//
//    private void testSingleDependentAlertDefinition(AlertDefinition alertDef, ParentType parentType, int expectedParentId) {
//        assertEquals("There should be exactly 1 notification on the definition " + alertDef, 1, alertDef
//            .getAlertNotifications().size());
//
//        Configuration config = alertDef.getAlertNotifications().get(0).getConfiguration();
//        assertEquals("Unexpected persistent value in notif config " + alertDef,
//            TestAlertSender.PERSISTEN_PROPERTY_EXPECTED_VALUE,
//            config.getSimpleValue(TestAlertSender.PERSISTENT_PROPERTY_NAME, null));
//        assertNull("Ephemeral property seems to have been saved " + alertDef,
//            config.getSimpleValue(TestAlertSender.EPHEMERAL_PROPERTY_NAME, null));
//
//        if (parentType == ParentType.GROUP) {
//            assertEquals("The group parent id has unexpected value", expectedParentId, alertDef.getGroupAlertDefinition().getId());
//        } else if (parentType == ParentType.TEMPLATE) {
//            assertEquals("The parent id has unexpected value", Integer.valueOf(expectedParentId), alertDef.getParentId());
//        }
//    }
//
//    private Resource createResourceForTest(String resourceKey) {
//        Resource res = new ResourceBuilder().createPlatform().withRandomUuid().withResourceKey(resourceKey)
//            .withResourceType(resourceType).withName(resourceKey)
//            .withInventoryStatus(InventoryStatus.COMMITTED).build();
//        res.setAgent(agent);
//
//        return res;
//    }
//}
