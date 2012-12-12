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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class DecoratingInvocationHandler<Type, Decorator extends Type> implements InvocationHandler {

    private final List<DecoratorPicker<Type, Decorator>> pickers;
    private Type object;
    
    public DecoratingInvocationHandler(List<DecoratorPicker<Type, Decorator>> pickers, Type object) {
        this.pickers = pickers;
        this.object = object;
    }
    
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Type target = object;
        Class<?> methodClass = method.getDeclaringClass();
        
        for(DecoratorPicker<Type, Decorator> picker : pickers) {
            target = picker.decorate(target, methodClass);
        }
        
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            if (e.getCause() != null) {
                throw e.getCause();
            } else {
                throw e;
            }
        }
    }
}