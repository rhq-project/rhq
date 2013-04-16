/*
 * RHQ Management Platform
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.modules.plugins.jbossas7;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

/**
 * Authenticator to authenticate against as7.
 *
 * @deprecated as of 4.7. It is no longer used in {@link ASConnection} nor in {@link ASUploadConnection}.
 *
 * @author Heiko W. Rupp
 */
@Deprecated
public class AS7Authenticator extends Authenticator {

    private String user;
    private String pass;

    public AS7Authenticator(String user, String pass) {
        this.user = user;
        this.pass = pass;
        if (this.pass == null)
            this.pass = ""; // prevent NPE later
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(user, pass.toCharArray());
    }
}
