package org.rhq.enterprise.server.search.execution;

public class SearchSuggestion {
    public enum SearchCategory {
        Simple, Advanced, SavedSearch, InstructionalTextComment, Unknown;
    }

    private final SearchCategory category;
    private final String value;
    private final String label;
    private final int startIndex;
    private final int endIndex;

    public SearchSuggestion(SearchCategory category, String value) {
        this(category, value, 0, 0);
    }

    public SearchSuggestion(SearchCategory category, String value, int startIndex, int length) {
        this.category = category;
        this.label = value;
        this.value = value;
        this.startIndex = startIndex;
        this.endIndex = startIndex + length;
    }

    public SearchCategory getCategory() {
        return category;
    }

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public String toString() {
        String before = label.substring(0, startIndex);
        String highlight = label.substring(startIndex, endIndex);
        String after = label.substring(endIndex);
        return before + "(" + highlight + ")" + after + "->" + value;
    }
}
