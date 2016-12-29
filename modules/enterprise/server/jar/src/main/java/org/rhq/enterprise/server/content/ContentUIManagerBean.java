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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.composite.IntegerOptionItem;
import org.rhq.core.domain.content.Advisory;
import org.rhq.core.domain.content.ContentRequestStatus;
import org.rhq.core.domain.content.ContentServiceRequest;
import org.rhq.core.domain.content.InstalledPackage;
import org.rhq.core.domain.content.InstalledPackageHistory;
import org.rhq.core.domain.content.PackageBits;
import org.rhq.core.domain.content.PackageInstallationStep;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.composite.AdvisoryDetailsComposite;
import org.rhq.core.domain.content.composite.LoadedPackageBitsComposite;
import org.rhq.core.domain.content.composite.PackageListItemComposite;
import org.rhq.core.domain.content.composite.PackageVersionComposite;
import org.rhq.core.domain.criteria.InstalledPackageHistoryCriteria;
import org.rhq.core.domain.server.PersistenceUtility;
import org.rhq.core.domain.util.OrderingField;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;

/**
 * @author Jason Dobies
 */
@Stateless
public class ContentUIManagerBean implements ContentUIManagerLocal {
    // Attributes  --------------------------------------------

    @SuppressWarnings("unused")
    private final Log log = LogFactory.getLog(this.getClass());

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;

    // ContentUIManagerLocal Implementation  --------------------------------------------

    public LoadedPackageBitsComposite getLoadedPackageBitsComposite(int packageVersionId) {
        Query query = entityManager.createNamedQuery(PackageBits.QUERY_PACKAGE_BITS_LOADED_STATUS_PACKAGE_VERSION_ID);
        query.setParameter("id", packageVersionId);
        LoadedPackageBitsComposite composite = (LoadedPackageBitsComposite) query.getSingleResult();
        return composite;
    }

    public InstalledPackage getInstalledPackage(int id) {
        InstalledPackage installedPackage = entityManager.find(InstalledPackage.class, id);
        return installedPackage;
    }

    public PackageType getPackageType(int id) {
        PackageType packageType = entityManager.find(PackageType.class, id);
        return packageType;
    }

    public List<PackageType> getPackageTypes() {
        OrderingField orderingField = new OrderingField("pt.displayName", PageOrdering.ASC);

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            PackageType.QUERY_FIND_ALL, orderingField);

        @SuppressWarnings("unchecked")
        List<PackageType> packageList = (List<PackageType>) query.getResultList();
        
