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

package org.rhq.enterprise.server.naming;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

import org.rhq.enterprise.server.naming.context.ContextDecorator;
import org.rhq.enterprise.server.naming.util.DecoratingInvocationHandler;
import org.rhq.enterprise.server.naming.util.DecoratorPicker;

/**
 * This class implements an initial context factory that decorates the contexts
 * returned from a "wrapped" initial factory passed to this class in the constructor.
 * <p>
 * The contexts returned from the wrapped initial factory are hidden behind a proxy
 * that implements the intersection of interfaces from the <code>supportedContextInterfaces</code>
 * constructor parameter and the actual interfaces the wrapped context implements.
 * <p>
 * The proxy method calls are handled using the {@link DecoratingInvocationHandler} which is initialized
 * with the list of {@link DecoratorPicker pickers} that are used to intercept the method
 * calls on the wrapped context.
 * 
 * @see DecoratorPicker
 * @see DecoratingInvocationHandler
 * 
 * @author Lukas Krejci
 */
public class DecoratingInitialContextFactory implements InitialContextFactory {

    List<DecoratorPicker<Context, ContextDecorator>> pickers;
    private InitialContextFactory factory;
    private Set<Class<? extends Context>> supportedContextInterfaces;
    
    public DecoratingInitialContextFactory(InitialContextFactory factory, List<DecoratorPicker<Context, ContextDecorator>> decoratorPickers) {
        this.factory = factory;
        this.pickers = decoratorPickers;
        this.supportedContextInterfaces = new HashSet<Class<? extends Context>>();
        for(DecoratorPicker<Context, ContextDecorator> picker : pickers) {
            supportedContextInterfaces.addAll(picker.getContext().getSupportedInterfaces());
        }
    }
    
    public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
        Context ctx = factory.getInitialContext(environment);
        
        Set<Class<?>> implementedIfaces = getAllImplementedInterfaces(ctx.getClass());
        Class<?>[] ii = new Class<?>[implementedIfaces.size()];
        implementedIfaces.toArray(ii);
        
        return (Context) Proxy.newProxyInstance(ctx.getClass().getClassLoader(), ii, new DecoratingInvocationHandler<Context, ContextDecorator>(pickers, ctx));
    }

    private Set<Class<?>> getAllImplementedInterfaces(Class<?> cls) {
        HashSet<Class<?>> ret = new HashSet<Class<?>>();
        getAllImplementedInterfaces(cls, ret);

        ret.retainAll(supportedContextInterfaces);

        return ret;
    }

    private static void getAllImplementedInterfaces(Class<?> cls, Set<Class<?>> output) {
        Class<?>[] ifaces = cls.getInterfaces();

        for (Class<?> iface : Arrays.asList(ifaces)) {
            output.add(iface);
            getAllImplementedInterfaces(iface, output);
        }

        if (cls.getSuperclass() != null) {
            getAllImplementedInterfaces(cls.getSuperclass(), output);
        }
    }  
    
    @Override
    public int hashCode() {
        return factory.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        return factory.equals(o);
    }
}
