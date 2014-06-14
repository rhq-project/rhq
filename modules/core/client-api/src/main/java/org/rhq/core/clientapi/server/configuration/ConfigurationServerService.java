 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
package org.rhq.core.clientapi.server.configuration;

import org.rhq.core.communications.command.annotation.Asynchronous;
import org.rhq.core.communications.command.annotation.LimitedConcurrency;
import org.rhq.core.domain.configuration.Configuration;

/**
 * Interface that allows an agent to provide information about a resource's configuration.
 */
public interface ConfigurationServerService {
    String CONCURRENCY_LIMIT_CONFIG_UPDATE = "rhq.server.concurrency-limit.configuration-update";

    /**
     * The agent will notify the server when a configuration update request has been completed by calling this method.
     *
     * <p>The completed request will contain the final {@link ConfigurationUpdateResponse#getStatus() status}(for
     * example, {@link ConfigurationUpdateStatus#SUCCESS} or {@link ConfigurationUpdateStatus#FAILURE}). If an error
     * occurred, the request's {@link ConfigurationUpdateResponse#getErrorMessage() error message} should be
     * non-<code>null</code> to describe an overall error message and it should have a Configuration with properties
     * that contain {@link Property#getErrorMessage() error messages} that indicate which properties failed to get
     * updated and why. This allows you to indicate all the errors that occurred, in case more than one property was
     * invalid or could not be updated.</p>
     *
     * <p>If the update was successful, the <code>completedRequest</code> object does not need to have a
     * non-<code>null</code> {@link ConfigurationUpdateResponse#getConfiguration()} (to avoid sending a duplicate
     * configuration back over the wire). Therefore, callers can
     * {@link ConfigurationUpdateResponse#setConfiguration(Configuration) set the configuration} to <code>null</code> if
     * the status was {@link ConfigurationUpdateStatus#SUCCESS}. When the status is successful, the <code>
     * response</code>'s configuration is actually ignored and the original configuration that was sent in the original
     * request will be assumed to have been used (this is why agents can just set the configuration to <code>null</code>
     * when successful - avoid sending it over the wire since this method doesn't even need it.</p>
     *
     * @param response information about the request that was completed (which may have succeeded or failed)
     */
    @Asynchronous(guaranteedDelivery = true)
    void completeConfigurationUpdate(ConfigurationUpdateResponse response);


    /**
     * This is for when the agent needs to notify the server that a new Resource configuration has been detected
     * on the agent side. This happens when a resource configuration is changed outside of this system and is
     * detected on its regularly scheduled checks for update.
     * @param resourceId the resourceId to update
     * @param resourceConfiguration the newly detected configuration
     */
    @Asynchronous(guaranteedDelivery = true)
    @LimitedConcurrency(CONCURRENCY_LIMIT_CONFIG_UPDATE)
    void persistUpdatedResourceConfiguration(int resourceId, Configuration resourceConfiguration);

    /**
     * This is for when the agent needs to notify the server that a new Plugin configuration has been
     * discovered on the agent side. This happens when resource discovery discovers a new version of a resource.
     * The properties set by discovery are merged into the existing config, and need to be updated
     * server-side.
     *
     * @param resourceId the resourceId to update
     * @param resourceConfiguration the newly detected configuration
     *
     * @return The persisted plugin configuration
     */
    @LimitedConcurrency(CONCURRENCY_LIMIT_CONFIG_UPDATE)
    Configuration persistUpdatedPluginConfiguration(int resourceId, Configuration pluginConfiguration);

}