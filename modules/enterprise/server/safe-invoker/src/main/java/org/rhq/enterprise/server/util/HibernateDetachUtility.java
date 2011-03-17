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
package org.rhq.enterprise.server.util;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;

/**
 * @author Greg Hinkle
 */
@SuppressWarnings("unchecked")
public class HibernateDetachUtility {

    private static final Log LOG = LogFactory.getLog(HibernateDetachUtility.class);

    public static enum SerializationType {
        SERIALIZATION, JAXB
    }

    public static void nullOutUninitializedFields(Object value, SerializationType serializationType) throws Exception {
        long start = System.currentTimeMillis();
        Set<Integer> checkedObjs = new HashSet<Integer>();
        nullOutUninitializedFields(value, checkedObjs, 0, serializationType);
        long duration = System.currentTimeMillis() - start;
        if (duration > 1000) {
            LOG.info("Detached [" + checkedObjs.size() + "] objects in [" + duration + "]ms");
        } else {
            LOG.debug("Detached [" + checkedObjs.size() + "] objects in [" + duration + "]ms");
        }
    }

    private static void nullOutUninitializedFields(Object value, Set<Integer> nulledObjects, int depth,
        SerializationType serializationType) throws Exception {
        if (depth > 50) {
            LOG.warn("Getting different object hierarchies back from calls: " + value.getClass().getName());
            return;
        }

        if ((value == null) || nulledObjects.contains(System.identityHashCode(value))) {
            return;
        }

        nulledObjects.add(System.identityHashCode(value));

        if (value instanceof Object[]) {
            Object[] objArray = (Object[]) value;
            for (int i = 0; i < objArray.length; i++) {
                nullOutUninitializedFields(objArray[i], nulledObjects, depth + 1, serializationType);
            }

        } else if (value instanceof List) {
            // Null out any entries in initialized collections
            ListIterator i = ((List) value).listIterator();
            while (i.hasNext()) {
                Object val = i.next();
                Object replace = replaceObject(val);
                if (replace != null) {
                    val = replace;
                    i.set(replace);
                }
                nullOutUninitializedFields(val, nulledObjects, depth + 1, serializationType);
            }

        } else if (value instanceof Collection) {
            Collection collection = (Collection) value;
            Collection itemsToBeReplaced = new ArrayList();
            Collection replacementItems = new ArrayList();
            for (Object item : collection) {
                Object replacementItem = replaceObject(item);
                if (replacementItem != null) {
                    itemsToBeReplaced.add(item);
                    replacementItems.add(replacementItem);
                    item = replacementItem;
                }
                nullOutUninitializedFields(item, nulledObjects, depth + 1, serializationType);
            }
            collection.removeAll(itemsToBeReplaced);
            collection.addAll(replacementItems); // watch out! if this collection is a Set, HashMap$MapSet doesn't support addAll. See BZ 688000
        } else if (value instanceof Map) {
            for (Object key : ((Map) value).keySet()) {
                nullOutUninitializedFields(((Map) value).get(key), nulledObjects, depth + 1, serializationType);
                nullOutUninitializedFields(key, nulledObjects, depth + 1, serializationType);
            }
        } else if (value instanceof Enum) {
            // don't need to detach enums, treat them as special objects
            return;
        }

        if (serializationType == SerializationType.JAXB) {
            XmlAccessorType at = value.getClass().getAnnotation(XmlAccessorType.class);
            if (at != null && at.value() == XmlAccessType.FIELD) {
                nullOutFieldsByFieldAccess(value, nulledObjects, depth, serializationType);
            } else {
                nullOutFieldsByAccessors(value, nulledObjects, depth, serializationType);
            }
        } else if (serializationType == SerializationType.SERIALIZATION) {
            nullOutFieldsByFieldAccess(value, nulledObjects, depth, serializationType);
        }

    }

    private static void nullOutFieldsByFieldAccess(Object object, Set<Integer> nulledObjects, int depth,
        SerializationType serializationType) throws Exception {

        Class tmpClass = object.getClass();
        List<Field> fieldsToClean = new ArrayList<Field>();
        while (tmpClass != null && tmpClass != Object.class) {
            Collections.addAll(fieldsToClean, tmpClass.getDeclaredFields());
            tmpClass = tmpClass.getSuperclass();
        }

        nullOutFieldsByFieldAccess(object, fieldsToClean, nulledObjects, depth, serializationType);
    }

