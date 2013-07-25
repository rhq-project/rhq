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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
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
        String key = this.resourceContext.getResourceKey();
        String serverPath = this.resourceContext.getParentResourceComponent().getPath();
        JSONArray projects = SonarQubeJSONUtility.getDatas(serverPath, "resources?resource=" + key);
        ;
        if (projects != null && projects.length() == 1) {
            return AvailabilityType.UP;
        }
        return AvailabilityType.DOWN;
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        try {

            String key = this.resourceContext.getResourceKey();
            String serverPath = this.resourceContext.getParentResourceComponent().getPath();
            JSONArray projects = SonarQubeJSONUtility.getDatas(serverPath, "resources?resource=" + key);

            if (projects != null && projects.length() == 1) {
                String date = projects.getJSONObject(0).getString("date");
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss Z");
                Date lastAnalysisTime = df.parse(date.replaceAll("(?=.{5}$)", " "));
                //    DateFormat.getDateInstance().parse(date);
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
