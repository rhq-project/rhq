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
package org.rhq.enterprise.server.plugins.alertMobicents;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.notification.SenderResult;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSender;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * AlertSender that notifies users via Mobicents (voice, sms)
 * @author Heiko W. Rupp
 */
public class MobicentsSender extends AlertSender {

    private final Log log = LogFactory.getLog(MobicentsSender.class);

    @Override
    public SenderResult send(Alert alert) {

        String baseUrl = preferences.getSimpleValue("mobicentsServerUrl", "http://localhost:8080/mobicents");
        String tel = alertParameters.getSimpleValue("targetAddress", null);
        if (tel == null) {
            log.warn("No number to call given, not sending");
            return SenderResult.getSimpleFailure("No target address given");
        }
        String kindString = alertParameters.getSimpleValue("kind", "VOICE");
        MobiKind kind = MobiKind.valueOf(kindString);

        Integer alertId = alert.getId();
        StringBuilder b = new StringBuilder("alertText=");
        Resource res = alert.getAlertDefinition().getResource();
        AlertManagerLocal alertManager = LookupUtil.getAlertManager();

        switch (kind) {
        case VOICE:
            b.append("Alert on resource ");
            do {
                b.append(res.getName());
                res = res.getParentResource();
                if (res != null)
                    b.append(" with parent ");
            } while (res != null);
            b.append(". Cause is ");

            // Switch locale to english, as the voice synthesizer expects this for now
            Locale currentLocale = Locale.getDefault();
            Locale.setDefault(Locale.ENGLISH);
            b.append(alertManager.prettyPrintAlertConditions(alert, false));
            Locale.setDefault(currentLocale);

            boolean willBeDisabled = alertManager.willDefinitionBeDisabled(alert);

            if (willBeDisabled)
                b.append(" The alert definition will now be disabled. \n\n");

            //            b.append(" Please press ");
            //
            //            if (willBeDisabled) {
            //                b.append(AlertFeedback.REENABLE.getText());
            //            } else {
            //                b.append(AlertFeedback.DISABLE.getText());
            //            }
            //            b.append(", ");
            //            b.append(AlertFeedback.DELETE.getText());
            //            b.append(" or just hang up to do nothing.");
            break;
        case SMS:
            b.append("Alert: ");
            b.append(res.getName());
            b.append(",id=(").append(res.getId()).append(")");
            b.append("Brought by RHQ");
            break;
        default:
            log.warn("Unsupported Mobicents notification type for now");
        }

        URL url;
        int code = 0;
        HttpURLConnection conn = null;
        try {
            tel = tel.trim();
            String telEnc = URLEncoder.encode(tel, "UTF-8"); // url encode tel-no, as it can contain '+'
            switch (kind) {
            case SMS:
                baseUrl = baseUrl + "sms"; // No trailing '/' !
                break;
            case VOICE:
                baseUrl = baseUrl + "call"; // No trailing '/' !
                break;
            default:
                baseUrl = baseUrl + "--not-supported-yet--";
            }
            baseUrl = baseUrl + "?alertId=" + alertId;
            baseUrl = baseUrl + "&tel=";
            if (kind == MobiKind.VOICE) {
                if (!tel.startsWith("sip:")) {
                    baseUrl = baseUrl + "sip:";
                }
            }
            baseUrl = baseUrl + telEnc;
            if (kind == MobiKind.VOICE) {
                if (!tel.contains("@")) { // Append domain from preferences if user has none provided
                    String domain = preferences.getSimpleValue("defaultVoipDomain", "localhost");
                    baseUrl = baseUrl + "@" + domain;
                }
            }
            // TODO SMS url

            log.info("Mobicents alert [" + kind + "] to baseUrl [" + baseUrl + "] with message:\n" + b.toString());
            url = new URL(baseUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            OutputStream out = conn.getOutputStream();

            //conn.addRequestProperty("text",b.toString());
            // TODO encode?
            out.write(b.toString().getBytes());
            out.flush();
            conn.connect();
            code = conn.getResponseCode();
        } catch (Exception e) {
            log.warn("Notification via VoIP failed: " + e);
            return SenderResult.getSimpleFailure("Sending failed " + e.getMessage());
        } finally {
            if (conn != null)
                conn.disconnect();
        }

        if (code != 200) {
            log.info("Notification via Mobicents returned code " + code);
            return SenderResult.getSimpleFailure("Notification via Mobicents returned code " + code);
        } else {
            return SenderResult.getSimpleSuccess("Mobicents alert [" + kind + "] to baseUrl [" + baseUrl + "]");
        }
    }
}
