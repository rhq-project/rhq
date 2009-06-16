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

import org.jboss.remoting.invocation.NameBasedInvocation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author Greg Hinkle
 */
public class RHQRemoteClientProxy implements InvocationHandler {

    private RHQRemoteClient client;
//    private Class targetClass;
    private String targetClass;

//    public RHQRemoteClientProxy(RHQRemoteClient client, Class targetClass) {
    public RHQRemoteClientProxy(RHQRemoteClient client, String targetClass) {
        this.client = client;
        this.targetClass = targetClass;
    }

//    public static <T> T getProcessor(RHQRemoteClient remoteClient, Class targetClass, Class<T> targetInterface) {
    public static <T> T getProcessor(RHQRemoteClient remoteClient, String targetClass, Class<T> targetInterface) {
        try {
            RHQRemoteClientProxy gpc = new RHQRemoteClientProxy(remoteClient, targetClass);

            return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                    new Class[]{targetInterface}, gpc);
        } catch (Exception e) {
            throw new RuntimeException("Failled to get remote connection proxy", e);
        }
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        try {
//            String methodName = targetClass.getSimpleName() + ":" + method.getName();
            String methodName = targetClass + ":" + method.getName();
            String[] paramSig = createParamSignature(method.getParameterTypes());
            NameBasedInvocation request = new NameBasedInvocation(methodName,
                    args,
                    paramSig);

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
