/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.gwt;

import com.google.gwt.user.client.rpc.RemoteService;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.SubjectCriteria;
import org.rhq.core.domain.util.PageList;

/**
 * @author Greg Hinkle
 * @see org.rhq.enterprise.server.auth.SubjectManagerRemote
 */
public interface SubjectGWTService extends RemoteService {

    /**
     * Change the password for a user.
     *
     * @param username The user whose password will be changed
     * @param password The new password for the user
     */
    void changePassword(String username, String password);

    /**
     * Creates a new principal (username and password) in the internal database. The password will be encoded before
     * being stored.
     *
     * @param username The username part of the principal
     * @param password The password part ofthe principal
     * @throws Exception if the principal could not be added
     */
    void createPrincipal(String username, String password);

    /**
     * Create a a new subject. This <b>ignores</b> the roles in <code>subject</code>. The created subject will not be
     * assigned to any roles; use the {@link org.rhq.enterprise.server.authz.RoleManagerLocal role manager} to assign roles to a subject.
     *
     * @param subjectToCreate The subject to be created.
     * @return the newly persisted {@link Subject}
     */
    Subject createSubject(Subject subjectToCreate);

    /**
     * Create a a new subject. Same as createSubject, but uses overlord privileges to automate Subject creation. Ex. Used during
     * LDAP logins.
     *
     * @param subjectToCreate The subject to be created.
     * @return the newly persisted {@link Subject}
     */
    Subject createSubjectUsingOverlord(Subject subjectToCreate);

    /**
     * Deletes the given set of users, including both the {@link Subject} and {@link org.rhq.core.domain.auth.Principal} objects associated with
     * those users.
     *
     * @param subjectIds identifies the subject IDs for all the users that are to be deleted
     * @throws Exception if failed to delete one or more users
     */
    void deleteSubjects(int[] subjectIds);

    /**
     * Logs a user into the system. This will authenticate the given user with the given password. If the user was
     * already logged in, the current session will be used but the password will still need to be authenticated.
     *
     * @param username The name of the user.
     * @param password The password.
     * @return The subject of the authenticated user.
     * @throws org.rhq.enterprise.server.exception.LoginException
     *          if the login failed for some reason
     */
    Subject login(String username, String password);

    /**
     * Logs out a user.
     *
     * @param subject The username for the current user
     */
    void logout(Subject subject);

    /**
     * Updates an existing subject with new data. This does <b>not</b> cascade any changes to the roles but it will save
     * the subject's configuration.
     *
     * @param subjectToModify the subject whose data is to be updated (which may or may not be the same as <code>user</code>)
     * @return the merged subject, which may or may not be the same instance of <code>subjectToModify</code>
     */
    Subject updateSubject(Subject subjectToModify);

    PageList<Subject> findSubjectsByCriteria(SubjectCriteria criteria);

}
