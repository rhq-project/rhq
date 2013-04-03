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

package org.rhq.enterprise.server.alert.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.Test;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertDampening;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.alert.BooleanExpression;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerBeanTest;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
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
public class AlertManagerTest extends AbstractEJB3Test {

    private static final Log LOG = LogFactory.getLog(AlertDefinitionManagerBeanTest.class);

    private AlertManagerLocal alertManager;

    private AlertDefinitionManagerLocal alertDefinitionManager;

    private ResourceGroupManagerLocal resourceGroupManager;

    private RoleManagerLocal roleManager;

    private SubjectManagerLocal subjectManager;

    private TestData testData;

    @Override
    protected void beforeMethod() throws Exception {
        alertManager = LookupUtil.getAlertManager();
        subjectManager = LookupUtil.getSubjectManager();
        resourceGroupManager = LookupUtil.getResourceGroupManager();
        roleManager = LookupUtil.getRoleManager();
        alertDefinitionManager = LookupUtil.getAlertDefinitionManager();
        createTestData();
        prepareScheduler();
    }

    private void createTestData() {
        testData = executeInTransaction(false, new TransactionCallbackReturnable<TestData>() {
            @Override
            public TestData execute() throws Exception {
                TestData newTestData = new TestData();
                Subject subject = SessionTestHelper.createNewSubject(em, "fake subject");
                newTestData.setSubject(subject);
                Role role = SessionTestHelper.createNewRoleForSubject(em, subject, "fake role", Permission.values());
                newTestData.setRole(role);
                ResourceType resourceType = SessionTestHelper.createNewResourceType(em);
                newTestData.setResourceType(resourceType);
                ResourceGroup resourceGroup = new ResourceGroup("fake group", resourceType);
                resourceGroup = resourceGroupManager.createPrivateResourceGroup(subject, resourceGroup);
                newTestData.setResourceGroup(resourceGroup);
                roleManager.setAssignedResourceGroups(subjectManager.getOverlord(), role.getId(),
                    new int[] { resourceGroup.getId() });
                Resource resource = SessionTestHelper.createNewResourceForGroup(em, resourceGroup, "fake resource");
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
            removeAlerts();
            removeEntity(AlertDefinition.class, testData.getAlertDefinitionId());
            removeEntity(ResourceGroup.class, testData.getResourceGroup().getId());
            removeEntity(Resource.class, testData.getResource().getId());
            removeEntity(ResourceType.class, testData.getResourceType().getId());
            removeEntity(Subject.class, testData.getSubject().getId());
            removeEntity(Role.class, testData.getRole().getId());
            testData = null;
        }
    }

    private void removeAlerts() {
        try {
            executeInTransaction(false, new TransactionCallback() {
                @Override
                public void execute() throws Exception {
                    alertManager.deleteAlertsByContext(subjectManager.getOverlord(), EntityContext.forSubsystemView());
                }
            });
        } catch (Exception e) {
            LOG.error("Failed to remove alerts", e);
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
                    }
                }
            });
        } catch (Exception e) {
            LOG.error("Failed to delete object from database: " + entityClass + "[id=" + entityId + "]", e);
        }
    }

    @Test
    public void testDeleteAlertsByResourceContext() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                int resourceAlertDefinitionId = createResourceAlertDefinitionAndGetId("fake resource alertdef");
                alertManager.fireAlert(resourceAlertDefinitionId);

                PageList<Alert> foundAlerts = findAlertsByDefinitionIds(resourceAlertDefinitionId);
                assertEquals(1, foundAlerts.size());

                alertManager.deleteAlertsByContext(testData.getSubject(),
                    EntityContext.forResource(testData.getResource().getId()));
                foundAlerts = findAlertsByDefinitionIds();
                assertEquals(0, foundAlerts.size());
            }
        });
    }

    @Test
    public void testDeleteAlertsByGroupContext() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                int resourceAlertDefinitionId = createResourceAlertDefinitionAndGetId("fake resource alertdef");
                alertManager.fireAlert(resourceAlertDefinitionId);

                PageList<Alert> foundAlerts = findAlertsByDefinitionIds(resourceAlertDefinitionId);
                assertEquals(1, foundAlerts.size());

                alertManager.deleteAlertsByContext(testData.getSubject(),
                    EntityContext.forGroup(testData.getResourceGroup().getId()));
                foundAlerts = findAlertsByDefinitionIds(resourceAlertDefinitionId);
                assertEquals(0, foundAlerts.size());
            }
        });
    }

    @Test
    public void testAcknowledgeAlertsByTemplateContext() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                try {
                    // Template definition alert id could be anything
                    alertManager.acknowledgeAlertsByContext(testData.getSubject(), EntityContext.forTemplate(42));
                    fail("It should not be possible to acknowledge alerts by template context");
                } catch (Exception e) {
                    assertEquals(e.getCause().getClass(), IllegalArgumentException.class);
                }
            }
        });
    }

    @Test
    public void testAcknowledgeAlertsByResourceContext() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                int resourceAlertDefinitionId = createResourceAlertDefinitionAndGetId("fake resource alertdef");
                alertManager.fireAlert(resourceAlertDefinitionId);

                long testStartTimeStamp = System.currentTimeMillis();
                int count = alertManager.acknowledgeAlertsByContext(testData.getSubject(),
                    EntityContext.forResource(testData.getResource().getId()));
                assertEquals(1, count);

                PageList<Alert> foundAlerts = findAlertsByDefinitionIds(resourceAlertDefinitionId);
                assertEquals(1, foundAlerts.size());
                Alert alert = foundAlerts.iterator().next();
                em.refresh(alert);

                assertNotNull(alert.getAcknowledgingSubject());
                assertNotNull(alert.getAcknowledgeTime());
                assertEquals(testData.getSubject().getName(), alert.getAcknowledgingSubject());
                assertTrue("Alert should just have been acknowledged", alert.getAcknowledgeTime() >= testStartTimeStamp);
            }
        });
    }

    @Test
    public void testAcknowledgeAlertsByGroupContext() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                int resourceAlertDefinitionId = createResourceAlertDefinitionAndGetId("fake resource alertdef");
                alertManager.fireAlert(resourceAlertDefinitionId);

                long testStartTimeStamp = System.currentTimeMillis();
                int count = alertManager.acknowledgeAlertsByContext(testData.getSubject(),
                    EntityContext.forGroup(testData.getResourceGroup().getId()));
                assertEquals(1, count);

                PageList<Alert> foundAlerts = findAlertsByDefinitionIds(resourceAlertDefinitionId);
                assertEquals(1, foundAlerts.size());
                Alert alert = foundAlerts.iterator().next();
                em.refresh(alert);

                assertNotNull(alert.getAcknowledgingSubject());
                assertNotNull(alert.getAcknowledgeTime());
                assertEquals(testData.getSubject().getName(), alert.getAcknowledgingSubject());
                assertTrue("Alert should just have been acknowledged", alert.getAcknowledgeTime() >= testStartTimeStamp);
            }
        });
    }

    @Test
    public void testAcknowledgeAlertsBySubsystemContext() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                int resourceAlertDefinitionId = createResourceAlertDefinitionAndGetId("fake resource alertdef");
                alertManager.fireAlert(resourceAlertDefinitionId);

                long testStartTimeStamp = System.currentTimeMillis();
                int count = alertManager.acknowledgeAlertsByContext(testData.getSubject(),
                    EntityContext.forSubsystemView());
                assertEquals(1, count);

                PageList<Alert> foundAlerts = findAlertsByDefinitionIds(resourceAlertDefinitionId);
                assertEquals(1, foundAlerts.size());
                Alert alert = foundAlerts.iterator().next();
                em.refresh(alert);

                assertNotNull(alert.getAcknowledgingSubject());
                assertNotNull(alert.getAcknowledgeTime());
                assertEquals(testData.getSubject().getName(), alert.getAcknowledgingSubject());
                assertTrue("Alert should just have been acknowledged", alert.getAcknowledgeTime() >= testStartTimeStamp);
            }
        });
    }

    private PageList<Alert> findAlertsByDefinitionIds(Integer... alertDefinitionIds) {
        AlertCriteria criteria = new AlertCriteria();
        criteria.addFilterAlertDefinitionIds(alertDefinitionIds);
        return alertManager.findAlertsByCriteria(testData.getSubject(), criteria);
    }

    private int createResourceAlertDefinitionAndGetId(String name) {
        AlertDefinition alertDefinition = new AlertDefinition();
        alertDefinition.setName(name);
        alertDefinition.setPriority(AlertPriority.MEDIUM);
        alertDefinition.setAlertDampening(new AlertDampening(AlertDampening.Category.NONE));
        alertDefinition.setConditionExpression(BooleanExpression.ANY);
        alertDefinition.setRecoveryId(0);
        alertDefinition.setEnabled(true);
        int alertDefinitionId = alertDefinitionManager.createAlertDefinition(testData.getSubject(), alertDefinition,
            testData.getResource().getId(), true);
        testData.setAlertDefinitionId(alertDefinitionId);
        return alertDefinitionId;
    }

    private static final class TestData {

        private Subject subject;

        private Role role;

        private ResourceType resourceType;

        private ResourceGroup resourceGroup;

        private Resource resource;

        private int alertDefinitionId;

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

        public int getAlertDefinitionId() {
            return alertDefinitionId;
        }

        public void setAlertDefinitionId(int alertDefinitionId) {
            this.alertDefinitionId = alertDefinitionId;
        }
    }

}
