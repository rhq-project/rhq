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
package org.rhq.enterprise.server.core.jaas;

import java.security.acl.Group;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.security.SimpleGroup;
import org.jboss.security.auth.spi.UsernamePasswordLoginModule;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * A "special" JAAS login module that checks to see if the password is a valid temporary session password. If it is, the
 * user is authenticated; if not, the user is denied access.
 *
 * <p>This is used mainly to support the use-case where a server component needs to submit a request that needs to again
 * be authenticated. In this case, the server doesn't know the user's true password; instead a temporary session
 * password was created for the user.
 *
 * @author John Mazzitelli
 */
public class TempSessionLoginModule extends UsernamePasswordLoginModule {
    /**
     * Logger
     */
    private static final Log LOG = LogFactory.getLog(TempSessionLoginModule.class);

    /**
     * A place where this login module can initialize itself.
     *
     * @param theSubject
     * @param theHandler
     * @param theSharedState
     * @param theOptions
     */
    public void initialize(Subject theSubject, CallbackHandler theHandler, Map theSharedState, Map theOptions) {
        super.initialize(theSubject, theHandler, theSharedState, theOptions);
    }

    /**
     * We don't know the user's true password, so we don't know the expected password; therefore, this returns an empty
     * string. This class overrides {@link #validatePassword(String, String)} so it can validate on the input password
     * that was entered by the client, since it is all we need to check validity.
     *
     * @return empty string
     */
    protected String getUsersPassword() {
        return "";
    }

    /**
     * Authenticates the user by seeing if the <code>inputPassword</code> is a valid temporary session password. <code>
     * expectedPassword</code> is ignored.
     *
     * @see UsernamePasswordLoginModule#validatePassword(String, String)
     */
    protected boolean validatePassword(String inputPassword, String expectedPassword) {
        boolean validPassword = false;

        // call into the JON authentication subsystem to see if the input password was valid.
        try {
            validPassword = LookupUtil.getSubjectManager().authenticateTemporarySessionPassword(inputPassword);
        } catch (Exception e) {
            LOG.error("Failed to authenticate a session password for user [" + getUsername() + "]", e);
        }

        return validPassword;
    }

    /**
     * @see org.jboss.security.auth.spi.AbstractServerLoginModule#getRoleSets()
     */
    protected Group[] getRoleSets() throws LoginException {
        SimpleGroup roles = new SimpleGroup("Roles");

        //roles.addMember( new SimplePrincipal( "some role" ) );

        return new Group[] { roles };
    }
}