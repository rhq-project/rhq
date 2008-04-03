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
import org.rhq.core.domain.common.composite.IntegerOptionItem;
import org.rhq.core.domain.content.Architecture;
import org.rhq.core.domain.content.ContentRequestStatus;
import org.rhq.core.domain.content.ContentServiceRequest;
import org.rhq.core.domain.content.InstalledPackage;
import org.rhq.core.domain.content.InstalledPackageHistory;
import org.rhq.core.domain.content.PackageInstallationStep;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.composite.LoadedPackageBitsComposite;
import org.rhq.core.domain.content.composite.PackageListItemComposite;
import org.rhq.core.domain.content.composite.PackageVersionComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * EJB interface for operations that support the UI for the content subsystem.
 *
 * @author Jason Dobies
 */
@Local
public interface ContentUIManagerLocal {
    /**
     * This will return a composite object that tells you if the actual content (the "bits") for a particular package
     * version is loaded into inventory or not, and, if the content is loaded, whether or not that content is stored in
     * the database.
     *
     * @param packageVersionId the {@link org.rhq.core.domain.content.PackageVersion} identifier
     * @return indicates if the package version content is loaded and available
     * @see org.rhq.core.domain.content.composite.LoadedPackageBitsComposite
     */
    LoadedPackageBitsComposite getLoadedPackageBitsComposite(int packageVersionId);

    /**
     * Loads the installed package identified by the ID from the database.
     *
     * @param id identifies the installed package
     * @return installed package if one exists; <code>null</code> otherwise
     */
    InstalledPackage getInstalledPackage(int id);

    List<IntegerOptionItem> getInstalledPackageTypes(Subject user, int resourceId);

    List<String> getInstalledPackageVersions(Subject user, int resourceId);

    /**
     * Loads the package type identified by the ID from the database.
     *
     * @param id package type to load
     * @return package type if one exists for the ID; <code>null</code> otherwise
     */
    PackageType getPackageType(int id);

    /**
     * Returns all package types that are available to the specified resource type.
     *
     * @param resourceTypeId identifies the resource type
     * @return set of package types
     */
    List<PackageType> getPackageTypes(int resourceTypeId);

    /**
     * Returns the package type that backs resources of the specified type.
     *
     * @param resourceTypeId identifies the resource type.
     * @return backing package type if one exists; <code>null</code> otherwise
     */
    PackageType getResourceCreationPackageType(int resourceTypeId);

    /**
     * Returns all package types that are available to the specified resource type in a page control.
     *
     * @param resourceTypeId identifies the resource type
     * @param pageControl    paging control
     * @return pagable list of package types
     */
    PageList<PackageType> getPackageTypes(int resourceTypeId, PageControl pageControl);

    /**
     * Returns a list of all content requests made against the specified resource that match the specified status.
     *
     * @param user        the user who is requesting the retrieval
     * @param resourceId  identifies the resource whose requests to retrieve
     * @param status      request status being matched
     * @param pageControl pagination controller
     * @return list of artifact requests for the specified resource
     */
    PageList<ContentServiceRequest> getContentRequestsWithStatus(Subject user, int resourceId,
        ContentRequestStatus status, PageControl pageControl);

    /**
     * Returns a list of all content requests made against the specified resource that do not match the specified
     * status.
     *
     * @param user        the user who is requesting the retrieval
     * @param resourceId  identifies the resource whose requests to retrieve
     * @param status      request status to not match
     * @param pageControl pagination controller
     * @return list of content requests for the specified resource
     */
    PageList<ContentServiceRequest> getContentRequestsWithNotStatus(Subject user, int resourceId,
        ContentRequestStatus status, PageControl pageControl);

    /**
     * Returns a list of all installed packages on the specified resource.
     *
     * @param user        the user who is requesting the retrieval
     * @param resourceId  identifies the resource whose requests to retrieve
     * @param pageControl pagination controller
     * @return pagable list of packages installed on the resource
     */
    PageList<PackageListItemComposite> getInstalledPackages(Subject user, int resourceId, Integer packageTypeFilterId,
        String packageVersionFilter, PageControl pageControl);

    PageList<InstalledPackageHistory> getInstalledPackageHistory(Subject subject, int resourceId, int generalPackageId,
        PageControl pc);

    PageList<PackageVersionComposite> getPackageVersionCompositesByFilter(Subject user, int resourceId, String filter,
        PageControl pc);

    /**
     * Used to retrieve information about a package version to display to a user.
     *
     * @param  user             user who wants to see the information
     * @param  packageVersionId identifies what package version to return info on
     *
     * @return the information on the package version
     */
    PackageVersionComposite loadPackageVersionComposite(Subject user, int packageVersionId);

    /**
     * Used to retrieve information about a package version to display to a user. This call will also load the
     * extra properties configuration object on the package version. 
     *
     * @param  user             user who wants to see the information
     * @param  packageVersionId identifies what package version to return info on
     *
     * @return the information on the package version
     */
    PackageVersionComposite loadPackageVersionCompositeWithExtraProperties(Subject user, int packageVersionId);

    /**
     * Used to retrieve information about multiple packages to display to the user.
     *
     * @param user              user who wants to see the information
     * @param packageVersionIds identifies what package versions to return info on
     *
     * @return package version information for each package identified
     */
    List<PackageVersionComposite> getPackageVersionComposites(Subject user, int[] packageVersionIds);

    /**
     * Used to retrieve information about multiple packages to display to the user.
     *
     * @param user              user who wants to see the information
     * @param packageVersionIds identifies what package versions to return info on
     * @param pageControl       pagination controller
     *
     * @return package version information for each package identified
     */
    PageList<PackageVersionComposite> getPackageVersionComposites(Subject user, int[] packageVersionIds,
        PageControl pageControl);

    /**
     * Returns all architectures known to the system.
     *
     * @return list of all architectures in the database
     */
    List<Architecture> getArchitectures();

    /**
     * Retrieves a package version by its ID. One and only one package version must exist for the ID; an error
     * will be thrown if exactly one package version is not found.
     *
     * @param packageVersionId identifies the package version
     * @return package version entity
     */
    PackageVersion getPackageVersion(int packageVersionId);

    /**
     * Retrieves a content request by its ID. One and only one content request must exist for the ID; an error
     * will be thrown if exactly one content request is not found. 
     *
     * @param requestId identifies the request
     * @return content request entity
     */
    ContentServiceRequest getContentServiceRequest(int requestId);

    /**
     * Returns a list of installed package history entries that were created as a result of executing the indicated
     * request.
     *
     * @param contentServiceRequestId identifies the request that caused the history entries
     * @param pc                      pagination controller
     * @return list of history entries
     */
    PageList<InstalledPackageHistory> getInstalledPackageHistory(int contentServiceRequestId, PageControl pc);

    /**
     * Retrieves a specific history entry by its ID. This call will also load the list of installation steps and
     * their results if they exist on the entry. One and only one history entry must exist for the ID; an error
     * will be thrown if exactly one history entry is not found.
     *
     * @param historyId identifies the history entry
     * @return history entry
     */
    InstalledPackageHistory getInstalledPackageHistoryWithSteps(int historyId);

    /**
     * Retrieves a specific step entry by its ID.
     *
     * @param stepId identifies the step to retrieve
     * @return step entry
     */
    PackageInstallationStep getPackageInstallationStep(int stepId);
}