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
package org.rhq.enterprise.gui.download;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.mx.util.MBeanServerLocator;
import org.jboss.system.server.ServerConfig;

import org.rhq.core.domain.cloud.Server.OperationMode;
import org.rhq.core.util.ObjectNameFactory;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Serves the static content found in rhq-downloads.
 */
public class DownloadServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // the system property that disables/enables file listing - default is to show the listing
    private static String SYSPROP_SHOW_DOWNLOADS_LISTING = "rhq.server.show-downloads-listing";

    private static int numActiveDownloads = 0;
    private static boolean showDownloadsListing;

    @Override
    public void init() throws ServletException {
        log("Starting the download servlet");

        String propValue = System.getProperty(SYSPROP_SHOW_DOWNLOADS_LISTING, "true");
        showDownloadsListing = Boolean.parseBoolean(propValue);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (isServerAcceptingRequests()) {
            File requestedDirectory = getRequestedDirectory(req);
            if (requestedDirectory == null) {
                numActiveDownloads++;
                download(req, resp);
                numActiveDownloads--;
            } else {
                if (showDownloadsListing) {
                    outputFileList(requestedDirectory, req, resp);
                } else {
                    disableBrowserCache(resp);
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Listing disabled");
                }
            }
        } else {
            sendErrorServerNotAcceptingRequests(resp);
        }
        return;
    }

    private File getRequestedDirectory(HttpServletRequest req) throws ServletException {

        String pathInfo = req.getPathInfo();

        File rootDownloadsDir;
        try {
            rootDownloadsDir = getRootDownloadsDir();
        } catch (Throwable t) {
            throw new ServletException(t);
        }

        if (pathInfo == null || pathInfo.equals("") || pathInfo.equals("/")) {
            return rootDownloadsDir;
        }

        File downloadDir = new File(rootDownloadsDir, pathInfo);
        if (downloadDir.isDirectory()) {
            return downloadDir;
        }

        // either the path does not exist or its a file, not a directory
        return null;
    }

    private void download(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            File downloadDir = getRootDownloadsDir();
            File downloadFile = new File(downloadDir, req.getPathInfo());
            if (!isForbiddenPath(downloadFile, resp)) {
                if (!downloadFile.exists()) {
                    disableBrowserCache(resp);
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File does not exist: " + downloadFile.getName());
                    return;
                }
                resp.setContentType(getMimeType(downloadFile));
                resp.setHeader("Content-Disposition", "attachment; filename=" + downloadFile.getName());
                resp.setContentLength((int) downloadFile.length());
                resp.setDateHeader("Last-Modified", downloadFile.lastModified());

                FileInputStream stream = new FileInputStream(downloadFile);
                try {
                    StreamUtil.copy(stream, resp.getOutputStream(), false);
                } finally {
                    stream.close();
                }
            }
        } catch (Throwable t) {
            log("Failed to stream download content", t);
            disableBrowserCache(resp);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to download content");
        }

        return;
    }

    private String getMimeType(File file) {
        String mimeType = null;

        try {
            mimeType = getServletContext().getMimeType(file.getName());
        } catch (Throwable t) {
            // i'm paranoid, no idea if this will ever happen, but its not fatal so keep going
            log("Failed to get mime type for [" + file + "]. Cause: " + t);
        }

        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        return mimeType;
    }

    private void outputFileList(File requestedDirectory, HttpServletRequest req, HttpServletResponse resp)
        throws ServletException {

        try {
            if (!isForbiddenPath(requestedDirectory, resp)) {
                String dirName = requestedDirectory.getName();
                disableBrowserCache(resp);
                resp.setContentType("text/html");
                PrintWriter writer = resp.getWriter();
                writer.println(String.format("<html><head><title>Available Downloads: %s</title></head>", dirName));
                writer.println("<body><h1>Available Downloads</h1>");
                writer.println(String.format("<font size=\"+2\"><b><pre>%s</pre></b></font>", dirName));
                List<File> files = getDownloadFiles(requestedDirectory);
                if (files.size() > 0) {
                    String pathInfo = req.getPathInfo();
                    if (!pathInfo.endsWith("/")) {
                        pathInfo += "/";
                    }
                    writer.println("<ul>");
                    for (File file : files) {
                        writer.println("<li><a href=\"" + req.getServletPath() + pathInfo + file.getName() + "\">"
                            + file.getName() + "</a></li>");
                    }
                    writer.println("</ul>");
                } else {
                    writer.println("<h4>NONE</h4>");
                }
                writer.println("</body></html>");
            }
        } catch (Exception e) {
            throw new ServletException("Cannot get downloads listing", e);
        }
    }

    private boolean isForbiddenPath(File requestedFile, HttpServletResponse resp) throws IOException {
        boolean forbidden = true; // assume it is forbidden
        if (requestedFile.toString().contains("rhq-agent")) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Use /agentupdate/download to obtain the agent");
        } else if (requestedFile.toString().contains("rhq-client")) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Use /client/download to obtain the client");
        } else {
            forbidden = false;
        }
        return forbidden;
    }

    private void sendErrorServerNotAcceptingRequests(HttpServletResponse resp) throws IOException {
        disableBrowserCache(resp);
        resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Server Is Down For Maintenance");
    }

    private void disableBrowserCache(HttpServletResponse resp) {
        resp.setHeader("Cache-Control", "no-cache");
        resp.setHeader("Expires", "-1");
        resp.setHeader("Pragma", "no-cache");
    }

    private List<File> getDownloadFiles(File requestedDirectory) throws Exception {
        File[] filesArray = requestedDirectory.listFiles();

        // this is simple - we only serve up files located in the requested directory - no content from subdirectories
        List<File> files = new ArrayList<File>();
        if (filesArray != null) {
            for (File file : filesArray) {
                if (file.isFile()) {
                    files.add(file);
                }
            }
        }
        return files;
    }

    private File getRootDownloadsDir() throws Exception {
        MBeanServer mbs = getMBeanServer();
        ObjectName name = ObjectNameFactory.create("jboss.system:type=ServerConfig");
        Object mbean = MBeanServerInvocationHandler.newProxyInstance(mbs, name, ServerConfig.class, false);
        File serverHomeDir = ((ServerConfig) mbean).getServerHomeDir();
        File downloadDir = new File(serverHomeDir, "deploy/rhq.ear/rhq-downloads");
        if (!downloadDir.exists()) {
            throw new FileNotFoundException("Missing downloads directory at [" + downloadDir + "]");
        }
        return downloadDir;
    }

    private MBeanServer getMBeanServer() {
        return MBeanServerLocator.locateJBoss();
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