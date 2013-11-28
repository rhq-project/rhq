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

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.criteria.SubjectCriteria;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.search.SearchSubsystem;
import org.rhq.core.domain.server.PersistenceUtility;
import org.rhq.core.domain.tagging.Tag;
import org.rhq.core.domain.util.OrderingField;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.server.search.SearchExpressionException;
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
        GROUP, // specifies the group alias to join on for standard group-role-subject authorization checking
        BUNDLE, // specifies the bundle alias to join on for standard bundle-bundleGroup-role-subject authorization checking
        BUNDLE_GROUP; // specifies the bundle group alias to join on for standard bundleGroup-role-subject authorization checking        
    }

    private Criteria criteria;
    private String searchExpressionWhereClause;

    private Subject subject;
    private String authorizationPermsFragment;
    private String authorizationCustomConditionFragment;
    private int authorizationSubjectId;

    private String alias;
    private String className;
    private String projection;
    private String countProjection;
    private String groupByClause;
    private String havingClause;
    private static String NL = System.getProperty("line.separator");

    private static List<String> EXPRESSION_START_KEYWORDS;

    private List<Field> persistentBagFields = new ArrayList<Field>();
    private List<Field> joinFetchFields = new ArrayList<Field>();

    static {
        EXPRESSION_START_KEYWORDS = new ArrayList<String>(2);
        EXPRESSION_START_KEYWORDS.add("NOT");
        EXPRESSION_START_KEYWORDS.add("EXISTS");
    }

    public CriteriaQueryGenerator(Criteria criteria) {
        this(LookupUtil.getSubjectManager().getOverlord(), criteria);
    }

    public CriteriaQueryGenerator(Subject subject, Criteria criteria) {
        this.subject = subject;
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
            setAuthorizationResourceFragment(type, defaultFragment, subjectId);
        } else if (type == AuthorizationTokenType.GROUP) {
            defaultFragment = "group";
            setAuthorizationResourceFragment(type, defaultFragment, subjectId);
        } else {
            throw new IllegalArgumentException(this.getClass().getSimpleName()
                + " does not yet support generating resource queries for '" + type + "' token types");
        }
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
            setAuthorizationCustomConditionFragment(getEnhancedResourceAuthorizationWhereFragment(fragment, subjectId));
        } else if (type == AuthorizationTokenType.GROUP) {
            // support for: 1) role-based for groups, 2) role-based for containing cluster groups, 3) private groups
            setAuthorizationCustomConditionFragment(getEnhancedGroupAuthorizationWhereFragment(fragment, subjectId));
        } else {
            throw new IllegalArgumentException(this.getClass().getSimpleName()
                + " does not yet support generating queries for '" + type + "' token types");
        }

        // If the query results are narrowed by requiredPerms generate the fragment now. It's done
        // here for two reasons. First, it seems to make sense to apply this only when an authFragment is
        // being used.  Second, because one day the query may be less brute force and may modify or
        // leverage the joinFragment above.  But, after extensive trying a more elegant
        // query could not be constructed due to Hibernate limitations. So, for now, here it is...
        List<Permission> requiredPerms = this.criteria.getRequiredPermissions();
        if (!(null == requiredPerms || requiredPerms.isEmpty())) {
            this.authorizationPermsFragment = "" //
                + "( SELECT COUNT(DISTINCT p)" + NL //
                + "   FROM Subject innerSubject" + NL //
                + "   JOIN innerSubject.roles r" + NL //
                + "   JOIN r.permissions p" + NL //
                + "   WHERE innerSubject.id = " + this.authorizationSubjectId + NL //
                + "   AND p IN ( :requiredPerms ) ) = :requiredPermsSize" + NL;
        }
    }

    private String getEnhancedResourceAuthorizationWhereFragment(String fragment, int subjectId) {
        String customAuthzFragment = "" //
            + "( %aliasWithFragment%.id IN ( SELECT %innerAlias%.id " + NL //
            + "                    FROM %alias% innerAlias " + NL //
            + "                    JOIN %innerAlias%.implicitGroups g JOIN g.roles r JOIN r.subjects s " + NL //
            + "                   WHERE s.id = %subjectId% ) )" + NL; //
        String aliasReplacement = criteria.getAlias() + (fragment != null ? "." + fragment : "");
        String innerAliasReplacement = "innerAlias" + (fragment != null ? "." + fragment : "");
        customAuthzFragment = customAuthzFragment.replace("%alias%", criteria.getAlias());
        customAuthzFragment = customAuthzFragment.replace("%aliasWithFragment%", aliasReplacement);
        customAuthzFragment = customAuthzFragment.replace("%innerAlias%", innerAliasReplacement);
        customAuthzFragment = customAuthzFragment.replace("%subjectId%", String.valueOf(subjectId));
        return customAuthzFragment;
    }

    private String getEnhancedGroupAuthorizationWhereFragment(String fragment, int subjectId) {
        String customAuthzFragment = "" //
            + "( %aliasWithFragment%.id IN ( SELECT %innerAlias%.id " + NL //
            + "                    FROM %alias% innerAlias " + NL //
            + "                    JOIN %innerAlias%.roles r JOIN r.subjects s " + NL //
            + "                   WHERE s.id = %subjectId% )" + NL //
            + "  OR" + NL //
            + "  %aliasWithFragment%.id IN ( SELECT %innerAlias%.id " + NL //
            + "                    FROM %alias% innerAlias " + NL //
            + "                    JOIN %innerAlias%.clusterResourceGroup crg JOIN crg.roles r JOIN r.subjects s " + NL //
            + "                   WHERE crg.recursive = true AND s.id = %subjectId% )" + NL //
            + "  OR" + NL //
            + "  %aliasWithFragment%.id IN ( SELECT %innerAlias%.id" + NL //
            + "                    FROM %alias% innerAlias " + NL //
            + "                    JOIN %innerAlias%.subject s" + NL //
            + "                   WHERE s.id = %subjectId% ) ) " + NL;
        String aliasReplacement = criteria.getAlias() + (fragment != null ? "." + fragment : "");
        String innerAliasReplacement = "innerAlias" + (fragment != null ? "." + fragment : "");
        customAuthzFragment = customAuthzFragment.replace("%alias%", criteria.getAlias());
        customAuthzFragment = customAuthzFragment.replace("%aliasWithFragment%", aliasReplacement);
        customAuthzFragment = customAuthzFragment.replace("%innerAlias%", innerAliasReplacement);
        customAuthzFragment = customAuthzFragment.replace("%subjectId%", String.valueOf(subjectId));
        return customAuthzFragment;
    }

    public void setAuthorizationBundleFragment(AuthorizationTokenType type, int subjectId) {
        if (type == AuthorizationTokenType.BUNDLE) {
            setAuthorizationBundleFragment(type, subjectId, "bundle");
        } else if (type == AuthorizationTokenType.BUNDLE_GROUP) {
            setAuthorizationBundleFragment(type, subjectId, "bundleGroup");
        } else {
            throw new IllegalArgumentException(this.getClass().getSimpleName()
                + " does not yet support generating bundle queries for '" + type + "' token types");
        }
    }

    public void setAuthorizationBundleFragment(AuthorizationTokenType type, int subjectId, String fragment) {
        if (type == AuthorizationTokenType.BUNDLE) {
            setAuthorizationBundleFragment(subjectId, fragment);
        } else if (type == AuthorizationTokenType.BUNDLE_GROUP) {
            setAuthorizationBundleGroupFragment(subjectId, fragment);
        } else {
            throw new IllegalArgumentException(this.getClass().getSimpleName()
                + " does not yet support generating bundle queries for '" + type + "' token types");
        }
    }

    private void setAuthorizationBundleFragment(int subjectId, String fragment) {
        this.authorizationSubjectId = subjectId;

        String customAuthzFragment = "" //
            + "( %aliasWithFragment%.id IN ( SELECT %innerAlias%.id " + NL //
            + "                    FROM %alias% innerAlias " + NL //
            + "                    JOIN %innerAlias%.bundleGroups g JOIN g.roles r JOIN r.subjects s " + NL //
            + "                   WHERE s.id = %subjectId% ) )" + NL; //
        String aliasReplacement = criteria.getAlias() + (fragment != null ? "." + fragment : "");
        String innerAliasReplacement = "innerAlias" + (fragment != null ? "." + fragment : "");
        customAuthzFragment = customAuthzFragment.replace("%alias%", criteria.getAlias());
        customAuthzFragment = customAuthzFragment.replace("%aliasWithFragment%", aliasReplacement);
        customAuthzFragment = customAuthzFragment.replace("%innerAlias%", innerAliasReplacement);
        customAuthzFragment = customAuthzFragment.replace("%subjectId%", String.valueOf(subjectId));
        this.authorizationCustomConditionFragment = customAuthzFragment;

        // If the query results are narrowed by requiredPerms generate the fragment now. It's done
        // here for two reasons. First, it seems to make sense to apply this only when an authFragment is
        // being used.  Second, because one day the query may be less brute force and may modify or
        // leverage the joinFragment above.  But, after extensive trying a more elegant
        // query could not be constructed due to Hibernate limitations. So, for now, here it is...
        List<Permission> requiredPerms = this.criteria.getRequiredPermissions();
        if (!(null == requiredPerms || requiredPerms.isEmpty())) {
            this.authorizationPermsFragment = "" //
                + "( SELECT COUNT(DISTINCT p)" + NL //
                + "   FROM Subject innerSubject" + NL //
                + "   JOIN innerSubject.roles r" + NL //
                + "   JOIN r.permissions p" + NL //
                + "   WHERE innerSubject.id = " + this.authorizationSubjectId + NL //
                + "   AND p IN ( :requiredPerms ) ) = :requiredPermsSize" + NL;
        }
    }

    private void setAuthorizationBundleGroupFragment(int subjectId, String fragment) {
        this.authorizationSubjectId = subjectId;

        String customAuthzFragment = "" //
            + "( %aliasWithFragment%.id IN ( SELECT %innerAlias%.id " + NL //
            + "                    FROM %alias% innerAlias " + NL //
            + "                    JOIN %innerAlias%.roles r JOIN r.subjects s " + NL //
            + "                   WHERE s.id = %subjectId% ) ) " + NL;
        String aliasReplacement = criteria.getAlias() + (fragment != null ? "." + fragment : "");
        String innerAliasReplacement = "innerAlias" + (fragment != null ? "." + fragment : "");
        customAuthzFragment = customAuthzFragment.replace("%alias%", criteria.getAlias());
        customAuthzFragment = customAuthzFragment.replace("%aliasWithFragment%", aliasReplacement);
        customAuthzFragment = customAuthzFragment.replace("%innerAlias%", innerAliasReplacement);
        customAuthzFragment = customAuthzFragment.replace("%subjectId%", String.valueOf(subjectId));
        this.authorizationCustomConditionFragment = customAuthzFragment;

        // If the query results are narrowed by requiredPerms generate the fragment now. It's done
        // here for two reasons. First, it seems to make sense to apply this only when an authFragment is
        // being used.  Second, because one day the query may be less brute force and may modify or
        // leverage the joinFragment above.  But, after extensive trying a more elegant
        // query could not be constructed due to Hibernate limitations. So, for now, here it is...
        List<Permission> requiredPerms = this.criteria.getRequiredPermissions();
        if (!(null == requiredPerms || requiredPerms.isEmpty())) {
            this.authorizationPermsFragment = "" //
                + "( SELECT COUNT(DISTINCT p)" + NL //
                + "   FROM Subject innerSubject" + NL //
                + "   JOIN innerSubject.roles r" + NL //
                + "   JOIN r.permissions p" + NL //
                + "   WHERE innerSubject.id = " + this.authorizationSubjectId + NL //
                + "   AND p IN ( :requiredPerms ) ) = :requiredPermsSize" + NL;
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
        String returnValue;
        if (value instanceof String) {
            returnValue = "'" + prepareStringBindValue((String) value) + "'";
        } else if (value instanceof Enum<?>) {
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

    public String getQueryString(boolean countQuery) {
        StringBuilder results = new StringBuilder();

        PageControl pc = getPageControl(criteria);

        results.append("SELECT ");

        List<String> fetchFields = getFetchFields(criteria);
        boolean useJoinFetch = projection == null && pc.isUnlimited() && !fetchFields.isEmpty();

        if (countQuery) {
            if (countProjection != null) {
                //just use whatever we are told
                results.append(countProjection).append(NL);
            } else if (groupByClause == null) { // non-grouped method
                // use count(*) instead of count(alias) due to https://bugzilla.redhat.com/show_bug.cgi?id=699842
                results.append("COUNT(*)").append(NL);
            } else {
                // gets the count of the number of aggregate/grouped rows
                // NOTE: this only works when the groupBy is a single element, as opposed to a list of elements
                results.append("COUNT(DISTINCT ").append(groupByClause).append(")").append(NL);
            }
        } else {
            if (projection == null) {
                //we need to just return distinct results when using JOIN FETCH otherwise we might see duplicates
                //in the result set and create discrepancy between the data query and the count query (which doesn't
                //use the JOIN FETCH but only the WHERE clause).
                if (useJoinFetch) {
                    results.append("DISTINCT ");
                }
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
            for (String fetchField : fetchFields) {
                if (isPersistentBag(fetchField)) {
                    addPersistentBag(fetchField);
                } else {
                    if (this.projection == null) {
                        /*
                         * if not altering the projection, join fetching can be used
                         * to retrieve the associated instance in the same SELECT
                         *
                         * We further avoid a JOIN FETCH when executing queries with limits.
                         * Such execution has performance problems that we solve by initializing the fields
                         * "manually" in the CriteriaQueryRunner and by defining a default batch fetch size in the
                         * persistence.xml.
                         */
                        if (useJoinFetch) {
                            results.append("LEFT JOIN FETCH ").append(alias).append('.').append(fetchField).append(NL);
                        } else {
                            addJoinFetch(fetchField);
                        }
                    } else {
                        /*
                         * if the projection is altered (perhaps converting it into a constructor query), then all
                         * fields specified in the fetch must be in the explicit return list.  this is not possible
                         * today with constructor queries, so any altered projection will implicitly disable fetching.
                         * instead, we'll record which fields need to be explicitly fetched after the primary query
                         * returns the bulk of the data, and use a similar methodology at the SLSB layer to eagerly
                         * load those before returning the PageList back to the caller.
                         */
                        addJoinFetch(fetchField);
                    }
                }
            }
        }

        // figure out the 'LEFT JOIN's needed for 'ORDER BY' tokens
        List<String> orderingFieldRequiredJoins = new ArrayList<String>();
        List<String> orderingFieldTokens = new ArrayList<String>();

        for (OrderingField orderingField : pc.getOrderingFields()) {
            PageOrdering ordering = orderingField.getOrdering();
            String fieldName = orderingField.getField();
            String override = criteria.getJPQLSortOverride(fieldName);
            String suffix = (override == null) ? fieldName : override;

            /*
             * do not prefix the alias when:
             *
             *    1) if the suffix is numerical, which allows us to sort by column ordinal
             *    2) if the user wants full control and has explicitly chosen to disable alias prepending
             */
            boolean doNotPrefixAlias = isNumber(suffix) || criteria.hasCustomizedSorting();
            String sortFragment = doNotPrefixAlias ? suffix : (alias + "." + suffix);

            if (criteria.hasCustomizedSorting()) {
                // customized sorting does not get LEFT JOIN expressions added
                orderingFieldTokens.add(sortFragment + " " + ordering);
                continue;
            }

            int lastDelimiterIndex = sortFragment.lastIndexOf('.');
            if (lastDelimiterIndex == -1) {
                // does not require joins, just add the ordering field token directly
                orderingFieldTokens.add(sortFragment + " " + ordering);
                continue;
            }

            int firstDelimiterIndex = sortFragment.indexOf('.');
            if (firstDelimiterIndex == lastDelimiterIndex) {
                // only one dot implies its a property/field directly off of the primary alias
                // thus, also does not require joins, just add the ordering field token directly
                orderingFieldTokens.add(sortFragment + " " + ordering);
                continue;
            }

            String expressionRoot = sortFragment.substring(0, lastDelimiterIndex);
            String expressionLeaf = sortFragment.substring(lastDelimiterIndex + 1);
            int expressionRootIndex = orderingFieldRequiredJoins.indexOf(expressionRoot);

            String joinAlias;
            if (expressionRootIndex == -1) {
                // new join
                joinAlias = "orderingField" + orderingFieldRequiredJoins.size();
                orderingFieldRequiredJoins.add(expressionRoot);
                results.append("LEFT JOIN ").append(expressionRoot).append(" ").append(joinAlias).append(NL);
            } else {
                joinAlias = "orderingField" + expressionRootIndex;
            }

            orderingFieldTokens.add(joinAlias + "." + expressionLeaf + " " + ordering);
        }

        Map<String, Object> filterFields = getFilterFields(criteria);
        if (filterFields.size() > 0 || authorizationPermsFragment != null
            || authorizationCustomConditionFragment != null || searchExpressionWhereClause != null) {
            results.append("WHERE ");
        }

        String conjunctiveFragment = criteria.isFiltersOptional() ? "OR " : "AND ";
        boolean wantCaseInsensitiveMatch = !criteria.isCaseSensitive();

        // criteria
        StringBuilder conjunctiveResults = new StringBuilder();
        boolean firstCrit = true;
        for (Map.Entry<String, Object> filterField : filterFields.entrySet()) {
            Object filterFieldValue = filterField.getValue();

            // if this filter field is non-binding (that is, the query has no parameter whose value is to be bound for the field)
            // and that filter field is turned off, do nothing and continue to the next filter.
            // this in effect does not filter on this field at all.
            if (Criteria.NonBindingOverrideFilter.OFF.equals(filterFieldValue)) {
                continue;
            }

            if (firstCrit) {
                firstCrit = false;
            } else {
                conjunctiveResults.append(NL).append(conjunctiveFragment);
            }
            String fieldName = filterField.getKey();
            String override = criteria.getJPQLFilterOverride(fieldName);
            String fragment;
            if (override != null) {
                fragment = fixFilterOverride(override, fieldName);
            } else {
                String operator = "=";
                if (filterFieldValue instanceof String) {
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
        if (authorizationPermsFragment != null) {
            if (firstCrit) {
                firstCrit = false;
            } else {
                // always want AND for security, regardless of conjunctiveFragment
                results.append(NL).append(" AND ");
            }
            results.append(this.authorizationPermsFragment).append(" ");
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

            // ordering clause
            boolean first = true;
            for (String next : orderingFieldTokens) {
                if (first) {
                    results.append(NL).append("ORDER BY ");
                    first = false;
                } else {
                    results.append(", ");
                }
                results.append(next);
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
        for (Field fetchField : CriteriaUtil.getFields(criteria, Criteria.Type.FETCH)) {
            Object fetchFieldValue;
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

    public static String getCleansedFieldName(Field field, int leadingCharsToStrip) {
        String fieldNameFragment = field.getName().substring(leadingCharsToStrip);
        String fieldName = Character.toLowerCase(fieldNameFragment.charAt(0)) + fieldNameFragment.substring(1);
        return fieldName;
    }

    public Map<String, Object> getFilterFields(Criteria criteria) {
        Map<String, Object> results = new HashMap<String, Object>();
        for (Field filterField : CriteriaUtil.getFields(criteria, Criteria.Type.FILTER)) {
            Object filterFieldValue;
            try {
                filterField.setAccessible(true);
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
            SearchTranslationManager searchManager = new SearchTranslationManager(criteria.getAlias(), subject,
                SearchSubsystem.get(entityClass));
            searchManager.setExpression(searchExpression);

            // translate first, if there was an error we won't add the dangling 'AND' to the where clause
            String translatedJPQL = searchManager.getJPQLWhereFragment();
            LOG.debug("Translated JPQL Fragment was: " + translatedJPQL);
            if (translatedJPQL != null) {
                searchExpressionWhereClause = translatedJPQL;
            }
        } catch (SearchExpressionException see) {
            throw see; // bubble up to the top
        } catch (RuntimeException re) {
            LOG.error("Could not get JPQL translation for '" + searchExpression + "': "
                + ThrowableUtil.getAllMessages(re, true));
            throw re; // don't wrap exceptions that are already RuntimeExceptions in another RuntimeException
        } catch (Exception e) {
            LOG.error("Could not get JPQL translation for '" + searchExpression + "': "
                + ThrowableUtil.getAllMessages(e, true));
            throw new RuntimeException(e);
        }
    }

    private boolean isPersistentBag(String fieldName) {
        Field field = findField(fieldName);

        return field != null && isAList(field) && !field.isAnnotationPresent(IndexColumn.class);
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
        Field f = findField(fieldName);
        if (f == null) {
            LOG.warn("Failed to add persistent bag collection [" + fieldName + "] on class ["
                + criteria.getPersistentClass().getName()
                + "]. There doesn't seem to be a field of that name on the class or any of its superclasses.");
        } else {
            persistentBagFields.add(f);
        }
    }

    private void addJoinFetch(String fieldName) {
        Field f = findField(fieldName);
        if (f == null) {
            LOG.warn("Failed to add join fetch field [" + fieldName + "] on class ["
                + criteria.getPersistentClass().getName()
                + "]. There doesn't seem to be a field of that name on the class or any of its superclasses.");
        } else {
            joinFetchFields.add(f);
        }
    }

    private Field findField(String fieldName) {
        Class<?> cls = criteria.getPersistentClass();
        while (cls != null) {
            try {
                return cls.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                cls = cls.getSuperclass();
            }
        }

        return null;
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
     * <strong>Note:</strong> This method should only be called after {@link #getQueryString(boolean)}} because it is
     * that method where the persistentBagFields property is initialized.
     * <p/>
     * The elements of the returned list are a (sub)set of the fields that the criteria object specified to be fetched
     * (using the fetchXXX() methods). If the {@link CriteriaQueryRunner} is not set to automatically fetch all the
     * fields, you need to manually initialize these fields by for example using
     * {@link CriteriaQueryRunner#initFetchFields(Object)} method on each of the results.
     *
     * @see #alterProjection(String) <code>alterProjection(String)</code> for special attention you need to make when
     * mixing fetching fields and altered projection.
     *
     * @return Returns a list of fields from the persistent class to which the criteria class corresponds. The fields in
     *         the list are fields specified by the criteria to be fetched (using the fetchXXX() methods).
     */
    public List<Field> getJoinFetchFields() {
        return joinFetchFields;
    }

    /**
     * If you want to return something other than the list of entities represented by the passed Criteria object,
     * you can alter the projection here to return a customized subset or superset of data.  The projection will
     * only affect the ResultSet for the data query, not the count query.
     * <p/>
     * If you are projecting a composite object that does not directly extend the entity your Criteria object
     * represents, then you will need to manually initialize the persistent bags and fetch fields using the
     * {@link CriteriaQueryRunner#initFetchFields(Object)} method for each object in the results (for which you need
     * to instantiate the {@link CriteriaQueryRunner} with automatic fetching switched OFF). <b>Note</b> that this will
     * NOT work on the composite object itself. You need to pass an instance of the entity class to the
     *{@link CriteriaQueryRunner#initFetchFields(Object)} method.
     */
    public void alterProjection(String projection) {
        this.projection = projection;
    }

    /**
     * Sometimes the altered projection ({@link #alterProjection(String)}) might cause the result set to have different
     * number of results than the default/unaltered projection. Leaving the count query in the default form could then
     * generate seemingly inconsistent results, where the data query and the count query wouldn't match up.
     * <p/>
     * An example of a projection that might alter the number of results is the {@code " distinct ..."} projection that
     * would only return distinct results from a dataset, while the default count query (COUNT(*)) would produce the
     * count including duplicate results that were eliminated in the returned data.
     * <p/>
     * In these cases one can also alter the count query to count the results the data query will return.
     *
     * @param countProjection a complete JPQL fragment expressing the count expression (e.g.
     *                        {@code COUNT(DISTINCT ...)})
     */
    public void alterCountProjection(String countProjection) {
        this.countProjection = countProjection;
    }

    public boolean isProjectionAltered() {
        return this.projection != null;
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
        setBindValues(query);
        PersistenceUtility.setDataPage(query, getPageControl(criteria));
        return query;
    }

    public Query getCountQuery(EntityManager em) {
        String countQueryString = getQueryString(true);
        Query query = em.createQuery(countQueryString);
        setBindValues(query);
        return query;
    }

    private void setBindValues(Query query) {
        for (Map.Entry<String, Object> critField : getFilterFields(criteria).entrySet()) {
            Object value = critField.getValue();

            if (value instanceof Tag) {
                Tag tag = (Tag) value;
                query.setParameter("tagNamespace", tag.getNamespace());
                query.setParameter("tagSemantic", tag.getSemantic());
                query.setParameter("tagName", tag.getName());

            } else if (value instanceof Criteria.NonBindingOverrideFilter) {
                // skip this one - do nothing since there is no parameter binding for this value
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
        //testResourceCriteria();
        testResourceGroupCriteria();
    }

    public static void testSubjectCriteria() {
        SubjectCriteria subjectCriteria = new SubjectCriteria();
        subjectCriteria.addFilterFirstName("joe");
        subjectCriteria.addFilterFactive(true);
        subjectCriteria.fetchRoles(true);
        subjectCriteria.addSortName(PageOrdering.ASC);

        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
        CriteriaQueryGenerator subjectGenerator = new CriteriaQueryGenerator(overlord, subjectCriteria);
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

        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(overlord, alertCriteria);
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

        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(overlord, historyCriteria);
        System.out.println(generator.getQueryString(false));
        System.out.println(generator.getQueryString(true));
    }

    public static void testResourceGroupCriteria() {
        ResourceGroupCriteria groupCriteria = new ResourceGroupCriteria();
        groupCriteria.addSortName(PageOrdering.DESC);
        groupCriteria.addSortResourceTypeName(PageOrdering.ASC);
        groupCriteria.addSortPluginName(PageOrdering.DESC);

        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(new Subject(), groupCriteria);
        System.out.println(generator.getQueryString(false));
        System.out.println(generator.getQueryString(true));

        PageControl customPC = new PageControl();
        customPC.addDefaultOrderingField("0", PageOrdering.DESC);
        customPC.addDefaultOrderingField("name", PageOrdering.DESC);
        customPC.addDefaultOrderingField("resourceType.name", PageOrdering.ASC);
        groupCriteria.setPageControl(customPC);

        System.out.println(generator.getQueryString(false));
        System.out.println(generator.getQueryString(true));
    }

    public static void testResourceCriteria() {
        ResourceCriteria resourceCriteria = new ResourceCriteria();
        resourceCriteria.addFilterResourceCategories(ResourceCategory.SERVER);
        resourceCriteria.addFilterName("marques");
        resourceCriteria.fetchAgent(true);
        resourceCriteria.addSortResourceTypeName(PageOrdering.ASC);
        resourceCriteria.setCaseSensitive(true);
        resourceCriteria.setFiltersOptional(true);

        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(overlord, resourceCriteria);
        System.out.println(generator.getQueryString(false));
        System.out.println(generator.getQueryString(true));
    }

    public static PageControl getPageControl(Criteria criteria) {
        PageControl pc;

        if (criteria.getPageControlOverrides() != null) {
            pc = criteria.getPageControlOverrides();
        } else {
            if (criteria.getPageNumber() == null || criteria.getPageSize() == null) {
                pc = PageControl.getUnlimitedInstance();
            } else {
                pc = new PageControl(criteria.getPageNumber(), criteria.getPageSize());
            }

            for (String fieldName : criteria.getOrderingFieldNames()) {
                for (Field sortField : CriteriaUtil.getFields(criteria, Criteria.Type.SORT)) {
                    if (sortField.getName().equals(fieldName) == false) {
                        continue;
                    }
                    Object sortFieldValue;
                    try {
                        sortField.setAccessible(true);
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

        // Unless paging is unlimited or it's not supported, add a sort on ID.  This ensures that when paging
        // we always have a consistent ordering. In other words, if the data set is unchanged between pages, there
        // will no overlap/repetition of rows. Note that this applies even if other sort fields have been
        // set, because they may still not have unique values. See https://bugzilla.redhat.com/show_bug.cgi?id=966665.
        if (!pc.isUnlimited() && criteria.isSupportsAddSortId()) {
            pc.addDefaultOrderingField("id");
        }

        return pc;
    }

}
