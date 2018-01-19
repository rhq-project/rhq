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
package org.rhq.enterprise.server.core.jaas;

import java.io.IOException;
import java.security.Principal;
import java.security.acl.Group;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.security.SimpleGroup;
import org.jboss.security.SimplePrincipal;
import org.jboss.security.auth.spi.UsernamePasswordLoginModule;

import org.rhq.core.domain.common.composite.SystemSetting;
import org.rhq.core.domain.common.composite.SystemSettings;
import org.rhq.core.util.StringUtil;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.resource.group.LdapGroupManagerLocal;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * A login module that just delegates all work to a different security domain.<p/>
 * When you use container managed security (CMS), EAP 6.1 requires the security domain being
 * already present in standalone.xml
 *
 * With our setup we (re)-create the security domain of RHQUserSecurityDomain dynamically,
 * which makes CMS fail on startup and also on re-create.
 *
 * The approach of just exchanging login modules does not work correctly either (principals
 * keep being cached, server goes into need-reload state).
 *
 * So we now have a security domain for the CMS for the REST api that just delegates to the
 * RHQUserSecuritDomain.
 *
 * <pre>
 * &lt;security-domain name="RHQRESTSecurityDomain" cache-type="default">
 *   &lt;authentication>
 *       &lt;login-module code="org.rhq.enterprise.server.core.jaas.DelegatingLoginModule" flag="required">
 *           &lt;module-option name="delegateTo" value="RHQUserSecurityDomain"/>
 *           &lt;module-option name="additionalRoles" value="rest-user"/>
 *       &lt;/login-module>
 *   &lt;/authentication>
 * &lt;/security-domain>
 *</pre>
 *
 * @author Heiko W. Rupp
 * @author Simeon Pinder
 */
@SuppressWarnings("unused")
public class DelegatingLoginModule extends UsernamePasswordLoginModule {

    private static Log LOG = LogFactory.getLog("DelegatingLoginModule");

