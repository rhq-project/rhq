/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.core.domain.alert.builder.condition;

import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.AlertConditionOperator;

/**
 * @author Michael Burman
 */
public class RangeCondition extends AbstractCondition {

    public RangeCondition metric(Integer metricId) {
        setMeasurementDefinitionId(metricId);
        return this;
    }

    /**
     * OR_EQUAL = inclusive
     * GREATER_THAN = Outside
     * LESS_THAN = Inside
     * @param comparator
     * @return
     */
    public RangeCondition comparator(AlertConditionOperator comparator) {
        setComparator(comparator);
        return this;
    }

    public RangeCondition min(Double min) {
        setThreshold(min);
        return this;
    }

    public RangeCondition max(Double max) {
        overrideOption(max.toString());
        return this;
    }

    @Override
    AlertConditionCategory getCategory() {
        return AlertConditionCategory.RANGE;
    }
}
