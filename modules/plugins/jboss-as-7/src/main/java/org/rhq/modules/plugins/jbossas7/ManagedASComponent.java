/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.modules.plugins.jbossas7;


import java.util.Date;
import java.util.Set;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Common stuff for the Domain
 * @author Heiko W. Rupp
 */
@SuppressWarnings("unused")
public class ManagedASComponent extends BaseComponent {

    /**
     * Get the availability of the managed AS server. We can't just check if
     * a connection succeeds, as the check runs against the API/HostController
     * and the managed server may still be down even if the connection succeeds.
     * @return Availability of the managed AS instance.
     */
    @Override
    public AvailabilityType getAvailability() {

        if (context.getResourceType().getName().equals("Managed Server")) {
            Address theAddress = new Address();
            String host = pluginConfiguration.getSimpleValue("domainHost","local");
            theAddress.add("host",host);
            theAddress.add("server-config", myServerName);
            Operation getStatus = new ReadAttribute(theAddress,"status");
            Result result;
            try {
                result = connection.execute(getStatus);
            } catch (Exception e) {
                log.warn(e.getMessage());
                return AvailabilityType.DOWN;
            }
            if (!result.isSuccess())
                return AvailabilityType.DOWN;

            String msg = result.getResult().toString();
            if (msg.contains("STARTED"))
                return AvailabilityType.UP;
            else
                return AvailabilityType.DOWN;
        }

        return super.getAvailability();
    }


    public void getValues(MeasurementReport report, Set metrics) throws Exception {

        Set<MeasurementScheduleRequest> requests = metrics;

        for (MeasurementScheduleRequest request: requests) {
            if (request.getName().equals("startTime")) {
                String path = getPath();
                path = path.replace("server-config","server");
                Address address = new Address(path);
                address.add("core-service","platform-mbean");
                address.add("type","runtime");
                Operation op = new ReadAttribute(address,"start-time");
                Result res = getASConnection().execute(op);

                if (res.isSuccess()) {
                    Long startTime= (Long) res.getResult();
                    MeasurementDataTrait data = new MeasurementDataTrait(request,new Date(startTime).toString());
                    report.addData(data);
                }

            }
        }

        super.getValues(report, metrics);
    }

}
