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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.modules.plugins.wildfly10;

import java.util.Map;
import java.util.Set;

import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.modules.plugins.wildfly10.json.ReadResource;
import org.rhq.modules.plugins.wildfly10.json.Result;

public class RAConnectionDefinitionRuntimeComponent extends BaseComponent<BaseComponent<?>> {

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        if (!metrics.isEmpty()) {
            ReadResource op = new ReadResource(getAddress());
            op.includeRuntime(true);
            Result result = getASConnection().execute(op);
            if (result.isSuccess()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) result.getResult();
                for (MeasurementScheduleRequest request : metrics) {
                    Object value = data.get(request.getName());
                    Double d = Double.parseDouble(getStringValue(value));
                    report.addData(new MeasurementDataNumeric(request, d));
                }
            }
        }
    };
}
