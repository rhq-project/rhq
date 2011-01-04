/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.server.search.translation;

import static org.rhq.enterprise.server.search.common.SearchQueryGenerationUtility.getJPQLForString;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.enterprise.server.search.SearchExpressionException;
import org.rhq.enterprise.server.search.translation.antlr.RHQLAdvancedTerm;
import org.rhq.enterprise.server.search.translation.antlr.RHQLComparisonOperator;
import org.rhq.enterprise.server.search.translation.jpql.SearchFragment;

/**
 * @author Joseph Marques
 */
public class GroupSearchTranslator extends AbstractSearchTranslator {

    public GroupSearchTranslator(Subject subject) {
        super(subject);
    }

    public SearchFragment getSearchFragment(String alias, RHQLAdvancedTerm term) {
        String path = term.getPath();
        RHQLComparisonOperator op = term.getOperator();
        String param = term.getParam();

        String filter = term.getValue();

        if (path.equals("availability")) {
            if (op == RHQLComparisonOperator.NOT_NULL || op == RHQLComparisonOperator.NULL) {
                return new SearchFragment(SearchFragment.Type.WHERE_CLAUSE, "true");
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
                SearchFragment.Type.PRIMARY_KEY_SUBQUERY, "SELECT rg.id" //
                    + "  FROM ResourceGroup rg " //
                    + " WHERE ( SELECT AVG( iavail.availabilityType ) " //
                    + "           FROM rg.explicitResources ires " //
                    + "           JOIN ires.currentAvailability iavail ) " + numericAvailabilityFragment);

        } else if (path.equals("category")) {
            return new SearchFragment(SearchFragment.Type.WHERE_CLAUSE, //
                getJPQLForEnum(alias + ".resourceType.category", op, filter, ResourceCategory.class, false));

        } else if (path.equals("type")) {
            return new SearchFragment(SearchFragment.Type.WHERE_CLAUSE, //
                getJPQLForString(alias + ".resourceType.name", op, filter));

        } else if (path.equals("plugin")) {
            return new SearchFragment(SearchFragment.Type.WHERE_CLAUSE, //
                getJPQLForString(alias + ".resourceType.plugin", op, filter));

        } else if (path.equals("name")) {
            return new SearchFragment(SearchFragment.Type.WHERE_CLAUSE, //
                getJPQLForString(alias + ".name", op, filter));

        } else {
            if (param == null) {
                throw new SearchExpressionException("No search fragment available for " + path);
            } else {
                throw new SearchExpressionException("No search fragment available for " + path + "[" + param + "]");
            }
        }
    }

}
