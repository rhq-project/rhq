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

package org.rhq.enterprise.client.utility;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import javax.jws.WebParam;

import org.rhq.scripting.MetadataProvider;

/**
 * @author Lukas Krejci
 */
public class CLIMetadataProvider implements MetadataProvider {

    @Override
    public String getParameterName(Method method, int parameterIndex) {
        String name = null;

        Annotation[][] paramAnnotations = method.getParameterAnnotations();

        if (paramAnnotations.length > parameterIndex) {
            Annotation[] annotations = paramAnnotations[parameterIndex];
            for (Annotation a : annotations) {
                if (a instanceof WebParam) {
                    name = ((WebParam) a).name();
                    break;
                }
            }
        }

        return name;
    }

    @Override
    public String getDocumentation(Class<?> clazz) {
        // TODO it'd be fantastic if we could do this, wouldn't it? 
        return null;
    }

    @Override
    public String getDocumentation(Method method) {
        // TODO it'd be fantastic if we could do this, wouldn't it? 
        return null;
    }

    @Override
    public String getTypeName(Type type, boolean fullNames) {
        return ReflectionUtility.getTypeString(type, fullNames);
    }

}
