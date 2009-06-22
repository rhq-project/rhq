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

import org.jboss.remoting.invocation.NameBasedInvocation;

import org.rhq.core.domain.util.serial.ExternalizableStrategy;

/**
 * @author Greg Hinkle
 */
@SuppressWarnings("unchecked")
public class RemoteClientProxy implements InvocationHandler {

    private RemoteClient client;
    private RemoteClient.Manager manager;

    //    public RHQRemoteClientProxy(RHQRemoteClient client, Class targetClass) {
    public RemoteClientProxy(RemoteClient client, RemoteClient.Manager manager) {
        this.client = client;
        this.manager = manager;
    }

    public static <T> T getProcessor(RemoteClient remoteClient, RemoteClient.Manager manager) {
        try {
            RemoteClientProxy gpc = new RemoteClientProxy(remoteClient, manager);

            return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[] { gpc.manager
                .remote() }, gpc);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get remote connection proxy", e);
        }
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        try {
            // make sure we're serializing in Remote Client mode for rich serialization
            ExternalizableStrategy.setStrategy(ExternalizableStrategy.Subsystem.REMOTEAPI);

            String methodName = manager.beanName() + ":" + method.getName();
            String[] paramSig = createParamSignature(method.getParameterTypes());
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

    private String[] createParamSignature(Class[] types) {
        String[] paramSig = new String[types.length];
        for (int x = 0; x < types.length; x++) {
            paramSig[x] = types[x].getName();
        }
        return paramSig;
    }

}
