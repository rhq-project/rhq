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
package org.rhq.enterprise.server.alert.engine.test;

import java.util.Set;
import javax.persistence.EntityManager;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.AlertConditionLog;
import org.rhq.core.domain.alert.AlertDampening;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.BooleanExpression;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.alert.engine.jms.CachedConditionProducerBean;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

/*
 * This is not actually deployed as a test since it was deemed more of a test of the embedded configuration to support
 * MDBs than any largely meaningful or complex logic.  For now, since it was written, it's being checked in.
 */
// @Test
public class ActiveConditionProducerConsumerTest extends AbstractEJB3Test {
    CachedConditionProducerBean producerBean;

    Resource newResource;
    AlertDefinition newAlertDefinition;
    AlertCondition newAlertCondition;

    @BeforeClass
    public void beforeClass() {
        producerBean = LookupUtil.getActiveConditionProducer();
    }

    @BeforeMethod
    public void beforeMethod() throws Exception {
        newResource = createNewResource();
        newAlertDefinition = newResource.getAlertDefinitions().iterator().next(); // get first & only
        newAlertCondition = newAlertDefinition.getConditions().iterator().next(); // get first & only
    }

    @AfterMethod
    public void afterMethod() throws Exception {
        deleteNewResource(newResource);
    }

    public void testProducerConsumerChain() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();

        try {
            String logValue = "testValue";
            producerBean.sendActivateAlertConditionMessage(newAlertCondition.getId(), logValue, System
                .currentTimeMillis());

            Thread.sleep(1000); // make sure the consumer has time to process it

            em.refresh(newAlertCondition);

            Set<AlertConditionLog> conditionLogs = newAlertCondition.getConditionLogs();
            assert conditionLogs.size() == 1 : "Alert condition log wasn't persisted properly";

            AlertConditionLog resultLog = conditionLogs.iterator().next();
            assert newAlertCondition.equals(resultLog.getCondition()) : "Condition log doesn't properly reference condition definition";
            assert newAlertCondition.equals(resultLog.getValue()) : "Alert condition value not persisted properly: got "
                + resultLog.getValue() + " expected " + logValue;
            assert resultLog.getAlert() == null : "Alert should not have been set until conditions have been processed";
        } finally {
            getTransactionManager().rollback();
        }
    }

    private Resource createNewResource() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();

        Resource resource;

        try {
            try {
                ResourceType resourceType = new ResourceType("platName" + System.currentTimeMillis(), "testPlugin",
                    ResourceCategory.PLATFORM, null);
                em.persist(resourceType);

                resource = new Resource("resKey" + System.currentTimeMillis(), "resName", resourceType);

                AlertDefinition alertDefinition = new AlertDefinition();
                alertDefinition.setEnabled(true);
                alertDefinition.setAlertDampening(new AlertDampening(AlertDampening.Category.NONE));
                alertDefinition.setName("alertDefinitionName" + System.currentTimeMillis());
                alertDefinition.setConditionExpression(BooleanExpression.ALL);
                alertDefinition.setResource(resource); // this sets both sides of the relationship

                AlertCondition alertCondition = new AlertCondition();
                alertCondition.setName("alertConditionName" + System.currentTimeMillis());
                alertCondition.setCategory(AlertConditionCategory.AVAILABILITY);
                alertDefinition.addCondition(alertCondition); // this sets both sides of the relationship

                em.persist(resource); // cascade settings will persist alertDefinition too
            } catch (Exception e) {
                System.out.println("CANNOT PREPARE TEST: " + e);
                getTransactionManager().rollback();
                throw e;
            }

            getTransactionManager().commit();
        } finally {
            em.close();
        }

        return resource;
    }

    private void deleteNewResource(Resource resource) throws Exception {
        if (resource != null) {
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            try {
                ResourceType type = em.find(ResourceType.class, resource.getResourceType().getId());
                Resource res = em.find(Resource.class, resource.getId());

                /*
                 * cascade effects:
                 *
                 * - removing the resource removes its alert definitions   - removing an alert definition will remove
                 * its alert conditions   - removing an alert condition will remove its alert condition logs
                 */
                em.remove(res);
                em.remove(type);

                getTransactionManager().commit();
            } catch (Exception e) {
                try {
                    System.out.println("CANNOT CLEAN UP TEST: Cause: " + e);
                    getTransactionManager().rollback();
                } catch (Exception ignore) {
                }
            } finally {
                em.close();
            }
        }
    }
}