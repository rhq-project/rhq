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
package org.rhq.enterprise.server.authz;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.Stateless;
import javax.interceptor.ExcludeDefaultInterceptors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Permission.Target;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.server.RHQConstants;

/**
 * @author Joseph Marques
 */
// we exclude the default interceptors because the required permissions interceptor calls into some of these
// and other methods that take a Subject may not have a session attached to that Subject
@ExcludeDefaultInterceptors
@Stateless
public class AuthorizationManagerBean implements AuthorizationManagerLocal {

    private static final int SUBJECT_ID_OVERLORD = 1;
    private static final int SUBJECT_ID_RHQADMIN = 2;

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public Set<Permission> getExplicitGlobalPermissions(Subject subject) {
        Query query = entityManager.createNamedQuery(Subject.QUERY_GET_GLOBAL_PERMISSIONS);
        query.setParameter("subject", subject);
        List<Permission> intermediate = query.getResultList();
        Set<Permission> results = new HashSet<Permission>();
        for (Permission permission : intermediate) {
            if (permission.getTarget() == Target.GLOBAL) {
                results.add(permission);
            }
        }

        return results;
    }

    @SuppressWarnings("unchecked")
    public Set<Permission> getExplicitGroupPermissions(Subject subject, int groupId) {
        Set<Permission> result = new HashSet<Permission>();

        ResourceGroup group = entityManager.find(ResourceGroup.class, groupId);
        Subject owner = group.getSubject();

        if (null == owner) {
            // role-owned group
            Query query = entityManager.createNamedQuery(Subject.QUERY_GET_PERMISSIONS_BY_GROUP_ID);
            query.setParameter("subject", subject);
            query.setParameter("groupId", groupId);
            List<Permission> resultList = query.getResultList();
            for (Permission permission : resultList) {
                result.add(permission);
            }

        } else {
            // don't let a user other than the owner do anything with this group
            if (subject.equals(owner)) {
                Query query = entityManager.createNamedQuery(Subject.QUERY_GET_PERMISSIONS_BY_PRIVATE_GROUP_ID);
                query.setParameter("subjectId", subject.getId());
                query.setParameter("privateGroupId", groupId);
                List<Object[]> resultList = query.getResultList();
                for (Object[] row : resultList) {
                    result.add((Permission) row[0]);
                }
            }
        }

        return result;
    }

    public Set<Permission> getImplicitGroupPermissions(Subject subject, int groupId) {
        Set<Permission> permissions = isInventoryManager(subject) ? Permission.RESOURCE_ALL
            : getExplicitGroupPermissions(subject, groupId);
        return permissions;
    }

    @SuppressWarnings("unchecked")
    public Set<Permission> getExplicitResourcePermissions(Subject subject, int resourceId) {
        Query query = entityManager.createNamedQuery(Subject.QUERY_GET_PERMISSIONS_BY_RESOURCE_ID);
        query.setParameter("subject", subject);
        query.setParameter("resourceId", resourceId);
        List<Permission> intermediate = query.getResultList();
        Set<Permission> results = new HashSet<Permission>();
        for (Permission permission : intermediate) {
            results.add(permission);
        }

        return results;
    }

    public Set<Permission> getImplicitResourcePermissions(Subject subject, int resourceId) {
        Set<Permission> permissions = isInventoryManager(subject) ? Permission.RESOURCE_ALL
            : getExplicitResourcePermissions(subject, resourceId);
        return permissions;
    }

    public boolean hasGlobalPermission(Subject subject, Permission permission) {
        if (isOverlord(subject)) {
            return true;
        }

        Query query = entityManager.createNamedQuery(Subject.QUERY_HAS_GLOBAL_PERMISSION);
        query.setParameter("subject", subject);
        query.setParameter("permission", permission);
        long count = (Long) query.getSingleResult();
        return (count != 0);
    }

    @SuppressWarnings("unchecked")
    public boolean hasGroupPermission(Subject subject, Permission permission, int groupId) {
        if (isInventoryManager(subject)) {
            return true;
        }

        ResourceGroup group = entityManager.find(ResourceGroup.class, groupId);
        Subject owner = group.getSubject();

        if (null == owner) {
            // role-owned group
            Query query = entityManager.createNamedQuery(Subject.QUERY_HAS_GROUP_PERMISSION);
            query.setParameter("subject", subject);
            query.setParameter("permission", permission);
            query.setParameter("groupId", groupId);
            long count = (Long) query.getSingleResult();
            return (count != 0);

        } else {
            // don't let a user other than the owner do anything with this group
            if (!subject.equals(owner)) {
                return false;
            }

            Query query = entityManager.createNamedQuery(Subject.QUERY_HAS_PRIVATE_GROUP_PERMISSION);
            query.setParameter("subjectId", subject.getId());
            query.setParameter("permission", permission);
            query.setParameter("privateGroupId", groupId);
            List<Object[]> resultList = query.getResultList();
            return (!resultList.isEmpty());
        }
    }

