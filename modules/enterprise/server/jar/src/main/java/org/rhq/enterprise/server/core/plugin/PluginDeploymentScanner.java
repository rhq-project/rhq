/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.enterprise.server.core.plugin;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.descriptor.AgentPluginDescriptorUtil;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.file.FileUtil;
import org.rhq.enterprise.server.util.LoggingThreadFactory;
import org.rhq.enterprise.server.xmlschema.ServerPluginDescriptorUtil;

/**
 * This looks at both the file system and the database for new agent and server plugins.
 *
 * @author John Mazzitelli
 */
public class PluginDeploymentScanner implements PluginDeploymentScannerMBean {

    private Log log = LogFactory.getLog(PluginDeploymentScanner.class);

    /** time, in millis, between each scans */
    private long scanPeriod = 300000L;

    /** handles the scheduled scanning */
    private ScheduledExecutorService poller;

    /** where the server-side plugins live */
    private File serverPluginDir = null;

    /** where the user can copy agent or server plugins */
    private File userPluginDir = null;

    /** what looks for new or changed agent plugins */
    private AgentPluginScanner agentPluginScanner = new AgentPluginScanner();

    public Long getScanPeriod() {
        return Long.valueOf(this.scanPeriod);
    }

    public void setScanPeriod(Long ms) {
        if (ms != null) {
            this.scanPeriod = ms.longValue();
        } else {
            this.scanPeriod = 300000L;
        }
    }

    public File getUserPluginDir() {
        return this.userPluginDir;
    }

    public void setUserPluginDir(File dir) {
        this.userPluginDir = dir;
    }

    public File getServerPluginDir() {
        return this.serverPluginDir;
    }

    public void setServerPluginDir(File dir) {
        this.serverPluginDir = dir;
    }

    public File getAgentPluginDir() {
        return this.agentPluginScanner.getAgentPluginDeployer().getPluginDir();
    }

    public void setAgentPluginDir(File dir) {
        this.agentPluginScanner.getAgentPluginDeployer().setPluginDir(dir);
    }

    public File getLicenseFile() {
        return this.agentPluginScanner.getAgentPluginDeployer().getLicenseFile();
    }

    public void setLicenseFile(File file) {
        this.agentPluginScanner.getAgentPluginDeployer().setLicenseFile(file);
    }

    public void start() throws Exception {
        // This will check to see if there are any agent plugin records in the database
        // that do not have content associated with them and if so, will stream
        // the content from the file system to the database. This is needed only
        // in the case when this server has recently been upgraded from an old
        // version of the software that did not originally have content stored in the DB.
        // Once we do that, we can start the agent plugin deployer.
        this.agentPluginScanner.fixMissingAgentPluginContent();
        this.agentPluginScanner.getAgentPluginDeployer().start();

        shutdownPoller(); // paranoia - just in case somehow one is still running
        this.poller = Executors.newSingleThreadScheduledExecutor(new LoggingThreadFactory("PluginScanner", true));
        return;
    }

    public void stop() {
        this.agentPluginScanner.getAgentPluginDeployer().stop();
        shutdownPoller();
        return;
    }

    private void shutdownPoller() {
        if (this.poller != null) {
            this.poller.shutdownNow();
            this.poller = null;
        }
        return;
    }

    public void startDeployment() {
        // We are being called by the server's startup servlet which essentially informs us that
        // the server's internal EJB/SLSBs are ready and can be called. This means we are allowed to start.
        this.agentPluginScanner.getAgentPluginDeployer().startDeployment();

        // this is the runnable task that executes each scan period - it runs in our thread pool
        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    scanAndRegister();
                } catch (Throwable t) {
                    log.error("Scan failed. Cause: " + ThrowableUtil.getAllMessages(t));
                    if (log.isDebugEnabled()) {
                        log.debug("Scan failure stack trace follows:", t);
                    }
                }
            }
        };

        // do the initial scan now
        runnable.run();

        // schedule it to run periodically from here on out
        this.poller.scheduleWithFixedDelay(runnable, this.scanPeriod, this.scanPeriod, TimeUnit.MILLISECONDS);
        return;
    }

    public synchronized void scan() throws Exception {
        // The user directory is a simple location for the user to put all plugins in.
        // It makes it easy for the user to know where to put the plugins without
        // having to know the internal location for the real plugins under the ear.
        // Now we move the user's plugins to their real location in the ear.
        scanUserDirectory();

        // scan for agent plugins
        this.agentPluginScanner.agentPluginScan();

        return;
    }

    public synchronized void scanAndRegister() throws Exception {
        // do the scan first to find any new/updated plugins
        scan();

        // now tell the agent plugin scanner to register plugins that it determined needs to be registered
        this.agentPluginScanner.registerAgentPlugins();
    }

    /**
     * Take the plugins placed in the user directory, and copy them to their apprpriate places
     * in the server.
     */
    private void scanUserDirectory() {
        File userDir = getUserPluginDir();
        if (userDir == null || !userDir.isDirectory()) {
            return; // not configured for a user directory, just return immediately and do nothing
        }

        File[] listFiles = userDir.listFiles();
        if (listFiles == null || listFiles.length == 0) {
            return; // nothing to do
        }

        for (File file : listFiles) {
            File destinationDirectory;
            if (file.getName().endsWith(".jar")) {
                try {
                    AgentPluginDescriptorUtil.loadPluginDescriptorFromUrl(file.toURI().toURL());
                    destinationDirectory = getAgentPluginDir();
                } catch (Exception e) {
                    try {
                        ServerPluginDescriptorUtil.loadPluginDescriptorFromUrl(file.toURI().toURL());
                        destinationDirectory = getServerPluginDir();
                    } catch (Exception e1) {
                        // skip it, doesn't look like a valid plugin jar
                        log.warn("Does not look like [" + file.getAbsolutePath() + "] is a plugin jar - ignoring");
                        continue;
                    }
                }

                try {
                    String fileMd5 = MessageDigestGenerator.getDigestString(file);
                    File realPluginFile = new File(destinationDirectory, file.getName());
                    String realPluginFileMd5 = null;
                    if (realPluginFile.exists()) {
                        realPluginFileMd5 = MessageDigestGenerator.getDigestString(realPluginFile);
                    }
                    if (!fileMd5.equals(realPluginFileMd5)) {
                        if (file.lastModified() > realPluginFile.lastModified()) {
                            FileUtil.copyFile(file, realPluginFile);
                            realPluginFile.setLastModified(file.lastModified());
                            log.info("Found plugin jar at [" + file.getAbsolutePath() + "] and placed it at ["
                                + realPluginFile.getAbsolutePath() + "]");
                        }
                    }
                    boolean deleted = file.delete();
                    if (!deleted) {
                        log.info("The plugin jar found at[" + file.getAbsolutePath()
                            + "] has been processed and can be deleted. It failed to get deleted, "
                            + "so it may get processed again. You should delete it manually now.");
                    }
                } catch (Exception e) {
                    log.error("Failed to process plugin [" + file.getAbsolutePath() + "], ignoring it", e);
                }
            }
        }

        return;
    }
}
