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

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.ExcludeDefaultInterceptors;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.security.auth.login.LoginContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.annotation.IgnoreDependency;
import org.jboss.security.Util;
import org.jboss.security.auth.callback.UsernamePasswordHandler;

import org.rhq.core.domain.auth.Principal;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.criteria.SubjectCriteria;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.server.PersistenceUtility;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.alert.AlertNotificationManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.core.CustomJaasDeploymentServiceMBean;
import org.rhq.enterprise.server.exception.LoginException;
import org.rhq.enterprise.server.resource.group.LdapGroupManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;

/**
 * Provides functionality to access and manipulate subjects and principals, mainly for authentication purposes.
 *
 * @author John Mazzitelli
 */
@Stateless
public class SubjectManagerBean implements SubjectManagerLocal, SubjectManagerRemote {
    private final Log log = LogFactory.getLog(SubjectManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;

    @EJB
    @IgnoreDependency
    private ResourceGroupManagerLocal resourceGroupManager;

    @EJB
    @IgnoreDependency
    private LdapGroupManagerLocal ldapManager;

    @EJB
    private SystemManagerLocal systemManager;

    @EJB
    @IgnoreDependency
    private AlertNotificationManagerLocal alertNotificationManager;

    private SessionManager sessionManager = SessionManager.getInstance();

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
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#findSubjectsById(Integer[],PageControl)
     */
    @SuppressWarnings("unchecked")
    public PageList<Subject> findSubjectsById(Integer[] subjectIds, PageControl pc) {
        if ((subjectIds == null) || (subjectIds.length == 0)) {
            return new PageList<Subject>(pc);
        }

        pc.initDefaultOrderingField("s.name");

        String queryName = Subject.QUERY_FIND_BY_IDS;
        Query queryCount = PersistenceUtility.createCountQuery(entityManager, queryName);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, queryName, pc);

        List<Integer> subjectIdsList = Arrays.asList(subjectIds);
        queryCount.setParameter("ids", subjectIdsList);
        query.setParameter("ids", subjectIdsList);

        long count = (Long) queryCount.getSingleResult();
        List<Subject> subjects = query.getResultList();

        if (subjects != null) {
            // eagerly load in the members - can't use left-join due to PersistenceUtility usage; perhaps use EAGER
            for (Subject subject : subjects) {
                subject.getRoles().size();
            }
        } else {
            subjects = new ArrayList<Subject>();
        }

        return new PageList<Subject>(subjects, (int) count, pc);
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

        // Reset the roles and ldap roles according to the current settings as this method will not update them
        // To update assinged roles see RoleManager
        Subject currentSubject = entityManager.find(Subject.class, subjectToModify.getId());
        subjectToModify.setRoles(currentSubject.getRoles());
        subjectToModify.setLdapRoles(currentSubject.getLdapRoles());

        return entityManager.merge(subjectToModify);
    }

    /**
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#getOverlord()
     */
    public Subject getOverlord() {
        return sessionManager.getOverlord();
    }

    /**
     * @see org.rhq.enterprise.server.auth.SubjectManagerRemote#getSubjectByName(String)
     */
    public Subject getSubjectByName(String username) {
        Subject subject;

        try {
            Query query = entityManager.createNamedQuery(Subject.QUERY_FIND_BY_NAME);
            query.setParameter("name", username);
            subject = (Subject) query.getSingleResult();
        } catch (NoResultException nre) {
            subject = null;
        }

        return subject;
    }

