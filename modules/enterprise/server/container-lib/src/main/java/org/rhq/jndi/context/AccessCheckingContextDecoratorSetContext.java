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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.naming.Context;

import org.rhq.jndi.util.DecoratorSetContext;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class AccessCheckingContextDecoratorSetContext implements DecoratorSetContext<Context, ContextDecorator> {

    private static final Set<Class<? extends ContextDecorator>> DECORATOR_CLASSES;
    static {
        HashSet<Class<? extends ContextDecorator>> tmp = new HashSet<Class<? extends ContextDecorator>>();
        tmp.add(AccessCheckingContextDecorator.class);
        tmp.add(AccessCheckingDirContextDecorator.class);
        tmp.add(AccessCheckingEventContextDecorator.class);
        tmp.add(AccessCheckingEventDirContextDecorator.class);
        tmp.add(AccessCheckingLdapContextDecorator.class);
        
        DECORATOR_CLASSES = Collections.unmodifiableSet(tmp);
    }
    
    private Set<Class<? extends Context>> supportedInterfaces;
    private String[] checkedSchemes;
    
    public AccessCheckingContextDecoratorSetContext(Set<Class<? extends Context>> supportedInterfaces, String... checkedSchemes) {
        this.supportedInterfaces = supportedInterfaces;
        this.checkedSchemes = checkedSchemes;
    }
    
    public ContextDecorator instantiate(Class<? extends ContextDecorator> decoratorClass) throws Exception {
        Constructor<? extends ContextDecorator> ctor = decoratorClass.getConstructor(String[].class);
        
        return ctor.newInstance((Object)checkedSchemes);
    }

    public Set<Class<? extends Context>> getSupportedInterfaces() {
        return supportedInterfaces;
    }
    
    public Set<Class<? extends ContextDecorator>> getDecoratorClasses() {
        return DECORATOR_CLASSES;
    }
    
    public void init(ContextDecorator decorator, Context object) throws Exception {
        decorator.init(object);
    }
}
