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
package org.rhq.enterprise.server.plugins.alertMicroblog;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.notification.SenderResult;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSender;
import org.rhq.enterprise.server.util.LookupUtil;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

/**
 * Send alert notifications via Microblogging services like Twitter or laconi.ca
 * @author Heiko W. Rupp
 */
public class MicroblogSender extends AlertSender {

    private final Log log = LogFactory.getLog(MicroblogSender.class);

    @Override
    public SenderResult send(Alert alert) {

        String user = preferences.getSimpleValue("user", null);
        String password = preferences.getSimpleValue("password", null);
        String baseUrl = preferences.getSimpleValue("microblogServerUrl", "http://twitter.com/");
        if (!baseUrl.endsWith("/"))
            baseUrl = baseUrl + "/";
        Twitter twitter = new Twitter(user, password, baseUrl);
        AlertManagerLocal alertManager = LookupUtil.getAlertManager();
        twitter.setSource("Jopr");
        StringBuilder b = new StringBuilder("Alert ");
        b.append(alert.getId()).append(":'"); // Alert id
        b.append(alert.getAlertDefinition().getResource().getName());
        b.append("' (");
        b.append(alert.getAlertDefinition().getResource().getId());
        b.append("): ");
        b.append(alertManager.prettyPrintAlertConditions(alert, true));
        b.append("-by @JBossJopr"); // TODO not for production :-)
        // TODO use some alert url shortening service

        String txt = "user@baseUrl [" + user + "@" + baseUrl + "]:";
        try {
            String msg = b.toString();
            if (msg.length() > 140)
                msg = msg.substring(0, 140);

            Status status = twitter.updateStatus(msg);

            return SenderResult.getSimpleSuccess("Send notification to " + txt + ", msg-id: " + status.getId());
        } catch (TwitterException e) {

            log.warn("Notification via Microblog failed for " + txt + " ", e);
            return SenderResult.getSimpleFailure("Sending failed :" + e.getMessage());

        }
    }
}
