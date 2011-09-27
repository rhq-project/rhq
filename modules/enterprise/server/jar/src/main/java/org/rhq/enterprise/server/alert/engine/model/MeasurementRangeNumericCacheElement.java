/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

public class MeasurementRangeNumericCacheElement extends MeasurementNumericCacheElement {

    public MeasurementRangeNumericCacheElement(AlertConditionOperator operator, Double loValue, Double hiValue,
        int conditionTriggerId) {
        super(operator, loValue, conditionTriggerId);
        this.alertConditionOperatorOption = hiValue;
    }

    @Override
    public boolean matches(Double providedValue, Object... extraParams) {
        // this.alertConditionValue is the low value of the range
        // this.alertConditionOperationOption is the high value of the range
        // both must be specified, if either are null, return false (non-match)

        if (alertConditionValue == null || alertConditionValue.isNaN() || alertConditionValue.isInfinite()) {
            return false;
        }
        if (alertConditionOperatorOption == null) {
            return false;
        }

        if (providedValue == null || providedValue.isNaN() || providedValue.isInfinite()) {
            return false;
        }

        // there are two ways we can check the value compared to the range, each with either inclusivness or exclusivness.
        // inclusive means if value equals either the lo or hi, it is considered inside the range.
        // exclusive means if value equals either the lo or hi, it is NOT considered inside the range, it is outside the range
        // <  - if the value is inside the range (i.e. between the low and high values), exclusive
        // >  - if the value is outside the range (i.e. lower than the low value or higher than the high value), exclusive
        // <= - if the value is inside the range (i.e. between the low and high values), inclusive
        // >= - if the value is outside the range (i.e. lower than the low value or higher than the high value), inclusive
        // Example:
        //   Given a value of 20, with a low-high range of 20...50.
        //   <  (inside,  exclusive) - NO match - 20 is not considered inside the range but we are looking for values inside the range 
        //   >  (outside, exclusive) - MATCH    - 20 is not considered inside the range and we are looking for values outside the range 
        //   <= (inside,  inclusive) - MATCH    - 20 is considered inside the range and we are looking for values inside the range 
        //   >= (outside, inclusive) - NO match - 20 is considered inside the range but we are looking for values outside the range 

        if (alertConditionOperator == AlertConditionOperator.LESS_THAN) {
            if ((providedValue.compareTo(this.alertConditionValue) > 0)
                && (providedValue.compareTo((Double) this.alertConditionOperatorOption) < 0)) {
                return true;
            }
        } else if (alertConditionOperator == AlertConditionOperator.GREATER_THAN) {
            if (!((providedValue.compareTo(this.alertConditionValue) > 0) && (providedValue
                .compareTo((Double) this.alertConditionOperatorOption) < 0))) {
                return true;
            }
        } else if (alertConditionOperator == AlertConditionOperator.LESS_THAN_OR_EQUAL_TO) {
            if ((providedValue.compareTo(this.alertConditionValue) >= 0)
                && (providedValue.compareTo((Double) this.alertConditionOperatorOption) <= 0)) {
                return true;
            }
        } else if (alertConditionOperator == AlertConditionOperator.GREATER_THAN_OR_EQUAL_TO) {
            if (!((providedValue.compareTo(this.alertConditionValue) >= 0) && (providedValue
                .compareTo((Double) this.alertConditionOperatorOption) <= 0))) {
                return true;
            }
        }

        return false;
    }

    @Override
    public AlertConditionOperator.Type getOperatorSupportsType(AlertConditionOperator operator) {
        if ((operator == AlertConditionOperator.GREATER_THAN) || (operator == AlertConditionOperator.LESS_THAN)
            || (operator == AlertConditionOperator.GREATER_THAN_OR_EQUAL_TO)
            || (operator == AlertConditionOperator.LESS_THAN_OR_EQUAL_TO)) {
            return operator.getDefaultType();
        }

        return AlertConditionOperator.Type.NONE;
    }
}
