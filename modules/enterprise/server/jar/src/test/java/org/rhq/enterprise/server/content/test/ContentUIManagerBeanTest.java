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
package org.rhq.enterprise.server.content.test;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.content.Architecture;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.content.PackageBits;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.PackageCategory;
import org.rhq.core.domain.content.InstalledPackage;
import org.rhq.core.domain.content.Channel;
import org.rhq.core.domain.content.ResourceChannel;
import org.rhq.core.domain.content.ChannelPackageVersion;
import org.rhq.core.domain.content.ProductVersionPackageVersion;
import org.rhq.core.domain.content.composite.LoadedPackageBitsComposite;
import org.rhq.core.domain.content.composite.PackageVersionComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ProductVersion;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.content.ContentUIManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.SessionTestHelper;

/**
 * Test case for general {@link org.rhq.enterprise.server.content.ContentUIManagerBean} tests. Any tests that would
 * require a large DB state prior to running are split off into their own test classes.
 *
 * @author Jason Dobies
 */
public class ContentUIManagerBeanTest extends AbstractEJB3Test {

    private static final boolean ENABLE_TESTS = true;

    private final Log log = LogFactory.getLog(this.getClass());

    private ContentUIManagerLocal contentUIManager;
    private SubjectManagerLocal subjectManager;

    // Setup  --------------------------------------------

    @BeforeClass
    public void setupBeforeClass() throws Exception {
        contentUIManager = LookupUtil.getContentUIManager();
        subjectManager = LookupUtil.getSubjectManager();
    }

    // Test Cases  --------------------------------------------

