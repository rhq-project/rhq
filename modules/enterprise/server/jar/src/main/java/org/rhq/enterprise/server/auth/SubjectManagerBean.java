/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.enterprise.server.auth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.interceptor.ExcludeDefaultInterceptors;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.security.auth.login.LoginContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.crypto.CryptoUtil;
import org.jboss.security.auth.callback.UsernamePasswordHandler;

import org.rhq.core.domain.auth.Principal;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.common.composite.SystemSetting;
import org.rhq.core.domain.common.composite.SystemSettings;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.RoleCriteria;
import org.rhq.core.domain.criteria.SavedSearchCriteria;
import org.rhq.core.domain.criteria.SubjectCriteria;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.search.SavedSearch;
import org.rhq.core.domain.server.PersistenceUtility;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.alert.AlertNotificationManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.authz.RoleManagerLocal;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.core.CustomJaasDeploymentServiceMBean;
import org.rhq.enterprise.server.exception.LoginException;
import org.rhq.enterprise.server.resource.group.LdapGroupManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.search.SavedSearchManagerLocal;
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
    //@IgnoreDependency
    private ResourceGroupManagerLocal resourceGroupManager;

    @EJB
    //@IgnoreDependency
    private LdapGroupManagerLocal ldapManager;

    @EJB
    private SystemManagerLocal systemManager;

    @EJB
    //@IgnoreDependency
    private AlertNotificationManagerLocal alertNotificationManager;

    @EJB
    //@IgnoreDependency
    private RoleManagerLocal roleManager;

    @EJB
    //@IgnoreDependency
    private RepoManagerLocal repoManager;

    @EJB
    private SavedSearchManagerLocal savedSearchManager;

    @Resource
    private TimerService timerService;

    private SessionManager sessionManager = SessionManager.getInstance();

    public void scheduleSessionPurgeJob() {
        // each time the webapp is reloaded, we don't want to create duplicate jobs
        Collection<Timer> timers = timerService.getTimers();
        for (Timer existingTimer : timers) {
            if (log.isDebugEnabled()) {
                log.debug("Found timer - attempting to cancel: " + existingTimer.toString());
            }
            try {
                existingTimer.cancel();
            } catch (Exception e) {
                log.warn("Failed in attempting to cancel timer: " + existingTimer.toString());
            }
        }

        // timer that will trigger every 60 seconds
        timerService.createIntervalTimer(60000L, 60000L, new TimerConfig(null, false));
    }

    @Timeout
    // we don't need transactioning here
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void purgeTimedOutSessions() {
        try {
            sessionManager.purgeTimedOutSessions();
        } catch (Throwable t) {
            log.error("Failed to purge timed out sessions - will try again later. Cause: " + t);
        }
    }

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
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#updateSubject(Subject, Subject)
     */
    public Subject updateSubject(Subject whoami, Subject subjectToModify) {
        // let a user change his own details
        Set<Permission> globalPermissions = authorizationManager.getExplicitGlobalPermissions(whoami);
        if (!whoami.equals(subjectToModify) && !globalPermissions.contains(Permission.MANAGE_SECURITY)) {
            throw new PermissionException("You [" + whoami.getName() + "] do not have permission to update user ["
                + subjectToModify.getName() + "].");
        }
        if (authorizationManager.isSystemSuperuser(subjectToModify) && !subjectToModify.getFactive()) {
            throw new PermissionException("You cannot disable system user [" + subjectToModify.getName()
                + "] - it must always be active.");
        }

        // Reset the roles, LDAP roles, and owned groups according to the current settings as this method will not
        // update them. To update assigned roles, use the 3-param createSubject() or use RoleManagerLocal.
        Subject currentSubject = entityManager.find(Subject.class, subjectToModify.getId());
        subjectToModify.setRoles(currentSubject.getRoles());
        subjectToModify.setLdapRoles(currentSubject.getLdapRoles());
        subjectToModify.setOwnedGroups(currentSubject.getOwnedGroups());

        return entityManager.merge(subjectToModify);
    }

    public Subject createSubject(Subject whoami, Subject subjectToCreate, String password) throws SubjectException,
        EntityExistsException {
        if (getSubjectByName(subjectToCreate.getName()) != null) {
            throw new EntityExistsException("A user named [" + subjectToCreate.getName() + "] already exists.");
        }

        if (subjectToCreate.getFsystem()) {
            throw new SubjectException("Cannot create new system users: " + subjectToCreate.getName());
        }

        entityManager.persist(subjectToCreate);

        createPrincipal(whoami, subjectToCreate.getName(), password);

        return subjectToCreate;
    }

    public Subject updateSubject(Subject whoami, Subject subjectToModify, String newPassword) {
        // let a user change his own details
        Set<Permission> globalPermissions = authorizationManager.getExplicitGlobalPermissions(whoami);
        boolean isSecurityManager = globalPermissions.contains(Permission.MANAGE_SECURITY);
        if (!whoami.equals(subjectToModify) && !isSecurityManager) {
            throw new PermissionException("You [" + whoami.getName() + "] do not have permission to update user ["
                + subjectToModify.getName() + "].");
        }

        boolean subjectToModifyIsSystemSuperuser = authorizationManager.isSystemSuperuser(subjectToModify);
        if (!subjectToModify.getFactive() && subjectToModifyIsSystemSuperuser) {
            throw new PermissionException("You cannot disable the system user [" + subjectToModify.getName() + "].");
        }

        Subject attachedSubject = getSubjectById(subjectToModify.getId());
        if (attachedSubject == null) {
            throw new IllegalArgumentException("No user exists with id [" + subjectToModify.getId() + "].");
        }
        if (!attachedSubject.getName().equals(subjectToModify.getName())) {
            throw new IllegalArgumentException("You cannot change a user's username.");
        }

        Set<Role> newRoles = subjectToModify.getRoles();
        if (newRoles != null) {
            Set<Role> currentRoles = new HashSet<Role>(roleManager.findRolesBySubject(subjectToModify.getId(),
                PageControl.getUnlimitedInstance()));
            boolean rolesChanged = !(newRoles.containsAll(currentRoles) && currentRoles.containsAll(newRoles));
            if (rolesChanged) {
                int[] newRoleIds = new int[newRoles.size()];
                int i = 0;
                for (Role role : newRoles) {
                    newRoleIds[i++] = role.getId();
                }
                roleManager.setAssignedSubjectRoles(whoami, subjectToModify.getId(), newRoleIds);
            }
        }

        boolean ldapRolesModified = false;
        Set<Role> newLdapRoles = subjectToModify.getLdapRoles();
        if (newLdapRoles == null) {
            newLdapRoles = Collections.emptySet();
        }
        if (newLdapRoles != null) {
            RoleCriteria subjectLdapRolesCriteria = new RoleCriteria();
            subjectLdapRolesCriteria.addFilterLdapSubjectId(subjectToModify.getId());
            subjectLdapRolesCriteria.clearPaging();//disable paging as the code assumes all the results will be returned.

            PageList<Role> currentLdapRoles = roleManager.findRolesByCriteria(whoami, subjectLdapRolesCriteria);

            ldapRolesModified = !(currentLdapRoles.containsAll(newLdapRoles) && newLdapRoles
                .containsAll(currentLdapRoles));
        }

        boolean isUserWithPrincipal = isUserWithPrincipal(subjectToModify.getName());
        if (ldapRolesModified) {
            if (!isSecurityManager) {
                throw new PermissionException("You cannot change the LDAP roles assigned to ["
                    + subjectToModify.getName() + "] - only a user with the MANAGE_SECURITY permission can do so.");
            } else if (isUserWithPrincipal) {
                throw new PermissionException("You cannot set LDAP roles on non-LDAP user ["
                    + subjectToModify.getName() + "].");
            }

            // TODO: Update LDAP roles.
        }

        if (newPassword != null) {
            if (!isUserWithPrincipal(subjectToModify.getName())) {
                throw new IllegalArgumentException("You cannot set a password for an LDAP user.");
            }

            changePasswordInternal(subjectToModify.getName(), newPassword);
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
     * @see org.rhq.enterprise.server.auth.SubjectManagerRemote#getSubjectByName(String)
     */
    public Subject getSubjectByName(String username) {
        //TODO: this method needs to be modified to require a Subject and probably MANAGE_SECURITY
        //      permissions to defend against unrestricted access to subjects.

        SubjectCriteria c = new SubjectCriteria();
        c.addFilterName(username);
        //to return the right user and to be deterministic the criteria should be strict.
        c.setStrict(true);

        PageList<Subject> result = findSubjectsByCriteria(getOverlord(), c);

        return result.isEmpty() ? null : result.get(0);
    }

    /**
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#createSubject(Subject, Subject)
     */
    @RequiredPermission(Permission.MANAGE_SECURITY)
    public Subject createSubject(Subject whoami, Subject subject) throws SubjectException {
        // Make sure there's not an existing subject with the same name.
        if (getSubjectByName(subject.getName()) != null) {
            throw new EntityExistsException("A user named [" + subject.getName() + "] already exists.");
        }

        if (subject.getFsystem()) {
            throw new SubjectException("Cannot create new system subjects: " + subject.getName());
        }

        // we are ignoring roles - anything the caller gave us is thrown out
        subject.setRoles(null);
        subject.setLdapRoles(null);
        subject.setOwnedGroups(null);
        Configuration configuration = subject.getUserConfiguration();
        if (configuration != null) {
            configuration = entityManager.merge(configuration);
            subject.setUserConfiguration(configuration);
        }

        entityManager.persist(subject);

        return subject;
    }

    /**
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#getSubjectById(int)
     */
    public Subject getSubjectById(int id) {
        Subject subject = entityManager.find(Subject.class, id);
        return subject;
    }

    public Subject login(String username, String password) throws LoginException {
        return _login(username, password, true, true);
    }

    public Subject loginLocal(String username, String password) throws LoginException {
        return _login(username, password, true, false);
    }

    private Subject _login(String username, String password, boolean checkRoles, boolean remote) throws LoginException {
        if (password == null) {
            throw new LoginException("No password was given");
        }

        // Use the JAAS modules to perform the auth.
        _checkAuthentication(username, password);

        // User is authenticated!

        Subject subject = getSubjectByName(username);

        if (subject != null) {//authenticated user
            if (!subject.getFactive()) {
                throw new LoginException("User account has been disabled.");
            }

            if (checkRoles) {

                //insert ldap authz check
                boolean isLdapUser = !isUserWithPrincipal(username);
                if (isLdapUser) {
                    //we can proceed with LDAP checking
                    //BZ-580127: only do group authz check if one or both of group filter fields is set
                    if (isLdapAuthenticationEnabled() & isLdapAuthorizationEnabled()) {
                        List<String> groupNames = new ArrayList<String>(
                            ldapManager.findAvailableGroupsFor(subject.getName()));
                        if (log.isDebugEnabled()) {
                            log.debug("Updating LDAP authorization data for user [" + subject.getName()
                                + "] with LDAP groups " + groupNames + "...");
                        }
                        ldapManager.assignRolesToLdapSubject(subject.getId(), groupNames);
                        if (!systemManager.isLoginWithoutRolesEnabled() && subject.getRoles().isEmpty()) {
                            throw new LoginException("Subject [" + subject.getName()
                                + "] is authenticated for LDAP, but there are no preconfigured roles for them.");
                        }
                    }
                }

                // fetch the roles
                int rolesNumber = subject.getRoles().size();
                if (rolesNumber == 0) {
                    if (systemManager.isLoginWithoutRolesEnabled()) {
                        if (log.isInfoEnabled()) {
                            log.info("Letting in user [" + subject.getName() + "]  without any assigned roles.");
                        }
                    } else {
                        throw new LoginException("There are no preconfigured roles for user [" + subject.getName()
                            + "]");
                    }
                }
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

            boolean isLdapAuthenticationEnabled = isLdapAuthenticationEnabled();
            if (isLdapAuthenticationEnabled) {
                if (remote) {
                    throw new IllegalStateException(
                        "Use the web UI for the first log in and fill all the necessary information.");
                }
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

        // make sure to return the session-activated subject
        subject = sessionManager.put(subject);
        return subject;
    }

    public Subject checkAuthentication(String username, String password) {
        try {
            _checkAuthentication(username, password);
            return getSubjectByName(username);
        } catch (LoginException e) {
            return null;
        }
    }

    private void _checkAuthentication(String username, String password) throws LoginException {
        try {
            UsernamePasswordHandler handler = new UsernamePasswordHandler(username, password.toCharArray());
            LoginContext loginContext;
            loginContext = new LoginContext(CustomJaasDeploymentServiceMBean.RHQ_USER_SECURITY_DOMAIN, handler);

            loginContext.login();
            loginContext.getSubject().getPrincipals().iterator().next();
            loginContext.logout();
        } catch (javax.security.auth.login.LoginException e) {
            throw new LoginException(e.getMessage());
        }
    }

    /**This method is applied to Subject instances that may require LDAP auth/authz processing.
     * Called from both SLSB and SubjectGWTServiceImpl and:
     * -if Subject passed in has Principal(not LDAP account) then we immediately return Subject as no processing needed.
     * -if Subject for LDAP account
     *
     * @param subject Authenticated subject.
     * @return same or new Subject returned from LDAP processing.
     * @throws LoginException
     */
    public Subject processSubjectForLdap(Subject subject, String subjectPassword) throws LoginException {
        if (subject != null) {//null check

            //if user has principal then bail as LDAP processing not required
            boolean userHasPrincipal = isUserWithPrincipal(subject.getName());
            if (log.isDebugEnabled()) {
                log.debug("Processing subject '" + subject.getName() + "' for LDAP check, userHasPrincipal:"
                    + userHasPrincipal);
            }

            //if user has principal then return as non-ldap user
            if (userHasPrincipal) {
                return subject; //bail. No further checking required.
            } else {//Start LDAP check.

                boolean isLdapAuthenticationEnabled = isLdapAuthenticationEnabled();
                if (isLdapAuthenticationEnabled) {//we can proceed with LDAP checking
                    //check that session is valid. RHQ auth has already occurred. Security check required to initiate following
                    //spinder BZ:682755: 3/10/11: can't use isValidSessionId() as it also compares subject.id which is changing during case insensitive
                    // and new registration. This worked before because HTTP get took longer to invalidate sessions.
                    Subject sessionSubject;
                    try {
                        sessionSubject = sessionManager.getSubject(subject.getSessionId());
                    } catch (SessionNotFoundException e) {
                        throw new LoginException("User session not valid. Login to proceed.");
                    } catch (SessionTimeoutException e) {
                        throw new LoginException("User session not valid. Login to proceed.");
                    }
                    if (!subject.getName().equals(sessionSubject.getName())) {
                        throw new LoginException("User session not valid. Login to proceed.");
                    }

                    //Subject.id == 0 then is registration or case insensitive check and subject update.
                    if (subject.getId() == 0) {
                        //i)case insensitive check or ii)ldap new user registration.
                        //BZ-586435: insert case insensitivity for usernames with ldap auth
                        // locate first matching subject and attach.
                        SubjectCriteria subjectCriteria = new SubjectCriteria();
                        subjectCriteria.setCaseSensitive(false);
                        subjectCriteria.setStrict(true);
                        subjectCriteria.fetchRoles(false);
                        subjectCriteria.fetchConfiguration(false);
                        subjectCriteria.addFilterName(subject.getName());
                        //BZ-798465: spinder 3/1/12 we now need to pass in overlord because of BZ-786159
                        // We've verified that this user has valid session, and is using ldap. Safe to elevate search here.
                        PageList<Subject> subjectsLocated = findSubjectsByCriteria(getOverlord(), subjectCriteria);
                        //if subject variants located then take the first one with a principal otherwise do nothing
                        //To defend against the case where they create an account with the same name but not
                        //case as an rhq sysadmin or higher perms, then make them relogin with same creds entered.
                        if ((!subjectsLocated.isEmpty())
                            && (!subjectsLocated.get(0).getName().equals(subject.getName()))) {//then case insensitive username matches found. Try to use instead.
                            Subject ldapSubject = subjectsLocated.get(0);
                            String msg = "Located existing ldap account with different case for ["
                                + ldapSubject.getName() + "]. "
                                + "Attempting to authenticate with that account instead.";
                            if (log.isInfoEnabled()) {
                                log.info(msg);
                            }
                            logout(subject.getSessionId().intValue());
                            subject = _login(ldapSubject.getName(), subjectPassword, false, false);
                            Integer sessionId = subject.getSessionId();
                            if (log.isDebugEnabled()) {
                                log.debug("Logged in as [" + ldapSubject.getName() + "] with session id [" + sessionId
                                    + "]");
                            }
                        } else {//then this is a registration request. insert overlord registration and login
                            //we've verified that this user has valid session, requires registration and that ldap is configured.
                            Subject superuser = getOverlord();

                            // create the subject, but don't add a principal since LDAP will handle authentication
                            if (log.isDebugEnabled()) {
                                log.debug("registering new LDAP-authenticated subject [" + subject.getName() + "]");
                            }
                            createSubject(superuser, subject);
                            subject.setFactive(true);

                            // nuke the temporary session and establish a new
                            // one for this subject.. must be done before pulling the
                            // new subject in order to do it with his own credentials
                            logout(subject.getSessionId().intValue());
                            subject = _login(subject.getName(), subjectPassword, false, false);

                            prepopulateLdapFields(subject);

                            //insert empty configuration to start
                            Configuration newUserConfig = new Configuration();
                            //set flag on user so that we know registration is still required.
                            PropertySimple simple = new PropertySimple("isNewUser", true);
                            newUserConfig.put(simple);
                            subject.setUserConfiguration(newUserConfig);
                        }
                    }

                    //Subject.id guaranteed to be > 0 then iii)authorization updates for ldap groups necessary
                    //BZ-580127: only do group authz check if one or both of group filter fields is set
                    if (isLdapAuthorizationEnabled()) {
                        List<String> groupNames = new ArrayList<String>(ldapManager.findAvailableGroupsFor(subject
                            .getName()));
                        if (groupNames.isEmpty()) {
                            if (systemManager.isLoginWithoutRolesEnabled()) {
                                if (log.isInfoEnabled()) {
                                    log.info("Letting in user [" + subject.getName() + "]  without any assigned roles.");
                                }
                            } else {
                                // there are no LDAP groups so don't even bother with assignRolesToLdapSubject and fail fast.
                                throw new LoginException("Subject [" + subject.getName()
                                    + "] is authenticated for LDAP, but there are no preconfigured roles for them.");
                            }
                        } else {
                            if (log.isDebugEnabled()) {
                                log.debug("Updating LDAP authorization data for user [" + subject.getName()
                                    + "] with LDAP groups " + groupNames + "...");
                            }
                            ldapManager.assignRolesToLdapSubject(subject.getId(), groupNames);
                            if (!systemManager.isLoginWithoutRolesEnabled() && subject.getRoles().isEmpty()) {
                                throw new LoginException("Subject [" + subject.getName()
                                    + "] is authenticated for LDAP, but there are no preconfigured roles for them.");
                            }
                        }
                    }
                } else {//ldap not configured. Somehow authenticated for LDAP without ldap being configured. Error. Bail
                    throw new LoginException("Subject[" + subject.getName()
                        + "] is authenticated for LDAP, but LDAP is not configured.");
                }
            }
        }
        return subject;
    }

    /**
     * @see org.rhq.enterprise.server.auth.SubjectManagerRemote#logout(Subject)
     */
    public void logout(Subject subject) {
        try {
            // make sure the Subject is valid by pairing the name and sessionId
            Subject s = getSubjectByNameAndSessionId(subject.getName(), subject.getSessionId());
            sessionManager.invalidate(s.getSessionId());
        } catch (Exception e) {
            // ignore invalid logout request
        }
    }

    /**
      * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#logout(int)
      */
    public void logout(int sessionId) {
        sessionManager.invalidate(sessionId);
    }

    /**
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#createPrincipal(Subject, String, String)
     */
    @RequiredPermission(Permission.MANAGE_SECURITY)
    public void createPrincipal(Subject whoami, String username, String password) throws SubjectException {
        Principal principal = new Principal(username, CryptoUtil.createPasswordHash("MD5", "base64", null, null,
            password));
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
            throw new SubjectException("Failed to create " + principal + ".", e);
        }
    }

    /**
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#changePassword(Subject, String, String)
     */
    public void changePassword(Subject whoami, String username, String password) {
        // a user can change his/her own password, as can a user with the appropriate permission
        if (!whoami.getName().equals(username)
            && !authorizationManager.hasGlobalPermission(whoami, Permission.MANAGE_SECURITY)) {
            throw new PermissionException("You do not have permission to change the password for user [" + username
                + "]");
        }

        changePasswordInternal(username, password);

        return;
    }

    private void changePasswordInternal(String username, String password) {
        Query query = entityManager.createNamedQuery(Principal.QUERY_FIND_BY_USERNAME);
        query.setParameter("principal", username);
        Principal principal = (Principal) query.getSingleResult();
        String passwordHash = CryptoUtil.createPasswordHash("MD5", CryptoUtil.BASE64_ENCODING, null, null, password);
        principal.setPassword(passwordHash);
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
     * @see org.rhq.enterprise.server.auth.SubjectManagerLocal#loginUnauthenticated(String)
     */
    public Subject loginUnauthenticated(String username) throws LoginException {

        // we currently use a shared session for overlord.  ensure we use the shared session if the overlord user is
        // requested.
        if ("admin".equals(username)) {
            return getOverlord();
        }

        Subject subject = getSubjectByName(username);

        if (subject == null) {
            throw new LoginException("User account does not exist. [" + username + "]");
        }

        if (!subject.getFactive()) {
            throw new LoginException("User account has been disabled. [" + username + "]");
        }

        // make sure we return the Subject returned from this call, which may differ from the one passed in
        subject = sessionManager.put(subject, 1000L * 60 * 2); // 2mins only
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

            if (authorizationManager.isSystemSuperuser(doomedSubject)) {
                throw new PermissionException("You cannot delete a system root user - they must always exist");
            }

            Set<Role> roles = doomedSubject.getRoles();
            doomedSubject.setRoles(new HashSet<Role>()); // clean out roles

            for (Role doomedRoleRelationship : roles) {
                doomedRoleRelationship.removeSubject(doomedSubject);
            }

            // TODO: we need to reassign ownership of things this user used to own

            // if this user was authenticated via JDBC and thus has a principal, remove it
            if (isUserWithPrincipal(doomedSubject.getName())) {
                deletePrincipal(doomedSubject);
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
                        log.error("Error deleting owned group " + Arrays.toString(ownedGroupIds), t);
                    } else {
                        log.error("Error deleting owned group " + Arrays.toString(ownedGroupIds) + ": "
                            + t.getMessage());
                    }
                }
            }

            // Delete searches saved by this user
            SavedSearchCriteria savedSearchCriteria = new SavedSearchCriteria();
            savedSearchCriteria.addFilterSubjectId(doomedSubjectId);
            savedSearchCriteria.clearPaging();
            PageList<SavedSearch> savedSearches = savedSearchManager.findSavedSearchesByCriteria(subject,
                savedSearchCriteria);
            for (SavedSearch savedSearch : savedSearches) {
                savedSearchManager.deleteSavedSearch(subject, savedSearch.getId());
            }

            alertNotificationManager.cleanseAlertNotificationBySubject(doomedSubject.getId());

            repoManager.removeOwnershipOfSubject(doomedSubject.getId());

            entityManager.remove(doomedSubject);
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
     * Delete a user's principal from the internal database.
     *
     *
     * @param  subject The user whose principal is to be deleted
     *
     * @throws PermissionException if the caller tried to delete a system superuser
     */
    private void deletePrincipal(Subject subject) throws PermissionException {
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

    public PageList<Subject> findSubjectsByCriteria(Subject subject, SubjectCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        CriteriaQueryRunner<Subject> queryRunner = new CriteriaQueryRunner<Subject>(criteria, generator, entityManager);
        PageList<Subject> subjects = queryRunner.execute();
        boolean canViewUsers = (authorizationManager.isSystemSuperuser(subject)
            || authorizationManager.hasGlobalPermission(subject, Permission.MANAGE_SECURITY) || authorizationManager
            .hasGlobalPermission(subject, Permission.VIEW_USERS));
        if (!canViewUsers) {
            if (subjects.contains(subject)) {
                Subject attachedSubject = subjects.get(subjects.indexOf(subject));
                subjects.clear();
                subjects.add(attachedSubject);
            } else {
                subjects.clear();
            }
            subjects.setTotalSize(subjects.size());
        }
        return subjects;
    }

    private boolean isLdapAuthenticationEnabled() {
        SystemSettings systemSettings = systemManager.getUnmaskedSystemSettings(true);
        String value = systemSettings.get(SystemSetting.LDAP_BASED_JAAS_PROVIDER);
        return (value != null) ? Boolean.valueOf(value) : false;
    }

    private boolean isLdapAuthorizationEnabled() {
        SystemSettings systemSettings = systemManager.getUnmaskedSystemSettings(true);
        String groupFilter = systemSettings.get(SystemSetting.LDAP_GROUP_FILTER);
        String groupMember = systemSettings.get(SystemSetting.LDAP_GROUP_MEMBER);
        return ((groupFilter != null) && (groupFilter.trim().length() > 0))
            || ((groupMember != null) && (groupMember.trim().length() > 0));
    }

    private void prepopulateLdapFields(Subject subject) {
        // Note: A schema defining the standard LDAP attributes can be found here:
        //       http://www.openldap.org/devel/gitweb.cgi?p=openldap.git;a=blob;f=servers/slapd/schema/core.ldif
        Map<String, String> ldapUserAttributes = ldapManager.findLdapUserDetails(subject.getName());

        String givenName = (ldapUserAttributes.get("givenName") != null) ? ldapUserAttributes.get("givenName")
            : ldapUserAttributes.get("gn");
        subject.setFirstName(givenName);

        String surname = (ldapUserAttributes.get("sn") != null) ? ldapUserAttributes.get("sn") : ldapUserAttributes
            .get("surname");
        subject.setLastName(surname);

        String telephoneNumber = ldapUserAttributes.get("telephoneNumber");
        subject.setPhoneNumber(telephoneNumber);

        String mail = (ldapUserAttributes.get("mail") != null) ? ldapUserAttributes.get("mail") : ldapUserAttributes
            .get("rfc822Mailbox");
        subject.setEmailAddress(mail);

        String organizationalUnit = (ldapUserAttributes.get("ou") != null) ? ldapUserAttributes.get("ou")
            : ldapUserAttributes.get("organizationalUnitName");
        subject.setDepartment(organizationalUnit);

        return;
    }

}
