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

import java.util.regex.Pattern;

import org.rhq.core.domain.alert.AlertConditionOperator;

/**
 * @author Joseph Marques
 */

/*
 * If even one element in the Measurement cache were a Double, then they all need to be compared
 */
public abstract class StringCacheElement extends AbstractCacheElement<String> {
    final Pattern pattern;

    public StringCacheElement(AlertConditionOperator operator, String value, int conditionTriggerId) {
        super(operator, value, conditionTriggerId);
        if (operator.equals(AlertConditionOperator.REGEX)) {
            /*
             * assume that the user meant to match characters before and after whatever pattern they used; this
             * makes the UI simpler to understand for those that only ever care about simple substring matching
             */
            if (!value.startsWith("^")) {
                value = ".*" + value;
            }
            if (!value.endsWith("$")) {
                value = value + ".*";
            }
            pattern = Pattern.compile(value, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
        } else {
            pattern = null;
        }
    }

    @Override
    public boolean matches(String providedValue, Object... extraParams) {
        if (providedValue == null) {
            return false;
        }

        // changes operator requires a slightly different logic as alertConditionValue allows null
        if (alertConditionOperator == AlertConditionOperator.CHANGES) {
            boolean result;

            if (null == alertConditionValue) {
                result = (null != providedValue);
            } else {
                result = !alertConditionValue.equals(providedValue);
            }

            alertConditionValue = providedValue;

            return result;
        }

        int result = alertConditionValue.compareTo(providedValue);

        if (alertConditionOperator == AlertConditionOperator.GREATER_THAN) {
            return result > 0;
        } else if (alertConditionOperator == AlertConditionOperator.LESS_THAN) {
            return result < 0;
        } else if (alertConditionOperator == AlertConditionOperator.EQUALS) {
            return result == 0;
        } else if (alertConditionOperator == AlertConditionOperator.REGEX) {
            return pattern.matcher(providedValue).matches();
        } else {
            throw new UnsupportedAlertConditionOperatorException(getClass().getSimpleName() + " does not yet support "
                + alertConditionOperator);
        }
    }

    @Override
    public AlertConditionOperator.Type getOperatorSupportsType(AlertConditionOperator operator) {
        if ((operator == AlertConditionOperator.GREATER_THAN) || (operator == AlertConditionOperator.EQUALS)
            || (operator == AlertConditionOperator.LESS_THAN) || (operator == AlertConditionOperator.CHANGES)
            || (operator == AlertConditionOperator.REGEX)) {
            return operator.getDefaultType();
        }

        return AlertConditionOperator.Type.NONE;
    }
}