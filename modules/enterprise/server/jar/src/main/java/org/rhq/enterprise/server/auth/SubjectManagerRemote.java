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

import javax.ejb.CreateException;
import javax.ejb.Remote;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.security.auth.login.LoginException;
import javax.xml.bind.annotation.XmlSeeAlso;

import org.rhq.core.domain.auth.Principal;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.enterprise.server.authz.RoleManagerLocal;

@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService
@Remote
@XmlSeeAlso( { PropertySimple.class, PropertyList.class, PropertyMap.class })
public interface SubjectManagerRemote {

    /**
     * Change the password for a user.
     *
     * @param  user  The logged in user's subject.
     * @param  username The user whose password will be changed
     * @param  password The new password for the user
     *
     * @throws Exception if the password could not be changed
     */
    @WebMethod
    void changePassword( //
        @WebParam(name = "user")
        Subject user, //
        @WebParam(name = "username")
        String username, //
        @WebParam(name = "password")
        String password) //
        throws Exception;

    /**
     * Creates a new principal (username and password) in the internal database. The password will be encoded before
     * being stored.
     *
     * @param  user  The logged in user's subject.
     * @param  username The username part of the principal
     * @param  password The password part ofthe principal
     *
     * @throws Exception if the principal could not be added
     */
    @WebMethod
    void createPrincipal( //
        @WebParam(name = "user")
        Subject user, //
        @WebParam(name = "username")
        String username, //
        @WebParam(name = "password")
        String password) throws Exception;

    /**
     * Create a a new subject. This <b>ignores</b> the roles in <code>subject</code>. The created subject will not be
     * assigned to any roles; use the {@link RoleManagerLocal role manager} to assign roles to a subject.
     *
     * @param  user  The logged in user's subject.
     * @param  subject The subject to be created.
     *
     * @return the newly persisted {@link Subject}
     *
     * @throws CreateException if there is already a subject with the same name
     */
    @WebMethod
    Subject createSubject( //
        @WebParam(name = "user")
        Subject user, //
        @WebParam(name = "subject")
        Subject subject) throws CreateException;

    /**
     * Deletes the given set of users, including both the {@link Subject} and {@link Principal} objects associated with
     * those users.
     *
     * @param  user  The logged in user's subject.
     * @param  subjectIds identifies the subject IDs for all the users that are to be deleted
     *
     * @throws Exception if failed to delete one or more users
     */
    @WebMethod
    void deleteUsers( //
        @WebParam(name = "user")
        Subject user, //
        @WebParam(name = "subjectIds")
        Integer[] subjectIds) throws Exception;

    /**
     * Looks up the existing of a subject by the given username.
     *
     * @param  user  The logged in user's subject.
     * @param  username the name of the subject to look for
     *
     * @return the subject that was found or <code>null</code> if not found
     */
    @WebMethod
    Subject findSubjectByName( //
        @WebParam(name = "user")
        Subject user, //
        @WebParam(name = "username")
        String username);

    /**
     * Check if a user is logged in.
     *
     * @param  username The name of the user.
     *
     * @return <code>true</code> if the given user is logged in; <code>false</code> if not.
     */
    @WebMethod
    boolean isLoggedIn( //
        @WebParam(name = "username")
        String username);

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
        @WebParam(name = "username")
        String username, //
        @WebParam(name = "password")
        String password) //
        throws LoginException;

    /**
     * Logs out a user.
     *
     * @param sessionId The session id for the current user
     */
    @WebMethod
    void logout( //
        @WebParam(name = "sessionId")
        int sessionId);

    /**
     * Updates an existing subject with new data. This does <b>not</b> cascade any changes to the roles but it will save
     * the subject's configuration.
     *
     * @param  user  The logged in user's subject.
     * @param  subjectToModify the subject whose data is to be updated (which may or may not be the same as <code>user</code>)
     *
     * @return the merged subject, which may or may not be the same instance of <code>subjectToModify</code>
     */
    @WebMethod
    Subject updateSubject( //
        @WebParam(name = "user")
        Subject user, //
        @WebParam(name = "subjectToModify")
        Subject subjectToModify);

}