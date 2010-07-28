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

import java.util.Set;

import javax.ejb.Local;
import javax.jws.WebParam;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.criteria.RoleCriteria;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * The local interface to the role manager that provides the API to manipulate the security rules within the JON Server.
 * This API provides the ability to modify roles and their associated subjects and permissions.
 *
 * @author John Mazzitelli
 */
@Local
public interface RoleManagerLocal {
    /**
     * This returns a page list of all the roles that a subject is authorized to access.
     *
     * @param  subjectId the id of the subject whose roles are to be returned
     * @param  pc
     *
     * @return page list of all subject's roles
     */
    PageList<Role> findRolesBySubject(int subjectId, PageControl pc);

    /**
     * Returns a list of all roles in the system.
     *
     * @param  pc
     *
     * @return list of all roles
     */
    PageList<Role> findRoles(PageControl pc);

    /**
     * Persists the new role to the database. The subjects assigned to the role are ignored - this only creates the role
     * entity with 0 subjects initially assigned to it.
     *
     * @param  subject the user attempting to create the role
     * @param  newRole the new role to persist
     *
     * @return the persisted role with the primary key populated
     */
    Role createRole(Subject subject, Role newRole);

    /**
     * Removes a set of roles from the database. The subjects assigned to the roles are no longer authorized with the
     * deleted roles. Groups attached to the deleted roles are left alone.
     *
     * @param subject       the user attempting to delete the role
     * @param doomedRoleIds the IDs of the roles to delete
     */
    void deleteRoles(Subject subject, Integer[] doomedRoleIds);

    /**
     * Sets the permissions for the specified role. Any currently existing role permissions are overwritten - that is,
     * <code>permissions</code> will be the complete set of permissions the role will now be authorized with.
     *
     * @param subject     the user attempting to peform the set
     * @param roleId      the ID of the role to modify
     * @param permissions a set of permissions to give to the role
     */
    void setPermissions(Subject subject, Integer roleId, Set<Permission> permissions);

    /**
     * Given a role ID, this will return the complete set of authorized permissions for that role.
     *
     * @param  roleId
     *
     * @return set of permissions that the given role explicitly authorize
     */
    Set<Permission> getPermissions(Integer roleId);

    /**
     * Updates the given role, excluding the subjects and groups. This updates permissions, name, description, etc.
     *
     * @param  subject user asking to update the role
     * @param  role
     *
     * @return the updated role
     */
    Role updateRole(Subject subject, Role role);

    /**
     * Given a set of role Ids, this returns a list of all the roles.
     *
     * @param  roleIds
     * @param  pc
     *
     * @return all the roles with the given ID
     */
    PageList<Role> findRolesByIds(Integer[] roleIds, PageControl pc);

    /**
     * Get all subjects that have been assigned the given role.
     *
     * @param  roleId
     * @param  pc
     *
     * @return list of all subjects assigned the role
     */
    PageList<Subject> findSubjectsByRole(Integer roleId, PageControl pc);

    /**
     * This returns a list of roles that are available to be assigned to a given subject but not yet assigned to that
     * subject. This excludes roles already assigned to the subject. The returned list will not include the roles
     * identified by <code>pendingRoleIds</code> since it is assumed the pending roles will be assigned to the user.
     *
     * @param  subject        user attempting to make this call
     * @param  subjectId      the subject whose list of available roles are to be returned
     * @param  pendingRoleIds the list of roles that are planned to be given to the subject
     * @param  pc
     *
     * @return the list of roles that can be assigned to the given user, not including the pending roles
     */
    PageList<Role> findAvailableRolesForSubject(Subject subject, Integer subjectId, Integer[] pendingRoleIds,
        PageControl pc);

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //
    // The following are shared with the Remote Interface
    //
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    /**
     * Returns the role with the given ID
     *
     * @param  roleId
     *
     * @return the role or <code>null</code> if it wasn't found
     */
    @Deprecated
    // Use getRole instead
    Role getRoleById(Integer roleId);

    Role getRole(Subject subject, int roleId);

    PageList<Role> findSubjectAssignedRoles(Subject subject, int subjectId, PageControl pc);

    //This is a proxy of getAvailableRolesForSubject but without pendingRoleIds as required by remote spec
    PageList<Role> findSubjectUnassignedRoles(Subject subject, int subjectId, PageControl pc);

    /**
     * Assigns a set of roles to a subject which authorizes the subject to do anything the roles permit.
     *
     * @param subject   the user attempting to assign the roles to the subject
     * @param subjectId the subject who is to be authorized with the given roles
     * @param roleIds   the roles to assign
     */
    void addRolesToSubject(Subject subject, int subjectId, int[] roleIds);

    /**
     * Disassociates particular roles from a subject. Once complete, the subject will no longer be authorized with the
     * given roles.
     *
     * @param subject   the user that is attempting to perform the remove
     * @param subjectId the user that is to have the roles unassigned from it
     * @param roleIds   list of role IDs that are to be removed from user
     */
    void removeRolesFromSubject(Subject subject, int subjectId, int[] roleIds);

    void setAssignedSubjectRoles(Subject subject, int subjectId, int[] roleIds); 


    void addSubjectsToRole(Subject subject, int roleId, int[] subjectIds);

    void removeSubjectsFromRole(Subject subject, int roleId, int[] subjectIds);

    void setAssignedSubjects(Subject sessionSubject, int roleId, int[] subjectIds);

    /**
     * Adds the given resource groups to the given role.
     *
     * @param subject          user attempting to add the groups to the role
     * @param roleId
     * @param pendingGroupIds
     */
    void addResourceGroupsToRole(Subject subject, int roleId, int[] pendingGroupIds);

    void addRolesToResourceGroup(Subject subject, int groupId, int[] roleIds);

    void setAssignedResourceGroups(Subject subject, int roleId, int[] groupIds);



    /**
     * Removes the given resource groups from the given role.
     *
     * @param subject   user attempting to remove the groups from the role
     * @param roleId
     * @param groupIds
     */
    void removeResourceGroupsFromRole(Subject subject, int roleId, int[] groupIds);

    void removeRolesFromResourceGroup(Subject subject, int groupId, int[] roleIds);

    PageList<Role> findRolesByCriteria(Subject subject, RoleCriteria criteria);

}