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
package org.rhq.gui.webdav;

import javax.security.auth.login.LoginException;

import com.bradmcevoy.http.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Provides a utility object to perform user authentication.
 * 
 * @author John Mazzitelli
 */
public class Authenticator {
    private static final Log LOG = LogFactory.getLog(Authenticator.class);

    private Subject subject = null;

    public Authenticator(Subject subject) {
        this.subject = subject;
    }

    /**
     * If the user has successfully authenticated himself, this will return the Subject for that login.
     * If the user never logged in or failed to authenticate successfully, this will be <code>null</code>.
     * 
     * @return the authenticated subject, or <code>null</code> if user is not authenticated
     */
    public Subject getSubject() {
        return this.subject;
    }

    /**
     * For those times when a user does not need to be authenticated to obtain data, use
     * the overlord user, which provides full superuser authentication.
     * 
     * @return the overlord user
     */
    public Subject getOverlord() {
        return LookupUtil.getSubjectManager().getOverlord();
    }

    /**
     * Logs the user in with the given credentials.
     * 
     * <i>This signature must match that of {@link Resource#authenticate(String, String)}</i>
     *
     * @param username
     * @param password
     * 
     * @return the {@link Subject} of the authenticated user
     */
    public Object authenticate(String username, String password) {
        try {
            this.subject = LookupUtil.getSubjectManager().login(username, password);
            LOG.debug("User [" + username + "] logged into the webdav server");
        } catch (LoginException e) {
            LOG.warn("User [" + username + "] failed to log into the webdav server");
            this.subject = null;
        }
        return this.subject;
    }

    /**
     * Returns the security realm that the user must authenticate with.
     * 
     * <i>This signature must match that of {@link Resource#getRealm()}</i>
     * 
     * @return security realm name
     */
    public String getRealm() {
        return "Jopr Management Environment";
    }
}
