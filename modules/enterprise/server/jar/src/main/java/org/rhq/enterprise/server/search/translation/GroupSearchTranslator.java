package org.rhq.enterprise.server.search.translation;

import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.enterprise.server.search.translation.antlr.RHQLAdvancedTerm;
import org.rhq.enterprise.server.search.translation.antlr.RHQLComparisonOperator;
import org.rhq.enterprise.server.search.translation.jpql.SearchFragment;
import org.rhq.enterprise.server.search.translation.jpql.SearchFragmentType;

public class GroupSearchTranslator extends AbstractSearchTranslator {

    public SearchFragment getSearchFragment(String alias, RHQLAdvancedTerm term) {
        String path = term.getPath();
        RHQLComparisonOperator op = term.getOperator();
        String param = term.getParam();

        String filter = term.getValue();

        if (path.equals("availability")) {
            if (op == RHQLComparisonOperator.NOT_NULL || op == RHQLComparisonOperator.NULL) {
                return new SearchFragment(SearchFragmentType.WHERE_CLAUSE, "true");
            }

            String numericAvailabilityFragment = null;
            if (op == RHQLComparisonOperator.EQUALS || op == RHQLComparisonOperator.EQUALS_STRICT) {
                numericAvailabilityFragment = " = ";
            } else {
                numericAvailabilityFragment = " != ";
            }

            if (filter.equalsIgnoreCase("up")) {
                numericAvailabilityFragment += "1";
            } else {
                numericAvailabilityFragment += "0";
            }

            return new SearchFragment( //
                SearchFragmentType.PRIMARY_KEY_SUBQUERY, "SELECT rg.id" //
                    + "  FROM ResourceGroup rg " //
                    + " WHERE ( SELECT AVG( iavail.availabilityType ) " //
                    + "           FROM rg.explicitResources ires " //
                    + "           JOIN ires.currentAvailability iavail ) " + numericAvailabilityFragment);

        } else if (path.equals("category")) {
            return new SearchFragment(SearchFragmentType.WHERE_CLAUSE, //
                getJPQLForEnum(alias + ".resourceType.category", op, filter, ResourceCategory.class, false));

        } else if (path.equals("type")) {
            return new SearchFragment(SearchFragmentType.WHERE_CLAUSE, //
                getJPQLForString(alias + ".resourceType.name", op, filter));

        } else if (path.equals("plugin")) {
            return new SearchFragment(SearchFragmentType.WHERE_CLAUSE, //
                getJPQLForString(alias + ".resourceType.plugin", op, filter));

        } else if (path.equals("name")) {
            return new SearchFragment(SearchFragmentType.WHERE_CLAUSE, //
                getJPQLForString(alias + ".name", op, filter));

        } else {
            if (param == null) {
                throw new IllegalArgumentException("No search fragment available for " + path);
            } else {
                throw new IllegalArgumentException("No search fragment available for " + path + "[" + param + "]");
            }
        }
    }

}
