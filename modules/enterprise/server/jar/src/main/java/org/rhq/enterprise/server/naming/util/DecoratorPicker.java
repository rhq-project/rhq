/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.enterprise.server.naming.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Given as set of decorators extending given type, this class can pick
 * the most appropriate set of decorators for a class or a method call.
 * <p>
 * To configure the decorator, one has to provide a {@link DecoratorSetContext} that
 * is then used to obtain the list of 
 * {@link DecoratorSetContext#getSupportedInterfaces() supported interfaces}, which are 
 * all the interfaces that should be used for decorator resolution (i.e. all other interfaces 
 * that a class might implement are ignored during decorator resolution), the list of
 * {@link DecoratorSetContext#getDecoratorClasses() decorator classes}, which is a list
 * of decorators the picker can choose from and is also used to instantiate and initialize
 * the decorators.
 * 
 * @author Lukas Krejci
 */
public class DecoratorPicker<Type, Decorator extends Type> {

    private DecoratorSetContext<Type, Decorator> context;

    public DecoratorSetContext<Type, Decorator> getContext() {
        return context;
    }

    public void setContext(DecoratorSetContext<Type, Decorator> decoratorSetContext) {
        this.context = decoratorSetContext;
    }

    /**
     * Returns a set of decorators applicable for given method. The set is established based
     * on the declaring class of the method.
     * 
     * @param method the method to inspect
     * @return the set of decorators that can be used to wrap a method call
     * @throws Exception
     */
    public Set<Decorator> getDecoratorsForMethod(Method method) throws Exception {
        return getDecoratorsForClass_Private(method.getDeclaringClass());
    }

    /**
     * Returns a set of decorators that can be used on instances of given class.
     * @param cls the class to inspect
     * @return
     * @throws Exception
     */
    public Set<Decorator> getDecoratorsForClass(Class<? extends Type> cls) throws Exception {
        return getDecoratorsForClass_Private(cls);
    }

    /**
     * This method first establishes the set of decorators to use based on the class of the supplied
     * object and then chains the decorators (in arbitrary order) with the supplied object at the
     * "root" of the chain.
     * <p>
     * If a method is then called on the returned object, the methods of all the decorators are called
     * in chain (each supposedly calling the next) and finally, at the end of the chain, the method on 
     * the original object (the one supplied to this method) is called.
     * <p>
     * Note that the above is only an intended behavior and actually depends on the implementation of
     * the decorators that are resposinble for the chaining. Each decorator is initialized 
     * (@{link {@link DecoratorSetContext#init(Object, Object)} which should set it up for such chaining.
     *  
     * @param object
     * @return
     * @throws Exception
     */
    public Type decorate(Type object) throws Exception {
        Set<Decorator> decs = getDecoratorsForClass_Private(object.getClass());
        Type ret = object;
        for(Decorator d : decs) {
            context.init(d, ret);
            ret = d;
        }
        
        return ret;
    }
    
    /**
     * Similar to {@link #decorate(Object)} but instead of the class of the object itself,
     * uses the significantSuperClass as the basis for the decorator resolution.
     * <p>
     * This is important, because if the object implements two mutually incompatible sub-interfaces of <code>Type</code>, 
     * the chained decorators might fail to execute a method later on if the decorator depends on the upper part
     * of the chain to implement certain sub-interface of <code>Type</code>.
     * 
     * @param object the object to wrap in decorators
     * @param significantSuperClass the class to base the decorator resolution on
     * @return 
     * @throws Exception
     */
    public Type decorate(Type object, Class<?> significantSuperClass) throws Exception {
        Set<Decorator> decs = getDecoratorsForClass_Private(significantSuperClass);
        Type ret = object;
        for(Decorator d : decs) {
            context.init(d, ret);
            ret = d;
        }
        
        return ret;
    }
    
    private Set<Decorator> getDecoratorsForClass_Private(Class<?> cls) throws Exception {
        Set<Class<? extends Type>> ifaces = getNearestApplicableInterfaces(cls);

        HashSet<Decorator> ret = new HashSet<Decorator>();

        for (Class<? extends Type> iface : ifaces) {
            for (Class<? extends Decorator> decClass : getMatch(iface)) {
                ret.add(context.instantiate(decClass));
            }
        }

        return ret;
    }

    private Set<Class<? extends Type>> getNearestApplicableInterfaces(Class<?> cls) {
        List<Class<? extends Type>> ifaces = new ArrayList<Class<? extends Type>>(getAllApplicableInterfaces(cls));

        //now compact the set to only contain the most concrete interfaces

        Iterator<Class<? extends Type>> it = ifaces.iterator();
        while (it.hasNext()) {
            Class<? extends Type> c = it.next();

            for (int i = 0; i < ifaces.size(); ++i) {
                Class<? extends Type> nextC = ifaces.get(i);
                if (!c.equals(nextC) && c.isAssignableFrom(nextC)) {
                    it.remove();
                    break;
                }
            }
        }

        return new HashSet<Class<? extends Type>>(ifaces);
    }

    private Set<Class<? extends Type>> getAllApplicableInterfaces(Class<?> cls) {
        Set<Class<? extends Type>> ifaces = new HashSet<Class<? extends Type>>();

        for (Class<? extends Type> iface : context.getSupportedInterfaces()) {
            if (iface.isAssignableFrom(cls)) {
                ifaces.add(iface);
            }
        }

        if (ifaces.isEmpty()) {
            throw new IllegalArgumentException("Class " + cls
                + " doesn't implement any of the applicable interfaces. Cannot find decorators for it.");
        }

        return ifaces;
    }

    private Set<Class<? extends Decorator>> getMatch(Class<?> targetIface) {

        Set<Class<? extends Decorator>> ret = new HashSet<Class<? extends Decorator>>();

        for (Class<? extends Decorator> cls : context.getDecoratorClasses()) {
            if (Arrays.asList(cls.getInterfaces()).contains(targetIface)) {
                ret.add(cls);
            }
        }

        return ret;
    }
}
