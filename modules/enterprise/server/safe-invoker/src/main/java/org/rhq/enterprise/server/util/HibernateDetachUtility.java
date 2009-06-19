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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;

/**
 * @author Greg Hinkle
 */
public class HibernateDetachUtility {

    private static final Log LOG = LogFactory.getLog(HibernateDetachUtility.class);

    public static enum SerializationType {
        SERIALIZATION, JAXB
    }

    public static void nullOutUninitializedFields(Object value, SerializationType serializationType) throws Exception {
        long start = System.currentTimeMillis();
        Set<Integer> checkedObjs = new HashSet<Integer>();
        nullOutUninitializedFields(value, checkedObjs, 0, serializationType);
        LOG.debug("Checked [" + checkedObjs.size() + "] in [" + (System.currentTimeMillis() - start) + "]ms");
    }

    private static void nullOutUninitializedFields(Object value, Set<Integer> nulledObjects, int depth,
        SerializationType serializationType) throws Exception {
        //        System.out.println("*");
        if (depth > 50) {
            //         LOG.warn("Getting different object hierarchies back from calls: " + value.getClass().getName());
            return;
        }

        if ((value == null) || nulledObjects.contains(System.identityHashCode(value))) {
            return;
        }

        nulledObjects.add(System.identityHashCode(value));

        if (value instanceof Collection) {
            // Null out any entries in initialized collections
            for (Object val : (Collection) value) {
                nullOutUninitializedFields(val, nulledObjects, depth + 1, serializationType);
            }
        } else {

            if (serializationType == SerializationType.JAXB) {
                XmlAccessorType at = value.getClass().getAnnotation(XmlAccessorType.class);
                if (at != null && at.value() == XmlAccessType.FIELD) {
                    //System.out.println("----------XML--------- field access");
                    nullOutFieldsByFieldAccess(value, nulledObjects, depth, serializationType);
                } else {
                    //System.out.println("----------XML--------- accessor access");
                    nullOutFieldsByAccessors(value, nulledObjects, depth, serializationType);
                }
            } else if (serializationType == SerializationType.SERIALIZATION) {
                //                System.out.println("-----------JRMP-------- field access");
                nullOutFieldsByFieldAccess(value, nulledObjects, depth, serializationType);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void nullOutFieldsByFieldAccess(Object object, Set<Integer> nulledObjects, int depth,
        SerializationType serializationType) throws Exception {

        // BeanInfo bi = Introspector.getBeanInfo(value.getClass(), Object.class);

        boolean isDomainObject = object.getClass().getName().startsWith("org.rhq.core.domain");
        Field[] fields = object.getClass().getDeclaredFields();

        for (Field field : fields) {
            int mods = field.getModifiers();
            if (Modifier.isStatic(mods) || Modifier.isTransient(mods) || Modifier.isFinal(mods))
                continue;

            Object fieldValue = null;

            try {
                field.setAccessible(true);
                fieldValue = field.get(object);
            } catch (Throwable lie) {
                if (LOG.isDebugEnabled()) {
                    LOG
                        .debug("Couldn't load: " + field.getName() + " off of " + object.getClass().getSimpleName(),
                            lie);
                }
                nullOutField(object, field.getName());
            }

            if (isDomainObject) {
                if (!Hibernate.isInitialized(fieldValue)) {
                    try {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Nulling out: " + field.getName() + " off of "
                                + object.getClass().getSimpleName());
                        }

                        nullOutField(object, field.getName());
                    } catch (Exception lie) {
                        LOG.debug("Couldn't null out: " + field.getName() + " off of "
                            + object.getClass().getSimpleName() + " trying field access", lie);
                    }
                } else {
                    if (null != fieldValue) {
                        if (fieldValue.getClass().getName().contains("javassist")) {

                            Class assistClass = fieldValue.getClass();
                            Method m = assistClass.getMethod("writeReplace");
                            Object replacement = m.invoke(fieldValue);

                            setField(object, field.getName(), replacement);

                            String className = fieldValue.getClass().getName();
                            className = className.substring(0, className.indexOf("_$$_"));
                        }
                    }
                }
            }

            // Get the value in case it was updated above. Recurse on any field value that may
            // itself lead to proxies. That includes any collection and any org.rhq type (like a composite).
            fieldValue = field.get(object);
            if ((null != fieldValue)
                && ((fieldValue instanceof Collection) || fieldValue.getClass().getName().startsWith("org.rhq"))) {
                nullOutUninitializedFields(fieldValue, nulledObjects, depth + 1, serializationType);
            }
        }
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