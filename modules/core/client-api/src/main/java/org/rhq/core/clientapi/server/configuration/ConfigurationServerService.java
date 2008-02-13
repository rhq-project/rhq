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
package org.rhq.core.clientapi.server.configuration;

import org.rhq.core.communications.command.annotation.Asynchronous;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.Property;

/**
 * Interface that allows an agent to provide information about a resource's configuration.
 */
public interface ConfigurationServerService {
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
    void completedConfigurationUpdate(ConfigurationUpdateResponse response);
}