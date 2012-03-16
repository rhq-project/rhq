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

import org.rhq.core.domain.alert.AlertConditionOperator;
import org.rhq.core.domain.measurement.AvailabilityType;

/**
 * @author Jay Shaughnessy
 */
public class AvailabilityDurationComposite {
    private int conditionId;
    private AlertConditionOperator operator;
    private int resourceId;
    private AvailabilityType availabilityType;
    private long duration;

    public AvailabilityDurationComposite(int conditionId, AlertConditionOperator operator, int resourceId,
        AvailabilityType availabilityType, long duration) {
        super();
        this.conditionId = conditionId;
        this.operator = operator;
        this.resourceId = resourceId;
        this.availabilityType = availabilityType;
        this.duration = duration;
    }

    public int getConditionId() {
        return conditionId;
    }

    public AlertConditionOperator getOperator() {
        return operator;
    }

    public int getResourceId() {
        return resourceId;
    }

    public AvailabilityType getAvailabilityType() {
        return availabilityType;
    }

    public long getDuration() {
        return duration;
    }

}
