/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.enterprise.server.plugins.alertSnmp;

import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.notification.ResultState;
import org.rhq.core.domain.alert.notification.SenderResult;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.plugins.alertSnmp.SnmpTrapSender;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSender;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * AlertSender that sends alerts via SNMP Traps
 * @author Heiko W. Rupp
 */
public class SnmpSender extends AlertSender {

    private final Log log = LogFactory.getLog(SnmpSender.class);

    @Override
    public SenderResult send(Alert alert) {

        Date bootTime = null;


        String oid = alertParameters.getSimpleValue("OID",null);
        if (oid==null) {
            return new SenderResult(ResultState.FAILURE,"no OID given");
        }
        String host = alertParameters.getSimpleValue("host",null);
        if (host==null) {
            return new SenderResult(ResultState.FAILURE,"no host given");
        }
        String portS = alertParameters.getSimpleValue("port","162");
        Integer port = Integer.valueOf(portS);

        AlertManagerLocal alertManager = LookupUtil.getAlertManager();
        ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

        SnmpTrapSender snmpTrapSender = new SnmpTrapSender(preferences);
        log.debug("Sending SNMP trap with OID " + oid + " to SNMP engine "
            + host + ":" + port + "...");
        String result;
        List<Resource> lineage = resourceManager.getResourceLineage(alert.getAlertDefinition().getResource().getId());
        String platformName = lineage.get(0).getName();
        String conditions = alertManager.prettyPrintAlertConditions(alert, false);
        String alertUrl = alertManager.prettyPrintAlertURL(alert);

        SenderResult res ;
        try {
            bootTime = new Date(); // TODO = LookupUtil.getCoreServer().getBootTime();
            result = snmpTrapSender.sendSnmpTrap(alert, alertParameters, platformName, conditions, bootTime, alertUrl);
            res = new SenderResult(ResultState.SUCCESS,result);
        } catch (Throwable t) {
            result = "failed - cause: " + t;
            res = new SenderResult(ResultState.FAILURE,result);
        }

        log.debug("Result of sending SNMP trap: " + result);

        return res;
    }
}
