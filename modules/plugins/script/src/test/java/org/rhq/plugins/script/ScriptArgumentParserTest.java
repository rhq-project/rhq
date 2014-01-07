package org.rhq.plugins.script;

import static org.testng.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

/**
 * @author Lukas Krejci
 * @since 4.10
 */
@Test
public class ScriptArgumentParserTest {

    public void test() {
        HashMap<String, String[]> testCases = new HashMap<String, String[]>();

        testCases.put("1   2\t3", new String[]{"1", "2", "3"});
        testCases.put("1 '2 ' \"3'\" '4'abs '5\"'", new String[]{"1", "2 ", "3'", "4abs", "5\""});
        testCases.put("\\  \\2 '3\\'a'", new String[]{" ", "2", "3'a"});

        for(Map.Entry<String, String[]> testCase : testCases.entrySet()) {
            String[] result = ScriptArgumentParser.parse(testCase.getKey(), '\\');

            assertEquals(result, testCase.getValue(), "Failed to parse [" + testCase.getKey() + "]");
        }
    }
}
