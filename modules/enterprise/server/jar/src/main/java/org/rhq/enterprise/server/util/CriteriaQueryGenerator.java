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
package org.rhq.enterprise.server.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.annotations.IndexColumn;

import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.criteria.SubjectCriteria;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.util.OrderingField;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.domain.util.PersistenceUtility;

/**
 * A query generator used to generate queries with specific filtering, prefetching, or sorting requirements.
 * 
 * @author Joseph Marques
 */
public final class CriteriaQueryGenerator {

    private static final Log LOG = LogFactory.getLog(CriteriaQueryGenerator.class);

    public enum AuthorizationTokenType {
        RESOURCE, // specifies the resource alias to join on for standard res-group-role-subject authorization checking 
        GROUP; // specifies the group alias to join on for standard group-role-subject authorization checking
    }

    private Criteria criteria;

    private String authorizationJoinFragment;
    private String authorizationPermsFragment;
    private int authorizationSubjectId;

    private String alias;
    private String className;
    private static String NL = System.getProperty("line.separator");
    private static String ESCAPE_CHARACTER = null;

    private List<Field> persistentBagFields = new ArrayList<Field>();

    public CriteriaQueryGenerator(Criteria criteria) {
        this.criteria = criteria;

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

    private String fixFilterOverride(String expression, String fieldName) {
        boolean fuzzyMatch = expression.toLowerCase().indexOf(" like ") != -1;
        boolean wantCaseInsensitiveMatch = !criteria.isCaseSensitive() && fuzzyMatch;

        while (expression.indexOf('?') != -1) {
            String replacement = ":" + fieldName;
            expression = expression.replaceFirst("\\?", replacement);
        }

        if (wantCaseInsensitiveMatch) {
            int indexOfFirstSpace = expression.indexOf(" ");
            String filterToken = expression.substring(0, indexOfFirstSpace);
            expression = "LOWER( " + alias + "." + filterToken + " ) " + expression.substring(indexOfFirstSpace);
        } else {
            expression = alias + "." + expression;
        }

        if (fuzzyMatch) {
            expression += " ESCAPE '" + getEscapeCharacter() + "'";
        }

        return expression;
    }

    public void setAuthorizationResourceFragment(AuthorizationTokenType type, String fragment, int subjectId) {
        this.authorizationSubjectId = subjectId;
        if (type == AuthorizationTokenType.RESOURCE) {
            if (fragment == null) {
                this.authorizationJoinFragment = "" // 
                    + "JOIN " + alias + ".implicitGroups authGroup " + NL //
                    + "JOIN authGroup.roles authRole " + NL //
                    + "JOIN authRole.subjects authSubject " + NL;
            } else {
                this.authorizationJoinFragment = "" //
                    + "JOIN " + alias + "." + fragment + " authRes " + NL // 
                    + "JOIN authRes.implicitGroups authGroup " + NL //
                    + "JOIN authGroup.roles authRole " + NL //
                    + "JOIN authRole.subjects authSubject " + NL;
            }
        } else if (type == AuthorizationTokenType.GROUP) {
            if (fragment == null) {
                this.authorizationJoinFragment = "" // 
                    + "JOIN " + alias + ".roles authRole " + NL //
                    + "JOIN authRole.subjects authSubject " + NL;
            } else {
                this.authorizationJoinFragment = "" //
                    + "JOIN " + alias + "." + fragment + " authGroup " + NL //
                    + "JOIN authGroup.roles authRole " + NL //
                    + "JOIN authRole.subjects authSubject " + NL;
            }
        } else {
            throw new IllegalArgumentException(this.getClass().getSimpleName()
                + " does not yet support generating queries for '" + type + "' token types");
        }

        // If the query results are narrowed by requiredParams generate the fragment now. It's done
        // here for two reasons. First, it seems to make sense to apply this only when an authFragment is
        // being used.  Second, because ond day the query may be less brute force and may modify or
        // leverage the joinFragment above.  But, after extensive trying a more elegant
        // query could not be constructed due to Hibernate limitations. So, for now, here it is...
        List<Permission> requiredPerms = this.criteria.getRequiredPermissions();
        if (!(null == requiredPerms || requiredPerms.isEmpty())) {
            this.authorizationPermsFragment = "" //
                + "AND ( SELECT COUNT(DISTINCT p)" + NL //
                + "      FROM Subject innerSubject" + NL //
                + "      JOIN innerSubject.roles r" + NL //
                + "      JOIN r.permissions p" + NL //
                + "      WHERE innerSubject.id = " + this.authorizationSubjectId + NL //
                + "      AND p IN ( :requiredPerms ) ) = :requiredPermsSize" + NL;
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
                if (isPersistentBag(fetchJoin)) {
                    addPersistentBag(fetchJoin);
                } else {
                    results.append("LEFT JOIN FETCH ").append(alias).append('.').append(fetchJoin).append(NL);
                }
            }
        }
        if (authorizationJoinFragment != null) {
            results.append(authorizationJoinFragment);
        }

        Map<String, Object> filterFields = criteria.getFilterFields();
        if (filterFields.size() > 0 || authorizationJoinFragment != null) {
            results.append("WHERE ");
        }

        String conjunctiveFragment = criteria.isFiltersOptional() ? "OR " : "AND ";
        boolean wantCaseInsensitiveMatch = !criteria.isCaseSensitive();

        // criteria
        StringBuilder conjunctiveResults = new StringBuilder();
        boolean firstCrit = true;
        for (Map.Entry<String, Object> filterField : filterFields.entrySet()) {
            if (firstCrit) {
                firstCrit = false;
            } else {
                conjunctiveResults.append(NL).append(conjunctiveFragment);
            }
            String fieldName = filterField.getKey();
            String override = criteria.getJPQLFilterOverride(fieldName);
            String fragment = null;
            if (override != null) {
                fragment = fixFilterOverride(override, fieldName);
            } else {
                String operator = "=";
                if (filterField.getValue() instanceof String) {
                    operator = "like";
                    if (wantCaseInsensitiveMatch) {
                        fragment = "LOWER( " + alias + "." + fieldName + " ) " + operator + " :" + fieldName;
                    } else {
                        fragment = alias + "." + fieldName + " " + operator + " :" + fieldName;
                    }
                    fragment += " ESCAPE '" + getEscapeCharacter() + "'";
                } else {
                    fragment = alias + "." + fieldName + " " + operator + " :" + fieldName;
                }
            }

            conjunctiveResults.append(fragment).append(' ');
        }

        if (conjunctiveResults.length() > 0) {
            results.append("( ").append(conjunctiveResults).append(")");
        }

        // authorization
        if (authorizationJoinFragment != null) {
            if (firstCrit) {
                firstCrit = false;
            } else {
                // always want AND for security, regardless of conjunctiveFragment
                results.append(NL).append(" AND ");
            }
            results.append("authSubject.id = " + authorizationSubjectId + " ");
            if (null != this.authorizationPermsFragment) {
                results.append(this.authorizationPermsFragment + " ");
            }
        }

        if (countQuery == false) {
            boolean overridden = true;
            PageControl pc = criteria.getPageControlOverrides();
            if (pc == null) {
                overridden = false;
                pc = criteria.getPageControl();
            }

            boolean first = true;
            for (OrderingField orderingField : pc.getOrderingFields()) {
                if (first) {
                    results.append(NL).append("ORDER BY ");
                    first = false;
                } else {
                    results.append(", ");
                }

                if (overridden) {
                    String fieldName = orderingField.getField();
                    PageOrdering ordering = orderingField.getOrdering();

                    results.append(fieldName).append(' ').append(ordering);
                } else {
                    String fieldName = orderingField.getField();
                    String override = criteria.getJPQLSortOverride(fieldName);
                    String fragment = override != null ? override : fieldName;

                    results.append(alias).append('.').append(fragment);
                    results.append(' ').append(orderingField.getOrdering());
                }
            }
        }
        results.append(NL);

        LOG.info(results);
        return results.toString();
    }

