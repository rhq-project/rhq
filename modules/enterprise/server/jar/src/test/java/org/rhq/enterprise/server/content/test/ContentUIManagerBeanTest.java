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
 * @author Jason Dobies
 */
public class ContentUIManagerBeanTest extends AbstractEJB3Test {

    private static final boolean ENABLE_TESTS = true;

    private final Log log = LogFactory.getLog(this.getClass());

    private ContentUIManagerLocal contentUIManager;
    private SubjectManagerLocal subjectManager;

    private Architecture architecture;
    private PackageType packageType1;
    
    private Package package1;
    private Package package2;
    private Package package3;
    private Package package4;

    private ChannelPackageVersion channelPackageVersion1;
    private ChannelPackageVersion channelPackageVersion2;
    private ChannelPackageVersion channelPackageVersion3;
    private ChannelPackageVersion channelPackageVersion4;

    private InstalledPackage installedPackage1;

    private Resource resource;
    private ResourceType resourceType;

    private Channel channel1;
    private Channel channel2;

    private ResourceChannel resourceChannel1;
    private ResourceChannel resourceChannel2;

    private ProductVersion productVersion1;
    private ProductVersion productVersion2;

    // Setup  --------------------------------------------

    @BeforeClass
    public void setupBeforeClass() throws Exception {
        contentUIManager = LookupUtil.getContentUIManager();
        subjectManager = LookupUtil.getSubjectManager();
    }

    @BeforeMethod
    public void populateDatabase() throws Exception {
        setupTestEnvironment();
    }

    @AfterMethod
    public void cleanDatabase() throws Exception {
        tearDownTestEnvironment();
    }

    // Test Cases  --------------------------------------------

    @Test(enabled = ENABLE_TESTS)
    public void testEligiblePackagesLogic() throws Exception {

        // This test should currently fail, since the query does not take all of the required information into account.
        // jdobies, Mar 7, 2008

        Subject overlord = subjectManager.getOverlord();
        PageControl pageControl = new PageControl(0, 100);

        PageList<PackageVersionComposite> pageList =
            contentUIManager.getPackageVersionCompositesByFilter(overlord, resource.getId(), null, pageControl);

        assert pageList != null : "Null page list returned from query";

        /* Expected results:
           - Package 1 - Included in the list; it has no product version thus is always applicable
           - Package 2 - Included in the list; it includes the resource's version in its product version list
           - Package 3 - Not included; it contains a product version list but not the resource's version
           - Package 4 - Not included; it has no product version but it is already installed on the resource

         */

        for (PackageVersionComposite pvc : pageList.getValues()) {
            log.warn("Package: " + pvc.getPackageName());
        }

        assert pageList.getTotalSize() == 2 : "Incorrect total size found. Found: " + pageList.getTotalSize();
    }

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

    private void setupTestEnvironment() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();

