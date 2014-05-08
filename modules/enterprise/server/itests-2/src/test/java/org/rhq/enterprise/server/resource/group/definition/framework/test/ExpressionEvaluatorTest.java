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
package org.rhq.enterprise.server.resource.group.definition.framework.test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.testng.annotations.Test;

import org.rhq.core.domain.resource.group.DuplicateExpressionTypeException;
import org.rhq.core.domain.resource.group.InvalidExpressionException;
import org.rhq.enterprise.server.resource.group.definition.framework.ExpressionEvaluator;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.QueryUtility;

public class ExpressionEvaluatorTest extends AbstractEJB3Test {

    // Due to initialization order could not call QueryUtility.getEscapeClauseCharacter() prior
    // to this point.  Did not work in class initializer, BeforeClass or even BeforeTest.
    private String[][] getSuccessTestCases() {
        String escapeChar = QueryUtility.getEscapeClauseCharacter();

        return new String[][] {

        { "resource.child.name = joseph",

        "SELECT res.id FROM Resource res " + //
            " JOIN res.childResources child " + //
            "WHERE child.name = :arg1 " },

        { "resource.name = joseph",

        "SELECT res.id FROM Resource res " + //
            "WHERE res.name = :arg1" },

        { "resource.version = 1.0",

        "SELECT res.id FROM Resource res " + //
            "WHERE res.version = :arg1" },

        { "resource.type.plugin = harry",

        "SELECT res.id FROM Resource res " + //
            "WHERE res.resourceType.plugin = :arg1" },

        { "resource.type.name = sally",

        "SELECT res.id FROM Resource res " + //
            "WHERE res.resourceType.name = :arg1" },

        { "resource.pluginConfiguration[partition] = cluster-1",

        "SELECT res.id FROM Resource res " + //
            "  JOIN res.pluginConfiguration pluginConf, PropertySimple simple, PropertyDefinition simpleDef  " + //
            "  JOIN res.resourceType.pluginConfigurationDefinition pluginConfDef " + // 
            " WHERE simple.name = :arg1 " + //
            "   AND simple.stringValue = :arg2 " + //
            "   AND simple.configuration = pluginConf " + //
            "   AND simpleDef.configurationDefinition = pluginConfDef " + //
            "   AND simple.name = simpleDef.name AND simpleDef.type != 'PASSWORD' " },

        { "resource.resourceConfiguration[partition].contains = cluster-1",

        "SELECT res.id FROM Resource res " + //
            "  JOIN res.resourceConfiguration conf, PropertySimple simple, PropertyDefinition simpleDef  " + //
            "  JOIN res.resourceType.resourceConfigurationDefinition confDef " + // 
            " WHERE simple.name = :arg1 " + //
            "   AND simple.stringValue LIKE :arg2 ESCAPE '" + escapeChar + "'" + //
            "   AND simple.configuration = conf " + //
            "   AND simpleDef.configurationDefinition = confDef " + //
            "   AND simple.name = simpleDef.name AND simpleDef.type != 'PASSWORD' " },

        { "groupBy resource.type.plugin; " + //
            "groupBy resource.type.name",

        "  SELECT res.resourceType.plugin, res.resourceType.name " + //
            "    FROM Resource res " + //
            "GROUP BY res.resourceType.plugin, res.resourceType.name",

        "  SELECT res.id FROM Resource res " + //
            "   WHERE res.resourceType.plugin = :arg1 " + //
            "     AND res.resourceType.name = :arg2 " },

        { "groupBy resource.resourceConfiguration[partition-name]",

        "  SELECT simple.stringValue FROM Resource res " + //
            "    JOIN res.resourceConfiguration conf, PropertySimple simple, PropertyDefinition simpleDef  " + //
            "    JOIN res.resourceType.resourceConfigurationDefinition confDef " + //
            "   WHERE simple.name = :arg1 " + //
            "     AND simple.configuration = conf " + //
            "     AND simpleDef.configurationDefinition = confDef " + //
            "     AND simple.name = simpleDef.name AND simpleDef.type != 'PASSWORD' " + //
            "GROUP BY simple.stringValue ",

        "  SELECT res.id FROM Resource res " + //
            "  JOIN res.resourceConfiguration conf, PropertySimple simple, PropertyDefinition simpleDef  " + //
            "  JOIN res.resourceType.resourceConfigurationDefinition confDef " + //
            " WHERE simple.name = :arg1 " + //
            "   AND simple.stringValue = :arg2 " + //
            "     AND simple.configuration = conf " + //
            "     AND simpleDef.configurationDefinition = confDef " + //
            "     AND simple.name = simpleDef.name AND simpleDef.type != 'PASSWORD' " },

        { "resource.type.name = Windows;" + //
            "resource.trait[Trait.osversion] = 5.1",

        "   SELECT res.id " + //
            " FROM Resource res JOIN res.schedules sched JOIN sched.definition def, MeasurementDataTrait trait" + //
            " WHERE res.resourceType.name = :arg1 " + //
            " AND def.name LIKE :arg2 ESCAPE '" + escapeChar + "'" + //
            " AND trait.value = :arg3 " + //
            " AND trait.schedule = sched " + //
            " AND trait.id.timestamp = " + //
            "     (SELECT max(mdt.id.timestamp) FROM MeasurementDataTrait mdt WHERE sched.id = mdt.schedule.id)" },

        { " ;" + // test empty first line, which should be allowed
            "resource.name.contains = joseph; " + //
            " ;" + // test empty intermediate line, which should be allowed
            "resource.parent.name.contains = joseph; " + //
            " ;", // test empty last line, which should be allowed,

            "SELECT res.id FROM Resource res " + //
                "WHERE res.name LIKE :arg1 ESCAPE '" + escapeChar + "'" + //
                "  AND res.parentResource.name LIKE :arg2 ESCAPE '" + escapeChar + "'" },

        { "EMPTY resource.name",

        "SELECT res.id FROM Resource res " + //
            "WHERE res.name IS NULL" },

        { "empty resource.pluginConfiguration[partition]",

        "SELECT res.id FROM Resource res " + //
            "  JOIN res.pluginConfiguration pluginConf, PropertySimple simple, PropertyDefinition simpleDef  " + //
            "  JOIN res.resourceType.pluginConfigurationDefinition pluginConfDef " + // 
            " WHERE simple.name = :arg1 " + //
            "   AND simple.stringValue IS NULL " + //
            "   AND simple.configuration = pluginConf " + //
            "   AND simpleDef.configurationDefinition = pluginConfDef " + //
            "   AND simple.name = simpleDef.name AND simpleDef.type != 'PASSWORD' " },

        { "resource.name = joseph; " + //
            "memberof = Group Name",

        "SELECT res.id FROM Resource res " + //
            "JOIN  res.implicitGroups implicitGroup  " + //        
            "WHERE res.name = :arg1 " + //
            "  AND implicitGroup.name IN ('Group Name') " }, };
    }

