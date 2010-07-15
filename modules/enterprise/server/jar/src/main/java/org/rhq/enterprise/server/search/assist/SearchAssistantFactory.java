package org.rhq.enterprise.server.search.assist;

import org.rhq.core.domain.search.SearchSubsystem;

public class SearchAssistantFactory {
    private SearchAssistantFactory() {
        // force use of static methods only
    }

    public static SearchAssistant getAssistant(SearchSubsystem searchContext) {
        if (searchContext == SearchSubsystem.RESOURCE) {
            return new ResourceSearchAssistant(null);
        } else if (searchContext == SearchSubsystem.GROUP) {
            return new GroupSearchAssistant(null);
        } else {
            throw new IllegalArgumentException("No SearchAssistant found for SearchSubsystem[" + searchContext + "]");
        }
    }

    public static SearchAssistant getTabAwareAssistant(SearchSubsystem searchContext, String tab) {
        if (searchContext == SearchSubsystem.RESOURCE) {
            return new ResourceSearchAssistant(tab);
        } else if (searchContext == SearchSubsystem.GROUP) {
            return new GroupSearchAssistant(tab);
        } else {
            throw new IllegalArgumentException("No SearchAssistant found for SearchSubsystem[" + searchContext + "]");
        }
    }
}
