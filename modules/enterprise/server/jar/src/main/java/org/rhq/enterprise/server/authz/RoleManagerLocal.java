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

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * The local interface to the role manager that provides the API to manipulate the security rules within the JON Server.
 * This API provides the ability to modify roles and their associated subjects and permissions.
 *
 * @author John Mazzitelli
 */
@Local
public interface RoleManagerLocal extends RoleManagerRemote {
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

    void setAssignedSubjects(Subject sessionSubject, int roleId, int[] subjectIds);
}