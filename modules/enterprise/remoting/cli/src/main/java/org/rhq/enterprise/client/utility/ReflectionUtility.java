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

import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.WildcardType;

/**
 * @author Greg Hinkle
 */
public class ReflectionUtility {



    public static String getSimpleTypeString(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            Type[] typeArguments = pType.getActualTypeArguments();

            String typeArgString = "";
            for (Type typeArgument : typeArguments) {

                if (typeArgString.length() > 0) {
                    typeArgString += ",";
                }

                typeArgString += getSimpleTypeString(typeArgument);
            }
            return getSimpleTypeString(pType.getRawType()) + "<" + typeArgString + ">";


        } else if (type instanceof TypeVariable) {
            TypeVariable vType = (TypeVariable) type;
            return vType.getClass().getSimpleName();
        } else if (type instanceof GenericArrayType) {
            GenericArrayType aType = (GenericArrayType) type;
            return aType.getClass().getSimpleName() + "[" + getSimpleTypeString(aType.getGenericComponentType()) + "]";
        } else if (type instanceof WildcardType) {
            return ((WildcardType)type).toString();
        } else {
            if (type == null) {
                return "";
            } else {
                return ((Class)type).getSimpleName();
            }
        }
    }

}
