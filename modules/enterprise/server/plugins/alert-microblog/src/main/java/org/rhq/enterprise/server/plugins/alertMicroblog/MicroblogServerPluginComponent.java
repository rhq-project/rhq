/*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.plugins.alertMicroblog;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.enterprise.server.plugin.pc.ControlFacet;
import org.rhq.enterprise.server.plugin.pc.ControlResults;
import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent;
import org.rhq.enterprise.server.plugin.pc.ServerPluginContext;
import org.rhq.enterprise.server.plugin.pc.alert.CustomAlertSenderBackingBean;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

/**
 * Microblog Server Plugin Component
 *
 * @author Rafael Soares
 *
 */
public class MicroblogServerPluginComponent extends CustomAlertSenderBackingBean implements ServerPluginComponent,
    ControlFacet {

    private Twitter twitter;
    private RequestToken requestToken;

    private final Log log = LogFactory.getLog(MicroblogServerPluginComponent.class);

    private ServerPluginContext context;

    public void initialize(ServerPluginContext context) throws Exception {
        this.context = context;

        String consumerKey = this.context.getPluginConfiguration().getSimpleValue("consumerKey", MicroblogSender.CONS_KEY);
        String consumerSecret = this.context.getPluginConfiguration().getSimpleValue("consumerSecret", MicroblogSender.CONS_SECRET);

        if (consumerKey == null || consumerSecret == null)
            throw new TwitterException(
                "consumerKey or consumerSecret missing. Please configure the Microblog plugin before.");

        // The factory instance is re-useable and thread safe.
        this.twitter = new TwitterFactory().getInstance();
        this.twitter.setOAuthConsumer(consumerKey, consumerSecret);

        log.debug("Twitter using consumerKey [" + consumerKey + "] and consumerSecret: [" + consumerSecret + "]");
    }

    private String getAuthorizationURL() throws TwitterException {
        RequestToken requestToken = twitter.getOAuthRequestToken();

        log.info("Open the following URL and grant access to your account: " + requestToken.getAuthorizationURL());
        return requestToken.getAuthorizationURL();
    }

    private String storeAccessToken(AccessToken token) throws IOException {
        //use buffering
        String filePath = null;
        if (this.context.getDataDirectory().exists() || this.context.getDataDirectory().mkdir()) {

            filePath = this.context.getDataDirectory().getAbsolutePath() + "/OAuthAccessToken_" + token.getUserId() + ".ser";

            // merge the PLugin Configuration to store the token file path reference.
            // this property will be user by Microblog AlertSender to load the accessToken from file system
            this.context.getPluginConfiguration().put(new PropertySimple("accessTokenFilePath", filePath));
            this.persistConfiguration(this.context.getPluginConfiguration());

            OutputStream file = new FileOutputStream(filePath);
            OutputStream buffer = new BufferedOutputStream(file);
            ObjectOutput output = new ObjectOutputStream(buffer);

            try {
                output.writeObject(token);
            } finally {
                output.close();

                log.info("AccessToken saved at " + filePath);
            }
        } else
            throw new IOException("AccessToken not stored!");

        return filePath;
    }

    @Override
    public ControlResults invoke(String controlOperation, Configuration operationConfig) {

        ControlResults ctrlResult = new ControlResults();

        try {
            if (controlOperation.equals("GET_OAUTH_REQUEST_URL")) {
                // get and store AuthURL in plugin config to be rederend on UI
                ctrlResult.getComplexResults().put(new PropertySimple("authorizationURL", getAuthorizationURL()));
            } else if (controlOperation.equals("GET_ACCESS_TOKEN")) {

                AccessToken accessToken = null;
                String pin = operationConfig.getSimpleValue("pin", null);

                log.debug("using PIN [" + pin + "]");

                if (pin != null && pin.length() > 0) {
                    accessToken = this.twitter.getOAuthAccessToken(requestToken, pin);
                } else {
                    accessToken = this.twitter.getOAuthAccessToken();
                }

                log.debug("ScreenName: " + twitter.getScreenName());
                log.debug("TwitterId: [" + twitter.verifyCredentials().getId() + "]");
                log.debug("token: [" + accessToken.getToken() + "]");
                log.debug("tokenSecret: [" + accessToken.getTokenSecret() + "]");

                // Save the accessToken for future use by this plugin.
                String filePath = storeAccessToken(accessToken);

                ctrlResult.getComplexResults().put(
                    new PropertySimple("accessToken", "token[" + accessToken.getToken() + "] tokenSecret["
                        + accessToken.getTokenSecret() + ""));
                ctrlResult.getComplexResults().put(new PropertySimple("twitterScreenName", twitter.getScreenName()));
                ctrlResult.getComplexResults().put(new PropertySimple("accessTokenFilePath", filePath));
            } else {
                ctrlResult.setError("Invalid Operation! Please Select a valid one.");
            }
        } catch (TwitterException te) {
            log.error("Twitter Error: ", te);
            ctrlResult.setError(te);
        } catch (IOException ioe) {
            log.error("Error storing AccessToken: ", ioe);
            ctrlResult.setError(ioe);
        }

        return ctrlResult;
    }

    @Override
    public void start() {
        // TODO Auto-generated method stub

    }

    @Override
    public void stop() {
        // TODO Auto-generated method stub

    }

    @Override
    public void shutdown() {
    }

}