    @Test(groups = "integration.session")
    public void testWellFormedExpressions() throws Exception {

        String[][] successTestCases = getSuccessTestCases();
        List<Integer> suppressedCases = Collections.emptyList();

        getTransactionManager().begin();
        try {
            for (int i = 0; i < successTestCases.length; i++) {
                if (suppressedCases.contains(i)) {
                    continue;
                }

                String inputExpressions = successTestCases[i][0];
                String expectedTopResult = successTestCases[i][1];
                String expectedGroupResult = "";
                if (successTestCases[i].length == 3) {
                    expectedGroupResult = successTestCases[i][2];
                }

                ExpressionEvaluator evaluator = new ExpressionEvaluator();
                evaluator.setTestMode(true); // to prevent actual query from happening
                for (String expression : inputExpressions.split(";")) {
                    try {
                        evaluator.addExpression(expression); // do not trim, evaluator must handle sloppy expressions
                    } catch (Exception e) {
                        e.printStackTrace(System.out);
                        assert false : "Error in TestCase[" + i + "], could not add expression[" + expression
                            + "], input[" + inputExpressions + "]";
                    }
                }

                evaluator.execute(); // execute will compute the JPQL statements

                String actualTopResult = evaluator.getComputedJPQLStatement();
                String actualGroupResult = evaluator.getComputedJPQLGroupStatement();

                expectedTopResult = cleanUp(expectedTopResult);
                actualTopResult = cleanUp(actualTopResult);
                expectedGroupResult = cleanUp(expectedGroupResult);
                actualGroupResult = cleanUp(actualGroupResult);

                boolean success = expectedTopResult.equalsIgnoreCase(actualTopResult)
                    && expectedGroupResult.equalsIgnoreCase(actualGroupResult);
                if (!success) {
                    System.out.println("TestCase[" + i + "] = \"" + inputExpressions + "\" failed. \n"
                        + "Expected Top Result: \"" + expectedTopResult + "\"\n" + "Received Top Result: \""
                        + actualTopResult + "\"\n" + "Expected Group Result: \"" + expectedGroupResult + "\"\n"
                        + "Received Group Result: \"" + actualGroupResult + "\"\n");
                }
                assert success;
            }
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test(groups = "integration.session")
    public void testTokenizer() {

        String[] input = { "resource.child.name", //
            "resource.pluginConfiguration[partition]", //
            "resource.pluginConfiguration[partition].contains", //
            "resource.pluginConfiguration[partition.name].contains", //
            "memberof = GroupName" };

        String[][] expectedOutput = { { "resource", "child", "name" }, //
            { "resource", "pluginConfiguration[partition]" }, //
            { "resource", "pluginConfiguration[partition]", "contains" }, //
            { "resource", "pluginConfiguration[partition.name]", "contains" }, //
            { "memberof" } };

        ExpressionEvaluator evaluator = new ExpressionEvaluator();
        evaluator.setTestMode(true); // to prevent actual query from happening

        for (int i = 0; i < input.length; i++) {
            String nextInput = input[i];
            String[] nextExpectedOutput = expectedOutput[i];

            List<String> output = evaluator.tokenizeCondition(nextInput);
            String[] outputArray = output.toArray(new String[0]);

            if (nextExpectedOutput.length != outputArray.length) {
                System.out.println("Expected (" + Arrays.asList(nextExpectedOutput) + "), Received (" + output + ")");
                continue;
            }

            boolean failed = false;
            for (int j = 0; j < nextExpectedOutput.length; j++) {
                if (!nextExpectedOutput[j].equals(outputArray[j])) {
                    System.out.println("Expected (" + Arrays.asList(nextExpectedOutput) + "), Received (" + output
                        + ")");
                    failed = true;
                }
            }
            if (!failed) {
                System.out.println("Successfully tokenized (" + nextInput + ")");
            }
        }
    }

    private static interface ExpressionGenerator {
        String[] getExpressions();
    }

    private void evaluateExpressions(ExpressionGenerator generator) throws Exception {
        try {
            getTransactionManager().begin();
            ExpressionEvaluator evaluator = new ExpressionEvaluator();
            for (String expression : generator.getExpressions()) {
                evaluator.addExpression(expression);
            }
            evaluator.execute();
            evaluator.iterator().next();
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleResourceTraitExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.trait[agentHomeDirectory] = /var/rhq-agent",
                    "resource.trait[reasonForLastRestart] = OOMError" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleChildResourceTraitExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.child.trait[agentHomeDirectory] = /var/rhq-agent",
                    "resource.child.trait[reasonForLastRestart] = OOMError" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleParentResourceTraitExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.parent.trait[agentHomeDirectory] = /var/rhq-agent",
                    "resource.parent.trait[reasonForLastRestart] = OOMError" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleGrandParentResourceTraitExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.grandParent.trait[agentHomeDirectory] = /var/rhq-agent",
                    "resource.grandParent.trait[reasonForLastRestart] = OOMError" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleGreatGrandParentResourceTraitExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.greatGrandParent.trait[agentHomeDirectory] = /var/rhq-agent",
                    "resource.greatGrandParent.trait[reasonForLastRestart] = OOMError" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleGreatGreatGrandParentResourceTraitExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.greatGreatGrandParent.trait[agentHomeDirectory] = /var/rhq-agent",
                    "resource.greatGreatGrandParent.trait[reasonForLastRestart] = OOMError" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleResourceIdExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.id = 5", "resource.id = 6" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleChildResourceIdExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.child.id = 5", "resource.child.id = 6" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleParentResourceIdExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.parent.id = 5", "resource.parent.id = 6" };
            }
        });
    }

