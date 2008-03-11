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
import java.util.ArrayList;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.composite.IntegerOptionItem;
import org.rhq.core.domain.content.ContentRequestStatus;
import org.rhq.core.domain.content.ContentServiceRequest;
import org.rhq.core.domain.content.InstalledPackage;
import org.rhq.core.domain.content.PackageBits;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.InstalledPackageHistory;
import org.rhq.core.domain.content.Architecture;
import org.rhq.core.domain.content.PackageInstallationStep;
import org.rhq.core.domain.content.composite.LoadedPackageBitsComposite;
import org.rhq.core.domain.content.composite.PackageListItemComposite;
import org.rhq.core.domain.content.composite.PackageVersionComposite;
import org.rhq.core.domain.util.OrderingField;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.enterprise.server.RHQConstants;

/**
 * @author Jason Dobies
 */
@Stateless
public class ContentUIManagerBean implements ContentUIManagerLocal {
    // Attributes  --------------------------------------------

    private final Log log = LogFactory.getLog(this.getClass());

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    // ContentUIManagerLocal Implementation  --------------------------------------------

    public LoadedPackageBitsComposite getLoadedPackageBitsComposite(int packageVersionId) {
        Query query = entityManager.createNamedQuery(PackageBits.QUERY_PACKAGE_BITS_LOADED_STATUS_PACKAGE_VERSION_ID);
        query.setParameter("id", packageVersionId);
        LoadedPackageBitsComposite composite = (LoadedPackageBitsComposite) query.getSingleResult();
        return composite;
    }

    public InstalledPackage getInstalledPackage(int id) {
        InstalledPackage installedPackage = entityManager.find(InstalledPackage.class, id);
        installedPackage.getPackageVersion().getGeneralPackage().getPackageType()
            .getDeploymentConfigurationDefinition().getPropertyDefinitions();
        return installedPackage;
    }

    public PackageType getPackageType(int id) {
        PackageType packageType = entityManager.find(PackageType.class, id);
        return packageType;
    }

    @SuppressWarnings("unchecked")
    public List<PackageType> getPackageTypes(int resourceTypeId) {
        OrderingField orderingField = new OrderingField("pt.displayName", PageOrdering.ASC);

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            PackageType.QUERY_FIND_BY_RESOURCE_TYPE_ID, orderingField);
        query.setParameter("typeId", resourceTypeId);

