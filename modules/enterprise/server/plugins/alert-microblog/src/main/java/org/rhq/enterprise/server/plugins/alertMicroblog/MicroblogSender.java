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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Properties;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.PropertyConfiguration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.notification.ResultState;
import org.rhq.core.domain.alert.notification.SenderResult;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.plugin.pc.ControlFacet;
import org.rhq.enterprise.server.plugin.pc.ControlResults;
import org.rhq.enterprise.server.plugin.pc.ServerPluginContext;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSender;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Send alert notifications via Microblogging services like Twitter or laconi.ca
 * @author Heiko W. Rupp
 */
public class MicroblogSender extends AlertSender {

    static final String CONS_KEY = "iXCqk1vR2vKksDHkulZQ";
    static final String CONS_SECRET = "d2iwloVgHSghDfEmPWzjxAKdtp18TEvcBJsyaqBjst0";
    private final Log log = LogFactory.getLog(MicroblogSender.class);

    @Override
    public SenderResult send(Alert alert) {

        SenderResult result;
        String consumerKey = preferences.getSimpleValue("consumerKey", CONS_KEY);
        String consumerSecret = preferences.getSimpleValue("consumerSecret",
                CONS_SECRET);
        String accessTokenFilePath = preferences.getSimpleValue("accessTokenFilePath", "/path/to/token.ser");


        try {
            TwitterFactory tFactory = new TwitterFactory();
            AccessToken accessToken = restoreAccessToken(accessTokenFilePath);

            log.debug("loading accessToken from " + accessTokenFilePath);
            log.debug("token: [" + accessToken.getToken() + "]");
            log.debug("tokenSecret: [" + accessToken.getTokenSecret() + "]");

            Twitter twitter = tFactory.getInstance();
            twitter.setOAuthConsumer(consumerKey, consumerSecret);
            twitter.setOAuthAccessToken(accessToken);

            AlertManagerLocal alertManager = LookupUtil.getAlertManager();
            StringBuilder b = new StringBuilder("Alert ");
            b.append(alert.getId()).append(":'"); // Alert id
            b.append(alert.getAlertDefinition().getResource().getName());
            b.append("' (");
            b.append(alert.getAlertDefinition().getResource().getId());
            b.append("): ");
            b.append(alertManager.prettyPrintAlertConditions(alert, true));
            b.append("-by " + this.alertParameters.getSimpleValue("twittedBy", "@RHQ")); // TODO not for production :-)
            // TODO use some alert url shortening service

            String msg = b.toString();
            if (msg.length() > 140)
                msg = msg.substring(0, 140);

            Status status = twitter.updateStatus(msg);

            result = SenderResult.getSimpleSuccess("Send notification - msg-id: " + status.getId());
        } catch (TwitterException e) {

            log.warn("Notification via Microblog failed!", e);
            result = SenderResult.getSimpleFailure("Sending failed :" + e.getMessage());

        } catch (IOException e) {
            log.error("Notification via Microblog failed!", e);
            result = SenderResult.getSimpleFailure("Sending failed :" + e.getMessage());
        }
        return result;
    }

    private AccessToken restoreAccessToken(String tokenFilePath) throws IOException {
        //use buffering
        InputStream file = new FileInputStream(tokenFilePath);
        InputStream buffer = new BufferedInputStream(file);
        ObjectInput input = new ObjectInputStream(buffer);

        AccessToken token = null;

        try {
            token = (AccessToken) input.readObject();
        } catch (ClassNotFoundException e) {
            log.error("Erro reading token from disk: ", e);
        } finally {
            input.close();
        }

        return token;
    }

}