        return packageList;
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
        Integer packageTypeFilterId, String packageVersionFilter, String search, PageControl pageControl) {
        pageControl.initDefaultOrderingField("gp.name", PageOrdering.ASC);

        Query queryCount = PersistenceUtility.createCountQuery(entityManager,
            InstalledPackage.QUERY_FIND_PACKAGE_LIST_ITEM_COMPOSITE);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            InstalledPackage.QUERY_FIND_PACKAGE_LIST_ITEM_COMPOSITE, pageControl);

        queryCount.setParameter("resourceId", resourceId);
        query.setParameter("resourceId", resourceId);

        queryCount.setParameter("packageTypeFilterId", packageTypeFilterId);
        query.setParameter("packageTypeFilterId", packageTypeFilterId);

        queryCount.setParameter("packageVersionFilter", packageVersionFilter);
        query.setParameter("packageVersionFilter", packageVersionFilter);

        if (search != null) {
            search = "%" + search.toUpperCase() + "%";
        }

        queryCount.setParameter("search", search);
        query.setParameter("search", search);

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
        PageControl unlimitedpc = PageControl.getUnlimitedInstance();
        unlimitedpc.initDefaultOrderingField("pv.generalPackage.name", PageOrdering.ASC);

        Query queryInstalled = PersistenceUtility.createQueryWithOrderBy(entityManager,
            InstalledPackage.QUERY_FIND_PACKAGE_LIST_ITEM_COMPOSITE, unlimitedpc);

        queryInstalled.setParameter("resourceId", resourceId);
        queryInstalled.setParameter("packageTypeFilterId", null);
        queryInstalled.setParameter("packageVersionFilter", null);
        queryInstalled.setParameter("search", null);

        List<PackageListItemComposite> packagesInstalled = queryInstalled.getResultList();

        Map<String, String> installedPackages = new HashMap<String, String>();

        for (PackageListItemComposite packageInstalled : packagesInstalled) {
            installedPackages.put(packageInstalled.getPackageName(), packageInstalled.getVersion());
        }

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            PackageVersion.QUERY_FIND_COMPOSITE_BY_FILTERS, unlimitedpc);
        Query queryCount = PersistenceUtility.createCountQuery(entityManager,
            PackageVersion.QUERY_FIND_COMPOSITE_BY_FILTERS);

        query.setParameter("resourceId", resourceId);
        queryCount.setParameter("resourceId", resourceId);

        query.setParameter("filter", filter);
        queryCount.setParameter("filter", filter);

        long count = (Long) queryCount.getSingleResult();
        List<PackageVersionComposite> results = query.getResultList();
        List<PackageVersionComposite> modifiedResults = new ArrayList<PackageVersionComposite>();

        for (PackageVersionComposite result : results) {
            String installedVersion = installedPackages.get(result.getPackageName());
            if (installedVersion != null && installedVersion.equals(result.getPackageVersion().getVersion())) {
                if (count > 0)
                    count--;
            } else {
                modifiedResults.add(result);
            }
        }

        Collections.sort(modifiedResults, new Comparator() {

            public int compare(Object o1, Object o2) {
                PackageVersionComposite p1 = (PackageVersionComposite) o1;
                PackageVersionComposite p2 = (PackageVersionComposite) o2;
                return p1.getPackageName().compareToIgnoreCase(p2.getPackageName());
            }
        });

        // Trim modifiedResults to match pc.
        modifiedResults = modifiedResults.subList(
                Math.min(pc.getStartRow(), modifiedResults.size()),
                Math.min(pc.getStartRow() + pc.getPageSize(), modifiedResults.size())
        );

        return new PageList<PackageVersionComposite>(modifiedResults, (int) count, pc);
    }

    @SuppressWarnings("unchecked")
    public PageList<PackageVersionComposite> getUpdatePackageVersionCompositesByFilter(Subject user, int resourceId,
        String filter, PageControl pc) {
        pc.initDefaultOrderingField("pv.generalPackage.name", PageOrdering.ASC);
        PageControl unlimitedpc = PageControl.getUnlimitedInstance();

        Query queryInstalled = PersistenceUtility.createQueryWithOrderBy(entityManager,
            InstalledPackage.QUERY_FIND_PACKAGE_LIST_ITEM_COMPOSITE, unlimitedpc);

        queryInstalled.setParameter("resourceId", resourceId);
        queryInstalled.setParameter("packageTypeFilterId", null);
        queryInstalled.setParameter("packageVersionFilter", null);
        queryInstalled.setParameter("search", null);

        List<PackageListItemComposite> packagesInstalled = queryInstalled.getResultList();

        Map<String, String> installedPackageNameAndVersion = new HashMap<String, String>();
        for (PackageListItemComposite packageInstalled : packagesInstalled) {
            installedPackageNameAndVersion.put(packageInstalled.getPackageName(), packageInstalled.getVersion());
        }

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            PackageVersion.QUERY_FIND_COMPOSITE_BY_FILTERS, unlimitedpc);
        query.setParameter("resourceId", resourceId);
        query.setParameter("filter", filter);

        List<PackageVersionComposite> results = query.getResultList();
        List<PackageVersionComposite> modifiedResults = new ArrayList<PackageVersionComposite>();
        String packageName = new String();

        for (PackageVersionComposite result : results) {
            if (installedPackageNameAndVersion.get(result.getPackageName()) != null) {

                packageName = result.getPackageName();

                if (installedPackageNameAndVersion.get(packageName).compareTo(result.getPackageVersion().getVersion()) < 0) {
                    modifiedResults.add(result);
                }
            }
        }

        List<PackageVersionComposite> testResults = new ArrayList<PackageVersionComposite>();
        for (PackageVersionComposite result : modifiedResults) {
            testResults.add(result);
        }

        List<PackageVersionComposite> latestResults = new ArrayList<PackageVersionComposite>();

        PackageVersionComposite latestPackage = null;

        for (PackageVersionComposite newPackage : modifiedResults) {
            latestPackage = newPackage;

            for (PackageVersionComposite pack : testResults) {
                if (pack.getPackageName().equals(latestPackage.getPackageName())
                    && latestPackage.getPackageVersion().getVersion().compareTo(pack.getPackageVersion().getVersion()) < 0) {
                    latestPackage = pack;
                }
            }
            latestResults.add(latestPackage);
        }

        List<PackageVersionComposite> finalResults = new ArrayList<PackageVersionComposite>();

        long count = 0;
        for (PackageVersionComposite pack : latestResults) {
            if (finalResults.contains(pack)) {
                continue;
            } else {
                finalResults.add(pack);
                count++;
            }
        }

        Collections.sort(finalResults, new Comparator() {

            public int compare(Object o1, Object o2) {
                PackageVersionComposite p1 = (PackageVersionComposite) o1;
                PackageVersionComposite p2 = (PackageVersionComposite) o2;
                return p1.getPackageName().compareToIgnoreCase(p2.getPackageName());
            }
        });

        // Trim finalResults to match pc.
        finalResults = finalResults.subList(
                Math.min(pc.getStartRow(), finalResults.size()),
                Math.min(pc.getStartRow() + pc.getPageSize(), finalResults.size())
        );

        return new PageList<PackageVersionComposite>(finalResults, (int) count, pc);
    }

    public PackageVersionComposite loadPackageVersionComposite(Subject user, int packageVersionId) {
        Query q = entityManager.createNamedQuery(PackageVersion.QUERY_FIND_COMPOSITE_BY_ID);
        q.setParameter("id", packageVersionId);
        PackageVersionComposite pv = (PackageVersionComposite) q.getSingleResult();
        return pv;
    }

    public PackageVersionComposite loadPackageVersionCompositeWithExtraProperties(Subject user, int packageVersionId) {
        Query q = entityManager.createNamedQuery(PackageVersion.QUERY_FIND_COMPOSITE_BY_ID_WITH_PROPS);
        q.setParameter("id", packageVersionId);
        PackageVersionComposite pv = (PackageVersionComposite) q.getSingleResult();
        return pv;
    }

    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
    public AdvisoryDetailsComposite loadAdvisoryDetailsComposite(Subject user, Integer advisoryId) {
        Query q = entityManager.createNamedQuery(Advisory.QUERY_FIND_COMPOSITE_BY_ID);
        q.setParameter("id", advisoryId);
        AdvisoryDetailsComposite results = (AdvisoryDetailsComposite) q.getSingleResult();

        return results;
    }

    @SuppressWarnings("unchecked")
    public PageList<PackageVersionComposite> getPackageVersionComposites(Subject user, int[] packageVersionIds,
        PageControl pageControl) {
        pageControl.initDefaultOrderingField("pv.id");

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

    public PackageVersion getPackageVersion(int packageVersionId) {
        Query q = entityManager.createNamedQuery(PackageVersion.QUERY_FIND_BY_ID);
        q.setParameter("id", packageVersionId);
        PackageVersion pv = (PackageVersion) q.getSingleResult();
        return pv;
    }

    public ContentServiceRequest getContentServiceRequest(int requestId) {
        Query q = entityManager.createNamedQuery(ContentServiceRequest.QUERY_FIND_BY_ID);
        q.setParameter("id", requestId);
        ContentServiceRequest csr = (ContentServiceRequest) q.getSingleResult();
        return csr;
    }

    @SuppressWarnings("unchecked")
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

    public InstalledPackageHistory getInstalledPackageHistory(int historyId) {
        Query query = entityManager.createNamedQuery(InstalledPackageHistory.QUERY_FIND_BY_ID);
        query.setParameter("id", historyId);
        InstalledPackageHistory history = (InstalledPackageHistory) query.getSingleResult();
        return history;
    }

    @SuppressWarnings("unchecked")
    public List<PackageInstallationStep> getPackageInstallationSteps(int installedPackageHistoryId) {
        Query query = entityManager
            .createNamedQuery(PackageInstallationStep.QUERY_FIND_BY_INSTALLED_PACKAGE_HISTORY_ID);
        query.setParameter("installedPackageHistoryId", installedPackageHistoryId);
        List<PackageInstallationStep> steps = query.getResultList();
        return steps;
    }

    public PackageInstallationStep getPackageInstallationStep(int stepId) {
        PackageInstallationStep step = entityManager.find(PackageInstallationStep.class, stepId);
        return step;
    }

    @SuppressWarnings("unchecked")
    public PageList<InstalledPackageHistory> getInstalledPackageHistoryForResource(int resourceId, PageControl pc) {
        pc.initDefaultOrderingField("iph.timestamp", PageOrdering.DESC);

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            InstalledPackageHistory.QUERY_FIND_BY_RESOURCE_ID, pc);
        Query queryCount = PersistenceUtility.createCountQuery(entityManager,
            InstalledPackageHistory.QUERY_FIND_BY_RESOURCE_ID);

        query.setParameter("resourceId", resourceId);
        queryCount.setParameter("resourceId", resourceId);

        long totalCount = (Long) queryCount.getSingleResult();
        List<InstalledPackageHistory> packages = query.getResultList();

        return new PageList<InstalledPackageHistory>(packages, (int) totalCount, pc);
    }

    @SuppressWarnings("unchecked")
    public PageList<InstalledPackageHistory> findInstalledPackageHistoryByCriteria(Subject subject,
        InstalledPackageHistoryCriteria criteria) {

        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        ;

        if (!authorizationManager.isInventoryManager(subject)) {
            // Ensure we limit to packages installed to viewable resources
            generator.setAuthorizationResourceFragment(CriteriaQueryGenerator.AuthorizationTokenType.RESOURCE,
                "resource", subject.getId());
        }

        CriteriaQueryRunner<InstalledPackageHistory> queryRunner = new CriteriaQueryRunner(criteria, generator,
            entityManager);

        return queryRunner.execute();

    }
}