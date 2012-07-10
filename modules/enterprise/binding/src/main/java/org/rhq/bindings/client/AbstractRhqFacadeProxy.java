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
package org.rhq.bindings.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.rhq.bindings.util.InterfaceSimplifier;

/**
 * An abstract {@link InvocationHandler} to help the script users create proxies to actually call the
 * correct methods on RHQ. This base implementation provides its inheritors with the facade impl and the concrete
 * manager they should invoke the methods on and does the "de-simplification" of the arguments (i.e. the opposite of {@link InterfaceSimplifier#simplify(Class)}.
 *
 * @author Lukas Krejci
 */
public abstract class AbstractRhqFacadeProxy<T extends RhqFacade> implements InvocationHandler {

    private T facade;
    private RhqManager manager;
    
    protected AbstractRhqFacadeProxy(T facade, RhqManager manager) {
        this.facade = facade;
        this.manager = manager;
    }
    
    protected T getRhqFacade() {
        return facade;
    }
    
    protected RhqManager getManager() {
        return manager;
    }
    
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Method origMethod = InterfaceSimplifier.getOriginalMethod(method);

        if (origMethod != null) {
            if (InterfaceSimplifier.isSimplified(method)) {
                // If this was not in the original interface it must've been added in the Simplifier... add back the subject argument
                int numArgs = (null == args) ? 0 : args.length;
                Object[] newArgs = new Object[numArgs + 1];
                if (numArgs > 0) {
                    System.arraycopy(args, 0, newArgs, 1, numArgs);
                }
                newArgs[0] = getRhqFacade().getSubject();

                args = newArgs;
            }
            method = origMethod;
        }
        
        return doInvoke(proxy, method, args);
    }

    /**
     * This method actually calls the method according to the RhqFacade's implementation. The <code>argTypes</code>
     * and <code>args</code> are de-simplified (and thus the method can't be supplied just as a simple {@link Method} instance).
     * 
     * @param proxy the proxy the method is executing on
     * @param originalMethod the original method
     * @param args the de-simplified argumens
     * @return the result of the invocation
     * @throws Throwable if invocation throws an error
     */
    protected abstract Object doInvoke(Object proxy, Method originalMethod, Object[] args) throws Throwable;
}
