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

package org.rhq.enterprise.server.content;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.content.Advisory;
import org.rhq.core.domain.content.AdvisoryBuglist;
import org.rhq.core.domain.content.AdvisoryCVE;
import org.rhq.core.domain.content.AdvisoryPackage;
import org.rhq.core.domain.content.CVE;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.server.PersistenceUtility;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.RequiredPermission;

/**
 * @author Pradeep Kilambi
 */
@Stateless
public class AdvisoryManagerBean implements AdvisoryManagerLocal {

    // Attributes  --------------------------------------------

    private final Log log = LogFactory.getLog(this.getClass());

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public Advisory createAdvisory(Subject user, String advisory, String advisoryType, String synopsis)
        throws AdvisoryException {

        Advisory adv = new Advisory(advisory, advisoryType, synopsis);

        validateAdvisory(adv);
        entityManager.persist(adv);

        return adv;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public CVE createCVE(Subject user, String cvename) throws AdvisoryException {
        log.debug("User [" + user + "] is creating CVE [" + cvename + "]");
        CVE cve = new CVE(cvename);

        entityManager.persist(cve);
        return cve;

    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public AdvisoryCVE createAdvisoryCVE(Subject user, Advisory advisory, CVE cve) throws AdvisoryException {
        log.debug("User [" + user + "] is creating AdvisoryCVE [" + advisory + "]");
        AdvisoryCVE advcve = new AdvisoryCVE(advisory, cve);

        entityManager.persist(advcve);
        return advcve;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public AdvisoryBuglist createAdvisoryBuglist(Subject user, Advisory advisory, String buginfo)
        throws AdvisoryException {
        log.debug("User [" + user + "] is creating AdvisoryCVE [" + advisory + "]");
        AdvisoryBuglist advbug = new AdvisoryBuglist(advisory, buginfo);

        entityManager.persist(advbug);
        return advbug;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public AdvisoryPackage createAdvisoryPackage(Subject user, Advisory advisory, PackageVersion pkg)
        throws AdvisoryException {
        log.debug("User [" + user + "] is creating AdvisoryPackage [" + advisory + "]");
        AdvisoryPackage advpkg = new AdvisoryPackage(advisory, pkg);

        entityManager.persist(advpkg);
        return advpkg;
    }

    public void deleteCVE(Subject user, int cveId) {
        log.debug("User [" + user + "] is deleting CVE [" + cveId + "]");

        entityManager.flush();
        entityManager.clear();

        int success = entityManager.createNamedQuery(CVE.DELETE_BY_CVE_ID).setParameter("cveId", cveId).executeUpdate();

        if (success == 1) {
            log.debug("User [" + user + "] deleted CVE [" + cveId + "]");
        } else {
            log.debug("CVE ID [" + cveId + "] doesn't exist - nothing to delete");
        }
    }

    public void deleteAdvisoryCVE(Subject user, int advId) {
        log.debug("User [" + user + "] is deleting CVE [" + advId + "]");

        entityManager.flush();
        entityManager.clear();

        int success = entityManager.createNamedQuery(AdvisoryCVE.DELETE_BY_ADV_ID).setParameter("advId", advId)
            .executeUpdate();

        if (success == 1) {
            log.debug("User [" + user + "] deleted CVEs for " + advId);
        } else {
            log.debug("Advisory ID [" + advId + "] CVE deletion unsuccessful");
        }
    }

    public void deleteAdvisoryByAdvId(Subject user, int advId) {
        log.debug("User [" + user + "] is deleting Advisory [" + advId + "]");

        entityManager.flush();
        entityManager.clear();

        int success = entityManager.createNamedQuery(Advisory.QUERY_DELETE_BY_ADV_ID).setParameter("advid", advId)
            .executeUpdate();

        if (success == 1) {
            log.debug("User [" + user + "] deleted Advisory for " + advId);
        } else {
            log.debug("Advisory ID [" + advId + "] deletion unsuccessful");
        }

    }

    public void deleteAdvisoryPackage(Subject user, int advId) {
        log.debug("User [" + user + "] is deleting packages for advisory [" + advId + "]");

        entityManager.flush();
        entityManager.clear();

        int success = entityManager.createNamedQuery(AdvisoryPackage.DELETE_PACKAGES_BY_ADV_ID).setParameter("advId",
            advId).executeUpdate();

        if (success == 1) {
            log.debug("User [" + user + "] deleted AdvisoryPackage for [" + advId + "]");
        } else {
            log.debug("Advisory ID [" + advId + "] doesn't exist - nothing to delete");
        }
    }

    public void deleteAdvisoryBugList(Subject user, int advId) {
        log.debug("User [" + user + "] is deleting Buglist for Advisory [" + advId + "]");

        entityManager.flush();
        entityManager.clear();

        int success = entityManager.createNamedQuery(AdvisoryBuglist.DELETE_BY_ADV_ID).setParameter("advId", advId)
            .executeUpdate();

        if (success == 1) {
            log.debug("User [" + user + "] deleted buglist for Advisory [" + advId + "]");
        } else {
            log.debug("Advisory ID [" + advId + "] doesn't exist - nothing to delete");
        }
    }

    @SuppressWarnings("unchecked")
    public Advisory getAdvisoryByName(String advlabel) {
        Query query = entityManager.createNamedQuery(Advisory.QUERY_FIND_BY_ADV);

        query.setParameter("advisory", advlabel);
        List<Advisory> results = query.getResultList();

        if (results.size() > 0) {
            return results.get(0);
        } else {
            return null;
        }
    }

    /**
     * find list of Packages associated to an advisory
     * @param advId advisoryId
     * @return list of Package objects
     */
    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<AdvisoryPackage> findPackageByAdvisory(Subject subject, int advId, PageControl pc) {
        Query query = entityManager.createNamedQuery(AdvisoryPackage.FIND_PACKAGES_BY_ADV_ID);

        query.setParameter("advId", advId);
        List<AdvisoryPackage> results = query.getResultList();

        long count = getPackageCountFromAdv(subject, advId);
        return new PageList<AdvisoryPackage>(results, (int) count, pc);

    }

    /**
     * @param advId advisoryId
     * @return an advisorypackage object if exists else null
     */
    public AdvisoryPackage findAdvisoryPackage(Subject subject, int advId, int pkgVerId) {
        Query query = entityManager.createNamedQuery(AdvisoryPackage.FIND_ADVISORY_PACKAGE);
        log.error("Advisory ID: " + advId + " PackageVerId: " + pkgVerId);
        query.setParameter("advId", advId);
        query.setParameter("pkgVerId", pkgVerId);
        try {
            AdvisoryPackage result = (AdvisoryPackage) query.getSingleResult();
            return result;
        } catch (NoResultException nre) {
            //Object not found, return null for new creation
            return null;
        }

    }

    /**
     * find list of Packages Versions associated to an advisory
     * @return list of PackageVersion objects
     */
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PackageVersion findPackageVersionByPkgId(Subject subject, String rpmName, PageControl pc) {
        Query query = entityManager.createNamedQuery(PackageVersion.QUERY_FIND_PACKAGEVERSION_BY_FILENAME);

        query.setParameter("rpmName", rpmName);
        PackageVersion results = (PackageVersion) query.getSingleResult();
        if (results != null) {
            return results;
        } else {
            return null;
        }
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public long getPackageCountFromAdv(Subject subject, int advId) {
        Query countQuery = PersistenceUtility.createCountQuery(entityManager, AdvisoryPackage.FIND_PACKAGES_BY_ADV_ID);

        countQuery.setParameter("advId", advId);

        return ((Long) countQuery.getSingleResult()).longValue();
    }

    /**
     * Returns a list of available cves for requested Advisory
     * @param advId
     * @return A list of CVE objects associated to a given Advisory
     */
    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<AdvisoryCVE> getAdvisoryCVEByAdvId(Subject subject, int advId, PageControl pc) {
        entityManager.flush();
        Query query = entityManager.createNamedQuery(AdvisoryCVE.FIND_CVE_BY_ADV_ID);

        query.setParameter("advId", advId);
        List<AdvisoryCVE> results = query.getResultList();

        long count = getCVECountFromAdv(subject, advId);
        return new PageList<AdvisoryCVE>(results, (int) count, pc);

    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public long getCVECountFromAdv(Subject subject, int advId) {
        Query countQuery = PersistenceUtility.createCountQuery(entityManager, AdvisoryCVE.FIND_CVE_BY_ADV_ID);
        countQuery.setParameter("advId", advId);

        return ((Long) countQuery.getSingleResult()).longValue();
    }

    /**
     * Returns a list of bugs for requested Advisory
     * @param advId
     * @return A list of AdvisoryBuglist objects
     */
    @SuppressWarnings("unchecked")
    public List<AdvisoryBuglist> getAdvisoryBuglistByAdvId(Subject subject, int advId) {
        Query query = entityManager.createNamedQuery(AdvisoryBuglist.FIND_BUGS_BY_ADV_ID);

        query.setParameter("advId", advId);
        List<AdvisoryBuglist> results = query.getResultList();

        if (results.size() > 0) {
            return results;
        } else {
            return null;
        }

    }

    /**
     * Returns a Advisorybuglist object if exists else null
     * @param advId
     * @param buginfo
     * @return A AdvisoryBuglist objects
     */
    public AdvisoryBuglist getAdvisoryBuglist(Subject subject, int advId, String buginfo) {
        Query query = entityManager.createNamedQuery(AdvisoryBuglist.FIND_ADVISORY_BUGLIST);

        query.setParameter("advId", advId);
        query.setParameter("buginfo", buginfo);

        try {
            AdvisoryBuglist result = (AdvisoryBuglist) query.getSingleResult();
            return result;
        } catch (NoResultException nre) {
            //Object not found, return null for new creation
            return null;
        }

    }

    private void validateAdvisory(Advisory adv) throws AdvisoryException {
        if (adv.getAdvisory() == null || adv.getAdvisory().trim().equals("")) {
            throw new AdvisoryException("A valid Advisory tree is required");
        }
        log.debug("Advisory validating done " + adv);
    }

}
