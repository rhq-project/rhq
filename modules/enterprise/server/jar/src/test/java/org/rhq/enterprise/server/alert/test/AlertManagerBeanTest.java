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
package org.rhq.enterprise.server.alert.test;

import javax.persistence.EntityManager;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.AlertConditionLog;
import org.rhq.core.domain.alert.AlertDampening;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.alert.BooleanExpression;
import org.rhq.core.domain.alert.notification.AlertNotificationLog;
import org.rhq.core.domain.alert.notification.EmailNotification;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Test for {@link AlertManagerLocal} SLSB.
 */
@Test
public class AlertManagerBeanTest extends AbstractEJB3Test {
    private AlertManagerLocal alertManager;
    private Subject superuser;
    private Resource newResource;

    @BeforeMethod
    public void beforeMethod() throws Exception {
        alertManager = LookupUtil.getAlertManager();
        superuser = LookupUtil.getSubjectManager().getOverlord();
        newResource = createNewResource();
    }

    @AfterMethod
    public void afterMethod() throws Exception {
        deleteNewResource(newResource);
    }

    public void testAlertDelete() {
        assert 1 == alertManager.deleteAlerts(superuser, newResource.getId());
    }

    public void testAlertDeleteInRange() {
        assert 1 == alertManager.deleteAlerts(0L, System.currentTimeMillis() + 600000L); // go out into the future to make sure we get our alert
    }

    private Resource createNewResource() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();

        Resource resource;

        try {
            try {
                long now = System.currentTimeMillis();
                ResourceType resourceType = new ResourceType("plat" + now, "test", ResourceCategory.PLATFORM, null);

                em.persist(resourceType);

                Agent agent = new Agent("testagent", "testaddress", 1, "", "testtoken");
                em.persist(agent);
                em.flush();

                resource = new Resource("reskey" + now, "resname", resourceType);
                resource.setAgent(agent);
                em.persist(resource);

                AlertDefinition ad = new AlertDefinition();
                ad.setName("alertTest");
                ad.setEnabled(true);
                ad.setPriority(AlertPriority.HIGH);
                ad.setResource(resource);
                ad.setAlertDampening(new AlertDampening(AlertDampening.Category.NONE));
                ad.setConditionExpression(BooleanExpression.ALL);
                ad.setRecoveryId(0);
                em.persist(ad);

                AlertCondition ac = new AlertCondition(ad, AlertConditionCategory.AVAILABILITY);
                ac.setComparator("==");
                em.persist(ac);

                EmailNotification an = new EmailNotification(ad, "foo@bar.com");
                em.persist(an);

                AlertConditionLog acl = new AlertConditionLog(ac, now);
                em.persist(acl);

                Alert a = new Alert(ad, now);
                AlertNotificationLog anl = new AlertNotificationLog(ad);
                anl.setAlert(a);
                em.persist(a);
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
                Agent agent = em.find(Agent.class, resource.getAgent().getId());

                for (AlertDefinition ad : res.getAlertDefinitions()) {
                    em.remove(ad);
                }

                em.remove(res);
                em.remove(agent);
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