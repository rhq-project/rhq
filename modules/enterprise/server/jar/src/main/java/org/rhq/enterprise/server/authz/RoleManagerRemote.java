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
import org.rhq.enterprise.server.exception.FetchException;
import org.rhq.enterprise.server.exception.UpdateException;

/**
 * The remote interface to the role manager, providing a restricted set of Role Management services. that provides the API to manipulate the security rules within the JON Server.
 *
 * @author Jay Shaughnessy
 */
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService
@Remote
public interface RoleManagerRemote {

    //available as getRoleById
    /**
     * Returns the role with the given ID
     *
     * @param sessionSubject
     * @param roleId
     *
     * @return the role or <code>null</code> if it wasn't found
     */
    @WebMethod
    Role getRole( //
        @WebParam(name = "sessionSubject") Subject sessionSubject, //
        @WebParam(name = "roleId") int roleId) //
        throws FetchException;

    //not avialable
    /**
     * Get all roles assigned for a certain subject
     * 
     * @param sessionSubject The logged in user's subject
     * @param subjectId The subject ID to find the associated roles for 
     * @param pc PageControl
     * @return A page list of assigned
     */
    @WebMethod
    PageList<Role> getSubjectAssignedRoles( //
        @WebParam(name = "sessionSubject") Subject sessionSubject, //
        @WebParam(name = "subjectId") int subjectId, //
        @WebParam(name = "pc") PageControl pc) //
        throws FetchException;

    //Available as getAvailableRolesForSubject, but better proxy!
    @WebMethod
    PageList<Role> getSubjectUnassignedRoles( //
        @WebParam(name = "sessionSubject") Subject sessionSubject, //
        @WebParam(name = "subjectId") int subjectId, //
        @WebParam(name = "pc") PageControl pc) //
        throws FetchException;

    //does not exist
    @WebMethod
    PageList<Role> findRoles( //
        @WebParam(name = "sessionSubject") Subject sessionSubject, //
        @WebParam(name = "criteria") Role criteria, //
        @WebParam(name = "pc") PageControl pc) //
        throws FetchException;

    //available as: assignRolesToSubject
    /**
     * Assigns a set of roles to a subject which authorizes the subject to do anything the roles permit.
     *
     * @param sessionSubject The logged in user's subject.
     * @param subjectId the subject who is to be authorized with the given roles
     * @param roleIds   the roles to assign
     */
    @WebMethod
    void addRolesToSubject( //
        @WebParam(name = "sessionSubject") Subject sessionSubject, //
        @WebParam(name = "subjectId") int subjectId, //
        @WebParam(name = "roleIds") int[] roleIds) //
        throws UpdateException;

    //avaialble 
    /**
     * Disassociates particular roles from a subject. Once complete, the subject will no longer be authorized with the
     * given roles.
     *
     * @param sessionSubject The logged in user's subject.
     * @param subjectId the user that is to have the roles unassigned from it
     * @param roleIds   list of role IDs that are to be removed from user
     */
    @WebMethod
    void removeRolesFromSubject( //
        @WebParam(name = "sessionSubject") Subject sessionSubject, //
        @WebParam(name = "subjectId") int subjectId, //
        @WebParam(name = "roleIds") int[] roleIds) //
        throws UpdateException;

    //available as assignSubjectsToRole
    /**
     * Assigns a set of subjects to a role which authorizes the subjects to do anything the role permits.
     *
     * @param sessionSubject     the user attempting to assign the roles to the subject
     * @param roleId     the role who will authorized with the given subjects
     * @param subjectIds the subjects to assign the role
     */
    @WebMethod
    void addSubjectsToRole( //
        @WebParam(name = "sessionSubject") Subject sessionSubject, //
        @WebParam(name = "roleId") int roleId, //
        @WebParam(name = "subjectIds") int[] subjectIds) //
        throws UpdateException;

    //does not exist
    /**
     * Dissociate particular subjects from a role.
     * 
     * @param sessionSubject The logged in user's subject.
     * @param roleId The role ID to dissociate the roles from
     * @param subjectIds The IDs of the subjects to remove from the specified Role
     */
    @WebMethod
    void removeSubjectsFromRole( //
        @WebParam(name = "sessionSubject") Subject sessionSubject, //
        @WebParam(name = "roleId") int roleId, //
        @WebParam(name = "subjectIds") int[] subjectIds) //
        throws UpdateException;

    //available!
    /**
     * Adds the given resource groups to the given role.
     *
     * @param sessionSubject The logged in user's subject.
     * @param roleId
     * @param pendingGroupIds
     */
    @WebMethod
    void addResourceGroupsToRole( //
        @WebParam(name = "sessionSubject") Subject sessionSubject, //
        @WebParam(name = "roleId") int roleId, //
        @WebParam(name = "pendingGroupIds") int[] pendingGroupIds) //
        throws UpdateException;

    //does not exist
    @WebMethod
    void addRolesToResourceGroup( //
        @WebParam(name = "sessionSubject") Subject sessionSubject, //
        @WebParam(name = "groupId") int groupId, //
        @WebParam(name = "roleIds") int[] roleIds) //
        throws UpdateException;

    //available!
    /**
     * Removes the given resource groups from the given role.
     *
     * @param sessionSubject user attempting to remove the groups from the role
     * @param roleId
     * @param groupIds
     */
    @WebMethod
    void removeResourceGroupsFromRole( //
        @WebParam(name = "sessionSubject") Subject sessionSubject, //
        @WebParam(name = "roleId") int roleId, //
        @WebParam(name = "groupIds") int[] groupIds) //
        throws UpdateException;

    //not available
    @WebMethod
    void removeRolesFromResourceGroup( //
        @WebParam(name = "sessionSubject") Subject sessionSubject, //
        @WebParam(name = "groupId") int groupId, //
        @WebParam(name = "roleIds") int[] roleIds) //
        throws UpdateException;

}