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
package org.rhq.core.domain.criteria;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageOrdering;

/**
 * @author Joseph Marques
 */
public abstract class Criteria {
    private List<Field> filterFields;
    private List<Field> fetchFields;
    private List<Field> sortFields;
    private List<Field> overrideFields;

    protected Map<String, String> filterOverrides;
    protected Map<String, String> sortOverrides;

    private List<String> orderingFieldNames;

    public Criteria() {
        Field[] fields = this.getClass().getDeclaredFields();
        filterFields = new ArrayList<Field>();
        fetchFields = new ArrayList<Field>();
        sortFields = new ArrayList<Field>();
        overrideFields = new ArrayList<Field>();

        for (Field field : fields) {
            field.setAccessible(true);
            if (field.getName().startsWith("filter")) {
                filterFields.add(field);
            } else if (field.getName().startsWith("fetch")) {
                fetchFields.add(field);
            } else if (field.getName().startsWith("sort")) {
                sortFields.add(field);
            } else {
                overrideFields.add(field);
            }
        }

        filterOverrides = new HashMap<String, String>();
        sortOverrides = new HashMap<String, String>();

        orderingFieldNames = new ArrayList<String>();
    }

    public Map<String, Object> getFilterFields() {
        Map<String, Object> results = new HashMap<String, Object>();
        for (Field filterField : filterFields) {
            Object filterFieldValue = null;
            try {
                filterFieldValue = filterField.get(this);
            } catch (IllegalAccessException iae) {
                throw new RuntimeException(iae);
            }
            if (filterFieldValue != null) {
                results.put(getCleansedFieldName(filterField, 6), filterFieldValue);
            }
        }
        return results;
    }

    public String getJPQLFilterOverride(String fieldName) {
        return filterOverrides.get(fieldName);
    }

    public String getJPQLSortOverride(String fieldName) {
        return sortOverrides.get(fieldName);
    }

    public List<String> getFetchFields() {
        List<String> results = new ArrayList<String>();
        for (Field fetchField : fetchFields) {
            Object fetchFieldValue = null;
            try {
                fetchField.setAccessible(true);
                fetchFieldValue = fetchField.get(this);
            } catch (IllegalAccessException iae) {
                throw new RuntimeException(iae);
            }
            if (fetchFieldValue != null) {
                boolean shouldFetch = ((Boolean) fetchFieldValue).booleanValue();
                if (shouldFetch) {
                    results.add(getCleansedFieldName(fetchField, 5));
                }
            }
        }
        return results;
    }

    protected void addSortField(String fieldName) {
        orderingFieldNames.add("sort" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1));
    }

    public PageControl getPageControl() {
        PageControl pc = PageControl.getUnlimitedInstance();
        for (String fieldName : orderingFieldNames) {
            for (Field sortField : sortFields) {
                if (sortField.getName().equals(fieldName) == false) {
                    continue;
                }
                Object sortFieldValue = null;
                try {
                    sortFieldValue = sortField.get(this);
                } catch (IllegalAccessException iae) {
                    throw new RuntimeException(iae);
                }
                if (sortFieldValue != null) {
                    PageOrdering pageOrdering = (PageOrdering) sortFieldValue;
                    pc.addDefaultOrderingField(getCleansedFieldName(sortField, 4), pageOrdering);
                }
            }
        }
        return pc;
    }

    private String getCleansedFieldName(Field field, int leadingCharsToStrip) {
        String fieldNameFragment = field.getName().substring(leadingCharsToStrip);
        String fieldName = Character.toLowerCase(fieldNameFragment.charAt(0)) + fieldNameFragment.substring(1);
        return fieldName;
    }
}
