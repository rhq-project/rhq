package org.rhq.enterprise.server.search.translation;

import org.rhq.core.domain.search.SearchSubsystem;

public class SearchTranslatorFactory {
    private SearchTranslatorFactory() {
        // force use of static methods only
    }

    public static SearchTranslator getTranslator(SearchSubsystem searchContext) {
        if (searchContext == SearchSubsystem.RESOURCE) {
            return new ResourceSearchTranslator();
        } else if (searchContext == SearchSubsystem.GROUP) {
            return new GroupSearchTranslator();
        }
        throw new IllegalArgumentException("No SearchTranslator found for SearchContext[" + searchContext + "]");
    }
}
