 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
package org.rhq.plugins.platform;

import java.net.InetAddress;
import java.util.List;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.util.ObjectUtil;
import org.rhq.core.system.NetworkAdapterInfo;
import org.rhq.core.system.NetworkAdapterStats;

public class NetworkAdapterComponent implements ResourceComponent<PlatformComponent>, MeasurementFacet {
    private final Log log = LogFactory.getLog(NetworkAdapterComponent.class);

    private ResourceContext<PlatformComponent> context;

    public void start(ResourceContext<PlatformComponent> resourceContext) {
        this.context = resourceContext;
    }

    public void stop() {
    }

    public AvailabilityType getAvailability() {
        if (getInfo().getOperationalStatus() == NetworkAdapterInfo.OperationState.UP) {
            return AvailabilityType.UP;
        }

        return AvailabilityType.DOWN;
    }

    private NetworkAdapterInfo getInfo() {
        for (NetworkAdapterInfo info : this.context.getSystemInformation().getAllNetworkAdapters()) {
            if (context.getResourceKey().equals(info.getName())) {
                return info;
            }
        }

        throw new RuntimeException("Could not find network adapter info [" + context.getResourceKey() + "]");
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) {
        NetworkAdapterInfo info = getInfo();
        NetworkAdapterStats stats = this.context.getSystemInformation().getNetworkAdapterStats(info.getName());

        for (MeasurementScheduleRequest request : metrics) {
            String property = request.getName();

            if (property.startsWith("Trait.net4.address")) // TODO
            { // this trait is supported regardless of having native support or not
                List<InetAddress> addrs = info.getUnicastAddresses();
                String ifAddrs = (addrs.size() > 0) ? addrs.get(0).getHostAddress() : "";
                for (int i = 1; i < addrs.size(); i++) {
                    ifAddrs += "," + addrs.get(i).getHostAddress();
                }

                report.addData(new MeasurementDataTrait(request, ifAddrs));
            } else if (property.equals("Trait.interfaceFlags")) {
                report.addData(new MeasurementDataTrait(request, info.getAllFlags()));
            } else {
                Number number = ((Number) ObjectUtil.lookupAttributeProperty(stats, request.getName()));
                report.addData(new MeasurementDataNumeric(request, number.doubleValue()));
            }
        }

        return;
    }
}