/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

import mazz.i18n.Logger;

import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;

/**
 * Checks to see if there is an software update for the agent. In other words,
 * this checks to see if the current agent's version is older than the latest
 * available agent. If there is a newer agent available, this object will
 * tell you that.
 *  
 * @author John Mazzitelli
 *
 */
public class AgentUpdateCheck {
    private static final Logger LOG = AgentI18NFactory.getLogger(AgentUpdateCheck.class);

    private AgentMain agent;

    public AgentUpdateCheck(AgentMain agent) {
        this.agent = agent;
    }

    /**
     * Returns the URL that will be accessed to obtain the agent update version information.
     * 
     * @return version URL
     * 
     * @throws Exception if for some reason a valid URL could not be obtained
     */
    public URL getUrl() throws Exception {
        return new URL(this.agent.getConfiguration().getAgentUpdateVersionUrl());
    }

    /**
     * Returns the agent update version information.
     * 
     * @return version information of both the update and the current agent
     * 
     * @throws Exception if agent has disabled updates or it failed to get the update version information
     */
    public AgentUpdateInformation getAgentUpdateInformation() throws Exception {

        if (!agent.getConfiguration().isAgentUpdateEnabled()) {
            throw new Exception(this.agent.getI18NMsg().getMsg(AgentI18NResourceKeys.UPDATE_VERSION_DISABLED_BY_AGENT));
        }

        Properties versionProps = null;
        URL url = null;
        HttpURLConnection conn = null;
        boolean keep_going = true;

        while (keep_going) {
            try {
                // we only support http/s
                url = getUrl();
                LOG.debug(AgentI18NResourceKeys.UPDATE_VERSION_RETRIEVAL, url);
                conn = (HttpURLConnection) url.openConnection();
                versionProps = new Properties();
                versionProps.load(conn.getInputStream());
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
            }
        }

        // we only ever get here when we are successful
        return new AgentUpdateInformation(versionProps);
    }
}
