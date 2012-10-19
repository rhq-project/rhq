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
import org.json.JSONObject;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Greg Hinkle
 */
public class HudsonServerComponent implements ResourceComponent {


    private ResourceContext resourceContext;
    private Map<String, JSONArray> healthMap = new HashMap<String, JSONArray>();

    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = resourceContext;
    }

    public void stop() {
    }

    public AvailabilityType getAvailability() {
        try {
            JSONObject server = HudsonJSONUtility.getData(this.resourceContext.getPluginConfiguration().getSimple("urlBase").getStringValue(), 1);


            JSONArray jobs = server.getJSONArray("jobs");
            for (int i = 0; i < jobs.length(); i++) {
                JSONObject job = jobs.getJSONObject(i);

                JSONArray healthArray = job.getJSONArray("healthReport");

                String url = job.getString("url");

                healthMap.put(url, healthArray);
            }

            return AvailabilityType.UP;
        } catch (Exception e) {
            return AvailabilityType.DOWN;
        }
    }

    public JSONArray getProjectHealth(String url) {
        return this.healthMap.get(url);
    }

    public String getPath() {
        return this.resourceContext.getPluginConfiguration().getSimple("urlBase").getStringValue();
    }

}
