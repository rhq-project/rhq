package org.rhq.enterprise.server.search;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rhq.core.domain.search.SearchSubsystem;
import org.rhq.enterprise.server.search.assist.AbstractSearchAssistant;
import org.rhq.enterprise.server.search.assist.SearchAssistant;
import org.rhq.enterprise.server.search.execution.SearchAssistManager;
import org.rhq.enterprise.server.util.LookupUtil;

public class TestAutoCompletionManager extends SearchAssistManager {
    public static class TestAutoCompletor extends AbstractSearchAssistant {
        /*
         * color[primary] = red, green, blue
         * color[other] = red-green, green-blue, blue-red
         * number = two, four, twenty-two, twenty-four, forty-two
         */

        private static Map<String, List<String>> suggestionMap = new HashMap<String, List<String>>();

        static {
            suggestionMap.put("color:params", build("primary", "other"));
            suggestionMap.put("color:primary", build("red", "green", "blue"));
            suggestionMap.put("color:other", build("red-green", "green-blue", "blue-red"));
            suggestionMap.put("number:", build("two", "four", "twenty-two", "twenty-four", "forty-two"));
        }

        private static List<String> build(String... items) {
            return Collections.unmodifiableList(Arrays.asList(items));
        }

        @Override
        public SearchSubsystem getSearchSubsystem() {
            return null;
        }

        @Override
        public List<String> getSimpleContexts() {
            return Arrays.asList("number");
        }

        @Override
        public List<String> getParameterizedContexts() {
            return Arrays.asList("color");
        }

        @Override
        public List<String> getParameters(String context, String filter) {
            super.getParameters(context, filter);
            return filter(suggestionMap.get("color:params"), filter);
        }

        @Override
        public List<String> getValues(String context, String param, String filter) {
            super.getValues(context, param, filter);
            String lookup = context + ":" + (param == null ? "" : param);
            return filter(suggestionMap.get(lookup), filter);
        }
    }

    public TestAutoCompletionManager() {
        super(LookupUtil.getSubjectManager().getOverlord(), null);
    }

    @Override
    protected AbstractSearchAssistant getSearchAssistant() {
        return new TestAutoCompletor();
    }

    @Override
    protected SearchAssistant getTabAwareSearchAssistant(String tab) {
        return new TestAutoCompletor();
    }
}
