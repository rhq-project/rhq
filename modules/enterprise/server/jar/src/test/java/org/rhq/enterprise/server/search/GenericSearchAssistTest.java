package org.rhq.enterprise.server.search;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.search.SearchSubsystem;
import org.rhq.core.domain.search.SearchSuggestion;
import org.rhq.enterprise.server.search.execution.SearchAssistManager;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

public class GenericSearchAssistTest extends AbstractEJB3Test {

    @Test
    public void testGenericAssist() throws Exception {
        BufferedReader reader = null;
        try {
            InputStream stream = getClass().getClassLoader().getResourceAsStream("test-assist-color-number.txt");
            reader = new BufferedReader(new InputStreamReader(stream));
            String expression = null;
            String expected = null;

            int count = 0;
            while (true) {
                expression = reader.readLine();
                if (expression == null) {
                    break;
                }
                expected = reader.readLine();
                if (expected == null) {
                    break;
                }

                count++;
                List<SearchSuggestion> results = new TestAutoCompletionManager().getAdvancedSuggestions(expression,
                    expression.length(), null);
                List<String> expectedResults = Arrays.asList(expected.split(" "));

                System.out.println();
                System.out.println("Expression: " + expression);
                try {
                    compare(results, expectedResults);
                    System.out.println("Assist worked: " + parenthesize(expectedResults.toString()));
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
            System.out.println("Tested " + count + " expressions for assist");
        } catch (Exception e) {
            System.out.println("Error testing GenericSearchAssistTest: " + e);
            e.printStackTrace(System.out);
            throw e;
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    @Test
    public void test2() throws Exception {
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
        SearchAssistManager mgr = new SearchAssistManager(overlord, SearchSubsystem.RESOURCE);
        List<SearchSuggestion> suggestions = mgr.getSuggestions("category=PLATFORM",0);
        System.out.println("Number of suggestions: " + suggestions.size());
        for (SearchSuggestion s : suggestions) {
            System.out.println(s);
        }


    }

    private void compare(List<SearchSuggestion> results, List<String> expected) {
        List<String> rawResults = new ArrayList<String>();
        for (SearchSuggestion suggestion : results) {
            rawResults.add(suggestion.getValue());
        }

        Collections.sort(rawResults);
        Collections.sort(expected);

        assert rawResults.size() == expected.size() : print(expected, rawResults);
        for (int i = 0; i < rawResults.size(); i++) {
            String nextRaw = rawResults.get(i);
            String nextExpected = expected.get(i);
            assert nextRaw.equals(nextExpected) : print(expected, rawResults);
        }
    }

    private String print(List<String> expected, List<String> results) {
        return "expected" + parenthesize(expected.toString()) + ", results" + parenthesize(results.toString());
    }

    private String parenthesize(String data) {
        return "(" + data.substring(1, data.length() - 1) + ")";
    }

    public static void main(String[] args) throws Exception {
        new GenericSearchAssistTest().testGenericAssist();
    }
}
