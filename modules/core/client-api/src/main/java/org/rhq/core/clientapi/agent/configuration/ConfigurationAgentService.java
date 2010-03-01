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
package org.rhq.core.clientapi.agent.configuration;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.server.configuration.ConfigurationUpdateResponse;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationValidationException;

/**
 * Interface to the plugin container for triggering configuration operations on the plugin's resources.
 *
 * @author Jason Dobies
 * @author John Mazzitelli
 */
public interface ConfigurationAgentService {
    /**
     * Configures a resource with the new set of configuration values found in the {@link Configuration} object within
     * the given <code>request</code>. This configuration contains the entire configuration for the resource and not
     * simply a series of changes to the existing values. The plugin responsible for managing the given resource must
     * inform that resource that its configuration has changed to the given set of new values.
     *
     * <p>Note that this method should not throw any exceptions; instead, all error conditions should be indicated in
     * the response object when sent back to the caller.</p>
     *
     * @param request tells you the resource to configure and the full set of configuration values for the resource
     */
    void updateResourceConfiguration(ConfigurationUpdateRequest request);

    /**
     * Configures a resource with the new set of configuration values found in the {@link Configuration} object within
     * the given <code>request</code>. This configuration contains the entire configuration for the resource and not
     * simply a series of changes to the existing values. The plugin responsible for managing the given resource must
     * inform that resource that its configuration has changed to the given set of new values.
     *
     * <p>Note that this method should not throw any exceptions for configuration errors; instead, all error conditions
     * should be indicated in the response object when sent back to the caller. Any Runtime or Plugin Excpetions will
     * still be thrown</p>
     *
     * @param  request tells you the resource to configure and the full set of configuration values for the resource
     *
     * @return ConfigurationUpdateResponse response object with the updated values and also any specific errors on a
     *         configuration
     *
     * @throws PluginContainerException if update fails due to an unrecoverable error, then a PluginContainerException
     *                                  is thrown.
     */
    ConfigurationUpdateResponse executeUpdateResourceConfigurationImmediately(ConfigurationUpdateRequest request)
        throws PluginContainerException;

    /**
     * Loads the current configuration for the given resource. This returns the entire set of configuration values for
     * the resource.
     *
     * @param  resourceId id of the resource to load
     *
     * @return current values of the entire configuration for the resource
     *
     * @throws PluginContainerException if a runtime or plugin error occurs a PluginContainerException is thrown
     */
    Configuration loadResourceConfiguration(int resourceId) throws PluginContainerException;

    /**
     * If the <code>fromStructured</code> flag is <code>true</code>, then the structured configuration (i.e., <code>
     * Configuration.properties</code>) is merged into the latest raw configurations returned from the plugin. The
     * Configuration object returned will consist of the structured configuration along with the merged raw 
     * configurations. If <code>fromStructured</code> is <code>false</code>, then the raw configurations (i.e., <code>
     * Configuration.rawConfigurations</code>) is merged into the latest structured configurations returned from the
     * plugin. The returned Configuration will then consist of the raw configuration along with the merged structured
     * configuration.
     *
     * @param configuration The Configuration with the changes to be merged
     * @param resourceId The id of the resource to which the configuration belongs
     * @param fromStructured A flag that if <code>true</code> indicates the merge should be from structured to raw,
     * otherwise merge from raw to structured.
     * @return The <strong>merged</strong> configuration, where a merge consists of a refresh of the side being merged
     * followed by the values of the side merging from being applied to the side being merged into.
     * @throws PluginContainerException if a runtime or plugin error occurs.
     */
    Configuration merge(Configuration configuration, int resourceId, boolean fromStructured)
        throws PluginContainerException;

    /**
     * If the <code>fromStructured</code> flag is <code>true</code>, then the s
     * Configuration.properties</code>) is merged into the latest raw configura
     * Configuration object returned will consist of the structured configurati
     * configurations. If <code>fromStructured</code> is <code>false</code>, th
     * Configuration.rawConfigurations</code>) is merged into the latest struct
     * plugin. The returned Configuration will then consist of the raw configur
     * configuration.
     *
     * @param configuration The Configuration with the changes to be merged
     * @param resourceId The id of the resource to which the configuration belongs
     * @param A flag that if <code>true</code> indicates validate the Structured configuration
     * otherwisre, it validates the raw
     * 
     * @return If validate succeeds, it returns null, to avoid doing any unnecessary marshaling.
     *          If validation fails, it returns the configuration object with embedded messages
     * @throws PluginContainerException 
     * @throws PluginContainerException if a runtime or plugin error occurs.
     * @throws ConfigurationValidationException 
     */
    void validate(Configuration configuration, int resourceId, boolean isStructured)
        throws ConfigurationValidationException;

}