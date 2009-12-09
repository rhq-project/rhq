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
package org.rhq.enterprise.server.plugins.alertIrc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.alert.Alert;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSender;
import org.rhq.enterprise.server.plugin.pc.alert.ResultState;
import org.rhq.enterprise.server.plugin.pc.alert.SenderResult;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Sends an alert notification via IRC.
 *
 * @author Justin Harris
 */
public class IrcSender extends AlertSender<IrcAlertComponent> {

    private final Log log = LogFactory.getLog(IrcSender.class);

    @Override
    public SenderResult send(Alert alert) {
        SenderResult result;
        String channel = this.alertParameters.getSimpleValue("channel", null);

        try {
            this.pluginComponent.sendIrcMessage(channel, getIrcMessage(alert));
            result = new SenderResult(ResultState.SUCCESS, "IRC Alert sent.");
        } catch (IllegalStateException e) {
            log.error(e.getMessage(), e);
            result = new SenderResult(ResultState.FAILURE, "IRC Alert failed!");
        }

        return result;
    }

    private String getIrcMessage(Alert alert) {
        AlertManagerLocal alertManager = LookupUtil.getAlertManager();

        StringBuilder b = new StringBuilder("Alert -- ");
        b.append(alert.getAlertDefinition().getName());
        b.append(" (on ");
        b.append(alert.getAlertDefinition().getResource().getName());
        b.append("):  ");
        b.append(alertManager.prettyPrintAlertURL(alert));

        return b.toString();
    }
}
