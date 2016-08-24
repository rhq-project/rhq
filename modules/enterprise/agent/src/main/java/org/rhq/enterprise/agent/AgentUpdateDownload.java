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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import mazz.i18n.Logger;

import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;

/**
 * Downloads the agent update binary from the server, if one is available.
 *  
 * @author John Mazzitelli
 */
public class AgentUpdateDownload {
    private static final Logger LOG = AgentI18NFactory.getLogger(AgentUpdateDownload.class);

    private final AgentMain agent;
    private final AgentUpdateVersion agentUpdateVersion;
    private File downloadedFile;

    public AgentUpdateDownload(AgentMain agent) {
        this.agent = agent;
        this.agentUpdateVersion = new AgentUpdateVersion(agent);
        this.downloadedFile = null;
    }

    public AgentUpdateVersion getAgentUpdateVersion() {
        return agentUpdateVersion;
    }

    /**
     * Returns the URL that will be accessed to download the agent update binary.
     * 
     * @return download URL
     * 
     * @throws Exception if for some reason a valid URL could not be obtained
     */
    public URL getDownloadUrl() throws Exception {
        return new URL(this.agent.getConfiguration().getAgentUpdateDownloadUrl());
    }

    /**
     * If the agent update binary has been {@link #download() downloaded}, this will return
     * the file where the downloaded content was stored.
     * 
     * @return the file of the downloaded agent update binary; <code>null</code> if it was
     *         never downloaded
     */
    public File getAgentUpdateBinaryFile() {
        return this.downloadedFile;
    }

    /**
     * Returns the location on the local file system where any downloaded
     * files will be stored.
     * 
     * @return local file system location where the downloaded files are stored
     */
    public File getLocalDownloadDirectory() {
        String agentHome = this.agent.getAgentHomeDirectory();
        File dir = null;

        if (agentHome != null && agentHome.length() > 0) {
            dir = (new File(agentHome)).getParentFile();
        }

        if (dir == null) {
            dir = new File(System.getProperty("java.io.tmpdir"));
        }

        return dir;
    }

    /**
     * This will validate the MD5 of the {@link #getAgentUpdateBinaryFile() downloaded binary file}.
     * If it validates, this method returns normally, otherwise, an exception is thrown.
     * You must first download the file first before calling this method.
     * 
     * @throws Exception if the downloaded agent update binary file does not validate with the expected
     *                   MD5, or the agent update binary has not been downloaded yet.
     */
    public void validate() throws Exception {
        File fileToValidate = this.downloadedFile;
        if (fileToValidate == null || !fileToValidate.exists()) {
            throw new IllegalStateException(this.agent.getI18NMsg().getMsg(
                AgentI18NResourceKeys.UPDATE_DOWNLOAD_MD5_MISSING_FILE, fileToValidate));
        }

        AgentUpdateInformation info = this.agentUpdateVersion.getAgentUpdateInformation();
        String md5 = info.getUpdateMd5();
        if (!validateFile(this.downloadedFile, md5)) {
            // its invalid, move it out of the way so we can download a new one
            File invalidFile = new File(fileToValidate.getParentFile(), fileToValidate.getName() + ".invalid");
            invalidFile.delete(); // remove any old one that might be hanging around
            fileToValidate.renameTo(invalidFile);
            throw new IllegalStateException(this.agent.getI18NMsg().getMsg(
                AgentI18NResourceKeys.UPDATE_DOWNLOAD_MD5_INVALID, fileToValidate));

        }

        return; // it validates OK
    }

