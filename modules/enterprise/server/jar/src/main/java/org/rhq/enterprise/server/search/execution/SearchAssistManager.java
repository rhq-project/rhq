package org.rhq.enterprise.server.search.execution;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.SavedSearchCriteria;
import org.rhq.core.domain.search.SavedSearch;
import org.rhq.core.domain.search.SearchSubsystem;
import org.rhq.core.domain.search.SearchSuggestion;
import org.rhq.core.domain.search.SearchSuggestion.Kind;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.server.search.SavedSearchManagerLocal;
import org.rhq.enterprise.server.search.assist.SearchAssistant;
import org.rhq.enterprise.server.search.assist.SearchAssistantFactory;
import org.rhq.enterprise.server.util.LookupUtil;

public class SearchAssistManager {

    private static final Log LOG = LogFactory.getLog(SearchAssistManager.class);
    private SavedSearchManagerLocal savedSearchManager = LookupUtil.getSavedSearchManager();

    private static List<String> stringComparisonOperators = Arrays.asList("!==", "!=", "==", "=");
    private static List<String> numericComparisonOperators = Arrays.asList("<", ">", "!=", "=");
    private static List<String> enumComparisonOperators = Arrays.asList("!=", "=");
    private static List<String> allComparisonOperators = Arrays.asList("!==", "!=", "==", "=", "<", ">");
    private static List<String> booleanOperators = Arrays.asList("|");

    private Subject subject;
    private SearchSubsystem searchSubsystem;

    /*
     * states:
     * 
     *    empty expression                                --> suggest contexts with empty filter
     *    incomplete context                              --> suggest contexts with passed filter, suffixed with open bracket for parameterization
     *    complete context                                --> suggest comparison operators with empty filter
     *    begin parameterizations                         --> suggest params with empty filter
     *    incomplete parameterization                     --> suggest params with passed filter
     *    complete parameterization                       --> suggest comparison operators with empty filter
     *    incomplete operator                             --> suggest comparison operators with passed filter
     *    complete operator                               --> suggest values with empty filter
     *    incomplete value                                --> suggest values with passed filter
     *    otherwise assume complete previous expression   --> suggest boolean operators at the top
     *                                                    --> suggest contexts with empty filter below that
     */
    static class SearchTermAssistant {

        private static final String CARET = "@@@";

        String expression;

        List<String> terms;
        int currentTermIndex;
        int indexWithinTerm;

        public SearchTermAssistant(String expression, int caretPos) {
            // insert CARET token into expression at caretPos
            String before = expression.substring(0, caretPos);
            String after = expression.substring(caretPos);
            this.expression = before + CARET + after;
            tokenize();
            for (int i = 0; i < terms.size(); i++) {
                String term = terms.get(i);
                int index = term.indexOf("@@@");
                if (index != -1) {
                    String replaced = term.replace("@@@", "");
                    terms.set(i, replaced);
                    currentTermIndex = i;
                    indexWithinTerm = index;
                    break;
                }
            }
        }

        private void tokenize() {
            List<String> fragments = tokenizeIntoFragments(expression);
            this.terms = joinIntoTerms(fragments);
        }

        private List<String> tokenizeIntoFragments(String expression) {
            List<String> fragments = new ArrayList<String>();

            char quoteChar = 0;
            StringBuilder term = new StringBuilder();
            for (char nextChar : expression.toCharArray()) {
                if (quoteChar != 0) { // accept space inside quoted strings
                    term.append(nextChar);
                    if (nextChar == quoteChar) { // look for end quote
                        quoteChar = 0; // i just left a quoted string
                    }
                } else { // ignore spaces outside quoted string
                    if (Character.isWhitespace(nextChar)) { // spaces delimit terms outside quoted strings
                        if (term.length() > 0) {
                            fragments.add(term.toString());
                            term = new StringBuilder();
                        }
                    } else if (nextChar == '(' || nextChar == ')') {
                        // completed ignore parentheses outside quoted strings
                    } else {
                        term.append(nextChar);
                        if (nextChar == '\'' || nextChar == '"') {
                            quoteChar = nextChar; // entering a quoted string
                        }
                    }
                }
            }
            if (term.length() > 0) {
                fragments.add(term.toString());
            }

            return fragments;
        }

