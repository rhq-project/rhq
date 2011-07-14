/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.domain.content.test;

import java.util.List;
import java.util.Random;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.content.Architecture;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.ContentSourceType;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.content.PackageBits;
import org.rhq.core.domain.content.PackageBitsBlob;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.PackageVersionContentSource;
import org.rhq.core.domain.content.PackageVersionContentSourcePK;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.content.RepoContentSource;
import org.rhq.core.domain.content.RepoPackageVersion;
import org.rhq.core.domain.content.ResourceRepo;
import org.rhq.core.domain.content.composite.PackageVersionMetadataComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.test.AbstractEJB3Test;

/**
 * Test the many-to-many relationship that we model with an explicit entity representing the mapping table.
 *
 * @author John Mazzitelli
 */
@Test(groups = "integration.ejb3")
public class PackageVersionContentSourceTest extends AbstractEJB3Test {
    public void testInsert() throws Exception {
        getTransactionManager().begin();
        try {
            EntityManager em = getEntityManager();

            ResourceType rt = new ResourceType("testPVCSResourceType", "testPlugin", ResourceCategory.PLATFORM, null);
            Resource resource = new Resource("testPVCSResource", "testPVCSResource", rt);
            resource.setUuid("" + new Random().nextInt());
            Architecture arch = new Architecture("testPVCSInsertArch");
            PackageType pt = new PackageType("testPVCSInsertPT", resource.getResourceType());
            Package pkg = new Package("testPVCSInsertPackage", pt);
            PackageVersion pv = new PackageVersion(pkg, "version", arch);
            ContentSourceType cst = new ContentSourceType("testPVCSContentSourceType");
            ContentSource cs = new ContentSource("testPVCSContentSource", cst);
            PackageVersionContentSource pvcs = new PackageVersionContentSource(pv, cs, "fooLocation");

            Configuration csConfig = new Configuration();
            csConfig.put(new PropertySimple("csConfig1", "csConfig1Value"));
            cs.setConfiguration(csConfig);

            Configuration pvConfig = new Configuration();
            pvConfig.put(new PropertySimple("pvConfig1", "pvConfig1Value"));
            pv.setExtraProperties(pvConfig);

            String pvMetadata = "testInsertMetadata";
            pv.setMetadata(pvMetadata.getBytes());

            em.persist(rt);
            em.persist(resource);
            em.persist(arch);
            em.persist(pt);
            em.persist(pkg);
            em.persist(pv);
            em.persist(cst);
            em.persist(cs);
            em.persist(pvcs);
            em.flush();
            em.close();
            em = getEntityManager();

            PackageVersionContentSourcePK pk = new PackageVersionContentSourcePK(pv, cs);
            PackageVersionContentSource pvcsDup = em.find(PackageVersionContentSource.class, pk);

            em.close();

            assert pvcsDup != null;
            assert pvcsDup.equals(pvcs);
            assert pvcsDup.getLocation().equals("fooLocation");

            PackageVersionContentSourcePK pkDup = pvcsDup.getPackageVersionContentSourcePK();
            assert pkDup.getContentSource().getName().equals("testPVCSContentSource");
            assert pkDup.getPackageVersion().getGeneralPackage().getName().equals("testPVCSInsertPackage");

            em = getEntityManager();
            Query q = em.createNamedQuery(PackageVersionContentSource.QUERY_FIND_BY_CONTENT_SOURCE_ID);
            q.setParameter("id", cs.getId());
            List<PackageVersionContentSource> allPvcs = q.getResultList();

            em.close();

            // make sure the fetch joining works by looking deep inside the objects
            assert allPvcs != null;
            assert allPvcs.size() == 1;
            pvcs = allPvcs.get(0);
            assert pvcs.getPackageVersionContentSourcePK() != null;
            assert pvcs.getLocation().equals("fooLocation");
            assert pvcs.getPackageVersionContentSourcePK().getPackageVersion().equals(pv);
            assert pvcs.getPackageVersionContentSourcePK().getPackageVersion().getArchitecture().equals(arch);
            assert pvcs.getPackageVersionContentSourcePK().getPackageVersion().getGeneralPackage().equals(pkg);
            assert pvcs.getPackageVersionContentSourcePK().getPackageVersion().getExtraProperties().equals(pvConfig);
            assert new String(pvcs.getPackageVersionContentSourcePK().getPackageVersion().getMetadata())
                .equals(pvMetadata);
            assert pvcs.getPackageVersionContentSourcePK().getContentSource().equals(cs);
            assert pvcs.getPackageVersionContentSourcePK().getContentSource().getConfiguration().equals(csConfig);
            assert pvcs.getPackageVersionContentSourcePK().getPackageVersion().getGeneralPackage().getPackageType()
                .getResourceType().equals(rt);

            // add repo and subscribe resource to it; test metadata query
            em = getEntityManager();
            Repo repo = new Repo("testPVCSRepo");
            em.persist(repo);
            RepoContentSource ccsmapping = repo.addContentSource(cs);
            em.persist(ccsmapping);
            ResourceRepo subscription = repo.addResource(resource);
            em.persist(subscription);
            RepoPackageVersion mapping = repo.addPackageVersion(pv);
            em.persist(mapping);
            em.flush();

            repo = em.find(Repo.class, repo.getId());
            assert repo.getResources().contains(resource);
            assert repo.getContentSources().contains(cs);

            q = em.createNamedQuery(PackageVersion.QUERY_FIND_METADATA_BY_RESOURCE_ID);
            q.setParameter("resourceId", resource.getId());
            List<PackageVersionMetadataComposite> metadataList = q.getResultList();
            assert metadataList.size() == 1 : "-->" + metadataList;
            PackageVersionMetadataComposite composite = metadataList.get(0);
            assert composite.getPackageVersionId() == pv.getId();
            assert new String(composite.getMetadata()).equals(new String(pv.getMetadata()));
            assert new String(composite.getPackageDetailsKey().getName()).equals(pkg.getName());
            assert new String(composite.getPackageDetailsKey().getVersion()).equals(pv.getVersion());
            assert new String(composite.getPackageDetailsKey().getPackageTypeName()).equals(pt.getName());
            assert new String(composite.getPackageDetailsKey().getArchitectureName()).equals(arch.getName());
            em.close();

            // remove cs from pv
            em = getEntityManager();
            pvcs = em.find(PackageVersionContentSource.class, pvcs.getPackageVersionContentSourcePK());
            em.remove(pvcs);
        } finally {
            getTransactionManager().rollback();
        }
    }

