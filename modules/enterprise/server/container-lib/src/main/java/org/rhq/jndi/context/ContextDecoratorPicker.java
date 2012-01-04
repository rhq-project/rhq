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

package org.rhq.jndi.context;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingException;

/**
 * @author Lukas Krejci
 */
public class ContextDecoratorPicker  {

    private Set<Class<? extends ContextDecorator>> possibleDecorators = new HashSet<Class<? extends ContextDecorator>>();
    private Class<?>[] constructorParameterTypes;
    private Object[] constructorParameters;
    
    public ContextDecoratorPicker() {
    }

    public ContextDecoratorPicker(Collection<? extends Class<? extends ContextDecorator>> c) {
        possibleDecorators.addAll(c);
    }

    public Set<Class<? extends ContextDecorator>> getPossibleDecorators() {
        return possibleDecorators;
    }
    
    public Class<?>[] getConstructorParameterTypes() {
        return constructorParameterTypes;
    }

    public void setConstructorParameterTypes(Class<?>[] constructorParameterTypes) {
        this.constructorParameterTypes = constructorParameterTypes;
    }

    public Object[] getConstructorParameters() {
        return constructorParameters;
    }

    public void setConstructorParameters(Object[] constructorParameters) {
        this.constructorParameters = constructorParameters;
    }
    
    public Context wrapInAppropriateDecorator(Context context) throws NamingException {
        Class<? extends ContextDecorator> cls = getMatchByInterfaces(context, possibleDecorators);

        if (cls == null) {
            throw new IllegalArgumentException("Could not find a matching context decorator for " + context.getClass() + " in " + this);
        }
        
        Constructor<? extends ContextDecorator> ctor = null;
        try {
            ctor = cls.getConstructor(constructorParameterTypes);
            ContextDecorator ctx = ctor.newInstance(constructorParameters);
            
            ctx.init(context);
            
            return ctx;
        } catch (SecurityException e) {
            throw new IllegalStateException("Could not instantiate a class through reflection.", e);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                "Could not construct a context decorator - unable to find a constructor with parameters "
                    + (constructorParameterTypes == null ? "[no parameters]" : Arrays.asList(constructorParameterTypes)) + " on class " + cls.getName(), e);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Could not instantiate a context decorator " + cls + " using constructor " + ctor, e);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("Could not instantiate a context decorator " + cls + " using constructor " + ctor, e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Could not instantiate a context decorator " + cls + " using constructor " + ctor, e);
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException("Could not instantiate a context decorator " + cls + " using constructor " + ctor, e);
        }
    }

    private static <T> Class<? extends T> getMatchByInterfaces(Object obj, Set<Class<? extends T>> classes) {
        Set<Class<?>> ifaces = getAllImplementedInterfaces(obj.getClass());
        
        Class<? extends T> match = null;
        int maxMatchCnt = Integer.MIN_VALUE;
        
        for(Class<? extends T> cls : classes) {
            int cnt = getBestMatchByIfaces(cls, ifaces);
            if (cnt > maxMatchCnt) {
                maxMatchCnt = cnt;
                match = cls;
            }
        }
        
        return match;
    }
    
    private static int getBestMatchByIfaces(Class<?> cls, Set<Class<?>> ifaces) {
        int ret = 0;
        
        //count how many interfaces from the supplied array the class implements
        for(Class<?> iface : ifaces) {
            if (iface.isAssignableFrom(cls)) {
                ++ret;
            } else {
                --ret;
            }
        }
        
        //that's not all - to get the best possible match, we need to take into account
        //the fact that the class might implement more than just the interfaces provided
        Set<Class<?>> clsIfaces = getAllImplementedInterfaces(cls);
        
        for(Class<?> clsIface : clsIfaces) {
            if (!(ifaces.contains(clsIface))) {
                --ret;
            }
        }
        
        return ret;
    }
    
    private static Set<Class<?>> getAllImplementedInterfaces(Class<?> cls) {
        HashSet<Class<?>> ret = new HashSet<Class<?>>();
        getAllImplementedInterfaces(cls, ret);
        
        return ret;
    }
    
    private static void getAllImplementedInterfaces(Class<?> cls, Set<Class<?>> output) {
        Class<?>[] ifaces = cls.getInterfaces();
        
        for(Class<?> iface : Arrays.asList(ifaces)) {
            output.add(iface);
            getAllImplementedInterfaces(iface, output);
        }
        
        if (cls.getSuperclass() != null) {
            getAllImplementedInterfaces(cls.getSuperclass(), output);
        }
    }
}
