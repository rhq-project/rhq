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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mazz.i18n.Logger;

import org.rhq.core.clientapi.server.core.CoreServerService;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.util.MD5Generator;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;
import org.rhq.enterprise.communications.command.client.RemoteIOException;

/**
 * This object's job is to update any and all plugin jars that need to be updated. If this object determines that a
 * more recent plugin jar exists, this will retrieve it and overwrite the current plugin jar with that latest plugin
 * jar. This object can also be used if you just want to get information on the current set of plugins - see
 * {@link #getCurrentPluginFiles()}.
 *
 * @author John Mazzitelli
 * @author Ian Springer
 */
public class PluginUpdate {
    private static final Logger LOG = AgentI18NFactory.getLogger(PluginUpdate.class);

    private static final String MARKER_FILENAME = ".updatelock";

    private final CoreServerService coreServerService;
    private final PluginContainerConfiguration config;

    /**
     * All {@link PluginUpdate} objects know if they are currently updating plugins given a specific <code>
     * config</code>. Call this static method to ask if any plugin update object is currently updating plugins with the
     * given configuration
     *
     * @param  config used to determine where the plugins are being updated
     *
     * @return <code>true</code> if a plugin updater object is currently updating plugin; <code>false</code> if all
     *         plugins are up-to-date and nothing is being updated anymore.
     */
    public static boolean isCurrentlyUpdating(PluginContainerConfiguration config) {
        File marker = new File(config.getPluginDirectory(), MARKER_FILENAME);
        return marker.exists();
    }

    /**
     * Blocks the calling thread for a maximum of the given amount of milliseconds timeout waiting for a plugin update
     * to completely. This will return sooner if the update finishes early or if there is no update currently happening.
     *
     * @param  config  used to determine where the plugins are being updated
     * @param  timeout max milliseconds to wait
     *
     * @return <code>true</code> if a plugin updater object is currently updating plugin and this method timed out;
     *         <code>false</code> if all plugins are up-to-date and nothing is being updated anymore.
     *
     * @throws InterruptedException if thread was interrupted while waiting
     */
    public static boolean waitForUpdateToComplete(PluginContainerConfiguration config, long timeout)
        throws InterruptedException {
        long time_limit = System.currentTimeMillis() + timeout;
        boolean currently_updating = true; // for us to sleep at least an initial amount before checking the first time

        while (currently_updating && (time_limit > System.currentTimeMillis())) {
            Thread.sleep(2000L);
            currently_updating = isCurrentlyUpdating(config);
        }

        return currently_updating;
    }

    /**
     * Constructor for {@link PluginUpdate}. You can pass in a <code>null</code> <code>core_server_service</code> if you
     * only plan to use this object to obtain information on the currently installed plugins and not actually update
     * them.
     *
     * @param core_server_service the server that is to be used to retrieve the latest plugins
     * @param config              the plugin container configuration used to find what current plugins exist locally
     */
    public PluginUpdate(CoreServerService core_server_service, PluginContainerConfiguration config) {
        this.coreServerService = core_server_service;
        this.config = config;
    }

    /**
     * Constructor for {@link PluginUpdate} that can only be used to get info on current plugins; calling
     * {@link #updatePlugins()} on the object will fail. you can only call {@link #getCurrentPluginFiles()}.
     *
     * @param config the plugin container configuration used to find what current plugins exist locally
     */
    public PluginUpdate(PluginContainerConfiguration config) {
        this(null, config);
    }

    /**
     * This will compare the current set of plugins that exist locally with the set of latest plugins that are available
     * from the core server service. If we need to upgrade to one or more latest plugins, this method will download them
     * and overwrite the old plugin jars they replace.
     *
     * <p>You can only call this method if a valid, non-<code>null</code> {@link CoreServerService} implementation was
     * given to this object's constructor.</p>
     *
     * @return a list of plugins that have been updated (empty list if we already had all the latest plugins)
     *
     * @throws Exception if failed to determine our current set of plugins due to the inability to calculate their MD5
     *                   hashcodes or failed to download a plugin
     */
    public List<Plugin> updatePlugins() throws Exception {
        LOG.debug(AgentI18NResourceKeys.UPDATING_PLUGINS);

        List<Plugin> updated_plugins = new ArrayList<Plugin>();

        createMarkerFile();

        try {
            // find out what plugins we already have locally
            Map<String, Plugin> current_plugins = getCurrentPlugins();

            // find out what the latest plugins are available to us
            List<Plugin> latest_plugins = coreServerService.getLatestPlugins();
            Map<String, Plugin> latest_plugins_map = new HashMap<String, Plugin>(latest_plugins.size());

            // determine if we need to upgrade any of our current plugins to the latest versions
            for (Plugin latest_plugin : latest_plugins) {
                String plugin_filename = latest_plugin.getPath();
                latest_plugins_map.put(plugin_filename, latest_plugin);
                Plugin current_plugin = current_plugins.get(plugin_filename);

                if (current_plugin == null) {
                    updated_plugins.add(latest_plugin); // we don't have any version of this plugin, we'll need to get it
                    LOG.debug(AgentI18NResourceKeys.NEED_MISSING_PLUGIN, plugin_filename);
                } else {
                    String latest_md5 = latest_plugin.getMD5();
                    String current_md5 = current_plugin.getMD5();

                    if (!current_md5.equals(latest_md5)) {
                        updated_plugins.add(latest_plugin);
                        LOG.debug(AgentI18NResourceKeys.PLUGIN_NEEDS_TO_BE_UPDATED, plugin_filename, current_md5,
                            latest_md5);
                    } else {
                        LOG.debug(AgentI18NResourceKeys.PLUGIN_ALREADY_AT_LATEST, plugin_filename);
                    }
                }
            }

            deleteIllegitimatePlugins(current_plugins, latest_plugins_map);

            // Let's go ahead and download all the plugins that we need.
            // Try to update all plugins, even if one or more fails to update. At the end,
            // if an exception was thrown, we'll rethrow it but only after all update attempts were made
            Exception last_error = null;

            for (Plugin updated_plugin : updated_plugins) {
                try {
                    downloadPluginWithRetries(updated_plugin); // tries our very best to get it
                } catch (Exception e) {
                    last_error = e;
                }
            }

            if (last_error != null) {
                throw last_error;
            }
        } finally {
            deleteMarkerFile();
        }

        LOG.info(AgentI18NResourceKeys.UPDATING_PLUGINS_COMPLETE);

        return updated_plugins;
    }

