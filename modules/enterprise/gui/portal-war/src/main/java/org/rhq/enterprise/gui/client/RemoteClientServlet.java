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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rhq.core.domain.cloud.Server.OperationMode;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.core.CoreServerMBean;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Serves the remote client binary that is stored in the RHQ Server's download area.
 * This servlet also provides version information regarding the version of the remote
 * client this servlet serves up.
 */
public class RemoteClientServlet extends HttpServlet {
    private static final String RHQ_SERVER_VERSION = "rhq-server.version";
    private static final String RHQ_SERVER_BUILD_NUMBER = "rhq-server.build-number";
    private static final String RHQ_CLIENT_MD5 = "rhq-client.md5";

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

    @Override
    public void init() throws ServletException {
        log("Starting the remote client servlet");

        // make sure we have a binary distro file; log its location
        try {
            log("Remote Client Binary File: " + getRemoteClientZip());
        } catch (Throwable t) {
            log("Remote client is not available for deployment. Cause: " + t.toString());
            return;
        }

        // make sure we create a version file if we have to by getting the version file now
        try {
            File versionFile = getVersionFile();

            // log the version info - this also makes sure we can read it back in
            log(versionFile + ": " + new String(StreamUtil.slurp(new FileInputStream(versionFile))));

        } catch (Throwable t) {
            log("Cannot determine the remote client version information", t);
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
            sendErrorDownloadDisabled(resp);
            return;
        } else if (limit < numActiveDownloads) {
            sendErrorTooManyDownloads(resp);
            return;
        }

        try {
            File zip = getRemoteClientZip();
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
            log("Failed to stream remote client zip", t);
            disableBrowserCache(resp);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to stream remote client zip");
        }

        return;
    }

    private File getRemoteClientZip() throws Exception {
        File dir = getDownloadDir();
        for (File file : dir.listFiles()) {
            if (file.getName().endsWith(".zip")) {
                return file;
            }
        }

        throw new FileNotFoundException("Missing remote client binary in [" + dir + "]");
    }

    private File getDownloadDir() throws Exception {
        File serverHomeDir = LookupUtil.getCoreServer().getJBossServerHomeDir();
        File downloadDir = new File(serverHomeDir, "deploy/rhq.ear/rhq-downloads/rhq-client");
        if (!downloadDir.exists()) {
            throw new FileNotFoundException("Missing remote client download directory at [" + downloadDir + "]");
        }
        return downloadDir;
    }

    private void getVersion(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            File versionFile = getVersionFile();
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
        String limitStr = System.getProperty(SYSPROP_CLIENT_DOWNLOADS_LIMIT);
        int limit;
        try {
            limit = Integer.parseInt(limitStr);
        } catch (Exception e) {
            limit = DEFAULT_CLIENT_DOWNLOADS_LIMIT;
            log("Remote Client downloads limit system property [" + SYSPROP_CLIENT_DOWNLOADS_LIMIT
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

    private File getVersionFile() throws Exception {
        File versionFile = new File(getDownloadDir(), "rhq-client-version.properties");
        if (!versionFile.exists()) {
            // we do not have the version properties file yet, let's extract some info and create one
            StringBuilder serverVersionInfo = new StringBuilder();

            CoreServerMBean coreServer = LookupUtil.getCoreServer();
            serverVersionInfo.append(RHQ_SERVER_VERSION + '=').append(coreServer.getVersion()).append('\n');
            serverVersionInfo.append(RHQ_SERVER_BUILD_NUMBER + '=').append(coreServer.getBuildNumber()).append('\n');

            // calculate the MD5 of the client zip
            File zip = getRemoteClientZip();
            String md5Property = RHQ_CLIENT_MD5 + '=' + MessageDigestGenerator.getDigestString(zip) + '\n';

            // now write the server version info in our internal version file our servlet will use
            FileOutputStream versionFileOutputStream = new FileOutputStream(versionFile);
            try {
                versionFileOutputStream.write(serverVersionInfo.toString().getBytes());
                versionFileOutputStream.write(md5Property.getBytes());
            } finally {
                try {
                    versionFileOutputStream.close();
                } catch (Exception e) {
                }
            }
        }

        return versionFile;
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