        try {
            try {
                architecture = em.find(Architecture.class, 1);

                resourceType = new ResourceType("resourcetype-" + System.currentTimeMillis(), "TestPlugin",
                    ResourceCategory.PLATFORM, null);
                em.persist(resourceType);

                // Create resource against which we'll be retrieving packages
                resource = new Resource("parent" + System.currentTimeMillis(), "name", resourceType);
                resource.setVersion("1.0");
                em.persist(resource);

                // Product versions
                productVersion1 = new ProductVersion();
                productVersion1.setVersion("1.0");
                productVersion1.setResourceType(resourceType);
                em.persist(productVersion1);

                resource.setProductVersion(productVersion1);

                productVersion2 = new ProductVersion();
                productVersion2.setVersion("2.0");
                productVersion2.setResourceType(resourceType);
                em.persist(productVersion2);

                // Add package types to resource type
                packageType1 = new PackageType();
                packageType1.setName("package1-" + System.currentTimeMillis());
                packageType1.setDescription("");
                packageType1.setCategory(PackageCategory.DEPLOYABLE);
                packageType1.setDisplayName("TestResourcePackage");
                packageType1.setCreationData(true);
                packageType1.setResourceType(resourceType);
                em.persist(packageType1);

                resourceType.addPackageType(packageType1);

                // Package 1 - No product versions specified
                package1 = new Package("Package1", packageType1);
                PackageVersion packageVersion1 = new PackageVersion(package1, "1.0.0", architecture);
                package1.addVersion(packageVersion1);

                em.persist(package1);

                // Package 2 - Has list of product versions that contains the resource's version
                package2 = new Package("Package2", packageType1);
                PackageVersion packageVersion2 = new PackageVersion(package2, "1.0.0", architecture);
                ProductVersionPackageVersion pvpv1 = packageVersion2.addProductVersion(productVersion1);
                ProductVersionPackageVersion pvpv2 = packageVersion2.addProductVersion(productVersion2);
                package2.addVersion(packageVersion2);

                em.persist(package2);
                em.persist(pvpv1);
                em.persist(pvpv2);

                // Package 3 - Has list of product versions where the resource version is not included
                package3 = new Package("Package3", packageType1);
                PackageVersion packageVersion3 = new PackageVersion(package3, "1.0.0", architecture);
                ProductVersionPackageVersion pvpv3 = packageVersion3.addProductVersion(productVersion2);
                package3.addVersion(packageVersion3);

                em.persist(package3);
                em.persist(pvpv3);

                // Package 4 - No product version restriction, but already installed on the resource
                package4 = new Package("Package4", packageType1);
                PackageVersion packageVersion4 = new PackageVersion(package4, "1.0.0", architecture);
                package4.addVersion(packageVersion4);

                em.persist(package4);

                // Wire up the channel to the resource and add all of these packages to it
                channel1 = new Channel("testChannel1");
                em.persist(channel1);

                channelPackageVersion1 = channel1.addPackageVersion(packageVersion1);
                channelPackageVersion2 = channel1.addPackageVersion(packageVersion2);
                channelPackageVersion3 = channel1.addPackageVersion(packageVersion3);
                channelPackageVersion4 = channel1.addPackageVersion(packageVersion4);

                em.persist(channelPackageVersion1);
                em.persist(channelPackageVersion2);
                em.persist(channelPackageVersion3);
                em.persist(channelPackageVersion4);

                resourceChannel1 = channel1.addResource(resource);
                em.persist(resourceChannel1);

                // Subscribe the resource to a second channel to make sure the joins won't duplicate stuff
                channel2 = new Channel("testChannel2");
                em.persist(channel2);

                resourceChannel2 = channel2.addResource(resource);
                em.persist(resourceChannel2);
                
                installedPackage1 = new InstalledPackage();
                installedPackage1.setResource(resource);
                installedPackage1.setPackageVersion(packageVersion4);
                resource.addInstalledPackage(installedPackage1);

                getTransactionManager().commit();
            } catch (Exception e) {
                e.printStackTrace();
                getTransactionManager().rollback();
                throw e;
            }
        } finally {
            em.close();
        }
    }

    private void tearDownTestEnvironment() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();

        try {
            try {
                Query q = em.createNamedQuery(ChannelPackageVersion.DELETE_BY_CHANNEL_ID);
                q.setParameter("channelId", channel1.getId());
                q.executeUpdate();

                q = em.createNamedQuery(ResourceChannel.DELETE_BY_RESOURCE_ID);
                q.setParameter("resourceId", resource.getId());
                q.executeUpdate();

                getTransactionManager().commit();
            } catch (Exception e) {
                e.printStackTrace();
                getTransactionManager().rollback();
                throw e;
            }
        } finally {
            em.close();
        }

        getTransactionManager().begin();
        em = getEntityManager();

        try {
            try {
                resource = em.find(Resource.class, resource.getId());
                for (InstalledPackage ip : resource.getInstalledPackages()) {
                    em.remove(ip);
                }

                package1 = em.find(Package.class, package1.getId());
                em.remove(package1);

                package2 = em.find(Package.class, package2.getId());
                em.remove(package2);

                package3 = em.find(Package.class, package3.getId());
                em.remove(package3);

                package4 = em.find(Package.class, package4.getId());
                em.remove(package4);

                packageType1 = em.find(PackageType.class, packageType1.getId());
                em.remove(packageType1);

                em.remove(resource);

                channel1 = em.find(Channel.class, channel1.getId());
                em.remove(channel1);

                productVersion1 = em.find(ProductVersion.class, productVersion1.getId());
                em.remove(productVersion1);

                productVersion2 = em.find(ProductVersion.class, productVersion2.getId());
                em.remove(productVersion2);

                resourceType = em.find(ResourceType.class, resourceType.getId());
                em.remove(resourceType);

                getTransactionManager().commit();
            } catch (Exception e) {
                e.printStackTrace();
                getTransactionManager().rollback();
                throw e;
            }
        } finally {
            em.close();
        }
    }
    
}