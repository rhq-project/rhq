package org.rhq.enterprise.server.content.test;

import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.Advisory;
import org.rhq.core.domain.content.AdvisoryCVE;
import org.rhq.core.domain.content.AdvisoryPackage;
import org.rhq.core.domain.content.Architecture;
import org.rhq.core.domain.content.CVE;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.content.PackageCategory;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.content.AdvisoryManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Pradeep Kilambi
 */
public class AdvisoryManagerBeanTest extends AbstractEJB3Test {
    private final static boolean ENABLED = true;
    private AdvisoryManagerLocal advManager;
    private Subject overlord;
    private PackageType packageType1;

    @BeforeMethod
    public void setupBeforeMethod() throws Exception {
        TransactionManager tx = getTransactionManager();
        tx.begin();

        advManager = LookupUtil.getAdvisoryManagerLocal();

        overlord = LookupUtil.getSubjectManager().getOverlord();
    }

    @AfterMethod
    public void tearDownAfterMethod() throws Exception {
        TransactionManager tx = getTransactionManager();
        if (tx != null) {
            tx.rollback();
        }
    }

    @Test(enabled = ENABLED)
    public void createDeleteAdvisory() throws Exception {

        String advisory = "RHBA-2009:1202-3";
        String advisoryType = "BugFix Advisory";
        String synopsis = "synopsis to test advisory creation";

        int id1 = advManager.createAdvisory(overlord, advisory, advisoryType, synopsis).getId();

        Advisory advobject = advManager.getAdvisoryByName(advisory);

        assert advobject != null;
        assert id1 == advobject.getId();
        System.out.println("advisory Created " + advobject);
        advManager.deleteAdvisoryByAdvId(overlord, advobject.getId());
        System.out.println("advisory deleted " + advobject);
        advobject = advManager.getAdvisoryByName(advobject.getAdvisory());
        assert advobject == null;

    }

    @Test(enabled = ENABLED)
    public void getCVEFromAdvisoryTest() throws Exception {
        EntityManager entityManager = getEntityManager();

        String advisory = "RHBA-2009:1202-4";
        String advisoryType = "BugFix Advisory";
        String synopsis = "synopsis to test advisory creation";

        Advisory advobject = new Advisory(advisory, advisoryType, synopsis);
        entityManager.persist(advobject);
        int id1 = advobject.getId();
        entityManager.flush();

        CVE cve = advManager.createCVE(overlord, "CAN-2009:1203");
        AdvisoryCVE advisory_cve = advManager.createAdvisoryCVE(overlord, advobject, cve);

        PageList<AdvisoryCVE> cvelist = advManager.getAdvisoryCVEByAdvId(overlord, id1, PageControl
            .getUnlimitedInstance());

        assert cvelist != null;
        assert cvelist.size() != 0;
        advManager.deleteAdvisoryCVE(overlord, id1);
        advManager.deleteCVE(overlord, cve.getId());
        advManager.deleteAdvisoryByAdvId(overlord, advobject.getId());
        advobject = advManager.getAdvisoryByName(advobject.getAdvisory());
        assert advobject == null;

    }

    @Test(enabled = ENABLED)
    public void getPackagesForAdvisoryTest() throws Exception {
        EntityManager em = getEntityManager();

        String advisory = "RHBA-2009:1202-4";
        String advisoryType = "BugFix Advisory";
        String synopsis = "synopsis to test advisory creation";

        Advisory advobject = advManager.createAdvisory(overlord, advisory, advisoryType, synopsis); //new Advisory(advisory, advisoryType, synopsis);
        int id1 = advobject.getId();

        ResourceType resourceType1 = new ResourceType("platform-" + System.currentTimeMillis(), "TestPlugin",
            ResourceCategory.PLATFORM, null);
        em.persist(resourceType1);
        em.flush();
        System.out.println("persisted resourceTYpe");

        Architecture architecture1 = em.find(Architecture.class, 1);

        // Add package types to resource type
        packageType1 = new PackageType();
        packageType1.setName("package1-" + System.currentTimeMillis());
        packageType1.setDescription("");
        packageType1.setCategory(PackageCategory.DEPLOYABLE);
        packageType1.setDisplayName("TestResourcePackage");
        packageType1.setCreationData(true);
        packageType1.setResourceType(resourceType1);
        em.persist(packageType1);
        em.flush();
        System.out.println("persisted packageType");
        Package package1 = new Package("Package1", packageType1);

        package1.addVersion(new PackageVersion(package1, "1.0.0", architecture1));
        package1.addVersion(new PackageVersion(package1, "2.0.0", architecture1));

        em.persist(package1);
        em.flush();
        System.out.println("persisted pqckage");
        //AdvisoryPackage ap = new AdvisoryPackage(advobject, package1);
        //em.persist(ap);

        AdvisoryPackage ap = advManager.createAdvisoryPackage(overlord, advobject, package1);

        System.out.println("persisted aadvisoryPackage" + ap);
        PageList<AdvisoryPackage> pkglist = advManager.findPackageByAdvisory(overlord, id1, PageControl
            .getUnlimitedInstance());
        System.out.println("list of pkgs" + pkglist);
        assert pkglist != null;
        assert pkglist.size() != 0;

    }
}
