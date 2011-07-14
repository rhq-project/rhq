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

package org.rhq.enterprise.client;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.bindings.client.AbstractRhqFacadeProxy;
import org.rhq.bindings.client.RhqManagers;

/**
 * This is a proxy interface that simply calls given (de-simplified) method on a provided object.
 * The use-case here is to invoke a method from a remote interface on the local SLSB.
 *
 * @author Lukas Krejci
 */
public class LocalClientProxy extends AbstractRhqFacadeProxy<LocalClient> {
   
    private static final Log LOG = LogFactory.getLog(LocalClientProxy.class);
    
    private Object localSLSB;
    
    public LocalClientProxy(Object localSLSB, LocalClient client, RhqManagers manager) {
        super(client, manager);
        this.localSLSB = localSLSB;
    }
    
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            return super.invoke(proxy, method, args);
        } catch (Throwable t) {
            LOG.warn("Proxied invocation of method [" + method + "] on [" + getManager() + "] failed.", t);
            throw t;
        }
    }
    
    protected Object doInvoke(Object proxy, Method originalMethod, java.lang.Class<?>[] argTypes, Object[] args) throws Throwable {
        try {
            Method realMethod = localSLSB.getClass().getMethod(originalMethod.getName(), argTypes);
            
            return realMethod.invoke(localSLSB, args);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Method [" + originalMethod + "] does not have a desimplified counterpart with arguments " + Arrays.asList(argTypes) + ".", e);
        }
    };
    
}
