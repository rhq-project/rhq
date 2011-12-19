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
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

/**
 * A decorator of an {@link InitialContextFactory} that returns an {@link URLPreferringContext}
 * backed by the wrapped initial context factory.
 * <p>
 * This is to support contexts that don't need to be secured, yet we need to make sure to
 * break the call-chain loop caused by the {@link InitialContext} asking the RHQ's {@link AccessCheckingInitialContextFactoryBuilder} for
 * default contexts. 
 *
 * @author Lukas Krejci
 */
public class URLPreferringInitialContextFactoryDecorator implements InitialContextFactory {

    private final InitialContextFactory factory;
    
    public URLPreferringInitialContextFactoryDecorator(InitialContextFactory factory) {
        this.factory = factory;
    }
    
    public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
        return new URLPreferringContext(environment, getFactory());
    }

    protected InitialContextFactory getFactory() {
        return factory;
    }
}
