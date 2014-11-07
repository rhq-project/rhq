/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.client.utility;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

import org.apache.commons.lang.ClassUtils;

/**
 * @author Greg Hinkle
 */
public class ReflectionUtility {



    public static String getSimpleTypeString(Type type) {
        return getTypeString(type, false);
    }

    public static String getTypeString(Type type, boolean fullNames) {
        if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            Type[] typeArguments = pType.getActualTypeArguments();

            String typeArgString = "";
            for (Type typeArgument : typeArguments) {

                if (typeArgString.length() > 0) {
                    typeArgString += ",";
                }

                typeArgString += getTypeString(typeArgument, fullNames);
            }
            return getTypeString(pType.getRawType(), fullNames) + "<" + typeArgString + ">";

        } else if (type instanceof TypeVariable) {
            TypeVariable<?> vType = (TypeVariable<?>) type;
            return getName(vType.getClass(), fullNames);
        } else if (type instanceof GenericArrayType) {
            GenericArrayType aType = (GenericArrayType) type;
            return getName(aType.getClass(), fullNames) + "["
                + getTypeString(aType.getGenericComponentType(), fullNames) + "]";
        } else if (type instanceof WildcardType) {
            return ((WildcardType) type).toString();
        } else {
            if (type == null) {
                return "";
            } else {
                return ClassUtils.getShortClassName(type.toString());
            }
        }
    }

    private static String getName(Class<?> cls, boolean fullName) {
        return fullName ? cls.getName() : cls.getSimpleName();
    }
}
