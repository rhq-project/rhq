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
public class BaselineCondition extends AbstractCondition {

    public BaselineCondition metric(Integer metricId) {
        setMeasurementDefinitionId(metricId);
        return this;
    }

    public BaselineCondition comparator(AlertConditionOperator comparator) {
        setComparator(comparator);
        return this;
    }

    public BaselineCondition percentage(Double percentage) {
        setThreshold(percentage);
        return this;
    }

    /**
     * one of "min", "max" or "mean" - indicates what the threshold is compared to (min/max/avg baseline value)
     *
     * @param baseline
     * @return
     */
    public BaselineCondition baseline(String baseline) {
        overrideOption(baseline);
        return this;
    }

    @Override
    AlertConditionCategory getCategory() {
        return AlertConditionCategory.BASELINE;
    }
}
