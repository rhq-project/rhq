/*
 * RHQ Management Platform
 * Copyright (C) 2013 Red Hat, Inc.
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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.rhq.core.domain.criteria.Criteria;

/**
 * @author Lukas Krejci
 * @since 4.9
 */
public class CriteriaUtil {

    private CriteriaUtil() {

    }

    public static List<Field> getFields(Criteria criteria, Criteria.Type type) {
        String prefix = type.name().toLowerCase();
        List<Field> results = new ArrayList<Field>();

        Class<?> currentLevelClass = criteria.getClass();
        List<String> globalFields = type.getGlobalFields();
        boolean isCriteriaClass = false;

        do {
            isCriteriaClass = currentLevelClass.equals(Criteria.class);

            for (Field field : currentLevelClass.getDeclaredFields()) {
                if (isCriteriaClass) {
                    if (globalFields.contains(field.getName()))
                        results.add(field);

                } else if (field.getName().startsWith(prefix)) {
                    results.add(field);
                }
            }

            currentLevelClass = currentLevelClass.getSuperclass();

        } while (!isCriteriaClass);

        return results;
    }

    /**
     * This method is <b>VERY EXPENSIVE</b>. Do not use it "casually" but rather only in very concrete and exceptional
     * cases like severe error reporting.
     *
     * @return a human readable representation of the criteria object
     */
    public static String toString(Criteria criteria) {
        StringBuilder bld = new StringBuilder();

        bld.append(criteria.getClass().getSimpleName()).append("[");

        for(Criteria.Type type : Criteria.Type.values()) {
            switch (type) {
            case FETCH:
                bld.append("fetche");
                break;
            default:
                bld.append(type.name().toLowerCase());
            }
            bld.append("s: [");

            List<Field> fields = getFields(criteria, type);

            Collections.sort(fields, new Comparator<Field>() {
                @Override
                public int compare(Field o1, Field o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });

            boolean hasValues = false;
            for (Field f : fields) {
                boolean hasValue = false;
                try {
                    f.setAccessible(true);
                    Object value = f.get(criteria);

                    //don't show fetch fields that are not applied
                    if (type == Criteria.Type.FETCH && value instanceof Boolean && !(Boolean)value) {
                        value = null;
                    }

                    if (value != null) {
                        bld.append(f.getName()).append("=");
                        appendToString(value, bld);
                        hasValue = true;
                        hasValues = true;
                    }
                } catch (IllegalAccessException e) {
                    bld.append("<value-inaccessible>");
                    hasValue = true;
                    hasValues = true;
                }

                if (hasValue) {
                    bld.append(", ");
                }
            }

            if (hasValues) {
                bld.replace(bld.length() - 2, bld.length(), "");
            }

            bld.append("], ");
        }

        bld.replace(bld.length() - 2, bld.length(), "").append("]");
        return bld.toString();
    }

    private static void appendToString(Object object, StringBuilder bld) {
        if (object == null) {
            bld.append("null");
        } else if (object.getClass().isArray()) {
            Class<?> componentType = object.getClass().getComponentType();
            String str;
            if (componentType == boolean.class) {
                str = Arrays.toString((boolean[]) object);
            } else if (componentType == byte.class) {
                str = Arrays.toString((byte[]) object);
            } else if (componentType == char.class) {
                str = Arrays.toString((char[]) object);
            } else if (componentType == double.class) {
                str = Arrays.toString((double[]) object);
            } else if (componentType == float.class) {
                str = Arrays.toString((float[]) object);
            } else if (componentType == int.class) {
                str = Arrays.toString((int[]) object);
            } else if (componentType == long.class) {
                str = Arrays.toString((long[]) object);
            } else if (componentType == short.class) {
                str = Arrays.toString((short[]) object);
            } else {
                str = Arrays.deepToString((Object[]) object);
            }

            bld.append(str);
        } else if (object instanceof CharSequence) {
            bld.append("\"").append(object).append("\"");
        } else {
            bld.append(object.toString());
        }
    }

}
