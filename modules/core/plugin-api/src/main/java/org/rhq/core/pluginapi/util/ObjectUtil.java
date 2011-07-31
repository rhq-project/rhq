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
package org.rhq.core.pluginapi.util;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.HashSet;
import java.util.Set;
import java.lang.reflect.Method;

import javax.management.openmbean.CompositeData;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.Nullable;

import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.util.exception.ThrowableUtil;

 /**
 * @author Greg Hinkle
 */
public class ObjectUtil {
    private static final Log LOG = LogFactory.getLog(ObjectUtil.class);

     /**
     * Returns the value of the property with the specified name for the given Object, or null if the Object has no such
     * property.
     *
     * @param obj an Object
     * @param propertyName the name of the property whose value should be returned
     *
     * @return the value of the property with the specified name for the given Object, or null if the Object has no such
     *         property
     */
    @Nullable
    public static Object lookupAttributeProperty(Object obj, String propertyName) {
        Object value = null;
        if (obj instanceof CompositeData) {
            CompositeData compositeData = ((CompositeData)obj);
            if (compositeData.containsKey(propertyName)) {
                value = compositeData.get(propertyName);
            } else {
                LOG.debug("Unable to read attribute/property [" + propertyName + "] from object [" + obj + "] using OpenMBean API - no such property.");
            }
        } else {
            // Try to use reflection.
            try {
                PropertyDescriptor[] pds = Introspector.getBeanInfo(obj.getClass()).getPropertyDescriptors();
                boolean propertyFound = false;
                for (PropertyDescriptor pd : pds) {
                    if (pd.getName().equals(propertyName)) {
                        propertyFound = true;
                        Method readMethod = pd.getReadMethod();
                        if (readMethod == null) {
                            LOG.debug("Unable to read attribute/property [" + propertyName + "] from object [" + obj
                                + "] using Reflection - property is not readable (i.e. it has no getter).");
                        } else {
                            value = readMethod.invoke(obj);
                        }
                    }
                }
                if (!propertyFound) {
                    LOG.debug("Unable to read attribute/property [" + propertyName + "] from object [" + obj
                        + "] using Reflection - no such property.");
                }
            } catch (Exception e) {
                LOG.debug("Unable to read attribute/property [" + propertyName + "] from object [" + obj
                        + "] using Reflection - cause: " + ThrowableUtil.getAllMessages(e));
            }
        }

        return value;
    }

    private static Set<String> getAttributeNames(Set<MeasurementScheduleRequest> requests) {
        Set<String> names = new HashSet<String>();
        for (MeasurementScheduleRequest request : requests) {
            names.add(getAttributeName(request.getName()));
        }

        return names;
    }

    private static String getAttributeName(String property) {
        if (property.startsWith("{")) {
            return property.substring(1, property.indexOf('.'));
        } else {
            return property;
        }
    }

    private static String getAttributeProperty(String property) {
        if (property.startsWith("{")) {
            return property.substring(property.indexOf('.') + 1, property.length() - 1);
        } else {
            return null;
        }
    }

    /**
     * Reads a numeric value from a deep object graph as per {@see ObjectUtil.lookupDeepAttributeProperty()}
     *
     * @param  value        The object to look into
     * @param  propertyPath the property path to search
     *
     * @return the double value read from the object's property path or Double.NaN if it can't be read
     */
    public static Double lookupDeepNumericAttributeProperty(Object value, String propertyPath) {
        Object val = lookupDeepAttributeProperty(value, propertyPath);
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        } else {
            return Double.NaN;
        }
    }

    /**
     * Looks up deep object graph attributes using a dot delimited java bean spec style path. So if I have an object A
     * with a Member object B with a String value C I can refer to it as "b.c" and pass in the object A and I'll get
     * back the value of a.getB().getC()
     *
     * @param  value        The object to look into
     * @param  propertyPath the property path to search
     *
     * @return the value read from the object's property path
     */
    public static Object lookupDeepAttributeProperty(Object value, String propertyPath) {

        if (value==null)
            return null;

        String[] ps = propertyPath.split("\\.", 2);

        String searchProperty = ps[0];

        if (value instanceof CompositeData) {
            CompositeData compositeData = ((CompositeData) value);
            if (compositeData.containsKey(searchProperty)) {
                value = compositeData.get(searchProperty);
            } else {
                LOG.debug("Unable to read attribute property [" + propertyPath + "] from composite data value");
            }
        } else {
            // Try to use reflection
            try {
                PropertyDescriptor[] pds = Introspector.getBeanInfo(value.getClass()).getPropertyDescriptors();
                for (PropertyDescriptor pd : pds) {
                    if (pd.getName().equals(searchProperty)) {
                        value = pd.getReadMethod().invoke(value);
                    }
                }
            } catch (Exception e) {
                LOG.debug("Unable to read property from measurement attribute [" + searchProperty + "] not found on ["
                    + ((value != null) ? value.getClass().getSimpleName() : "null") + "]");
            }
        }

        if (ps.length > 1) {
            value = lookupDeepAttributeProperty(value, ps[1]);
        }

        return value;
    }

    // Prevent instantiation of this class.
    private ObjectUtil() {
    }
 }
