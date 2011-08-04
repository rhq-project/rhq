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
package org.rhq.enterprise.server.auth;

import javax.ejb.Remote;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.rhq.core.domain.auth.Principal;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.SubjectCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.authz.RoleManagerLocal;
import org.rhq.enterprise.server.exception.LoginException;
import org.rhq.enterprise.server.system.ServerVersion;

@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService(targetNamespace = ServerVersion.namespace)
@Remote
public interface SubjectManagerRemote {

    /**
     * Change the password for a user.
     *
     * @param  subject  The logged in user's subject.
     * @param  username The user whose password will be changed
     * @param  password The new password for the user
     */
    @WebMethod
    void changePassword( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "username") String username, //
        @WebParam(name = "password") String password);

    /**
     * Creates a new principal (username and password) in the internal database. The password will be encoded before
     * being stored.
     *
     * @param  subject  The logged in user's subject.
     * @param  username The username part of the principal
     * @param  password The password part ofthe principal
     *
     * @throws Exception if the principal could not be added
     */
    @WebMethod
    void createPrincipal( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "username") String username, //
        @WebParam(name = "password") String password) //
        throws SubjectException;

    /**
     * Create a a new subject. This <b>ignores</b> the roles in <code>subject</code>. The created subject will not be
     * assigned to any roles; use the {@link RoleManagerLocal role manager} to assign roles to a subject.
     *
     * @param  subject         The logged in user's subject.
     * @param  subjectToCreate The subject to be created.
     *
     * @return the newly persisted {@link Subject}
     */
    @WebMethod
    Subject createSubject( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "subjectToCreate") Subject subjectToCreate) //
        throws SubjectException;

    /**
     * Deletes the given set of users, including both the {@link Subject} and {@link Principal} objects associated with
     * those users.
     *
     * @param  subject    The logged in user's subject.
     * @param  subjectIds identifies the subject IDs for all the users that are to be deleted
     *
     * @throws Exception if failed to delete one or more users
     */
    @WebMethod
    void deleteSubjects( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "subjectIds") int[] subjectIds);

    /**
     * Looks up the existing of a subject by the given username.
     *
     * @param  username the name of the subject to look for
     *
     * @return the subject that was found or <code>null</code> if not found
     * 
     * @deprecated This method should be avoided as it may be removed in
     * a future release.  Given that multiple sessions may exist for a
     * single user the result of this call is non-deterministic.
     */
    @WebMethod
    Subject getSubjectByName( //
        @WebParam(name = "username") String username);

    /**
     * Looks up the Subject for a current RHQ session by username and sessionId.
     *
     * @param username The name of the user.
     * @param sessionId The sessionId of the desired Subject.
     *
     * @return The Subject that was found
     * 
     * @throws Exception if the sessionId is not valid
     */
    @WebMethod
    Subject getSubjectByNameAndSessionId( //
        @WebParam(name = "username") String username, //        
        @WebParam(name = "sessionId") int sessionId) throws Exception;

    /**
     * Logs a user into the system. This will authenticate the given user with the given password. If the user was
     * already logged in, the current session will be used but the password will still need to be authenticated.
     *
     * @param     username The name of the user.
     * @param     password The password.
     *
     * @return    The subject of the authenticated user.
     *
     * @exception LoginException if the login failed for some reason
     */
    @WebMethod
    Subject login( //
        @WebParam(name = "username") String username, //
        @WebParam(name = "password") String password) //
        throws LoginException;

    /**
     * Logs out a user.
     *
     * @param subject The Subject to log out. The sessionId must be valid.
     */
    @WebMethod
    void logout( //
        @WebParam(name = "subject") Subject subject);

    /**
     * Updates an existing subject with new data. This does <b>not</b> cascade any changes to the roles but it will save
     * the subject's configuration.
     *
     * @param  subject         The logged in user's subject.
     * @param  subjectToModify the subject whose data is to be updated (which may or may not be the same as <code>user</code>)
     *
     * @return the merged subject, which may or may not be the same instance of <code>subjectToModify</code>
     */
    @WebMethod
    Subject updateSubject( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "subjectToModify") Subject subjectToModify);

    @WebMethod
    PageList<Subject> findSubjectsByCriteria(//
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "criteria") SubjectCriteria criteria);
}