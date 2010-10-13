/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.core.pc.upgrade;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * This class provides a simple proxy that just forwards the calls to the
 * actual object.
 * <p>
 * This seemingly pointless behavior is useful when dealing with the classes
 * loaded in different classloaders.
 * <p>
 * The arguments passed to the method invocations of the proxy must unfortunately be "understandable"
 * both in the "current" classloader and  in the context of the classloader that loaded the target object. 
 * I don't know about a generic solution to this problem. 
 * A partial solution would be to serialize the arguments and deserialize them in the context
 * of the target classloader but that doesn't cut it for non-serializable classes obviously.
 * 
 * @author Lukas Krejci
 */
public class ForwardingProxy {

    private static class ForwardingHandler implements InvocationHandler {
        private Object target;
        
        public ForwardingHandler(Object target) {
            this.target = target;           
        }
        
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Method targetMethod = findMethodOnTarget(method);
            
            return targetMethod.invoke(target, args);
        }
        
        private Method findMethodOnTarget(Method method) throws SecurityException, NoSuchMethodException {
            Class<?>[] origParamTypes = method.getParameterTypes();
            
            return target.getClass().getMethod(method.getName(), origParamTypes);
        }
    }

    public static <T> T forward(Object target, Class<T> targetInterface, Class<?>... additionalInterfaces) {
        Class<?>[] interfaces = new Class<?>[additionalInterfaces.length + 1];
        interfaces[0] = targetInterface;
        System.arraycopy(additionalInterfaces, 0, interfaces, 1, additionalInterfaces.length);
        
        Object proxy = Proxy.newProxyInstance(targetInterface.getClassLoader(), interfaces, new ForwardingHandler(target));
        return targetInterface.cast(proxy);
    }
}
