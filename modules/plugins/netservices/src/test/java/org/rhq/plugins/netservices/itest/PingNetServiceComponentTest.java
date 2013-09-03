/*
 * RHQ Management Platform
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.plugins.netservices.itest;

import static org.rhq.plugins.netservices.util.StringUtil.isNotBlank;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.discovery.MergeResourceResponse;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.plugins.netservices.PingNetServiceComponent;
import org.rhq.plugins.netservices.PingNetServiceComponent.ConfigKeys;

/**
 * @author Thomas Segismont
 */
public class PingNetServiceComponentTest extends NetServiceComponentTest {

    private static final String SERVICE_NAME = "PingService";

    private static final String LOOPBACK = "127.0.0.1";

    private PingNetServiceComponent pingNetServiceComponent;

    @Test(dependsOnMethods = "testPluginLoad")
    public void testManualAdd() throws Exception {
        Configuration configuration = new Configuration();
        configuration.setSimpleValue(ConfigKeys.ADDRESS, LOOPBACK);
        MergeResourceResponse response = getInventoryManager().manuallyAddResource(
            getPluginManager().getMetadataManager().getType(SERVICE_NAME, PLUGIN_NAME), getPlatform().getId(),
            configuration, -1);
        assertNotNull(response, "Manual add response is null");
        @SuppressWarnings("rawtypes")
        ResourceComponent resourceComponent = getInventoryManager().getResourceContainer(response.getResourceId())
            .getResourceComponent();
        assertEquals(resourceComponent.getClass(), PingNetServiceComponent.class);
        pingNetServiceComponent = (PingNetServiceComponent) resourceComponent;
    }

    @Test(dependsOnMethods = "testManualAdd")
    public void testAvailability() throws Exception {
        assertEquals(pingNetServiceComponent.getAvailability(), AvailabilityType.UP);
    }

    @Test(dependsOnMethods = "testAvailability")
    public void testMeasurement() throws Exception {
        MeasurementReport report = new MeasurementReport();
        Set<MeasurementScheduleRequest> metrics = new HashSet<MeasurementScheduleRequest>();
        int scheduleId = 1;
        metrics.add(new MeasurementScheduleRequest(scheduleId++, "ipAddress", 1000, true, DataType.TRAIT));
        metrics.add(new MeasurementScheduleRequest(scheduleId++, "hostName", 1000, true, DataType.TRAIT));
        metrics.add(new MeasurementScheduleRequest(scheduleId++, "responseTime", 1000, true, DataType.MEASUREMENT));
        pingNetServiceComponent.getValues(report, metrics);
        Map<String, Object> datas = new HashMap<String, Object>();
        for (MeasurementData data : report.getNumericData()) {
            datas.put(data.getName(), data.getValue());
        }
        for (MeasurementData data : report.getTraitData()) {
            datas.put(data.getName(), data.getValue());
        }
        assertEquals(getTrait(datas, "ipAddress"), LOOPBACK);
        assertTrue(isNotBlank(getTrait(datas, "hostName")));
        assertTrue(getMetric(datas, "responseTime") >= 0);
    }

}
