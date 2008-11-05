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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.ejb.CreateException;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.ExcludeDefaultInterceptors;
import javax.jws.WebService;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.security.Util;
import org.jboss.security.auth.callback.UsernamePasswordHandler;

import org.rhq.core.domain.auth.Principal;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.core.CustomJaasDeploymentServiceMBean;
import org.rhq.enterprise.server.legacy.common.shared.HQConstants;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Provides functionality to access and manipulate subjects and principals, mainly for authentication purposes.
 *
 * @author John Mazzitelli
 */
@Stateless
@WebService(endpointInterface = "org.rhq.enterprise.server.auth.SubjectManagerRemote")
public class SubjectManagerBean implements SubjectManagerLocal, SubjectManagerRemote {
    private final Log log = LogFactory.getLog(SubjectManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;
    private SessionManager sessionManager = SessionManager.getInstance();

    /**
     * This is used to generate temporary session passwords and to validate those passwords.
     */
    private TemporarySessionPasswordGenerator m_sessionPasswordGenerator = new TemporarySessionPasswordGenerator();

    /**
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#loadUserConfiguration(Integer)
     */
    public Subject loadUserConfiguration(Integer subjectId) {
        Subject subject = entityManager.find(Subject.class, subjectId);
        Configuration config = subject.getUserConfiguration();
        if ((config != null) && (config.getProperties() != null)) {
            config.getProperties().size(); // force it to load
        }

        if (subject.getRoles() != null) {
            subject.getRoles().size();
        }

        return subject;
    }

    /**
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#getSubjectsById(Integer[],PageControl)
     */
    @SuppressWarnings("unchecked")
    public PageList<Subject> getSubjectsById(Integer[] subjectIds, PageControl pc) {
        if ((subjectIds == null) || (subjectIds.length == 0)) {
            return new PageList<Subject>(pc);
        }

        pc.initDefaultOrderingField("s.name");

        String query_name = Subject.QUERY_FIND_BY_IDS;
        Query subject_query_count = PersistenceUtility.createCountQuery(entityManager, query_name);
        Query subject_query = PersistenceUtility.createQueryWithOrderBy(entityManager, query_name, pc);

        List<Integer> subject_ids_list = Arrays.asList(subjectIds);
        subject_query_count.setParameter("ids", subject_ids_list);
        subject_query.setParameter("ids", subject_ids_list);

        long total_count = (Long) subject_query_count.getSingleResult();

        List<Subject> subjects = subject_query.getResultList();

        if (subjects != null) {
            // eagerly load in the members - can't use left-join due to PersistenceUtility usage; perhaps use EAGER
            for (Subject subject : subjects) {
                subject.getRoles().size();
            }
        } else {
            subjects = new ArrayList<Subject>();
        }

        return new PageList<Subject>(subjects, (int) total_count, pc);
    }

    /**
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#updateSubject(Subject, Subject)
     */
    public Subject updateSubject(Subject whoami, Subject subjectToModify) {
        // let a user change his own details
        if (whoami.equals(subjectToModify)
            || authorizationManager.getExplicitGlobalPermissions(whoami).contains(Permission.MANAGE_SECURITY)) {
            if (subjectToModify.getFsystem() || (authorizationManager.isSystemSuperuser(subjectToModify))) {
                if (!subjectToModify.getFactive()) {
                    throw new PermissionException("You cannot disable user [" + subjectToModify.getName()
                        + "] - it must always be active");
                }
            }
        } else {
            throw new PermissionException("You [" + whoami.getName() + "] do not have permission to update user ["
                + subjectToModify.getName() + "]");
        }

        return entityManager.merge(subjectToModify);
    }

    /**
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#getOverlord()
     */
    public Subject getOverlord() {
        return sessionManager.getOverlord();
    }

    /**
     * @see org.rhq.enterprise.server.auth.SubjectManagerRemote#findSubjectByName(Subject,String)
     */
    @RequiredPermission(Permission.MANAGE_SECURITY)
    public Subject findSubjectByName(Subject user, String username) {
        return findSubjectByName(username);
    }

    /**
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#findSubjectByName(String)
     */
    public Subject findSubjectByName(String username) {
        Subject subject;

        try {
            subject = (Subject) entityManager.createNamedQuery(Subject.QUERY_FIND_BY_NAME).setParameter("name",
                username).getSingleResult();
        } catch (NoResultException nre) {
            subject = null;
        }

        return subject;
    }

    /**
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#createSubject(Subject, Subject)
     */
    @RequiredPermission(Permission.MANAGE_SECURITY)
    public Subject createSubject(Subject whoami, Subject subject) throws CreateException {
        // Make sure there's not already a system subject with that name
        if (findSubjectByName(subject.getName()) != null) {
            throw new CreateException("A user already exists with " + subject.getName());
        }

        if (subject.getFsystem()) {
            throw new CreateException("Cannot create new system subjects: " + subject.getName());
        }

        // we are ignoring roles - anything the caller gave us is thrown out
        subject.setRoles(null);
        Configuration configuration = subject.getUserConfiguration();
        if (configuration != null) {
            configuration = entityManager.merge(configuration);
            subject.setUserConfiguration(configuration);
        }

        entityManager.persist(subject);

        return subject;
    }

    /**
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#getAllSubjects(PageControl)
     */
    @SuppressWarnings("unchecked")
    public PageList<Subject> getAllSubjects(PageControl pc) {
        pc.initDefaultOrderingField("s.name");

        String queryName = Subject.QUERY_FIND_ALL;
        Query subjectQueryCount = PersistenceUtility.createCountQuery(entityManager, queryName);
        Query subjectQuery = PersistenceUtility.createQueryWithOrderBy(entityManager, queryName, pc);

        long totalCount = (Long) subjectQueryCount.getSingleResult();

        List<Subject> subjects = subjectQuery.getResultList();

        return new PageList<Subject>(subjects, (int) totalCount, pc);
    }

    /**
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#findSubjectById(int)
     */
    public Subject findSubjectById(int id) {
        Subject subject = entityManager.find(Subject.class, id);
        return subject;
    }

    /**
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#generateTemporarySessionPassword(int)
     */
    public String generateTemporarySessionPassword(int sessionId) {
        return m_sessionPasswordGenerator.generateSessionPassword(sessionId);
    }

    /**
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#authenticateTemporarySessionPassword(java.lang.String)
     */
    public boolean authenticateTemporarySessionPassword(String password) throws Exception {
        Integer sessionId = m_sessionPasswordGenerator.authenticateSessionPassword(password);
        boolean validPassword = false;

        if (sessionId != null) {
            // If the password was valid, sessionId will be the ID to its associated session.  We now have to make
            // sure that session is still valid - this just makes sure the session hasn't timed out or was invalidated
            if (sessionManager.getSubject(sessionId.intValue()) != null) {
                validPassword = true;
            }
        }

        return validPassword;
    }

    /**
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#login(String, String)
     */
    public Subject login(String username, String password) throws LoginException {
        if (password == null) {
            throw new LoginException("No password was given");
        }

        // get the configuration properties and use the JAAS modules to perform the login
        Properties config = LookupUtil.getSystemManager().getSystemConfiguration();
        UsernamePasswordHandler handler = new UsernamePasswordHandler(username, password.toCharArray());
        LoginContext loginContext = new LoginContext(CustomJaasDeploymentServiceMBean.SECURITY_DOMAIN_NAME, handler);
        loginContext.login();
        loginContext.getSubject().getPrincipals().iterator().next();
        loginContext.logout();

        // User is authenticated!

        Subject subject = findSubjectByName(username);

        if (subject != null) {
            if (!subject.getFactive()) {
                throw new LoginException("User account has been disabled.");
            }

            // let's see if this user was already logged in with a valid session
            try {
                int session_id = sessionManager.getSessionIdFromUsername(username);
                subject.setSessionId(session_id);
                return subject;
            } catch (SessionException se) {
                // nope, no session; continue on so we can create the session
            }
        } else {
            // There is no subject in the database yet.
            // If LDAP authentication is enabled and we cannot find the subject,
            // it means we must have authenticated via LDAP, not JDBC (otherwise,
            // how else can there be a Principal without a Subject?).  In the
            // case of LDAP authenticated without having a Subject, it means the
            // user is logging in for the first time and must go through a special
            // GUI workflow to create a subject record.  Let's create a dummy
            // placeholder subject in here for now.
            if (config.getProperty(HQConstants.JAASProvider).equals(HQConstants.LDAPJAASProvider)) {
                subject = new Subject();
                subject.setId(0);
                subject.setName(username);
                subject.setFactive(true);
                subject.setFsystem(false);
            } else {
                // LDAP is not enabled, so how in the world did we authenticate?  This should never happen
                throw new IllegalStateException(
                    "Somehow you authenticated with a principal that has no associated subject. Your account is invalid.");
            }
        }

        sessionManager.put(subject);

        return subject;
    }

    /**
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#logout(int)
     */
    public void logout(int sessionId) {
        sessionManager.invalidate(sessionId);
    }

    /**
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#isLoggedIn(java.lang.String)
     */
    public boolean isLoggedIn(String username) {
        boolean loggedIn = false;

        try {
            sessionManager.getSessionIdFromUsername(username);
            loggedIn = true;
        } catch (SessionException e) {
            // safely ignore
        }

        return loggedIn;
    }

    /**
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#createPrincipal(Subject, String, String)
     */
    @RequiredPermission(Permission.MANAGE_SECURITY)
    public void createPrincipal(Subject whoami, String username, String password) throws Exception {
        Principal principal = new Principal(username, Util.createPasswordHash("MD5", "base64", null, null, password));
        entityManager.persist(principal);
    }

    /**
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#createPrincipal(Subject, Principal)
     */
    @RequiredPermission(Permission.MANAGE_SECURITY)
    public void createPrincipal(Subject whoami, Principal principal) throws Exception {
        entityManager.persist(principal);
    }

    /**
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#changePassword(Subject, String, String)
     */
    public void changePassword(Subject whoami, String username, String password) throws Exception {
        // a user can change his own password, as can a user with the appropriate permission
        if (!whoami.getName().equals(username)) {
            if (!authorizationManager.hasGlobalPermission(whoami, Permission.MANAGE_SECURITY)) {
                throw new PermissionException("You do not have permission to change the password for user [" + username
                    + "]");
            }
        }

        Query q = entityManager.createNamedQuery(Principal.QUERY_FIND_BY_USERNAME);
        q.setParameter("principal", username);
        Principal local = (Principal) q.getSingleResult();
        String hash = Util.createPasswordHash("MD5", "base64", null, null, password);
        local.setPassword(hash);

        return;
    }

    /**
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#isUserWithPrincipal(String)
     */
    public boolean isUserWithPrincipal(String username) {
        try {
            Query q = entityManager.createNamedQuery(Principal.QUERY_FIND_BY_USERNAME);
            q.setParameter("principal", username);
            Principal principal = (Principal) q.getSingleResult();
            return (principal != null);
        } catch (NoResultException e) {
            return false;
        }
    }

    /**
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#getAllUsersWithPrincipals()
     */
    @SuppressWarnings("unchecked")
    public Collection<String> getAllUsersWithPrincipals() {
        Query q = entityManager.createNamedQuery(Principal.QUERY_FIND_ALL_USERS);
        List<Principal> principals = q.getResultList();

        List<String> users = new ArrayList<String>();

        for (Principal p : principals) {
            users.add(p.getPrincipal());
        }

        return users;
    }

    /**
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#loginUnauthenticated(String, boolean)
     */
    public Subject loginUnauthenticated(String username, boolean reattach) throws LoginException {
        if (reattach) {
            try {
                int session_id = sessionManager.getSessionIdFromUsername(username);
                return sessionManager.getSubject(session_id);
            } catch (SessionException e) {
                // continue, we'll need to create a session
            }
        }

        Subject subject = findSubjectByName(username);

        if (subject == null) {
            throw new LoginException("User account does not exist. [" + username + "]");
        }

        if (!subject.getFactive()) {
            throw new LoginException("User account has been disabled. [" + username + "]");
        }

        sessionManager.put(subject, 1000L * 60 * 2); // 2mins only

        return subject;
    }

    /**
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#deleteUsers(Subject, Integer[])
     */
    @RequiredPermission(Permission.MANAGE_SECURITY)
    public void deleteUsers(Subject whoami, Integer[] subjectIds) throws Exception {
        for (Integer doomedSubjectId : subjectIds) {
            Subject doomedSubject = findSubjectById(doomedSubjectId);

            if (whoami.getName().equals(doomedSubject.getName())) {
                throw new PermissionException("You cannot remove yourself: " + doomedSubject.getName());
            }

            Set<Role> roles = doomedSubject.getRoles();
            doomedSubject.setRoles(new HashSet<Role>()); // clean out roles

            for (Role doomedRoleRelationship : roles) {
                doomedRoleRelationship.removeSubject(doomedSubject);
            }

            doomedSubject = entityManager.merge(doomedSubject);

            // TODO: we need to reassign ownership of things this user used to own

            // if this user was authenticated via JDBC and thus has a principal, remove it
            if (isUserWithPrincipal(doomedSubject.getName())) {
                deletePrincipal(whoami, doomedSubject);
            }

            deleteSubject(whoami, doomedSubject);
        }

        return;
    }

    /**
     * Deletes the given {@link Subject} from the database.
     *
     * @param  whoami        the person requesting the deletion
     * @param  doomedSubject identifies the subject to delete
     *
     * @throws Exception           if failed to delete one or more users
     * @throws PermissionException if caller tried to delete a system superuser
     */
    private void deleteSubject(Subject whoami, Subject doomedSubject) throws Exception {
        if (authorizationManager.isSystemSuperuser(doomedSubject)) {
            throw new PermissionException("You cannot delete a system root user - they must always exist");
        }

        entityManager.remove(doomedSubject);

        return;
    }

    /**
     * Delete a user's principal from the internal database.
     *
     * @param  whoami  The subject of the currently logged in user
     * @param  subject The user whose principal is to be deleted
     *
     * @throws Exception           if failed to delete the user's principal
     * @throws PermissionException if the caller tried to delete a system superuser
     */
    private void deletePrincipal(Subject whoami, Subject subject) throws Exception {
        if (authorizationManager.isSystemSuperuser(subject)) {
            throw new PermissionException("You cannot delete the principal for the root user [" + subject.getName()
                + "]");
        }

        Query q = entityManager.createNamedQuery(Principal.QUERY_FIND_BY_USERNAME);
        q.setParameter("principal", subject.getName());
        Principal principal = (Principal) q.getSingleResult();
        entityManager.remove(principal);
    }

    /**
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#getSessionSubject(int)
     */
    public Subject getSessionSubject(int sessionId) throws Exception {
        Subject subject = sessionManager.getSubject(sessionId);
        return subject;
    }

    /**
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#isValidSessionId(int, String)
     */
    // we exclude the default interceptors because the required permissions interceptor calls into this
    @ExcludeDefaultInterceptors
    public boolean isValidSessionId(int session, String username) {
        try {
            Subject sessionSubject = sessionManager.getSubject(session);
            return username.equals(sessionSubject.getName());
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public PageList<Subject> getAvailableSubjectsForAlertDefinition(Subject whoami, Integer alertDefinitionId,
        Integer[] pendingSubjectIds, PageControl pc) {
        pc.initDefaultOrderingField("s.name");

        String query_name;

        if ((pendingSubjectIds == null) || (pendingSubjectIds.length == 0)) {
            query_name = Subject.QUERY_FIND_AVAILABLE_SUBJECTS_FOR_ALERT_DEFINITION;
        } else {
            query_name = Subject.QUERY_FIND_AVAILABLE_SUBJECTS_FOR_ALERT_DEFINITION_WITH_EXCLUDES;
        }

        Query role_query_count = PersistenceUtility.createCountQuery(entityManager, query_name, "distinct s");
        Query role_query = PersistenceUtility.createQueryWithOrderBy(entityManager, query_name, pc);

        role_query_count.setParameter("alertDefinitionId", alertDefinitionId);
        role_query.setParameter("alertDefinitionId", alertDefinitionId);

        if ((pendingSubjectIds != null) && (pendingSubjectIds.length > 0)) {
            List<Integer> pending_ids_list = Arrays.asList(pendingSubjectIds);
            role_query_count.setParameter("excludes", pending_ids_list);
            role_query.setParameter("excludes", pending_ids_list);
        }

        long total_count = (Long) role_query_count.getSingleResult();

        List<Subject> subjects = role_query.getResultList();

        // eagerly load in the roles - can't use left-join due to PersistenceUtility usage; perhaps use EAGER
        for (Subject subject : subjects) {
            subject.getRoles().size();
        }

        return new PageList<Subject>(subjects, (int) total_count, pc);
    }

    @RequiredPermission(Permission.MANAGE_SECURITY)
    @SuppressWarnings("unchecked")
    public PageList<Subject> getAvailableSubjectsForRole(Subject whoami, Integer roleId, Integer[] pendingSubjectIds,
        PageControl pc) {
        pc.initDefaultOrderingField("s.name");

        String query_name;

        if ((pendingSubjectIds == null) || (pendingSubjectIds.length == 0)) {
            query_name = Subject.QUERY_FIND_AVAILABLE_SUBJECTS_FOR_ROLE;
        } else {
            query_name = Subject.QUERY_FIND_AVAILABLE_SUBJECTS_FOR_ROLE_WITH_EXCLUDES;
        }

        Query role_query_count = PersistenceUtility.createCountQuery(entityManager, query_name, "distinct s");
        Query role_query = PersistenceUtility.createQueryWithOrderBy(entityManager, query_name, pc);

        role_query_count.setParameter("roleId", roleId);
        role_query.setParameter("roleId", roleId);

        if ((pendingSubjectIds != null) && (pendingSubjectIds.length > 0)) {
            List<Integer> pending_ids_list = Arrays.asList(pendingSubjectIds);
            role_query_count.setParameter("excludes", pending_ids_list);
            role_query.setParameter("excludes", pending_ids_list);
        }

        long total_count = (Long) role_query_count.getSingleResult();

        List<Subject> subjects = role_query.getResultList();

        // eagerly load in the roles - can't use left-join due to PersistenceUtility usage; perhaps use EAGER
        for (Subject subject : subjects) {
            subject.getRoles().size();
        }

        return new PageList<Subject>(subjects, (int) total_count, pc);
    }
}