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
package org.rhq.enterprise.communications.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Utilities used for working with Class objects.
 *
 * @author John Mazzitelli
 */
public class ClassUtil {
    /**
     * Maps primitive type names with their associated Class representations.
     */
    private static final Map<String, Class> PRIMITIVE_CLASSES_MAP;

    static {
        PRIMITIVE_CLASSES_MAP = new HashMap<String, Class>();
        PRIMITIVE_CLASSES_MAP.put(Boolean.TYPE.getName(), Boolean.TYPE);
        PRIMITIVE_CLASSES_MAP.put(Character.TYPE.getName(), Character.TYPE);
        PRIMITIVE_CLASSES_MAP.put(Byte.TYPE.getName(), Byte.TYPE);
        PRIMITIVE_CLASSES_MAP.put(Short.TYPE.getName(), Short.TYPE);
        PRIMITIVE_CLASSES_MAP.put(Integer.TYPE.getName(), Integer.TYPE);
        PRIMITIVE_CLASSES_MAP.put(Long.TYPE.getName(), Long.TYPE);
        PRIMITIVE_CLASSES_MAP.put(Float.TYPE.getName(), Float.TYPE);
        PRIMITIVE_CLASSES_MAP.put(Double.TYPE.getName(), Double.TYPE);
    }

    /**
     * Prevents instantiation.
     */
    private ClassUtil() {
    }

    /**
     * Converts a type name (as a String) into its <code>Class</code> representation. Primitives are converted to their
     * object types (e.g. "int" will return <code>Integer.TYPE</code>).
     *
     * @param  type_name the type name string
     *
     * @return the type's <code>Class</code>
     *
     * @throws ClassNotFoundException if the type name cannot be converted to a class object
     */
    public static Class getClassFromTypeName(String type_name) throws ClassNotFoundException {
        Class clazz;

        try {
            clazz = Class.forName(type_name);
        } catch (ClassNotFoundException cnfe) {
            clazz = PRIMITIVE_CLASSES_MAP.get(type_name);

            if (clazz == null) {
                throw cnfe;
            }
        }

        return clazz;
    }
}