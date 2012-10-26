/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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

package org.rhq.enterprise.server.test.ldap;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.ObjectFactory;

import org.apache.commons.lang.NotImplementedException;

/**
 * An implementation of {@link InitialContextFactory} that can be used for 
 * testing LDAP context operations. This implementation simply returns an 
 * instance of {@link FakeLdapContext}.
 * 
 * @author loleary
 *
 */
public class FakeLdapCtxFactory implements ObjectFactory, InitialContextFactory {

    /**
     * Returns a fake {@link LdapContext} object useful for unit testing
     * 
     * @return A fake <code>Context</code> 
     */
    @Override
    public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
        return new FakeLdapContext();
    }

    /**
     * Not implemented
     */
    @Override
    public Object getObjectInstance(Object arg0, Name arg1, Context arg2, Hashtable<?, ?> arg3) throws Exception {
        throw new NotImplementedException();
    }

}