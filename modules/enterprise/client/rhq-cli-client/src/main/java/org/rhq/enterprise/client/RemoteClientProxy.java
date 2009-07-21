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

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import javassist.bytecode.ParameterAnnotationsAttribute;
import org.jboss.remoting.invocation.NameBasedInvocation;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.util.serial.ExternalizableStrategy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * This class acts as a local SLSB proxy to make remote invocations
 * to SLSB Remotes over a remoting invoker.
 *
 *
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

    public Class getRemoteInterface() {
        return this.manager.remote();
    }

    public static <T> T getProcessor(RemoteClient remoteClient, RemoteClient.Manager manager) {
        try {
            RemoteClientProxy gpc = new RemoteClientProxy(remoteClient, manager);

            Class intf = simplifyInterface(gpc.manager.remote());

            return (T) Proxy.newProxyInstance(
                    Thread.currentThread().getContextClassLoader(),
                    new Class[]{intf},
                    gpc);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get remote connection proxy", e);
        }
    }


    
    private static Class simplifyInterface(Class intf) {
        try {
            ClassPool cp = ClassPool.getDefault();

            String simpleName = intf.getName() + "Simple";

            try {
                CtClass cached = cp.get(simpleName);
                return Class.forName(simpleName, false, cp.getClassLoader());

            } catch (NotFoundException e) {
                // ok... load it
            } catch (ClassNotFoundException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }


            CtClass cc = cp.get(intf.getName());


            CtClass cz = cp.getAndRename(intf.getName(), simpleName);
//            CtClass cz = cp.makeInterface(simpleName, cc);

            cz.defrost();

            cz.setSuperclass(cc);

            CtMethod[] methods = cc.getMethods();

            for (CtMethod originalMethod : methods) {

                CtClass[] params = originalMethod.getParameterTypes();
                if (params.length > 0 && params[0].getName().equals(Subject.class.getName())) {

                    CtClass[] simpleParams = new CtClass[params.length - 1];

                    System.arraycopy(params, 1, simpleParams, 0, params.length - 1);
                    cz.defrost();

                    CtMethod newMethod = CtNewMethod.abstractMethod(originalMethod.getReturnType(), originalMethod.getName(), simpleParams, null, cz);

                    ParameterAnnotationsAttribute originalAnnotationsAttribute =
                            (ParameterAnnotationsAttribute) originalMethod.getMethodInfo().getAttribute(ParameterAnnotationsAttribute.visibleTag);

                    // If there are any parameter annotations, copy the one's we're keeping
                    if (originalAnnotationsAttribute != null) {

                        javassist.bytecode.annotation.Annotation[][] originalAnnotations = originalAnnotationsAttribute.getAnnotations();
                        javassist.bytecode.annotation.Annotation[][] newAnnotations = new javassist.bytecode.annotation.Annotation[originalAnnotations.length - 1][];

                        int i = 1;
                        for (int x = 1; x < originalAnnotations.length; x++) {
                            newAnnotations[x - 1] = new javassist.bytecode.annotation.Annotation[originalAnnotations[x].length];
                            System.arraycopy(originalAnnotations[x], 0, newAnnotations[x - 1], 0, originalAnnotations[x].length);
                        }


                        ParameterAnnotationsAttribute newAnnotationsAttribute =
                                new ParameterAnnotationsAttribute(newMethod.getMethodInfo().getConstPool(), ParameterAnnotationsAttribute.visibleTag);

                        newAnnotationsAttribute.setAnnotations(newAnnotations);

                        newMethod.getMethodInfo().addAttribute(newAnnotationsAttribute);

                    }

                    cz.addMethod(newMethod);
                }
            }


            return cz.toClass();

        } catch (NotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (CannotCompileException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return intf;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        try {
            // make sure we're serializing in Remote Client mode for rich serialization
            ExternalizableStrategy.setStrategy(ExternalizableStrategy.Subsystem.REMOTEAPI);

            String methodName = manager.beanName() + ":" + method.getName();


            Class[] params = method.getParameterTypes();



            try {
                Class originalClass = method.getDeclaringClass().getInterfaces()[0];
                // See if this method really exists or if its a simplified set of parameters
                originalClass.getMethod(method.getName(), method.getParameterTypes());

            } catch (Exception e) {
                
                // If this was not in the original interface it must've been added in the Simplifier... add back the subject argument
                    // if (!method.getName().equals("login") &&  == null) {
                Object[] newArgs = new Object[args.length + 1];
                System.arraycopy(args, 0, newArgs, 1, args.length);
                newArgs[0] = client.getSubject();
                args = newArgs;

                Class[] newParams = new Class[params.length + 1];
                System.arraycopy(params, 0, newParams, 1, params.length);
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

    private String[] createParamSignature(Class[] types) {
        String[] paramSig = new String[types.length];
        for (int x = 0; x < types.length; x++) {
            paramSig[x] = types[x].getName();
        }
        return paramSig;
    }

}
