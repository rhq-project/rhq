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
package org.rhq.enterprise.agent;

import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.server.CommandAuthenticator;

/**
 * Authenticator our tests can use that simply check for the existence of a constant string in each command's
 * configuration placed there by the {@link SimpleCommandPreprocessor}.
 *
 * @author John Mazzitelli
 */
public class SimpleCommandAuthenticator implements CommandAuthenticator {
    /**
     * The property this authenticator will look up when authenticating.
     */
    public static final String AUTHENTICATION_PROP = "rhq.test.authentication";

    /**
     * The authentication property value that will be expected in order to authenticate commands.
     */
    public static final String AUTHENTICATION_PROP_VALUE = "1234567890";

    /**
     * @see CommandAuthenticator#isAuthenticated(Command)
     */
    public boolean isAuthenticated(Command command) {
        String prop_value = command.getConfiguration().getProperty(AUTHENTICATION_PROP);
        return AUTHENTICATION_PROP_VALUE.equals(prop_value);
    }
}