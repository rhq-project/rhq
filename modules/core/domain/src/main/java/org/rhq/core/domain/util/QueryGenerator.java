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
package org.rhq.core.domain.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Role;

/**
 * A small proof-of-concept to show one possible method of how someone might expose an abstract strategy
 * for generating queries with specific fetch join or sorting requirements.  Implementors extend QueryBase
 * and add a mapping for it to the generator.  It is intended for QueryBase to be relatively self-documenting.
 * 
 * @author Asaf Shakarchi
 * @author Joseph Marques
 */
public class QueryGenerator {

    public static Map<String, QueryBase> mappings = new HashMap<String, QueryBase>();
    static {
        mappings.put(Subject.class.getSimpleName(), new SubjectQuery());
        mappings.put(Role.class.getSimpleName(), new RoleQuery());
    }

    public static String buildDynamicQuery(Class<?> clazz, String[] fetchJoins, PageControl pc) {
        String className = clazz.getSimpleName();
        QueryBase base = mappings.get(className);
        if (base == null) {
            throw new IllegalArgumentException("Can not generate query for unsupported type '" + className + "'.");
        }
        String queryResult = base.getQuery(fetchJoins, pc.getOrderingFieldsAsArray());
        return queryResult;
    }

    public static void main(String[] args) {
        PageControl subjectPageControl = PageControl.getUnlimitedInstance();
        subjectPageControl.addDefaultOrderingField("firstName", PageOrdering.ASC);
        subjectPageControl.addDefaultOrderingField("lastName", PageOrdering.DESC);

        String[] subjectFetchFields = new String[] { "roles", "configuration" };

        String simpleSubjectQuery = QueryGenerator.buildDynamicQuery(Subject.class, subjectFetchFields,
            subjectPageControl);
        System.out.println(simpleSubjectQuery);

        // -----

        PageControl rolePageControl = PageControl.getUnlimitedInstance();
        rolePageControl.addDefaultOrderingField("name", PageOrdering.ASC);
        rolePageControl.addDefaultOrderingField("id", PageOrdering.ASC);

        String[] roleFetchFields = new String[] { "subjects", "resourceGroups" };

        String roleSubjectQuery = QueryGenerator.buildDynamicQuery(Role.class, roleFetchFields, rolePageControl);
        System.out.println(roleSubjectQuery);
    }
}

abstract class QueryBase {

    private static String NL = System.getProperty("line.separator");

    protected String alias;
    protected String className;

    protected Set<String> canOrderBy;
    protected Set<String> canJoinOn;

    protected QueryBase(Class<?> criteriaClass, String[] canJoinOn, String[] canOrderBy) {
        this.className = criteriaClass.getSimpleName();
        StringBuilder aliasBuilder = new StringBuilder();
        for (char c : this.className.toCharArray()) {
            if (Character.isUpperCase(c)) {
                aliasBuilder.append(Character.toLowerCase(c));
            }
        }
        this.alias = aliasBuilder.toString();
        this.canJoinOn = new HashSet<String>(Arrays.asList(canJoinOn));
        this.canOrderBy = new HashSet<String>(Arrays.asList(canOrderBy));
    }

    protected String getQuery(String[] fetchFieldNames, OrderingField[] orderingFields) {

        for (String name : fetchFieldNames) {
            // TODO: handle case-sensitivity properly
            if (!canJoinOn.contains(name)) {
                throw new IllegalArgumentException("Can not fetchJoin '" + name + "'.  Valid values are: " + canJoinOn);
            }
        }

        for (OrderingField field : orderingFields) {
            // TODO: handle case-sensitivity properly
            String name = field.getField();
            if (!canOrderBy.contains(name)) {
                throw new IllegalArgumentException("Can not orderBy '" + name + "'.  Valid values are: " + canOrderBy);
            }
        }

        StringBuilder results = new StringBuilder();
        results.append("SELECT ").append(alias).append(NL);
        results.append("FROM ").append(className).append(' ').append(alias).append(NL);
        for (String fetchJoin : fetchFieldNames) {
            results.append("LEFT JOIN FETCH ").append(alias).append('.').append(fetchJoin).append(NL);
        }

        boolean first = true;
        for (OrderingField orderingField : orderingFields) {
            if (first) {
                results.append("ORDER BY ");
                first = false;
            } else {
                results.append(", ");
            }
            results.append(alias).append('.').append(orderingField.getField());
            results.append(' ').append(orderingField.getOrdering());
        }

        return results.append(NL).toString();
    }
}

class SubjectQuery extends QueryBase {
    public SubjectQuery() {
        super(Subject.class, //
            new String[] { "configuration", "roles", "subjectNotifications" }, //
            new String[] { "id", "name", "firstName", "lastName", "emailAddress", "smsAddress", "phoneNumber",
                "department" });
    }
}

class RoleQuery extends QueryBase {
    public RoleQuery() {
        super(Role.class, //
            new String[] { "subjects", "resourceGroups", "permissions", "roleNotifications" }, //
            new String[] { "id", "name", "description", "firstName" });
    }
}