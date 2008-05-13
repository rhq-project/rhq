/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.resource.group.definition.framework;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.enterprise.server.common.EntityManagerFacadeLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ExpressionEvaluator implements Iterable<ExpressionEvaluator.Result> {

    private final Log log = LogFactory.getLog(ExpressionEvaluator.class);

    private static final String INVALID_EXPRESSION_FORM_MSG = "Expression must be in the form of 'condition = value' or 'groupBy condition'";

    private static final String PROP_SIMPLE_ALIAS = "simple";
    private static final String TRAIT_ALIAS = "trait";
    private static final String METRIC_DEF_ALIAS = "def";

    private enum JoinCondition {
        RESOURCE_CONFIGURATION(".resourceConfiguration", "conf"), // 
        PLUGIN_CONFIGURATION(".pluginConfiguration", "pluginConf"), //
        SCHEDULES(".schedules", "sched"), //
        RESOURCE_CHILD(".childResources", "child");

        String subexpression;
        String alias;

        private JoinCondition(String subexpression, String alias) {
            this.subexpression = subexpression;
            this.alias = alias;
        }
    }

    private Set<JoinCondition> joinConditions;
    private Map<String, String> whereConditions;
    private Map<String, Object> whereReplacements;
    private Set<String> whereStatics;
    private List<String> groupByElements;

    private List<String> simpleSubExpressions;
    private List<String> groupedSubExpressions;

    private int expressionCount;
    private boolean isInvalid;
    private boolean isTestMode;
    private boolean resultsComputed;

    private String computedJPQLStatement;
    private String computedJPQLGroupStatement;

    private EntityManagerFacadeLocal entityManagerFacade;

    public ExpressionEvaluator() {
        /*
         * used LinkedHashMap for whereConditions on purpose so that the iterator will return them in the same order
         * they were added to the list, this ensures that the getComputed*Statement() methods will construct the
         * generated JPQL in the order that the bind parameter names are generated;
         *
         * the query technology, of course, doesn't require this...but it makes generating a test suite and verifying
         * expected output a lot easier
         */
        joinConditions = new HashSet<JoinCondition>();
        whereConditions = new LinkedHashMap<String, String>();
        whereReplacements = new HashMap<String, Object>();
        whereStatics = new HashSet<String>();
        groupByElements = new ArrayList<String>();

        simpleSubExpressions = new ArrayList<String>();
        groupedSubExpressions = new ArrayList<String>();

        expressionCount = 0;
        isInvalid = false;
        isTestMode = false;
        resultsComputed = false;

        computedJPQLStatement = "";
        computedJPQLGroupStatement = "";

        entityManagerFacade = LookupUtil.getEntityManagerFacade();

        /*
         * initialization for special handling that all dynagroups should get
         */
        whereStatics.add("res.inventoryStatus = org.rhq.core.domain.resource.InventoryStatus.COMMITTED");
    }

    public class Result {
        private final List<Integer> data;
        private final String groupByClause;

        public Result(List<Integer> data) {
            this.data = data;
            this.groupByClause = "";
        }

        public Result(List<Integer> data, String groupByExpression) {
            this.data = data;
            this.groupByClause = groupByExpression;
        }

        public List<Integer> getData() {
            return data;
        }

        public String getGroupByClause() {
            return groupByClause;
        }
    }

    /**
     * @param mode passing a value of true will bypass query later and only compute the effective JPQL statements, handy
     *             for testing
     */
    public void setTestMode(boolean mode) {
        isTestMode = mode;
        whereStatics.remove("res.inventoryStatus = org.rhq.core.domain.resource.InventoryStatus.COMMITTED");
    }

    /**
     * @param  expression a string in the form of 'condition = value' or 'groupBy condition'
     *
     * @return a reference to itself, so that method chaining can be used
     *
     * @throws InvalidExpressionException if the expression can not be parsed for any reason, the message will try to
     *                                    get the details as to the parse failure
     */
    public ExpressionEvaluator addExpression(String expression) throws InvalidExpressionException {
        if (isInvalid) {
            throw new IllegalStateException("This evaluator previously threw an exception and can no longer be used");
        }

        try {
            isGroupBy = false; // this needs to be reset each time a new expression is added
            parseExpression(expression);
            expressionCount++;
        } catch (InvalidExpressionException iee) {
            isInvalid = true;
            throw iee;
        }

        return this;
    }

    /**
     * @return the JPQL statement that will be sent to the database, assuming test mode is false (the default): -- if no
     *         groupBy expressions are present, it will query for the target object -- if at least one groupBy
     *         expression was used, it will return the query to find the pivot data
     */
    public String getComputedJPQLStatement() {
        if (resultsComputed == false) {
            throw new IllegalStateException("Results must be computed before this method can be called");
        }

        return computedJPQLStatement;
    }

    /**
     * @return the JPQL statement that will be sent to the database, assuming test mode is false (the default): -- if no
     *         groupBy expressions are present, this will return "" -- if at least one groupBy expression was used, it
     *         will return the pivoted query
     */
    public String getComputedJPQLGroupStatement() {
        if (resultsComputed == false) {
            throw new IllegalStateException("Results must be computed before this method can be called");
        }

        return computedJPQLGroupStatement;
    }

    private enum ParseContext {
        BEGIN(false), //
        Pivot(false), //
        Resource(false), //
        ResourceParent(false), //
        ResourceGrandParent(false), //
        ResourceChild(false), //
        ResourceType(false), //
        Trait(true), //
        Configuration(true), //
        StringMatch(true), //
        END(true);

        private boolean canTerminateExpression;

        private ParseContext(boolean canTerminateExpression) {
            this.canTerminateExpression = canTerminateExpression;
        }

        public boolean isExpressionTerminator() {
            return this.canTerminateExpression;
        }
    }

    private enum ParseSubContext {
        PluginConfiguration, ResourceConfiguration;
    }

    private ParseContext context = ParseContext.BEGIN;
    private ParseSubContext subcontext = null;
    private int parseIndex = 0;
    private boolean isGroupBy = false;

    private ParseContext deepestResourceContext = null;

    /**
     * @param  expression a string in the form of 'condition = value' or 'groupBy condition'
     *
     * @throws InvalidExpressionException if the expression can not be forward-only parsed for any reason, the exception
     *                                    message will contain the details of the parse failure
     */
    private void parseExpression(String expression) throws InvalidExpressionException {
        if (expression.trim().equals("")) {
            // allow empty lines, by simply ignoring them
            return;
        }

        String condition;
        String value = null;

        /*
         * instead of building '= value' parsing into the below algorithm, let's chop this off early and store it; this
         * makes the rest of the parsing a bit simpler because some ParseContexts need the value immediately in order to
         * properly build up internal maps constructs to be used in generating the requisite JPQL statement
         */
        int equalsIndex = expression.indexOf('=');
        if (equalsIndex == -1) {
            condition = expression;
        } else {
            condition = expression.substring(0, equalsIndex);
            value = expression.substring(equalsIndex + 1).trim();
            if (value.equals("")) {
                throw new InvalidExpressionException(INVALID_EXPRESSION_FORM_MSG);
            }
        }

        /*
         * the remainder of the passed expression should be in the form of '[groupBy] condition', so let's tokenize on
         * '.' and ' ' and continue the parse
         */
        List<String> originalTokens = tokenizeCondition(condition);

        String[] tokens = new String[originalTokens.size()];
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = originalTokens.get(i).toLowerCase();
        }

        /*
         * build the normalized expression outside of the parse, to keep the parse code as clean as possible;
         * however, this string will be used to determine if the expression being added to this evaluator, based
         * on whether it's grouped or not, compared against all expressions seen so far, is valid
         */
        StringBuilder normalizedSubExpressionBuilder = new StringBuilder();
        for (String subExpressionToken : tokens) {
            if (subExpressionToken.equals("groupby")) {
                continue;
            }
            normalizedSubExpressionBuilder.append(subExpressionToken);
        }
        String normalizedSubExpression = normalizedSubExpressionBuilder.toString();

        /*
         * it's easier to code a quick solution when each ParseContext can ignore additional whitespace from the
         * original expression
         */
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = tokens[i].trim();
        }

        // setup some instance-level context data to be manipulated and leveraged during the parse
        context = ParseContext.BEGIN;
        subcontext = null;
        parseIndex = 0;

        deepestResourceContext = null;

        for (; parseIndex < tokens.length; parseIndex++) {
            String nextToken = tokens[parseIndex];

            if (context == ParseContext.BEGIN) {
                if (nextToken.equals("resource")) {
                    if (value == null) {
                        // filter expressions must have "= <value>" part
                        throw new InvalidExpressionException(INVALID_EXPRESSION_FORM_MSG);
                    }
                    validateSubExpressionAgainstPreviouslySeen(normalizedSubExpression, false);
                    context = ParseContext.Resource;
                    deepestResourceContext = context;
                } else if (nextToken.equals("groupby")) {
                    if (value != null) {
                        // grouped expressions must NOT have "= <value>" part
                        throw new InvalidExpressionException(INVALID_EXPRESSION_FORM_MSG);
                    }
                    validateSubExpressionAgainstPreviouslySeen(normalizedSubExpression, true);
                    isGroupBy = true;
                    context = ParseContext.Pivot;
                } else {
                    throw new InvalidExpressionException(
                        "Expression must either start with 'resource' or 'groupby' token");
                }
            } else if (context == ParseContext.Pivot) {
                if (nextToken.equals("resource")) {
                    context = ParseContext.Resource;
                    deepestResourceContext = context;
                } else {
                    throw new InvalidExpressionException("Grouped expressions must be followed by the 'resource' token");
                }
            } else if (context == ParseContext.Resource) {
                if (nextToken.equals("parent")) {
                    context = ParseContext.ResourceParent;
                    deepestResourceContext = context;
                } else if (nextToken.equals("grandparent")) {
                    context = ParseContext.ResourceGrandParent;
                    deepestResourceContext = context;
                } else if (nextToken.equals("child")) {
                    context = ParseContext.ResourceChild;
                    deepestResourceContext = context;
                } else {
                    parseExpression_resourceContext(value, tokens, nextToken);
                }
            } else if ((context == ParseContext.ResourceParent) || (context == ParseContext.ResourceGrandParent)
                || (context == ParseContext.ResourceChild)) {
                // since a parent or child *is* a resource, support the exact same processing
                parseExpression_resourceContext(value, tokens, nextToken);
            } else if (context == ParseContext.ResourceType) {
                if (nextToken.equals("plugin")) {
                    populatePredicateCollections(getResourceRelativeContextToken() + ".resourceType.plugin", value);
                } else if (nextToken.equals("name")) {
                    populatePredicateCollections(getResourceRelativeContextToken() + ".resourceType.name", value);
                } else if (nextToken.equals("category")) {
                    populatePredicateCollections(getResourceRelativeContextToken() + ".resourceType.category",
                        (value == null) ? null : ResourceCategory.valueOf(value.toUpperCase()));
                } else {
                    throw new InvalidExpressionException("Invalid 'type' subexpression: "
                        + PrintUtils.getDelimitedString(tokens, parseIndex, "."));
                }
            } else if (context == ParseContext.Trait) {
                // SELECT res.id FROM Resource res JOIN res.schedules sched, sched.definition def, MeasurementDataTrait trait
                // WHERE def.name = :arg1 AND trait.value = :arg2 AND trait.schedule = sched AND trait.id.timestamp =
                // (SELECT max(mdt.id.timestamp) FROM MeasurementDataTrait mdt WHERE sched.id = mdt.schedule.id)
                String traitName = parseTraitName(originalTokens);
                joinConditions.add(JoinCondition.SCHEDULES);
                populatePredicateCollections(METRIC_DEF_ALIAS + ".name", traitName, false);
                populatePredicateCollections(TRAIT_ALIAS + ".value", value);
                whereStatics.add(TRAIT_ALIAS + ".schedule = " + JoinCondition.SCHEDULES.alias);
                whereStatics.add(TRAIT_ALIAS
                    + ".id.timestamp = (SELECT max(mdt.id.timestamp) FROM MeasurementDataTrait mdt WHERE "
                    + JoinCondition.SCHEDULES.alias + ".id = mdt.schedule.id)");
            } else if (context == ParseContext.Configuration) {
                String prefix;
                JoinCondition joinCondition;

                if (subcontext == ParseSubContext.PluginConfiguration) {
                    prefix = "pluginconfiguration";
                    joinCondition = JoinCondition.PLUGIN_CONFIGURATION;
                } else if (subcontext == ParseSubContext.ResourceConfiguration) {
                    prefix = "resourceconfiguration";
                    joinCondition = JoinCondition.RESOURCE_CONFIGURATION;
                } else {
                    throw new InvalidExpressionException("Invalid 'configuration' subexpression: " + subcontext);
                }

                String suffix = originalTokens.get(parseIndex).substring(prefix.length());
                if (suffix.length() < 3) {
                    throw new InvalidExpressionException("Unrecognized connection property '" + suffix + "'");
                }

                if ((suffix.charAt(0) != '[') || (suffix.charAt(suffix.length() - 1) != ']')) {
                    throw new InvalidExpressionException("Property '" + suffix
                        + "' must be contained within '[' and ']' characters");
                }

                String propertyName = suffix.substring(1, suffix.length() - 1);

                joinConditions.add(joinCondition);
                populatePredicateCollections(PROP_SIMPLE_ALIAS + ".name", propertyName, false);
                populatePredicateCollections(PROP_SIMPLE_ALIAS + ".stringValue", value);
                whereStatics.add(PROP_SIMPLE_ALIAS + ".configuration = " + joinCondition.alias);
            } else if (context == ParseContext.StringMatch) {
                String lastArgumentName = getLastArgumentName();
                String argumentValue = (String) whereReplacements.get(lastArgumentName);

                if (nextToken.equals("startswith")) {
                    argumentValue = "%" + argumentValue.replaceAll("\\_", "\\\\_");
                } else if (nextToken.equals("endswith")) {
                    argumentValue = argumentValue.replaceAll("\\_", "\\\\_") + "%";
                } else if (nextToken.equals("contains")) {
                    argumentValue = "%" + argumentValue.replaceAll("\\_", "\\\\_") + "%";
                } else {
                    throw new InvalidExpressionException("Unrecognized string function '" + nextToken
                        + "' at end of condition");
                }

                // fix the value replacement with the JPQL fragment that maps to the specified string function
                whereReplacements.put(lastArgumentName, argumentValue);

                context = ParseContext.END;
            } else if (context == ParseContext.END) {
                throw new InvalidExpressionException("Unrecognized tokens at end of expression");
            } else {
                throw new InvalidExpressionException("Unknown parse context: " + context);
            }
        }

        if (context.isExpressionTerminator() == false) {
            throw new InvalidExpressionException("Unexpected termination of expression");
        }
    }

    private String getResourceRelativeContextToken() {
        if (deepestResourceContext == ParseContext.Resource) {
            return "res";
        } else if (deepestResourceContext == ParseContext.ResourceParent) {
            return "res.parentResource";
        } else if (deepestResourceContext == ParseContext.ResourceGrandParent) {
            return "res.parentResource.parentResource";
        } else if (deepestResourceContext == ParseContext.ResourceChild) {
            // populate child stuff
            joinConditions.add(JoinCondition.RESOURCE_CHILD);
            return "child";
        } else {
            throw new IllegalStateException("Expression only supports filtering on two levels of resource ancestry");
        }
    }

    private void parseExpression_resourceContext(String value, String[] tokens, String nextToken)
        throws InvalidExpressionException {
        if (nextToken.equals("id")) {
            populatePredicateCollections(getResourceRelativeContextToken() + ".id", value);
        } else if (nextToken.equals("name")) {
            populatePredicateCollections(getResourceRelativeContextToken() + ".name", value);
        } else if (nextToken.equals("version")) {
            populatePredicateCollections(getResourceRelativeContextToken() + ".version", value);
        } else if (nextToken.equals("type")) {
            context = ParseContext.ResourceType;
        } else if (nextToken.startsWith("trait")) {
            context = ParseContext.Trait;
            parseIndex--; // undo auto-inc, since this context requires element re-parse
        } else if (nextToken.startsWith("pluginconfiguration")) {
            context = ParseContext.Configuration;
            subcontext = ParseSubContext.PluginConfiguration;
            parseIndex--; // undo auto-inc, since this context requires element re-parse
        } else if (nextToken.startsWith("resourceconfiguration")) {
            context = ParseContext.Configuration;
            subcontext = ParseSubContext.ResourceConfiguration;
            parseIndex--; // undo auto-inc, since this context requires element re-parse
        } else {
            throw new InvalidExpressionException("Invalid 'resource' subexpression: "
                + PrintUtils.getDelimitedString(tokens, parseIndex, "."));
        }
    }

    // used to auto-generate unique names for bind variables
    private int counter = 0;

    private String getNextArgumentName() {
        counter++;
        String argumentName = "arg" + counter;
        return argumentName;
    }

    private String getLastArgumentName() {
        String argumentName = "arg" + (counter);
        return argumentName;
    }

    /*
     * the following two methods are used to add data to the appropriate predicate maps (whereConditions and
     * whereReplacements);
     *
     * it will only add data to the predicate list groupByElements if necessary, as determined by the instance-level
     * isGroupBy field or the explicitly overriding groupBy 3rd argument
     */
    private void populatePredicateCollections(String predicateName, Object value) {
        populatePredicateCollections(predicateName, value, isGroupBy);
    }

    private void populatePredicateCollections(String predicateName, Object value, boolean groupBy) {
        if (groupBy) {
            groupByElements.add(predicateName);
        } else {
            String argumentName = getNextArgumentName();

            whereConditions.put(predicateName, argumentName);
            whereReplacements.put(argumentName, value);
        }

        // always see if the user wants a portion of this, instead of an exact match
        context = ParseContext.StringMatch;
    }

    @SuppressWarnings("unchecked")
    public void execute() {
        if (isInvalid) {
            throw new IllegalStateException("This evaluator previously threw an exception and can no longer be used");
        }

        // if there are no expressions, leave the default value for computedJPQLStatement
        if (this.expressionCount == 0) {
            return;
        }

        // build the initial query
        String selectExpression = getQuerySelectExpression(false);

        String queryStr = "SELECT " + selectExpression + " FROM Resource res ";
        queryStr += getQueryJoinConditions();
        queryStr += getQueryWhereConditions();
        queryStr += getQueryGroupBy(selectExpression);
        queryStr = queryStr.trim();

        // always save this query, group query is conditionally saved below
        computedJPQLStatement = queryStr;

        /*
         * one or more passed expressions were pivots, thus we have to query the database against, N times, once for
         * each unique N-tuple of results we got from executing the first query
         */
        if (groupByElements.size() > 0) {
            // only group the group as necessary
            String groupQueryStr = "SELECT " + getQuerySelectExpression(true) + " FROM Resource res ";
            groupQueryStr += getQueryJoinConditions();
            for (String groupedElement : groupByElements) {
                String argumentName = getNextArgumentName();
                whereConditions.put(groupedElement, argumentName);
            }

            groupQueryStr += getQueryWhereConditions();

            computedJPQLGroupStatement = groupQueryStr;
        }

        // mark processing complete, so getComputed* methods return successfully
        resultsComputed = true;
    }

    public Iterator<ExpressionEvaluator.Result> iterator() {
        if (resultsComputed == false) {
            execute();
        }

        if (groupByElements.size() == 0) {
            return new SingleQueryIterator();
        } else {
            return new MultipleQueryIterator();
        }
    }

    private class SingleQueryIterator implements Iterator<ExpressionEvaluator.Result> {
        boolean firstTime = true;

        public boolean hasNext() {
            return firstTime;
        }

        public ExpressionEvaluator.Result next() {
            log.debug("SingleQueryIterator: '" + computedJPQLStatement + "'");
            List<Integer> results = getSingleResultList(computedJPQLStatement);
            firstTime = false;

            return new ExpressionEvaluator.Result(results);
        }

        public void remove() /* no-op */
        {
        }
    }

    /*
     * each result is a unique pivot consisting of an N-tuple result, returned to us as an array ordered by the index
     * they were added to the predicate list groupByElements
     */
    private class MultipleQueryIterator implements Iterator<ExpressionEvaluator.Result> {
        @SuppressWarnings("unchecked")
        List uniqueTuples;
        int index;

        @SuppressWarnings("unchecked")
        public MultipleQueryIterator() {
            log.debug("MultipleQueryIterator: '" + computedJPQLStatement + "'");
            this.uniqueTuples = getSingleResultList(computedJPQLStatement);
            this.index = 0;
        }

        public boolean hasNext() {
            return (index < uniqueTuples.size());
        }

        public ExpressionEvaluator.Result next() {
            int i = 0;
            Object nextResult = uniqueTuples.get(index++);

            if (nextResult == null) {
                return null;
            }

            Object[] groupByExpression;
            if (nextResult.getClass().isArray()) {
                groupByExpression = (Object[]) nextResult;
            } else {
                groupByExpression = new Object[] { nextResult };
            }

            /*
             * we built the basic structure earlier, now all we have to do is iterate over the unique N-tuples and set
             * the bind variables; conveniently, map semantics will, for each named group element, eject the current
             * replacement value for the newly added one in this iteration
             */
            for (String groupedElement : groupByElements) {
                String bindArgumentName = whereConditions.get(groupedElement);
                whereReplacements.put(bindArgumentName, groupByExpression[i++]);
            }

            log.debug("MultipleQueryIterator: '" + computedJPQLGroupStatement + "'");
            List<Integer> results = getSingleResultList(computedJPQLGroupStatement);

            return new ExpressionEvaluator.Result(results, PrintUtils.getDelimitedString(groupByExpression, 0, ","));
        }

        public void remove() /* no-op */
        {
        }
    }

    /*
     * given a JPQL query in string form, and assuming the predicate map whereRepalcements is populated correctly,
     * return the result list:  -- if no groupBy expressions are present, this will return a collection of Resource
     * objects   -- if at least one groupBy expression was used, it will return an Object[] representing a     unique
     * combination of value N-tuples returned from the set of N-pivoted expressions
     */
    @SuppressWarnings("unchecked")
    private List<Integer> getSingleResultList(String queryStr) {
        if (isTestMode) {
            return Collections.emptyList();
        }

        Query query = entityManagerFacade.createQuery(queryStr);

        for (Map.Entry<String, Object> replacement : whereReplacements.entrySet()) {
            String bindArgument = replacement.getKey();
            Object bindValue = replacement.getValue();

            /*
             * bind as the most appropriate type; today only integers and strings are supported
             * 
             * note: this is only meant to be a short-term solution to work around JBNADM-3141;
             *       the appropriate/proper solution would be to include type information into
             *       the whereReplacements map during the parse, and then switch on the type to
             *       cast the data to the appropriate type during the binding here
             */
            try {
                query.setParameter(bindArgument, Integer.valueOf(bindValue.toString()));
            } catch (NumberFormatException nfe) {
                query.setParameter(bindArgument, bindValue);
            }
        }

        return query.getResultList();
    }

    private String getQuerySelectExpression(boolean returnDefault) {
        String selectExpression = "";
        if ((returnDefault == false) && (groupByElements.size() > 0)) {
            selectExpression = groupByElements.get(0);
            for (int i = 1; i < groupByElements.size(); i++) {
                selectExpression += ", " + groupByElements.get(i);
            }
        } else {
            selectExpression = "res.id";
        }

        return selectExpression;
    }

    private String getQueryGroupBy(String selectExpression) {
        if (groupByElements.size() > 0) {
            return " GROUP BY " + selectExpression;
        }

        return "";
    }

    private String getQueryJoinConditions() {
        String result = "";
        for (JoinCondition joinCondition : joinConditions) {
            result += " JOIN res" + joinCondition.subexpression + " " + joinCondition.alias;
            if (joinCondition == JoinCondition.SCHEDULES) {
                result += " JOIN " + joinCondition.alias + ".definition " + METRIC_DEF_ALIAS;
                result += ", MeasurementDataTrait " + TRAIT_ALIAS + " ";
            } else if (joinCondition == JoinCondition.PLUGIN_CONFIGURATION
                || joinCondition == JoinCondition.RESOURCE_CONFIGURATION) {
                result += ", PropertySimple " + PROP_SIMPLE_ALIAS + " ";
            }
        }

        return result;
    }

    private String getQueryWhereConditions() {
        String result = "";
        if (whereConditions.size() > 0) {
            result += " WHERE ";
            boolean first = true;
            for (Map.Entry<String, String> whereCondition : whereConditions.entrySet()) {
                if (!first) {
                    result += " AND ";
                }

                String whereConditionOperator = " = ";

                Object bindValue = whereReplacements.get(whereCondition.getValue());
                if (bindValue != null) {
                    /*
                     * there will *not* necessarily be a replacement value ready at this point in the processing; these
                     * get set earlier if this is a SingleQuery, but a MultipleQuery will set these later based on the
                     * results of the pivoted query; so, only attempt processing here if necessary
                     */
                    String bindValueAsString = bindValue.toString();
                    if ((bindValueAsString != null) // whereConditionValue is null when whereCondition isn't a groupBy expression
                        && (bindValueAsString.startsWith("%") || bindValueAsString.endsWith("%"))) {
                        whereConditionOperator = " like ";
                    }
                }

                result += whereCondition.getKey() + whereConditionOperator + ":" + whereCondition.getValue() + " ";
                first = false;
            }
        }

        if (whereStatics.size() > 0) {
            boolean first;
            if (result.length() == 0) {
                result += " WHERE ";
                first = true;
            } else {
                first = false;
            }

            for (String whereStatic : whereStatics) {
                if (!first) {
                    result += " AND ";
                }

                result += whereStatic + " ";
                first = false;
            }
        }

        return result;
    }

    private List<String> tokenizeCondition(String condition) {
        List<String> originalTokens = new ArrayList<String>();
        String[] outerTokens = condition.split(" ");
        for (String topToken : outerTokens) {
            int bracketIndex = topToken.indexOf('[');
            String preBracket;
            String bracketed;
            if (bracketIndex != -1) {
                preBracket = topToken.substring(0, bracketIndex);
                bracketed = topToken.substring(bracketIndex);
            } else {
                preBracket = topToken;
                bracketed = "";
            }
            // If there's a '[', tokenize on dots only in the portion before the '['; this is necessary because
            // config prop names and trait names can both potentially contain dots.
            String[] innerTokens = preBracket.split("\\.");
            if (bracketed != null)
                innerTokens[innerTokens.length - 1] += bracketed;
            originalTokens.addAll(Arrays.asList(innerTokens));
        }
        return originalTokens;
    }

    private String parseTraitName(List<String> originalTokens) throws InvalidExpressionException {
        String prefix = "trait";
        String suffix = originalTokens.get(parseIndex).substring(prefix.length());
        if (suffix.length() < 3) {
            throw new InvalidExpressionException("Unrecognized trait name '" + suffix + "'");
        }
        if ((suffix.charAt(0) != '[') || (suffix.charAt(suffix.length() - 1) != ']')) {
            throw new InvalidExpressionException("Trait name '" + suffix
                + "' must be contained within '[' and ']' characters");
        }
        return suffix.substring(1, suffix.length() - 1);
    }

    private void validateSubExpressionAgainstPreviouslySeen(String normalizedSubExpression, boolean grouped)
        throws InvalidExpressionException {
        normalizedSubExpression = stripFunctionSuffix(normalizedSubExpression);
        if (grouped) {
            if (groupedSubExpressions.contains(normalizedSubExpression)) {
                throw new InvalidExpressionException(
                    "Redundant 'groupby' expressions - these expressions must be unique");
            }
            if (simpleSubExpressions.contains(normalizedSubExpression)) {
                throw new InvalidExpressionException("Can not group by the same condition you are filtering on");
            }
            groupedSubExpressions.add(normalizedSubExpression);
        } else {
            if (groupedSubExpressions.contains(normalizedSubExpression)) {
                throw new InvalidExpressionException("Can not group by the same condition you are filtering on");
            }
            simpleSubExpressions.add(normalizedSubExpression);
        }
    }

    private final String[] functions = { "contains", "startswith", "endswith" };

    private String stripFunctionSuffix(String expression) {
        for (String function : functions) {
            if (expression.endsWith(function)) {
                return expression.substring(0, expression.length() - function.length());
            }
        }
        return expression;
    }

    private static class PrintUtils {
        public static String getDelimitedString(Object[] tokens, int fromIndex, String delimiter) {
            StringBuilder builder = new StringBuilder();

            for (int j = fromIndex; j < tokens.length; j++) {
                if (j != fromIndex) {
                    builder.append(delimiter);
                }

                builder.append(tokens[j].toString());
            }

            return builder.toString();
        }
    }
}