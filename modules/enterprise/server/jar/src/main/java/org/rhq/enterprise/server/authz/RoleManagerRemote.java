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

import javax.ejb.Remote;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.criteria.RoleCriteria;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * The remote interface to the role manager, providing a restricted set of Role Management services. that provides the API to manipulate the security rules within the JON Server.
 *
 * @author Jay Shaughnessy
 */
@Remote
public interface RoleManagerRemote {

    /**
     * Returns the role with the given ID
     *
     * @param subject
     * @param roleId
     *
     * @return the role or <code>null</code> if it wasn't found
     */
    Role getRole(Subject subject, int roleId);

    /**
     * Persists the new role to the database.
     *
     * @param subject The user attempting to create the role
     * @param newRole The new role being created
     * @return The persisted role with the primary key populated
     */
    Role createRole(Subject subject, Role newRole);

    /**
     * Removes a set of roles from the database. The subjects assigned to the roles are no longer authorized with the
     * deleted roles. Groups attached to the deleted roles are left alone.
     *
     * @param subject The user attempting to delete the role
     * @param roleIds The IDs of the roles to delete
     */
    void deleteRoles(Subject subject, int[] roleIds);

    /**
     * Updates the given role including permissions. To update subjects, resource groups, ldap groups
     * or bundle groups pass a non-null value.
     *
     * @param subject The user updating the role
     * @param role The role being updated
     * @return The updated role
     */
    Role updateRole(Subject subject, Role role);

    /**
     * Get all roles assigned for a certain subject
     * 
     * @param subject The logged in user's subject
     * @param subjectId The subject ID to find the associated roles for 
     * @param pc PageControl
     * @return A page list of assigned
     */
    PageList<Role> findSubjectAssignedRoles(Subject subject, int subjectId, PageControl pc);

    PageList<Role> findSubjectUnassignedRoles(Subject subject, int subjectId, PageControl pc);

    /**
     * Assigns a set of roles to a subject which authorizes the subject to do anything the roles permit.
     *
     * @param subject The logged in user's subject.
     * @param subjectId the subject who is to be authorized with the given roles
     * @param roleIds   the roles to assign
     */
    void addRolesToSubject(Subject subject, int subjectId, int[] roleIds);

    /**
     * Disassociates particular roles from a subject. Once complete, the subject will no longer be authorized with the
     * given roles.
     *
     * @param subject The logged in user's subject.
     * @param subjectId the user that is to have the roles unassigned from it
     * @param roleIds   list of role IDs that are to be removed from user
     */
    void removeRolesFromSubject(Subject subject, int subjectId, int[] roleIds);

    /**
     * Assigns a set of subjects to a role which authorizes the subjects to do anything the role permits.
     *
     * @param subject     the user attempting to assign the roles to the subject
     * @param roleId     the role who will authorized with the given subjects
     * @param subjectIds the subjects to assign the role
     */
    void addSubjectsToRole(Subject subject, int roleId, int[] subjectIds);

    /**
     * Dissociate particular subjects from a role.
     * 
     * @param subject The logged in user's subject.
     * @param roleId The role ID to dissociate the roles from
     * @param subjectIds The IDs of the subjects to remove from the specified Role
     */
    void removeSubjectsFromRole(Subject subject, int roleId, int[] subjectIds);

    /**
     * Sets the set of roles assigned to a subject. Requires SECURITY_ADMIN
     * @param subject
     * @param subjectId
     * @param roleIds
     */
    void setAssignedSubjectRoles(Subject subject, int subjectId, int[] roleIds);

    /**
     * Adds the given bundle groups to the given role.
     *
     * @param subject The logged in user's subject.
     * @param roleId
     * @param bundleGroupIds
     */
    void addBundleGroupsToRole(Subject subject, int roleId, int[] bundleGroupIds);

    /**
     * Adds the given resource groups to the given role.
     *
     * @param subject The logged in user's subject.
     * @param roleId
     * @param pendingGroupIds
     */
    void addResourceGroupsToRole(Subject subject, int roleId, int[] pendingGroupIds);

    void addRolesToBundleGroup(Subject subject, int bundleGroupId, int[] roleIds);

    void addRolesToResourceGroup(Subject subject, int groupId, int[] roleIds);

    void setAssignedBundleGroups(Subject subject, int roleId, int[] bundleGroupIds);

    void setAssignedResourceGroups(Subject subject, int roleId, int[] groupIds);

    /**
     * Removes the given bundle groups from the given role.
     *
     * @param subject user attempting to remove the groups from the role
     * @param roleId
     * @param bundleGroupIds
     */
    void removeBundleGroupsFromRole(Subject subject, int roleId, int[] bundleGroupIds);

    /**
     * Removes the given resource groups from the given role.
     *
     * @param subject user attempting to remove the groups from the role
     * @param roleId
     * @param groupIds
     */
    void removeResourceGroupsFromRole(Subject subject, int roleId, int[] groupIds);

    void removeRolesFromBundleGroup(Subject subject, int bundleGroupId, int[] roleIds);

    void removeRolesFromResourceGroup(Subject subject, int groupId, int[] roleIds);

    PageList<Role> findRolesByCriteria(Subject subject, RoleCriteria criteria);

}