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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.TypedQuery;

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
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.criteria.AlertDefinitionCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
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
@Test(groups = "alert")
public class GroupAlertDefinitionManagerBeanTest extends AbstractEJB3Test {
    private static final Log LOG = LogFactory.getLog(GroupAlertDefinitionManagerBeanTest.class);
    public static final int NUMBER_OF_RESOURCES_TO_CREATE = 3753;

    final private String prefix = this.getClass().getSimpleName() + "_";
    final private String subjectName = prefix + "subject";
    final private String roleName = prefix + "role";
    final private String groupName = prefix + "group";
    final private String resourceName = prefix + "resource";

    private SubjectManagerLocal subjectManager;
    private RoleManagerLocal roleManager;
    private ResourceGroupManagerLocal resourceGroupManager;
    private GroupAlertDefinitionManagerLocal groupAlertDefinitionManager;
    private AlertDefinitionManagerLocal alertDefinitionManager;
    private TestData testData;

    @Override
    protected void beforeMethod() throws Exception {
        subjectManager = LookupUtil.getSubjectManager();
        roleManager = LookupUtil.getRoleManager();
        resourceGroupManager = LookupUtil.getResourceGroupManager();
        groupAlertDefinitionManager = LookupUtil.getGroupAlertDefinitionManager();
        alertDefinitionManager = LookupUtil.getAlertDefinitionManager();
        createTestData();
        prepareScheduler();
    }

    /**
     * Bug 738799 - deleting a group alertdef containing >1000 member alertdefs fails with
     * "SQLException: ORA-01795: maximum number of expressions in a list is 1000" error
     * https://bugzilla.redhat.com/show_bug.cgi?id=738799
     * (jshaughn 6/16/2014): make sure this works when called through the alert def remote, which now calls
     * into the local.
     */
    public void testBug738799() {
        List<Integer> childDefIds = getChildrenAlertDefinitionIds(testData.getGroupAlertDefinitionId());
        assertEquals(testData.getResources().size(), childDefIds.size());
        int modified = alertDefinitionManager.removeAlertDefinitions(testData.getSubject(),
            new int[] { testData.getGroupAlertDefinitionId() });
        assertEquals(modified, 1);
        assertEquals(0, getChildrenAlertDefinitionIds(testData.getGroupAlertDefinitionId()).size());
        AlertDefinitionCriteria criteria = new AlertDefinitionCriteria();
        criteria.addFilterIds(childDefIds.toArray(new Integer[childDefIds.size()]));
        List<AlertDefinition> remainingChildDefs = alertDefinitionManager.findAlertDefinitionsByCriteria(
            testData.getSubject(), criteria);
        assertEquals(0, remainingChildDefs.size());
    }

    @Override
    protected void afterMethod() throws Exception {
        try {
            deleteTestData();
        } finally {
            testData = null;
            unprepareScheduler();
        }
    }

    private void createTestData() throws Exception {
        testData = executeInTransaction(false, new TransactionCallbackReturnable<TestData>() {
            @Override
            public TestData execute() throws Exception {
                TestData newTestData = new TestData();
                Subject subject = SessionTestHelper.createNewSubject(em, subjectName);
                newTestData.setSubject(subject);
                Role role = SessionTestHelper.createNewRoleForSubject(em, subject, roleName, Permission.MANAGE_ALERTS,
                    Permission.MANAGE_SETTINGS, Permission.MANAGE_INVENTORY);
                newTestData.setRole(role);
                ResourceType resourceType = SessionTestHelper.createNewResourceType(em);
                newTestData.setResourceType(resourceType);
                ResourceGroup resourceGroup = new ResourceGroup(groupName, resourceType);
                resourceGroup = resourceGroupManager.createResourceGroup(subject, resourceGroup);
                newTestData.setResourceGroup(resourceGroup);
                roleManager.setAssignedResourceGroups(subjectManager.getOverlord(), role.getId(),
                    new int[] { resourceGroup.getId() });
                return newTestData;
            }
        });
        testData.setResources(new ArrayList<Resource>(NUMBER_OF_RESOURCES_TO_CREATE));
        while (testData.getResources().size() < NUMBER_OF_RESOURCES_TO_CREATE) {
            executeInTransaction(false, new TransactionCallback() {
                // Create resources in batches to avoid too big transactions
                @Override
                public void execute() throws Exception {
                    for (int i = 0; i < 50 && testData.getResources().size() < NUMBER_OF_RESOURCES_TO_CREATE; i++) {
                        testData.getResources().add(
                            SessionTestHelper.createNewResource(em, resourceName, testData.getResourceType()));
                    }
                }
            });
            if (LOG.isDebugEnabled()) {
                LOG.debug(testData.getResources().size() + " resources created");
            }
        }
        int[] resourceIds = new int[testData.getResources().size()];
        for (int i = 0; i < testData.getResources().size(); i++) {
            resourceIds[i] = testData.getResources().get(i).getId();
        }
        resourceGroupManager.setAssignedResources(testData.getSubject(), testData.getResourceGroup().getId(),
            resourceIds, true);
        testData.setGroupAlertDefinitionId(createGroupAlertDefinition(testData.getSubject(),
            testData.getResourceGroup()).getId());
    }

