/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.alert.description;

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.util.MeasurementConverter;

/**
 * Describes <code>THRESHOLD </code> {@link AlertCondition}s.
 *
 * @author Justin Harris
 */
public class ThresholdDescriber extends AlertConditionDescriber {

    @Override
    public AlertConditionCategory[] getDescribedCategories() {
        return makeCategories(AlertConditionCategory.THRESHOLD);
    }

    @Override
    public void createDescription(AlertCondition condition, StringBuilder builder) {
        builder.append(condition.getName());
        builder.append(' ');
        builder.append(condition.getComparator());
        builder.append(' ');
        builder.append(getMeasurementDescription(condition));
    }

    private String getMeasurementDescription(AlertCondition condition) {
        double threshold = condition.getThreshold();
        MeasurementUnits units = condition.getMeasurementDefinition().getUnits();

        return MeasurementConverter.format(threshold, units, true);
    }
}
