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
import org.rhq.core.domain.measurement.AvailabilityType;

/**
 * @author Joseph Marques
 */

public final class AvailabilityCacheElement extends AbstractEnumCacheElement<AvailabilityType> {

    public AvailabilityCacheElement(AlertConditionOperator operator, AvailabilityType value, int conditionTriggerId) {

        super(operator, null, value, conditionTriggerId);
    }

    @Override
    public boolean matches(AvailabilityType providedValue, Object... extraParams) {
        if (null == providedValue) {
            return false;
        }

        boolean result = false;

        switch (alertConditionOperator) {
        case AVAIL_GOES_DOWN:
            result = (AvailabilityType.DOWN == providedValue && AvailabilityType.DOWN != alertConditionValue);
            break;
        case AVAIL_GOES_NOT_UP:
            result = (AvailabilityType.UP != providedValue && AvailabilityType.UP == alertConditionValue);
            break;
        case AVAIL_GOES_UP:
            result = (AvailabilityType.UP == providedValue && AvailabilityType.UP != alertConditionValue);
            break;
        case AVAIL_GOES_DISABLED:
            result = (AvailabilityType.DISABLED == providedValue && AvailabilityType.DISABLED != alertConditionValue);
            break;
        case AVAIL_GOES_UNKNOWN:
            result = (AvailabilityType.UNKNOWN == providedValue && AvailabilityType.UNKNOWN != alertConditionValue);
            break;
        }

        alertConditionValue = providedValue;

        return result;
    }

    @Override
    public AlertConditionOperator.Type getOperatorSupportsType(AlertConditionOperator operator) {
        switch (operator) {
        case AVAIL_GOES_DOWN:
        case AVAIL_GOES_NOT_UP:
        case AVAIL_GOES_UP:
        case AVAIL_GOES_DISABLED:
        case AVAIL_GOES_UNKNOWN:
        case AVAIL_DURATION_DOWN:
        case AVAIL_DURATION_NOT_UP:

            return operator.getDefaultType();

        default:
            return AlertConditionOperator.Type.NONE;
        }
    }
}