    public void testDelete() throws Exception {
        getTransactionManager().begin();
        try {
            EntityManager em = getEntityManager();

            ResourceType rt = new ResourceType("testPVCSResourceType", "testPlugin", ResourceCategory.PLATFORM, null);
            Resource resource = new Resource("testPVCSResource", "testPVCSResource", rt);
            resource.setUuid("" + new Random().nextInt());
            Architecture arch = new Architecture("testPVCSInsertArch");
            PackageType pt = new PackageType("testPVCSInsertPT", resource.getResourceType());
            Package pkg = new Package("testPVCSInsertPackage", pt);
            PackageVersion pv = new PackageVersion(pkg, "version", arch);
            ContentSourceType cst = new ContentSourceType("testPVCSContentSourceType");
            ContentSource cs = new ContentSource("testPVCSContentSource", cst);
            PackageVersionContentSource pvcs = new PackageVersionContentSource(pv, cs, "fooLocation");

            Configuration csConfig = new Configuration();
            csConfig.put(new PropertySimple("csConfig1", "csConfig1Value"));
            cs.setConfiguration(csConfig);

            Configuration pvConfig = new Configuration();
            pvConfig.put(new PropertySimple("pvConfig1", "pvConfig1Value"));
            pv.setExtraProperties(pvConfig);

            em.persist(rt);
            em.persist(resource);
            em.persist(arch);
            em.persist(pt);
            em.persist(pkg);
            em.persist(pv);
            em.persist(cst);
            em.persist(cs);
            em.persist(pvcs);
            em.flush();
            em.close();
            em = getEntityManager();

            PackageVersionContentSourcePK pk = new PackageVersionContentSourcePK(pv, cs);
            PackageVersionContentSource pvcsDup = em.find(PackageVersionContentSource.class, pk);

            em.close();

            assert pvcsDup != null;
            assert pvcsDup.equals(pvcs);
            assert pvcsDup.getLocation().equals("fooLocation");

            em = getEntityManager();
            Query q = em.createNamedQuery(PackageVersionContentSource.DELETE_BY_CONTENT_SOURCE_ID);
            q.setParameter("contentSourceId", cs.getId());
            assert 1 == q.executeUpdate();
            em.close();
        } finally {
            getTransactionManager().rollback();
        }
    }

