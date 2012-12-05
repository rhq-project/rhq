/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.jndi.context;

import javax.naming.NamingException;
import javax.naming.ldap.Control;
import javax.naming.ldap.ExtendedRequest;
import javax.naming.ldap.ExtendedResponse;
import javax.naming.ldap.LdapContext;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class AccessCheckingLdapContextDecorator extends AccessCheckingDirContextDecorator implements LdapContext {

    private static final long serialVersionUID = 1L;

    public AccessCheckingLdapContextDecorator(String... checkedSchemes) {
        super(checkedSchemes);
    }

    public AccessCheckingLdapContextDecorator(LdapContext original, String... checkedSchemes) {
        super(original, checkedSchemes);
    }

    @Override
    protected LdapContext getOriginal() {
        return (LdapContext) super.getOriginal();
    }

    public ExtendedResponse extendedOperation(ExtendedRequest request) throws NamingException {
        check();
        return getOriginal().extendedOperation(request);
    }

    public LdapContext newInstance(Control[] requestControls) throws NamingException {
        check();
        return getOriginal().newInstance(requestControls);
    }

    public void reconnect(Control[] connCtls) throws NamingException {
        check();
        getOriginal().reconnect(connCtls);
    }

    public Control[] getConnectControls() throws NamingException {
        check();
        return getOriginal().getConnectControls();
    }

    public void setRequestControls(Control[] requestControls) throws NamingException {
        check();
        getOriginal().setRequestControls(requestControls);
    }

    public Control[] getRequestControls() throws NamingException {
        check();
        return getOriginal().getRequestControls();
    }

    public Control[] getResponseControls() throws NamingException {
        check();
        return getOriginal().getResponseControls();
    }

}