    LoginContext loginContext;
    private String[] usernamePassword;
    private Principal identity;
    private List<String> rolesList;
    private boolean debugEnabled;
    private SubjectManagerLocal subjectMgrLocal;
    private LdapGroupManagerLocal ldapMgrLocal;
    private SystemManagerLocal sysMgrLocal;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
                           Map<String, ?> options) {

        debugEnabled = LOG.isDebugEnabled();

        super.initialize(subject, callbackHandler, sharedState, options);

        /* This is the login context (=security domain) we want to delegate to */
        String delegateTo = (String) options.get("delegateTo");

        /* Comma separated list of roles that should be set for the principal */
        String additionalRoles = (String) options.get("roles");
        rolesList = StringUtil.explode(additionalRoles, ",");

        if (delegateTo ==null || delegateTo.isEmpty()) {
            delegateTo = "other";
            LOG.warn("module-option 'delegateTo' was not set. Defaults to 'other'.");
        }

        if (debugEnabled) {
            LOG.debug("Delegating to " + delegateTo + " with roles " + additionalRoles);
        }

        // Now create the context for later use
        try {
            loginContext = new LoginContext(delegateTo, new DelegateCallbackHandler());
        } catch (LoginException e) {
            LOG.warn("Initialize failed : " + e.getMessage());
        }
    }

    /**
     * Do the actual login work - we obtain the user/password passed in and then try to
     * log into the delegated context. If this succeeds, we tell the super-module,
     * so this can do further processing (especially running the #commit() method).
     *
     * @return True on success
     * @throws LoginException If anything goes wrong
     */
    @Override
    public boolean login() throws LoginException {
        try {
            // Get the username / password the user entered and save if for later use
            usernamePassword = super.getUsernameAndPassword();

            // Try to log in via the delegate
            loginContext.login();

            String username = usernamePassword[0];

            subjectMgrLocal = LookupUtil.getSubjectManager();
            ldapMgrLocal = LookupUtil.getLdapGroupManager();
            sysMgrLocal = LookupUtil.getSystemManager();
            if ((subjectMgrLocal != null) && (ldapMgrLocal != null) && (sysMgrLocal != null)) {
                org.rhq.core.domain.auth.Subject rSubject = subjectMgrLocal.getSubjectByName(username);

                //insert ldap authz check
                boolean isLdapUser = !subjectMgrLocal.isUserWithPrincipal(username);
                if (isLdapUser) {
                    //we can proceed with LDAP checking
                    //BZ-580127: only do group authz check if one or both of group filter fields is set
                    if (ldapAuthDetected() & ldapAuthzDetected()) {
                        List<String> groupNames = new ArrayList<String>(ldapMgrLocal.findAvailableGroupsFor(username));
                        if (log.isDebugEnabled()) {
                            log.debug("Updating LDAP authorization data for user [" + username + "] with LDAP groups "
                                + groupNames + "...");
                        }
                        ldapMgrLocal.assignRolesToLdapSubject(rSubject.getId(), groupNames);
                        if (!sysMgrLocal.isLoginWithoutRolesEnabled() && rSubject.getRoles().isEmpty()) {
                            throw new LoginException("Subject [" + username
                                + "] is authenticated for LDAP, but there are no preconfigured roles for them.");
                        }
                    }
                }
            }

            // Nix out the password
            usernamePassword[1] = null;

            // login was success, so we can continue
            identity = createIdentity(usernamePassword[0]);
            useFirstPass=true;

            // This next flag is important. Without it the principal will not be
            // propagated
            loginOk = true;

            if (debugEnabled) {
                LOG.debug("Login ok for " + usernamePassword[0]);
            }

            return true;
        } catch (Exception e) {
            if (debugEnabled) {
                LOG.debug("Login failed for : " + usernamePassword[0] + ": " + e.getMessage());
            }
            loginOk = false;
            return false;
        }
    }


    @Override
    protected String getUsersPassword() throws LoginException {

        // This is not used but abstract in super.
        return null;
    }

    @Override
    protected Principal getIdentity() {
        return identity;
    }


    @Override
    protected Group[] getRoleSets() throws LoginException {

        SimpleGroup roles = new SimpleGroup("Roles");

        for (String role : rolesList ) {
            roles.addMember( new SimplePrincipal(role));
        }
        Group[] roleSets = { roles };
        return roleSets;
    }

    private boolean ldapAuthDetected() {
        SystemSettings systemSettings = sysMgrLocal.getUnmaskedSystemSettings(true);
        String value = systemSettings.get(SystemSetting.LDAP_BASED_JAAS_PROVIDER);
        return (value != null) ? Boolean.valueOf(value) : false;
    }

    private boolean ldapAuthzDetected() {
        SystemSettings systemSettings = sysMgrLocal.getUnmaskedSystemSettings(true);
        String groupFilter = systemSettings.get(SystemSetting.LDAP_GROUP_FILTER);
        String groupMember = systemSettings.get(SystemSetting.LDAP_GROUP_MEMBER);
        return ((groupFilter != null) && (groupFilter.trim().length() > 0))
            || ((groupMember != null) && (groupMember.trim().length() > 0));
    }


    /**
     * Handle the callbacks from the other security domain that we delegate to
     */
    private class DelegateCallbackHandler implements CallbackHandler {
        @Override
        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {

            if (debugEnabled) {
                LOG.debug("private handle callbacks");
            }
            for (Callback cb : callbacks) {
                if (cb instanceof NameCallback) {
                    NameCallback nc = (NameCallback) cb;
                    nc.setName(usernamePassword[0]);
                }
                else if (cb instanceof PasswordCallback) {
                    PasswordCallback pc = (PasswordCallback) cb;
                    pc.setPassword(usernamePassword[1].toCharArray());
                }
                else {
                    throw new UnsupportedCallbackException(cb,"Callback " + cb + " not supported");
                }
            }
        }
    }
}