    public void testDeleteOrphanedPV() throws Exception {
        getTransactionManager().begin();
        try {
            EntityManager em = getEntityManager();

            ResourceType rt = new ResourceType("testPVCSResourceType", "testPlugin", ResourceCategory.PLATFORM, null);
            Resource resource = new Resource("testPVCSResource", "testPVCSResource", rt);
            resource.setUuid("" + new Random().nextInt());
            Architecture arch = new Architecture("testPVCSInsertArch");
            PackageType pt = new PackageType("testPVCSInsertPT", resource.getResourceType());
            Package pkg = new Package("testPVCSInsertPackage", pt);
            PackageBits bits = createPackageBits(em);
            PackageVersion pv = new PackageVersion(pkg, "version", arch);
            ContentSourceType cst = new ContentSourceType("testPVCSContentSourceType");
            ContentSource cs = new ContentSource("testPVCSContentSource", cst);
            PackageVersionContentSource pvcs = new PackageVersionContentSource(pv, cs, "fooLocation");

            Configuration csConfig = new Configuration();
            csConfig.put(new PropertySimple("csConfig1", "csConfig1Value"));
            cs.setConfiguration(csConfig);

            Configuration pvConfig = new Configuration();
            pvConfig.put(new PropertySimple("pvConfig1", "pvConfig1Value"));
            pv.setExtraProperties(pvConfig);

            bits.getBlob().setBits("testDeleteOrphanedPV".getBytes());
            pv.setPackageBits(bits);

            em.persist(rt);
            em.persist(resource);
            em.persist(arch);
            em.persist(pt);
            em.persist(pkg);
            em.persist(pv);
            em.persist(cst);
            em.persist(cs);
            em.persist(pvcs);
            em.flush();
            em.close();

            em = getEntityManager();
            PackageVersionContentSourcePK pk = new PackageVersionContentSourcePK(pv, cs);
            PackageVersionContentSource pvcsDup = em.find(PackageVersionContentSource.class, pk);
            em.close();

            assert pvcsDup != null;
            assert pvcsDup.equals(pvcs);
            assert pvcsDup.getLocation().equals("fooLocation");

            em = getEntityManager();
            Query q = em.createNamedQuery(PackageVersionContentSource.DELETE_BY_CONTENT_SOURCE_ID);
            q.setParameter("contentSourceId", cs.getId());
            assert 1 == q.executeUpdate();
            em.close();

            em = getEntityManager();
            PackageBits findBits = em.find(PackageBits.class, bits.getId());
            em.close();
            assert findBits != null : "The bits did not cascade-persist for some reason";
            assert findBits.getId() > 0 : "The package bits did not cascade-persist for some reason!";
            assert new String(bits.getBlob().getBits()).equals(new String(findBits.getBlob().getBits()));

            em = getEntityManager();
            q = em.createNamedQuery(PackageVersion.DELETE_IF_NO_CONTENT_SOURCES_OR_REPOS);
            assert 1 == q.executeUpdate();
            em.close();

            em = getEntityManager();
            q = em.createNamedQuery(PackageBits.DELETE_IF_NO_PACKAGE_VERSION);
            assert q.executeUpdate() > 0;
            em.close();

            em = getEntityManager();
            findBits = em.find(PackageBits.class, bits.getId());
            em.close();
            assert findBits == null : "The bits did not delete for some reason";
        } finally {
            getTransactionManager().rollback();
        }
    }

    private PackageBits createPackageBits(EntityManager em) {
        PackageBits bits = null;
        PackageBitsBlob blob = null;

        // We have to work backwards to avoid constraint violations. PackageBits requires a PackageBitsBlob,
        // so create and persist that first, getting the ID
        blob = new PackageBitsBlob();
        em.persist(blob);

        // Now create the PackageBits entity and assign the Id and blob.  Note, do not persist the
        // entity, the row already exists. Just perform and flush the update.
        bits = new PackageBits();
        bits.setId(blob.getId());
        bits.setBlob(blob);
        em.flush();

        // return the new PackageBits and associated PackageBitsBlob
        return bits;
    }

}