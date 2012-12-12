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

package org.rhq.enterprise.server.naming.context;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.naming.NotContextException;
import javax.naming.ldap.Control;
import javax.naming.ldap.ExtendedRequest;
import javax.naming.ldap.ExtendedResponse;
import javax.naming.ldap.LdapContext;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class URLPreferringLdapContextDecorator extends URLPreferringDirContextDecorator implements LdapContext {

    private static final long serialVersionUID = 1L;

    public URLPreferringLdapContextDecorator() {
        super(null);
    }
    
    public URLPreferringLdapContextDecorator(LdapContext original) {
        super(original);
    }
    
    @Override
    protected LdapContext checkAndCast(Context ctx) throws NamingException {
        if (!(ctx instanceof LdapContext)) {
            if (ctx == null) {
                throw new NoInitialContextException();
            } else {
                throw new NotContextException("Not an instance of LdapContext");
            }
        }

        return (LdapContext) ctx;
    }

    @Override
    protected LdapContext getOriginal() throws NamingException {
        return checkAndCast(super.getOriginal());
    }

    public ExtendedResponse extendedOperation(ExtendedRequest request) throws NamingException {
        return getOriginal().extendedOperation(request);
    }

    public LdapContext newInstance(Control[] requestControls) throws NamingException {
        return new URLPreferringLdapContextDecorator(getOriginal().newInstance(requestControls));
    }

    public void reconnect(Control[] connCtls) throws NamingException {
        getOriginal().reconnect(connCtls);
    }

    public Control[] getConnectControls() throws NamingException {
        return getOriginal().getConnectControls();
    }

    public void setRequestControls(Control[] requestControls) throws NamingException {
        getOriginal().setRequestControls(requestControls);
    }

    public Control[] getRequestControls() throws NamingException {
        return getOriginal().getRequestControls();
    }

    public Control[] getResponseControls() throws NamingException {
        return getOriginal().getResponseControls();
    }

}
