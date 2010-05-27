package org.rhq.enterprise.server.search.assist;

import org.rhq.core.domain.search.SearchSubsystem;

public class SearchAssistantFactory {
    private SearchAssistantFactory() {
        // force use of static methods only
    }

    public static SearchAssistant getAssistant(SearchSubsystem searchContext) {
        if (searchContext == SearchSubsystem.RESOURCE) {
            return new ResourceSearchAssistant();
        } else if (searchContext == SearchSubsystem.GROUP) {
            return new GroupSearchAssistant();
        } else {
            throw new IllegalArgumentException("No SearchAssistant found for SearchSubsystem[" + searchContext + "]");
        }
    }
}
