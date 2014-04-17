/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.server.plugins.alertUps;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.aerogear.unifiedpush.SenderClient;
import org.jboss.aerogear.unifiedpush.message.MessageResponseCallback;
import org.jboss.aerogear.unifiedpush.message.UnifiedMessage;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.notification.SenderResult;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSender;

/**
 * Sends an alert notification via IRC.
 *
 * @author Justin Harris
 */
@SuppressWarnings("unused")
public class UpsSender extends AlertSender<UpsAlertComponent> {

    private final Log log = LogFactory.getLog(UpsSender.class);

    public UpsSender() {
        System.setProperty("jsse.enableSNIExtension", "false");
    }

    @Override
    public SenderResult send(Alert alert) {
        String channel = this.alertParameters.getSimpleValue("channel", null);
        String server = preferences.getSimpleValue("server", "-not set-");

        StringBuilder b = new StringBuilder();
         b.append("Alert on");
         b.append(alert.getAlertDefinition().getResource().getName());
         b.append(":");
         b.append(alert.getConditionLogs().iterator().next().getCondition().toString());

         b.append("Brought by RHQ");


        UnifiedMessage unifiedMessage = new UnifiedMessage.Builder()
                        .pushApplicationId("b0a1b452-b749-4dc3-b789-bf2cefd28398")
                        .masterSecret("45b65fc9-2947-4735-ad73-b18e4518b406")
                        .alert(b.toString()) // TODO nicer max 160 chars
                        .sound("default") // iOS specific
                        .build();


        SenderClient sender = new SenderClient("https://" +  pluginComponent.targetHost);

        final SenderResult[] result = new SenderResult[1];
        sender.send(unifiedMessage, new MessageResponseCallback() {

        			@Override
        			public void onError(Throwable throwable) {

        				result[0] = SenderResult.getSimpleFailure(throwable.getMessage());

        			}

        			@Override
        			public void onComplete(int statusCode) {
        				//
                        result[0] = SenderResult.getSimpleSuccess("Yieha! " + statusCode);

        			}
        		});

        return result[0];
    }

}
