package org.rhq.enterprise.server.search.translation.jpql;

public class SearchFragment {

    private final SearchFragmentType type;
    private final String fragment;

    public SearchFragment(SearchFragmentType type, String fragment) {
        this.type = type;
        this.fragment = fragment;
    }

    public SearchFragmentType getType() {
        return type;
    }

    public String getFragment() {
        return fragment;
    }
}
