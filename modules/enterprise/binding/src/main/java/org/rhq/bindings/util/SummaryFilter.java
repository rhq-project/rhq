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
package org.rhq.bindings.util;

import org.rhq.core.domain.util.Summary;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.IndexedPropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class SummaryFilter {



    public PropertyDescriptor[] getPropertyDescriptors(Object object, boolean exportMode) throws IntrospectionException {

        BeanInfo info = Introspector.getBeanInfo(object.getClass(), object.getClass().getSuperclass());

        final Map<PropertyDescriptor, Integer> indexes = new HashMap<PropertyDescriptor, Integer>();



        for (PropertyDescriptor property : info.getPropertyDescriptors()) {
            boolean add = false;
            int index = 100;
            try {
                Field field = getField(property);

                if (field.isAnnotationPresent(Summary.class)) {
                    add = true;
                    index = field.getAnnotation(Summary.class).index();
                } else if (property.getReadMethod() != null && property.getReadMethod().isAnnotationPresent(Summary.class)) {
                    add = true;
                    index = property.getReadMethod().getAnnotation(Summary.class).index();
                }

            } catch (NoSuchFieldException e) {
                if (property.getReadMethod() != null && property.getReadMethod().isAnnotationPresent(Summary.class)) {
                    add = true;
                    index = property.getReadMethod().getAnnotation(Summary.class).index();
                }
            }
            if (add || exportMode) {
                indexes.put(property, index);
            }
        }

        if (indexes.isEmpty()) {
            for (PropertyDescriptor property : info.getPropertyDescriptors()) {
                indexes.put(property, 0);
            }
        }


        PropertyDescriptor[] descriptors;
        descriptors = indexes.keySet().toArray(new PropertyDescriptor[indexes.size()]);
        Arrays.sort(descriptors, new Comparator<PropertyDescriptor>() {
            public int compare(PropertyDescriptor o1, PropertyDescriptor o2) {
                int i = indexes.get(o1).compareTo(indexes.get(o2));
                if (i == 0) {
                    i = o1.getName().compareTo(o2.getName());
                }
                return i;
            }
        });

        return descriptors;
    }


    private Field getField(PropertyDescriptor property) throws NoSuchFieldException {
        String propertyName = property.getName();
        Class<?> declaringClass = getDeclaringClass(property);

        return declaringClass.getDeclaredField(propertyName);
    }

    private Class<?> getDeclaringClass(PropertyDescriptor property) {
        Method method = null;

        if (property instanceof IndexedPropertyDescriptor) {
            method = ((IndexedPropertyDescriptor)property).getIndexedReadMethod();
            if (method == null) {
                method = ((IndexedPropertyDescriptor)property).getIndexedWriteMethod();
            }
        } else {
            method = property.getReadMethod();

            if (method == null) {
                method = property.getWriteMethod();
            }
        }

        return method.getDeclaringClass();

    }


}
