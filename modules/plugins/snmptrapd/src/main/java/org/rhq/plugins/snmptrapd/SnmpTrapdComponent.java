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

package org.rhq.plugins.snmptrapd;

import java.io.IOException;
import java.util.Set;

import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.event.EventContext;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

/**
 * The actual implementation of the Snmp trapd
 * @author Heiko W. Rupp
 *
 */
public class SnmpTrapdComponent implements ResourceComponent, MeasurementFacet {

    public static final String TRAP_TYPE = "SnmpTrap";

    private EventContext eventContext;
    private Snmp snmp;
    private SnmpTrapEventPoller snmpTrapEventPoller;
    public static int trapCount = 0;

    /* (non-Javadoc)
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#getAvailability()
     */
    public AvailabilityType getAvailability() {
        return AvailabilityType.UP;
    }

    /** 
     * Start the event polling mechanism and the actual trap listener.
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#start(org.rhq.core.pluginapi.inventory.ResourceContext)
     */
    public void start(ResourceContext context) throws InvalidPluginConfigurationException, Exception {
        // TODO Auto-generated method stub
        // TODO start teh even poller
        eventContext = context.getEventContext();
        snmpTrapEventPoller = new SnmpTrapEventPoller();
        eventContext.registerEventPoller(snmpTrapEventPoller, 60);

        try {
            UdpAddress targetAddress = new UdpAddress(1162);
            TransportMapping transport = new DefaultUdpTransportMapping(targetAddress);
            Snmp snmp = new Snmp(transport);
            snmp.addCommandResponder(snmpTrapEventPoller);
            transport.listen();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * Tear down the trap listener and stop polling for events.
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#stop()
     */
    public void stop() {
        if (snmp != null) {
            snmp.removeCommandResponder(snmpTrapEventPoller);
            try {
                snmp.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        eventContext.unregisterEventPoller(TRAP_TYPE);
    }

    /* (non-Javadoc)
     * @see org.rhq.core.pluginapi.measurement.MeasurementFacet#getValues(org.rhq.core.domain.measurement.MeasurementReport, java.util.Set)
     */
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {

        for (MeasurementScheduleRequest req : metrics) {
            if (req.getName().equals("trap_count")) {
                MeasurementDataNumeric res = new MeasurementDataNumeric(req, Double.valueOf(trapCount)); // TODO FIXME
                report.addData(res);
            }
        }
    }

}
