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
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.ExcludeDefaultInterceptors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.annotation.IgnoreDependency;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.criteria.RoleCriteria;
import org.rhq.core.domain.resource.group.LdapGroup;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.server.PersistenceUtility;
import org.rhq.core.util.collection.ArrayUtils;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.alert.AlertNotificationManagerLocal;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.resource.group.LdapGroupManagerLocal;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;

/**
 * This bean provides functionality to manipulate the security {@link Role role}s. That is, adding/modifying/deleting
 * roles and their associated subjects and permissions is performed by this manager.
 *
 * @author John Mazzitelli
 */
@Stateless
public class RoleManagerBean implements RoleManagerLocal, RoleManagerRemote {

    @SuppressWarnings("unused")
    private final Log log = LogFactory.getLog(RoleManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private RoleManagerLocal roleManager; // self-referencing

    @EJB
    private SubjectManagerLocal subjectManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;

    @EJB
    @IgnoreDependency
    private AlertNotificationManagerLocal alertNotificationManager;

    @EJB
    @IgnoreDependency
    private LdapGroupManagerLocal ldapGroupManager;

    /**
     * @see org.rhq.enterprise.server.authz.RoleManagerLocal#findRolesBySubject(int subjectId,PageControl pageControl)
     */
    @SuppressWarnings("unchecked")
    // the first param, subject, is not the subject making the request, its the subject whose roles are to be returned.
    // therefore, we won't want our security interceptor to check this method since the subject won't have a session associated with it
    @ExcludeDefaultInterceptors
    public PageList<Role> findRolesBySubject(int subjectId, PageControl pc) {
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
     * @see org.rhq.enterprise.server.authz.RoleManagerLocal#findRoles(PageControl)
     */
    @SuppressWarnings("unchecked")
    public PageList<Role> findRoles(PageControl pc) {
        pc.initDefaultOrderingField("r.name");

        String queryName = Role.QUERY_FIND_ALL;
        Query queryCount = PersistenceUtility.createCountQuery(entityManager, queryName);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, queryName, pc);

        long count = (Long) queryCount.getSingleResult();
        List<Role> roles = query.getResultList();

        if (roles != null) {
            // eagerly load in the members - can't use left-join due to PersistenceUtility usage; perhaps use EAGER
            for (Role role : roles) {
                role.getMemberCount();
            }
        } else {
            roles = new ArrayList<Role>();
        }

        return new PageList<Role>(roles, (int) count, pc);
    }

    /**
     * @see org.rhq.enterprise.server.authz.RoleManagerLocal#createRole(Subject, Role)
     */
    @RequiredPermission(Permission.MANAGE_SECURITY)
    public Role createRole(Subject whoami, Role newRole) {
        // TODO (ips): Do we want to enforce uniqueness of the Role name?

        Boolean isSystemRole = newRole.getFsystem();
        if (isSystemRole) {
            throw new IllegalArgumentException("Unable to create system role [" + newRole.getName()
                + "] - new system roles cannot be created.");
        }
        processDependentPermissions(newRole);

        Set<LdapGroup> ldapGroups = newRole.getLdapGroups();
        for (LdapGroup ldapGroup : ldapGroups) {
            ldapGroup.setRole(newRole);
        }

        entityManager.persist(newRole);

        // Now we must merge subjects and resource groups, since those fields in Role do not have persist cascade
        // enabled.
        int[] subjectIds = new int[newRole.getSubjects().size()];
        int i = 0;
        for (Subject subject : newRole.getSubjects()) {
            subjectIds[i++] = subject.getId();
        }
        addSubjectsToRole(whoami, newRole.getId(), subjectIds);

        int[] resourceGroupIds = new int[newRole.getResourceGroups().size()];
        i = 0;
        for (ResourceGroup resourceGroup : newRole.getResourceGroups()) {
            resourceGroupIds[i++] = resourceGroup.getId();
        }
        addResourceGroupsToRole(whoami, newRole.getId(), resourceGroupIds);

        return newRole;
    }

    /**
     * @see org.rhq.enterprise.server.authz.RoleManagerLocal#deleteRoles(Subject, int[])
     */
    @RequiredPermission(Permission.MANAGE_SECURITY)
    public void deleteRoles(Subject subject, int[] doomedRoleIds) {
        if (doomedRoleIds != null) {
            for (int roleId : doomedRoleIds) {
                Role doomedRole = entityManager.find(Role.class, roleId);

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

                alertNotificationManager.cleanseAlertNotificationByRole(doomedRole.getId());
                entityManager.remove(doomedRole); // there should not be any cascading - this does not touch subjects/groups
            }
        }

        return;
    }

    /**
     * @see org.rhq.enterprise.server.authz.RoleManagerLocal#addRolesToSubject(Subject, int, int[])
     */
    @RequiredPermission(Permission.MANAGE_SECURITY)
    public void addRolesToSubject(Subject subject, int subjectId, int[] roleIds) {
        addRolesToSubject(subject, subjectId, roleIds, false);
    }

    /**
     * @see org.rhq.enterprise.server.authz.RoleManagerLocal#addRolesToSubject(Subject, int, int[])
     */
    @RequiredPermission(Permission.MANAGE_SECURITY)
    public void addRolesToSubject(Subject subject, int subjectId, int[] roleIds, boolean isLdap) {
        if (roleIds != null) {
            Subject subjectToModify = subjectManager.getSubjectById(subjectId); // attach it
            if (subjectToModify == null) {
                throw new IllegalArgumentException("Could not find subject[" + subjectId + "] to add roles to");
            }

            if (subjectToModify.getFsystem() || (authorizationManager.isSystemSuperuser(subjectToModify))) {
                throw new PermissionException("You cannot assign roles to user [" + subjectToModify.getName()
                    + "] - roles are fixed for this user");
            }

            subjectToModify.getRoles().size();

            for (Integer roleId : roleIds) {
                Role role = entityManager.find(Role.class, roleId);
                if (role == null) {
                    throw new IllegalArgumentException("Tried to add role[" + roleId + "] to subject[" + subjectId
                        + "], but role was not found");
                }
                role.addSubject(subjectToModify);
                if (isLdap) {
                    role.addLdapSubject(subjectToModify);
                }
            }
        }
    }

    /**
     * @see org.rhq.enterprise.server.authz.RoleManagerLocal#addSubjectsToRole(Subject, int, int[])
     */
    @RequiredPermission(Permission.MANAGE_SECURITY)
    public void addSubjectsToRole(Subject subject, int roleId, int[] subjectIds) {
        if (subjectIds != null) {
            Role role = getRoleById(roleId); // attach it
            if (role == null) {
                throw new IllegalArgumentException("Could not find role[" + roleId + "] to add subjects to");
            }

            for (Integer subjectId : subjectIds) {
                Subject newSubject = entityManager.find(Subject.class, subjectId);
                if (newSubject == null) {
                    throw new IllegalArgumentException("Tried to add subject[" + subjectId + "] to role[" + roleId
                        + "], but subject was not found");
                }

                if (newSubject.getFsystem() || (authorizationManager.isSystemSuperuser(newSubject))) {
                    throw new PermissionException("You cannot alter the roles for user [" + newSubject.getName()
                        + "] - roles are fixed for this user");
                }

                role.addSubject(newSubject);

            }
        }

        return;
    }

    /**
     * @see org.rhq.enterprise.server.authz.RoleManagerLocal#removeRolesFromSubject(Subject, int, int[])
     */
    @RequiredPermission(Permission.MANAGE_SECURITY)
    public void removeRolesFromSubject(Subject subject, int subjectId, int[] roleIds) {
        if (roleIds != null) {
            Subject subjectToModify = subjectManager.getSubjectById(subjectId); // attach it

            if (subjectToModify.getFsystem() || (authorizationManager.isSystemSuperuser(subjectToModify))) {
                throw new PermissionException("You cannot remove roles from user [" + subjectToModify.getName()
                    + "] - roles are fixed for this user");
            }

            for (Integer roleId : roleIds) {
                Role role = entityManager.find(Role.class, roleId);
                if (role != null) {
                    role.removeSubject(subjectToModify);
                }
            }
        }

        return;
    }

    @RequiredPermission(Permission.MANAGE_SECURITY)
    public void setAssignedSubjectRoles(Subject subject, int subjectId, int[] roleIds) {

        Subject subjectToModify = subjectManager.getSubjectById(subjectId); // attach it
        List<Integer> currentRoles = new ArrayList<Integer>();
        for (Role role : subjectToModify.getRoles()) {
            currentRoles.add(role.getId());
        }

        List<Integer> newRoles = ArrayUtils.wrapInList(roleIds); // members needing addition
        newRoles.removeAll(currentRoles);
        if (newRoles.size() > 0) {
            int[] newRoleIds = new int[newRoles.size()];
            int i = 0;
            for (Integer id : newRoles) {
                newRoleIds[i++] = id;
            }
            roleManager.addRolesToSubject(subject, subjectId, newRoleIds);
        }

        List<Integer> removedRoles = new ArrayList<Integer>(currentRoles); // members needing removal
        removedRoles.removeAll(ArrayUtils.wrapInList(roleIds));
        if (removedRoles.size() > 0) {
            int[] removedRoleIds = new int[removedRoles.size()];
            int i = 0;
            for (Integer id : removedRoles) {
                removedRoleIds[i++] = id;
            }
            roleManager.removeRolesFromSubject(subject, subjectId, removedRoleIds);
        }
    }

    /**
     * @see org.rhq.enterprise.server.authz.RoleManagerLocal#getRoleById(Integer)
     */
    public Role getRoleById(Integer roleId) {
        Role role = entityManager.find(Role.class, roleId);
        return role;
    }

    /**
     * @see org.rhq.enterprise.server.authz.RoleManagerLocal#setPermissions(Subject, Integer, Set)
     */
    @RequiredPermission(Permission.MANAGE_SECURITY)
    public void setPermissions(Subject subject, Integer roleId, Set<Permission> permissions) {
        Role role = entityManager.find(Role.class, roleId);
        Set<Permission> rolePermissions = role.getPermissions();
        rolePermissions.clear();
        rolePermissions.addAll(permissions);
        entityManager.merge(role);
        entityManager.flush();
        return;
    }

    /**
     * @see org.rhq.enterprise.server.authz.RoleManagerLocal#getPermissions(Integer)
     */
    public Set<Permission> getPermissions(Integer roleId) {
        Role role = entityManager.find(Role.class, roleId);
        Set<Permission> rolePermissions = role.getPermissions();
        return rolePermissions;
    }

    /**
     * @see org.rhq.enterprise.server.authz.RoleManagerLocal#updateRole(Subject, Role)
     */
    @RequiredPermission(Permission.MANAGE_SECURITY)
    public Role updateRole(Subject whoami, Role role) {
        Role attachedRole = entityManager.find(Role.class, role.getId());
        if (attachedRole == null) {
            throw new IllegalStateException("Cannot update " + role + ", since no role exists with that id.");
        }

        // First update the simple fields and the permissions.
        attachedRole.setName(role.getName());
        attachedRole.setDescription(role.getDescription());
        attachedRole.setPermissions(role.getPermissions());
        processDependentPermissions(attachedRole);

        // Then update the subjects, resourceGroups, and/or ldapGroups, but only if those fields are non-null on the
        // passed-in Role.
        Set<Subject> newSubjects = role.getSubjects();
        if (newSubjects != null) {
            Set<Subject> currentSubjects = attachedRole.getSubjects();
            // wrap in new HashSet to avoid ConcurrentModificationExceptions.
            Set<Subject> subjectsToRemove = new HashSet<Subject>(currentSubjects);
            for (Subject subject : currentSubjects) {
                // Never remove a system user.
                if (subject.getFsystem()) {
                    subjectsToRemove.remove(subject);
                }
            }
            for (Subject subject : subjectsToRemove) {
                attachedRole.removeSubject(subject);
            }

            for (Subject subject : newSubjects) {
                Subject attachedSubject = entityManager.find(Subject.class, subject.getId());
                attachedRole.addSubject(attachedSubject);
            }
        }

        Set<ResourceGroup> newResourceGroups = role.getResourceGroups();
        if (newResourceGroups != null) {
            // wrap in new HashSet to avoid ConcurrentModificationExceptions.
            Set<ResourceGroup> currentResourceGroups = new HashSet<ResourceGroup>(attachedRole.getResourceGroups());
            for (ResourceGroup resourceGroup : currentResourceGroups) {
                attachedRole.removeResourceGroup(resourceGroup);
            }

            for (ResourceGroup resourceGroup : newResourceGroups) {
                ResourceGroup attachedResourceGroup = entityManager.find(ResourceGroup.class, resourceGroup.getId());
                attachedRole.addResourceGroup(attachedResourceGroup);
            }
        }

        Set<LdapGroup> newLdapGroups = role.getLdapGroups();
        if (newLdapGroups != null) {
            // wrap in new HashSet to avoid ConcurrentModificationExceptions.
            Set<LdapGroup> currentLdapGroups = new HashSet<LdapGroup>(attachedRole.getLdapGroups());
            for (LdapGroup ldapGroup : currentLdapGroups) {
                if (!newLdapGroups.contains(ldapGroup)) {
                    attachedRole.removeLdapGroup(ldapGroup);
                    entityManager.remove(ldapGroup);
                }
            }

            for (LdapGroup ldapGroup : newLdapGroups) {
                LdapGroup attachedLdapGroup = (ldapGroup.getId() != 0) ?
                    entityManager.find(LdapGroup.class, ldapGroup.getId()) : null;
                if (attachedLdapGroup == null) {
                    ldapGroup.setRole(attachedRole);
                    entityManager.persist(ldapGroup);
                    attachedLdapGroup = ldapGroup;
                }
                attachedRole.addLdapGroup(attachedLdapGroup);
            }
        }

        // Fetch the lazy Sets on the Role to be returned.
        attachedRole.getResourceGroups().size();
        attachedRole.getSubjects().size();
        attachedRole.getLdapGroups().size();

        return attachedRole;
    }

    /**
     * @see org.rhq.enterprise.server.authz.RoleManagerLocal#findSubjectsByRole(Integer roleId,PageControl pageControl)
     */
    public PageList<Subject> findSubjectsByRole(Subject subject, Integer roleId, PageControl pc) {
        return findSubjectsByRole(roleId, pc);
    }

    /**
     * @see org.rhq.enterprise.server.authz.RoleManagerLocal#findSubjectsByRole(Integer,PageControl)
     */
    @SuppressWarnings("unchecked")
    public PageList<Subject> findSubjectsByRole(Integer roleId, PageControl pc) {
        pc.initDefaultOrderingField("s.name");

        String queryName = Subject.QUERY_GET_SUBJECTS_ASSIGNED_TO_ROLE;
        Query queryCount = PersistenceUtility.createCountQuery(entityManager, queryName);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, queryName, pc);

        queryCount.setParameter("id", roleId);
        query.setParameter("id", roleId);

        long count = (Long) queryCount.getSingleResult();
        List<Subject> subjects = query.getResultList();

        return new PageList<Subject>(subjects, (int) count, pc);
    }

