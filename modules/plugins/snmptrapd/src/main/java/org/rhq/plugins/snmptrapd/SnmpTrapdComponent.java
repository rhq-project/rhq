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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.snmp4j.Snmp;
import org.snmp4j.log.Log4jLogFactory;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.event.EventContext;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.util.exception.ThrowableUtil;

/**
 * The actual implementation of the Snmp trapd
 * @author Heiko W. Rupp
 *
 */
public class SnmpTrapdComponent implements ResourceComponent, MeasurementFacet {

    private final Log log = LogFactory.getLog(SnmpTrapdComponent.class);

    public static final String TRAP_TYPE = "SnmpTrap";

    private EventContext eventContext;
    private Snmp snmp;
    private SnmpTrapEventPoller snmpTrapEventPoller;
    public static int trapCount = 0;
    static {
        org.snmp4j.log.LogFactory.setLogFactory(new Log4jLogFactory());
    }

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

        Configuration conf = context.getPluginConfiguration();
        PropertySimple ps = conf.getSimple("port");
        Integer port = ps.getIntegerValue();
        ps = conf.getSimple("community");
        String community = ps.getStringValue();
        ps = conf.getSimple("eventSeverityOid");
        String severityOid = ps.getStringValue();
        ps = conf.getSimple("pollInterval");
        int pollInterval = ps.getIntegerValue();

        eventContext = context.getEventContext();

        // TODO: check if the engine is already alive
        try {
            UdpAddress targetAddress = new UdpAddress(port);
            // TransportMapping transport = new DefaultUdpTransportMapping(targetAddress);
            snmp = new Snmp(new DefaultUdpTransportMapping());
            snmpTrapEventPoller = new SnmpTrapEventPoller(severityOid);
            eventContext.registerEventPoller(snmpTrapEventPoller, pollInterval);
            // TODO set up the community here
            if (!snmp.addNotificationListener(targetAddress, snmpTrapEventPoller))
                throw new IOException("cannot attach to " + targetAddress);
            //transport.listen();
        } catch (IOException e) {
            log.error("Cannot start snmp engine. Cause: " + ThrowableUtil.getAllMessages(e));
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
                log.error("Cannot stop snmp engine. Cause: " + ThrowableUtil.getAllMessages(e));
            }
            snmp = null;
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
