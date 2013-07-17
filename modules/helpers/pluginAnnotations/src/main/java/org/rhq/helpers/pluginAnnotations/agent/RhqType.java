/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.helpers.pluginAnnotations.agent;

import java.io.File;


/**
 * Base data types from RHQ for properties
 * @author Heiko W. Rupp
 */
public enum RhqType {
    INTEGER(new Class<?>[]{Integer.class,int.class},Boolean.class),
    LONG(new Class<?>[]{Long.class,long.class},Long.class),
    DOUBLE(new Class<?>[]{Double.class,double.class},Double.class),
    STRING(new Class<?>[]{String.class},String.class),
    LONG_STRING(new Class<?>[]{},String.class),
    PASSWORD(new Class<?>[]{},String.class),
    BOOLEAN(new Class<?>[]{Boolean.class, boolean.class},Boolean.class),
    FLOAT(new Class<?>[]{Float.class, float.class},Float.class),
    FILE(new Class<?>[]{File.class},File.class),
    DIRECTORY(new Class<?>[]{},File.class),
    VOID(new Class<?>[]{Void.class,void.class},Void.class)
    ;
    private Class<?>[] fromClasses;
    private Class<?> toClass;

    private RhqType(Class<?>[] fromClasses,Class<?> toClass) {

        this.fromClasses = fromClasses;
        this.toClass = toClass;
    }

    public Class<?>[] getFromClasses() {
        return fromClasses;
    }

    public Class<?> getToClass() {
        return toClass;
    }

    public static RhqType findType(Class<?> clazz) {
        for (RhqType type : RhqType.values()) {
            for (Class from : type.getFromClasses()) {
                if (clazz.equals(from)) {
                    return type;
                }
            }
        }
        return null;
    }

    public String getRhqName() {
        String name = name().toLowerCase();
        if (name.equals("long_string")) {
            name = "longString";
        }
        return name;
    }
}