        private List<String> joinIntoTerms(List<String> fragments) {
            if (fragments.size() < 3) {
                return fragments;
            }
            List<String> terms = new ArrayList<String>();
            int i = 1;
            while (i < fragments.size() - 1) {
                String before = fragments.get(i - 1);
                String term = fragments.get(i);
                if (allComparisonOperators.contains(term)) {
                    String after = fragments.get(i + 1);
                    terms.add(before + term + after);
                    i += 3; // a triple of terms were processed
                } else {
                    terms.add(before);
                    i++; // only one term processed
                }
            }

            // if the last three terms weren't a valid triple, there will be two left over
            if (i < fragments.size()) {
                String nextToLast = fragments.get(fragments.size() - 2);
                String last = fragments.get(fragments.size() - 1);
                if (allComparisonOperators.contains(last)) { // last couple was an incomplete term
                    terms.add(nextToLast + last);
                } else {
                    terms.add(nextToLast); // there are unrelated terms, possibly simple text matches
                    terms.add(last);
                }
            } else if (i == fragments.size()) { // found a triple just before last fragment
                String last = fragments.get(fragments.size() - 1);
                terms.add(last);
            }
            return terms;
        }

        public List<String> getTerms() {
            return terms;
        }

        public String getPreviousToken() {
            return terms.get(currentTermIndex - 1);
        }

        public String getCurrentToken() {
            return terms.get(currentTermIndex);
        }

        public String getFragmentBeforeCaret() {
            return getCurrentToken().substring(0, indexWithinTerm);
        }

        public void report(PrintStream output) {
            output.println("Expression: " + expression);
            List<String> fragments = tokenizeIntoFragments(expression);
            List<String> terms = joinIntoTerms(fragments);
            int counter = 0;
            for (String result : terms) {
                output.println("Token[" + (++counter) + "]: " + result);
            }
            output.println();
        }

        public String toString() {
            return null;
        }

