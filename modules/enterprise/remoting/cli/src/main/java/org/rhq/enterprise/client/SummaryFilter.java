/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.client;

import org.rhq.core.domain.util.Summary;

import javax.persistence.Id;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class SummaryFilter {

    public boolean filter(PropertyDescriptor property) {
        try {
            Field field = getField(property);
            return field.isAnnotationPresent(Summary.class) || field.isAnnotationPresent(Id.class);
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    private Field getField(PropertyDescriptor property) throws NoSuchFieldException {
        String propertyName = property.getName();
        Class<?> declaringClass = getDeclaringClass(property);
        return declaringClass.getDeclaredField(propertyName);
    }

    private Class<?> getDeclaringClass(PropertyDescriptor property) {
        Method method = property.getReadMethod();

        if (method == null) {
            method = property.getWriteMethod();
        }

        return method.getDeclaringClass();
    }

}
