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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.mx.util.MBeanServerLocator;
import org.jboss.system.server.ServerConfig;

import org.rhq.core.domain.util.MD5Generator;
import org.rhq.core.util.ObjectNameFactory;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.core.CoreServerMBean;
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
    private static int DEFAULT_AGENT_DOWNLOADS_LIMIT = 100;

    // the error code that will be returned if the server has been configured to disable agent updates
    private static final int ERROR_CODE_AGENT_UPDATE_DISABLED = HttpServletResponse.SC_FORBIDDEN;

    // the error code that will be returned if the server has too many agents downloading the agent update binary
    private static final int ERROR_CODE_TOO_MANY_DOWNLOADS = HttpServletResponse.SC_SERVICE_UNAVAILABLE;

    private static int numActiveDownloads = 0;

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
        String servletPath = req.getServletPath();
        if (servletPath != null) {
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
        int limit = getDownloadLimit();
        if (limit <= 0) {
            sendErrorAgentUpdateDisabled(resp);
            return;
        }

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
        // TODO: have SystemManager store a "agent update disable" flag in RHQ_SYSTEM_CONFIG table
        //       read that setting and return 0 if we are disabled

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
        resp.setHeader("Cache-Control", "no-cache");
        resp.setHeader("Expires", "-1");
        resp.setHeader("Pragma", "no-cache");
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
        File agentDownloadDir = getAgentDownloadDir();
        File versionFile = new File(agentDownloadDir, "rhq-server-agent-versions.properties");
        if (!versionFile.exists()) {
            // we do not have the version properties file yet, let's extract some info and create one
            StringBuilder serverVersionInfo = new StringBuilder();

            // first, get the server version info (by asking our server for the info)
            CoreServerMBean coreServer = LookupUtil.getCoreServer();
            serverVersionInfo.append("rhq-server.version=").append(coreServer.getVersion()).append('\n');
            serverVersionInfo.append("rhq-server.build-number=").append(coreServer.getBuildNumber()).append('\n');

            // calculate the MD5 of the agent update binary file
            File binaryFile = getAgentUpdateBinaryFile();
            String md5Property = "rhq-agent.latest.md5=" + MD5Generator.getDigestString(binaryFile) + '\n';

            // second, get the agent version info (by peeking into the agent update binary jar)
            JarFile binaryJarFile = new JarFile(binaryFile);
            JarEntry binaryJarFileEntry = binaryJarFile.getJarEntry("rhq-agent-update-version.properties");
            InputStream binaryJarFileEntryStream = binaryJarFile.getInputStream(binaryJarFileEntry);

            // now write the server and agent version info in our internal version file our servlet will use
            FileOutputStream versionFileOutputStream = new FileOutputStream(versionFile);
            try {
                versionFileOutputStream.write(serverVersionInfo.toString().getBytes());
                versionFileOutputStream.write(md5Property.getBytes());
                StreamUtil.copy(binaryJarFileEntryStream, versionFileOutputStream, false);
            } finally {
                try {
                    versionFileOutputStream.close();
                } catch (Exception e) {
                }
                try {
                    binaryJarFileEntryStream.close();
                } catch (Exception e) {
                }
            }
        }

        return versionFile;
    }

    private File getAgentUpdateBinaryFile() throws Exception {
        File agentDownloadDir = getAgentDownloadDir();
        for (File file : agentDownloadDir.listFiles()) {
            if (file.getName().endsWith(".jar")) {
                return file;
            }
        }

        throw new FileNotFoundException("Missing agent update binary in [" + agentDownloadDir + "]");
    }

    private File getAgentDownloadDir() throws Exception {
        MBeanServer mbs = getMBeanServer();
        ObjectName name = ObjectNameFactory.create("jboss.system:type=ServerConfig");
        Object mbean = MBeanServerInvocationHandler.newProxyInstance(mbs, name, ServerConfig.class, false);
        File serverHomeDir = ((ServerConfig) mbean).getServerHomeDir();
        File agentDownloadDir = new File(serverHomeDir, "deploy/rhq.ear/rhq-downloads/rhq-agent");
        if (!agentDownloadDir.exists()) {
            throw new FileNotFoundException("Missing agent downloads directory at [" + agentDownloadDir + "]");
        }
        return agentDownloadDir;
    }

    private MBeanServer getMBeanServer() {
        return MBeanServerLocator.locateJBoss();
    }
}