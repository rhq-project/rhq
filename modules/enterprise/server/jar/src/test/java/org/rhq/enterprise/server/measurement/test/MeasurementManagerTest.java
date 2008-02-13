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
package org.rhq.enterprise.server.measurement.test;

import javax.persistence.EntityManager;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementProblemManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

public class MeasurementManagerTest extends AbstractEJB3Test {
    MeasurementProblemManagerLocal measurementProblemManager;
    MeasurementDataManagerLocal dataManager;

    @BeforeSuite
    public void init() {
        try {
            measurementProblemManager = LookupUtil.getMeasurementProblemManager();
            dataManager = LookupUtil.getMeasurementDataManager();
        } catch (Throwable t) {
            // Catch RuntimeExceptions and Errors and dump their stack trace, because Surefire will completely swallow them
            // and throw a cryptic NPE (see http://jira.codehaus.org/browse/SUREFIRE-157)!
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    /**
     * Just set up a resource where we can tack the availabilities on
     *
     * @param  em The EntityManager to use
     *
     * @return A Resource ready to use
     */
    private Resource setupResource(EntityManager em) {
        ResourceType resourceType = new ResourceType("fake platform", "fake plugin", ResourceCategory.PLATFORM, null);
        em.persist(resourceType);
        Resource platform = new Resource("org.jboss.on.TestPlatform", "Fake Platform", resourceType);
        em.persist(platform);
        em.flush();
        return platform;
    }

    @Test
    public void testRemoveGatheredMetrics() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        Resource res = setupResource(em);

        MeasurementDefinition md = new MeasurementDefinition(res.getResourceType(), "Testing...");
        em.persist(md);

        MeasurementSchedule sched = new MeasurementSchedule(md, res);
        em.persist(sched);

        em.flush();

        dataManager.removeGatheredMetricsForSchedule(sched);

        getTransactionManager().rollback();
    }
}