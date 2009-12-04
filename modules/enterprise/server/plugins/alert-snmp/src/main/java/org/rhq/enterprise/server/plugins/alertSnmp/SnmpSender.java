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
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.plugin.pc.alert.ResultState;
import org.rhq.enterprise.server.plugins.alertSnmp.SnmpTrapSender;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSender;
import org.rhq.enterprise.server.plugin.pc.alert.SenderResult;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * // TODO: Document this
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
        String portS = alertParameters.getSimpleValue("port","161");
        Integer port = Integer.valueOf(portS);

        AlertManagerLocal alertManager = LookupUtil.getAlertManager();
        ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

        SnmpTrapSender snmpTrapSender = new SnmpTrapSender();
        log.debug("Sending SNMP trap with OID " + oid + " to SNMP engine "
            + host + ":" + port + "...");
        String result;
        List<Resource> lineage = resourceManager.getResourceLineage(alert.getAlertDefinition().getResource().getId());
        String platformName = lineage.get(0).getName();
        String conditions = alertManager.prettyPrintAlertConditions(alert);
        String alertUrl = alertManager.prettyPrintAlertURL(alert);
        try {
            bootTime = new Date(); // TODO = LookupUtil.getCoreServer().getBootTime();
            result = snmpTrapSender.sendSnmpTrap(alert, alertParameters, platformName, conditions, bootTime, alertUrl);
        } catch (Throwable t) {
            result = "failed - cause: " + t;
        }

        log.debug("Result of sending SNMP trap: " + result);
        // TODO: Log the action result to the DB (i.e. as an AlertNotificationLog).
        //       (see http://jira.jboss.com/jira/browse/JBNADM-1820)

        return null;  // TODO: Customise this generated block
    }
}
