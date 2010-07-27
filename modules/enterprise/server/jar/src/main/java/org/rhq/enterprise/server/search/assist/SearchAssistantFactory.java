package org.rhq.enterprise.server.search.assist;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.search.SearchSubsystem;

public class SearchAssistantFactory {
    private SearchAssistantFactory() {
        // force use of static methods only
    }

    public static SearchAssistant getAssistant(Subject subject, SearchSubsystem searchContext) {
        if (searchContext == SearchSubsystem.RESOURCE) {
            return new ResourceSearchAssistant(subject, null);
        } else if (searchContext == SearchSubsystem.GROUP) {
            return new GroupSearchAssistant(subject, null);
        } else {
            throw new IllegalArgumentException("No SearchAssistant found for SearchSubsystem[" + searchContext + "]");
        }
    }

    public static SearchAssistant getTabAwareAssistant(Subject subject, SearchSubsystem searchContext, String tab) {
        if (searchContext == SearchSubsystem.RESOURCE) {
            return new ResourceSearchAssistant(subject, tab);
        } else if (searchContext == SearchSubsystem.GROUP) {
            return new GroupSearchAssistant(subject, tab);
        } else {
            throw new IllegalArgumentException("No SearchAssistant found for SearchSubsystem[" + searchContext + "]");
        }
    }
}
