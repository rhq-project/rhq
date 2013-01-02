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
 * Serves the static content found in rhq-downloads.
 */
public class DownloadServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // the system property that disables/enables file listing - default is to show the listing
    private static String SYSPROP_SHOW_DOWNLOADS_LISTING = "rhq.server.show-downloads-listing";

    private static int numActiveDownloads = 0;
    private static boolean showDownloadsListing;

    private Log log = LogFactory.getLog(this.getClass());

    @Override
    public void init() throws ServletException {
        log.info("Starting the RHQ download servlet...");

        String propValue = System.getProperty(SYSPROP_SHOW_DOWNLOADS_LISTING, "true");
        showDownloadsListing = Boolean.parseBoolean(propValue);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // seeing odd browser caching issues, even though we set Last-Modified. so force no caching for now
        disableBrowserCache(resp);

        if (isServerAcceptingRequests()) {
            String requestedDirectory = getRequestedDirectory(req);
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

    /**
     * Returns the relative path of the requested directory but only if it exists in
     * one of the root directories. If the requested path is a file or the directory
     * doesn't exist under a root directory, null is returned.
     *
     * @param req
     * @return relative path of the directory being requested
     * @throws ServletException
     */
    private String getRequestedDirectory(HttpServletRequest req) throws ServletException {

        String pathInfo = req.getPathInfo();

        File[] rootDownloadsDirs;
        try {
            rootDownloadsDirs = getRootDownloadsDirs();
        } catch (Throwable t) {
            throw new ServletException(t);
        }

        if (pathInfo == null || pathInfo.equals("") || pathInfo.equals("/")) {
            return "";
        }

        for (File rootDownloadsDir : rootDownloadsDirs) {
            File downloadDir = new File(rootDownloadsDir, pathInfo);
            if (downloadDir.isDirectory()) {
                return pathInfo;
            }
        }

        // either the path does not exist or its a file, not a directory
        return null;
    }

    private void download(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            if (isForbiddenPath(req.getPathInfo(), resp)) {
                return;
            }

            // look for the file in one of the root download directories
            File fileToDownload = null;
            File[] downloadDirs = getRootDownloadsDirs();
            for (File downloadDir : downloadDirs) {
                File file = new File(downloadDir, req.getPathInfo());
                if (file.exists()) {
                    fileToDownload = file;
                    break;
                }
            }

            if (fileToDownload == null) {
                disableBrowserCache(resp);
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File does not exist: " + req.getPathInfo());
                return;
            }

            resp.setContentType(getMimeType(fileToDownload));
            resp.setHeader("Content-Disposition", "attachment; filename=" + fileToDownload.getName());
            resp.setContentLength((int) fileToDownload.length());
            resp.setDateHeader("Last-Modified", fileToDownload.lastModified());

            FileInputStream stream = new FileInputStream(fileToDownload);
                try {
                    StreamUtil.copy(stream, resp.getOutputStream(), false);
                } finally {
                    stream.close();
                }
        } catch (Throwable t) {
            log.error("Failed to stream download content.", t);
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
            log.warn("Failed to get mime type for [" + file + "].", t);
        }

        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        return mimeType;
    }

    private void outputFileList(String requestedDirectory, HttpServletRequest req, HttpServletResponse resp)
        throws ServletException {

        try {
            if (!isForbiddenPath(requestedDirectory, resp)) {
                File requestedDirectoryRelativePath = new File(requestedDirectory);
                String dirName = requestedDirectoryRelativePath.getName();
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

    private boolean isForbiddenPath(String requestedPath, HttpServletResponse resp) throws IOException {
        boolean forbidden = true; // assume it is forbidden
        if (requestedPath.toString().contains("rhq-agent")) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Use /agentupdate/download to obtain the agent");
        } else if (requestedPath.toString().contains("rhq-client")) {
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
        resp.setHeader("Cache-Control", "no-cache, no-store");
        resp.setHeader("Expires", "-1");
        resp.setHeader("Pragma", "no-cache");
    }

    private List<File> getDownloadFiles(String requestedDirectory) throws Exception {
        // its possible if more than one root dir has the file, we'll get duplicates.
        // this should never happen, so ignore this edge case.
        List<File> returnFiles = new ArrayList<File>();
        File[] rootDownloadDirs = getRootDownloadsDirs();
        for (File rootDownloadDir : rootDownloadDirs) {
            File dir = new File(rootDownloadDir, requestedDirectory);
            File[] filesArray = dir.listFiles();
            // this is simple - we only serve up files located in the requested directory - no content from subdirectories
            if (filesArray != null) {
                for (File file : filesArray) {
                    if (file.isFile()) {
                        returnFiles.add(file);
                    }
                }
            }
        }

        return returnFiles;
    }

    /**
     * There are two locations for downloads - the static content under the EAR's rhq-downloads
     * and dynamic content in the data directory.
     *
     * @return the two root locations
     *
     * @throws Exception
     */
    private File[] getRootDownloadsDirs() throws Exception {
        File earDir = LookupUtil.getCoreServer().getEarDeploymentDir();
        File downloadDir = new File(earDir, "rhq-downloads");
        if (!downloadDir.exists()) {
            throw new FileNotFoundException("Missing downloads directory at [" + downloadDir + "]");
        }

        File dataDir = LookupUtil.getCoreServer().getJBossServerDataDir();
        File dataDownloadDir = new File(dataDir, "rhq-downloads");

        return new File[] { dataDownloadDir, downloadDir }; // put the data dir first, I think that should take precedence
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