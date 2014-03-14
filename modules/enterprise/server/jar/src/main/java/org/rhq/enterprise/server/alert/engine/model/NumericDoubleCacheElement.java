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

/*
 * If even one element in the Measurement cache were a Double, then they all need to be compared
 */
public abstract class NumericDoubleCacheElement extends AbstractCacheElement<Double> {
    public NumericDoubleCacheElement(AlertConditionOperator operator, Double value, int conditionTriggerId) {
        super(operator, value, conditionTriggerId);
    }

    @Override
    public boolean matches(Double providedValue, Object... extraParams) {
        /*
         * allow null/NaN/infinite Numeric elements to be processed;
         *
         * this will support baseline-based AlertConditions whose baselines haven't been calculated yet
         */
        if (alertConditionValue == null || alertConditionValue.isNaN() || alertConditionValue.isInfinite()) {
            return false;
        }

        if (providedValue == null || providedValue.isNaN() || providedValue.isInfinite()) {
            return false;
        }

        if (alertConditionOperator == AlertConditionOperator.GREATER_THAN) {
            return (providedValue.compareTo(alertConditionValue) > 0);

        } else if (alertConditionOperator == AlertConditionOperator.LESS_THAN) {
            return (providedValue.compareTo(alertConditionValue) < 0);

        } else if (alertConditionOperator == AlertConditionOperator.EQUALS) {
            return (providedValue.compareTo(alertConditionValue) == 0);

        } else if (alertConditionOperator == AlertConditionOperator.CHANGES) {
            boolean result;

            result = (alertConditionValue.compareTo(providedValue) != 0);

            alertConditionValue = providedValue;

            return result;

        } else {
            throw new UnsupportedAlertConditionOperatorException(getClass().getSimpleName() + " does not yet support "
                + alertConditionOperator);
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