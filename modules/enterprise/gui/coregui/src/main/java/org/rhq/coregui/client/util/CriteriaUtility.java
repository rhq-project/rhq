/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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
package org.rhq.coregui.client.util;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.smartgwt.client.data.Criteria;

/**
 * A collection of utility methods for working with SmartGWT {@link com.smartgwt.client.data.Criteria}s.
 *
 * @author Ian Springer
 */
public class CriteriaUtility {

    private CriteriaUtility() {
        // static utility class only
    }

    public static String toString(Criteria criteria) {
        if (criteria == null) {
            return "null";
        }
        return "Criteria[" + criteria.getValues() + "]";
    }

    /**
     * Clone the specified criteria and return the clone.
     *
     * @param criteria the criteria to be cloned.
     *
     * @return the clone
     */
    public static Criteria clone(Criteria criteria) {
        if (criteria == null) {
            return null;
        }
        Criteria newCriteria = new Criteria();
        addCriteria(criteria, newCriteria);
        return newCriteria;
    }

    /**
     * Add the source criteria to the destination criteria.
     *
     * SmartGWT 2.4's {@link Criteria#addCriteria(com.smartgwt.client.data.Criteria)} for some reason doesn't have else
     * clauses for the array types, and it doesn't handle Object types properly (seeing odd behavior because of this),
     * so this method explicitly supports adding array types and Objects.
     *
     * @param target the Criteria to be added to
     * @param source the Criteria to be added from
     */
    public static void addCriteria(Criteria target, Criteria source) {
        Map sourceValueMap = source.getValues();
        Set sourceKeys = sourceValueMap.keySet();
        for (Object sourceKey : sourceKeys) {
            String field = (String) sourceKey;
            Object value = sourceValueMap.get(field);

            if (value instanceof Integer) {
                target.addCriteria(field, (Integer) value);
            } else if (value instanceof Float) {
                target.addCriteria(field, (Float) value);
            } else if (value instanceof String) {
                target.addCriteria(field, (String) value);
            } else if (value instanceof Date) {
                target.addCriteria(field, (Date) value);
            } else if (value instanceof Boolean) {
                target.addCriteria(field, (Boolean) value);
            } else if (value instanceof Integer[]) {
                target.addCriteria(field, (Integer[]) value);
            } else if (value instanceof Double[]) {
                target.addCriteria(field, (Double[]) value);
            } else if (value instanceof String[]) {
                target.addCriteria(field, (String[]) value);
            } else {
                // This is the magic piece - we need to get attribute as an object and set that value.
                target.setAttribute(field, source.getAttributeAsObject(field));
            }
        }
    }

    public static boolean equals(Criteria criteria1, Criteria criteria2) {
        if (criteria1 == criteria2) {
            return true;
        }
        if (criteria1 == null || criteria2 == null) {
            return false;
        }
        Set<String> attributes1 = new HashSet<String>(Arrays.asList(criteria1.getAttributes()));
        Set<String> attributes2 = new HashSet<String>(Arrays.asList(criteria2.getAttributes()));
        if (!attributes1.equals(attributes2)) {
            return false;
        }
        for (String attribute : attributes1) {
            String value1 = criteria1.getAttribute(attribute);
            String value2 = criteria2.getAttribute(attribute);
            if ((value1 == null && value2 != null) || (value1 != null && !value1.equals(value2))) {
                return false;
            }
        }
        return true;
    }

}
