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
     * This method will only return after the updates are complete.
     *
     * @param subject the authenticated user
     */
    void update(Subject subject) throws Exception;

    /**
     * Instructs all the running and connected agents to update their plugins after a given delay.
     * Be aware that in case there was a plugin update, the plugin containers of the agents will be restarted
     * and thus there will be a short period of time when the agent will not be collecting any metrics, etc.
     * <p/>
     * Note that by nature this is an asynchronous operation and the update is not finished by the time it returns.
     * You can use the provided handle to periodically query the
     * {@link #isPluginUpdateOnAgentsFinished(org.rhq.core.domain.auth.Subject, String)} method using the supplied
     * handle.
     *
     * @param subject the authenticated user
     * @param delayInMilliseconds the number of milliseconds to wait before running the update on the agents
     *
     * @return returns a handle that can be used to retrieve info about the execution of the update
     *
     * @see #isPluginUpdateOnAgentsFinished(org.rhq.core.domain.auth.Subject, String)
     */
    String schedulePluginUpdateOnAgents(Subject subject, long delayInMilliseconds) throws Exception;

    /**
     * Use this method to check whether given scheduled plugin update on agents finished.
     * <p/>
     * This will return false until the update on all agents has truly finished.
     * <p/>
     * Note that the schedule only updates the agents that are live at the time it is executed. The agents that will
     * come live later will only update their plugins once they start and only if they are configured to do so (which is
     * the default behavior). It is currently not possible to find out what versions of plugins agents use if they are
     * NOT auto-updating themselves.
     *
     * @param subject the authenticated user
     * @param handle the handle of the schedule
     * @return true if the scheduled plugin update finished on all live agents, false if it has not finished yet.
     *
     * @see #schedulePluginUpdateOnAgents(org.rhq.core.domain.auth.Subject, long)
     */
    boolean isPluginUpdateOnAgentsFinished(Subject subject, String handle);

    /**
     * Deploys a new agent plugin to RHQ asynchronously. Note that the
     * {@link #deployUsingContentHandle(org.rhq.core.domain.auth.Subject, String, String)} method is the preferred way
     * of doing this because it avoids having to keep the whole of the plugin jar in memory.
     * <p/>
     * In another words, use this method with caution because it may require a large amount of memory.
     * <p/>
     * This method returns after the plugin is deployed. Because there might be other plugins pending to be deployed
     * in the filesystem, this operation might result in more than just the single requested plugin being deployed.
     * This is why this method returns a list instead of a single plugin corresponding to the provided jar..
     *
     * @param subject the authenticated user
     * @param pluginJarName the name of the jar file which should be used to store the plugin on the filesystem
     * @param pluginJarBytes the bytes of the plugin jar file
     *
     * @throws Exception on error
     */
    List<Plugin> deployUsingBytes(Subject subject, String pluginJarName, byte[] pluginJarBytes) throws Exception;

    /**
     * Deploys a new agent plugin to RHQ asynchronously using the content handle pointing to a previously uploaded file.
     * <p/>
     * This method returns after the plugin is deployed. Because there might be other plugins pending to be deployed
     * in the filesystem, this operation might result in more than just the single requested plugin being deployed.
     * This is why this method returns a list instead of a single plugin corresponding to the provided jar..
     *
     * @param subject the authenticated user
     * @param pluginJarName the name of the jar file which should be used to store the plugin on the filesystem
     * @param handle the handle to the uploaded file
     *
     * @throws Exception on error
     * @see org.rhq.enterprise.server.content.ContentManagerRemote#createTemporaryContentHandle()
     * @see org.rhq.enterprise.server.content.ContentManagerRemote#uploadContentFragment(String, byte[], int, int)
     */
    List<Plugin> deployUsingContentHandle(Subject subject, String pluginJarName, String handle) throws Exception;

    PageList<Plugin> findPluginsByCriteria(Subject subject, PluginCriteria criteria);

    void enablePlugins(Subject subject, List<Integer> pluginIds) throws Exception;

    void disablePlugins(Subject subject, List<Integer> pluginIds) throws Exception;

    /**
     * This method puts the plugin into a <i>deleted</i> state and removes the plugin JAR file from the file system and
     * schedules its eventual deletion from the database, too.
     *
     * @param subject   The user performing the deletion
     * @param pluginIds The ids of the plugins to be deleted
     *
     * @throws Exception if an error occurs
     */
    void deletePlugins(Subject subject, List<Integer> pluginIds) throws Exception;
}