    private boolean isPersistentBag(String fieldName) {
        try {
            Class<?> persistentClass = criteria.getPersistentClass();
            Field field = persistentClass.getDeclaredField(fieldName);

            return isAList(field) && !field.isAnnotationPresent(IndexColumn.class);
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    private boolean isAList(Field field) {
        Class<?> fieldType = field.getType();

        if (List.class.isAssignableFrom(fieldType)) {
            return true;
        }

        for (Class declaredInterface : fieldType.getInterfaces()) {
            if (List.class.isAssignableFrom(declaredInterface)) {
                return true;
            }
        }
        return false;
    }

    private void addPersistentBag(String fieldName) {
        try {
            Field field = criteria.getPersistentClass().getDeclaredField(fieldName);
            persistentBagFields.add(field);
        } catch (NoSuchFieldException e) {
            LOG.warn("Failed to add persistent bag collection.", e);
        }
    }

    /**
     * <strong>Note:</strong> This method should only be called after {@link #getQueryString(boolean)}} because it is
     * that method where the persistentBagFields property is initialized.
     *
     * @return Returns a list of fields from the persistent class to which the criteria class corresponds. The fields in
     * the list are themselves instances of List and have "bag" semantics.
     */
    public List<Field> getPersistentBagFields() {
        return persistentBagFields;
    }

    public Query getQuery(EntityManager em) {
        String queryString = getQueryString(false);
        Query query = em.createQuery(queryString);
        setBindValues(query, false);
        PersistenceUtility.setDataPage(query, criteria.getPageControl());
        return query;
    }

    public Query getCountQuery(EntityManager em) {
        String countQueryString = getQueryString(true);
        Query query = em.createQuery(countQueryString);
        setBindValues(query, false);
        return query;
    }

    private void setBindValues(Query query, boolean countQuery) {
        boolean wantCaseInsensitiveMatch = !criteria.isCaseSensitive();
        boolean wantsFuzzyMatching = !criteria.isStrict();
        boolean handleEscapedBackslash = "\\\\".equals(getEscapeCharacter());

        for (Map.Entry<String, Object> critField : criteria.getFilterFields().entrySet()) {
            Object value = critField.getValue();
            if (value instanceof String) {
                String formattedValue = (String) value;
                if (wantCaseInsensitiveMatch) {
                    formattedValue = formattedValue.toLowerCase();
                }
                /* 
                 * Double escape backslashes if they are not treated as string literals by the db vendor
                 * http://opensource.atlassian.com/projects/hibernate/browse/HHH-2674
                 */
                if (handleEscapedBackslash) {
                    formattedValue = ((String) formattedValue).replaceAll("\\_", "\\\\_");
                }
                if (wantsFuzzyMatching) {
                    // append '%' onto edges that don't already have '%' explicitly set from the caller
                    formattedValue = (formattedValue.startsWith("%") ? "" : "%") + formattedValue
                        + (formattedValue.endsWith("%") ? "" : "%");
                }
                value = formattedValue;
            }
            LOG.debug("Bind: (" + critField.getKey() + ", " + value + ")");
            query.setParameter(critField.getKey(), value);
        }
        if (null != this.authorizationPermsFragment) {
            List<Permission> requiredPerms = this.criteria.getRequiredPermissions();
            query.setParameter("requiredPerms", requiredPerms);
            query.setParameter("requiredPermsSize", (long) requiredPerms.size());
        }
    }

    public static String getEscapeCharacter() {
        if (null == ESCAPE_CHARACTER) {
            ESCAPE_CHARACTER = DatabaseTypeFactory.getDefaultDatabaseType().getEscapeCharacter();
        }

        return ESCAPE_CHARACTER;
    }

    public static void main(String[] args) {
        //testSubjectCriteria();
        //testAlertCriteria();
        //testInheritanceCriteria();
        testResourceCriteria();
    }

    public static void testSubjectCriteria() {
        SubjectCriteria subjectCriteria = new SubjectCriteria();
        subjectCriteria.addFilterFirstName("joe");
        subjectCriteria.addFilterFactive(true);
        subjectCriteria.fetchRoles(true);
        subjectCriteria.addSortName(PageOrdering.ASC);

        CriteriaQueryGenerator subjectGenerator = new CriteriaQueryGenerator(subjectCriteria);
        System.out.println(subjectGenerator.getQueryString(false));
        System.out.println(subjectGenerator.getQueryString(true));
    }

    public static void testAlertCriteria() {
        AlertCriteria alertCriteria = new AlertCriteria();
        alertCriteria.addFilterName("joe");
        alertCriteria.addFilterDescription("query generation is cool");
        alertCriteria.addFilterStartTime(42L);
        alertCriteria.addFilterEndTime(100L);
        alertCriteria.addFilterResourceIds(1, 2, 3);
        alertCriteria.fetchAlertDefinition(true);
        alertCriteria.addSortPriority(PageOrdering.DESC);
        alertCriteria.addSortName(PageOrdering.ASC);
        alertCriteria.setPaging(0, 100);
        alertCriteria.setFiltersOptional(true);
        //alertCriteria.setCaseSensitive(false);

        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(alertCriteria);
        System.out.println(generator.getQueryString(false));
        System.out.println(generator.getQueryString(true));

        generator.setAuthorizationResourceFragment(AuthorizationTokenType.RESOURCE, "definition.resource", 1);
        System.out.println(generator.getQueryString(false));
        System.out.println(generator.getQueryString(true));
    }

    public static void testInheritanceCriteria() {
        ResourceOperationHistoryCriteria historyCriteria = new ResourceOperationHistoryCriteria();
        historyCriteria.addFilterResourceIds(1);
        historyCriteria.addFilterStatus(OperationRequestStatus.FAILURE);

        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(historyCriteria);
        System.out.println(generator.getQueryString(false));
        System.out.println(generator.getQueryString(true));
    }

    public static void testResourceCriteria() {
        ResourceCriteria resourceCriteria = new ResourceCriteria();
        resourceCriteria.addFilterResourceCategory(ResourceCategory.SERVER);
        resourceCriteria.addFilterName("marques");
        resourceCriteria.fetchAgent(true);
        resourceCriteria.addSortResourceTypeName(PageOrdering.ASC);
        resourceCriteria.setCaseSensitive(true);
        resourceCriteria.setFiltersOptional(true);

        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(resourceCriteria);
        generator.getQueryString(false);
        generator.getQueryString(true);
    }
}