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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.group.DuplicateExpressionTypeException;
import org.rhq.core.domain.resource.group.InvalidExpressionException;
import org.rhq.enterprise.server.common.EntityManagerFacadeLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.QueryUtility;

public class ExpressionEvaluator implements Iterable<ExpressionEvaluator.Result> {

    private final Log log = LogFactory.getLog(ExpressionEvaluator.class);

    private static final String INVALID_EXPRESSION_FORM_MSG = "Expression must be in one of the follow forms: " + //
        "'groupby condition', 'memberof = groupname', 'condition = value', 'empty condition', 'not empty condition";

    private static final String PROP_SIMPLE_ALIAS = "simple";
    private static final String PROP_SIMPLE_DEF_ALIAS = "simpleDef";
    private static final String TRAIT_ALIAS = "trait";
    private static final String METRIC_DEF_ALIAS = "def";

    private enum JoinCondition {
        RESOURCE_CONFIGURATION(".resourceConfiguration", "conf"), //
        PLUGIN_CONFIGURATION(".pluginConfiguration", "pluginConf"), //
        SCHEDULES(".schedules", "sched"), //
        RESOURCE_CHILD(".childResources", "child"), //
        AVAILABILITY(".currentAvailability", "avail"), //
        RESOURCE_CONFIGURATION_DEFINITION(".resourceType.resourceConfigurationDefinition", "confDef"), //
        PLUGIN_CONFIGURATION_DEFINITION(".resourceType.pluginConfigurationDefinition", "pluginConfDef");

        String subexpression;
        String alias;

        private JoinCondition(String subexpression, String alias) {
            this.subexpression = subexpression;
            this.alias = alias;
        }
    }

    private Map<JoinCondition, ResourceRelativeContext> joinConditions;
    private Map<String, String> whereConditions;
    private Map<String, Object> whereReplacements;
    private Map<String, Class<?>> whereReplacementTypes;
    private Set<String> whereStatics;
    private List<String> groupByElements;
    private List<String> memberOfElements;

    private List<String> simpleSubExpressions;
    private List<String> groupedSubExpressions;
    private List<String> memberSubExpressions;

    private int expressionCount;
    private boolean isInvalid;
    private boolean isTestMode;
    private boolean resultsComputed;

    private String computedJPQLStatement;
    private String computedJPQLGroupStatement;

    private EntityManagerFacadeLocal entityManagerFacade;

    private Map<String, String> resourceExpressions = new TreeMap<String, String>();

