/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
package org.rhq.enterprise.server.rest.helper;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.core.MultivaluedMap;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.enterprise.server.rest.BadArgumentException;

/**
 * this helper class builds {@link ResourceCriteria based on query parameters map}
 * @author lzoubek
 *
 */
public class ResourceCriteriaHelper {

    /**
     * special parameters are either ignored or handled specialy
     */
    private static final List<String> SPECIAL_PARAMS = Arrays.asList("page", "ps", "strict");

    /**
     * mapping param shortcutName to param full name (this map gets filled in class constructor)
     */
    private static final Map<String, String> PARAM_SHORTCUTS = new LinkedHashMap<String, String>();

    /**
     * we store parameter name shortcuts as text for API documentation purposes
     */
    public static final String PARAM_SHORTCUTS_TEXT = "status=inventoryStatus, availability=currentAvailability, category=resourceCategories, plugin=pluginName, parentId=parentResourceId, parentName=parentResourceName, type=resourceTypeName";
    static {
        String[] pairs = PARAM_SHORTCUTS_TEXT.split(", ");
        for (int i = 0; i < pairs.length; i++) {
            String[] pair = pairs[i].split("=");
            PARAM_SHORTCUTS.put(pair[0], pair[1]);
        }
    }

    /**
     * creates new criteria instance based on given params. Currently we support single value addFilterXXX functions, and strict parameter. Paging is ignored.
     * @param params query parameters
     * @return resource criteria
     */
    public static ResourceCriteria create(MultivaluedMap<String,String> params) {
        ResourceCriteria criteria = new ResourceCriteria();
        criteria.clearPaging();
        Method[] methods = ResourceCriteria.class.getMethods();
        for (Entry<String, List<String>> e : params.entrySet()) {
            String value = params.getFirst(e.getKey());
            if (value == null) {
                continue;
            }
            String paramName = paramName(e.getKey());
            if (SPECIAL_PARAMS.contains(e.getKey())) {
                try {
                    handleSpecialParam(criteria, e.getKey(), value);
                } catch (Exception ex) {
                    throw new BadArgumentException("Unable to parse [" + e.getKey() + "] value [" + value
                        + "] is not valid");
                }
            } else {
                String filterName = "addFilter" + paramName.substring(0, 1).toUpperCase() + paramName.substring(1);
                Method m = findMethod(methods, filterName);
                if (m != null) {
                    try {
                        m.invoke(criteria, getValue(m, paramName, value));
                    } catch (BadArgumentException bae) {
                        throw bae;
                    } catch (Exception ex) {
                        throw new BadArgumentException("Unable to filter by [" + paramName + "] value [" + value
                            + "] is not valid for this filter");
                    }

                } else {
                    throw new BadArgumentException("Unable to filter by [" + paramName + "] : filter does not exist");
                }
            }

        }
        return criteria;
    }

    private static String paramName(String name) {
        String newName = PARAM_SHORTCUTS.get(name);
        return newName == null ? name : newName;
    }

    private static void handleSpecialParam(ResourceCriteria criteria, String filter, String value) {
        if ("strict".equals(filter)) {
            criteria.setStrict(Boolean.parseBoolean(value));
            return;
        }
        // skip ps and page .. we ignore those
    }

    private static Object getValue(Method m, String filter, String value) {
        Class<?> parameterType = m.getParameterTypes()[0];
        if (parameterType.isArray()) {
            parameterType = parameterType.getComponentType();
        }
        if (parameterType.isEnum()) {
            return enumParamValue(filter, value);
        }
        if (parameterType.isAssignableFrom(Integer.class)
            || (parameterType.isPrimitive() && "int".equals(parameterType.getName()))) {
            return Integer.parseInt(value);
        }
        if (parameterType.isAssignableFrom(Long.class)
            || (parameterType.isPrimitive() && "long".equals(parameterType.getName()))) {
            return Long.parseLong(value);
        }

        return value;
    }

    private static Object enumParamValue(String filter, String value) {
        if ("inventoryStatus".equals(filter)) {
            try {
                return InventoryStatus.valueOf(value.toUpperCase());
            } catch (Exception ex) {
                throw new BadArgumentException(filter, "Value " + value + " is not in the list of allowed values: "
                    + Arrays.toString(InventoryStatus.values()));
            }

        }
        if ("currentAvailability".equals(filter)) {
            try {
                return AvailabilityType.valueOf(value.toUpperCase());
            } catch (Exception ex) {
                throw new BadArgumentException(filter, "Value " + value + " is not in the list of allowed values: "
                    + Arrays.toString(AvailabilityType.values()));
            }
        }
        if ("resourceCategories".equals(filter)) {
            try {
                return new ResourceCategory[] { ResourceCategory.valueOf(value.toUpperCase()) };
            } catch (Exception ex) {
                throw new BadArgumentException(filter, "Value " + value + " is not in the list of allowed values: "
                    + Arrays.toString(ResourceCategory.values()));
            }
        }
        return null;

    }

    private static Method findMethod(Method[] methods, String name) {
        for (Method m : methods) {
            if (m.getName().equals(name)) {
                return m;
            }
        }
        return null;
    }
}