    /**
     * Downloads the agent update binary and stores it to the local file system.
     *  
     * @throws Exception if agent has disabled updates or it failed to download the update
     */
    public void download() throws Exception {

        if (!agent.getConfiguration().isAgentUpdateEnabled()) {
            throw new Exception(this.agent.getI18NMsg().getMsg(AgentI18NResourceKeys.UPDATE_DOWNLOAD_DISABLED_BY_AGENT));
        }

        // if need be, asks the server for the info - let this throw its own exceptions if it needs to
        AgentUpdateInformation info = this.agentUpdateVersion.getAgentUpdateInformation();

        URL url = null;
        boolean keep_going = true;
        File binaryFile = null;

        while (keep_going) {
            HttpURLConnection conn = null;
            InputStream inStream = null;

            try {
                // we only support http/s
                url = getDownloadUrl();
                LOG.info(AgentI18NResourceKeys.UPDATE_DOWNLOAD_RETRIEVAL, info, url);

                if (url.getProtocol().equals("https")) {
                    conn = openSecureConnection(url);
                } else {
                    conn = (HttpURLConnection) url.openConnection(); // we only support http(s), so this cast is OK
                }

                inStream = conn.getInputStream();

                // put the update content in the local file system
                // determine what the name should be of the agent update binary based on the header
                // Content-Disposition: attachment; filename=<filename.is.here>
                String fileName = conn.getHeaderField("Content-Disposition");

                if (fileName != null) {
                    int filenameIndex = fileName.indexOf("filename=");
                    if (filenameIndex > -1 && (filenameIndex + "filename=".length()) < fileName.length()) {
                        fileName = fileName.substring(filenameIndex + "filename=".length()).trim();
                    } else {
                        LOG.warn(AgentI18NResourceKeys.UPDATE_DOWNLOAD_BAD_NAME, fileName);
                        fileName = null;
                    }
                }

                if (fileName == null || fileName.length() == 0) {
                    // this should never happen, server should always give us the content-disposition
                    // but just in case the download URL is not pointing to a RHQ server and the URL it
                    // is pointing to doesn't give us that header, make a best guess at the name
                    fileName = "rhq-enterprise-agent-" + info.getUpdateVersion() + ".jar";
                    LOG.info(AgentI18NResourceKeys.UPDATE_DOWNLOAD_NO_NAME, fileName);
                }

                File dir = getLocalDownloadDirectory();
                binaryFile = new File(dir, fileName);

                // don't bother downloading if we already have it!
                if (validateFile(binaryFile, info.getUpdateMd5())) {
                    LOG.debug(AgentI18NResourceKeys.UPDATE_DOWNLOAD_ALREADY_HAVE_IT, binaryFile);
                    // we end up closing the stream before reading the data which causes an ugly error in the server log
                    // should we consider doing a HTTP HEAD request first?
                    keep_going = false;
                    break;
                }

                // slurp the entire agent update binary content and store it to our file
                binaryFile.delete();
                FileOutputStream fos = new FileOutputStream(binaryFile);
                StreamUtil.copy(inStream, fos, true);
                inStream = null;
                keep_going = false;
            } catch (Exception e) {
                if (conn != null) {
                    int responseCode = 0;
                    try {
                        responseCode = conn.getResponseCode();
                    } catch (Exception ignore) {
                    }
                    if (responseCode == HttpURLConnection.HTTP_UNAVAILABLE) {
                        // server is overloaded with other agents downloading, we must wait
                        LOG.info(AgentI18NResourceKeys.UPDATE_DOWNLOAD_UNAVAILABLE, info, url);
                        Thread.sleep(getRetryAfter(conn)); // sleep alittle bit to give the server some time (allow us to be interrupted!)
                        keep_going = true;
                    } else if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                        // server has disabled agent updates
                        Exception e1 = new Exception(this.agent.getI18NMsg().getMsg(
                            AgentI18NResourceKeys.UPDATE_DOWNLOAD_DISABLED_BY_SERVER, url), e);
                        LOG.warn(AgentI18NResourceKeys.UPDATE_DOWNLOAD_FAILURE, url, ThrowableUtil.getAllMessages(e1));
                        throw e1;
                    } else {
                        // some unexpected error occurred
                        LOG.warn(AgentI18NResourceKeys.UPDATE_DOWNLOAD_FAILURE, url, ThrowableUtil.getAllMessages(e));
                        throw e;
                    }
                } else {
                    LOG.warn(AgentI18NResourceKeys.UPDATE_DOWNLOAD_FAILURE, url, ThrowableUtil.getAllMessages(e));
                    throw e;
                }
            } finally {
                if (inStream != null) {
                    try {
                        inStream.close();
                    } catch (Exception e) {
                    }
                }
                if (conn != null) {
                    try {
                        conn.disconnect();
                    } catch (Exception e) {
                    }
                }
            }
        }

        // we only ever get here when we are successful
        LOG.info(AgentI18NResourceKeys.UPDATE_DOWNLOAD_DONE, info, url, binaryFile);
        this.downloadedFile = binaryFile;
        return;
    }

    /**
     * Gets the "Retry-After" header and returns its value as a long to indicate how long
     * we should wait before retrying. If can't get the header, a default time interval will be returned.
     * 
     * @param conn the connection where the header can be found
     * 
     * @return the header value, as a long
     */
    private long getRetryAfter(HttpURLConnection conn) {
        try {
            // get the header - by spec, it must be in seconds
            int retryAfter = conn.getHeaderFieldInt("Retry-After", 30);
            return 1000L * retryAfter;
        } catch (Exception e) {
            return 30000L;
        }
    }

    /**
     * This will validate the MD5 of the given file.
     * 
     * @return <code>true</code> if the file's MD5 matches the given MD5, <code>false</code> otherwise
     */
    private boolean validateFile(File file, String md5) {
        try {
            String filemd5 = MessageDigestGenerator.getDigestString(file);
            return (filemd5 != null && filemd5.equals(md5));
        } catch (Exception e) {
            return false;
        }
    }

    private HttpsURLConnection openSecureConnection(URL url) throws Exception {
        AgentConfiguration config = this.agent.getConfiguration();
        SecureConnectorFactory secureConnectorFactory = new SecureConnectorFactory();
        SecureConnector secureConnector = secureConnectorFactory.getInstanceWithAgentConfiguration(
                config, this.agent.getAgentHomeDirectory());
        return secureConnector.openSecureConnection(url);
    }
}
