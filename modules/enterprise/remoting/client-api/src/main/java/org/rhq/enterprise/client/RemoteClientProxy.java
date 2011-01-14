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
package org.rhq.enterprise.client;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.jboss.remoting.invocation.NameBasedInvocation;

import org.rhq.bindings.client.AbstractRhqFacadeProxy;
import org.rhq.bindings.client.RhqManagers;
import org.rhq.bindings.util.InterfaceSimplifier;
import org.rhq.core.server.ExternalizableStrategy;

/**
 * This class acts as a local SLSB proxy to make remote invocations
 * to SLSB Remotes over a remoting invoker.
 *
 * @author Greg Hinkle
 * @author Lukas Krejci
 */
public class RemoteClientProxy extends AbstractRhqFacadeProxy<RemoteClient> {

    public RemoteClientProxy(RemoteClient client, RhqManagers manager) {
        super(client, manager);
    }

    public Class<?> getRemoteInterface() {
        return this.getManager().remote();
    }

    @SuppressWarnings("unchecked")
    public static <T> T getProcessor(RemoteClient remoteClient, RhqManagers manager) {
        try {
            RemoteClientProxy gpc = new RemoteClientProxy(remoteClient, manager);

            Class<?> intf = InterfaceSimplifier.simplify(manager.remote());

            return (T) Proxy
                .newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[] { intf }, gpc);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get remote connection proxy", e);
        }
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            return super.invoke(proxy, method, args);
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }
    }

    protected Object doInvoke(Object proxy, Method originalMethod, java.lang.Class<?>[] argTypes, Object[] args) throws Throwable  {
        ExternalizableStrategy.setStrategy(ExternalizableStrategy.Subsystem.REFLECTIVE_SERIALIZATION);

        String methodName = getManager().beanName() + ":" + originalMethod.getName();

        String[] paramSig = createParamSignature(argTypes);
        
        NameBasedInvocation request = new NameBasedInvocation(methodName, args, paramSig);

        Object response = getRhqFacade().getRemotingClient().invoke(request);

        if (response instanceof Throwable) {
            throw (Throwable) response;
        }
        
        return response;
    }
    
    private String[] createParamSignature(Class<?>[] types) {
        String[] paramSig = new String[types.length];
        for (int x = 0; x < types.length; x++) {
            paramSig[x] = types[x].getName();
        }
        return paramSig;
    }
}