    private AlertDefinition createGroupAlertDefinition(Subject subject, ResourceGroup resourceGroup) {
        AlertDefinition alertDefinition = new AlertDefinition();
        alertDefinition.setName(AlertConditionOperator.AVAIL_GOES_UP.name());
        alertDefinition.setPriority(AlertPriority.MEDIUM);
        alertDefinition.setAlertDampening(new AlertDampening(AlertDampening.Category.NONE));
        alertDefinition.setConditionExpression(BooleanExpression.ANY);
        alertDefinition.setRecoveryId(0);
        alertDefinition.setGroup(resourceGroup);
        alertDefinition.setEnabled(true);
        AlertCondition alertCondition = new AlertCondition(alertDefinition, AlertConditionCategory.AVAILABILITY);
        alertCondition.setName(AlertConditionOperator.AVAIL_GOES_UP.name());
        alertDefinition.addCondition(alertCondition);
        groupAlertDefinitionManager.createGroupAlertDefinitions(subject, alertDefinition, resourceGroup.getId());
        return alertDefinition;
    }

    private void deleteTestData() throws Exception {
        if (testData != null) {
            groupAlertDefinitionManager.removeGroupAlertDefinitions(subjectManager.getOverlord(),
                new Integer[] { testData.getGroupAlertDefinitionId() });
            resourceGroupManager.deleteResourceGroup(subjectManager.getOverlord(), testData.getResourceGroup().getId());
            alertDefinitionManager.purgeUnusedAlertDefinitions();
            for (Resource resource : testData.getResources()) {
                removeEntity(Resource.class, resource.getId());
            }
            removeEntity(ResourceType.class, testData.getResourceType().getId());
            subjectManager.deleteSubjects(subjectManager.getOverlord(), new int[] { testData.getSubject().getId() });
            roleManager.deleteRoles(subjectManager.getOverlord(), new int[] { testData.getRole().getId() });
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

    private List<Integer> getChildrenAlertDefinitionIds(final int groupAlertDefinitionId) {
        return executeInTransaction(false, new TransactionCallbackReturnable<List<Integer>>() {
            @Override
            public List<Integer> execute() throws Exception {
                TypedQuery<Integer> query = getEntityManager().createNamedQuery(
                    AlertDefinition.QUERY_FIND_BY_GROUP_ALERT_DEFINITION_ID, Integer.class);
                query.setParameter("groupAlertDefinitionId", groupAlertDefinitionId);
                return query.getResultList();
            }
        });
    }

    private static final class TestData {
        private Subject subject;
        private Role role;
        private ResourceType resourceType;
        private ResourceGroup resourceGroup;
        private List<Resource> resources;
        private int groupAlertDefinitionId;

        private Subject getSubject() {
            return subject;
        }

        private void setSubject(Subject subject) {
            this.subject = subject;
        }

        private Role getRole() {
            return role;
        }

        private void setRole(Role role) {
            this.role = role;
        }

        private ResourceType getResourceType() {
            return resourceType;
        }

        private void setResourceType(ResourceType resourceType) {
            this.resourceType = resourceType;
        }

        private ResourceGroup getResourceGroup() {
            return resourceGroup;
        }

        private void setResourceGroup(ResourceGroup resourceGroup) {
            this.resourceGroup = resourceGroup;
        }

        private List<Resource> getResources() {
            return resources;
        }

        private void setResources(List<Resource> resources) {
            this.resources = resources;
        }

        private int getGroupAlertDefinitionId() {
            return groupAlertDefinitionId;
        }

        private void setGroupAlertDefinitionId(int groupAlertDefinitionId) {
            this.groupAlertDefinitionId = groupAlertDefinitionId;
        }
    }
}
