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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.ExcludeDefaultInterceptors;
import javax.jws.WebService;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.core.domain.util.QueryGenerator;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.exception.FetchException;
import org.rhq.enterprise.server.exception.UpdateException;

/**
 * This bean provides functionality to manipulate the security rules. That is, adding/modifying/deleting roles and their
 * associated subjects and permissions are performed by this manager.
 *
 * @author John Mazzitelli
 */
@Stateless
@WebService(endpointInterface = "org.rhq.enterprise.server.authz.RoleManagerRemote")
public class RoleManagerBean implements RoleManagerLocal, RoleManagerRemote {
    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private SubjectManagerLocal subjectManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;

    /**
     * @see org.rhq.enterprise.server.authz.RoleManagerLocal#getRoles(Subject,PageControl)
     */
    @SuppressWarnings("unchecked")
    // the first param, subject, is not the subject making the request, its the subject whose roles are to be returned.
    // therefore, we won't want our security interceptor to check this method since the subject won't have a session associated with it
    @ExcludeDefaultInterceptors
    public PageList<Role> getRoles(int subjectId, PageControl pc) {
        Subject subject = entityManager.find(Subject.class, subjectId); // attach it
        PageList<Role> roles = PersistenceUtility.createPaginationFilter(entityManager, subject.getRoles(), pc);

        if (roles != null) {
            // eagerly load in the members - think about writing a left-join query or use EAGER
            for (Role role : roles) {
                role.getMemberCount();
            }
        }

        return roles;
    }

    /**
     * @see org.rhq.enterprise.server.authz.RoleManagerRemote#getAllRoles(Subject,PageControl)
     */
    @RequiredPermission(Permission.MANAGE_SECURITY)
    public PageList<Role> getAllRoles(Subject subject, PageControl pc) {
        return getAllRoles(pc);
    }

    /**
     * @see org.rhq.enterprise.server.authz.RoleManagerLocal#getAllRoles(PageControl)
     */
    @SuppressWarnings("unchecked")
    public PageList<Role> getAllRoles(PageControl pc) {
        pc.initDefaultOrderingField("r.name");

        String query_name = Role.QUERY_FIND_ALL;
        Query role_query_count = PersistenceUtility.createCountQuery(entityManager, query_name);
        Query role_query = PersistenceUtility.createQueryWithOrderBy(entityManager, query_name, pc);

        long total_count = (Long) role_query_count.getSingleResult();

        List<Role> roles = role_query.getResultList();

        if (roles != null) {
            // eagerly load in the members - can't use left-join due to PersistenceUtility usage; perhaps use EAGER
            for (Role role : roles) {
                role.getMemberCount();
            }
        } else {
            roles = new ArrayList<Role>();
        }

        return new PageList<Role>(roles, (int) total_count, pc);
    }

    /**
     * @see org.rhq.enterprise.server.authz.RoleManagerLocal#createRole(Subject, Role)
     */
    @RequiredPermission(Permission.MANAGE_SECURITY)
    public Role createRole(Subject whoami, Role newRole) {
        processDependentPermissions(newRole);
        entityManager.persist(newRole);
        return newRole;
    }

    /**
     * @see org.rhq.enterprise.server.authz.RoleManagerLocal#deleteRoles(Subject, Integer[])
     */
    @RequiredPermission(Permission.MANAGE_SECURITY)
    public void deleteRoles(Subject whoami, Integer[] doomedRoleIds) {
        if (doomedRoleIds != null) {
            for (Integer role_id : doomedRoleIds) {
                Role doomedRole = entityManager.find(Role.class, role_id);

                Query deleteNotificationQuery = entityManager
                    .createQuery("DELETE FROM RoleNotification rn WHERE rn.role.id = :roleId");
                deleteNotificationQuery.setParameter("roleId", role_id);
                deleteNotificationQuery.executeUpdate(); // discard result, might not have been set for any notifications

                Set<Subject> subjectsToUnhook = new HashSet<Subject>(doomedRole.getSubjects()); // avoid concurrent mod exception
                for (Subject doomedSubjectRelationship : subjectsToUnhook) {
                    doomedRole.removeSubject(doomedSubjectRelationship);
                    entityManager.merge(doomedSubjectRelationship);
                }

                Set<ResourceGroup> groupsToUnhook = new HashSet<ResourceGroup>(doomedRole.getResourceGroups()); // avoid concurrent mod exception
                for (ResourceGroup doomedResourceGroupRelationship : groupsToUnhook) {
                    doomedRole.removeResourceGroup(doomedResourceGroupRelationship);
                    entityManager.merge(doomedResourceGroupRelationship);
                }

                doomedRole = entityManager.merge(doomedRole);

                if (doomedRole.getFsystem()) {
                    throw new PermissionException("You cannot delete an internal system role");
                }

                entityManager.remove(doomedRole); // there should not be any cascading - this does not touch subjects/groups
            }
        }

        return;
    }

