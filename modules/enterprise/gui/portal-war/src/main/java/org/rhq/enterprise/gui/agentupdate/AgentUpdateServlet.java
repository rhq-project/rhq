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
package org.rhq.enterprise.gui.agentupdate;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rhq.core.domain.cloud.Server.OperationMode;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Serves the agent update binary that is stored in the RHQ Server's download area.
 * This servlet also provides version information regarding the version of the agent
 * this servlet serves up as well as versions of agents the RHQ Server supports.
 */
public class AgentUpdateServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // the system property that defines how many concurrent downloads we will allow
    private static String SYSPROP_AGENT_DOWNLOADS_LIMIT = "rhq.server.agent-downloads-limit";

    // if the system property is not set or invalid, this is the default limit for number of concurrent downloads
    private static int DEFAULT_AGENT_DOWNLOADS_LIMIT = 45;

    // the error code that will be returned if the server has been configured to disable agent updates
    private static final int ERROR_CODE_AGENT_UPDATE_DISABLED = HttpServletResponse.SC_FORBIDDEN;

    // the error code that will be returned if the server has too many agents downloading the agent update binary
    private static final int ERROR_CODE_TOO_MANY_DOWNLOADS = HttpServletResponse.SC_SERVICE_UNAVAILABLE;

    private static int numActiveDownloads = 0;

    private AgentManagerLocal agentManager = null;

    @Override
    public void init() throws ServletException {
        log("Starting the agent update servlet");

        // make sure we have a agent update binary file; log its location
        try {
            log("Agent Update Binary File: " + getAgentUpdateBinaryFile());
        } catch (Throwable t) {
            log("Missing agent update binary file - agents will not be able to update", t);
        }

        // make sure we create a version file if we have to by getting the version file now
        try {
            File versionFile = getAgentUpdateVersionFile();

            // log the version info - this also makes sure we can read it back in
            log(versionFile + ": " + new String(StreamUtil.slurp(new FileInputStream(versionFile))));

        } catch (Throwable t) {
            log("Cannot determine the agent version information - agents will not be able to update", t);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // seeing odd browser caching issues, even though we set Last-Modified. so force no caching for now
        disableBrowserCache(resp);

        String servletPath = req.getServletPath();
        if (servletPath != null) {
            if (isServerAcceptingRequests()) {
                if (servletPath.endsWith("version")) {
                    getVersion(req, resp);
                } else if (servletPath.endsWith("download")) {
                    try {
                        numActiveDownloads++;
                        getDownload(req, resp);
                    } finally {
                        numActiveDownloads--;
                    }
                } else {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid servlet path [" + servletPath
                        + "] - please contact administrator");
                }
            } else {
                sendErrorServerNotAcceptingRequests(resp);
            }
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid servlet path - please contact administrator");
        }

        return;
    }

    private void getDownload(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int limit = getDownloadLimit();
        if (limit <= 0) {
            sendErrorAgentUpdateDisabled(resp);
            return;
        } else if (limit < numActiveDownloads) {
            sendErrorTooManyDownloads(resp);
            return;
        }

        try {
            File agentJar = getAgentUpdateBinaryFile();
            resp.setContentType("application/octet-stream");
            resp.setHeader("Content-Disposition", "attachment; filename=" + agentJar.getName());
            resp.setContentLength((int) agentJar.length());
            resp.setDateHeader("Last-Modified", agentJar.lastModified());

            FileInputStream agentJarStream = new FileInputStream(agentJar);
            try {
                StreamUtil.copy(agentJarStream, resp.getOutputStream(), false);
            } finally {
                agentJarStream.close();
            }
        } catch (Throwable t) {
            log("Failed to stream agent jar", t);
            disableBrowserCache(resp);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to stream agent jar");
        }

        return;
    }

    private void getVersion(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            File versionFile = getAgentUpdateVersionFile();
            resp.setContentType("text/plain");
            resp.setDateHeader("Last-Modified", versionFile.lastModified());

            FileInputStream stream = new FileInputStream(versionFile);
            byte[] versionData = StreamUtil.slurp(stream);
            resp.getOutputStream().write(versionData);
        } catch (Throwable t) {
            log("Failed to stream version info", t);
            disableBrowserCache(resp);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to stream version info");
        }

        return;
    }

    private int getDownloadLimit() {
        // if the server cloud was configured to disallow updates, return 0
        Properties systemConfig = LookupUtil.getSystemManager().getSystemConfiguration(
            LookupUtil.getSubjectManager().getOverlord());
        if (!Boolean.parseBoolean(systemConfig.getProperty(RHQConstants.EnableAgentAutoUpdate))) {
            return 0;
        }

        String limitStr = System.getProperty(SYSPROP_AGENT_DOWNLOADS_LIMIT);
        int limit;
        try {
            limit = Integer.parseInt(limitStr);
        } catch (Exception e) {
            limit = DEFAULT_AGENT_DOWNLOADS_LIMIT;
            log("Agent downloads limit system property [" + SYSPROP_AGENT_DOWNLOADS_LIMIT
                + "] is either not set or invalid [" + limitStr + "] - limit will be [" + limit + "]");
        }

        return limit;
    }

    private void disableBrowserCache(HttpServletResponse resp) {
        resp.setHeader("Cache-Control", "no-cache, no-store");
        resp.setHeader("Expires", "-1");
        resp.setHeader("Pragma", "no-cache");
    }

    private void sendErrorServerNotAcceptingRequests(HttpServletResponse resp) throws IOException {
        disableBrowserCache(resp);
        resp.sendError(ERROR_CODE_AGENT_UPDATE_DISABLED, "Server Is Down For Maintenance");
    }

    private void sendErrorAgentUpdateDisabled(HttpServletResponse resp) throws IOException {
        disableBrowserCache(resp);
        resp.sendError(ERROR_CODE_AGENT_UPDATE_DISABLED, "Agent Updates Has Been Disabled");
    }

    private void sendErrorTooManyDownloads(HttpServletResponse resp) throws IOException {
        disableBrowserCache(resp);
        resp.setHeader("Retry-After", "30");
        resp.sendError(ERROR_CODE_TOO_MANY_DOWNLOADS, "Maximum limit exceeded - download agent later");
    }

    private File getAgentUpdateVersionFile() throws Exception {
        return getAgentManager().getAgentUpdateVersionFile();
    }

    private File getAgentUpdateBinaryFile() throws Exception {
        return getAgentManager().getAgentUpdateBinaryFile();
    }

    private AgentManagerLocal getAgentManager() {
        if (this.agentManager == null) {
            this.agentManager = LookupUtil.getAgentManager();
        }
        return this.agentManager;
    }

    private boolean isServerAcceptingRequests() {
        try {
            OperationMode mode = LookupUtil.getServerManager().getServer().getOperationMode();
            return mode == OperationMode.NORMAL;
        } catch (Exception e) {
            return false;
        }
    }
}