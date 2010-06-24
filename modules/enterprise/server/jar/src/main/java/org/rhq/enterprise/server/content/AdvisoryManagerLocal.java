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

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.Advisory;
import org.rhq.core.domain.content.AdvisoryBuglist;
import org.rhq.core.domain.content.AdvisoryCVE;
import org.rhq.core.domain.content.AdvisoryPackage;
import org.rhq.core.domain.content.CVE;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * @author Pradeep Kilambi
 */
@Local
public interface AdvisoryManagerLocal {
    /**
     * Creates a new advisory in the system. If the advisory does not exist, it will be created.
     * If a advisory exists with the specified version ID, a new one will not be created and the
     * existing advisory will be returned.
     * @param subject
     * @param advisory advisory label
     * @param advisoryType adv type
     * @return newly created advisory object
     */
    Advisory createAdvisory(Subject subject, String advisory, String advisoryType, String synopsis)
        throws AdvisoryException;

    /**
     * creates a cve instance for a given cve name
     * @param user
     * @param cvename
     * @return a CVE object
     * @throws AdvisoryException
     */
    CVE createCVE(Subject subject, String cvename) throws AdvisoryException;

    /**
     * creates a AdvisoryCVE relationship object
     * @param user
     * @param advisory
     * @param cve
     * @return AdvisoryCVE object
     * @throws AdvisoryException
     */
    AdvisoryCVE createAdvisoryCVE(Subject subject, Advisory advisory, CVE cve) throws AdvisoryException;

    /**
     * creates a AdvisoryPackage mapping object
     * @param user
     * @param advisory
     * @param pkg
     * @return AdvisoryPackage object
     * @throws AdvisoryException
     */
    AdvisoryPackage createAdvisoryPackage(Subject subject, Advisory advisory, PackageVersion packageVersion)
        throws AdvisoryException;

    /**
     * deletes specified cve object
     * @param user
     * @param cveId
     */
    void deleteCVE(Subject subject, int cveId);

    /**
     * removes the AdvisoryCVE mapping
     * @param user
     * @param advId
     */
    void deleteAdvisoryCVE(Subject subject, int advId);

    /**
     * Deletes a given instance of advisory object.
     * @param user
     * @param advId
     */
    void deleteAdvisoryByAdvId(Subject subject, int advId);

    /**
     * find advisory by advisory name
     * @param advlabel
     * @return advisory object for a given name
     */
    Advisory getAdvisoryByName(String advlabel);

    /**
     * find packages associated for a given advisory
     * @param subject
     * @param advId
     * @param pc
     * @return a list of package objects
     */
    PageList<AdvisoryPackage> findPackageByAdvisory(Subject subject, int advId, PageControl pc);

    /**
     * find packages associated for a given package
     * @param subject
     * @param pkgId
     * @param pc
     * @return a list of packageversion objects
     */
    PackageVersion findPackageVersionByPkgId(Subject subject, String rpmName, PageControl pc);

    /**
     * find CVEs associated to a given advisory
     * @param subject
     * @param advId
     * @param pc
     * @return CVE objects associated to a given advisory
     */
    PageList<AdvisoryCVE> getAdvisoryCVEByAdvId(Subject subject, int advId, PageControl pc);

    /**
     * find bugs associated to a given advisory
     * @param subject
     * @param advId
     * @return list of AdvisoryBuglist objects
     */
    List<AdvisoryBuglist> getAdvisoryBuglistByAdvId(Subject subject, int advId);

    /**
     * Deletes a given instance of advisoryBuglist object.
     * @param user
     * @param advId
     */
    void deleteAdvisoryBugList(Subject overlord, int id);

    /**
     * Deletes a given instance of advisoryPackage object.
     * @param user
     * @param advId
     */
    void deleteAdvisoryPackage(Subject subject, int advId);

    /**
     *  find AdvisoryPackage object for given advId and packageVersion id
     * @param overlord
     * @param advId
     * @param pkgVerId
     */
    AdvisoryPackage findAdvisoryPackage(Subject overlord, int advId, int pkgVerId);

    /**
     * find AdvisoryBuglist object for given advId and buginfo
     * @param subject
     * @param advId
     * @param buginfo
     * @return
     */
    AdvisoryBuglist getAdvisoryBuglist(Subject subject, int advId, String buginfo);
}