        List<PackageType> packageList = query.getResultList();
        return packageList;
    }

    public PackageType getResourceCreationPackageType(int resourceTypeId) {
        Query query = entityManager.createNamedQuery(PackageType.QUERY_FIND_BY_RESOURCE_TYPE_ID_AND_CREATION_FLAG);
        query.setParameter("typeId", resourceTypeId);

        PackageType packageType = (PackageType) query.getSingleResult();
        return packageType;
    }

    @SuppressWarnings("unchecked")
    public PageList<PackageType> getPackageTypes(int resourceTypeId, PageControl pageControl) {
        pageControl.setPrimarySort("pt.name", PageOrdering.ASC);

        Query queryCount = PersistenceUtility.createCountQuery(entityManager,
            PackageType.QUERY_FIND_BY_RESOURCE_TYPE_ID);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            PackageType.QUERY_FIND_BY_RESOURCE_TYPE_ID, pageControl);

        queryCount.setParameter("typeId", resourceTypeId);
        query.setParameter("typeId", resourceTypeId);

        long totalCount = (Long) queryCount.getSingleResult();
        List<PackageType> types = query.getResultList();

        return new PageList<PackageType>(types, (int) totalCount, pageControl);
    }

    @SuppressWarnings("unchecked")
    public PageList<ContentServiceRequest> getContentRequestsWithStatus(Subject user, int resourceId,
                                                                        ContentRequestStatus status, PageControl pageControl) {
        pageControl.initDefaultOrderingField("csr.ctime", PageOrdering.DESC);

        Query queryCount = PersistenceUtility.createCountQuery(entityManager,
            ContentServiceRequest.QUERY_FIND_BY_RESOURCE_WITH_STATUS);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            ContentServiceRequest.QUERY_FIND_BY_RESOURCE_WITH_STATUS, pageControl);

        queryCount.setParameter("resourceId", resourceId);
        queryCount.setParameter("status", status);

        query.setParameter("resourceId", resourceId);
        query.setParameter("status", status);

        long totalCount = (Long) queryCount.getSingleResult();
        List<ContentServiceRequest> requests = query.getResultList();

        return new PageList<ContentServiceRequest>(requests, (int) totalCount, pageControl);
    }

    @SuppressWarnings("unchecked")
    public PageList<ContentServiceRequest> getContentRequestsWithNotStatus(Subject user, int resourceId,
                                                                           ContentRequestStatus status, PageControl pageControl) {
        pageControl.initDefaultOrderingField("csr.id", PageOrdering.DESC);

        Query queryCount = PersistenceUtility.createCountQuery(entityManager,
            ContentServiceRequest.QUERY_FIND_BY_RESOURCE_WITH_NOT_STATUS);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            ContentServiceRequest.QUERY_FIND_BY_RESOURCE_WITH_NOT_STATUS, pageControl);

        queryCount.setParameter("resourceId", resourceId);
        queryCount.setParameter("status", status);

        query.setParameter("resourceId", resourceId);
        query.setParameter("status", status);

        long totalCount = (Long) queryCount.getSingleResult();
        List<ContentServiceRequest> requests = query.getResultList();

        return new PageList<ContentServiceRequest>(requests, (int) totalCount, pageControl);
    }

    @SuppressWarnings("unchecked")
    public PageList<PackageListItemComposite> getInstalledPackages(Subject user, int resourceId,
                                                                   String packageTypeFilter, String packageVersionFilter, PageControl pageControl) {
        pageControl.initDefaultOrderingField("gp.name", PageOrdering.ASC);

        Query queryCount = PersistenceUtility.createCountQuery(entityManager,
            InstalledPackage.QUERY_FIND_PACKAGE_LIST_ITEM_COMPOSITE);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            InstalledPackage.QUERY_FIND_PACKAGE_LIST_ITEM_COMPOSITE, pageControl);

        queryCount.setParameter("resourceId", resourceId);
        query.setParameter("resourceId", resourceId);

        Integer packageTypeFilterId = packageTypeFilter == null ? null : Integer.parseInt(packageTypeFilter);
        queryCount.setParameter("packageTypeFilterId", packageTypeFilterId);
        query.setParameter("packageTypeFilterId", packageTypeFilterId);

        queryCount.setParameter("packageVersionFilter", packageVersionFilter);
        query.setParameter("packageVersionFilter", packageVersionFilter);

        long totalCount = (Long) queryCount.getSingleResult();
        List<PackageListItemComposite> packages = query.getResultList();

        return new PageList<PackageListItemComposite>(packages, (int) totalCount, pageControl);
    }

    @SuppressWarnings("unchecked")
    public List<IntegerOptionItem> getInstalledPackageTypes(Subject user, int resourceId) {
        Query query = entityManager.createNamedQuery(InstalledPackage.QUERY_FIND_PACKAGE_LIST_TYPES);
        query.setParameter("resourceId", resourceId);

        List<IntegerOptionItem> packages = query.getResultList();
        return packages;
    }

    @SuppressWarnings("unchecked")
    public List<String> getInstalledPackageVersions(Subject user, int resourceId) {
        Query query = entityManager.createNamedQuery(InstalledPackage.QUERY_FIND_PACKAGE_LIST_VERSIONS);
        query.setParameter("resourceId", resourceId);

        List<String> packages = query.getResultList();
        return packages;
    }

    @SuppressWarnings("unchecked")
    public PageList<InstalledPackageHistory> getInstalledPackageHistory(Subject subject, int resourceId,
                                                                        int generalPackageId, PageControl pageControl) {
        pageControl.initDefaultOrderingField("iph.timestamp", PageOrdering.DESC);

        Query queryCount = PersistenceUtility.createCountQuery(entityManager,
            InstalledPackageHistory.QUERY_FIND_BY_RESOURCE_ID_AND_PKG_ID);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            InstalledPackageHistory.QUERY_FIND_BY_RESOURCE_ID_AND_PKG_ID, pageControl);

        query.setParameter("resourceId", resourceId);
        queryCount.setParameter("resourceId", resourceId);
        query.setParameter("packageId", generalPackageId);
        queryCount.setParameter("packageId", generalPackageId);

        long totalCount = (Long) queryCount.getSingleResult();
        List<InstalledPackageHistory> packages = query.getResultList();

        return new PageList<InstalledPackageHistory>(packages, (int) totalCount, pageControl);
    }

    @SuppressWarnings("unchecked")
    public PageList<PackageVersionComposite> getPackageVersionCompositesByFilter(Subject user, int resourceId,
                                                                                 String filter, PageControl pc) {
        pc.initDefaultOrderingField("pv.generalPackage.name", PageOrdering.ASC);

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            PackageVersion.QUERY_FIND_COMPOSITE_BY_FILTERS, pc);
        Query queryCount = PersistenceUtility.createCountQuery(entityManager,
            PackageVersion.QUERY_FIND_COMPOSITE_BY_FILTERS);

        query.setParameter("resourceId", resourceId);
        queryCount.setParameter("resourceId", resourceId);

        query.setParameter("filter", filter);
        queryCount.setParameter("filter", filter);

        long count = (Long) queryCount.getSingleResult();
        List<PackageVersionComposite> results = query.getResultList();

        return new PageList<PackageVersionComposite>(results, (int) count, pc);
    }

    public PackageVersionComposite loadPackageVersionComposite(Subject user, int packageVersionId) {
        Query q = entityManager.createNamedQuery(PackageVersion.QUERY_FIND_COMPOSITE_BY_ID);
        q.setParameter("id", packageVersionId);
        PackageVersionComposite pv = (PackageVersionComposite) q.getSingleResult();
        return pv;
    }

    public List<PackageVersionComposite> getPackageVersionComposites(Subject user, int[] packageVersionIds) {
        List<Integer> iPackageVersionIds = new ArrayList<Integer>(packageVersionIds.length);
        for (int i : packageVersionIds) {
            iPackageVersionIds.add(i);
        }

        Query q = entityManager.createNamedQuery(PackageVersion.QUERY_FIND_COMPOSITES_BY_IDS);
        q.setParameter("ids", iPackageVersionIds);
        List<PackageVersionComposite> results = q.getResultList();
        
        return results;
    }

    public PageList<PackageVersionComposite> getPackageVersionComposites(Subject user, int[] packageVersionIds,
                                                                         PageControl pageControl) {
        List<Integer> iPackageVersionIds = new ArrayList<Integer>(packageVersionIds.length);
        for (int i : packageVersionIds) {
            iPackageVersionIds.add(i);
        }

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            PackageVersion.QUERY_FIND_COMPOSITES_BY_IDS, pageControl);
        Query queryCount = PersistenceUtility.createCountQuery(entityManager,
            PackageVersion.QUERY_FIND_COMPOSITES_BY_IDS);

        query.setParameter("ids", iPackageVersionIds);
        queryCount.setParameter("ids", iPackageVersionIds);

        long count = (Long) queryCount.getSingleResult();
        List<PackageVersionComposite> results = query.getResultList();

        return new PageList<PackageVersionComposite>(results, (int) count, pageControl);
    }

    @SuppressWarnings("unchecked")
    public List<Architecture> getArchitectures() {
        Query q = entityManager.createNamedQuery(Architecture.QUERY_FIND_ALL);
        List<Architecture> architectures = q.getResultList();
        return architectures;
    }

    public PackageVersion getPackageVersion(int packageVersionId) {
        Query q = entityManager.createNamedQuery(PackageVersion.QUERY_FIND_BY_ID);
        q.setParameter("id", packageVersionId);
        PackageVersion pv = (PackageVersion)q.getSingleResult();
        return pv;
    }

    public ContentServiceRequest getContentServiceRequest(int requestId) {
        Query q = entityManager.createNamedQuery(ContentServiceRequest.QUERY_FIND_BY_ID);
        q.setParameter("id", requestId);
        ContentServiceRequest csr = (ContentServiceRequest)q.getSingleResult();
        return csr;
    }

    public PageList<InstalledPackageHistory> getInstalledPackageHistory(int contentServiceRequestId, PageControl pc) {
        pc.initDefaultOrderingField("iph.timestamp", PageOrdering.DESC);

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            InstalledPackageHistory.QUERY_FIND_BY_CSR_ID, pc);
        Query queryCount = PersistenceUtility.createCountQuery(entityManager,
            InstalledPackageHistory.QUERY_FIND_BY_CSR_ID);

        query.setParameter("contentServiceRequestId", contentServiceRequestId);
        queryCount.setParameter("contentServiceRequestId", contentServiceRequestId);

        long totalCount = (Long) queryCount.getSingleResult();
        List<InstalledPackageHistory> packages = query.getResultList();

        return new PageList<InstalledPackageHistory>(packages, (int) totalCount, pc);
    }

    public InstalledPackageHistory getInstalledPackageHistoryWithSteps(int historyId) {
        Query query = entityManager.createNamedQuery(InstalledPackageHistory.QUERY_FIND_BY_ID_WITH_STEPS);
        query.setParameter("id", historyId);
        InstalledPackageHistory history = (InstalledPackageHistory)query.getSingleResult();
        return history;
    }

    public PackageInstallationStep getPackageInstallationStep(int stepId) {
        PackageInstallationStep step = entityManager.find(PackageInstallationStep.class, stepId);
        return step;
    }
}