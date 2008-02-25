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
package org.rhq.core.domain.content.test;

import javax.persistence.EntityManager;
import org.testng.annotations.Test;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.content.Architecture;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.ContentSourceType;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.test.AbstractEJB3Test;

@Test
public class ContentSourceTest extends AbstractEJB3Test {
    public void testInsert() throws Exception {
        getTransactionManager().begin();
        try {
            EntityManager em = getEntityManager();

            ResourceType rt = new ResourceType("testCSResourceType", "testPlugin", ResourceCategory.PLATFORM, null);
            Resource resource = new Resource("testCSResource", "testCSResource", rt);
            Architecture arch = new Architecture("testCSInsertArch");
            PackageType pt = new PackageType("testCSInsertPT", resource.getResourceType());
            Package pkg = new Package("testCSInsertPackage", pt);
            PackageVersion pv = new PackageVersion(pkg, "version", arch);
            ContentSourceType cst = new ContentSourceType("testCSContentSourceType");
            ContentSource cs = new ContentSource("testCSContentSource", cst);

            Configuration config = new Configuration();
            config.put(new PropertySimple("one", "oneValue"));
            cs.setConfiguration(config);

            em.persist(rt);
            em.persist(resource);
            em.persist(arch);
            em.persist(pt);
            em.persist(pkg);
            em.persist(pv);
            em.persist(cst);
            em.persist(cs);
            em.flush();
            em.close();
            em = getEntityManager();

            cs = em.find(ContentSource.class, cs.getId());
            assert cs != null;
            assert cs.getConfiguration() != null;
            assert cs.getConfiguration().getSimple("one").getStringValue().equals("oneValue");
            assert cs.getSyncSchedule() != null;
            assert cs.getContentSourceType().getDefaultSyncSchedule() != null;

            em.remove(cs);

            cs = em.find(ContentSource.class, cs.getId());
            assert cs == null;
        } finally {
            getTransactionManager().rollback();
        }
    }

    public void testNullSyncSchedule() throws Exception {
        getTransactionManager().begin();
        try {
            EntityManager em = getEntityManager();

            ResourceType rt = new ResourceType("testCSResourceType", "testPlugin", ResourceCategory.PLATFORM, null);
            Resource resource = new Resource("testCSResource", "testCSResource", rt);
            Architecture arch = new Architecture("testCSInsertArch");
            PackageType pt = new PackageType("testCSInsertPT", resource.getResourceType());
            Package pkg = new Package("testCSInsertPackage", pt);
            PackageVersion pv = new PackageVersion(pkg, "version", arch);
            ContentSourceType cst = new ContentSourceType("testCSContentSourceType");
            cst.setDefaultSyncSchedule(null);
            ContentSource cs = new ContentSource("testCSContentSource", cst);
            cs.setSyncSchedule(null);

            Configuration config = new Configuration();
            config.put(new PropertySimple("one", "oneValue"));
            cs.setConfiguration(config);

            em.persist(rt);
            em.persist(resource);
            em.persist(arch);
            em.persist(pt);
            em.persist(pkg);
            em.persist(pv);
            em.persist(cst);
            em.persist(cs);
            em.flush();
            em.close();
            em = getEntityManager();

            cs = em.find(ContentSource.class, cs.getId());
            assert cs != null;
            assert cs.getConfiguration() != null;
            assert cs.getConfiguration().getSimple("one").getStringValue().equals("oneValue");
            assert cs.getSyncSchedule() == null;
            assert cs.getContentSourceType().getDefaultSyncSchedule() == null;

            em.remove(cs);

            cs = em.find(ContentSource.class, cs.getId());
            assert cs == null;
        } finally {
            getTransactionManager().rollback();
        }
    }

    public void testEmptySyncSchedule() throws Exception {
        // using empty strings to see that Oracle still behaves itself
        getTransactionManager().begin();
        try {
            EntityManager em = getEntityManager();

            ResourceType rt = new ResourceType("testCSResourceType", "testPlugin", ResourceCategory.PLATFORM, null);
            Resource resource = new Resource("testCSResource", "testCSResource", rt);
            Architecture arch = new Architecture("testCSInsertArch");
            PackageType pt = new PackageType("testCSInsertPT", resource.getResourceType());
            Package pkg = new Package("testCSInsertPackage", pt);
            PackageVersion pv = new PackageVersion(pkg, "version", arch);
            ContentSourceType cst = new ContentSourceType("testCSContentSourceType");
            cst.setDefaultSyncSchedule("");
            ContentSource cs = new ContentSource("testCSContentSource", cst);
            cs.setSyncSchedule("");

            Configuration config = new Configuration();
            config.put(new PropertySimple("one", "oneValue"));
            cs.setConfiguration(config);

            em.persist(rt);
            em.persist(resource);
            em.persist(arch);
            em.persist(pt);
            em.persist(pkg);
            em.persist(pv);
            em.persist(cst);
            em.persist(cs);
            em.flush();
            em.close();
            em = getEntityManager();

            cs = em.find(ContentSource.class, cs.getId());
            assert cs != null;
            assert cs.getConfiguration() != null;
            assert cs.getConfiguration().getSimple("one").getStringValue().equals("oneValue");
            assert cs.getSyncSchedule() == null;
            assert cs.getContentSourceType().getDefaultSyncSchedule() == null;

            em.remove(cs);

            cs = em.find(ContentSource.class, cs.getId());
            assert cs == null;
        } finally {
            getTransactionManager().rollback();
        }
    }
}