/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.helpers.pluginGen.test;

import org.rhq.helpers.pluginAnnotations.agent.ConfigProperty;
import org.rhq.helpers.pluginAnnotations.agent.DataType;
import org.rhq.helpers.pluginAnnotations.agent.DisplayType;
import org.rhq.helpers.pluginAnnotations.agent.MeasurementType;
import org.rhq.helpers.pluginAnnotations.agent.Metric;
import org.rhq.helpers.pluginAnnotations.agent.Operation;
import org.rhq.helpers.pluginAnnotations.agent.Parameter;
import org.rhq.helpers.pluginAnnotations.agent.RhqType;
import org.rhq.helpers.pluginAnnotations.agent.Units;

/**
 * Just a sample
 * @author Heiko W. Rupp
 */

public class FooBean {

    @Metric(description = "How often was this bean invoked", displayType = DisplayType.SUMMARY, measurementType = MeasurementType.DYNAMIC,
        units = Units.SECONDS)
    int invocationCount;

    @Metric(description = "Just a foo", dataType = DataType.TRAIT)
    String lastCommand;

    @Operation(description = "Increase the invocation count")
    public int increaseCounter() {
        invocationCount++;
        return invocationCount;
    }

    @Operation(description = "Decrease the counter")
    public void decreaseCounter(@Parameter(description = "How much to decrease?", name = "by") int by) {
        invocationCount -= by;
    }

    @ConfigProperty(scope = ConfigProperty.Scope.PLUGIN, displayName="The Password",
        readOnly = false, property="thePassword",description = "A password", rhqType = RhqType.PASSWORD)
    String password;

    @ConfigProperty(scope = ConfigProperty.Scope.RESOURCE)
    int defaultSteps;

}