    @Test(expectedExceptions = InvalidExpressionException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleGrandParentResourceIdExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.grandParent.id = 5", "resource.grandParent.id = 6" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleGreatGrandParentResourceIdExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.greatGrandParent.id = 5", "resource.greatGrandParent.id = 6" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleGreatGreatGrandParentResourceIdExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.greatGreatGrandParent.id = 5", "resource.greatGreatGrandParent.id = 6" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleResourceNameExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.name = foo", "resource.name = bar" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleChildResourceNameExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.child.name = foo", "resource.child.name = bar" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleParentResourceNameExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.parent.name = foo", "resource.parent.name = bar" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleGrandParentResourceNameExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.grandParent.name = foo", "resource.grandParent.name = bar" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleGreatGrandParentResourceNameExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.greatGrandParent.name = foo", "resource.greatGrandParent.name = bar" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleGreatGreatGrandParentResourceNameExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.greatGreatGrandParent.name = foo",
                    "resource.greatGreatGrandParent.name = bar" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleResourceTypeExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.type.plugin = rhq-agent", "resource.type.name = RHQ Agent",
                    "resource.type.plugin = rhq-server", "resource.type.name = RHQ Server" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleChildResourceTypeExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.child.type.plugin = rhq-agent", "resource.child.type.name = RHQ Agent",
                    "resource.child.type.plugin = rhq-server", "resource.child.type.name = RHQ Server" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleParentResourceTypeExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.parent.type.plugin = rhq-agent",
                    "resource.parent.type.name = RHQ Agent", "resource.parent.type.plugin = rhq-server",
                    "resource.parent.type.name = RHQ Server" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleGrandParentResourceTypeExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.grandParent.type.plugin = rhq-agent",
                    "resource.grandParent.type.name = RHQ Agent", "resource.grandParent.type.plugin = rhq-server",
                    "resource.grandParent.type.name = RHQ Server" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleGreatGrandParentResourceTypeExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.greatGrandParent.type.plugin = rhq-agent",
                    "resource.greatGrandParent.type.name = RHQ Agent",
                    "resource.greatGrandParent.type.plugin = rhq-server",
                    "resource.greatGrandParent.type.name = RHQ Server" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleGreatGreatGrandParentResourceTypeExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.greatGreatGrandParent.type.plugin = rhq-agent",
                    "resource.greatGreatGrandParent.type.name = RHQ Agent",
                    "resource.greatGreatGrandParent.type.plugin = rhq-server",
                    "resource.greatGreatGrandParent.type.name = RHQ Server" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleResourceCategoryExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.type.category = PLATFORM", "resource.type.category = SERVER" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleChildResourceCategoryExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.child.type.category = PLATFORM",
                    "resource.child.type.category = SERVER" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleParentResourceCategoryExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.parent.type.category = PLATFORM",
                    "resource.parent.type.category = SERVER" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleGrandParentResourceCategoryExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.grandParent.type.category = PLATFORM",
                    "resource.grandParent.type.category = SERVER" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleGreatGrandParentResourceCategoryExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.greatGrandParent.type.category = PLATFORM",
                    "resource.greatGrandParent.type.category = SERVER" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleGreatGreatGrandParentResourceCategoryExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.greatGreatGrandParent.type.category = PLATFORM",
                    "resource.greatGreatGrandParent.type.category = SERVER" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleResourceAvailabilityExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.availability = UP", "resource.availability = UNKNOWN" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleChildResourceAvailabilityExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.child.availability = UP", "resource.child.availability = UNKNOWN" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleParentResourceAvailabilityExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.parent.availability = UP", "resource.parent.availability = UNKNOWN" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleGrandParentResourceAvailabilityExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.grandParent.availability = UP",
                    "resource.grandParent.availability = UNKNOWN" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleGreatGrandParentResourceAvailabilityExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.greatGrandParent.availability = UP",
                    "resource.greatGrandParent.availability = UNKNOWN" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleGreatGreatGrandParentResourceAvailabilityExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.greatGreatGrandParent.availability = UP",
                    "resource.greatGreatGrandParent.availability = UNKNOWN" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleResourcePluginConfigExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.pluginConfiguration[x] = 1", "resource.pluginConfiguration[y] = 2" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleChildResourcePluginConfigExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.child.pluginConfiguration[x] = 1",
                    "resource.child.pluginConfiguration[y] = 2" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleParentResourcePluginConfigExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.parent.pluginConfiguration[x] = 1",
                    "resource.parent.pluginConfiguration[y] = 2" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleGrandParentResourcePluginConfigExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.grandParent.pluginConfiguration[x] = 1",
                    "resource.grandParent.pluginConfiguration[y] = 2" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleGreatGrandParentResourcePluginConfigExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.greatGrandParent.pluginConfiguration[x] = 1",
                    "resource.greatGrandParent.pluginConfiguration[y] = 2" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleGreatGreatGrandParentResourcePluginConfigExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.greatGreatGrandParent.pluginConfiguration[x] = 1",
                    "resource.greatGreatGrandParent.pluginConfiguration[y] = 2" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleResourceConfigExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.resourceConfiguration[x] = 1", "resource.resourceConfiguration[y] = 2" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleChildResourceConfigExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.child.resourceConfiguration[x] = 1",
                    "resource.child.resourceConfiguration[y] = 2" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleParentResourceConfigExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.parent.resourceConfiguration[x] = 1",
                    "resource.parent.resourceConfiguration[y] = 2" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleGrandParentResourceConfigExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.grandParent.resourceConfiguration[x] = 1",
                    "resource.grandParent.resourceConfiguration[y] = 2" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleGreatGrandParentResourceConfigExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.greatGrandParent.resourceConfiguration[x] = 1",
                    "resource.greatGrandParent.resourceConfiguration[y] = 2" };
            }
        });
    }

    @Test(expectedExceptions = DuplicateExpressionTypeException.class, expectedExceptionsMessageRegExp = "You cannot specify multiple.*")
    public void doNotAllowMultipleGreatGreatGrandParentResourceConfigExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.greatGreatGrandParent.resourceConfiguration[x] = 1",
                    "resource.greatGreatGrandParent.resourceConfiguration[y] = 2" };
            }
        });
    }

    @Test(expectedExceptions = InvalidExpressionException.class, expectedExceptionsMessageRegExp = "Redundant.*")
    public void doNotAllowDuplicateMemberOfExpressions() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "memberof = foo", "memberof = foo" };
            }
        });
    }

    @Test(expectedExceptions = InvalidExpressionException.class, expectedExceptionsMessageRegExp = "Unrecognized.*")
    public void doNotAllowEmptyProperty() throws Exception {
        evaluateExpressions(new ExpressionGenerator() {
            @Override
            public String[] getExpressions() {
                return new String[] { "resource.greatGreatGrandParent.resourceConfiguration[] = 1" };
            }
        });
    }

    private String cleanUp(String result) {
        return result.replaceAll("\\s+", " ").trim();
    }
}