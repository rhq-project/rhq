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
package org.rhq.plugins.sonarqube;

import java.text.DateFormat;
import java.util.Date;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

/**
 * @author Jeremie lagarde
 */
public class SonarQubeProjectComponent implements ResourceComponent<SonarQubeServerComponent>, MeasurementFacet {

    private static final Log LOG = LogFactory.getLog(SonarQubeProjectComponent.class);

    private ResourceContext<SonarQubeServerComponent> resourceContext;

    public void start(ResourceContext<SonarQubeServerComponent> hudsonServerComponentResourceContext)
	throws InvalidPluginConfigurationException, Exception {
	this.resourceContext = hudsonServerComponentResourceContext;
    }

    public void stop() {

    }

    public AvailabilityType getAvailability() {
	try {
	    JSONArray healthArray = this.resourceContext.getParentResourceComponent().getProjectHealth(
		this.resourceContext.getResourceKey());
	    if (healthArray != null) {
		JSONObject healthReport = healthArray.getJSONObject(0);
		return (healthReport.getInt("score") == 100) ? AvailabilityType.UP : AvailabilityType.DOWN;
	    }
	    return AvailabilityType.DOWN;
	} catch (JSONException e) {
	    // e.printStackTrace();
	    return AvailabilityType.DOWN;
	}

    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
	try {

	    String key = this.resourceContext.getResourceKey();
	    String serverPath = this.resourceContext.getParentResourceComponent().getPath();
	    JSONObject resources = SonarQubeJSONUtility.getData(serverPath, "resources?resource=" + key);
	    JSONArray projects = resources.getJSONArray("resource");
	    if (projects.length() == 1) {
		String date = projects.getJSONObject(0).getString("date");
		Date lastAnalysisTime = DateFormat.getDateInstance().parse(date);
		long currentTime = System.currentTimeMillis();
		for (MeasurementScheduleRequest request : metrics) {
		    try {
			if (request.getName().equals("lastAnalysisTime") && lastAnalysisTime != null) {
			    report.addData(new MeasurementDataTrait(request, lastAnalysisTime.toString()));
			} else if (request.getName().equals("lastAnalysisElapsedTime") && lastAnalysisTime != null) {
			    report.addData(new MeasurementDataNumeric(request, (currentTime - lastAnalysisTime
				.getTime()) / 1000d));
			}

		    } catch (Exception e) {
			LOG.warn(e);
		    }
		}
	    }
	} catch (Exception e) {
	    LOG.warn(e);
	}
    }
}
