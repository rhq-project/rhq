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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.criteria.SubjectCriteria;

/**
 * A query generator used to generate queries with specific filtering, prefetching, or sorting requirements.
 * 
 * @author Joseph Marques
 */
public final class CriteriaQueryGenerator {
    public enum AuthorizationTokenType {
        RESOURCE, // specifies the resource alias to join on for standard res-group-role-subject authorization checking 
        GROUP; // specifies the group alias to join on for standard group-role-subject authorization checking
    }

    private static final String argPrefix = ":arg";
    private int argumentCounter = 0;
    private List<String> filterExpressions = new ArrayList<String>();
    private List<Object> filterValues = new ArrayList<Object>();

    private static final String relationshipPrefix = "rel";
    private int relationshipCounter = 0;
    private List<String> joinExpressions = new ArrayList<String>();

    private Criteria criteria;
    private PageControl pageControl;

    private String authorizationJoinFragment;
    private int authorizationSubjectId;

    private String alias;
    private String className;
    private static String NL = System.getProperty("line.separator");

    public CriteriaQueryGenerator(Criteria criteria, PageControl pageControl) {
        this.criteria = criteria;

        if (pageControl == null) {
            this.pageControl = PageControl.getUnlimitedInstance();
        } else {
            this.pageControl = pageControl;
        }

        String criteriaClassName = criteria.getClass().getSimpleName();
        className = criteriaClassName.substring(0, criteriaClassName.length() - 8);

        StringBuilder aliasBuilder = new StringBuilder();
        for (char c : this.className.toCharArray()) {
            if (Character.isUpperCase(c)) {
                aliasBuilder.append(Character.toLowerCase(c));
            }
        }
        this.alias = aliasBuilder.toString();
    }

    public void setAuthorizationResourceFragment(AuthorizationTokenType type, int subjectId) {
        String defaultFragment = null;
        if (type == AuthorizationTokenType.RESOURCE) {
            defaultFragment = "resource";
        } else if (type == AuthorizationTokenType.GROUP) {
            defaultFragment = "group";
        }
        setAuthorizationResourceFragment(type, defaultFragment, subjectId);
    }

    /**
     * Supports and arbitrarily style for adding more WHERE conditions. Use a question mark as a 
     * place holder for where you want values to be inserted.  You should include as many values
     * as you do question marks in the expression.  For example, if your criteria object contained
     * a field called 'grams', the call to this method might look like:
     * 
     *    addFilter("grams between ? and ?", 100, 1000); 
     * 
     * @param propertyExpression an expression contained question marks that should be parameter
     *                           replaced during query generation time
     * @param values             a list of objects that should replace the question marks in the
     *                           propertyExpression during query generation
     */
    public void addFilter(String expression, Object... values) {
        addFilterWithAlias(this.alias, expression, values);
    }

    /**
     * Supports and arbitrarily style for adding related JOIN and WHERE conditions. The relationship
     * is what you want to join on, and the expression and value arguments follow the semantics laid
     * out in {@link CriteriaQueryGenerator#addFilter(String, Object...)}.  For example, if you criteria
     * object contained a relationship called 'children', and if each of the child objects contained
     * a field called 'height', the call to this method might look like:
     * 
     *     addRelationshipFilter("children", "height between ? and ?", 42, 111);
     */
    public void addRelationshipFilter(String relationship, String expression, Object... values) {
        String relationshipName = relationshipPrefix + String.valueOf(relationshipCounter++);
        joinExpressions.add(this.alias + "." + relationship + " " + relationshipName);
        addFilterWithAlias(relationshipName, expression, values);
    }

    private void addFilterWithAlias(String alias, String expression, Object... values) {
        int argumentsFound = 0;
        while (expression.indexOf('?') != -1) {
            expression = expression.replaceFirst("\\?", argPrefix + String.valueOf(argumentCounter++));
            argumentsFound++;
        }
        if (argumentsFound != values.length) {
            throw new IllegalArgumentException("Passed an expression with " + argumentsFound
                + " placeholders, but provided " + values.length + " arguments");
        }

        filterExpressions.add(alias + "." + expression);
        filterValues.addAll(Arrays.asList(values));
    }

    public void setAuthorizationResourceFragment(AuthorizationTokenType type, String fragment, int subjectId) {
        this.authorizationSubjectId = subjectId;
        if (type == AuthorizationTokenType.RESOURCE) {
            if (fragment == null) {
                this.authorizationJoinFragment = "" // 
                    + "JOIN " + alias + ".implicitGroup authGroup " + NL //
                    + "JOIN authGroup.roles authRole " + NL //
                    + "JOIN authRole.subject authSubject " + NL;
            } else {
                this.authorizationJoinFragment = "" //
                    + "JOIN " + alias + "." + fragment + " authRes " + NL // 
                    + "JOIN authRes.implicitGroup authGroup " + NL //
                    + "JOIN authGroup.roles authRole " + NL //
                    + "JOIN authRole.subject authSubject " + NL;
            }
        } else if (type == AuthorizationTokenType.GROUP) {
            if (fragment == null) {
                this.authorizationJoinFragment = "" // 
                    + "JOIN " + alias + ".roles authRole " + NL //
                    + "JOIN authRole.subject authSubject " + NL;
            } else {
                this.authorizationJoinFragment = "" //
                    + "JOIN " + alias + "." + fragment + " authGroup " + NL //
                    + "JOIN authGroup.roles authRole " + NL //
                    + "JOIN authRole.subject authSubject " + NL;
            }
        } else {
            throw new IllegalArgumentException(this.getClass().getSimpleName()
                + " does not yet support generating queries for '" + type + "' token types");
        }
    }

