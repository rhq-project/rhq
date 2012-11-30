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
package org.rhq.enterprise.gui.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.cloud.Server.OperationMode;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Serves the remote client binary that is stored in the RHQ Server's download area.
 * This servlet also provides version information regarding the version of the remote
 * client this servlet serves up.
 */
public class RemoteClientServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // the system property that defines how many concurrent downloads we will allow
    private static String SYSPROP_CLIENT_DOWNLOADS_LIMIT = "rhq.server.client-downloads-limit";

    // if the system property is not set or invalid, this is the default limit for number of concurrent downloads
    // There is no reason this be heavily downloaded.
    private static int DEFAULT_CLIENT_DOWNLOADS_LIMIT = 5;

    // the error code that will be returned if the server has been configured to disable client updates
    private static final int ERROR_CODE_CLIENT_UPDATE_DISABLED = HttpServletResponse.SC_FORBIDDEN;

    // the error code that will be returned if the server has too many clients downloading the binary
    private static final int ERROR_CODE_TOO_MANY_DOWNLOADS = HttpServletResponse.SC_SERVICE_UNAVAILABLE;

    private static int numActiveDownloads = 0;

    private Log log = LogFactory.getLog(this.getClass());

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
            sendErrorDownloadDisabled(resp);
            return;
        } else if (limit < numActiveDownloads) {
            sendErrorTooManyDownloads(resp);
            return;
        }

        try {
            File zip = LookupUtil.getRemoteClientManager().getRemoteClientBinaryFile();
            if (!zip.exists()) {
                disableBrowserCache(resp);
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Remote Client binary does not exist: "
                    + zip.getName());
                return;
            }

            resp.setContentType("application/octet-stream");
            resp.setHeader("Content-Disposition", "attachment; filename=" + zip.getName());
            resp.setContentLength((int) zip.length());
            resp.setDateHeader("Last-Modified", zip.lastModified());

            FileInputStream zipStream = new FileInputStream(zip);
            try {
                StreamUtil.copy(zipStream, resp.getOutputStream(), false);
            } finally {
                zipStream.close();
            }
        } catch (Throwable t) {
            log.error("Failed to stream remote client zip.", t);
            disableBrowserCache(resp);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to stream remote client zip");
        }

        return;
    }

    private void getVersion(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            File versionFile = LookupUtil.getRemoteClientManager().getRemoteClientVersionFile();
            resp.setContentType("text/plain");
            resp.setDateHeader("Last-Modified", versionFile.lastModified());

            FileInputStream stream = new FileInputStream(versionFile);
            byte[] versionData = StreamUtil.slurp(stream);
            resp.getOutputStream().write(versionData);
        } catch (Throwable t) {
            log.error("Failed to stream version info.", t);
            disableBrowserCache(resp);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to stream version info");
        }

        return;
    }

    private int getDownloadLimit() {
        String limitStr = System.getProperty(SYSPROP_CLIENT_DOWNLOADS_LIMIT);
        int limit;
        try {
            limit = Integer.parseInt(limitStr);
        } catch (Exception e) {
            limit = DEFAULT_CLIENT_DOWNLOADS_LIMIT;
            log.warn("Remote Client downloads limit system property [" + SYSPROP_CLIENT_DOWNLOADS_LIMIT
                + "] is either not set or invalid [" + limitStr + "] - limit will be [" + limit + "].");
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
        resp.sendError(ERROR_CODE_CLIENT_UPDATE_DISABLED, "Server Is Down For Maintenance");
    }

    private void sendErrorDownloadDisabled(HttpServletResponse resp) throws IOException {
        disableBrowserCache(resp);
        resp.sendError(ERROR_CODE_CLIENT_UPDATE_DISABLED, "Client Download Has Been Disabled");
    }

    private void sendErrorTooManyDownloads(HttpServletResponse resp) throws IOException {
        disableBrowserCache(resp);
        resp.setHeader("Retry-After", "30");
        resp.sendError(ERROR_CODE_TOO_MANY_DOWNLOADS, "Maximum limit exceeded - download client later");
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