    /**
     * Returns the list of all the plugin archive files. These are the current set of plugins installed locally.
     *
     * @return the current list of locally installed plugin archive files
     */
    public List<File> getCurrentPluginFiles() {
        List<File> current_plugins = new ArrayList<File>();

        File plugin_dir = config.getPluginDirectory();
        File[] plugin_files = plugin_dir.listFiles();
        for (File plugin_file : plugin_files) {
            if (plugin_file.getName().endsWith(".jar")) {
                current_plugins.add(plugin_file);
            }
        }

        return current_plugins;
    }

    /**
     * This method will perform multiple attempts to try to get the plugin successfully.
     * The purpose of this method is to try our very best to get the plugin, even if it means
     * retrying several times to download it (i.e. this method tries to never throw an exception
     * if it can help it).  This is to prevent the case when lots of agents start hitting a server
     * at the same time which causes an outage that forces our agent to failover to another
     * server in the middle of streaming the plugin.  If a failover occurs while in the middle
     * of streaming the plugin, the download will fail.  When this happens, this method will simply
     * attempt to download the plugin again (this time, hopefully, we will remain connected to the
     * new server and the download will succeed).
     * 
     * @param plugin the plugin to download
     * 
     * @throws Exception if, despite our best efforts, the plugin could not be downloaded
     */
    private void downloadPluginWithRetries(Plugin plugin) throws Exception {
        LOG.info(AgentI18NResourceKeys.DOWNLOADING_PLUGIN, plugin.getPath());
        int attempt = 0;
        boolean keep_trying = true;
        while (keep_trying) {
            try {
                attempt++;
                getPluginArchive(plugin);
                keep_trying = false;
            } catch (Exception e) {
                // This error might be because the server is so loaded down with agent downloads
                // that a problem occurred on the server that caused us to failover.  To help spread
                // the load, let's sleep a random amount of time between 10s and 70s.
                // Note that we always retry at least 3 times - after that, we only retry if it looks
                // like we are getting remote IO exceptions (which happens when our remote streaming fails).
                // To make sure the agent never hangs indefinitely, we'll never retry more than 10 times.
                long sleep = ((long) (Math.random() * 60000L)) + 10000L;
                String errors = ThrowableUtil.getAllMessages(e);
                if ((attempt < 3 || errors.contains(RemoteIOException.class.getName())) && attempt < 10) {
                    LOG.warn(AgentI18NResourceKeys.DOWNLOAD_PLUGIN_FAILURE_WILL_RETRY, plugin.getPath(), attempt,
                        sleep, errors);
                    try {
                        Thread.sleep(sleep);
                    } catch (Exception e2) {
                    }
                } else {
                    LOG.warn(AgentI18NResourceKeys.DOWNLOAD_PLUGIN_FAILURE_WILL_NOT_RETRY, plugin.getPath(), attempt,
                        errors);
                    throw e; // abort! no more retries - we tried our best but failed to download the plugin
                }
            }
        }
        LOG.info(AgentI18NResourceKeys.DOWNLOADING_PLUGIN_COMPLETE, plugin.getPath());
    }

