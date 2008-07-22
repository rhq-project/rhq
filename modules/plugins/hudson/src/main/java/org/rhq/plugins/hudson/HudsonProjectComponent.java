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
package org.rhq.plugins.hudson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.rhq.core.domain.measurement.*;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

import java.util.Date;
import java.util.Set;

/**
 * @author Greg Hinkle
 */
public class HudsonProjectComponent implements ResourceComponent<HudsonServerComponent>, MeasurementFacet {

    ResourceContext<HudsonServerComponent> resourceContext;

    public void start(ResourceContext<HudsonServerComponent> hudsonServerComponentResourceContext) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = hudsonServerComponentResourceContext;
    }

    public void stop() {

    }

    public AvailabilityType getAvailability() {
        try {
            JSONArray healthArray =
                    this.resourceContext.getParentResourceComponent().getProjectHealth(this.resourceContext.getResourceKey());
            if (healthArray != null) {
                JSONObject healthReport = healthArray.getJSONObject(0);
                return (healthReport.getInt("score") == 100) ? AvailabilityType.UP : AvailabilityType.DOWN;
            }
            return AvailabilityType.DOWN;
        } catch (JSONException e) {
            //e.printStackTrace();
            return AvailabilityType.DOWN;
        }

    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        try {
            String path = this.resourceContext.getResourceKey();
            JSONObject job = HudsonJSONUtility.getData(path, 1);

            JSONObject lastSuccessfulBuild = job.getJSONObject("lastSuccessfulBuild");
            JSONObject lastBuild = job.getJSONObject("lastBuild");
            JSONArray healthArray = job.getJSONArray("healthReport");
            JSONObject healthReport = healthArray.getJSONObject(0);

            for (MeasurementScheduleRequest request : metrics) {
                try {
                    if (request.getName().equals("lastSuccessfulBuildNumber")) {
                        report.addData(new MeasurementDataTrait(request, lastSuccessfulBuild.getString("number")));
                    } else if (request.getName().equals("lastSuccessfulBuildTime")) {
                        report.addData(new MeasurementDataTrait(request, new Date(lastSuccessfulBuild.getLong("timestamp")).toString()));
                    } else if (request.getName().equals("lastBuildNumber")) {
                        report.addData(new MeasurementDataTrait(request, lastBuild.getString("number")));
                    } else if (request.getName().equals("lastBuildTime")) {
                        report.addData(new MeasurementDataTrait(request, new Date(lastBuild.getLong("timestamp")).toString()));
                    } else if (request.getName().equals("lastBuildResult")) {
                        report.addData(new MeasurementDataTrait(request, lastBuild.getString("result")));
                    } else if (request.getName().equals("healthScore")) {
                        report.addData(new MeasurementDataNumeric(request, healthReport.getDouble("score") / 100d));
                    } else if (request.getName().equals("lastBuildDuration")) {
                        report.addData(new MeasurementDataNumeric(request, lastBuild.getDouble("duration")));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