    private static void nullOutFieldsByFieldAccess(Object object, List<Field> classFields, Set<Integer> nulledObjects,
        int depth, SerializationType serializationType) throws Exception {

        boolean accessModifierFlag = false;
        for (Field field : classFields) {
            accessModifierFlag = false;
            if (!field.isAccessible()) {
                field.setAccessible(true);
                accessModifierFlag = true;
            }

            Object fieldValue = field.get(object);

            if (fieldValue instanceof HibernateProxy) {

                Object replacement = null;
                if (fieldValue.getClass().getName().contains("javassist")) {

                    Class assistClass = fieldValue.getClass();
                    try {
                        Method m = assistClass.getMethod("writeReplace");
                        replacement = m.invoke(fieldValue);

                        String className = fieldValue.getClass().getName();
                        className = className.substring(0, className.indexOf("_$$_"));
                        if (!replacement.getClass().getName().contains("hibernate")) {
                            nullOutUninitializedFields(replacement, nulledObjects, depth + 1, serializationType);

                            field.set(object, replacement);
                        } else {
                            replacement = null;
                        }
                    } catch (Exception e) {
                        LOG.error("Unable to write replace object " + fieldValue.getClass(), e);
                    }
                }

                if (replacement == null) {

                    String className = ((HibernateProxy) fieldValue).getHibernateLazyInitializer().getEntityName();
                    Class clazz = Class.forName(className);
                    Class[] constArgs = { Integer.class };
                    Constructor construct = null;

                    try {
                        construct = clazz.getConstructor(constArgs);
                        replacement = construct.newInstance((Integer) ((HibernateProxy) fieldValue)
                            .getHibernateLazyInitializer().getIdentifier());
                        field.set(object, replacement);
                    } catch (NoSuchMethodException nsme) {

                        try {
                            Field idField = clazz.getDeclaredField("id");
                            Constructor ct = clazz.getDeclaredConstructor();
                            ct.setAccessible(true);
                            replacement = ct.newInstance();
                            if (!idField.isAccessible()) {
                                idField.setAccessible(true);
                            }
                            idField.set(replacement, (Integer) ((HibernateProxy) fieldValue)
                                .getHibernateLazyInitializer().getIdentifier());
                        } catch (Exception e) {
                            e.printStackTrace();
                            LOG.error("No id constructor and unable to set field id for base bean " + className, e);
                        }

                        field.set(object, replacement);
                    }
                }

            } else {
                if (fieldValue instanceof org.hibernate.collection.PersistentCollection) {
                    // Replace hibernate specific collection types

                    if (!((org.hibernate.collection.PersistentCollection) fieldValue).wasInitialized()) {
                        field.set(object, null);
                    } else {

                        Object replacement = null;
                        boolean needToNullOutFields = true; // needed for BZ 688000
                        if (fieldValue instanceof Map) {
                            replacement = new HashMap((Map) fieldValue);
                        } else if (fieldValue instanceof List) {
                            replacement = new ArrayList((List) fieldValue);
                        } else if (fieldValue instanceof Set) {
                            ArrayList l = new ArrayList((Set) fieldValue); // cannot recurse Sets, see BZ 688000
                            nullOutUninitializedFields(l, nulledObjects, depth + 1, serializationType);
                            replacement = new HashSet(l); // convert it back to a Set since that's the type of the real collection, see BZ 688000
                            needToNullOutFields = false;
                        } else if (fieldValue instanceof Collection) {
                            replacement = new ArrayList((Collection) fieldValue);
                        }
                        setField(object, field.getName(), replacement);

                        if (needToNullOutFields) {
                            nullOutUninitializedFields(replacement, nulledObjects, depth + 1, serializationType);
                        }
                    }

                } else {
                    if (fieldValue != null
                        && (fieldValue.getClass().getName().contains("org.rhq") || fieldValue instanceof Collection
                            || fieldValue instanceof Object[] || fieldValue instanceof Map))
                        nullOutUninitializedFields((fieldValue), nulledObjects, depth + 1, serializationType);
                }
            }
            if (accessModifierFlag) {
                field.setAccessible(false);
            }
        }

    }

    private static Object replaceObject(Object object) {
        Object replacement = null;

        if (object instanceof HibernateProxy) {
            if (object.getClass().getName().contains("javassist")) {
                Class assistClass = object.getClass();
                try {
                    Method m = assistClass.getMethod("writeReplace");
                    replacement = m.invoke(object);

                    String className = object.getClass().getName();
                } catch (Exception e) {
                    LOG.error("Unable to write replace object " + object.getClass(), e);
                }
            }
        }
        return replacement;
    }

    private static void nullOutFieldsByAccessors(Object value, Set<Integer> nulledObjects, int depth,
        SerializationType serializationType) throws Exception {
        // Null out any collections that aren't loaded
        BeanInfo bi = Introspector.getBeanInfo(value.getClass(), Object.class);

        PropertyDescriptor[] pds = bi.getPropertyDescriptors();
        for (PropertyDescriptor pd : pds) {
            Object propertyValue = null;
            try {
                propertyValue = pd.getReadMethod().invoke(value);
            } catch (Throwable lie) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Couldn't load: " + pd.getName() + " off of " + value.getClass().getSimpleName(), lie);
                }
            }

            if (!Hibernate.isInitialized(propertyValue)) {
                try {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Nulling out: " + pd.getName() + " off of " + value.getClass().getSimpleName());
                    }

                    Method writeMethod = pd.getWriteMethod();
                    if ((writeMethod != null) && (writeMethod.getAnnotation(XmlTransient.class) == null)) {
                        pd.getWriteMethod().invoke(value, new Object[] { null });
                    } else {
                        nullOutField(value, pd.getName());
                    }
                } catch (Exception lie) {
                    LOG.debug("Couldn't null out: " + pd.getName() + " off of " + value.getClass().getSimpleName()
                        + " trying field access", lie);
                    nullOutField(value, pd.getName());
                }
            } else {
                if ((propertyValue instanceof Collection)
                    || ((propertyValue != null) && propertyValue.getClass().getName().startsWith("org.rhq.core.domain"))) {
                    nullOutUninitializedFields(propertyValue, nulledObjects, depth + 1, serializationType);
                }
            }
        }
    }

    private static void setField(Object object, String fieldName, Object newValue) {
        try {
            Field f = object.getClass().getDeclaredField(fieldName);
            if (f != null) {
                // try to set the field this way
                f.setAccessible(true);
                f.set(object, newValue);
            }
        } catch (NoSuchFieldException e) {
            // ignore this
        } catch (IllegalAccessException e) {
            // ignore this
        }
    }

    private static void nullOutField(Object value, String fieldName) {
        try {
            Field f = value.getClass().getDeclaredField(fieldName);
            if (f != null) {
                // try to set the field this way
                f.setAccessible(true);
                f.set(value, null);
            }
        } catch (NoSuchFieldException e) {
            // ignore this
        } catch (IllegalAccessException e) {
            // ignore this
        }
    }
}