    // for testing purposes only, should use getQuery(EntityManager) or getCountQuery(EntityManager) instead
    public String getQueryString(boolean countQuery) {
        StringBuilder results = new StringBuilder();
        results.append("SELECT ");
        if (countQuery) {
            results.append("COUNT(").append(alias).append(")").append(NL);
        } else {
            results.append(alias).append(NL);
        }
        results.append("FROM ").append(className).append(' ').append(alias).append(NL);
        if (countQuery == false) {
            /* 
             * don't fetch in the count query to avoid: "query specified join fetching, 
             * but the owner of the fetched association was not present in the select list"
             */
            for (String fetchJoin : criteria.getFetchFields()) {
                results.append("LEFT JOIN FETCH ").append(alias).append('.').append(fetchJoin).append(NL);
            }
        }
        if (authorizationJoinFragment != null) {
            results.append(authorizationJoinFragment);
        }
        for (String customJoinFragment : joinExpressions) {
            results.append("JOIN " + customJoinFragment).append(NL);
        }

        Map<String, Object> filterFields = criteria.getFilterFields();
        if (filterFields.size() > 0 || filterExpressions.size() > 0 || authorizationJoinFragment != null) {
            results.append("WHERE ");
        }

        // criteria
        boolean firstCrit = true;
        for (Map.Entry<String, Object> filterField : filterFields.entrySet()) {
            if (firstCrit) {
                firstCrit = false;
            } else {
                results.append(NL).append("AND ");
            }
            String fieldName = filterField.getKey();
            String override = criteria.getJPQLFilterOverride(fieldName);
            String fragment = null;
            if (override != null) {
                fragment = override.replaceFirst("\\?", ":" + fieldName);
            } else {
                String operator = "=";
                if (filterField.getValue() instanceof String) {
                    operator = "like";
                }
                fragment = fieldName + " " + operator + " :" + fieldName;
            }

            results.append(alias).append('.').append(fragment + " ");
        }

        // custom filters
        for (String filter : filterExpressions) {
            if (firstCrit) {
                firstCrit = false;
            } else {
                results.append(NL).append("AND ");
            }
            results.append(filter);
        }

        // authorization
        if (authorizationJoinFragment != null) {
            if (firstCrit) {
                firstCrit = false;
            } else {
                results.append(NL).append("AND ");
            }
            results.append("authSubject.id = " + authorizationSubjectId + " ");
        }

        if (countQuery == false) {
            boolean first = true;
            for (OrderingField orderingField : criteria.getPageControl().getOrderingFields()) {
                if (first) {
                    results.append(NL).append("ORDER BY ");
                    first = false;
                } else {
                    results.append(", ");
                }
                String fieldName = orderingField.getField();
                String override = criteria.getJPQLSortOverride(fieldName);
                String fragment = override != null ? override : fieldName;

                results.append(alias).append('.').append(fragment);
                results.append(' ').append(orderingField.getOrdering());
            }
        }

        return results.append(NL).toString();
    }

    public Query getQuery(EntityManager em) {
        String queryString = getQueryString(false);
        Query query = em.createQuery(queryString);
        setBindValues(query, false);
        PersistenceUtility.setDataPage(query, pageControl);
        return query;
    }

    public Query getCountQuery(EntityManager em) {
        String countQueryString = getQueryString(true);
        Query query = em.createQuery(countQueryString);
        setBindValues(query, false);
        return query;
    }

    private void setBindValues(Query query, boolean countQuery) {
        for (Map.Entry<String, Object> critField : criteria.getFilterFields().entrySet()) {
            query.setParameter(critField.getKey(), critField.getValue());
        }
        for (int i = 0; i < filterValues.size(); i++) {
            String argumentName = argPrefix + i;
            Object value = filterValues.get(i);
            if (value instanceof String) {
                value = PersistenceUtility.formatSearchParameter((String) value);
            }
            query.setParameter(argumentName, value);
        }
    }

    public static void main(String[] args) {
        testSubjectCriteria();
        testAlertCriteria();
    }

    public static void testSubjectCriteria() {
        PageControl pc = PageControl.getUnlimitedInstance();
        SubjectCriteria subjectCriteria = new SubjectCriteria();
        subjectCriteria.setFilterFirstName("joe");
        subjectCriteria.setFilterFactive(true);
        subjectCriteria.setFetchRoles(true);
        subjectCriteria.setSortName(PageOrdering.ASC);

        CriteriaQueryGenerator subjectGenerator = new CriteriaQueryGenerator(subjectCriteria, pc);
        System.out.println(subjectGenerator.getQueryString(false));
        System.out.println(subjectGenerator.getQueryString(true));
    }

    public static void testAlertCriteria() {
        PageControl pc = PageControl.getUnlimitedInstance();
        AlertCriteria alertCriteria = new AlertCriteria();
        alertCriteria.setFilterName("joe");
        alertCriteria.setFilterResourceIds(Arrays.asList(1, 2, 3));
        alertCriteria.setFetchAlertDefinition(true);
        alertCriteria.setSortPriority(PageOrdering.DESC);
        alertCriteria.setSortName(PageOrdering.ASC);

        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(alertCriteria, pc);
        System.out.println(generator.getQueryString(false));
        System.out.println(generator.getQueryString(true));

        generator.setAuthorizationResourceFragment(AuthorizationTokenType.RESOURCE, "definition.resource", 1);
        System.out.println(generator.getQueryString(false));
        System.out.println(generator.getQueryString(true));
    }
}