/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.enterprise.server.plugins.alertSnmp;

import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.notification.SenderResult;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSender;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * AlertSender that sends alerts via SNMP Traps
 * @author Heiko W. Rupp
 */
public class SnmpSender extends AlertSender {

    private static final Log LOG = LogFactory.getLog(SnmpSender.class);

    private ResourceManagerLocal resourceManager;

    private AlertManagerLocal alertManager;

    /**
     * Default constructor needed for instanciation by server plugin container
     */
    public SnmpSender() {
        this(LookupUtil.getResourceManager(), LookupUtil.getAlertManager());
    }

    public SnmpSender(ResourceManagerLocal resourceManager, AlertManagerLocal alertManager) {
        this.resourceManager = resourceManager;
        this.alertManager = alertManager;
    }

    @Override
    public SenderResult send(Alert alert) {

        SnmpInfo info = SnmpInfo.load(alertParameters);
        if (info.error != null) {
            return SenderResult.getSimpleFailure(info.error);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Sending SNMP trap to: " + info);
        }

        try {
            SnmpTrapSender snmpTrapSender = new SnmpTrapSender(preferences);

            List<Resource> lineage = resourceManager.getResourceLineage(alert.getAlertDefinition().getResource()
                .getId());
            String platformName = lineage.get(0).getName();
            String conditions = alertManager.prettyPrintAlertConditions(alert, false);
            String alertUrl = alertManager.prettyPrintAlertURL(alert);
            
        	
        	String hierarchy = getResourceHierarchyAsString(lineage);

            Date bootTime = new Date(); // TODO: want to use LookupUtil.getCoreServer().getBootTime() but ServiceMBean is not visible
            String result = snmpTrapSender.sendSnmpTrap(alert, alertParameters, platformName, conditions, bootTime,
                alertUrl, hierarchy);
            return SenderResult.getSimpleSuccess(result);
        } catch (Throwable t) {
            LOG.error("Could not send SNMP trap to " + info, t);
            return SenderResult.getSimpleFailure("failed - cause: " + t);
        }
    }

	private String getResourceHierarchyAsString(List<Resource> lineage) {
		StringBuilder stringBuilder = new StringBuilder();
		for (Resource resource : lineage) {
			stringBuilder.append(resource.getName());
			if (lineage.indexOf(resource) != lineage.size() - 1) {
				stringBuilder.append("::");
			}
		}
		return stringBuilder.toString();
	}

    @Override
    public String previewConfiguration() {
        SnmpInfo info = SnmpInfo.load(alertParameters);
        return info.toString();
    }
}
