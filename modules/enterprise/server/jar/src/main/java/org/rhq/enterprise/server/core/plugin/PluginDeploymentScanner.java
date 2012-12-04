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
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.util.StringPropertyReplacer;

import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.clientapi.descriptor.AgentPluginDescriptorUtil;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.file.FileUtil;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.xmlschema.ServerPluginDescriptorUtil;

/**
 * This looks at both the file system and the database for new agent and server plugins.
 * This does not perform any polling - it only performs scanning on-demand. It provides
 * some configuration settings that a timer could use to determine when to periodically
 * scan (see {@link #getScanPeriod()} for example.
 *
 * @author John Mazzitelli
 */
public class PluginDeploymentScanner implements PluginDeploymentScannerMBean {

    private Log log = LogFactory.getLog(PluginDeploymentScanner.class);

    /** time, in millis, between each scans */
    private long scanPeriod = 300000L;

    /** where the user can copy agent or server plugins */
    private File userPluginDir = null;

    /** what looks for new or changed agent plugins */
    private AgentPluginScanner agentPluginScanner = new AgentPluginScanner();

    /** what looks for new or changed agent plugins */
    private ServerPluginScanner serverPluginScanner = new ServerPluginScanner();

    // https://issues.jboss.org/browse/AS7-5343 forces us to change the attribute values as Strings
    // we then must convert ourselves.

    //public Long getScanPeriod() {
    public String getScanPeriod() {
        //return this.scanPeriod;
        return Long.toString(this.scanPeriod);
    }

    //public void setScanPeriod(Long ms) {
    public void setScanPeriod(String ms) {
        if (ms != null) {
            //this.scanPeriod = ms;
            this.scanPeriod = Long.parseLong(StringPropertyReplacer.replaceProperties(ms));
        } else {
            this.scanPeriod = 300000L;
        }
    }

    //public File getUserPluginDir() {
    public String getUserPluginDir() {
        //return this.userPluginDir;
        return this.userPluginDir.getAbsolutePath();
    }

    //public void setUserPluginDir(File dir) {
    public void setUserPluginDir(String dir) {
        if (dir == null) {
            return;
        }
        //this.userPluginDir = dir;
        this.userPluginDir = new File(StringPropertyReplacer.replaceProperties(dir));
    }

    //public File getServerPluginDir() {
    public String getServerPluginDir() {
        //return this.serverPluginScanner.getServerPluginDir();
        return this.serverPluginScanner.getServerPluginDir().getAbsolutePath();
    }

    //public void setServerPluginDir(File dir) {
    public void setServerPluginDir(String dir) {
        if (dir == null) {
            return;
        }
        //this.serverPluginScanner.setServerPluginDir(dir);
        this.serverPluginScanner.setServerPluginDir(new File(StringPropertyReplacer.replaceProperties(dir)));
    }

    //public File getAgentPluginDir() {
    public String getAgentPluginDir() {
        //return this.agentPluginScanner.getAgentPluginDeployer().getPluginDir();
        return this.agentPluginScanner.getAgentPluginDeployer().getPluginDir().getAbsolutePath();
    }

    //public void setAgentPluginDir(File dir) {
    public void setAgentPluginDir(String dir) {
        if (dir == null) {
            return;
        }
        //this.agentPluginScanner.getAgentPluginDeployer().setPluginDir(dir);
        this.agentPluginScanner.getAgentPluginDeployer().setPluginDir(
            new File(StringPropertyReplacer.replaceProperties(dir)));
    }

    public PluginMetadataManager getPluginMetadataManager() {
        return this.agentPluginScanner.getAgentPluginDeployer().getPluginMetadataManager();
    }

    private File getUserPluginDirAsFile() {
        return this.userPluginDir;
    }

    private File getAgentPluginDirAsFile() {
        return this.agentPluginScanner.getAgentPluginDeployer().getPluginDir();
    }

    private File getServerPluginDirAsFile() {
        return this.serverPluginScanner.getServerPluginDir();
    }

    public void start() throws Exception {
        return;
    }

    public void stop() {
        this.agentPluginScanner.getAgentPluginDeployer().stop();
        return;
    }