    /**
     * Retrieves a plugin archive and overwrites any older, existing plugin archive.
     *
     * @param  plugin_to_get the plugin to retrieve
     *
     * @throws Exception if failed to download the plugin
     */
    private void getPluginArchive(Plugin plugin_to_get) throws Exception {
        File plugin_dir = config.getPluginDirectory();
        String new_plugin_filename = plugin_to_get.getPath();
        File old_plugin = new File(plugin_dir, new_plugin_filename);
        File old_plugin_backup = null;

        // for error recovery purposes, let's backup our old plugin, if one exists
        if (old_plugin.exists()) {
            old_plugin_backup = new File(plugin_dir, new_plugin_filename + ".OLD");
            old_plugin_backup.delete(); // in case an old backup is for some reason still here, get rid of it
            boolean renamed = old_plugin.renameTo(old_plugin_backup);

            // note that we don't fail if we can't backup the old one, but we will log it
            if (!renamed) {
                LOG.warn(AgentI18NResourceKeys.PLUGIN_BACKUP_FAILURE, old_plugin, old_plugin_backup);
            }
        }

        // now let's download the latest plugin
        File new_plugin = new File(plugin_dir, new_plugin_filename);
        FileOutputStream new_plugin_outstream = null;
        InputStream server_plugin_instream;

        try {
            new_plugin_outstream = new FileOutputStream(new_plugin, false);
            server_plugin_instream = coreServerService.getPluginArchive(new_plugin_filename);
            StreamUtil.copy(server_plugin_instream, new_plugin_outstream, true);

            // we've successfully downloaded the latest plugin, so delete our backup of the old plugin
            if (old_plugin_backup != null) {
                old_plugin_backup.delete();
            }
        } catch (Exception e) {
            // we are going to rethrow this exception, but first let's clean up and try to restore the old plugin

            LOG.error(e, AgentI18NResourceKeys.DOWNLOAD_PLUGIN_FAILURE, plugin_to_get.getPath());

            if (new_plugin_outstream != null) {
                try {
                    new_plugin_outstream.close();
                } catch (Exception ignore) {
                } finally {
                    new_plugin.delete();
                }
            }

            if (old_plugin_backup != null) {
                boolean renamed = old_plugin_backup.renameTo(new_plugin);
                if (!renamed) {
                    LOG.error(AgentI18NResourceKeys.PLUGIN_BACKUP_RESTORE_FAILURE, old_plugin_backup, new_plugin);
                }
            }

            throw e;
        }

        return;
    }

    /**
     * Returns the map of plugins that are currently installed locally, where the map is keyed on the plugin filename.
     *
     * @return list of known plugins that are currently installed locally, keyed on plugin jar filename
     *
     * @throws IOException if failed to read a plugin file for the purposes of generating its MD5
     */
    private Map<String, Plugin> getCurrentPlugins() throws IOException {
        Map<String, Plugin> plugins = new HashMap<String, Plugin>();
        File plugin_dir = config.getPluginDirectory();
        File[] plugin_files = plugin_dir.listFiles();

        for (File plugin_file : plugin_files) {
            String plugin_filename = plugin_file.getName();
            if (plugin_filename.endsWith(".jar")) {
                Plugin cur_plugin = new Plugin(plugin_filename, plugin_filename);
                cur_plugin.setMD5(MD5Generator.getDigestString(plugin_file));
                plugins.put(plugin_filename, cur_plugin);
            }
        }

        return plugins;
    }

    private void createMarkerFile() {
        File marker = null;
        try {
            marker = new File(config.getPluginDirectory(), MARKER_FILENAME);

            // shouldn't exist, but if it does, oh well, just reuse it
            if (!marker.exists()) {
                new FileOutputStream(marker).close();
            }
        } catch (Exception e) {
            LOG.warn(AgentI18NResourceKeys.UPDATING_PLUGINS_MARKER_CREATE_FAILURE, marker, e);
        }

        return;
    }

    private void deleteMarkerFile() {
        File marker = new File(config.getPluginDirectory(), MARKER_FILENAME);

        // it should exist, but if it doesn't oh well, just skip trying to delete it
        if (marker.exists()) {
            if (!marker.delete()) {
                LOG.warn(AgentI18NResourceKeys.UPDATING_PLUGINS_MARKER_DELETE_FAILURE, marker);
            }
        }

        return;
    }

    private void deleteIllegitimatePlugins(Map<String, Plugin> current_plugins, Map<String, Plugin> latest_plugins_map) {
        for (Plugin current_plugin : current_plugins.values()) {
            if (!latest_plugins_map.containsKey(current_plugin.getPath())) {
                File plugin_dir = this.config.getPluginDirectory();
                String plugin_filename = current_plugin.getPath();
                File plugin = new File(plugin_dir, plugin_filename);
                if (plugin.exists()) {
                    File plugin_backup = new File(plugin_dir, plugin_filename + ".REJECTED");
                    LOG.warn(AgentI18NResourceKeys.PLUGIN_NOT_ON_SERVER, plugin_filename, plugin_backup.getName());
                    try {
                        plugin_backup.delete(); // in case an old backup is for some reason still here, get rid of it
                        boolean renamed = plugin.renameTo(plugin_backup);
                        // note that we don't fail if we can't backup the plugin, but we will log it
                        if (!renamed) {
                            LOG.error(AgentI18NResourceKeys.PLUGIN_RENAME_FAILED, plugin_filename, plugin_backup
                                .getName());
                        }
                    } catch (RuntimeException e) {
                        LOG.error(e, AgentI18NResourceKeys.PLUGIN_RENAME_FAILED, plugin_filename, plugin_backup
                            .getName());
                    }
                }
            }
        }
    }
}