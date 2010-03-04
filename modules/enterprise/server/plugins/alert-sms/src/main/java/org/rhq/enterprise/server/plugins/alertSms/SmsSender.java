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
package org.rhq.enterprise.server.plugins.alertSms;

import java.io.BufferedReader;
import java.io.StringReader;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientRequestFactory;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.client.core.executors.ApacheHttpClientExecutor;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.notification.ResultState;
import org.rhq.core.domain.alert.notification.SenderResult;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSender;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Sender class for Alerts
 * @author Heiko W. Rupp
 */
public class SmsSender extends AlertSender {

    private final Log log = LogFactory.getLog(SmsSender.class);
    private long TWO_MINUTES = 1000L * 60L * 2L;
    private static final String DEVGARDEN_SMS_GW = "https://gateway.developer.telekom.com/p3gw-mod-odg-sms/rest";
    private static final String DEVGARDEN_SECURE_TOKEN_SERVER = "https://sts.idm.telekom.com/rest-v1/tokens/odg";
    private static final String TOKEN_EXPIRY_TIME = "token-expiry-time";
    private static final String TOKEN = "token";

    @Override
    public SenderResult send(Alert alert) {

        String token = null;
        try {
            token = getToken();
        } catch (Exception e) {
            return new SenderResult(ResultState.FAILURE, "Can not obtain token: " + e.getMessage());
        }

        Resource res = alert.getAlertDefinition().getResource();
        Integer alertId = alert.getId();

        StringBuilder b = new StringBuilder();
        b.append("Alert ");
        b.append("(").append(alertId).append("): ");
        b.append(res.getName());
        b.append(",id=(").append(res.getId()).append(")");
        b.append("Brought by RHQ");

        try {
            sendAlertSms(token,b.toString());
        } catch (Exception e) {
            return new SenderResult(ResultState.FAILURE, "Failed to send SMS: " + e.getMessage());
        }

        return new SenderResult(ResultState.SUCCESS, "SMS sent");
    }

    /**
     * Do the actual SMS sending. For this to succeed, it needs an auth token.
     * @param token valid access token
     * @param message the message to send
     */
    private void sendAlertSms(String token, String message) throws Exception {

        String mode = preferences.getSimpleValue("environment","sandbox");
        String tel = alertParameters.getSimpleValue("tel",null);
        if (tel==null)
            throw new IllegalArgumentException("No telephone number given");

        ClientRequest req = new ClientRequest(DEVGARDEN_SMS_GW + "/" + mode + "/sms");

        String body = "number=" + tel + "&" + "message=" + message; // TODO encode?
        req.body(MediaType.APPLICATION_FORM_URLENCODED, body);

        req.header("Authorization","TAuth realm=\"https://odg.t-online.de\",tauth_token=\"" + token + "\"");
        req.accept(MediaType.TEXT_PLAIN_TYPE);



        try {
            ClientResponse resp = req.post(String.class);
            if (resp.getStatus()==200) {
                System.out.println(resp.getEntity());
            }
            else {
                System.out.println("Fehler " + resp.getStatus());
                System.out.println(resp.getEntity());
                throw new RuntimeException((String) resp.getEntity());
            }
        } catch (Exception e) {
            e.printStackTrace();  // TODO: Customise this generated block
            throw (e);
        }

    }

    /**
     * Get a token from the secure token server if needed. Store the token
     * in preferences for caching purposes.
     * @return a token if everything is ok
     * @throws Exception if no token can be obtained.
     */
    private String getToken() throws Exception {

        String token = preferences.getSimpleValue(TOKEN,null);
        PropertySimple tokenTimeProp = preferences.getSimple(TOKEN_EXPIRY_TIME);
        Long tokenExpTime = null;
        if (tokenTimeProp!=null)
            tokenExpTime = tokenTimeProp.getLongValue();

        if (token == null || tokenExpTime == null || tokenExpTime < System.currentTimeMillis() + TWO_MINUTES ) {

            String userId = preferences.getSimpleValue("login",null);
            String password = preferences.getSimpleValue("password",null);

            if (userId==null || password==null)
                throw new IllegalArgumentException("User name or password were missing");


            Credentials credentials = new UsernamePasswordCredentials(userId, password);
            HttpClient httpClient = new HttpClient();
            httpClient.getState().setCredentials(AuthScope.ANY, credentials);
            httpClient.getParams().setAuthenticationPreemptive(true);

            ClientExecutor clientExecutor = new ApacheHttpClientExecutor(httpClient);

            try {
                URI uri = new URI(DEVGARDEN_SECURE_TOKEN_SERVER);
                ClientRequestFactory fac = new ClientRequestFactory(clientExecutor,uri);

                ClientRequest request = fac.createRequest(DEVGARDEN_SECURE_TOKEN_SERVER);

                request.accept(MediaType.TEXT_PLAIN_TYPE);
                ClientResponse resp = request.get(String.class);

                // TODO check status
                System.out.println(resp.getStatus());
                String response = (String) resp.getEntity();
                BufferedReader reader = new BufferedReader(new StringReader(response));
                while (reader.ready()) {
                    String line = reader.readLine();
                    if (line.startsWith("token=")) {
                        token = line.substring(6);
                        break;
                    }
                }
                System.out.println("Token: " + token);
                MultivaluedMap<String, List> headers = resp.getHeaders();
                List expiresList = headers.get("Expires");
                String expires = (String) expiresList.get(0);
                SimpleDateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
                Date date = df.parse(expires);
                if (tokenTimeProp==null) {
                    tokenTimeProp=new PropertySimple(TOKEN_EXPIRY_TIME, date.getTime());
                    preferences.put(tokenTimeProp);
                }
                else
                    tokenTimeProp.setLongValue(date.getTime());

                PropertySimple tokenProp = preferences.getSimple(TOKEN);
                if (tokenProp ==null) {
                    tokenProp = new PropertySimple(TOKEN,token);
                    preferences.put(tokenProp);
                }
                else {
                    tokenProp.setStringValue(token);
                }
                ConfigurationManagerLocal mgr = LookupUtil.getConfigurationManager();
                mgr.mergeConfiguration(preferences);

            } catch (Exception e) {
                e.printStackTrace();  // TODO: Customise this generated block
                throw e;
            }
        }

        return token;
    }

}
