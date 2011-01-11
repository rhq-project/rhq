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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.remoting.invocation.NameBasedInvocation;

import org.rhq.bindings.client.RhqManagers;
import org.rhq.bindings.util.InterfaceSimplifier;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.server.ExternalizableStrategy;

/**
 * This class acts as a local SLSB proxy to make remote invocations
 * to SLSB Remotes over a remoting invoker.
 *
 * @author Greg Hinkle
 */
public class RemoteClientProxy implements InvocationHandler {
    private RemoteClient client;
    private RhqManagers manager;

    //    public RHQRemoteClientProxy(RHQRemoteClient client, Class targetClass) {
    public RemoteClientProxy(RemoteClient client, RhqManagers manager) {
        this.client = client;
        this.manager = manager;
    }

    public Class<?> getRemoteInterface() {
        return this.manager.remote();
    }

    @SuppressWarnings("unchecked")
    public static <T> T getProcessor(RemoteClient remoteClient, RhqManagers manager) {
        try {
            RemoteClientProxy gpc = new RemoteClientProxy(remoteClient, manager);

            Class<?> intf = InterfaceSimplifier.simplify(gpc.manager.remote());

            return (T) Proxy
                .newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[] { intf }, gpc);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get remote connection proxy", e);
        }
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        try {
            // make sure we're serializing in Remote Client mode for rich serialization
            ExternalizableStrategy.setStrategy(ExternalizableStrategy.Subsystem.REFLECTIVE_SERIALIZATION);

            String methodName = manager.beanName() + ":" + method.getName();

            Class<?>[] params = method.getParameterTypes();

            try {
                Class<?>[] interfaces = method.getDeclaringClass().getInterfaces();

                Class<?> originalClass;
                if (interfaces != null && interfaces.length > 0) {
                    originalClass = interfaces[0];
                } else {
                    originalClass = method.getDeclaringClass();
                }

                // See if this method really exists or if its a simplified set of parameters
                originalClass.getMethod(method.getName(), method.getParameterTypes());

            } catch (Exception e) {
                // If this was not in the original interface it must've been added in the Simplifier... add back the subject argument
                int numArgs = (null == args) ? 0 : args.length;
                Object[] newArgs = new Object[numArgs + 1];
                if (numArgs > 0) {
                    System.arraycopy(args, 0, newArgs, 1, numArgs);
                }
                newArgs[0] = client.getSubject();
                args = newArgs;

                int numParams = (null == params) ? 0 : params.length;
                Class<?>[] newParams = new Class[numParams + 1];
                if (numParams > 0) {
                    System.arraycopy(params, 0, newParams, 1, numParams);
                }
                newParams[0] = Subject.class;
                params = newParams;
            }

            String[] paramSig = createParamSignature(params);
            NameBasedInvocation request = new NameBasedInvocation(methodName, args, paramSig);

            Object response = client.getRemotingClient().invoke(request);

            if (response instanceof Throwable) {
                throw (Throwable) response;
            }
            return response;
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }
    }

    private String[] createParamSignature(Class<?>[] types) {
        String[] paramSig = new String[types.length];
        for (int x = 0; x < types.length; x++) {
            paramSig[x] = types[x].getName();
        }
        return paramSig;
    }
}