    public boolean hasResourcePermission(Subject subject, Permission permission, int resourceId) {
        if (isInventoryManager(subject)) {
            return true;
        }

        Query query = entityManager.createNamedQuery(Subject.QUERY_HAS_RESOURCE_PERMISSION);
        query.setParameter("subject", subject);
        query.setParameter("permission", permission);
        query.setParameter("resourceId", resourceId);
        long count = (Long) query.getSingleResult();
        return (count != 0);
    }

    public boolean hasAutoGroupPermission(Subject subject, Permission permission, int parentResourceId,
        int resourceTypeId) {
        if (isInventoryManager(subject)) {
            return true;
        }

        Query query = entityManager.createNamedQuery(Subject.QUERY_HAS_AUTO_GROUP_PERMISSION);
        query.setParameter("permission", permission);
        query.setParameter("parentResourceId", parentResourceId);
        query.setParameter("resourceTypeId", resourceTypeId);

        query.setParameter("subject", -SUBJECT_ID_OVERLORD);
        long baseCount = (Long) query.getSingleResult();

        query.setParameter("subject", subject);
        long subjectCount = (Long) query.getSingleResult();

        /* 
         * an auto-group is viewable if the count of resources with parent/type filters is identical
         * to the count of those same resources additionally filtered by standard authorization
         *  
         */
        return (baseCount == subjectCount);
    }

    public boolean canViewResource(Subject subject, int resourceId) {
        if (isInventoryManager(subject)) {
            return true;
        }

        Query query = entityManager.createNamedQuery(Subject.QUERY_CAN_VIEW_RESOURCE);
        query.setParameter("subject", subject);
        query.setParameter("resourceId", resourceId);
        long count = (Long) query.getSingleResult();
        return (count != 0);
    }

    public boolean canViewResources(Subject subject, List<Integer> resourceIds) {
        if (isInventoryManager(subject)) {
            return true;
        }

        Query query = entityManager.createNamedQuery(Subject.QUERY_CAN_VIEW_RESOURCES);
        query.setParameter("subject", subject);
        query.setParameter("resourceIds", resourceIds);
        long count = (Long) query.getSingleResult();

        return count == resourceIds.size();
    }

    public boolean canViewGroup(Subject subject, int groupId) {
        if (isInventoryManager(subject)) {
            return true;
        }

        Query query = entityManager.createNamedQuery(Subject.QUERY_CAN_VIEW_GROUP);
        query.setParameter("subject", subject);
        query.setParameter("groupId", groupId);
        long count = (Long) query.getSingleResult();
        return (count != 0);
    }

    public boolean canViewAutoGroup(Subject subject, int parentResourceId, int resourceTypeId) {
        if (isInventoryManager(subject)) {
            return true;
        }

        Query query = entityManager.createNamedQuery(Subject.QUERY_CAN_VIEW_AUTO_GROUP);
        query.setParameter("parentResourceId", parentResourceId);
        query.setParameter("resourceTypeId", resourceTypeId);

        query.setParameter("subject", -SUBJECT_ID_OVERLORD);
        long baseCount = (Long) query.getSingleResult();

        query.setParameter("subject", subject);
        long subjectCount = (Long) query.getSingleResult();

        /* 
         * an auto-group is viewable if the count of resources with parent/type filters is identical
         * to the count of those same resources additionally filtered by standard authorization
         *  
         */
        return (baseCount == subjectCount);
    }

    public boolean isInventoryManager(Subject subject) {
        return hasGlobalPermission(subject, Permission.MANAGE_INVENTORY);
    }

    @SuppressWarnings("unchecked")
    public boolean hasResourcePermission(Subject subject, Permission permission, Collection<Integer> resourceIds) {
        if (isInventoryManager(subject)) {
            return true;
        }

        Query query = entityManager.createNamedQuery(Subject.QUERY_GET_RESOURCES_BY_PERMISSION);
        query.setParameter("subject", subject);
        query.setParameter("permission", permission);

        List<Integer> results = query.getResultList();
        return results.containsAll(resourceIds);
    }

    public boolean isSystemSuperuser(Subject subject) {
        if (subject == null) {
            return false;
        }

        // We know that our overlord is always id=1 and the rhqadmin user is always id=2.
        return ((subject.getId() == SUBJECT_ID_OVERLORD) || (subject.getId() == SUBJECT_ID_RHQADMIN));
    }

    public boolean isOverlord(Subject subject) {
        if (subject == null) {
            return false;
        }

        return (subject.getId() == SUBJECT_ID_OVERLORD);
    }

}