    public ExpressionEvaluator() {
        /*
         * used LinkedHashMap for whereConditions on purpose so that the iterator will return them in the same order
         * they were added to the list, this ensures that the getComputed*Statement() methods will construct the
         * generated JPQL in the order that the bind parameter names are generated;
         *
         * the query technology, of course, doesn't require this...but it makes generating a test suite and verifying
         * expected output a lot easier
         */
        joinConditions = new HashMap<JoinCondition, ResourceRelativeContext>();
        whereConditions = new LinkedHashMap<String, String>();
        whereReplacements = new HashMap<String, Object>();
        whereReplacementTypes = new HashMap<String, Class<?>>();
        whereStatics = new LinkedHashSet<String>();
        groupByElements = new ArrayList<String>();
        memberOfElements = new ArrayList<String>();

        simpleSubExpressions = new ArrayList<String>();
        groupedSubExpressions = new ArrayList<String>();
        memberSubExpressions = new ArrayList<String>();

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

        resourceExpressions.put("res.id", "resource id");
        resourceExpressions.put("child.id", "resource id");
        resourceExpressions.put("res.parentResource.id", "resource id");
        resourceExpressions.put("res.parentResource.parentResource.id", "resource id");
        resourceExpressions.put("res.parentResource.parentResource.parentResource.id", "resource id");
        resourceExpressions.put("res.parentResource.parentResource.parentResource.parentResource.id", "resource id");

        resourceExpressions.put("res.name", "resource name");
        resourceExpressions.put("child.name", "resource name");
        resourceExpressions.put("res.parentResource.name", "resource name");
        resourceExpressions.put("res.parentResource.parentResource.name", "resource name");
        resourceExpressions.put("res.parentResource.parentResource.parentResource.name", "resource name");
        resourceExpressions
            .put("res.parentResource.parentResource.parentResource.parentResource.name", "resource name");

        resourceExpressions.put("res.version", "resource version");
        resourceExpressions.put("child.version", "resource version");
        resourceExpressions.put("res.parentResource.version", "resource version");
        resourceExpressions.put("res.parentResource.parentResource.version", "resource version");
        resourceExpressions.put("res.parentResource.parentResource.parentResource.version", "resource version");
        resourceExpressions.put("res.parentResource.parentResource.parentResource.parentResource.version",
            "resource version");

        resourceExpressions.put("res.resourceType.plugin", "resource type");
        resourceExpressions.put("res.resourceType.name", "resource type");
        resourceExpressions.put("child.resourceType.plugin", "resource type");
        resourceExpressions.put("child.resourceType.name", "resource type");
        resourceExpressions.put("res.parentResource.resourceType.plugin", "resource type");
        resourceExpressions.put("res.parentResource.resourceType.name", "resource type");
        resourceExpressions.put("res.parentResource.parentResource.resourceType.plugin", "resource type");
        resourceExpressions.put("res.parentResource.parentResource.resourceType.name", "resource type");
        resourceExpressions
            .put("res.parentResource.parentResource.parentResource.resourceType.plugin", "resource type");
        resourceExpressions.put("res.parentResource.parentResource.parentResource.resourceType.name", "resource type");
        resourceExpressions.put("res.parentResource.parentResource.parentResource.parentResource.resourceType.plugin",
            "resource type");
        resourceExpressions.put("res.parentResource.parentResource.parentResource.parentResource.resourceType.name",
            "resource type");

        resourceExpressions.put("res.resourceType.category", "resource category");
        resourceExpressions.put("child.resourceType.category", "resource category");
        resourceExpressions.put("res.parentResource.resourceType.category", "resource category");
        resourceExpressions.put("res.parentResource.parentResource.resourceType.category", "resource category");
        resourceExpressions.put("res.parentResource.parentResource.parentResource.resourceType.category",
            "resource category");
        resourceExpressions.put(
            "res.parentResource.parentResource.parentResource.parentResource.resourceType.category",
            "resource category");

        resourceExpressions.put("avail.availabilityType", "availability");
        resourceExpressions.put("child.avail.availabilityType", "availability");
        resourceExpressions.put("res.parentResource.avail.availabilityType", "availability");
        resourceExpressions.put("res.parentResource.parentResource.avail.availabilityType", "availability");
        resourceExpressions.put("res.parentResource.parentResource.parentResource.avail.availabilityType",
            "availability");
        resourceExpressions.put(
            "res.parentResource.parentResource.parentResource.parentResource.avail.availabilityType", "availability");

        resourceExpressions.put("trait.value", "trait");
        resourceExpressions.put("child.trait.value", "trait");
        resourceExpressions.put("res.parentResource.trait.value", "trait");
        resourceExpressions.put("res.parentResource.parentResource.trait.value", "trait");
        resourceExpressions.put("res.parentResource.parentResource.parentResource.trait.value", "trait");
        resourceExpressions.put("res.parentResource.parentResource.parentResource.parentResource.trait.value", "trait");

        resourceExpressions.put("simple.name", "configuration");
        resourceExpressions.put("child.simple.name", "configuration");
        resourceExpressions.put("res.parentResource.simple.name", "configuration");
        resourceExpressions.put("res.parentResource.parentResource.simple.name", "configuration");
        resourceExpressions.put("res.parentResource.parentResource.parentResource.simple.name", "configuration");
        resourceExpressions.put("res.parentResource.parentResource.parentResource.parentResource.simple.name",
            "configuration");
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
     * @throws org.rhq.core.domain.resource.group.InvalidExpressionException if the expression can not be parsed for any reason, the message will try to
     *                                    get the details as to the parse failure
     */
    public ExpressionEvaluator addExpression(String expression) throws InvalidExpressionException {
        if (isInvalid) {
            throw new IllegalStateException("This evaluator previously threw an exception and can no longer be used");
        }

        try {
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
        Modifier(false), // includes 'empty', 'not', and 'pivot'
        Resource(false), //
        ResourceParent(false), //
        ResourceGrandParent(false), //
        ResourceGreatGrandParent(false), //
        ResourceGreatGreatGrandParent(false), //
        ResourceChild(false), //
        ResourceType(false), //
        Availability(true), //
        Trait(true), //
        Configuration(true), // includes 'pluginConfiguration' and 'resourceConfiguration'
        StringMatch(true), //         
        END(true), //
        Membership(true);

        private boolean canTerminateExpression;

        private ParseContext(boolean canTerminateExpression) {
            this.canTerminateExpression = canTerminateExpression;
        }

        public boolean isExpressionTerminator() {
            return this.canTerminateExpression;
        }
    }

    private enum ParseSubContext {
        Negated, // only relevant for Modifier context
        NotEmpty, // only relevant for Modifier context
        Empty, // only relevant for Modifier context
        Pivot, // only relevant for Modifier context
        PluginConfiguration, // only relevant for Configuration context
        ResourceConfiguration; // only relevant for Configuration context
    }

    private enum ComparisonType {
        NONE, // expression in the form of 'groupBy condition'
        EQUALS, // expression in the form of 'condition = value'
        EMPTY, // expression in the form of 'empty value'
        NOT_EMPTY; // expression in the form of 'not empty value'
    }

    private enum Literal {
        NULL, NOTNULL;
    }

    private ParseContext context = ParseContext.BEGIN;
    private ParseSubContext subcontext = null;
    private int parseIndex = 0;
    private boolean isGroupBy = false;
    private boolean isMemberOf = false;
    private ComparisonType comparisonType = null;
    private Class<?> expressionType;

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

        if (expression.contains("<"))
            throw new InvalidExpressionException("Expressions must not contain the '<' character");

        String condition;
        String value = null;

        /*
         * instead of building '= value' parsing into the below algorithm, let's chop this off early and store it; this
         * makes the rest of the parsing a bit simpler because some ParseContexts need the value immediately in order to
         * properly build up internal maps / constructs to be used in generating the requisite JPQL statement
         *
         * however, since '=' can occur in the names of configuration properties and trait names, we need
         * to process from the end of the word skipping over all characters that are inside brackets
         */
        int equalsIndex = -1;
        boolean insideBrackets = false;
        for (int i = expression.length() - 1; i >= 0; i--) {
            char next = expression.charAt(i);
            if (insideBrackets) {
                if (next == '[') {
                    insideBrackets = false;
                }
            } else {
                if (next == ']') {
                    insideBrackets = true;
                } else if (next == '=') {
                    equalsIndex = i;
                    break;
                }
            }
        }

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
        log.debug("TOKENS: " + Arrays.asList(tokens));

        /*
         * build the normalized expression outside of the parse, to keep the parse code as clean as possible;
         * however, this string will be used to determine if the expression being added to this evaluator, based
         * on whether it's grouped or not, compared against all expressions seen so far, is valid
         */
        StringBuilder normalizedSubExpressionBuilder = new StringBuilder();
        for (String subExpressionToken : tokens) {
            // do not add modifiers to the normalized expression
            if (subExpressionToken.equals("groupby")) {
                continue;
            } else if (subExpressionToken.equals("memberof")) {
                continue;
            } else if (subExpressionToken.equals("not")) {
                continue;
            } else if (subExpressionToken.equals("empty")) {
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
        isGroupBy = false; // this needs to be reset each time a new expression is added
        isMemberOf = false; // this needs to be reset each time a new expression is added
        comparisonType = ComparisonType.EQUALS; // assume equals, unless "(not) empty" found during the parse

        deepestResourceContext = null;
        expressionType = String.class;

        for (; parseIndex < tokens.length; parseIndex++) {
            String nextToken = tokens[parseIndex];

            if (context == ParseContext.BEGIN) {
                if (nextToken.equals("resource")) {
                    context = ParseContext.Resource;
                    deepestResourceContext = context;

                } else if (nextToken.equals("memberof")) {
                    context = ParseContext.Membership; // ensure proper expression termination
                    String groupName = value;

                    if (null == groupName || groupName.isEmpty() || "=".equals(groupName)) {
                        throw new InvalidExpressionException(INVALID_EXPRESSION_FORM_MSG);
                    }

                    validateSubExpressionAgainstPreviouslySeen(groupName, false, true);
                    isMemberOf = true;
                    populatePredicateCollections(null, groupName);

                } else if (nextToken.equals("groupby")) {
                    context = ParseContext.Modifier;
                    subcontext = ParseSubContext.Pivot;

                } else if (nextToken.equals("not")) {

                    context = ParseContext.Modifier;
                    subcontext = ParseSubContext.Negated;
                    // 'not' must be followed by 'empty' today, but we won't know until next parse iteration
                    // furthermore, we may support other forms of negated expressions in the future

                } else if (nextToken.equals("empty")) {
                    context = ParseContext.Modifier;
                    subcontext = ParseSubContext.Empty;

                } else {
                    throw new InvalidExpressionException(
                        "Expression must either start with 'resource', 'groupby', 'empty', or 'not empty' tokens");
                }
            } else if (context == ParseContext.Modifier) {
                if (subcontext == ParseSubContext.Negated) {
                    if (nextToken.equals("empty")) {
                        subcontext = ParseSubContext.NotEmpty;
                    } else {
                        throw new InvalidExpressionException(
                            "Expression starting with 'not' must be followed by the 'empty' token");
                    }
                } else {
                    // first check for valid forms given the subcontext
                    if (subcontext == ParseSubContext.Pivot || subcontext == ParseSubContext.Empty
                        || subcontext == ParseSubContext.NotEmpty) {
                        if (value != null) {
                            // these specific types of 'modified' expressions must NOT HAVE "= <value>" part
                            throw new InvalidExpressionException(INVALID_EXPRESSION_FORM_MSG);
                        }
                    }

                    // then perform individual processing based on current subcontext
                    if (subcontext == ParseSubContext.Pivot) {
                        // validates the uniqueness of the subexpression after checking for INVALID_EXPRESSION_FORM_MSG
                        validateSubExpressionAgainstPreviouslySeen(normalizedSubExpression, true, false);
                        isGroupBy = true;
                        comparisonType = ComparisonType.NONE;
                    } else if (subcontext == ParseSubContext.NotEmpty) {
                        comparisonType = ComparisonType.NOT_EMPTY;
                    } else if (subcontext == ParseSubContext.Empty) {
                        comparisonType = ComparisonType.EMPTY;
                    } else {
                        throw new InvalidExpressionException("Unknown or unsupported ParseSubContext[" + subcontext
                            + "] for ParseContext[" + context + "]");
                    }

                    if (nextToken.equals("resource")) {
                        context = ParseContext.Resource;
                        deepestResourceContext = context;
                    } else {
                        throw new InvalidExpressionException(
                            "Grouped expressions must be followed by the 'resource' token");
                    }
                }
            } else if (context == ParseContext.Resource) {
                if (comparisonType == ComparisonType.EQUALS) {
                    if (value == null) {
                        // EQUALS filter expressions must HAVE "= <value>" part
                        throw new InvalidExpressionException(INVALID_EXPRESSION_FORM_MSG);
                    }
                    validateSubExpressionAgainstPreviouslySeen(normalizedSubExpression, false, false);
                }

                if (nextToken.equals("parent")) {
                    context = ParseContext.ResourceParent;
                    deepestResourceContext = context;
                } else if (nextToken.equals("grandparent")) {
                    context = ParseContext.ResourceGrandParent;
                    deepestResourceContext = context;
                } else if (nextToken.equals("greatgrandparent")) {
                    context = ParseContext.ResourceGreatGrandParent;
                    deepestResourceContext = context;
                } else if (nextToken.equals("greatgreatgrandparent")) {
                    context = ParseContext.ResourceGreatGreatGrandParent;
                    deepestResourceContext = context;
                } else if (nextToken.equals("child")) {
                    context = ParseContext.ResourceChild;
                    deepestResourceContext = context;
                } else {
                    parseExpression_resourceContext(value, tokens, nextToken);
                }
            } else if ((context == ParseContext.ResourceParent) || (context == ParseContext.ResourceGrandParent)
                || (context == ParseContext.ResourceGreatGrandParent)
                || (context == ParseContext.ResourceGreatGreatGrandParent) || (context == ParseContext.ResourceChild)) {
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
            } else if (context == ParseContext.Availability) {
                AvailabilityType type = null;
                if (isGroupBy == false) {
                    if (value == null) {
                        // pass through, NULL elements now supported
                    } else if ("up".equalsIgnoreCase(value)) {
                        type = AvailabilityType.UP;
                    } else if ("down".equalsIgnoreCase(value)) {
                        type = AvailabilityType.DOWN;
                    } else if ("disabled".equalsIgnoreCase(value)) {
                        type = AvailabilityType.DISABLED;
                    } else if ("unknown".equalsIgnoreCase(value)) {
                        type = AvailabilityType.UNKNOWN;
                    } else {
                        throw new InvalidExpressionException("Invalid 'resource.availability' comparision value, "
                            + "only 'UP''DOWN''DISABLED''UNKNOWN' are valid values");
                    }
                }
                addJoinCondition(JoinCondition.AVAILABILITY);
                populatePredicateCollections(JoinCondition.AVAILABILITY.alias + ".availabilityType", type);
            } else if (context == ParseContext.Trait) {
                // SELECT res.id FROM Resource res JOIN res.schedules sched, sched.definition def, MeasurementDataTrait trait
                // WHERE def.name = :arg1 AND trait.value = :arg2 AND trait.schedule = sched AND trait.id.timestamp =
                // (SELECT max(mdt.id.timestamp) FROM MeasurementDataTrait mdt WHERE sched.id = mdt.schedule.id)
                String traitName = parseTraitName(originalTokens);
                addJoinCondition(JoinCondition.SCHEDULES);
                populatePredicateCollections(METRIC_DEF_ALIAS + ".name", "%" + traitName + "%", false, false);
                populatePredicateCollections(TRAIT_ALIAS + ".value", value);
                whereStatics.add(TRAIT_ALIAS + ".schedule = " + JoinCondition.SCHEDULES.alias);
                whereStatics.add(TRAIT_ALIAS
                    + ".id.timestamp = (SELECT max(mdt.id.timestamp) FROM MeasurementDataTrait mdt WHERE "
                    + JoinCondition.SCHEDULES.alias + ".id = mdt.schedule.id)");
            } else if (context == ParseContext.Configuration) {
                String prefix;
                JoinCondition joinCondition;
                JoinCondition definitionJoinCondition;

                if (subcontext == ParseSubContext.PluginConfiguration) {
                    prefix = "pluginconfiguration";
                    joinCondition = JoinCondition.PLUGIN_CONFIGURATION;
                    definitionJoinCondition = JoinCondition.PLUGIN_CONFIGURATION_DEFINITION;
                } else if (subcontext == ParseSubContext.ResourceConfiguration) {
                    prefix = "resourceconfiguration";
                    joinCondition = JoinCondition.RESOURCE_CONFIGURATION;
                    definitionJoinCondition = JoinCondition.RESOURCE_CONFIGURATION_DEFINITION;
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

                addJoinCondition(joinCondition);
                addJoinCondition(definitionJoinCondition);

                populatePredicateCollections(PROP_SIMPLE_ALIAS + ".name", propertyName, false, false);
                populatePredicateCollections(PROP_SIMPLE_ALIAS + ".stringValue", value);

                whereStatics.add(PROP_SIMPLE_ALIAS + ".configuration = " + joinCondition.alias);
                whereStatics.add(PROP_SIMPLE_DEF_ALIAS + ".configurationDefinition = " + definitionJoinCondition.alias);
                whereStatics.add(PROP_SIMPLE_ALIAS + ".name = " + PROP_SIMPLE_DEF_ALIAS + ".name");
                whereStatics.add(PROP_SIMPLE_DEF_ALIAS + ".type != 'PASSWORD'");

            } else if (context == ParseContext.StringMatch) {
                if (expressionType != String.class) {
                    throw new InvalidExpressionException(
                        "Can not apply a string function to an expression that resolves to "
                            + expressionType.getSimpleName());
                }

                String lastArgumentName = getLastArgumentName();
                String argumentValue = (String) whereReplacements.get(lastArgumentName);

                if (nextToken.equals("startswith")) {
                    argumentValue = QueryUtility.escapeSearchParameter(argumentValue) + "%";
                } else if (nextToken.equals("endswith")) {
                    argumentValue = "%" + QueryUtility.escapeSearchParameter(argumentValue);
                } else if (nextToken.equals("contains")) {
                    argumentValue = "%" + QueryUtility.escapeSearchParameter(argumentValue) + "%";
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

    private enum ResourceRelativeContext {
        Resource("res"), //
        ResourceParent("res.parentResource"), //
        ResourceGrandParent("res.parentResource.parentResource"), //
        ResourceGreatGrandParent("res.parentResource.parentResource.parentResource"), //
        ResourceGreatGreatGrandParent("res.parentResource.parentResource.parentResource.parentResource"), //
        ResourceChild("child");

        public String pathToken;

        private ResourceRelativeContext(String pathToken) {
            this.pathToken = pathToken;
        }
    }

    private void addJoinCondition(JoinCondition condition) {
        joinConditions.put(condition, getResourceRelativeContext());
    }

    private String getResourceRelativeContextToken() {
        return getResourceRelativeContext().pathToken;
    }

    private ResourceRelativeContext getResourceRelativeContext() {
        if (deepestResourceContext == ParseContext.Resource) {
            return ResourceRelativeContext.Resource;
        } else if (deepestResourceContext == ParseContext.ResourceParent) {
            return ResourceRelativeContext.ResourceParent;
        } else if (deepestResourceContext == ParseContext.ResourceGrandParent) {
            return ResourceRelativeContext.ResourceGrandParent;
        } else if (deepestResourceContext == ParseContext.ResourceGreatGrandParent) {
            return ResourceRelativeContext.ResourceGreatGrandParent;
        } else if (deepestResourceContext == ParseContext.ResourceGreatGreatGrandParent) {
            return ResourceRelativeContext.ResourceGreatGreatGrandParent;
        } else if (deepestResourceContext == ParseContext.ResourceChild) {
            // populate child stuff
            joinConditions.put(JoinCondition.RESOURCE_CHILD, ResourceRelativeContext.Resource);
            return ResourceRelativeContext.ResourceChild;
        } else {
            throw new IllegalStateException("Expression only supports filtering on two levels of resource ancestry");
        }
    }

    private void parseExpression_resourceContext(String value, String[] tokens, String nextToken)
        throws InvalidExpressionException {
        if (nextToken.equals("id")) {
            expressionType = Integer.class;
            populatePredicateCollections(getResourceRelativeContextToken() + ".id", value);
        } else if (nextToken.equals("name")) {
            populatePredicateCollections(getResourceRelativeContextToken() + ".name", value);
        } else if (nextToken.equals("version")) {
            populatePredicateCollections(getResourceRelativeContextToken() + ".version", value);
        } else if (nextToken.equals("type")) {
            context = ParseContext.ResourceType;
        } else if (nextToken.startsWith("availability")) {
            context = ParseContext.Availability;
            parseIndex--; // undo auto-inc, since this context requires element re-parse
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
    private void populatePredicateCollections(String predicateName, Object value) throws InvalidExpressionException {
        populatePredicateCollections(predicateName, value, isGroupBy, isMemberOf);
    }

    private void populatePredicateCollections(String predicateName, Object value, boolean groupBy, boolean memberOf)
        throws InvalidExpressionException {
        if (groupBy) {
            groupByElements.add(predicateName);
        } else if (memberOf) {
            memberOfElements.add((String) value); // this is the group name in this situation
        } else {
            String argumentName = getNextArgumentName();

            // change the value as necessary based on the comparison type
            if (comparisonType == ComparisonType.EMPTY) {
                /*
                 * a single parse context may populate several predicate collections,
                 * but we want to make sure we are only performing extra comparison
                 * computation on values representing the "empty" RHS of the expression
                 */
                if (value == null) {
                    value = Literal.NULL;
                }
            } else if (comparisonType == ComparisonType.NOT_EMPTY) {
                // see comment for ComparisonType.EMPTY logic just above this block
                if (value == null) {
                    value = Literal.NOTNULL;
                }
            } else if (comparisonType == ComparisonType.EQUALS || comparisonType == ComparisonType.NONE) {
                // pass through
            } else {
                throw new InvalidExpressionException("Unknown or unsupported ComparisonType[" + comparisonType
                    + "] for predicate population");
            }

            if (resourceExpressions.containsKey(predicateName) && whereConditions.containsKey(predicateName)) {
                throw new DuplicateExpressionTypeException(resourceExpressions.get(predicateName));
            }

            whereConditions.put(predicateName, argumentName);
            whereReplacements.put(argumentName, value);

            whereReplacementTypes.put(argumentName, expressionType);
        }

        // always see if the user wants a portion of this, instead of an exact match
        context = ParseContext.StringMatch;
    }

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

        @SuppressWarnings("unchecked")
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
        /*
         * support multi-layer capture groups for flexibility, today they resolve to:
         *     group(0) = "token operator :argX" // where operator is '=' or 'LIKE'
         *     group(1) = "operator :argX"
         *     group(2) = "operator"
         *     group(3) = ":argX"
         */
        private final static String nullHandlerPattern = "" + // the 'token' will go in front
            "\\s+" + // followed by some whitespace
            "((\\=|LIKE)" + // and either an '=' or the word 'like'
            "\\s+" + // followed by more whitespace
            "(:arg[0-9]*))"; // ending in ':argX' where X is some integer

        @SuppressWarnings("unchecked")
        List uniqueTuples;
        int index;

        public MultipleQueryIterator() {
            log.debug("MultipleQueryIterator: '" + computedJPQLStatement + "'");
            this.uniqueTuples = getSingleResultList(computedJPQLStatement);
            this.index = 0;
        }

        public boolean hasNext() {
            return (index < uniqueTuples.size());
        }

        @SuppressWarnings("unchecked")
        public ExpressionEvaluator.Result next() {
            int i = 0;
            Object nextResult = uniqueTuples.get(index++);

            Object[] groupByExpression;
            if (nextResult == null) {
                groupByExpression = new Object[] { null };
            } else if (nextResult.getClass().isArray()) {
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
                Object groupByExpressionElement = groupByExpression[i++];
                if (groupByExpressionElement == null) {
                    whereReplacements.remove(bindArgumentName);
                    String patternWtihArgument = "\\Q" + groupedElement + "\\E" + nullHandlerPattern;
                    Pattern nullHandler = Pattern.compile(patternWtihArgument);
                    Matcher nullMatcher = nullHandler.matcher(computedJPQLGroupStatement);
                    if (nullMatcher.find() == false) {
                        log.warn("Did not match for pivoted NULL result");
                        log.warn("Handler pattern was: " + patternWtihArgument);
                        log.warn("Computed statement was: " + computedJPQLGroupStatement);
                        return null; // default to classic, non-null-supported handling
                    }
                    log.debug("Dynamic replacement made for pivoted NULL result on subexpression bind argument '"
                        + bindArgumentName + "'");
                    log.debug("Orginal query: " + computedJPQLGroupStatement);
                    computedJPQLGroupStatement = nullMatcher.replaceFirst(groupedElement + " IS NULL ");
                    log.debug("Updated query: " + computedJPQLGroupStatement);
                } else {
                    whereReplacements.put(bindArgumentName, groupByExpressionElement);
                    whereReplacementTypes.put(bindArgumentName, String.class);
                }
            }

            /*
                Object bindValue = whereReplacements.get(whereCondition.getValue());
                if (bindValue == Literal.NOTNULL) {
                    result += whereCondition.getKey() + " IS NOT NULL ";
                    whereReplacements.remove(whereCondition.getValue()); // no longer needed, literal rendered here
                } else if (bindValue == Literal.NULL) {
                    result += whereCondition.getKey() + " IS NULL ";
                    whereReplacements.remove(whereCondition.getValue()); // no longer needed, literal rendered here
             */

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
     * objects   -- if at least one groupBy expression was used, it will return an Object[] representing a unique
     * combination of value N-tuples returned from the set of N-pivoted expressions
     */
    @SuppressWarnings("unchecked")
    private List getSingleResultList(String queryStr) {
        if (log.isDebugEnabled()) {
            String resolvedQuery = queryStr;

            for (Map.Entry<String, Object> replacement : whereReplacements.entrySet()) {
                String bindArgument = replacement.getKey();
                String bindValueAsString = replacement.getValue().toString();
                Class bindType = whereReplacementTypes.get(bindArgument);

                if (bindType.equals(Integer.class)) {
                    resolvedQuery = resolvedQuery.replace(":" + bindArgument, bindValueAsString);
                } else if (bindType.equals(String.class)) {
                    resolvedQuery = resolvedQuery.replace(":" + bindArgument, "'" + bindValueAsString + "'");
                } else {
                    throw new IllegalArgumentException("Unknown bindType " + bindType + " for " + bindArgument
                        + " having value " + bindValueAsString);
                }
            }

            log.debug("Query: " + resolvedQuery);
        }

        if (isTestMode) {
            return Collections.emptyList();
        }

        Query query = entityManagerFacade.createQuery(queryStr);

        for (Map.Entry<String, Object> replacement : whereReplacements.entrySet()) {
            String bindArgument = replacement.getKey();
            Object bindValue = replacement.getValue();
            Class bindType = whereReplacementTypes.get(bindArgument);

            if (bindType.equals(Integer.class)) {
                try {
                    query.setParameter(bindArgument, Integer.valueOf(bindValue.toString()));
                } catch (NumberFormatException nfe) {
                    query.setParameter(bindArgument, bindValue);
                }
            } else if (bindType.equals(String.class)) {
                query.setParameter(bindArgument, bindValue);
            } else {
                throw new IllegalArgumentException("Unknown bindType " + bindType + " for " + bindArgument
                    + " having value " + bindValue);
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

        // question: can we support multiple complex join clauses (schedules and plugin/resourceConfig)
        JoinCondition[] orderedConditionProcessing = new JoinCondition[] { JoinCondition.RESOURCE_CHILD,
            JoinCondition.AVAILABILITY, JoinCondition.SCHEDULES, JoinCondition.PLUGIN_CONFIGURATION,
            JoinCondition.PLUGIN_CONFIGURATION_DEFINITION, JoinCondition.RESOURCE_CONFIGURATION,
            JoinCondition.RESOURCE_CONFIGURATION_DEFINITION };

        /*
         * process JoinConditions in a specific order, because hibernate AST parsing requires
         * tokens to have been identified in the JPQL before their first use; in this case,
         * JoinCondition.RESOURCE_CHILD must be first because ANY of the others might be joining
         * down the resource hierarchy (note: joining up the resource hierarchy doesn't require
         * any special processing because it follows from the "many" to the "one" side of the
         * relationship, for instance "resource.parent.parent" for grandparents.
         */
        for (JoinCondition joinCondition : orderedConditionProcessing) {
            ResourceRelativeContext context = joinConditions.get(joinCondition);
            if (context == null) {
                continue;
            }
            result += " JOIN " + context.pathToken + joinCondition.subexpression + " " + joinCondition.alias;
            if (joinCondition == JoinCondition.SCHEDULES) {
                result += " JOIN " + joinCondition.alias + ".definition " + METRIC_DEF_ALIAS;
                result += ", MeasurementDataTrait " + TRAIT_ALIAS + " ";
            } else if (joinCondition == JoinCondition.PLUGIN_CONFIGURATION
                || joinCondition == JoinCondition.RESOURCE_CONFIGURATION) {
                result += ", PropertySimple " + PROP_SIMPLE_ALIAS;
                result += ", PropertyDefinition " + PROP_SIMPLE_DEF_ALIAS;
            }
        }

        // finally, if we are narrowing by group membership, add the join on implicit groups
        if (!memberOfElements.isEmpty()) {
            result += " JOIN res.implicitGroups implicitGroup";
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

                Object bindValue = whereReplacements.get(whereCondition.getValue());
                if (bindValue == Literal.NOTNULL) {
                    result += whereCondition.getKey() + " IS NOT NULL ";
                    whereReplacements.remove(whereCondition.getValue()); // no longer needed, literal rendered here
                } else if (bindValue == Literal.NULL) {
                    result += whereCondition.getKey() + " IS NULL ";
                    whereReplacements.remove(whereCondition.getValue()); // no longer needed, literal rendered here
                } else {
                    String whereConditionOperator = " = ";
                    String ending = " ";
                    if (bindValue != null) {
                        /*
                         * there will *not* necessarily be a replacement value ready at this point in the processing; these
                         * get set earlier if this is a SingleQuery, but a MultipleQuery will set these later based on the
                         * results of the pivoted query; so, only attempt processing here if necessary
                         */
                        String bindValueAsString = bindValue.toString();
                        if ((bindValueAsString != null) // whereConditionValue is null when whereCondition isn't a groupBy expression
                            && (bindValueAsString.startsWith("%") || bindValueAsString.endsWith("%"))) {
                            whereConditionOperator = " LIKE ";
                            ending = QueryUtility.getEscapeClause();
                        }
                    }
                    result += whereCondition.getKey() + whereConditionOperator + ":" + whereCondition.getValue()
                        + ending;
                }

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

        // finally, if we are narrowing by group membership, add the implicit groups condition
        if (!memberOfElements.isEmpty()) {
            result += " AND implicitGroup.name IN (";
            String separator = "";
            for (String groupName : memberOfElements) {
                result += (separator + "'" + groupName + "'");
                separator = ", ";
            }
            result += ")";
        }

        return result;
    }

    public List<String> tokenizeCondition(String condition) {
        List<String> results = new ArrayList<String>();

        boolean insideBracket = false;
        StringBuilder currentToken = new StringBuilder();

        for (char c : condition.trim().toCharArray()) {
            if (insideBracket) {
                if (c == ']') {
                    insideBracket = false;
                }
                // always add bracket-bounded chars
                currentToken.append(c);
            } else {
                if (c == '.' || c == ' ') {
                    String token = currentToken.toString();
                    if (token.length() > 0) {
                        results.add(token);
                    }
                    currentToken = new StringBuilder();
                } else {
                    if (c == '[') {
                        insideBracket = true;
                    }
                    currentToken.append(c);
                }
            }
        }

        // and if there's anything left in the buffer
        String token = currentToken.toString();
        if (token.length() > 0) {
            results.add(token);
        }

        return results;
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

    private void validateSubExpressionAgainstPreviouslySeen(String normalizedSubExpression, boolean grouped,
        boolean membership) throws InvalidExpressionException {
        normalizedSubExpression = stripFunctionSuffix(normalizedSubExpression);
        if (grouped) {
            if (groupedSubExpressions.contains(normalizedSubExpression)) {
                throw new InvalidExpressionException("Redundant 'groupby' expression[" + normalizedSubExpression
                    + "] - these expressions must be unique");
            }
            if (simpleSubExpressions.contains(normalizedSubExpression)) {
                throw new InvalidExpressionException(
                    "Can not group by the same condition you are filtering on, expression[" + normalizedSubExpression
                        + "]");
            }
            groupedSubExpressions.add(normalizedSubExpression);

        } else if (membership) {
            if (memberSubExpressions.contains(normalizedSubExpression)) {
                throw new InvalidExpressionException("Redundant 'memberof' expression[" + normalizedSubExpression
                    + "] - these expressions must be unique");
            }
            memberSubExpressions.add(normalizedSubExpression);
        } else {
            if (groupedSubExpressions.contains(normalizedSubExpression)) {
                throw new InvalidExpressionException(
                    "Can not group by the same condition you are filtering on, expression[" + normalizedSubExpression
                        + "]");
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

                Object token = tokens[j];
                if (token == null) {
                    builder.append("empty");
                } else {
                    builder.append(token.toString());
                }
            }

            return builder.toString();
        }
    }
}