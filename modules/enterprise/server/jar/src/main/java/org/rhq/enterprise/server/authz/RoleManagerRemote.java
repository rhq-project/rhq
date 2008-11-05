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
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * The remote interface to the role manager, providing a restricted set of Role Management services. that provides the API to manipulate the security rules within the JON Server.
 *
 * @author Jay Shaughnessy
 */
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService
@Remote
public interface RoleManagerRemote {

    /**
     * Assigns a set of roles to a subject which authorizes the subject to do anything the roles permit.
     *
     * @param user The logged in user's subject.
     * @param subjectId the subject who is to be authorized with the given roles
     * @param roleIds   the roles to assign
     */
    void assignRolesToSubject( //
        @WebParam(name = "user")
        Subject user, //
        @WebParam(name = "subjectId")
        Integer subjectId, //
        @WebParam(name = "roleIds")
        Integer[] roleIds);

    /**
     * Returns a list of all roles in the system.
     *
     * @param user The logged in user's subject.
     * @param  pc
     *
     * @return list of all roles
     */
    @WebMethod
    PageList<Role> getAllRoles( //
        @WebParam(name = "user")
        Subject user, //
        @WebParam(name = "pc")
        PageControl pc);

    /**
     * This returns a list of roles that are available to be assigned to a given subject but not yet assigned to that
     * subject. This excludes roles already assigned to the subject. The returned list will not include the roles
     * identified by <code>pendingRoleIds</code> since it is assumed the pending roles will be assigned to the user.
     *
     * @param user The logged in user's subject.
     * @param  subjectId      the subject whose list of available roles are to be returned
     * @param  pendingRoleIds the list of roles that are planned to be given to the subject
     * @param  pc
     *
     * @return the list of roles that can be assigned to the given user, not including the pending roles
     */
    PageList<Role> getAvailableRolesForSubject( //
        @WebParam(name = "user")
        Subject user, // 
        @WebParam(name = "subjectId")
        Integer subjectId, // 
        @WebParam(name = "pendingRoleIds")
        Integer[] pendingRoleIds, //
        @WebParam(name = "pc")
        PageControl pc);

    /**
     * Get all subjects that have been assigned the given role.
     * 
     * @param user The logged in user's subject.
     * @param  roleId
     * @param  pc
     *
     * @return list of all subjects assigned the role
     */
    PageList<Subject> getRoleSubjects( //
        @WebParam(name = "user")
        Subject user, //
        @WebParam(name = "roleId")
        Integer roleId, //
        @WebParam(name = "pc")
        PageControl pc);

    /**
     * Disassociates particular roles from a subject. Once complete, the subject will no longer be authorized with the
     * given roles.
     *
     * @param user The logged in user's subject.
     * @param subjectId the user that is to have the roles unassigned from it
     * @param roleIds   list of role IDs that are to be removed from user
     */
    void removeRolesFromSubject( //
        @WebParam(name = "user")
        Subject user, //
        @WebParam(name = "subjectId")
        Integer subjectId, //
        @WebParam(name = "roleIds")
        Integer[] roleIds);

}