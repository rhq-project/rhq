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

package org.rhq.modules.plugins.jbossas7;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Component class for virtual hosts
 * @author Heiko W. Rupp
 */
public class VHostComponent extends BaseComponent<VHostComponent> implements MeasurementFacet {

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        Set<MeasurementScheduleRequest> leftovers = new HashSet<MeasurementScheduleRequest>(metrics.size());
        for (MeasurementScheduleRequest request : metrics) {
            if (request.getName().equals("_aliases")) {
                ReadAttribute op = new ReadAttribute(getAddress(), "alias");
                Result res = getASConnection().execute(op);
                if (res.isSuccess()) {
                    List<String> aliases = (List<String>) res.getResult();
                    MeasurementDataTrait data;
                    if (aliases != null) {
                        Collections.sort(aliases);
                        String trait = aliases.toString();
                        data = new MeasurementDataTrait(request, trait);
                    } else {
                        data = new MeasurementDataTrait(request, "-none-");
                    }
                    report.addData(data);
                } else
                    getLog().warn("Could not get aliases for " + getAddress() + ": " + res.getFailureDescription());
            } else {
                leftovers.add(request);
            }
        }
        super.getValues(report, leftovers);
    }
}
