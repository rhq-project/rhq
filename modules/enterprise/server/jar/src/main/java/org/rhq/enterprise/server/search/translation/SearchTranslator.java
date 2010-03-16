package org.rhq.enterprise.server.search.translation;

import org.rhq.enterprise.server.search.translation.antlr.RHQLTerm;
import org.rhq.enterprise.server.search.translation.jpql.SearchFragment;

public interface SearchTranslator {

    public SearchFragment getSearchFragment(String alias, RHQLTerm term);
}
