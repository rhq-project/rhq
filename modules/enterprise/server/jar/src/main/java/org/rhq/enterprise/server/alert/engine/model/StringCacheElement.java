/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.alert.engine.model;

/*
 * If even one element in the Measurement cache were a Double, then they all need to be compared
 */
public class StringCacheElement extends AbstractCacheElement<String> {
    public StringCacheElement(AlertConditionOperator operator, String value, int conditionTriggerId) {
        super(operator, value, conditionTriggerId);
    }

    @Override
    public boolean matches(String providedValue) {
        if (providedValue == null) {
            return false;
        }

        // changes requires a slightly
        if (alertConditionOperator == AlertConditionOperator.CHANGES) {
            Boolean results = null;

            if ((results == null) && (alertConditionValue.equals(providedValue) == false)) {
                results = Boolean.TRUE;
            }

            if (results == null) {
                results = Boolean.FALSE;
            }

            alertConditionValue = providedValue;

            return results;
        }

        int result = alertConditionValue.compareTo(providedValue);

        if (alertConditionOperator == AlertConditionOperator.GREATER_THAN) {
            return result > 0;
        } else if (alertConditionOperator == AlertConditionOperator.LESS_THAN) {
            return result < 0;
        } else if (alertConditionOperator == AlertConditionOperator.EQUALS) {
            return result == 0;
        } else {
            throw new UnsupportedAlertConditionOperatorException(getClass().getSimpleName()
                + ".isActive(String) implementation " + "does not account for all supported operators "
                + "as defined by " + getClass().getSimpleName() + ".supportsOperator(AlertConditionOperator)");
        }
    }

    @Override
    public AlertConditionOperator.Type getOperatorSupportsType(AlertConditionOperator operator) {
        if ((operator == AlertConditionOperator.GREATER_THAN) || (operator == AlertConditionOperator.EQUALS)
            || (operator == AlertConditionOperator.LESS_THAN) || (operator == AlertConditionOperator.CHANGES)) {
            return operator.getDefaultType();
        }

        return AlertConditionOperator.Type.NONE;
    }
}