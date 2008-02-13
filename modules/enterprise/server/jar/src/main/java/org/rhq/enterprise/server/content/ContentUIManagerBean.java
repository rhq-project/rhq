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
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.ContentRequestStatus;
import org.rhq.core.domain.content.ContentServiceRequest;
import org.rhq.core.domain.content.InstalledPackage;
import org.rhq.core.domain.content.PackageBits;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.composite.LoadedPackageBitsComposite;
import org.rhq.core.domain.content.composite.PackageListItemComposite;
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
        pageControl.setPrimarySort("at.name", PageOrdering.ASC);

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
    public PageList<PackageListItemComposite> getInstalledPackages(Subject user, int resourceId, PageControl pageControl) {
        pageControl.initDefaultOrderingField("gp.name", PageOrdering.ASC);

        Query queryCount = PersistenceUtility.createCountQuery(entityManager,
            InstalledPackage.QUERY_FIND_PACKAGE_LIST_ITEM_COMPOSITE);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            InstalledPackage.QUERY_FIND_PACKAGE_LIST_ITEM_COMPOSITE, pageControl);

        queryCount.setParameter("resourceId", resourceId);
        query.setParameter("resourceId", resourceId);

        long totalCount = (Long) queryCount.getSingleResult();
        List<PackageListItemComposite> packages = query.getResultList();

        return new PageList<PackageListItemComposite>(packages, (int) totalCount, pageControl);
    }

    public PageList<InstalledPackage> getInstalledPackageHistory(Subject subject, int resourceId, int generalPackageId,
        PageControl pageControl) {
        pageControl.initDefaultOrderingField("gp.name", PageOrdering.ASC);

        Query queryCount = PersistenceUtility.createCountQuery(entityManager,
            InstalledPackage.QUERY_FIND_INSTALLED_PACKAGE_HISTORY);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            InstalledPackage.QUERY_FIND_INSTALLED_PACKAGE_HISTORY, pageControl);

        query.setParameter("resourceId", resourceId);
        queryCount.setParameter("resourceId", resourceId);
        query.setParameter("generalPackageId", generalPackageId);
        queryCount.setParameter("generalPackageId", generalPackageId);

        long totalCount = (Long) queryCount.getSingleResult();
        List<InstalledPackage> packages = query.getResultList();

        return new PageList<InstalledPackage>(packages, (int) totalCount, pageControl);
    }
}