/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.enterprise.server.registration.test;

import javax.persistence.EntityManager;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.registration.RegistrationManagerLocal;
import org.rhq.enterprise.server.registration.ReleaseRepoMapping;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

public class RegistrationBeanTest extends AbstractEJB3Test {
    private RegistrationManagerLocal registrationManager;
    private ResourceTypeManagerLocal resourceTypeManager;
    private ResourceType platformType;
    private Agent agent;
    private int parentId = -1;
    private Subject overlord;

    @BeforeClass
    public void beforeClass() throws Exception {
        this.registrationManager = LookupUtil.getRegistrationManager();
        this.resourceTypeManager = LookupUtil.getResourceTypeManager();
        this.overlord = LookupUtil.getSubjectManager().getOverlord();

        getTransactionManager().begin();
        EntityManager em = getEntityManager();

        platformType = new ResourceType("RegistrationBeanTest.testRegisterPlatform",
            "RegistrationBeanTest.testRegisterPlatform", ResourceCategory.PLATFORM, null);
        em.persist(platformType);

        getTransactionManager().commit();
    }

    @AfterClass
    public void afterClass() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();

        platformType = em.find(ResourceType.class, platformType.getId());
        em.remove(platformType);

        getTransactionManager().commit();
    }

    @Test
    public void testRegisterPlatform() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        try {
            createAgent(em);
            em.flush();

            Resource platform = new Resource("alpha", "platform", platformType);

            registrationManager.registerPlatform(overlord, platform, parentId);

        } finally {
            getTransactionManager().rollback();
        }
    }

    public void testReleaseRepoMap() throws Exception {
        String release = "5Server";
        String version = "5.4.0";
        String arch = "x86_64";
        ReleaseRepoMapping repomap = new ReleaseRepoMapping(release, version, arch);
        String repo = repomap.getCompatibleRepo();
        assert repo.equals("rhel-x86_64-server-5");

        String release2 = "5Client";
        String arch2 = "x86_64";
        String version2 = "5.4.0";
        ReleaseRepoMapping repomap2 = new ReleaseRepoMapping(release2, version2, arch2);
        String repo2 = repomap2.getCompatibleRepo();
        assert repo2.equals("rhel-x86_64-client-5");

    }

    private void createAgent(EntityManager em) {
        agent = new Agent("TestAgent-dbbt", "0.0.0.0", 1, "http://here", "tok");
        em.persist(agent);
    }

}