    @Test(enabled = ENABLE_TESTS)
    public void testPackageBits() throws Throwable {
        LoadedPackageBitsComposite composite;

        getTransactionManager().begin();

        try {
            EntityManager em = getEntityManager();
            Resource resource = SessionTestHelper.createNewResource(em, "testPkgBitsResource");
            PackageType pkgType = new PackageType("testPkgBitsPT", resource.getResourceType());
            org.rhq.core.domain.content.Package pkg = new Package("testPkgBitsP", pkgType);
            Architecture arch = new Architecture("testPkgArch");
            PackageVersion pkgVer = new PackageVersion(pkg, "1", arch);

            em.persist(pkgType);
            em.persist(pkg);
            em.persist(arch);
            em.persist(pkgVer);
            em.flush();

            // test that no bits are available right now
            composite = contentUIManager.getLoadedPackageBitsComposite(pkgVer.getId());
            assert composite != null;
            assert composite.getPackageVersionId() == pkgVer.getId();
            assert composite.getPackageBitsId() == null;
            assert !composite.isPackageBitsAvailable();
            assert !composite.isPackageBitsInDatabase();

            // pretend we loaded the bits, but we stored them somewhere other then the DB
            PackageBits packageBits = new PackageBits();
            em.persist(packageBits);
            pkgVer.setPackageBits(packageBits);
            pkgVer = em.merge(pkgVer);
            em.flush();

            // test that the bits are available, but are not stored in the DB
            composite = contentUIManager.getLoadedPackageBitsComposite(pkgVer.getId());
            assert composite != null;
            assert composite.getPackageVersionId() == pkgVer.getId();
            assert composite.getPackageBitsId() == packageBits.getId();
            assert composite.isPackageBitsAvailable();
            assert !composite.isPackageBitsInDatabase();

            // let's make sure there really is no data in the DB
            packageBits = em.find(PackageBits.class, packageBits.getId());
            assert packageBits != null;
            assert packageBits.getBits() == null;

            // now lets store some bits in the DB
            final String DATA = "testPackageBits data";
            packageBits.setBits(DATA.getBytes());
            em.merge(packageBits);
            em.flush();

            // test that the bits are available and stored in the DB
            composite = contentUIManager.getLoadedPackageBitsComposite(pkgVer.getId());
            assert composite != null;
            assert composite.getPackageVersionId() == pkgVer.getId();
            assert composite.getPackageBitsId() == packageBits.getId();
            assert composite.isPackageBitsAvailable();
            assert composite.isPackageBitsInDatabase();

            // let's make sure the data really is in the DB
            packageBits = em.find(PackageBits.class, packageBits.getId());
            assert packageBits != null;
            assert DATA.equals(new String(packageBits.getBits()));

            ////////////////////////////////////////////////////
            // create another package version and test with that
            ////////////////////////////////////////////////////
            PackageVersion pkgVer2 = new PackageVersion(pkg, "2", arch);
            em.persist(pkgVer2);
            em.flush();

            // first make sure the query still gets the right answer for the first pkgVer
            composite = contentUIManager.getLoadedPackageBitsComposite(pkgVer.getId());
            assert composite != null;
            assert composite.getPackageVersionId() == pkgVer.getId();
            assert composite.getPackageBitsId() == packageBits.getId();
            assert composite.isPackageBitsAvailable();
            assert composite.isPackageBitsInDatabase();

            // test that no bits are available right now
            composite = contentUIManager.getLoadedPackageBitsComposite(pkgVer2.getId());
            assert composite != null;
            assert composite.getPackageVersionId() == pkgVer2.getId();
            assert composite.getPackageBitsId() == null;
            assert !composite.isPackageBitsAvailable();
            assert !composite.isPackageBitsInDatabase();

            // pretend we loaded the bits, but we stored them somewhere other then the DB
            PackageBits packageBits2 = new PackageBits();
            em.persist(packageBits2);
            pkgVer2.setPackageBits(packageBits2);
            pkgVer2 = em.merge(pkgVer2);
            em.flush();

            // make sure the query still gets the right answer for the first pkgVer
            composite = contentUIManager.getLoadedPackageBitsComposite(pkgVer.getId());
            assert composite != null;
            assert composite.getPackageVersionId() == pkgVer.getId();
            assert composite.getPackageBitsId() == packageBits.getId();
            assert composite.isPackageBitsAvailable();
            assert composite.isPackageBitsInDatabase();

            // test that the bits are available, but are not stored in the DB
            composite = contentUIManager.getLoadedPackageBitsComposite(pkgVer2.getId());
            assert composite != null;
            assert composite.getPackageVersionId() == pkgVer2.getId();
            assert composite.getPackageBitsId() == packageBits2.getId();
            assert composite.isPackageBitsAvailable();
            assert !composite.isPackageBitsInDatabase();

            // let's make sure there really is no data in the DB
            packageBits2 = em.find(PackageBits.class, packageBits2.getId());
            assert packageBits2 != null;
            assert packageBits2.getBits() == null;

            // now lets store some bits in the DB
            final String DATA2 = "testPackageBits more data";
            packageBits2.setBits(DATA2.getBytes());
            em.merge(packageBits2);
            em.flush();

            // make sure the query still gets the right answer for the first pkgVer
            composite = contentUIManager.getLoadedPackageBitsComposite(pkgVer.getId());
            assert composite != null;
            assert composite.getPackageVersionId() == pkgVer.getId();
            assert composite.getPackageBitsId() == packageBits.getId();
            assert composite.isPackageBitsAvailable();
            assert composite.isPackageBitsInDatabase();

            // test that the bits are available and stored in the DB
            composite = contentUIManager.getLoadedPackageBitsComposite(pkgVer2.getId());
            assert composite != null;
            assert composite.getPackageVersionId() == pkgVer2.getId();
            assert composite.getPackageBitsId() == packageBits2.getId();
            assert composite.isPackageBitsAvailable();
            assert composite.isPackageBitsInDatabase();

            // let's make sure the data really is in the DB
            packageBits2 = em.find(PackageBits.class, packageBits2.getId());
            assert packageBits2 != null;
            assert DATA2.equals(new String(packageBits2.getBits()));
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        } finally {
            getTransactionManager().rollback();
        }
    }
}