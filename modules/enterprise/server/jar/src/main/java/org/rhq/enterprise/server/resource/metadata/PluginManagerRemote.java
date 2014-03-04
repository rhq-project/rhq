/*
 * RHQ Management Platform
 * Copyright (C) 2014 Red Hat, Inc.
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

package org.rhq.enterprise.server.resource.metadata;

import java.util.List;

import javax.ejb.Remote;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.PluginCriteria;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.util.PageList;

/**
 * All the methods on this EJB require the {@link org.rhq.core.domain.authz.Permission#MANAGE_SETTINGS} permission.
 *
 * @author Lukas Krejci
 * @since 4.11
 */
@Remote
public interface PluginManagerRemote {

    /**
     * Updates the plugins that might have been changed on the filesystem of the server.
     * <p/>
     * Note that this is done asynchronously and there is no good way of checking if and when the operation completed
     * apart from watching the server log.
     *
     * @param subject the authenticated user
     */
    void update(Subject subject) throws Exception;

    /**
     * Instructs all the running and connected agents to update their plugins after a given delay.
     * Be aware that in case there was a plugin update, the plugin containers of the agents will be restarted
     * and thus there will be a short period of time when the agent will not be collecting any metrics, etc.
     * <p/>
     * Note that this is done asynchronously and there is no good way of checking if and when the operation completed
     * apart from watching the server log.
     *
     * @param subject the authenticated user
     * @param delayInMilliseconds the number of milliseconds to wait before running the update on the agents
     */
    void schedulePluginUpdateOnAgents(Subject subject, long delayInMilliseconds) throws Exception;

    /**
     * Deploys a new agent plugin to RHQ asynchronously. Note that the
     * {@link #deployUsingContentHandle(org.rhq.core.domain.auth.Subject, String, String)} method is the preferred way
     * of doing this because it avoids having to keep the whole of the plugin jar in memory.
     * <p/>
     * In another words, use this method with caution because it may require a large amount of memory.
     * <p/>
     * Note that this is done asynchronously and there is no good way of checking if and when the operation completed
     * apart from watching the server log.
     *
     *
     * @param subject the authenticated user
     * @param pluginJarName the name of the jar file which should be used to store the plugin on the filesystem
     * @param pluginJarBytes the bytes of the plugin jar file
     *
     * @throws Exception on error
     */
    void deployUsingBytes(Subject subject, String pluginJarName, byte[] pluginJarBytes) throws Exception;

    /**
     * Deploys a new agent plugin to RHQ asynchronously using the content handle pointing to a previously uploaded file.
     * <p/>
     * Note that the deployment is done asynchronously and there is no good way checking if and when the operation
     * completed apart from watching the server log.
     *
     * @param subject the authenticated user
     * @param pluginJarName the name of the jar file which should be used to store the plugin on the filesystem
     * @param handle the handle to the uploaded file
     *
     * @throws Exception on error
     * @see org.rhq.enterprise.server.content.ContentManagerRemote#createTemporaryContentHandle()
     * @see org.rhq.enterprise.server.content.ContentManagerRemote#uploadContentFragment(String, byte[], int, int)
     */
    void deployUsingContentHandle(Subject subject, String pluginJarName, String handle) throws Exception;

    PageList<Plugin> findPluginsByCriteria(Subject subject, PluginCriteria criteria);

    void enablePlugins(Subject subject, List<Integer> pluginIds) throws Exception;

    void disablePlugins(Subject subject, List<Integer> pluginIds) throws Exception;

    /**
     * This method puts the plugin into a <i>deleted</i> state and removes the plugin JAR file from the file system. It
     * does not remove the plugin from the database. This method does not purge the plugin from the database in order
     * to support HA deployments. In a HA deployment, if server A handles the request to delete the plugin and if it
     * purges the plugin from the database, server B might see the plugin on the file system and not in the database.
     * Server B would then proceed to try and re-install the plugin, not knowing it was deleted.
     *
     * @param subject   The user performing the deletion
     * @param pluginIds The ids of the plugins to be deleted
     *
     * @throws Exception if an error occurs
     */
    void deletePlugins(Subject subject, List<Integer> pluginIds) throws Exception;

    /**
     * Schedules a plugin to be purged. Purging a plugin permanently deletes it from the database. Purging is done
     * asynchronously and will not happen until all resource types defined by the plugin have first been purged.
     * Plugins must first be deleted before they can be purged. A plugin is considered a candidate for being purged if its
     * status is set to <code>DELETED</code> and its <code>ctime</code> is set to {@link Plugin#PURGED}. This method
     * does not flip the status of the plugins to <code>DELETED</code> since it assumes that has already been done. It
     * only sets <code>ctime</code> to <code>PURGED</code>.
     *
     * @param subject   The user purging the plugin
     * @param pluginIds The ids of the plugins to be purged
     *
     * @throws Exception if an error occurs
     */
    void purgePlugins(Subject subject, List<Integer> pluginIds) throws Exception;
}
