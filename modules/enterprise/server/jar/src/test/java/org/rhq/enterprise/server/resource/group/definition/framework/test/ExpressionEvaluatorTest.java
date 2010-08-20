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
            " WHERE simple.name LIKE :arg1 ESCAPE '" + escapeChar + "'" + //
            "   AND simple.stringValue = :arg2 " + //
            "   AND simple.configuration = pluginConf " + //
            "   AND simpleDef.configurationDefinition = pluginConfDef " + //
            "   AND simple.name = simpleDef.name AND simpleDef.type != 'PASSWORD' " },

        { "resource.resourceConfiguration[partition].contains = cluster-1",

        "SELECT res.id FROM Resource res " + //
            "  JOIN res.resourceConfiguration conf, PropertySimple simple, PropertyDefinition simpleDef  " + //
            "  JOIN res.resourceType.resourceConfigurationDefinition confDef " + // 
            " WHERE simple.name LIKE :arg1 ESCAPE '" + escapeChar + "'" + //
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
            "   WHERE simple.name LIKE :arg1 ESCAPE '" + escapeChar + "'" + //
            "     AND simple.configuration = conf " + //
            "     AND simpleDef.configurationDefinition = confDef " + //
            "     AND simple.name = simpleDef.name AND simpleDef.type != 'PASSWORD' " + //
            "GROUP BY simple.stringValue ",

        "  SELECT res.id FROM Resource res " + //
            "  JOIN res.resourceConfiguration conf, PropertySimple simple, PropertyDefinition simpleDef  " + //
            "  JOIN res.resourceType.resourceConfigurationDefinition confDef " + //
            " WHERE simple.name LIKE :arg1 ESCAPE '" + escapeChar + "'" + //
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
            " WHERE simple.name LIKE :arg1 ESCAPE '" + escapeChar + "'" + //
            "   AND simple.stringValue IS NULL " + //
            "   AND simple.configuration = pluginConf " + //
            "   AND simpleDef.configurationDefinition = pluginConfDef " + //
            "   AND simple.name = simpleDef.name AND simpleDef.type != 'PASSWORD' " }, };
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
            "resource.pluginConfiguration[partition.name].contains" };

        String[][] expectedOutput = { { "resource", "child", "name" }, //
            { "resource", "pluginConfiguration[partition]" }, //
            { "resource", "pluginConfiguration[partition]", "contains" }, //
            { "resource", "pluginConfiguration[partition.name]", "contains" } };

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

    private String cleanUp(String result) {
        return result.replaceAll("\\s+", " ").trim();
    }
}