        public String getExpressionWithReplacement(String replacement) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < terms.size(); i++) {
                if (i == currentTermIndex) {
                    builder.append(replacement);
                } else {
                    builder.append(terms.get(i));
                }
                builder.append(' ');
            }
            return builder.toString();
        }
    }

    static class ParsedContext {
        public enum State {
            CONTEXT, PARAM, OPERATOR, VALUE;
        }

        public enum Type {
            SIMPLE, ADVANCED;
        }

        public final String context;
        public final String param;
        public final String operator;
        public final String value;
        public final State state;
        public final Type type;

        private ParsedContext(String context, String param, String operator, String value) {
            this.state = computeState(context, param, operator, value);
            this.type = computeType(param, operator);

            this.context = (this.type == Type.SIMPLE) ? stripQuotes(context) : context;
            this.param = param == null ? "" : param; // ensure non-null
            this.operator = operator;
            this.value = value == null ? "" : value; // ensure non-null
        }

        private String stripQuotes(String data) {
            if (data.length() == 0) {
                return "";
            }

            char first = data.charAt(0);
            char last = data.charAt(data.length() - 1);
            if (first == '\'' || first == '"') {
                if (data.length() == 1) {
                    return "";
                }
                data = data.substring(1);
            }
            if (last == '\'' || last == '"') {
                if (data.length() == 1) {
                    return "";
                }
                data = data.substring(0, data.length() - 1);
            }
            return data;
        }

        private State computeState(String context, String param, String operator, String value) {
            if (value != null) {
                return State.VALUE;
            }
            if (operator != null) {
                return State.OPERATOR;
            }
            if (param != null) {
                return State.PARAM;
            }
            return State.CONTEXT;
        }

        public static ParsedContext get(String term) {
            int index = 0;
            char[] expr = term.toCharArray();
            StringBuilder buffer = new StringBuilder();

            while (index < expr.length && expr[index] != '[' && expr[index] != '!' && expr[index] != '=') { // read up until beginning of param or operator
                buffer.append(expr[index]);
                index++;
            }
            String context = buffer.toString(); // either beginning of param, beginning of operator, or end of expression
            buffer = new StringBuilder();
            if (index == expr.length || (expr[index] != '[' && expr[index] != '!' && expr[index] != '=')) { // if not beginning of param or operator, return 
                return new ParsedContext(context, null, null, null);
            }

            String param = null;
            if (expr[index] == '[') { // parameterized context
                index++; // skip over '['
                while (index < expr.length && expr[index] != ']') { // read up until end of param
                    buffer.append(expr[index]);
                    index++;
                }
                param = buffer.toString();
                buffer = new StringBuilder();
                if (index == expr.length || expr[index] != ']') { // if not end of param, incomplete param so return 
                    return new ParsedContext(context, param, null, null);
                }
                index++; // skip over ']'
            }

            while (index < expr.length && (expr[index] == '!' || expr[index] == '=')) { // read up until end of operator chars
                buffer.append(expr[index]);
                index++;
            }
            String operator = buffer.toString();
            if (index == expr.length) { // return if end of expression 
                return new ParsedContext(context, param, operator, null);
            }

            String value = term.substring(index); // any remain characters are the value
            if (value.length() > 0) {
                if (value.charAt(0) == '\'' || value.charAt(0) == '"') {
                    value = value.substring(1);
                }
            }
            return new ParsedContext(context, param, operator, value);
        }

        private Type computeType(String param, String operator) {
            if (operator != null) {
                return Type.ADVANCED; // non-null operator implies an incomplete, advanced term
            }

            if (param != null) {
                return Type.ADVANCED; // non-null operator implies an incomplete, advanced term
            }

            /*
             * otherwise it's still possible that the user wants a simple text completion.
             * 
             * note: it should not be necessary to check for non-null 'value' because operation/param would have
             *       had to be non-null first, which we've already verified by getting to here.
             */
            return Type.SIMPLE;
        }

        public String toString() {
            StringBuilder buffer = new StringBuilder();
            buffer.append(getClass().getSimpleName()).append("[");
            buffer.append("state=").append(state);
            buffer.append(", context(").append(context);
            buffer.append("), param(").append(param);
            buffer.append("), operator(").append(operator);
            buffer.append("), value(").append(value).append(")]");
            return buffer.toString();
        }
    }

    public SearchAssistManager(Subject subject, SearchSubsystem searchSubsystem) {
        this.subject = subject;
        this.searchSubsystem = searchSubsystem;
    }

    protected SearchAssistant getSearchAssistant() {
        return SearchAssistantFactory.getAssistant(searchSubsystem);
    }

    protected SearchAssistant getTabAwareSearchAssistant(String tab) {
        return SearchAssistantFactory.getTabAwareAssistant(searchSubsystem, tab);
    }

    private List<String> getAllContexts() {
        List<String> results = new ArrayList<String>(getSearchAssistant().getSimpleContexts());
        for (String parameterized : getSearchAssistant().getParameterizedContexts()) {
            results.add(parameterized + "[");
        }
        return results;
    }

    public List<SearchSuggestion> getSuggestions(String expression, int caretPos) {
        return getTabAwareSuggestions(expression, caretPos, null);
    }

    public List<SearchSuggestion> getTabAwareSuggestions(String expression, int caretPos, String tab) {
        List<SearchSuggestion> results = new ArrayList<SearchSuggestion>();

        if (subject == null) {
            // likely a client's misuse of the API, but gracefully return no saved searches if the user is not found
            SearchSuggestion footerMessage = new SearchSuggestion(Kind.InstructionalTextComment,
                "Error retrieving saved searches, please refresh page");
            results.add(footerMessage);
            return results;
        }

        try {
            if (expression == null) {
                expression = "";
            }

            List<SearchSuggestion> simple = getSimpleSuggestions(expression, caretPos, tab);
            List<SearchSuggestion> advanced = getAdvancedSuggestions(expression, caretPos, tab);
            List<SearchSuggestion> userSavedSearches = getUserSavedSearchSuggestions(expression);
            //List<SearchSuggestion> globalSavedSearches = getGlobalSavedSearchSuggestions(expression);

            results.addAll(simple);
            results.addAll(advanced);
            results.addAll(userSavedSearches);
            //results.addAll(globalSavedSearches);

            if (results.isEmpty()) {
                SearchSuggestion footerMessage = new SearchSuggestion(Kind.InstructionalTextComment,
                    "Start typing for more simple text matches");
                results.add(footerMessage);
            } else {
                Collections.sort(results);
            }

        } catch (Throwable t) {
            SearchSuggestion footerMessage = new SearchSuggestion(Kind.InstructionalTextComment,
                "Error retrieving suggestions: " + t.getMessage() + ", see server log for more details");
            results.add(footerMessage);
            LOG.info("Error retrieving suggestions", t);
        }
        return results;
    }

    public List<SearchSuggestion> getSimpleSuggestions(String expression, int caretPos, String tab) {
        SearchAssistant completor = getTabAwareSearchAssistant(tab);

        debug("getSimpleSuggestions: START");

        SearchTermAssistant assistant = new SearchTermAssistant(expression, caretPos);
        String beforeCaret = assistant.getFragmentBeforeCaret();
        ParsedContext parsed = ParsedContext.get(beforeCaret);

        if (parsed.type != ParsedContext.Type.SIMPLE) {
            return Collections.emptyList();
        }

        /*
         * if we know the ParsedContext object may represent a simple search term, then the 'context' attribute
         * would hold it's current value, which in this case we'll extract and use as the simple value match
         */
        String parsedTerm = parsed.context;
        String primarySimpleContext = completor.getPrimarySimpleContext();
        debug("getSimpleSuggestions: suggesting value completions for a simple context [" + primarySimpleContext + "]");

        List<String> valueSuggestions = padWithQuotes(beforeCaret, completor.getValues(primarySimpleContext, null,
            parsedTerm));
        List<SearchSuggestion> suggestions = convert(valueSuggestions, parsed, parsedTerm, Kind.Simple);
        return suggestions;
    }

    public List<SearchSuggestion> getAdvancedSuggestions(String expression, int caretPos, String tab) {
        SearchAssistant completor = getTabAwareSearchAssistant(tab);

        debug("getAdvancedSuggestions: START");
        SearchTermAssistant assistant = new SearchTermAssistant(expression, caretPos);
        String[] tokens = assistant.getTerms().toArray(new String[0]);
        debug("" + tokens.length + " tokens are " + Arrays.asList(tokens));

        if (tokens.length == 0 || caretPos == 0) {
            debug("getAdvancedSuggestions: no terms");
            return convert(getAllContexts()); // no terms yet defined
        }

        String beforeCaret = assistant.getFragmentBeforeCaret();
        debug("getAdvancedSuggestions: beforeCaret is '" + beforeCaret + "'");

        String pad = getQuotePadding(beforeCaret);
        debug("getAdvancedSuggestions: padding is ~" + pad + "~");

        if (beforeCaret.startsWith("'") || beforeCaret.startsWith("\"")) {
            return Collections.emptyList();
        }

        ParsedContext parsed = ParsedContext.get(beforeCaret);
        debug("getAdvancedSuggestions: parsed is " + parsed);
        switch (parsed.state) {
        case CONTEXT:
            if (parsed.context.equals("")) {
                debug("getAdvancedSuggestions: empty term, suggesting all contexts");
                return convert(getAllContexts());
                /*
                if (tokens.length == 1) {
                    debug("getAdvancedSuggestions: no terms yet, suggesting contexts");
                    return convert(getAllContexts());
                } else if (isBooleanTerm(assistant.getPreviousToken())) {
                    debug("getAdvancedSuggestions: previous term was boolean, suggesting contexts");
                    return convert(getAllContexts());
                } else {
                    debug("getAdvancedSuggestions: previous term was not boolean, suggesting boolean");
                    return convert(booleanOperators);
                }
                */
            } else if (isBooleanTerm(parsed.context)) {
                debug("getAdvancedSuggestions: beforeCaret is whole boolean operator");
                return convert(getAllContexts()); // TODO: should we tell user to type a space first?
            } else {
                // check if this context is complete or not
                if (completor.getSimpleContexts().contains(parsed.context)) {
                    debug("getAdvancedSuggestions: search term is simple context, wants operator");
                    List<String> contextComparisonOperators = getComparisonOperatorsForContext(parsed.context,
                        completor);
                    return convert(pad(parsed.context, contextComparisonOperators, ""), parsed, parsed.context);
                }
                if (completor.getParameterizedContexts().contains(parsed.context)) {
                    debug("getAdvancedSuggestions: search term is parameterized context, wants open bracket");
                    return convert(Arrays.asList(parsed.context + "["), parsed, parsed.context);
                }

                debug("getAdvancedSuggestions: search term wants context completion");
                List<String> startsWithContexts = new ArrayList<String>();
                String lowerCaseParsedContext = parsed.context.toLowerCase(); // contexts should already be lower-cased
                for (String context : completor.getSimpleContexts()) {
                    if (context.indexOf(lowerCaseParsedContext) != -1) {
                        startsWithContexts.add(context);
                    }
                }
                for (String context : completor.getParameterizedContexts()) {
                    if (context.indexOf(lowerCaseParsedContext) != -1) {
                        startsWithContexts.add(context + "[");
                    }
                }
                return convert(startsWithContexts, parsed, parsed.context);
            }
        case PARAM:
            debug("getAdvancedSuggestions: param state");
            return convert(pad(parsed.context + "[", completor.getParameters(parsed.context, parsed.param), "]"),
                parsed, parsed.param);
        case OPERATOR:
            debug("getAdvancedSuggestions: operator state");
            if (allComparisonOperators.contains(parsed.operator)) {
                debug("search term is complete operator, suggesting values instead");
                List<String> valueSuggestions = padWithQuotes(beforeCaret, completor.getValues(parsed.context,
                    parsed.param, ""));
                if (completor.getSimpleContexts().contains(parsed.context)) {
                    debug("getAdvancedSuggestions: suggesting value completions for a simple context");
                    return convert(pad(parsed.context + parsed.operator, valueSuggestions, ""));
                } else {
                    debug("getAdvancedSuggestions: suggesting value completions for a parameterized context");
                    return convert(pad(parsed.context + "[" + parsed.param + "]" + parsed.operator, valueSuggestions,
                        ""));
                }
            }

            List<String> operatorSuggestions = new ArrayList<String>();
            List<String> contextComparisonOperators = getComparisonOperatorsForContext(parsed.context, completor);
            for (String op : contextComparisonOperators) {
                if (op.startsWith(parsed.operator)) {
                    operatorSuggestions.add(op);
                }
            }

            debug("getAdvancedSuggestions: providing suggestions for comparison operators");
            if (completor.getSimpleContexts().contains(parsed.context)) {
                return convert(pad(parsed.context, operatorSuggestions, ""));
            } else {
                return convert(pad(parsed.context + "[" + parsed.param + "]", operatorSuggestions, ""));
            }
        case VALUE:
            debug("getAdvancedSuggestions: value state");
            List<String> valueSuggestions = padWithQuotes(beforeCaret, completor.getValues(parsed.context,
                parsed.param, parsed.value));
            if (completor.getSimpleContexts().contains(parsed.context)) {
                debug("getAdvancedSuggestions: suggesting value completions for a simple context");
                return convert(pad(parsed.context + parsed.operator, valueSuggestions, ""), parsed, parsed.value);
            } else {
                debug("getAdvancedSuggestions: suggesting value completions for a parameterized context");
                return convert(pad(parsed.context + "[" + parsed.param + "]" + parsed.operator, valueSuggestions, ""),
                    parsed, parsed.value);
            }
        default:
            return Collections.emptyList();
        }
    }

    private List<String> getComparisonOperatorsForContext(String context, SearchAssistant completor) {
        if (completor.isNumericalContext(context)) {
            return numericComparisonOperators;
        } else if (completor.isNumericalContext(context)) {
            return enumComparisonOperators;
        } else {
            return stringComparisonOperators;
        }
    }

    public List<SearchSuggestion> getUserSavedSearchSuggestions(String expression) {
        expression = expression.trim().toLowerCase().replaceAll("\\s+", " ");

        SavedSearchCriteria criteria = new SavedSearchCriteria();
        criteria.addFilterSubjectId(subject.getId());
        criteria.addFilterSearchSubsystem(searchSubsystem);
        if (expression.equals("") == false) {
            criteria.addFilterName(expression);
        }

        criteria.setCaseSensitive(false);
        criteria.addSortName(PageOrdering.ASC);

        List<SavedSearch> savedSearchResults = savedSearchManager.findSavedSearchesByCriteria(subject, criteria);

        List<SearchSuggestion> results = new ArrayList<SearchSuggestion>();
        for (SavedSearch next : savedSearchResults) {
            String label = next.getName();
            if (next.getResultCount() != null) {
                label += " (" + next.getResultCount() + ")";
            }
            String value = next.getPattern();
            int index = next.getName().toLowerCase().indexOf(expression);
            SearchSuggestion suggestion = new SearchSuggestion(Kind.UserSavedSearch, label, value, index, expression
                .length());
            results.add(suggestion);
        }
        return results;
    }

    public List<SearchSuggestion> getGlobalSavedSearchSuggestions(String expression) {
        expression = expression.trim().toLowerCase().replaceAll("\\s+", " ");

        SavedSearchCriteria criteria = new SavedSearchCriteria();
        criteria.addFilterGlobal(true);
        criteria.addFilterSearchSubsystem(searchSubsystem);
        if (expression.equals("") == false) {
            criteria.addFilterName(expression);
        }

        criteria.setCaseSensitive(false);
        criteria.addSortName(PageOrdering.ASC);

        List<SavedSearch> savedSearchResults = savedSearchManager.findSavedSearchesByCriteria(subject, criteria);

        List<SearchSuggestion> results = new ArrayList<SearchSuggestion>();
        for (SavedSearch next : savedSearchResults) {
            String label = next.getName();
            String value = next.getPattern();
            int index = next.getName().toLowerCase().indexOf(expression);
            SearchSuggestion suggestion = new SearchSuggestion(Kind.GlobalSavedSearch, label, value, index, expression
                .length());
            results.add(suggestion);
        }
        return results;
    }

    private boolean isBooleanTerm(String term) {
        for (String op : booleanOperators) {
            if (op.equals(term)) {
                return true;
            }
        }
        return false;
    }

    private List<SearchSuggestion> convert(List<String> suggestions) {
        return convert(suggestions, null, "", Kind.Advanced);
    }

    private List<SearchSuggestion> convert(List<String> suggestions, ParsedContext parsed, String term) {
        return convert(suggestions, parsed, term, Kind.Advanced);
    }

    private List<SearchSuggestion> convert(List<String> suggestions, ParsedContext parsed, String term, Kind kind) {
        int startIndex = getStartIndex(parsed);
        term = term.toLowerCase();
        List<SearchSuggestion> results = new ArrayList<SearchSuggestion>(suggestions.size());
        for (String suggestion : suggestions) {
            int index = suggestion.toLowerCase().indexOf(term, startIndex);
            results.add(new SearchSuggestion(kind, suggestion, index, term.length()));
        }
        return results;
    }

    private int getStartIndex(ParsedContext parsed) {
        if (parsed == null) {
            return 0;
        }
        switch (parsed.state) {
        case PARAM:
            return parsed.context.length();
        case OPERATOR:
            return parsed.context.length() + (parsed.param != null ? parsed.param.length() : 0);
        case VALUE:
            return parsed.context.length() + (parsed.param != null ? parsed.param.length() : 0)
                + parsed.operator.length();
        }
        return 0; // case CONTEXT
    }

    private List<String> pad(String leftPad, List<String> data, String rightPad) {
        List<String> results = new ArrayList<String>();
        for (String next : data) {
            results.add(leftPad + next + rightPad);
        }
        return results;
    }

    private List<String> padWithQuotes(String beforeCaret, List<String> data) {
        String defaultPad = getQuotePadding(beforeCaret);

        List<String> results = new ArrayList<String>();
        for (String next : data) {
            if (next == null) {
                results.add("null"); // null search comparisons should never be quoted
                continue;
            }

            boolean hasWhiteSpace = next.matches(".*\\s.*");
            if (hasWhiteSpace == false) {
                // don't pad things that don't need padding
                results.add(next);
                continue;
            }

            // we do have whitespace, let's also see if we have quotes
            boolean hasSingleQuote = next.indexOf("'") != -1;
            boolean hasDoubleQuote = next.indexOf('"') != -1;

            if (hasSingleQuote && hasDoubleQuote) {
                // don't pad if suggestion has both single- and double-quotes
                // instead, treat suggestion as individual terms, otherwise parser will bomb
                results.add(next);
                continue;
            }

            String pad = null;
            if (hasSingleQuote) {
                pad = "\""; // pad with double-quotes
            } else if (hasDoubleQuote) {
                pad = "'"; // pad with single-quotes
            } else {
                // otherwise respect the user-chosen padding
                pad = defaultPad;
            }

            results.add(pad + next + pad);
        }
        return results;
    }

    private String getQuotePadding(String beforeCaret) {
        if (beforeCaret.equals("")) {
            return "\"";
        }
        // if not empty, it has at least one char
        char first = beforeCaret.charAt(0);
        if (first == '\'') {
            return "'";
        } else /*  if (first == '"') */{
            return "\"";
        }
    }

    private void debug(String message) {
        LOG.debug(message);
    }
}