    /**
     * @see org.rhq.enterprise.server.authz.RoleManagerLocal#addRolesToSubject(Subject, int, int[])
     */
    @RequiredPermission(Permission.MANAGE_SECURITY)
    public void addRolesToSubject(Subject whoami, int subjectId, int[] roleIds) throws UpdateException {
        if (roleIds != null) {
            Subject subject = subjectManager.getSubjectById(subjectId); // attach it

            if (subject.getFsystem() || (authorizationManager.isSystemSuperuser(subject))) {
                throw new PermissionException("You cannot assign roles to user [" + subject.getName()
                    + "] - roles are fixed for this user");
            }

            for (Integer role_id : roleIds) {
                Role role = entityManager.find(Role.class, role_id);
                if (role != null) {
                    role.addSubject(subject);
                }
            }
        }

        return;
    }

    /**
     * @see org.rhq.enterprise.server.authz.RoleManagerLocal#addSubjectsToRole(Subject, int, int[])
     */
    @RequiredPermission(Permission.MANAGE_SECURITY)
    public void addSubjectsToRole(Subject whoami, int roleId, int[] subjectIds) throws UpdateException {
        if (subjectIds != null) {
            Role role = findRoleById(roleId); // attach it

            for (Integer subject_id : subjectIds) {
                Subject subject = entityManager.find(Subject.class, subject_id);

                if (subject != null) {
                    if (subject.getFsystem() || (authorizationManager.isSystemSuperuser(subject))) {
                        throw new PermissionException("You cannot alter the roles for user [" + subject.getName()
                            + "] - roles are fixed for this user");
                    }

                    role.addSubject(subject);
                }
            }
        }

        return;
    }

    /**
     * @see org.rhq.enterprise.server.authz.RoleManagerLocal#removeRolesFromSubject(Subject, int, int[])
     */
    @RequiredPermission(Permission.MANAGE_SECURITY)
    public void removeRolesFromSubject(Subject whoami, int subjectId, int[] roleIds) throws UpdateException {
        if (roleIds != null) {
            Subject subject = subjectManager.getSubjectById(subjectId); // attach it

            if (subject.getFsystem() || (authorizationManager.isSystemSuperuser(subject))) {
                throw new PermissionException("You cannot remove roles from user [" + subject.getName()
                    + "] - roles are fixed for this user");
            }

            for (Integer role_id : roleIds) {
                Role role = entityManager.find(Role.class, role_id);
                if (role != null) {
                    role.removeSubject(subject);
                }
            }
        }

        return;
    }

    /**
     * @see org.rhq.enterprise.server.authz.RoleManagerLocal#findRoleById(Integer)
     */
    public Role findRoleById(Integer roleId) {
        Role role = entityManager.find(Role.class, roleId);
        return role;
    }

    /**
     * @see org.rhq.enterprise.server.authz.RoleManagerLocal#setPermissions(Subject, Integer, Set)
     */
    @RequiredPermission(Permission.MANAGE_SECURITY)
    public void setPermissions(Subject whoami, Integer roleId, Set<Permission> permissions) {
        Role role = entityManager.find(Role.class, roleId);
        Set<Permission> role_permissions = role.getPermissions();
        role_permissions.clear();
        role_permissions.addAll(permissions);
        entityManager.merge(role);
        entityManager.flush();
        return;
    }

    /**
     * @see org.rhq.enterprise.server.authz.RoleManagerLocal#getPermissions(Integer)
     */
    public Set<Permission> getPermissions(Integer roleId) {
        Role role = entityManager.find(Role.class, roleId);
        Set<Permission> role_permissions = role.getPermissions();
        return role_permissions;
    }

    /**
     * @see org.rhq.enterprise.server.authz.RoleManagerLocal#updateRole(Subject, Role)
     */
    @RequiredPermission(Permission.MANAGE_SECURITY)
    public Role updateRole(Subject whoami, Role role) {
        processDependentPermissions(role);
        return entityManager.merge(role);
    }

