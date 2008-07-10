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

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.PDU;
import org.snmp4j.PDUv1;
import org.snmp4j.ScopedPDU;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;

import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.pluginapi.event.EventPoller;

/**
 * Polls the individual traps
 * @author Heiko W. Rupp
 *
 */
public class SnmpTrapEventPoller implements EventPoller, CommandResponder {

    List<Event> events = new Vector<Event>();
    OID severityOid;
    Properties translation;

    public SnmpTrapEventPoller() {
        severityOid = null;
        //        InputStream in = ClassLoader.getSystemResourceAsStream("MibTrans.properties");
        translation = new Properties();
        //        try {
        //            if (in != null)
        //                translation.load(in);
        //        } catch (IOException e) {
        //            // TODO Auto-generated catch block
        //            e.printStackTrace();
        //        }
        // TODO move the next to an external file (or better to a mib parser)
        translation.put("1.3.6.1.4.1.18016.2.1.1", "rhq.alert.alertName");
        translation.put("1.3.6.1.4.1.18016.2.1.2", "rhq.alert.resourceName");
        translation.put("1.3.6.1.4.1.18016.2.1.3", "rhq.alert.platformName");
        translation.put("1.3.6.1.4.1.18016.2.1.4", "rhq.alert.condition");
        translation.put("1.3.6.1.4.1.18016.2.1.5", "rhq.alert.severity");
    }

    public SnmpTrapEventPoller(String severityOidString) {
        this();
        if (severityOidString != null)
            severityOid = new OID(severityOidString);
    }

    /* (non-Javadoc)
     * @see org.rhq.core.pluginapi.event.EventPoller#getEventType()
     */
    public String getEventType() {
        return SnmpTrapdComponent.TRAP_TYPE;
    }

    /* (non-Javadoc)
     * @see org.rhq.core.pluginapi.event.EventPoller#poll()
     */
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
    public void processPdu(CommandResponderEvent arg0) {

        String sourceAddr = "snmp-agent";
        PDU pdu = arg0.getPDU();
        if (pdu != null) {
            //            System.out.println("=>PDU: " + pdu);
            // SNMP v1
            if (pdu instanceof PDUv1) {
                PDUv1 v1pdu = (PDUv1) pdu;
                sourceAddr = v1pdu.getAgentAddress().toString();
            }
            // SNMP v3
            else if (pdu instanceof ScopedPDU) {
                ScopedPDU spdu = (ScopedPDU) pdu;
            }

            SnmpTrapdComponent.trapCount++;
            EventSeverity severity = EventSeverity.INFO;
            StringBuffer payload = new StringBuffer();
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
                payload.append(" == ");
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

            Event event = new Event(getEventType(), sourceAddr, new Date(), severity, payload.toString());

            synchronized (events) {
                events.add(event);
            }

        }
    }
}
