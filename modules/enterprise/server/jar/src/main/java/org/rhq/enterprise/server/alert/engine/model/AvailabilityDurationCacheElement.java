/*
 * RHQ Management Platform
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

import java.util.List;

import org.rhq.core.domain.alert.AlertConditionOperator;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Jay Shaughnessy
 */

public final class AvailabilityDurationCacheElement extends AbstractEnumCacheElement<AvailabilityType> {

    private int alertDefinitionId;

    /**
     * @param operator
     * @param operatorOption the duration, in seconds (as String)
     * @param value
     * @param conditionTriggerId this is actually the alertConditionId, renamed here.
     */
    public AvailabilityDurationCacheElement(int alertDefinitionId, AlertConditionOperator operator,
        String operatorOption, AvailabilityType value, int conditionTriggerId) {

        super(operator, operatorOption, value, conditionTriggerId);

        this.alertDefinitionId = alertDefinitionId;
    }

    /**
     * Here we check to see if an availability change for a resource should initiate an avail duration check. For
     * each relevant avail duration condition for the resource schedule a job to check the avail state after the
     * condition's duration.
     *
     * @param cacheElements
     * @param resource
     * @param providedValue
     */
    @SuppressWarnings("incomplete-switch")
    public static void checkCacheElements(List<AvailabilityDurationCacheElement> cacheElements, Resource resource,
        Availability providedValue) {
        if (null == cacheElements) {
            return; // nothing to do
        }

        AvailabilityType availType = providedValue.getAvailabilityType();

        for (AvailabilityDurationCacheElement cacheElement : cacheElements) {
            switch (cacheElement.getAlertConditionOperator()) {
            case AVAIL_DURATION_DOWN:
                if (AvailabilityType.DOWN == availType
                    && AvailabilityType.DOWN != cacheElement.getAlertConditionValue()) {

                    LookupUtil.getAvailabilityManager().scheduleAvailabilityDurationCheck(cacheElement, resource,
                        providedValue.getStartTime());
                }
                break;
            case AVAIL_DURATION_NOT_UP:
                if (AvailabilityType.UP != availType && AvailabilityType.UP == cacheElement.getAlertConditionValue()) {

                    LookupUtil.getAvailabilityManager().scheduleAvailabilityDurationCheck(cacheElement, resource,
                        providedValue.getStartTime());
                }
                break;
            }

            cacheElement.setAlertConditionValue(availType);
        }
    }

    public int getAlertDefinitionId() {
        return alertDefinitionId;
    }

    @Override
    public boolean matches(AvailabilityType providedValue, Object... extraParams) {
        if (null == providedValue) {
            return false;
        }

        boolean result = false;

        switch (alertConditionOperator) {
        case AVAIL_DURATION_DOWN:
            result = (AvailabilityType.DOWN == providedValue);
            break;
        case AVAIL_DURATION_NOT_UP:
            result = (AvailabilityType.UP != providedValue);
            break;
        default:
            break;
        }

        alertConditionValue = providedValue;

        return result;
    }

    @Override
    public AlertConditionOperator.Type getOperatorSupportsType(AlertConditionOperator operator) {
        switch (operator) {
        case AVAIL_DURATION_DOWN:
        case AVAIL_DURATION_NOT_UP:
            return operator.getDefaultType();

        default:
            return AlertConditionOperator.Type.NONE;
        }
    }
}
