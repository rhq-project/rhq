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

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.server.search.SearchExpressionException;
import org.rhq.enterprise.server.search.translation.antlr.RHQLComparisonOperator;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Joseph Marques
 */
public abstract class AbstractSearchTranslator implements SearchTranslator {

    private int subjectId;
    private boolean requiresAuthorizationFragment;

    public AbstractSearchTranslator(Subject subject) {
        this.subjectId = subject.getId();
        this.requiresAuthorizationFragment = !LookupUtil.getAuthorizationManager().isInventoryManager(subject);
    }

    public int getSubjectId() {
        return subjectId;
    }

    public boolean requiresAuthorizationFragment() {
        return requiresAuthorizationFragment;
    }

    protected final String conditionallyAddAuthzFragment(String fragment) {
        if (requiresAuthorizationFragment == false) {
            return "";
        }

        return " AND " + fragment;
    }

    protected String getJPQLForEnum(String fragment, RHQLComparisonOperator operator, String value,
        Class<? extends Enum<?>> enumClass, boolean useOrdinal) {
        if (operator == RHQLComparisonOperator.NULL || //
            operator == RHQLComparisonOperator.NOT_NULL) {
            return fragment + operator.getDefaultTranslation();

        } else if (operator == RHQLComparisonOperator.EQUALS || //
            operator == RHQLComparisonOperator.EQUALS_STRICT || //
            operator == RHQLComparisonOperator.NOT_EQUALS || //
            operator == RHQLComparisonOperator.NOT_EQUALS_STRICT) {
            return fragment + operator.getDefaultTranslation() + getEnum(enumClass, value, useOrdinal);

        } else {
            throw new SearchExpressionException("Unsupported operator " + operator);
        }
    }

    private String getEnum(Class<? extends Enum<?>> enumClass, String value, boolean useOrdinal) {
        value = value.toLowerCase();
        for (Enum<?> nextEnum : enumClass.getEnumConstants()) {
            if (nextEnum.name().toLowerCase().equals(value)) {
                if (useOrdinal) {
                    return String.valueOf(nextEnum.ordinal());
                } else {
                    return quote(nextEnum.name());
                }
            }
        }
        throw new SearchExpressionException("No enum of type '" + enumClass.getSimpleName() + "' with name matching '"
            + value + "'");
    }

    protected String quote(String data) {
        return "'" + data + "'";
    }

    protected final String addFragmentIfParameterNotValue(String fragment, String parameter, String value) {
        if (!parameter.equalsIgnoreCase(value)) {
            return fragment;
        }
        return "";
    }

}
