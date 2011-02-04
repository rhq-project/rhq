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
package org.rhq.enterprise.server.plugins.alertlog4j;


import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.notification.SenderResult;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSender;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * AlertSender that sends alerts to Log4J
 * @author Steve Millidge (C2B2 Consulting Limited)
 */
public class Log4JSender extends AlertSender {

    private final Log log = LogFactory.getLog(Log4JSender.class);

    @Override
    public SenderResult send(Alert alert) {
        String category = alertParameters.getSimpleValue("log4JCategory", this.getClass().getName());
        Logger logger = Logger.getLogger(category);
        AlertManagerLocal alertManager = LookupUtil.getAlertManager();
        ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

        List lineage = resourceManager.getResourceLineage(alert.getAlertDefinition().getResource().getId());
        String platformName = ((Resource)lineage.get(0)).getName();
        String conditions = alertManager.prettyPrintAlertConditions(alert, false).replace('\n', ' ');
        String alertURL = alertManager.prettyPrintAlertURL(alert);
        StringBuilder message = new StringBuilder();
        message.append("ALERT,");
        message.append(alert.getAlertDefinition().getPriority().getName());
        message.append(',');
        message.append(alert.getAlertDefinition().getName());
        message.append(',');
        message.append(alert.getAlertDefinition().getDescription());
        message.append(',');
        message.append(alert.getAlertDefinition().getResource().getName()).append(',');
        message.append(platformName).append(',');
        message.append(conditions).append(',');
        message.append(alertURL);

        switch (alert.getAlertDefinition().getPriority()){
            case HIGH:
                logger.error(message);
                break;
            case MEDIUM:
                logger.warn(message);
                break;
            case LOW:
                logger.info(message);
                break;
            default: //do nothing
        }
        return SenderResult.getSimpleSuccess("Alert Sent to Log4J Category " + category);

    }

}
