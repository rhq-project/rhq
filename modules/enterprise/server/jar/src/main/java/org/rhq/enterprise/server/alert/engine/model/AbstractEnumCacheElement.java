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

import org.rhq.core.domain.alert.AlertConditionOperator;

/**
 * @author Joseph Marques
 */

public abstract class AbstractEnumCacheElement<E extends Enum<E>> extends AbstractCacheElement<E> {
    public AbstractEnumCacheElement(AlertConditionOperator operator, E value, int conditionTriggerId) {
        super(operator, value, conditionTriggerId);
    }

    public AbstractEnumCacheElement(AlertConditionOperator operator, Object operatorOption, E value,
        int conditionTriggerId) {
        super(operator, operatorOption, value, conditionTriggerId);
    }

    @Override
    public boolean matches(E providedValue, Object... extraParams) {
        if (providedValue == null) {
            return false;
        }

        if (alertConditionOperator == AlertConditionOperator.EQUALS) {
            return providedValue.equals(alertConditionValue);

        } else if (alertConditionOperator == AlertConditionOperator.CHANGES) {
            boolean result;

            if (null == alertConditionValue) {
                result = (null != providedValue);
            } else {
                result = !alertConditionValue.equals(providedValue);
            }

            alertConditionValue = providedValue;

            return result;

        } else if (alertConditionOperator == AlertConditionOperator.LESS_THAN_OR_EQUAL_TO) {
            return (providedValue.ordinal() <= alertConditionValue.ordinal());

        } else if (alertConditionOperator == AlertConditionOperator.LESS_THAN) {
            return (providedValue.ordinal() < alertConditionValue.ordinal());

        } else if (alertConditionOperator == AlertConditionOperator.GREATER_THAN) {
            return (providedValue.ordinal() > alertConditionValue.ordinal());

        } else if (alertConditionOperator == AlertConditionOperator.GREATER_THAN_OR_EQUAL_TO) {
            return (providedValue.ordinal() >= alertConditionValue.ordinal());

        } else if (alertConditionOperator == AlertConditionOperator.CHANGES_TO) {
            boolean result;

            // if the last value recorded doesn't equal the operator option, but the current value does
            result = !alertConditionOperatorOption.equals(alertConditionValue)
                && alertConditionOperatorOption.equals(providedValue);

            alertConditionValue = providedValue;

            return result;

        } else if (alertConditionOperator == AlertConditionOperator.CHANGES_FROM) {
            Boolean result;

            // if the last value recorded equals the operator option, but the current value does not
            result = alertConditionOperatorOption.equals(alertConditionValue)
                && !alertConditionOperatorOption.equals(providedValue);

            alertConditionValue = providedValue;

            return result;

        } else {
            throw new UnsupportedAlertConditionOperatorException(getClass().getSimpleName() + " does not yet support "
                + alertConditionOperator);
        }
    }

    @Override
    public AlertConditionOperator.Type getOperatorSupportsType(AlertConditionOperator operator) {
        if ((operator == AlertConditionOperator.LESS_THAN_OR_EQUAL_TO)
            || (operator == AlertConditionOperator.LESS_THAN) || (operator == AlertConditionOperator.EQUALS)
            || (operator == AlertConditionOperator.GREATER_THAN)
            || (operator == AlertConditionOperator.GREATER_THAN_OR_EQUAL_TO)
            || (operator == AlertConditionOperator.CHANGES) || (operator == AlertConditionOperator.CHANGES_TO)
            || (operator == AlertConditionOperator.CHANGES_FROM)) {
            return operator.getDefaultType();
        }

        return AlertConditionOperator.Type.NONE;
    }
}