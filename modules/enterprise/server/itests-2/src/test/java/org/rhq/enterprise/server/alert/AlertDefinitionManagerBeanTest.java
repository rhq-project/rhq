/*
 * RHQ Management Platform
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.enterprise.server.alert;

import static org.rhq.core.domain.measurement.DataType.MEASUREMENT;
import static org.rhq.core.domain.measurement.NumericType.DYNAMIC;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.Test;

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.AlertConditionOperator;
import org.rhq.core.domain.alert.AlertDampening;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.alert.BooleanExpression;
import org.rhq.core.domain.alert.builder.AlertDefinitionTemplate;
import org.rhq.core.domain.alert.builder.condition.AbsoluteValueCondition;
import org.rhq.core.domain.alert.builder.condition.AvailabilityCondition;
import org.rhq.core.domain.alert.builder.condition.DriftCondition;
import org.rhq.core.domain.alert.builder.condition.ResourceConfigurationCondition;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.criteria.AlertDefinitionCriteria;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.collection.ArrayUtils;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.RoleManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TransactionCallback;
import org.rhq.enterprise.server.test.TransactionCallbackReturnable;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.ResourceTreeHelper;
import org.rhq.enterprise.server.util.SessionTestHelper;

/**
 * @author Thomas Segismont
 */
public class AlertDefinitionManagerBeanTest extends AbstractEJB3Test {

    private static final Log LOG = LogFactory.getLog(AlertDefinitionManagerBeanTest.class);

    final private String prefix = this.getClass().getSimpleName() + "_";
    final private String subjectName = prefix + "subject";
    final private String roleName = prefix + "role";
    final private String groupName = prefix + "group";
    final private String resourceName = prefix + "resource";
    final private String measurementDefName = prefix + "measurement";

    private AlertDefinitionManagerLocal alertDefinitionManager;

    private ResourceGroupManagerLocal resourceGroupManager;

    private RoleManagerLocal roleManager;

    private SubjectManagerLocal subjectManager;

    private TestData testData;

    @Override
    protected void beforeMethod() throws Exception {
        alertDefinitionManager = LookupUtil.getAlertDefinitionManager();
        subjectManager = LookupUtil.getSubjectManager();
        resourceGroupManager = LookupUtil.getResourceGroupManager();
        roleManager = LookupUtil.getRoleManager();
        createTestData();
        prepareScheduler();
    }

    private void createTestData() {
        testData = executeInTransaction(false, new TransactionCallbackReturnable<TestData>() {
            @Override
            public TestData execute() throws Exception {

                TestData newTestData = new TestData();
                Subject subject = SessionTestHelper.createNewSubject(em, subjectName);
                newTestData.setSubject(subject);
                Role role = SessionTestHelper.createNewRoleForSubject(em, subject, roleName, Permission.MANAGE_ALERTS);
                newTestData.setRole(role);
                ResourceType resourceType = SessionTestHelper.createNewResourceType(em);

                // To test bug 949048 we need a metric on the type
                MeasurementDefinition dynamicMeasuremenDef = new MeasurementDefinition(resourceType, measurementDefName);
                dynamicMeasuremenDef.setDefaultOn(true);
                dynamicMeasuremenDef.setDataType(MEASUREMENT);
                dynamicMeasuremenDef.setMeasurementType(DYNAMIC);
                em.persist(dynamicMeasuremenDef);

                newTestData.setResourceType(resourceType);
                ResourceGroup resourceGroup = new ResourceGroup(groupName, resourceType);
                resourceGroup = resourceGroupManager.createPrivateResourceGroup(subject, resourceGroup);
                newTestData.setResourceGroup(resourceGroup);
                roleManager.setAssignedResourceGroups(subjectManager.getOverlord(), role.getId(),
                    new int[] { resourceGroup.getId() });
                Resource resource = SessionTestHelper.createNewResourceForGroup(em, resourceGroup, resourceName);
                newTestData.setResource(resource);
                return newTestData;
            }
        });
    }

    @Override
    protected void afterMethod() throws Exception {
        try {
            deleteTestData();
        } finally {
            unprepareScheduler();
        }
    }

