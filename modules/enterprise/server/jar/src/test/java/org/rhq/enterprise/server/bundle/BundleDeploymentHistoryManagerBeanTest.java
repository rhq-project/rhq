/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.enterprise.server.bundle;

import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.bundle.BundleDeployDefinition;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleDeploymentAction;
import org.rhq.core.domain.bundle.BundleDeploymentHistory;
import org.rhq.core.domain.criteria.BundleDeploymentHistoryCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Adam Young
 */
@SuppressWarnings( { "unchecked", "unused" })
@Test
public class BundleDeploymentHistoryManagerBeanTest extends AbstractEJB3Test {

    private static final String TEST_PREFIX = "bundledeploymenthistorytest";

    private static final boolean ENABLED = true;

    private BundleDeploymentHistoryManagerLocal bundleDeploymentHistoryManager;

    @BeforeMethod
    public void beforeMethod() {
        bundleDeploymentHistoryManager = LookupUtil.getBundleDeploymentHistoryManager();
    }

    @AfterMethod(alwaysRun = true)
    public void afterMethod() {

        EntityManager em = null;

        try {
            getTransactionManager().begin();
            em = getEntityManager();

            Query q;
            List doomed;

            // clean up any tests that don't already clean up after themselves

            getTransactionManager().commit();
            em.close();
            em = null;
        } catch (Exception e) {
            try {
                System.out.println("CANNOT CLEAN UP TEST: Cause: " + e);
                getTransactionManager().rollback();
            } catch (Exception ignore) {
            }
        } finally {
            if (null != em) {
                em.close();
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testFindByCriteria() throws Exception {

        Subject subject = getOverlord();
        Long auditTime = new Date().getTime();
        BundleDeploymentAction auditAction = BundleDeploymentAction.DEPLOYMENT_START;
        String auditMessage = "This is my message";
        BundleDeployDefinition def = new BundleDeployDefinition();
        Resource resource = new Resource();
        BundleDeployment bundleDeployment = new BundleDeployment(def, resource);
        BundleDeploymentHistory history = new BundleDeploymentHistory(bundleDeployment, subject.getName(), auditTime,
            auditAction, auditMessage);

        bundleDeploymentHistoryManager.addBundleDeploymentHistoryByBundleDeployment(history);

        BundleDeploymentHistoryCriteria criteria = new BundleDeploymentHistoryCriteria();
        List<BundleDeploymentHistory> histories = bundleDeploymentHistoryManager.findBundleDeploymentHistoryByCriteria(
            subject, criteria);

        assertNotNull(histories);
        assertTrue(histories.size() > 0);

    }

    private Subject getOverlord() {
        return LookupUtil.getSubjectManager().getOverlord();
    }
}
