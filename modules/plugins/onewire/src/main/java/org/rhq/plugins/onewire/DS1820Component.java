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
package org.rhq.plugins.onewire;

import java.util.Set;

import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.container.OneWireContainer10;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

/**
 * Resource component for DS1820 compatible thermometer chips 
 * @author Heiko W. Rupp
 *
 */
public class DS1820Component implements ResourceComponent<OneWireAdapterComponent>, MeasurementFacet {

    DSPortAdapter adapter;
    OneWireContainer10 container;
    boolean wantCelcius = true;
    OneWireAdapterComponent parent;
    private Log log = LogFactory.getLog(DS1820Component.class);

    /* (non-Javadoc)
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#getAvailability()
     */
    public AvailabilityType getAvailability() {

        boolean present = false;
        try {
            present = container.isPresent();
        } catch (OneWireException e) {
            //            parent.reopenAdapter();
        }

        return present ? AvailabilityType.UP : AvailabilityType.DOWN;
    }

    /* (non-Javadoc)
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#start(org.rhq.core.pluginapi.inventory.ResourceContext)
     */
    public void start(ResourceContext<OneWireAdapterComponent> context) throws InvalidPluginConfigurationException,
        Exception {

        parent = context.getParentResourceComponent();
        adapter = parent.getAdapter();
        String device = context.getResourceKey();
        container = (OneWireContainer10) adapter.getDeviceContainer(device);
        PropertySimple unitProp = context.getPluginConfiguration().getSimple("unit");
        if (unitProp != null) {
            Boolean unit = unitProp.getBooleanValue();
            if (unit != null)
                wantCelcius = unit.booleanValue();
        }
    }

    /* (non-Javadoc)
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#stop()
     */
    public void stop() {
        ; // nothing to do
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {

        for (MeasurementScheduleRequest metric : metrics) {
            if ("temperature".equals(metric.getName())) {
                boolean good = true;
                byte[] data = container.readDevice();

                container.doTemperatureConvert(data);
                Thread.sleep(100);

                double temp = container.getTemperature(data);
                /*
                 * I have seen strange spikes above 80 Deg Celcius once in a while 
                 * If we see one, ignore it
                 */
                if (temp > 75.0) {
                    good = false;
                    log.info("Found a spike at " + temp);
                }

                if (!wantCelcius) { // Fahrenheit then
                    temp = 1.8 * temp + 32;
                }

                if (good) {
                    MeasurementDataNumeric value = new MeasurementDataNumeric(metric, temp);
                    report.addData(value);
                }
            }
        }
    }
}
