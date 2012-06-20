/*
 * RHQ Management Platform
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.scripting;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * The scripting implementations don't have to supply an implementation of this interface but
 * rather one is provided to the {@link CodeCompletion}.
 * <p>
 * The code completion can ask this implementation for various metadata on methods or classes.
 * <p>
 * The {@link CodeCompletion} implementations can use this information to format more meaningful
 * completion hints.
 * 
 * @author Lukas Krejci
 */
public interface MetadataProvider {

    /**
     * Tries to determine the name of a parameter on a method.
     * 
     * @param method the method
     * @param parameterIndex the index of the method parameter
     * @return The name of the parameter or null if it could not be determined
     */
    String getParameterName(Method method, int parameterIndex);

    /**
     * @param clazz
     * @return the (java)doc on a class or null if none could be determined 
     */
    String getDocumentation(Class<?> clazz);

    /**
     * @param method
     * @return the (java)doc on a method or null if none could be determined
     */
    String getDocumentation(Method method);

    /**
     * This is a utility method for determining the type name including the type parameters.
     * 
     * @param type the type to determine the name for
     * @param fullNames if true, all the types and type parameters will be the full class names, otherwise
     *                  only the simple name of the class / type parameter will be used.
     * @return the name of the type including type parameters
     */
    String getTypeName(Type type, boolean fullNames);
}
