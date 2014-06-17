/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
package org.rhq.enterprise.server.core.jaas;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.keycloak.OAuth2Constants;
import org.keycloak.ServiceUrlConstants;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.util.HttpClientBuilder;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.KeycloakUriBuilder;

/**
 *
 * @author Jirka Kremser
 */
public class KeycloakLoginUtils {
    private static final String REALM_NAME = "master";
    public static final String APP_NAME = "coregui";
//    private static final String APP_NAME = "rhq-login-module";

    private static class TypedList extends ArrayList<RoleRepresentation> {
    }

    public static class Failure extends Exception {
        private int status;

        public Failure(int status) {
            this.status = status;
        }

        public int getStatus() {
            return status;
        }
    }

    // todo: may be memory consuming - add some starvation (WeakHashMap?)
    private static Map<String, AccessTokenResponse> tokens = new HashMap<String, AccessTokenResponse>();

    public static AccessTokenResponse getToken(String username, String password, String keycloakServerUrl)
        throws IOException {

        HttpClient client = new HttpClientBuilder().disableTrustManager().build();

        try {
            HttpPost post = new HttpPost(KeycloakUriBuilder.fromUri(keycloakServerUrl)
                .path(ServiceUrlConstants.TOKEN_SERVICE_DIRECT_GRANT_PATH).build(REALM_NAME));
            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            formparams.add(new BasicNameValuePair("username", username));
            formparams.add(new BasicNameValuePair("password", password));
            formparams.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, "rhq-login-module"));
            UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
            post.setEntity(form);

            HttpResponse response = client.execute(post);
            int status = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            if (status != 200) {
                throw new IOException("Bad status: " + status);
            }
            if (entity == null) {
                throw new IOException("No Entity");
            }
            InputStream is = entity.getContent();
            try {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                int c;
                while ((c = is.read()) != -1) {
                    os.write(c);
                }
                byte[] bytes = os.toByteArray();
                String json = new String(bytes);
                try {
                    AccessTokenResponse token = JsonSerialization.readValue(json, AccessTokenResponse.class);
                    tokens.put(username, token);
                    return token;
                } catch (IOException e) {
                    throw new IOException(json, e);
                }
            } finally {
                try {
                    is.close();
                } catch (IOException ignored) {

                }
            }
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    private static void logout(AccessTokenResponse res, String keycloakServerUrl) throws IOException {
        if (res == null)
            return;
        HttpClient client = new HttpClientBuilder().disableTrustManager().build();

        try {
            HttpGet get = new HttpGet(KeycloakUriBuilder.fromUri(keycloakServerUrl)
                .path(ServiceUrlConstants.TOKEN_SERVICE_LOGOUT_PATH).queryParam("session_state", res.getSessionState())
                .build(REALM_NAME));
            HttpResponse response = client.execute(get);
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                return;
            }
            InputStream is = entity.getContent();
            if (is != null)
                is.close();
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    public static void logout(String username, String keycloakServerUrl) throws IOException {
        logout(tokens.get(username), keycloakServerUrl);
    }

    public static List<RoleRepresentation> getRealmRoles(AccessTokenResponse res, String keycloakServerUrl)
        throws Failure {

        HttpClient client = new HttpClientBuilder().disableTrustManager().build();
        try {
            HttpGet get = new HttpGet(keycloakServerUrl + "/auth/admin/realms/" + REALM_NAME + "/roles");
            get.addHeader("Authorization", "Bearer " + res.getToken());
            try {
                HttpResponse response = client.execute(get);
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new Failure(response.getStatusLine().getStatusCode());
                }
                HttpEntity entity = response.getEntity();
                InputStream is = entity.getContent();
                try {
                    return JsonSerialization.readValue(is, TypedList.class);
                } finally {
                    is.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } finally {
            client.getConnectionManager().shutdown();
        }
    }
    
    public static List<RoleRepresentation> getUserRoles(AccessTokenResponse res, String username, String keycloakServerUrl)
        throws Failure {

        HttpClient client = new HttpClientBuilder().disableTrustManager().build();
        try {
            HttpGet get = new HttpGet(keycloakServerUrl + "/auth/admin/realms/" + REALM_NAME + "/users/" + username
                + "/role-mappings/applications/" + APP_NAME + "/composite");
            get.addHeader("Authorization", "Bearer " + res.getToken());
            try {
                HttpResponse response = client.execute(get);
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new Failure(response.getStatusLine().getStatusCode());
                }
                HttpEntity entity = response.getEntity();
                InputStream is = entity.getContent();
                try {
                    return JsonSerialization.readValue(is, TypedList.class);
                } finally {
                    is.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } finally {
            client.getConnectionManager().shutdown();
        }
    }
        
    public static AccessTokenResponse getToken(String username) {
        return tokens.get(username);
    }
    
    
    public static IDToken extractIdToken(String idToken) {
        if (idToken == null) return null;
        JWSInput input = new JWSInput(idToken);
        try {
            return input.readJsonContent(IDToken.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
