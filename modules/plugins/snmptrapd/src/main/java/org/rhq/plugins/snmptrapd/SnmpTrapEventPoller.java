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
import java.util.Set;
import java.util.Vector;

import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.PDU;
import org.snmp4j.PDUv1;

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

    public SnmpTrapEventPoller() {

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
            if (pdu instanceof PDUv1) {
                PDUv1 v1pdu = (PDUv1) pdu;
                sourceAddr = v1pdu.getAgentAddress().toString();
            }

            SnmpTrapdComponent.trapCount++;
            Event event = new Event(getEventType(), sourceAddr, new Date(), EventSeverity.INFO, pdu
                .getErrorStatusText());

            synchronized (events) {
                events.add(event);
            }

        }
    }

}
