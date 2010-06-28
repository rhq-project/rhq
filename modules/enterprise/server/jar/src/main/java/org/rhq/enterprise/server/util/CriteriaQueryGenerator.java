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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.annotations.IndexColumn;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.criteria.SubjectCriteria;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.search.SearchSubsystem;
import org.rhq.core.domain.tagging.Tag;
import org.rhq.core.domain.util.OrderingField;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.server.PersistenceUtility;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.server.search.execution.SearchTranslationManager;

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
    private String searchExpressionWhereClause;

    private String authorizationJoinFragment;
    private String authorizationPermsFragment;
    private String authorizationCustomConditionFragment;
    private int authorizationSubjectId;

    private String alias;
    private String className;
    private String projection;
    private String groupByClause;
    private String havingClause;
    private static String NL = System.getProperty("line.separator");

    private static List<String> EXPRESSION_START_KEYWORDS;

    private List<Field> persistentBagFields = new ArrayList<Field>();

    static {
        EXPRESSION_START_KEYWORDS = new ArrayList<String>(2);
        EXPRESSION_START_KEYWORDS.add("NOT");
        EXPRESSION_START_KEYWORDS.add("EXISTS");
    }

    public CriteriaQueryGenerator(Criteria criteria) {
        this.criteria = criteria;
        this.className = criteria.getPersistentClass().getSimpleName();
        this.alias = this.criteria.getAlias();

        initializeJPQLFragmentFromSearchExpression();
    }

    public void setAuthorizationCustomConditionFragment(String fragment) {
        this.authorizationCustomConditionFragment = fragment;
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
        boolean fuzzyMatch = expression.toLowerCase().contains(" like ")
            && !expression.toLowerCase().contains("select"); // Don't fuzzy match subselects
        boolean wantCaseInsensitiveMatch = !criteria.isCaseSensitive() && fuzzyMatch;

        while (expression.indexOf('?') != -1) {
            String replacement = ":" + fieldName;
            expression = expression.replaceFirst("\\?", replacement);
        }

        // if the override expression does not follow the usual format of ( field operator expression )
        // then don't prepend the alias or deal with other special handling. The override must be left
        // explicit.
        if (!expressionStartsWithKeyword(expression)) {
            if (wantCaseInsensitiveMatch) {
                int indexOfFirstSpace = expression.indexOf(" ");
                String filterToken = expression.substring(0, indexOfFirstSpace);
                expression = "LOWER( " + alias + "." + filterToken + " ) " + expression.substring(indexOfFirstSpace);
            } else {
                expression = alias + "." + expression;
            }
        }

        if (fuzzyMatch) {
            expression += QueryUtility.getEscapeClause();
        }

        return expression;
    }

    private boolean expressionStartsWithKeyword(String expression) {
        expression = expression.trim();
        int i = expression.trim().indexOf(" ");
        String startToken = expression.substring(0, i);
        return EXPRESSION_START_KEYWORDS.contains(startToken.toUpperCase());
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

    public String getParameterReplacedQuery(boolean countQuery) {
        String query = getQueryString(countQuery);

        for (Map.Entry<String, Object> critField : getFilterFields(criteria).entrySet()) {
            Object value = critField.getValue();

            if (value instanceof Tag) {
                Tag tag = (Tag) value;
                query = query.replace(":tagNamespace", tag.getNamespace());
                query = query.replace(":tagSemantic", tag.getSemantic());
                query = query.replace(":tagName", tag.getName());

            } else {
                value = getParameterReplacedValue(value);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Bind: (" + critField.getKey() + ", " + value + ")");
                }
                query = query.replace(":" + critField.getKey(), String.valueOf(value));
            }
        }

        if (null != this.authorizationPermsFragment) {
            List<Permission> requiredPerms = this.criteria.getRequiredPermissions();
            String perms = requiredPerms.toString(); // [data1, data, data3]
            query = query.replace(":requiredPerms", perms.subSequence(1, perms.length() - 1)); // remove first/last characters
            query = query.replace(":requiredPermsSize", String.valueOf(requiredPerms.size()));
        }

        return query;
    }

    private String getParameterReplacedValue(Object value) {
        String returnValue = null;
        if (value instanceof String) {
            returnValue = "'" + prepareStringBindValue((String) value) + "'";
        } else if (value instanceof Enum) {
            // note: this strategy won't work for entities with multiple enums that are persisted differently
            EnumType type = getPersistenceEnumType(value.getClass());
            if (type == EnumType.STRING) {
                returnValue = "'" + String.valueOf(value) + "'";
            } else {
                returnValue = String.valueOf(value);
            }
        } else if (value instanceof List<?>) {
            List<?> valueList = (List<?>) value;
            StringBuilder results = new StringBuilder();
            boolean first = true;
            for (Object nextValue : valueList) {
                if (first) {
                    first = false;
                } else {
                    results.append(",");
                }
                results.append(getParameterReplacedValue(nextValue));
            }
            returnValue = results.toString();
        } else {
            returnValue = String.valueOf(value);
        }
        return returnValue;
    }

    // calculates @Enumerated(EnumType.STRING) or @Enumerated(EnumType.ORDINAL)
    private EnumType getPersistenceEnumType(Class<?> enumFieldType) {
        for (Field nextField : getClass().getFields()) {
            nextField.setAccessible(true);
            if (nextField.getType().equals(enumFieldType)) {
                Enumerated enumeratedAnnotation = nextField.getAnnotation(Enumerated.class);
                if (enumeratedAnnotation != null) {
                    return enumeratedAnnotation.value();
                }
            }
        }
        return EnumType.STRING; // catch-all
    }

    // for testing purposes only, should use getQuery(EntityManager) or getCountQuery(EntityManager) instead
    public String getQueryString(boolean countQuery) {
        StringBuilder results = new StringBuilder();
        results.append("SELECT ");
        if (countQuery) {
            results.append("COUNT(").append(alias).append(")").append(NL);
        } else {
            if (projection == null) {
                results.append(alias).append(NL);
            } else {
                results.append(projection).append(NL);
            }
        }
        results.append("FROM ").append(className).append(' ').append(alias).append(NL);
        if (countQuery == false) {
            /* 
             * don't fetch in the count query to avoid: "query specified join fetching, 
             * but the owner of the fetched association was not present in the select list"
             */
            for (String fetchJoin : getFetchFields(criteria)) {
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

        Map<String, Object> filterFields = getFilterFields(criteria);
        if (filterFields.size() > 0 || authorizationJoinFragment != null || searchExpressionWhereClause != null) {
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
                    fragment += QueryUtility.getEscapeClause();
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

        if (authorizationCustomConditionFragment != null) {
            if (firstCrit) {
                firstCrit = false;
            } else {
                // always want AND for security, regardless of conjunctiveFragment
                results.append(NL).append(" AND ");
            }
            results.append(this.authorizationCustomConditionFragment);
        }

        if (searchExpressionWhereClause != null) {
            if (firstCrit) {
                firstCrit = false;
            } else {
                // always want to additionally filter by translated from the RHQL search expression
                results.append(NL).append(" AND ");
            }
            results.append(searchExpressionWhereClause);
        }

        if (countQuery == false) {
            // group by clause
            if (groupByClause != null) {
                results.append(NL).append("GROUP BY ").append(groupByClause);
            }

            // having clause
            if (havingClause != null) {
                results.append(NL).append("HAVING ").append(havingClause);
            }

            // order by clause
            boolean overridden = true;
            PageControl pc = criteria.getPageControlOverrides();
            if (pc == null) {
                overridden = false;
                pc = getPageControl(criteria);
            }

            boolean first = true;
            for (OrderingField orderingField : pc.getOrderingFields()) {
                if (first) {
                    results.append(NL).append("ORDER BY ");
                    first = false;
                } else {
                    results.append(", ");
                }

                String fieldName = orderingField.getField();
                String override = criteria.getJPQLSortOverride(fieldName);
                String suffix = (override == null) ? fieldName : override;

                /*
                 * do not prefix the alias when:
                 * 
                 *    1) if the suffix is numerical, which allows usto sort by column ordinal
                 *    2) if the user wants full control and has explicitly chosen to disable alias prepending
                 */
                boolean doNotPrefixAlias = isNumber(suffix) || criteria.hasCustomizedSorting();
                String sortFragment = doNotPrefixAlias ? suffix : (alias + "." + suffix);

                PageOrdering ordering = orderingField.getOrdering();

                results.append(sortFragment).append(' ').append(ordering);
            }
        }
        results.append(NL);

        LOG.debug(results);
        return results.toString();
    }

    private boolean isNumber(String input) {
        if (input == null) {
            return false;
        }
        for (char next : input.toCharArray()) {
            if (Character.isDigit(next) == false) {
                return false;
            }
        }
        return true;
    }

    public List<String> getFetchFields(Criteria criteria) {
        List<String> results = new ArrayList<String>();
        for (Field fetchField : getFields(criteria, Criteria.Type.FETCH)) {
            Object fetchFieldValue = null;
            try {
                fetchField.setAccessible(true);
                fetchFieldValue = fetchField.get(criteria);
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
        //        for (String entry : results) {
        //            LOG.info("Fetch: (" + entry + ")");
        //        }
        return results;
    }

    private static List<Field> getFields(Criteria criteria, Criteria.Type fieldType) {
        String prefix = fieldType.name().toLowerCase();
        List<Field> results = new ArrayList<Field>();

        Class<?> currentLevelClass = criteria.getClass();
        while (currentLevelClass.equals(Criteria.class) == false) {
            for (Field field : currentLevelClass.getDeclaredFields()) {
                field.setAccessible(true);
                if (field.getName().startsWith(prefix)) {
                    results.add(field);
                }
            }
            currentLevelClass = currentLevelClass.getSuperclass();
        }

        return results;
    }

    public static String getCleansedFieldName(Field field, int leadingCharsToStrip) {
        String fieldNameFragment = field.getName().substring(leadingCharsToStrip);
        String fieldName = Character.toLowerCase(fieldNameFragment.charAt(0)) + fieldNameFragment.substring(1);
        return fieldName;
    }

    public Map<String, Object> getFilterFields(Criteria criteria) {
        Map<String, Object> results = new HashMap<String, Object>();
        for (Field filterField : getFields(criteria, Criteria.Type.FILTER)) {
            Object filterFieldValue = null;
            try {
                filterFieldValue = filterField.get(criteria);
            } catch (IllegalAccessException iae) {
                throw new RuntimeException(iae);
            }
            if (filterFieldValue != null) {
                results.put(getCleansedFieldName(filterField, 6), filterFieldValue);
            }
        }
        //        for (Map.Entry<String, Object> entries : results.entrySet()) {
        //            LOG.info("Filter: (" + entries.getKey() + ", " + entries.getValue() + ")");
        //        }
        return results;
    }

    private void initializeJPQLFragmentFromSearchExpression() {
        String searchExpression = criteria.getSearchExpression();
        if (searchExpression == null) {
            return;
        }

        try {
            Class<?> entityClass = criteria.getPersistentClass();
            SearchTranslationManager searchManager = new SearchTranslationManager(SearchSubsystem.get(entityClass));
            searchManager.setExpression(searchExpression);

            // translate first, if there was an error we won't add the dangling 'AND' to the where clause
            String translatedJPQL = searchManager.getJPQLWhereFragment();
            LOG.debug("Translated JPQL Fragment was: " + translatedJPQL);
            if (translatedJPQL != null) {
                searchExpressionWhereClause = translatedJPQL;
            }
        } catch (Exception e) {
            LOG.error("Could not get JPQL translation for '" + searchExpression + "': "
                + ThrowableUtil.getAllMessages(e, true));
        }
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

        for (Class<?> declaredInterface : fieldType.getInterfaces()) {
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
     *         the list are themselves instances of List and have "bag" semantics.
     */
    public List<Field> getPersistentBagFields() {
        return persistentBagFields;
    }

    /**
     * If you want to return something other than the list of entities represented by the passed Criteria object,
     * you can alter the projection here to return a customized subset or superset of data.  The projection will
     * only affect the ResultSet for the data query, not the count query.
     * <p/>
     * If you are projecting a composite object that does not directly extend the entity your Criteria object
     * represents, then you will need to manually initialize the persistent bags using the methods exposed on
     * {@link CriteriaQueryRunner}
     */
    public void alterProjection(String projection) {
        this.projection = projection;
    }

    /**
     * The groupBy clause can be set if and only if the projection is altered.  The passed argument should not be
     * prefixed with 'group by'; that part of the query will be auto-generated if the argument is non-null.  The 
     * new projection must follow standard rules as they apply to statements with groupBy clauses.
     */
    public void setGroupByClause(String groupByClause) {
        if (groupByClause != null && projection == null) {
            throw new IllegalArgumentException("Must alter projection before calling setGroupByClause");
        }
        this.groupByClause = groupByClause;
    }

    /**
     * The having clause can be set if and only if the groupBy clause is set.  The passed argument should not be
     * prefixed with 'having'; that part of the query will be auto-generated if the argument is non-null.  The 
     * having clause must follow standard rules as they apply to statements with groupBy clauses.
     */
    public void setHavingClause(String havingClause) {
        if (havingClause != null && groupByClause == null) {
            throw new IllegalArgumentException("Must add some groupBy clause before calling setHavingClause");
        }
        this.havingClause = havingClause;
    }

    public Query getQuery(EntityManager em) {
        String queryString = getQueryString(false);
        Query query = em.createQuery(queryString);
        setBindValues(query, false);
        PersistenceUtility.setDataPage(query, getPageControl(criteria));
        return query;
    }

    public Query getCountQuery(EntityManager em) {
        String countQueryString = getQueryString(true);
        Query query = em.createQuery(countQueryString);
        setBindValues(query, false);
        return query;
    }

    private void setBindValues(Query query, boolean countQuery) {
        for (Map.Entry<String, Object> critField : getFilterFields(criteria).entrySet()) {
            Object value = critField.getValue();

            if (value instanceof Tag) {
                Tag tag = (Tag) value;
                query.setParameter("tagNamespace", tag.getNamespace());
                query.setParameter("tagSemantic", tag.getSemantic());
                query.setParameter("tagName", tag.getName());

            } else {
                if (value instanceof String) {
                    value = prepareStringBindValue((String) value);
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Bind: (" + critField.getKey() + ", " + value + ")");
                }
                query.setParameter(critField.getKey(), value);
            }
        }
        if (null != this.authorizationPermsFragment) {
            List<Permission> requiredPerms = this.criteria.getRequiredPermissions();
            query.setParameter("requiredPerms", requiredPerms);
            query.setParameter("requiredPermsSize", (long) requiredPerms.size());
        }
    }

    private String prepareStringBindValue(String value) {
        if (!criteria.isStrict()) {
            value = "%" + QueryUtility.escapeSearchParameter(value) + "%";
        }

        if (!criteria.isCaseSensitive()) {
            value = value.toLowerCase();
        }

        return value;
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

    public static PageControl getPageControl(Criteria criteria) {
        PageControl pc = null;

        if (criteria.getPageControlOverrides() != null) {
            pc = criteria.getPageControlOverrides();
        } else {
            if (criteria.getPageNumber() == null || criteria.getPageSize() == null) {
                pc = PageControl.getUnlimitedInstance();
            } else {
                pc = new PageControl(criteria.getPageNumber(), criteria.getPageSize());
            }
            for (String fieldName : criteria.getOrderingFieldNames()) {
                for (Field sortField : getFields(criteria, Criteria.Type.SORT)) {
                    if (sortField.getName().equals(fieldName) == false) {
                        continue;
                    }
                    Object sortFieldValue = null;
                    try {
                        sortFieldValue = sortField.get(criteria);
                    } catch (IllegalAccessException iae) {
                        throw new RuntimeException(iae);
                    }
                    if (sortFieldValue != null) {
                        PageOrdering pageOrdering = (PageOrdering) sortFieldValue;
                        pc.addDefaultOrderingField(getCleansedFieldName(sortField, 4), pageOrdering);
                    }
                }
            }
        }
        return pc;
    }
}