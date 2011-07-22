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
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.pluginapi.event.EventPoller;
import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.PDU;
import org.snmp4j.PDUv1;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;

/**
 * Polls the individual traps
 * @author Heiko W. Rupp
 *
 */
public class SnmpTrapEventPoller implements EventPoller, CommandResponder {

    private final List<Event> events = new Vector<Event>();
    private OID severityOid;
    private Properties translation;
    private Log log = LogFactory.getLog(SnmpTrapEventPoller.class);

    public SnmpTrapEventPoller() {
        severityOid = null;

        // Load properties, that translate from oid strings (1.2.2...) to their name in the mib
        // Should be done through a mib parser
        ClassLoader cl = getClass().getClassLoader();
        InputStream in = cl.getResourceAsStream("MibTrans.properties");
        translation = new Properties();
        try {
            if (in != null) {
                translation.load(in);
                in.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public SnmpTrapEventPoller(String severityOidString) {
        this();
        if (severityOidString != null)
            severityOid = new OID(severityOidString);
    }

    public String getEventType() {
        return SnmpTrapdComponent.TRAP_TYPE;
    }

    public Set<Event> poll() {
        Set<Event> eventSet = new HashSet<Event>();
        synchronized (events) {
            eventSet.addAll(events);
            events.clear();
        }
        return eventSet;
    }

    /**
     * Callback from the Snmp lib. Will be called on each incoming trap
     */
    public void processPdu(CommandResponderEvent cre) {

        if (log.isDebugEnabled())
            log.debug("recv: " + cre);
        PDU pdu = cre.getPDU();
        String sourceAddr;
        Address addr = cre.getPeerAddress();
        if (addr instanceof IpAddress) {
            sourceAddr = ((IpAddress) addr).getInetAddress().toString();
            if (sourceAddr.startsWith("/"))
                sourceAddr = sourceAddr.substring(1);
        } else {
            // Don't use addr.toString() as this would contain the port and generate too many
            // EventSources
            sourceAddr = "snmp-agent";
        }
        if (pdu != null) {
            StringBuffer payload = new StringBuffer();
            // SNMP v1
            if (pdu instanceof PDUv1) {
                PDUv1 v1pdu = (PDUv1) pdu;
                long timeTicks = v1pdu.getTimestamp();
                payload.append("Traptype (generic, specific): ");
                payload.append(v1pdu.getGenericTrap()).append(", ").append(v1pdu.getSpecificTrap()).append("\n");
                payload.append("Timestamp: " + new TimeTicks(timeTicks).toString());
                payload.append("\n");
            }

            SnmpTrapdComponent.trapCount++;
            EventSeverity severity = EventSeverity.INFO;

            Vector<VariableBinding> vbs = pdu.getVariableBindings();
            for (VariableBinding vb : vbs) {
                OID oid = vb.getOid();
                Variable var = vb.getVariable();
                int syntax = vb.getSyntax();

                // Try to translate the oid string (1.2.3....) into a readable name
                String oids = oid.toString();
                if (translation.getProperty(oids) != null) {
                    oids = translation.getProperty(oids);
                }

                payload.append(oids);
                payload.append(": ");
                payload.append(var.toString()); // TODO change depending on syntax !
                payload.append("\n");

                /*
                 * This corresponds with the values from AlertPriority
                 */
                if (severityOid != null && oid.compareTo(severityOid) == 0) {
                    String sev = var.toString();
                    if (sev.toLowerCase().contains("high"))
                        severity = EventSeverity.ERROR;
                    else if (sev.toLowerCase().contains("medium"))
                        severity = EventSeverity.WARN;
                    else
                        severity = EventSeverity.INFO;
                }
            }

            Event event = new Event(getEventType(), sourceAddr, System.currentTimeMillis(), severity, payload
                .toString());
            if (log.isDebugEnabled())
                log.debug("queue event " + event);

            synchronized (events) {
                events.add(event);
            }

        }
    }
}