    /**
     * @see org.rhq.enterprise.server.authz.RoleManagerLocal#findRolesByIds(Integer[],PageControl)
     */
    @SuppressWarnings("unchecked")
    public PageList<Role> findRolesByIds(Integer[] roleIds, PageControl pc) {
        if ((roleIds == null) || (roleIds.length == 0)) {
            return new PageList<Role>(pc);
        }

        pc.initDefaultOrderingField("r.name");

        String queryName = Role.QUERY_FIND_BY_IDS;
        Query queryCount = PersistenceUtility.createCountQuery(entityManager, queryName);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, queryName, pc);

        List<Integer> roleIdsList = Arrays.asList(roleIds);
        queryCount.setParameter("ids", roleIdsList);
        query.setParameter("ids", roleIdsList);

        long count = (Long) queryCount.getSingleResult();
        List<Role> roles = query.getResultList();

        // eagerly load in the members - can't use left-join due to PersistenceUtility usage; perhaps use EAGER
        for (Role role : roles) {
            role.getMemberCount();
        }

        return new PageList<Role>(roles, (int) count, pc);
    }

    @RequiredPermission(Permission.MANAGE_SECURITY)
    @SuppressWarnings("unchecked")
    public PageList<Role> findAvailableRolesForSubject(Subject subject, Integer subjectId, Integer[] pendingRoleIds,
        PageControl pc) {
        pc.initDefaultOrderingField("r.name");

        String queryName;

        if ((pendingRoleIds == null) || (pendingRoleIds.length == 0)) {
            queryName = Role.QUERY_FIND_AVAILABLE_ROLES;
        } else {
            queryName = Role.QUERY_FIND_AVAILABLE_ROLES_WITH_EXCLUDES;
        }

        Query queryCount = PersistenceUtility.createCountQuery(entityManager, queryName, "distinct r");
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, queryName, pc);

        queryCount.setParameter("subjectId", subjectId);
        query.setParameter("subjectId", subjectId);

        if ((pendingRoleIds != null) && (pendingRoleIds.length > 0)) {
            List<Integer> pendingIdsList = Arrays.asList(pendingRoleIds);
            queryCount.setParameter("excludes", pendingIdsList);
            query.setParameter("excludes", pendingIdsList);
        }

        long count = (Long) queryCount.getSingleResult();
        List<Role> roles = query.getResultList();

        // eagerly load in the members - can't use left-join due to PersistenceUtility usage; perhaps use EAGER
        for (Role role : roles) {
            role.getMemberCount();
        }

        return new PageList<Role>(roles, (int) count, pc);
    }

    @RequiredPermission(Permission.MANAGE_SECURITY)
    public PageList<Role> findSubjectUnassignedRoles(Subject subject, int subjectId, PageControl pc) {
        return findAvailableRolesForSubject(subject, subjectId, null, pc);
    }

    /**
     * @see org.rhq.enterprise.server.authz.RoleManagerLocal#addResourceGroupsToRole(Subject, int, int[])
     */
    @RequiredPermission(Permission.MANAGE_SECURITY)
    public void addResourceGroupsToRole(Subject subject, int roleId, int[] groupIds) {
        if ((groupIds != null) && (groupIds.length > 0)) {
            Role role = entityManager.find(Role.class, roleId);
            if (role == null) {
                throw new IllegalArgumentException("Could not find role[" + roleId + "] to add resourceGroups to");
            }
            role.getResourceGroups().size(); // load them in

            for (Integer groupId : groupIds) {
                ResourceGroup group = entityManager.find(ResourceGroup.class, groupId);
                if (group == null) {
                    throw new IllegalArgumentException("Tried to add resourceGroup[" + groupId + "] to role[" + roleId
                        + "], but resourceGroup was not found.");
                }
                role.addResourceGroup(group);
            }
        }

        return;
    }

    /**
     * @see org.rhq.enterprise.server.authz.RoleManagerLocal#removeResourceGroupsFromRole(Subject, int, int[])
     */
    @RequiredPermission(Permission.MANAGE_SECURITY)
    public void removeResourceGroupsFromRole(Subject subject, int roleId, int[] groupIds) {
        if ((groupIds != null) && (groupIds.length > 0)) {
            Role role = entityManager.find(Role.class, roleId);
            if (role == null) {
                throw new IllegalArgumentException("Could not find role[" + roleId + "] to remove resourceGroups from");
            }
            role.getResourceGroups().size(); // load them in

            for (Integer groupId : groupIds) {
                ResourceGroup doomedGroup = entityManager.find(ResourceGroup.class, groupId);
                if (doomedGroup == null) {
                    throw new IllegalArgumentException("Tried to remove doomedGroup[" + groupId + "] from role["
                        + roleId + "], but subject was not found");
                }
                role.removeResourceGroup(doomedGroup);
            }
        }
    }

    @RequiredPermission(Permission.MANAGE_SECURITY)
    public void setAssignedResourceGroups(Subject subject, int roleId, int[] groupIds) {
        Role role = getRole(subject, roleId);
        List<Integer> currentGroups = new ArrayList<Integer>();
        for (ResourceGroup group : role.getResourceGroups()) {
            currentGroups.add(group.getId());
        }

        List<Integer> newGroups = ArrayUtils.wrapInList(groupIds); // members needing addition
        newGroups.removeAll(currentGroups);
        int[] newGroupIds = ArrayUtils.unwrapCollection(newGroups);
        roleManager.addResourceGroupsToRole(subject, roleId, newGroupIds);

        List<Integer> removedGroups = new ArrayList<Integer>(currentGroups); // members needing removal
        removedGroups.removeAll(ArrayUtils.wrapInList(groupIds));
        int[] removedGroupIds = ArrayUtils.unwrapCollection(removedGroups);
        roleManager.removeResourceGroupsFromRole(subject, roleId, removedGroupIds);
    }

    private void processDependentPermissions(Role role) {
        /*
         * if you can control user/roles, then you can give yourself permissions, too;  so we might as well
         * automagically give all permissions to users that are explicitly given the MANAGE_SECURITY permission
         */
        if (role.getPermissions().contains(Permission.MANAGE_SECURITY)) {
            role.getPermissions().addAll(EnumSet.allOf(Permission.class));
        }

        /*
         * similarly, MANAGE_INVENTORY implies all Resource perms
         */
        if (role.getPermissions().contains(Permission.MANAGE_INVENTORY)) {
            role.getPermissions().addAll(Permission.RESOURCE_ALL);
        }

        /*
         * write-access implies read-access
         */
        if (role.getPermissions().contains(Permission.CONFIGURE_WRITE)) {
            role.getPermissions().add(Permission.CONFIGURE_READ);
        }

        /*
         * and lack of read-access implies lack of write-access
         */
        if (!role.getPermissions().contains(Permission.CONFIGURE_READ)) {
            role.getPermissions().remove(Permission.CONFIGURE_WRITE);
        }
    }

    public PageList<Role> findSubjectAssignedRoles(Subject subject, int subjectId, PageControl pc) {
        PageList<Role> assignedRoles = findRolesBySubject(subjectId, pc);
        return assignedRoles;
    }

    @RequiredPermission(Permission.MANAGE_SECURITY)
    public void removeSubjectsFromRole(Subject subject, int roleId, int[] subjectIds) {
        if ((subjectIds != null) && (subjectIds.length > 0)) {
            Role role = entityManager.find(Role.class, roleId);
            if (role == null) {
                throw new IllegalArgumentException("Could not find role[" + roleId + "] to remove subjects from");
            }
            role.getSubjects().size(); // load them in

            for (Integer subjectId : subjectIds) {
                Subject doomedSubject = entityManager.find(Subject.class, subjectId);
                if (doomedSubject == null) {
                    throw new IllegalArgumentException("Tried to remove subject[" + subjectId + "] from role[" + roleId
                        + "], but subject was not found");
                }
                if (doomedSubject.getFsystem() || (authorizationManager.isSystemSuperuser(doomedSubject))) {
                    throw new PermissionException("You cannot remove user[" + doomedSubject.getName()
                        + "] from role[" + roleId + "] - roles are fixed for this user");
                }
                role.removeSubject(doomedSubject);
            }
        }
    }

    @RequiredPermission(Permission.MANAGE_SECURITY)
    public void setAssignedSubjects(Subject subject, int roleId, int[] subjectIds) {

        Role role = getRole(subject, roleId);
        List<Integer> currentSubjects = new ArrayList<Integer>();
        for (Subject currentSubject : role.getSubjects()) {
            currentSubjects.add(currentSubject.getId());
        }

        List<Integer> newSubjects = ArrayUtils.wrapInList(subjectIds); // members needing addition
        newSubjects.removeAll(currentSubjects);
        if (newSubjects.size() > 0) {
            int[] newSubjectIds = new int[newSubjects.size()];
            int i = 0;
            for (Integer id : newSubjects) {
                newSubjectIds[i++] = id;
            }
            roleManager.addSubjectsToRole(subject, roleId, newSubjectIds);
        }

        List<Integer> removedSubjects = new ArrayList<Integer>(currentSubjects); // members needing removal
        removedSubjects.removeAll(ArrayUtils.wrapInList(subjectIds));
        if (removedSubjects.size() > 0) {
            int[] removedSubjectIds = new int[removedSubjects.size()];
            int i = 0;
            for (Integer id : removedSubjects) {
                removedSubjectIds[i++] = id;
            }
            roleManager.removeSubjectsFromRole(subject, roleId, removedSubjectIds);
        }
    }

    @RequiredPermission(Permission.MANAGE_SECURITY)
    public void removeRolesFromResourceGroup(Subject subject, int groupId, int[] roleIds) {
        if ((roleIds != null) && (roleIds.length > 0)) {
            ResourceGroup group = entityManager.find(ResourceGroup.class, groupId);
            if (group == null) {
                throw new IllegalArgumentException("Could not find resourceGroup[" + groupId + "] to remove roles from");
            }
            group.getRoles().size(); // load them in

            for (Integer roleId : roleIds) {
                Role doomedRole = entityManager.find(Role.class, roleId);
                if (doomedRole == null) {
                    throw new IllegalArgumentException("Tried to remove role[" + roleId + "] from resourceGroup["
                        + groupId + "], but role was not found");
                }
                group.removeRole(doomedRole);
            }
        }

        return;
    }

    public Role getRole(Subject subject, int roleId) {
        return entityManager.find(Role.class, roleId);
    }

    @RequiredPermission(Permission.MANAGE_SECURITY)
    public void addRolesToResourceGroup(Subject subject, int groupId, int[] roleIds) {
        if ((roleIds != null) && (roleIds.length > 0)) {
            ResourceGroup group = entityManager.find(ResourceGroup.class, groupId);
            if (group == null) {
                throw new IllegalArgumentException("Could not find resourceGroup[" + groupId + "] to add roles to");
            }
            group.getRoles().size(); // load them in

            for (Integer roleId : roleIds) {
                Role role = entityManager.find(Role.class, roleId);
                if (role == null) {
                    throw new IllegalArgumentException("Tried to add role[" + roleId + "] to resourceGroup[" + groupId
                        + "], but role was not found");
                }
                group.addRole(role);
            }
        }

        return;
    }

    @SuppressWarnings("unchecked")
    public PageList<Role> findRolesByCriteria(Subject subject, RoleCriteria criteria) {

        if (criteria.isSecurityManagerRequired()
            && !authorizationManager.hasGlobalPermission(subject, Permission.MANAGE_SECURITY)) {
            throw new PermissionException("Subject [" + subject.getName()
                + "] requires SecurityManager permission for requested query criteria.");
        }

        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        CriteriaQueryRunner<Role> queryRunner = new CriteriaQueryRunner<Role>(criteria, generator, entityManager);
        @SuppressWarnings({"UnnecessaryLocalVariable"})
        PageList<Role> roles = queryRunner.execute();

        return roles;
    }

}