    /**
     * @see org.rhq.enterprise.server.authz.RoleManagerRemote#getRoleSubjects(Subject,Integer,PageControl)
     */
    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_SECURITY)
    public PageList<Subject> getRoleSubjects(Subject subject, Integer roleId, PageControl pc) {
        return getRoleSubjects(roleId, pc);
    }

    /**
     * @see org.rhq.enterprise.server.authz.RoleManagerLocal#getRoleSubjects(Integer,PageControl)
     */
    @SuppressWarnings("unchecked")
    public PageList<Subject> getRoleSubjects(Integer roleId, PageControl pc) {
        pc.initDefaultOrderingField("s.name");

        String query_name = Subject.QUERY_GET_SUBJECTS_ASSIGNED_TO_ROLE;
        Query role_query_count = PersistenceUtility.createCountQuery(entityManager, query_name);
        Query role_query = PersistenceUtility.createQueryWithOrderBy(entityManager, query_name, pc);

        role_query_count.setParameter("id", roleId);
        role_query.setParameter("id", roleId);

        long total_count = (Long) role_query_count.getSingleResult();

        List<Subject> subjects = role_query.getResultList();

        return new PageList<Subject>(subjects, (int) total_count, pc);
    }

    /**
     * @see org.rhq.enterprise.server.authz.RoleManagerLocal#getRoleResourceGroups(Integer,PageControl)
     */
    @SuppressWarnings("unchecked")
    public PageList<ResourceGroup> getRoleResourceGroups(Integer roleId, PageControl pc) {
        pc.initDefaultOrderingField("rg.name");

        String query_name = ResourceGroup.QUERY_GET_RESOURCE_GROUPS_ASSIGNED_TO_ROLE;
        Query role_query_count = PersistenceUtility.createCountQuery(entityManager, query_name);
        Query role_query = PersistenceUtility.createQueryWithOrderBy(entityManager, query_name, pc);

        role_query_count.setParameter("id", roleId);
        role_query.setParameter("id", roleId);

        long total_count = (Long) role_query_count.getSingleResult();

        List<ResourceGroup> groups = role_query.getResultList();

        return new PageList<ResourceGroup>(groups, (int) total_count, pc);
    }

    /**
     * @see org.rhq.enterprise.server.authz.RoleManagerLocal#getRolesById(Integer[],PageControl)
     */
    @SuppressWarnings("unchecked")
    public PageList<Role> getRolesById(Integer[] roleIds, PageControl pc) {
        if ((roleIds == null) || (roleIds.length == 0)) {
            return new PageList<Role>(pc);
        }

        pc.initDefaultOrderingField("r.name");

        String query_name = Role.QUERY_FIND_BY_IDS;
        Query role_query_count = PersistenceUtility.createCountQuery(entityManager, query_name);
        Query role_query = PersistenceUtility.createQueryWithOrderBy(entityManager, query_name, pc);

        List<Integer> role_ids_list = Arrays.asList(roleIds);
        role_query_count.setParameter("ids", role_ids_list);
        role_query.setParameter("ids", role_ids_list);

        long total_count = (Long) role_query_count.getSingleResult();

        List<Role> roles = role_query.getResultList();

        // eagerly load in the members - can't use left-join due to PersistenceUtility usage; perhaps use EAGER
        for (Role role : roles) {
            role.getMemberCount();
        }

        return new PageList<Role>(roles, (int) total_count, pc);
    }

    @RequiredPermission(Permission.MANAGE_SECURITY)
    @SuppressWarnings("unchecked")
    public PageList<Role> getAvailableRolesForSubject(Subject whoami, Integer subjectId, Integer[] pendingRoleIds,
        PageControl pc) {
        pc.initDefaultOrderingField("r.name");

        String query_name;

        if ((pendingRoleIds == null) || (pendingRoleIds.length == 0)) {
            query_name = Role.QUERY_FIND_AVAILABLE_ROLES;
        } else {
            query_name = Role.QUERY_FIND_AVAILABLE_ROLES_WITH_EXCLUDES;
        }

        Query role_query_count = PersistenceUtility.createCountQuery(entityManager, query_name, "distinct r");
        Query role_query = PersistenceUtility.createQueryWithOrderBy(entityManager, query_name, pc);

        role_query_count.setParameter("subjectId", subjectId);
        role_query.setParameter("subjectId", subjectId);

        if ((pendingRoleIds != null) && (pendingRoleIds.length > 0)) {
            List<Integer> pending_ids_list = Arrays.asList(pendingRoleIds);
            role_query_count.setParameter("excludes", pending_ids_list);
            role_query.setParameter("excludes", pending_ids_list);
        }

        long total_count = (Long) role_query_count.getSingleResult();

        List<Role> roles = role_query.getResultList();

        // eagerly load in the members - can't use left-join due to PersistenceUtility usage; perhaps use EAGER
        for (Role role : roles) {
            role.getMemberCount();
        }

        return new PageList<Role>(roles, (int) total_count, pc);
    }

    @RequiredPermission(Permission.MANAGE_SECURITY)
    @SuppressWarnings("unchecked")
    public PageList<Role> getSubjectUnassignedRoles(Subject subject, int subjectId, PageControl pc) {
        return getAvailableRolesForSubject(subject, subjectId, null, pc);
    }

    @SuppressWarnings("unchecked")
    public PageList<Role> getAvailableRolesForAlertDefinition(Subject whoami, Integer alertDefinitionId,
        Integer[] pendingRoleIds, PageControl pc) {
        pc.initDefaultOrderingField("r.name");

        String query_name;

        if ((pendingRoleIds == null) || (pendingRoleIds.length == 0)) {
            query_name = Role.QUERY_FIND_AVAILABLE_ROLES_FOR_ALERT_DEFINITION;
        } else {
            query_name = Role.QUERY_FIND_AVAILABLE_ROLES_FOR_ALERT_DEFINITION_WITH_EXCLUDES;
        }

        Query role_query_count = PersistenceUtility.createCountQuery(entityManager, query_name, "distinct r");
        Query role_query = PersistenceUtility.createQueryWithOrderBy(entityManager, query_name, pc);

        role_query_count.setParameter("alertDefinitionId", alertDefinitionId);
        role_query.setParameter("alertDefinitionId", alertDefinitionId);

        if ((pendingRoleIds != null) && (pendingRoleIds.length > 0)) {
            List<Integer> pending_ids_list = Arrays.asList(pendingRoleIds);
            role_query_count.setParameter("excludes", pending_ids_list);
            role_query.setParameter("excludes", pending_ids_list);
        }

        long total_count = (Long) role_query_count.getSingleResult();

        List<Role> roles = role_query.getResultList();

        // eagerly load in the members - can't use left-join due to PersistenceUtility usage; perhaps use EAGER
        for (Role role : roles) {
            role.getMemberCount();
        }

        return new PageList<Role>(roles, (int) total_count, pc);
    }

    /**
     * @see org.rhq.enterprise.server.authz.RoleManagerLocal#addResourceGroupsToRole(Subject, int, int[])
     */
    @RequiredPermission(Permission.MANAGE_SECURITY)
    public void addResourceGroupsToRole(Subject whoami, int roleId, int[] pendingGroupIds) throws UpdateException {
        if ((pendingGroupIds != null) && (pendingGroupIds.length > 0)) {
            Role role = entityManager.find(Role.class, roleId);
            role.getResourceGroups().size(); // load them in

            for (Integer group_id : pendingGroupIds) {
                ResourceGroup group = entityManager.find(ResourceGroup.class, group_id);
                role.addResourceGroup(group);
            }
        }

        return;
    }

    /**
     * @see org.rhq.enterprise.server.authz.RoleManagerLocal#removeResourceGroupsFromRole(Subject, int, int[])
     */
    @RequiredPermission(Permission.MANAGE_SECURITY)
    public void removeResourceGroupsFromRole(Subject whoami, int roleId, int[] groupIds) throws UpdateException {
        if ((groupIds != null) && (groupIds.length > 0)) {
            Role role = entityManager.find(Role.class, roleId);
            role.getResourceGroups().size(); // load them in

            for (Integer group_id : groupIds) {
                ResourceGroup group = entityManager.find(ResourceGroup.class, group_id);
                role.removeResourceGroup(group);
            }
        }
    }

    private void processDependentPermissions(Role role) {
        /*
         * if you can control user/roles, then you can give yourself permissions, too;  so we might as well
         * automagically give all permissions to users that are explicitly given the MANAGE_SECURITY permission
         */
        if (role.getPermissions().contains(Permission.MANAGE_SECURITY)) {
            role.getPermissions().addAll(Arrays.asList(Permission.values()));
        }
    }

    //Specifically added for remove interface
    public PageList<Role> getSubjectAssignedRoles(Subject subject, int subjectId, PageControl pc) throws FetchException {
        //TODO: Implement, currently no references for this method.
        return null;
    }

    public void removeSubjectsFromRole(Subject subject, int roleId, int[] subjectIds) throws UpdateException {
        //TODO: Implement, currently no references for this method.
    }

    public PageList<Role> findRoles(Subject subject, Role criteria, PageControl pc) throws FetchException {
        try {
            QueryGenerator generator = new QueryGenerator(criteria, pc.getOptionalData(), pc);

            Query query = generator.getQuery(entityManager);
            Query countQuery = generator.getCountQuery(entityManager);

            long count = (Long) countQuery.getSingleResult();
            List<Role> roles = query.getResultList();

            return new PageList<Role>(roles, (int) count, pc);
        } catch (Exception e) {
            throw new FetchException(e.getMessage());
        }
    }

    public void removeRolesFromResourceGroup(Subject subject, int groupId, int[] roleIds) throws UpdateException {
        //TODO: Implement, currently no references for this method.
    }

    public Role getRole(Subject subject, int roleId) throws FetchException {
        return entityManager.find(Role.class, roleId);
    }

    public void addRolesToResourceGroup(Subject subject, int groupId, int[] roleIds) throws UpdateException {
        //TODO: Implement, currently no references for this method.
    }

}