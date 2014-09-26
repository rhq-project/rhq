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

import java.util.Collection;

import javax.ejb.Local;
import javax.persistence.EntityExistsException;

import org.rhq.core.domain.auth.Principal;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.exception.LoginException;

/**
 * The local EJB interface to the Authentication Boss.
 *
 * @author John Mazzitelli
 */
@Local
public interface SubjectManagerLocal extends SubjectManagerRemote {
    /**
     * Loads in the given subject's {@link Subject#getUserConfiguration() preferences} and
     * {@link Subject#getRoles() roles}.
     *
     * @param  subjectId identifies the subject whose preferences and roles are to be loaded
     *
     * @return the subject, with its preferences and roles loaded
     */
    Subject loadUserConfiguration(Integer subjectId);

    /**
     * This returns the system super user subject that can be used to authorize the caller for any other system call.
     * This must <b>not</b> be exposed to remote clients.
     *
     * @return a subject that is authorized to do anything
     */
    Subject getOverlord();

    /**
     * Logs in a user without performing any authentication. This method should be used with care and is not available to
     * remote clients. Because of the unauthenticated nature of this login, the new login session will have a session
     * timeout of only two minutes.
     *
     * @param  username The user to login
     *
     * @return A Subject with an active session for the the user
     *
     * @throws LoginException if failed to create a new session for the given user
     */
    Subject loginUnauthenticated(String username) throws LoginException;
    
    /**
     * Logs in a user performing the authentication. The difference between this method and 
     * {@link SubjectManagerRemote#login(String, String)} method is that the latter is meant to be used only for CLI, 
     * because if LDAP user without any role is trying to log in (and it is disabled) this one lets the user in 
     * because of the LDAP registration form.
     *
     * @param  username The user to login
     * @return A Subject with an active session for the the user
     * @throws LoginException if failed to create a new session for the given user
     */
    Subject loginLocal(String username, String password) throws LoginException;

    /**
     * Creates a new principal (username and password) in the internal database.
     *
     * @param  subject   The subject of the currently logged in user
     * @param  principal The principal to add
     *
     * @throws Exception if the principal could not be added
     */
    void createPrincipal(Subject subject, Principal principal) throws SubjectException;

    /**
     * Checks that the user exists <b>and</b> has a {@link Principal} associated with it. This means that the user both
     * exists and is authenticated via JDBC. An LDAP user will not have a {@link Principal} because it is authenticated
     * via the LDAP server, not from the database.
     *
     * @param  username the user whose existence is to be checked
     *
     * @return <code>true</code> if the user exists and has a {@link Principal}, <code>false</code> otherwise
     */
    boolean isUserWithPrincipal(String username);

    /**
     * Get a collection of all user names, where the collection contains the names of all users that have principals
     * only. You will not get a list of usernames for those users authenticated by LDAP.
     *
     * @return collection of all user names that have principals
     */
    Collection<String> findAllUsersWithPrincipals();

    /**
     * Deletes the given set of users, including both the {@link Subject} and {@link Principal} objects associated with
     * those users.
     *
     * @param  subject    the person requesting the deletion
     * @param  subjectIds identifies the subject IDs for all the users that are to be deleted
     *
     * @throws Exception if failed to delete one or more users
     */
    void deleteUsers(Subject subject, int[] subjectIds);

    /**
     * Determines if the given session ID is valid and it is associated with the given username and user ID.
     *
     * @param  session
     * @param  username
     * @param  userid
     *
     * @return <code>true</code> if the session ID indentifies a valid session; <code>false</code> if it is invalid or
     *         has timed out
     */
    boolean isValidSessionId(int session, String username, int userid);

    /**
     * This returns a list of subjects that are available to be assigned to a given role but not yet assigned to that
     * role. This excludes subjects already assigned to the role. The returned list will not include the subjects
     * identified by <code>pendingSubjectIds</code> since it is assumed the pending subjects will be assigned to the
     * role.
     *
     * @param  whoami            user attempting to make this call
     * @param  roleId            the role whose list of available subjects are to be returned
     * @param  pendingSubjectIds the list of subjects that are planned to be given to the role
     * @param  pc
     *
     * @return the list of subjects that can be assigned to the given role, not including the pending subjects
     */
    PageList<Subject> findAvailableSubjectsForRole(Subject whoami, Integer roleId, Integer[] pendingSubjectIds,
        PageControl pc);

    void logout(int sessionId);

    Subject getSubjectById(int id);

    Subject getSubjectBySessionId(int sessionId) throws Exception;

    /**
     * Creates a new subject, including their assigned roles, as well as an associated principal with the specified
     * password.
     *
     * @param subject the logged in user's subject
     * @param subjectToCreate the subject to be created (which will never be the same as <code>subject</code>)
     * @param password the password for the principal to be created for the new user
     *
     * @return the persisted subject
     */
    Subject createSubject(Subject subject, Subject subjectToCreate, String password) throws SubjectException,
        EntityExistsException;

    /**
     * Updates an existing subject, including their assigned roles, and optionally their password.
     *
     * @param  subject         the logged in user's subject
     * @param  subjectToModify the subject whose data is to be updated (which may or may not be the same as <code>subject</code>)
     * @param  newPassword     if non-null, a new password to be set on the user's associated principal
     *
     * @return the merged subject, which may or may not be the <code>subjectToModify</code> instance
     */
    Subject updateSubject(Subject subject, Subject subjectToModify, String newPassword);

    /**
     * Checks whether a user would successfully login with the provided credentials.
     * 
     * @param username the username
     * @param password the password
     * @return the subject if the credentials are correct, null otherwise
     */
    Subject checkAuthentication(String username, String password);

    /**
     * For internal use only - used by the StartupBean only - don't call this.
     * This will schedule the periodic EJB timer to trigger a purge of all timed out sessions.
     */
    void scheduleSessionPurgeJob();
    
    Subject processSubjectForLdap(Subject subject, String subjectPassword) throws LoginException;
}