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

package org.rhq.jndi.util;

import java.util.Set;

/**
 * Implementations of this interface provide a context to the {@link DecoratorPicker}.
 *
 * @author Lukas Krejci
 */
public interface DecoratorSetContext<Type, Decorator> {

    /**
     * @return the set of interfaces that the decorators are able support.
     * Usually this should be just a union of all interfaces the decorators implement
     * but can be trimmed down.
     */
    Set<Class<? extends Type>> getSupportedInterfaces();
    
    /**
     * @return the set of all decorator classes in this set
     */
    Set<Class<? extends Decorator>> getDecoratorClasses();
    
    /**
     * Instantiates a new decorator of given class.
     * @param decoratorClass
     * @return
     * @throws Exception
     */
    Decorator instantiate(Class<? extends Decorator> decoratorClass) throws Exception;
    
    /**
     * Initializes the decorator to decorate given object.
     * 
     * @param decorator
     * @param object
     * @throws Exception on error
     */
    void init(Decorator decorator, Type object) throws Exception;
}