    private void deleteTestData() throws Exception {
        if (testData != null) {
            // use the SLSB here, not just EM
            resourceGroupManager.deleteResourceGroup(subjectManager.getOverlord(), testData.getResourceGroup().getId());

            for (Integer alertDefinitionId : testData.getAlertDefinitionIds()) {
                removeEntity(AlertDefinition.class, alertDefinitionId);
            }

            removeEntity(Resource.class, testData.getResource().getId());
            removeEntity(ResourceType.class, testData.getResourceType().getId());

            subjectManager.deleteSubjects(subjectManager.getOverlord(), new int[] { testData.getSubject().getId() });
            roleManager.deleteRoles(subjectManager.getOverlord(), new int[] { testData.getRole().getId() });

            testData = null;
        }
    }

    private void removeEntity(final Class<?> entityClass, final Object entityId) {
        try {
            executeInTransaction(false, new TransactionCallback() {
                @Override
                public void execute() throws Exception {
                    Object object = em.find(entityClass, entityId);
                    if (object instanceof Resource) {
                        ResourceTreeHelper.deleteResource(em, (Resource) object);
                    } else {
                        em.remove(object);
                        em.flush();
                    }
                }
            });
        } catch (Exception e) {
            LOG.error("Failed to delete object from database: " + entityClass + "[id=" + entityId + "]", e);
        }
    }

