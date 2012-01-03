/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.jndi;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.NamingManager;

/**
 * This is a modification of the default {@link InitialContext} class that
 * ignores the initial context factory builder when constructing the default
 * context to use in lookups etc.
 * <p>
 * This is important because RHQ server has its own initial context factory
 * builder that creates factories that in turn create contexts. If the default
 * {@link InitialContext} implementation was used, we'd never be able to lookup
 * scheme-based names because the default implementation of the {@link InitialContext}
 * always uses the default context of the builder if one is installed no matter 
 * the scheme in the name. 
 * <p>
 * The {@link AccessCheckingInitialContextFactoryBuilder} wraps the context returned
 * by the factory in an instance of this class and thus is restoring the original
 * intended behavior of the {@link InitialContext}. It looks at the name being looked
 * up (bound or whatever) and prefers to use the URL context factories if the name
 * contains the scheme (as does the {@link InitialContext} if no builder is installed).
 * If the name doesn't contain a scheme, the provided default context factory is used to 
 * look up the name.
 * 
 * @author Lukas Krejci
 */
public class URLPreferringContext extends InitialContext {

    public URLPreferringContext(InitialContextFactory defaultContextFactory) throws NamingException {
        super(true);
        this.defaultInitCtx = defaultContextFactory.getInitialContext(null);
        this.gotDefault = true;
        init(null);
    }

    public URLPreferringContext(Hashtable<?, ?> environment, InitialContextFactory defaultContextFactory) throws NamingException {
        super(true);
        this.defaultInitCtx = defaultContextFactory.getInitialContext(environment);
        this.gotDefault = true;
        init(environment);
    }

    @Override
    protected Context getURLOrDefaultInitCtx(Name name) throws NamingException {
        if (name.size() > 0) {
            String first = name.get(0);
            String scheme = getURLScheme(first);
            if (scheme != null) {
                Context ctx = NamingManager.getURLContext(scheme, myProps);
                if (ctx != null) {
                    return ctx;
                }
            } 
        }
        return getDefaultInitCtx();
    }

    @Override
    protected Context getURLOrDefaultInitCtx(String name) throws NamingException {
        String scheme = getURLScheme(name);
        if (scheme != null) {
            Context ctx = NamingManager.getURLContext(scheme, myProps);
            if (ctx != null) {
                return ctx;
            }
        }
        return getDefaultInitCtx();
    }
    
    //copied from InitialContext
    private static String getURLScheme(String str) {
        int colon_posn = str.indexOf(':');
        int slash_posn = str.indexOf('/');

        if (colon_posn > 0 && (slash_posn == -1 || colon_posn < slash_posn))
            return str.substring(0, colon_posn);
        return null;
    }        
}
