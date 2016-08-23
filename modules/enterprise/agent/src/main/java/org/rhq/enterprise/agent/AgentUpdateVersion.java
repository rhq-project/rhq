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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.agent;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

import javax.net.ssl.HttpsURLConnection;

import mazz.i18n.Logger;

import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;

/**
 * Checks to see if there is an software update for the agent and if so,
 * retrieves the {@link #getAgentUpdateInformation() information} on it.
 *  
 * @author John Mazzitelli
 */
public class AgentUpdateVersion {
    private static final Logger LOG = AgentI18NFactory.getLogger(AgentUpdateVersion.class);

    private AgentMain agent;
    private AgentUpdateInformation agentUpdateInformation;

    public AgentUpdateVersion(AgentMain agent) {
        this.agent = agent;
        this.agentUpdateInformation = null; // we will lazy load this
    }

    /**
     * Returns the URL that will be accessed to obtain the agent update version information.
     * 
     * @return version URL
     * 
     * @throws Exception if for some reason a valid URL could not be obtained
     */
    public URL getVersionUrl() throws Exception {
        return new URL(this.agent.getConfiguration().getAgentUpdateVersionUrl());
    }

    /**
     * Returns the last known agent update version information. Note that the agent update
     * information is cached; if you want this object to ask the server again for the version
     * information, call {@link #refresh()}.  This method will, however, call {@link #refresh()}
     * for you if the information has not been retrieved at all yet.
     * 
     * @return version information of both the update and the current agent
     * 
     * @throws Exception if agent has disabled updates or it failed to get the update version information
     */
    public AgentUpdateInformation getAgentUpdateInformation() throws Exception {
        if (this.agentUpdateInformation == null) {
            refresh();
        }
        return this.agentUpdateInformation;
    }

    /**
     * Forces this object to refresh the agent update information by asking the server for the data again.
     * This always sends a request to the server in order to force the update information to be retrieved again.
     * After this method is called, you can retrieve the new information by calling the method
     * {@link #getAgentUpdateInformation()}.
     *  
     * @throws Exception if agent has disabled updates or it failed to get the update version information
     */
    public void refresh() throws Exception {

        if (!agent.getConfiguration().isAgentUpdateEnabled()) {
            throw new Exception(this.agent.getI18NMsg().getMsg(AgentI18NResourceKeys.UPDATE_VERSION_DISABLED_BY_AGENT));
        }

        Properties versionProps = null;
        URL url = null;
        boolean keep_going = true;

        while (keep_going) {
            HttpURLConnection conn = null;
            InputStream inStream = null;

            try {
                // we only support http/s
                url = getVersionUrl();
                LOG.debug(AgentI18NResourceKeys.UPDATE_VERSION_RETRIEVAL, url);

                if (url.getProtocol().equals("https")) {
                    conn = openSecureConnection(url);
                } else {
                    conn = (HttpURLConnection) url.openConnection(); // we only support http(s), so this cast is OK
                }

                versionProps = new Properties();
                inStream = conn.getInputStream();
                versionProps.load(inStream);
                keep_going = false;
            } catch (Exception e) {
                if (conn != null) {
                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_UNAVAILABLE) {
                        // server is overloaded with other agents downloading, we must wait
                        LOG.debug(AgentI18NResourceKeys.UPDATE_VERSION_UNAVAILABLE, url);
                        Thread.sleep(10000L); // sleep alittle bit to give the server some time (allow us to be interrupted!)
                        keep_going = true;
                    } else if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                        // server has disabled agent updates
                        throw new Exception(this.agent.getI18NMsg().getMsg(
                            AgentI18NResourceKeys.UPDATE_VERSION_DISABLED_BY_SERVER, url));
                    } else {
                        // some unexpected error occurred
                        LOG.warn(AgentI18NResourceKeys.UPDATE_VERSION_FAILURE, url, ThrowableUtil.getAllMessages(e));
                        throw e;
                    }
                } else {
                    LOG.warn(AgentI18NResourceKeys.UPDATE_VERSION_FAILURE, url, ThrowableUtil.getAllMessages(e));
                    throw e;
                }
            } finally {
                if (inStream != null) {
                    try {
                        inStream.close();
                    } catch (Exception ioe) {
                    }
                }
            }
        }

        // we only ever get here when we are successful
        this.agentUpdateInformation = new AgentUpdateInformation(versionProps);
        LOG.debug(AgentI18NResourceKeys.UPDATE_VERSION_RETRIEVED, url, this.agentUpdateInformation);

        return;
    }

    private HttpsURLConnection openSecureConnection(URL url) throws Exception {
        AgentConfiguration config = this.agent.getConfiguration();
        SecureConnectorFactory secureConnectorFactory = new SecureConnectorFactory();
        SecureConnector secureConnector = secureConnectorFactory.getInstanceWithAgentConfiguration(
                config, this.agent.getAgentHomeDirectory());
        return secureConnector.openSecureConnection(url);
    }
}