    @Test
    public void testEnableAlertDefinitions() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                List<Integer> alertDefinitionIds = new LinkedList<Integer>();
                for (int i = 0; i < 50; i++) {
                    alertDefinitionIds.add(createAlertDefinitionAndGetId(prefix + "alertdef_" + String.valueOf(i),
                        false));
                }
                List<Integer> alertDefinitionToEnableIds = alertDefinitionIds.subList(12, 37);
                int enabledCount = alertDefinitionManager.enableAlertDefinitions(testData.getSubject(),
                    ArrayUtils.unwrapCollection(alertDefinitionToEnableIds));
                assertEquals(alertDefinitionToEnableIds.size(), enabledCount);
            }
        });
    }

    @Test
    public void testDisableAlertDefinitions() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                List<Integer> alertDefinitionIds = new LinkedList<Integer>();
                for (int i = 0; i < 50; i++) {
                    alertDefinitionIds
                        .add(createAlertDefinitionAndGetId(prefix + "alertdef_" + String.valueOf(i), true));
                }
                List<Integer> alertDefinitionToDisableIds = alertDefinitionIds.subList(17, 48);
                int disabledCount = alertDefinitionManager.disableAlertDefinitions(testData.getSubject(),
                    ArrayUtils.unwrapCollection(alertDefinitionToDisableIds));
                assertEquals(alertDefinitionToDisableIds.size(), disabledCount);
            }
        });
    }

    @Test
    public void testIsResourceOrGroupAlertDefinition() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                int resourceAlertDefinitionId = createAlertDefinitionAndGetId(prefix + "resource_Alertdef", true);
                int groupAlertDefinitionId = createGroupAlertDefinitionAndGetId(prefix + "group_Alertdef");
                assertTrue("Failed to detect a group alert definition",
                    alertDefinitionManager.isGroupAlertDefinition(groupAlertDefinitionId));
                assertFalse("Should not have detected a group alert definition",
                    alertDefinitionManager.isGroupAlertDefinition(resourceAlertDefinitionId));
                assertTrue("Failed to detect a resource alert definition",
                    alertDefinitionManager.isResourceAlertDefinition(resourceAlertDefinitionId));
                assertFalse("Should not have detected a resource alert definition",
                    alertDefinitionManager.isResourceAlertDefinition(groupAlertDefinitionId));
            }
        });
    }

    @Test
    void testBug846451() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                String name = prefix + "group_Alertdef";
                int groupAlertDefinitionId = createGroupAlertDefinitionAndGetId(name);

                AlertDefinitionCriteria criteria = new AlertDefinitionCriteria();
                criteria.addFilterResourceGroupIds(testData.getResourceGroup().getId());

                // tests the reported bug
                List<AlertDefinition> result = alertDefinitionManager.findAlertDefinitionsByCriteria(
                    testData.getSubject(), criteria);
                assertNotNull(result);
                assertEquals(1, result.size());
                assertEquals(name, result.get(0).getName());
            }
        });
    }

    @Test
    void testAddRemoveGroupMembers() throws Exception {

        Resource resource2 = null;
        Resource resource3 = null;

        try {

            // creating alert definitions is performed in a new transaction. So, any involved entities must already be
            // committed.  Commit the additional test entities here. This stuff will get cleaned up in afterMethod            
            startTransaction();

            String name = prefix + "group_Alertdef";
            int groupAlertDefinitionId = createGroupAlertDefinitionAndGetId(name);

            String resource2Name = resourceName + "_2";
            String resource3Name = resourceName + "_3";
            resource2 = SessionTestHelper.createNewResource(em, resource2Name, testData.getResourceType());
            resource3 = SessionTestHelper.createNewResource(em, resource3Name, testData.getResourceType());

            commitTransaction();

            // add new members and ensure the resource-level alert defs gets applied. Make sure to use the SLSB to add it
            LookupUtil.getResourceGroupManager().addResourcesToGroup(subjectManager.getOverlord(),
                testData.getResourceGroup().getId(), new int[] { resource2.getId() });
            LookupUtil.getResourceGroupManager().addResourcesToGroup(subjectManager.getOverlord(),
                testData.getResourceGroup().getId(), new int[] { resource3.getId() });

            int[] ids = new int[] { resource2.getId(), resource3.getId() };
            AlertDefinitionCriteria criteria = new AlertDefinitionCriteria();
            criteria.fetchConditions(true);
            criteria.addFilterResourceIds(ids[0], ids[1]);

            List<AlertDefinition> result = alertDefinitionManager.findAlertDefinitionsByCriteria(testData.getSubject(),
                criteria);
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals(name, result.get(0).getName());
            assertEquals(name, result.get(1).getName());
            assertTrue(result.get(0).getId() != result.get(1).getId());
            assertEquals(1, result.get(0).getConditions().size());
            assertEquals(1, result.get(1).getConditions().size());

            // remove one member and ensure the alert def gets removed and no other members are affected (bug 949062)
            LookupUtil.getResourceGroupManager().removeResourcesFromGroup(subjectManager.getOverlord(),
                testData.getResourceGroup().getId(), new int[] { resource2.getId() });

            result = alertDefinitionManager.findAlertDefinitionsByCriteria(testData.getSubject(), criteria);
            assertNotNull(result);
            assertEquals(1, result.size());

            // now the other one
            LookupUtil.getResourceGroupManager().removeResourcesFromGroup(subjectManager.getOverlord(),
                testData.getResourceGroup().getId(), new int[] { resource3.getId() });
            result = alertDefinitionManager.findAlertDefinitionsByCriteria(testData.getSubject(), criteria);
            assertNotNull(result);
            assertEquals(0, result.size());

        } finally {
            Resource[] resources = new Resource[] { resource2, resource3 };
            try {
                startTransaction();
                for (Resource r : resources) {
                    if (null != r) {

                        r = em.find(Resource.class, r.getId());
                        Set<AlertDefinition> ads = r.getAlertDefinitions();
                        for (AlertDefinition ad : ads) {
                            em.remove(ad);
                        }
                        ResourceTreeHelper.deleteResource(em, r);
                    }
                }
                commitTransaction();

            } catch (Exception e) {
                rollbackTransaction();
            }
        }
    }

    private int createAlertDefinitionAndGetId(String name, boolean enabled) {
        AlertDefinition alertDefinition = new AlertDefinition();
        alertDefinition.setName(name);
        alertDefinition.setPriority(AlertPriority.MEDIUM);
        alertDefinition.setAlertDampening(new AlertDampening(AlertDampening.Category.NONE));
        alertDefinition.setConditionExpression(BooleanExpression.ANY);
        alertDefinition.setRecoveryId(0);
        alertDefinition.setEnabled(enabled);
        alertDefinition = alertDefinitionManager.createAlertDefinitionInNewTransaction(testData.getSubject(),
            alertDefinition, testData.getResource().getId(), true);
        testData.getAlertDefinitionIds().add(alertDefinition.getId());
        return alertDefinition.getId();
    }

    private int createGroupAlertDefinitionAndGetId(String name) {
        AlertDefinition alertDefinition = new AlertDefinition();
        alertDefinition.setName(name);
        alertDefinition.setPriority(AlertPriority.MEDIUM);
        alertDefinition.setAlertDampening(new AlertDampening(AlertDampening.Category.NONE));
        alertDefinition.setConditionExpression(BooleanExpression.ANY);
        alertDefinition.setRecoveryId(0);
        alertDefinition.setGroup(testData.getResourceGroup());
        alertDefinition.setEnabled(true);

        // We need a threshold alert condition to recreate the issue in bug 949048 
        AlertCondition ac = new AlertCondition();
        ac.setCategory(AlertConditionCategory.THRESHOLD);
        ac.setMeasurementDefinition(testData.getMeasurementDef());
        ac.setComparator("<");
        ac.setThreshold(0.5);
        alertDefinition.addCondition(ac);

        AlertDefinition newAlertDefinition = alertDefinitionManager.createAlertDefinitionInNewTransaction(
            testData.getSubject(), alertDefinition, null, true);
        testData.getAlertDefinitionIds().add(newAlertDefinition.getId());
        return newAlertDefinition.getId();
    }

    private AlertDefinitionTemplate createAlertTemplate() {
        AlertDefinitionTemplate template = new AlertDefinitionTemplate(1)
                .enabled(true)
                .description("description")
                .name("name")
                .priority(AlertPriority.MEDIUM)
                .alertProtocol(BooleanExpression.ANY);

        // Set recovery rules
        template.recovery()
                .disableWhenFired(false);

        // Set dampening rules
        template.dampening()
                .category(AlertDampening.Category.CONSECUTIVE_COUNT)
                .occurences(2);

        // Create and add conditions
        AvailabilityCondition availabilityCondition = new AvailabilityCondition()
                .availability(AlertConditionOperator.AVAIL_GOES_DOWN);

        DriftCondition driftCondition = new DriftCondition()
                .expression(".*")
                .name(".*");

        ResourceConfigurationCondition resourceConfigurationCondition = new ResourceConfigurationCondition();

        AbsoluteValueCondition absoluteValueCondition = new AbsoluteValueCondition()
                .metric(testData.getMeasurementDef().getId())
                .value(0.5)
                .comparator(AlertConditionOperator.LESS_THAN);

        template.addCondition(availabilityCondition)
                .addCondition(driftCondition)
                .addCondition(resourceConfigurationCondition)
                .addCondition(absoluteValueCondition);

        return template;
    }

    @Test
    public void testTemplateToDefinition() {
        AlertDefinitionTemplate alertTemplate = createAlertTemplate();
        AlertDefinition alertDefinition = alertTemplate.getAlertDefinition();
        assertNotNull(alertDefinition.getEnabled());
    }

    @Test
    public void testAlertTemplate() {

        AlertDefinitionTemplate template = createAlertTemplate();

        // Store
        AlertDefinition alertDefinitionFromTemplate = alertDefinitionManager.createAlertDefinitionFromTemplate(testData.getSubject(), template);

        assertNotNull(alertDefinitionFromTemplate.getId());

        // Check conditions are stored correctly
        assertEquals(4, alertDefinitionFromTemplate.getConditions().size());

        for (AlertCondition alertCondition : alertDefinitionFromTemplate.getConditions()) {
            switch (alertCondition.getCategory()) {
                case DRIFT:
                    assertEquals(".*", alertCondition.getName());
                    assertEquals(".*", alertCondition.getOption());
                    break;
                case THRESHOLD:
                    assertEquals("<", alertCondition.getComparator());
                    assertEquals(0.5, alertCondition.getThreshold());
                    assertEquals(testData.getMeasurementDef().getId(), alertCondition.getMeasurementDefinition().getId());
                    break;
                case AVAILABILITY:
                    assertEquals("AVAIL_GOES_DOWN", alertCondition.getOption());
                    break;
                case RESOURCE_CONFIG:
                    assertEquals("", alertCondition.getOption());
                    break;
                default:
                    fail("We did not store anything else than those defined earlier");
            }
        }
    }

    private static final class TestData {

        private Subject subject;

        private Role role;

        private ResourceType resourceType;

        private MeasurementDefinition measurementDef;

        private ResourceGroup resourceGroup;

        private Resource resource;

        private List<Integer> alertDefinitionIds = new LinkedList<Integer>();

        public Subject getSubject() {
            return subject;
        }

        public void setSubject(Subject subject) {
            this.subject = subject;
        }

        public Role getRole() {
            return role;
        }

        public void setRole(Role role) {
            this.role = role;
        }

        public ResourceType getResourceType() {
            return resourceType;
        }

        public void setResourceType(ResourceType resourceType) {
            this.resourceType = resourceType;
        }

        public MeasurementDefinition getMeasurementDef() {
            return measurementDef;
        }

        public void setMeasurementDef(MeasurementDefinition measurementDef) {
            this.measurementDef = measurementDef;
        }

        public ResourceGroup getResourceGroup() {
            return resourceGroup;
        }

        public void setResourceGroup(ResourceGroup resourceGroup) {
            this.resourceGroup = resourceGroup;
        }

        public Resource getResource() {
            return resource;
        }

        public void setResource(Resource resource) {
            this.resource = resource;
        }

        public List<Integer> getAlertDefinitionIds() {
            return alertDefinitionIds;
        }
    }

}
