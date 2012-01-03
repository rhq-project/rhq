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
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

/**
 * A decorator of an {@link InitialContextFactory} that adds the security checking
 * machinery to the returned initial contexts.
 *
 * @author Lukas Krejci
 */
public class AccessCheckingInitialContextFactoryDecorator extends URLPreferringInitialContextFactoryDecorator {

    private final String[] checkedSchemes;
    
    /**
     * @param factory the factory to wrap
     * @param checkedSchemes the list of JNDI name schemes to check for permissions
     * @see AccessCheckingContextDecorator
     */
    public AccessCheckingInitialContextFactoryDecorator(InitialContextFactory factory, String... checkedSchemes) {
        super(factory);
        this.checkedSchemes = checkedSchemes;
    }
    
    /**
     * @param environment the environment variables for the return {@link Context} to use
     * 
     * @return the initial context returned by the decorated factory wrapped in 
     * {@link URLPreferringContext} and {@link AccessCheckingContextDecorator} in that
     * order.
     */
    @Override
    public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
        return new AccessCheckingContextDecorator(super.getInitialContext(environment), checkedSchemes);
    }

}
