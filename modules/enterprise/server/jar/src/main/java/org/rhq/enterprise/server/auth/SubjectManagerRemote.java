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
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.authz.RoleManagerLocal;
import org.rhq.enterprise.server.exception.CreateException;
import org.rhq.enterprise.server.exception.DeleteException;
import org.rhq.enterprise.server.exception.FetchException;
import org.rhq.enterprise.server.exception.LoginException;
import org.rhq.enterprise.server.exception.UpdateException;

@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService
@Remote
public interface SubjectManagerRemote {

    /**
     * Change the password for a user.
     *
     * @param  subject  The logged in user's subject.
     * @param  username The user whose password will be changed
     * @param  password The new password for the user
     *
     * @throws UpdateException if the password could not be changed
     */
    @WebMethod
    void changePassword( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "username") String username, //
        @WebParam(name = "password") String password) //
        throws UpdateException;

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
        @WebParam(name = "password") String password) throws CreateException;

    /**
     * Create a a new subject. This <b>ignores</b> the roles in <code>subject</code>. The created subject will not be
     * assigned to any roles; use the {@link RoleManagerLocal role manager} to assign roles to a subject.
     *
     * @param  subject         The logged in user's subject.
     * @param  subjectToCreate The subject to be created.
     *
     * @return the newly persisted {@link Subject}
     *
     * @throws CreateException if there is already a subject with the same name
     */
    @WebMethod
    Subject createSubject( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "subjectToCreate") Subject subjectToCreate) throws CreateException;

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
        @WebParam(name = "subjectIds") int[] subjectIds) //
        throws DeleteException;

    /**
     * Looks up the existing of a subject by the given username.
     *
     * @param  username the name of the subject to look for
     *
     * @return the subject that was found or <code>null</code> if not found
     */
    @WebMethod
    Subject getSubjectByName( //
        @WebParam(name = "username") String username);

    /**
     * This find service can be used to find subjects based on various criteria and return various data.
     *
     * @param subject  The logged in user's subject.
     * @param criteria {@link Resource}, can be null
     * <pre>
     * If provided the Subject object can specify various search criteria as specified below.
     *   Subject.id : exact match
     *   Subject.description : case insensitive substring match
     *   Subject.firstName
     *   Subject.lastName
     *   Subject.emailAddress   
     * </pre>
     * @param pc {@link PageControl}
     * <pre>
     * If provided PageControl specifies page size, requested page, sorting, and optional data.
     * 
     * Supported OptionalData
     *   To specify optional data call pc.setOptionalData() and supply one of more of the DATA_* constants
     *   defined in this interface.
     * 
     * Supported Sorting:
     *   Possible values to provide PageControl for sorting (PageControl.orderingFields)
     *     name
     *     firstName
     *     lastName
     *   
     * </pre>
     * @return
     * @throws FetchException
     */
    @WebMethod
    PageList<Subject> findSubjects( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "username") Subject criteria, //
        @WebParam(name = "pageControl") PageControl pc) //
        throws FetchException;

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
     * @param username The username for the current user
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
        @WebParam(name = "subjectToModify") Subject subjectToModify) //
        throws UpdateException;

    @WebMethod
    PageList<Subject> findSubjectsByCriteria(//
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "criteria") SubjectCriteria criteria);

}