    public void startDeployment() {
        // We are being called by the server's startup bean which essentially informs us that
        // the server's internal EJB/SLSBs are ready and can be called. This means we are allowed to start.
        // NOTE: Make sure we are called BEFORE the master plugin container is started!

        // setup our attributes - skip if the plugin dirs were already set (e.g. from test code)
        String upd = getUserPluginDir();
        String apd = getAgentPluginDir();
        String spd = getServerPluginDir();

        // don't look up the core server mbean if we don't need do
        if (upd == null || apd == null || spd == null) {
            File homeDir = LookupUtil.getCoreServer().getInstallDir();
            File earDir = LookupUtil.getCoreServer().getEarDeploymentDir();

            if (upd == null) {
                upd = new File(homeDir, "plugins").getAbsolutePath();
                setUserPluginDir(upd);
            }

            if (apd == null) {
                apd = new File(earDir, "rhq-downloads/rhq-plugins").getAbsolutePath();
                setAgentPluginDir(apd);
            }

            if (spd == null) {
                spd = new File(earDir, "rhq-serverplugins").getAbsolutePath();
                setServerPluginDir(spd);
            }
        }

        log.info("user plugin dir=" + upd);
        log.info("agent plugin dir=" + apd);
        log.info("server plugin dir=" + spd);

        // This will check to see if there are any agent plugin records in the database
        // that do not have content associated with them and if so, will stream
        // the content from the file system to the database. This is needed only
        // in the case when this server has recently been upgraded from an old
        // version of the software that did not originally have content stored in the DB.
        // Once we do that, we can start the agent plugin deployer.
        try {
            this.agentPluginScanner.fixMissingAgentPluginContent();
            this.agentPluginScanner.getAgentPluginDeployer().start();
        } catch (Exception e) {
            throw new RuntimeException("Cannot start plugin deployment scanner properly", e);
        }

        this.agentPluginScanner.getAgentPluginDeployer().startDeployment();

        // do the initial scan now
        try {
            scanAndRegister();
        } catch (Throwable t) {
            log.error("Scan failed. Cause: " + ThrowableUtil.getAllMessages(t));
            if (log.isDebugEnabled()) {
                log.debug("Scan failure stack trace follows:", t);
            }
        }

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

        // scan for server plugins
        this.serverPluginScanner.serverPluginScan();
        return;
    }

    public synchronized void scanAndRegister() throws Exception {
        // do the scan first to find any new/updated plugins
        scan();

        // now tell the agent plugin scanner to register plugins that it determined needs to be registered
        this.agentPluginScanner.registerAgentPlugins();

        // tell the server plugin scanner the same
        this.serverPluginScanner.registerServerPlugins();
    }

    /**
     * Take the plugins placed in the user directory, and copy them to their apprpriate places
     * in the server.
     */
    private void scanUserDirectory() {
        File userDir = getUserPluginDirAsFile(); // 
        if (userDir == null || !userDir.isDirectory()) {
            return; // not configured for a user directory, just return immediately and do nothing
        }

        File[] listFiles = userDir.listFiles();
        if (listFiles == null || listFiles.length == 0) {
            return; // nothing to do
        }
        for (File file : listFiles) {
            File destinationDirectory;
            boolean isJarLess = file.getName().endsWith("-rhq-plugin.xml");
            if (file.getName().endsWith(".jar") || isJarLess ) {
                try {
                    if (!isJarLess && null == AgentPluginDescriptorUtil.loadPluginDescriptorFromUrl(file.toURI().toURL())) {
                        throw new NullPointerException("no xml descriptor found in jar");
                    }
                    destinationDirectory = getAgentPluginDirAsFile(); //
                } catch (Exception e) {
                    try {
                        log.debug("[" + file.getAbsolutePath() + "] is not an agent plugin jar (Cause: "
                            + ThrowableUtil.getAllMessages(e) + "). Will see if its a server plugin jar");

                        if (null == ServerPluginDescriptorUtil.loadPluginDescriptorFromUrl(file.toURI().toURL())) {
                            throw new NullPointerException("no xml descriptor found in jar");
                        }
                        destinationDirectory = getServerPluginDirAsFile(); //
                    } catch (Exception e1) {
                        // skip it, doesn't look like a valid plugin jar
                        File fixmeFile = new File(file.getAbsolutePath() + ".fixme");
                        boolean renamed = file.renameTo(fixmeFile);
                        log.warn("Does not look like [" + (renamed ? fixmeFile : file).getAbsolutePath()
                            + "] is a plugin jar -(Cause: " + ThrowableUtil.getAllMessages(e1)
                            + "). It will be ignored. Please fix that file or remove it.");
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
                            boolean succeeded = realPluginFile.setLastModified(file.lastModified());
                            if (!succeeded) {
                                log.error("Failed to set mtime to [" + new Date(file.lastModified()) + "] on file ["
                                    + realPluginFile + "].");
                            }
                            String tmp;
                            if (!isJarLess)
                                tmp = "jar";
                            else
                                tmp = "descriptor";
                            log.info("Found plugin " + tmp + " at [" + file.getAbsolutePath() + "] and placed it at ["
                                + realPluginFile.getAbsolutePath() + "]");
                        }
                    }
                    else {
                        log.info("Found a plugin at [" + file.getAbsolutePath() + "], which is the same as the existing one. It will be ignored");
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