    /**
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#createSubject(Subject, Subject)
     */
    @RequiredPermission(Permission.MANAGE_SECURITY)
    public Subject createSubject(Subject whoami, Subject subject) throws SubjectException {
        // Make sure there's not already a system subject with that name
        if (getSubjectByName(subject.getName()) != null) {
            throw new SubjectException("A user already exists with " + subject.getName());
        }

        if (subject.getFsystem()) {
            throw new SubjectException("Cannot create new system subjects: " + subject.getName());
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
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#findAllSubjects(PageControl)
     */
    @SuppressWarnings("unchecked")
    public PageList<Subject> findAllSubjects(PageControl pc) {
        pc.initDefaultOrderingField("s.name");

        String queryName = Subject.QUERY_FIND_ALL;
        Query subjectQueryCount = PersistenceUtility.createCountQuery(entityManager, queryName);
        Query subjectQuery = PersistenceUtility.createQueryWithOrderBy(entityManager, queryName, pc);

        long totalCount = (Long) subjectQueryCount.getSingleResult();

        List<Subject> subjects = subjectQuery.getResultList();

        return new PageList<Subject>(subjects, (int) totalCount, pc);
    }

    /**
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#getSubjectById(int)
     */
    public Subject getSubjectById(int id) {
        Subject subject = entityManager.find(Subject.class, id);
        return subject;
    }

    /**
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#login(String, String)
     */
    public Subject login(String username, String password) throws LoginException {
        if (password == null) {
            throw new LoginException("No password was given");
        }

        // get the configuration properties and use the JAAS modules to perform the login
        Properties config = systemManager.getSystemConfiguration();

        try {
            UsernamePasswordHandler handler = new UsernamePasswordHandler(username, password.toCharArray());
            LoginContext loginContext;
            loginContext = new LoginContext(CustomJaasDeploymentServiceMBean.SECURITY_DOMAIN_NAME, handler);

            loginContext.login();
            loginContext.getSubject().getPrincipals().iterator().next();
            loginContext.logout();
        } catch (javax.security.auth.login.LoginException e) {
            throw new LoginException(e.getMessage());
        }

        // User is authenticated!

        Subject subject = getSubjectByName(username);

        if (subject != null) {//regular JDBC user
            if (!subject.getFactive()) {
                throw new LoginException("User account has been disabled.");
            }

            // fetch the roles
            subject.getRoles().size();

            // let's see if this user was already logged in with a valid session
            try {
                int sessionId = sessionManager.getSessionIdFromUsername(username);
                subject.setSessionId(sessionId);
                //insert processing for LDAP users who have registered before and have jdbc credentials
                log.trace("Processing subject '" + subject.getName() + "' for LDAP functionality.");
                subject = processSubjectForLdap(subject, password, false);
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
            if (config.getProperty(RHQConstants.JAASProvider).equals(RHQConstants.LDAPJAASProvider)) {
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

    /**This method is applied to non-null Subject instances that may require LDAP auth/authz processing.
     * 
     * @param subject Authenticated subject.
     * @return same or new Subject returned from LDAP processing.
     * @throws LoginException 
     */
    public Subject processSubjectForLdap(Subject subject, String subjectPassword, boolean ldapRegistration)
        throws LoginException {

        if (subject != null) {//null check
            //if user has principal then bail as LDAP processing not required
            boolean userHasPrincipal = isUserWithPrincipal(subject.getName());

            //if user has principal then return as non-ldap user
            if (userHasPrincipal) {
                return subject; //bail. No further checking required.
            } else {//Start LDAP check.
                //retrieve configuration properties and do LDAP check
                Properties config = systemManager.getSystemConfiguration();
                boolean ldapConfigured = config.getProperty(RHQConstants.JAASProvider).equals(
                    RHQConstants.LDAPJAASProvider);

                if (ldapConfigured) {//i)registration ii)case sensitive matches iii)authorization updates
                    //check that session is valid. RHQ auth has already occurred.
                    if (!isValidSessionId(subject.getSessionId(), subject.getName(), subject.getId())) {
                        throw new LoginException("User session not valid. Login to proceed.");
                    }
                    if ((subject.getId() == 0) && ldapRegistration) {//insert overlord registration and login
                        //we've verified that this user has valid session, requires registration and that ldap is configured.
                        Subject superuser = getOverlord();

                        // create the subject, but don't add a principal since LDAP will handle authentication
                        log.trace("registering new LDAP-authenticated subject [" + subject.getName() + "]");
                        createSubject(superuser, subject);

                        // nuke the temporary session and establish a new
                        // one for this subject.. must be done before pulling the
                        // new subject in order to do it with his own credentials
                        logout(subject.getSessionId().intValue());
                        subject = login(subject.getName(), subjectPassword);
                    } else {//already registered
                        //BZ-586435: insert case insensitivity for usernames with ldap auth
                        // locate first matching subject and attach.
                        SubjectCriteria subjectCriteria = new SubjectCriteria();
                        subjectCriteria.setCaseSensitive(false);
                        subjectCriteria.setStrict(true);
                        subjectCriteria.fetchRoles(false);
                        subjectCriteria.fetchConfiguration(false);
                        subjectCriteria.addFilterName(subject.getName());
                        PageList<Subject> subjectsLocated = findSubjectsByCriteria(subject, subjectCriteria);
                        //if subject variants located then take the first one with a principal otherwise do nothing
                        //To defend against the case where they create an account with the same name but not 
                        //case as an rhq sysadmin or higher perms, then make them relogin with same creds entered.
                        if ((!subjectsLocated.isEmpty())
                            && (!subjectsLocated.get(0).getName().equals(subject.getName()))) {//then case insensitive username matches found. Try to use instead.
                            Subject ldapSubject = subjectsLocated.get(0);
                            String msg = "Located existing ldap account with different case for ["
                                + ldapSubject.getName() + "]. "
                                + "Attempting to authenticate with that account instead.";
                            log.info(msg);
                            subject = login(ldapSubject.getName(), subjectPassword);
                            Integer sessionId = subject.getSessionId();
                            log.debug("Logged in as [" + ldapSubject.getName() + "] with session id [" + sessionId
                                + "]");
                        }
                    }
                    {//now carry out authz refresh for this Subject
                        if (subject.getId() > 0) {
                            //BZ-580127: only do group authz check if one or both of group filter fields is set
                            Properties options = systemManager.getSystemConfiguration();
                            String groupFilter = (String) options.getProperty(RHQConstants.LDAPGroupFilter, "");
                            String groupMember = (String) options.getProperty(RHQConstants.LDAPGroupMember, "");
                            if ((groupFilter.trim().length() > 0) || (groupMember.trim().length() > 0)) {
                                List<String> groupNames = new ArrayList<String>(ldapManager
                                    .findAvailableGroupsFor(subject.getName()));
                                ldapManager.assignRolesToLdapSubject(subject.getId(), groupNames);
                            }
                        }
                    }
                }
            }
        }
        return subject;
    }

    /**
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#logout(Subject)
     */
    public void logout(Subject subject) {
        try {
            int sessionId = sessionManager.getSessionIdFromUsername(subject.getName());
            sessionManager.invalidate(sessionId);
        } catch (SessionTimeoutException ste) {
            // it's ok, logout can be considered successful if the user's session already timed out
        } catch (SessionNotFoundException snfe) {
            // it's ok, logout can be considered successful if the user's never logged in to begin with
        }
    }

    /**
     * Logs out a user.
     *
     * @param sessionId The sessionId for the user to log out
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
    public void createPrincipal(Subject whoami, String username, String password) throws SubjectException {
        Principal principal = new Principal(username, Util.createPasswordHash("MD5", "base64", null, null, password));
        createPrincipal(whoami, principal);
    }

    /**
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#createPrincipal(Subject, Principal)
     */
    @RequiredPermission(Permission.MANAGE_SECURITY)
    public void createPrincipal(Subject whoami, Principal principal) throws SubjectException {
        try {
            entityManager.persist(principal);
        } catch (Exception e) {
            throw new SubjectException("Failed creating principal: " + e.getMessage());
        }
    }

    /**
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#changePassword(Subject, String, String)
     */
    public void changePassword(Subject whoami, String username, String password) {
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
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#findAllUsersWithPrincipals()
     */
    @SuppressWarnings("unchecked")
    public Collection<String> findAllUsersWithPrincipals() {
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
                int sessionId = sessionManager.getSessionIdFromUsername(username);
                return sessionManager.getSubject(sessionId);
            } catch (SessionException e) {
                // continue, we'll need to create a session
            }
        }

        Subject subject = getSubjectByName(username);

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
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#deleteUsers(Subject, int[])
     */
    @RequiredPermission(Permission.MANAGE_SECURITY)
    public void deleteUsers(Subject subject, int[] subjectIds) {
        for (Integer doomedSubjectId : subjectIds) {
            Subject doomedSubject = getSubjectById(doomedSubjectId);

            if (subject.getName().equals(doomedSubject.getName())) {
                throw new PermissionException("You cannot remove yourself: " + doomedSubject.getName());
            }

            Set<Role> roles = doomedSubject.getRoles();
            doomedSubject.setRoles(new HashSet<Role>()); // clean out roles

            for (Role doomedRoleRelationship : roles) {
                doomedRoleRelationship.removeSubject(doomedSubject);
            }

            // TODO: we need to reassign ownership of things this user used to own

            // if this user was authenticated via JDBC and thus has a principal, remove it
            if (isUserWithPrincipal(doomedSubject.getName())) {
                deletePrincipal(subject, doomedSubject);
            }

            // one more thing, delete any owned groups
            List<ResourceGroup> ownedGroups = doomedSubject.getOwnedGroups();
            if (null != ownedGroups && !ownedGroups.isEmpty()) {
                int size = ownedGroups.size();
                int[] ownedGroupIds = new int[size];
                for (int i = 0; (i < size); ++i) {
                    ownedGroupIds[i] = ownedGroups.get(i).getId();
                }
                try {
                    resourceGroupManager.deleteResourceGroups(subject, ownedGroupIds);
                } catch (Throwable t) {
                    if (log.isDebugEnabled()) {
                        log.error("Error deleting owned group " + ownedGroupIds, t);
                    } else {
                        log.error("Error deleting owned group " + ownedGroupIds + ": " + t.getMessage());
                    }
                }
            }

            deleteSubject(subject, doomedSubject);
        }

        return;
    }

    /**
     * @see org.rhq.enterprise.server.auth.SubjectManagerRemote#deleteSubjects(org.rhq.core.domain.auth.Subject, int[])
     * TODO: A wrapper method for deleteUsers, exposed in remote, both should be merged at some point.
     */
    public void deleteSubjects(Subject sessionSubject, int[] subjectIds) {
        deleteUsers(sessionSubject, subjectIds);
    }

    /**
     * Deletes the given {@link Subject} from the database.
     *
     * @param  whoami        the person requesting the deletion
     * @param  doomedSubject identifies the subject to delete
     *
     * @throws PermissionException if caller tried to delete a system superuser
     */
    private void deleteSubject(Subject whoami, Subject doomedSubject) throws PermissionException {
        if (authorizationManager.isSystemSuperuser(doomedSubject)) {
            throw new PermissionException("You cannot delete a system root user - they must always exist");
        }

        alertNotificationManager.cleanseAlertNotificationBySubject(doomedSubject.getId());
        entityManager.remove(doomedSubject);

        return;
    }

    /**
     * Delete a user's principal from the internal database.
     *
     * @param  whoami  The subject of the currently logged in user
     * @param  subject The user whose principal is to be deleted
     *
     * @throws PermissionException if the caller tried to delete a system superuser
     */
    private void deletePrincipal(Subject whoami, Subject subject) throws PermissionException {
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
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#getSubjectBySessionId(int)
     */
    public Subject getSubjectBySessionId(int sessionId) throws Exception {
        Subject subject = sessionManager.getSubject(sessionId);

        return subject;
    }

    /**
     * Adds more security in the remote api call by requiring matching username
     */
    public Subject getSubjectByNameAndSessionId(String username, int sessionId) throws Exception {
        Subject subject = getSubjectBySessionId(sessionId);

        if (!username.equals(subject.getName())) {
            throw new SessionNotFoundException();
        }

        return subject;
    }

    /**
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#isValidSessionId(int, String, int)
     */
    // we exclude the default interceptors because the required permissions interceptor calls into this
    @ExcludeDefaultInterceptors
    public boolean isValidSessionId(int session, String username, int userid) {
        try {
            Subject sessionSubject = sessionManager.getSubject(session);
            return username.equals(sessionSubject.getName()) && userid == sessionSubject.getId();
        } catch (Exception e) {
            return false;
        }
    }

    @RequiredPermission(Permission.MANAGE_SECURITY)
    @SuppressWarnings("unchecked")
    public PageList<Subject> findAvailableSubjectsForRole(Subject whoami, Integer roleId, Integer[] pendingSubjectIds,
        PageControl pc) {
        pc.initDefaultOrderingField("s.name");

        String queryName;

        if ((pendingSubjectIds == null) || (pendingSubjectIds.length == 0)) {
            queryName = Subject.QUERY_FIND_AVAILABLE_SUBJECTS_FOR_ROLE;
        } else {
            queryName = Subject.QUERY_FIND_AVAILABLE_SUBJECTS_FOR_ROLE_WITH_EXCLUDES;
        }

        Query countQuery = PersistenceUtility.createCountQuery(entityManager, queryName, "distinct s");
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, queryName, pc);

        countQuery.setParameter("roleId", roleId);
        query.setParameter("roleId", roleId);

        if ((pendingSubjectIds != null) && (pendingSubjectIds.length > 0)) {
            List<Integer> pendingIdsList = Arrays.asList(pendingSubjectIds);
            countQuery.setParameter("excludes", pendingIdsList);
            query.setParameter("excludes", pendingIdsList);
        }

        long count = (Long) countQuery.getSingleResult();
        List<Subject> subjects = query.getResultList();

        // eagerly load in the roles - can't use left-join due to PersistenceUtility usage; perhaps use EAGER
        for (Subject subject : subjects) {
            subject.getRoles().size();
        }

        return new PageList<Subject>(subjects, (int) count, pc);
    }

    @SuppressWarnings("unchecked")
    public PageList<Subject> findSubjectsByCriteria(Subject subject, SubjectCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        ;

        CriteriaQueryRunner<Subject> queryRunner = new CriteriaQueryRunner(criteria, generator, entityManager);
        return queryRunner.execute();
    }

}