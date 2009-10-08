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
package org.rhq.core.pluginapi.configuration;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.Property;

/**
 * Components that implement this facet expose the ability to configure resources.
 *
 * @author Jason Dobies
 */
public interface ConfigurationFacet {
    /**
     * Returns the current values for the specified resource. Note that the returns configuration is the configuration
     * settings for the managed resource itself, not to be confused with the <i>plugin configuration</i> which is used
     * by the plugin code to connect to the managed resource.
     *
     * @return the current configuration of the managed resource
     *
     * @throws Exception if failed to obtain the current configuration from the resource
     */
    Configuration loadResourceConfiguration() throws Exception;

    /**
     * Configures the specified resource with the values found in the {@link Configuration} object within the given
     * <code>report</code>. This configuration contains the entire configuration for the resource and not simply a
     * series of changes to the existing values. The method implementation <b>must not</b> change the property values;
     * the Configuration must either be used as-is or a failure must be indicated with error messages attached to the
     * invalid property values indicating why they were invalid.
     *
     * <p>Implementations must set the report's final
     * {@link ConfigurationUpdateReport#setStatus(ConfigurationUpdateStatus) status} (for example,
     * {@link ConfigurationUpdateStatus#SUCCESS} or {@link ConfigurationUpdateStatus#FAILURE}). If the method leaves the
     * status to as <code>null</code> or {@link ConfigurationUpdateStatus#INPROGRESS}, the caller should assume the
     * method somehow aborted the update and will consider it a failure. If an error occurred, the implementation should
     * set the request's {@link ConfigurationUpdateReport#getErrorMessage() error message} to a non-<code>null</code>
     * value that will describe an overall error message and it should set the
     * {@link ConfigurationUpdateReport#setConfiguration(Configuration) configuration} with all the properties in them
     * but that contain {@link Property#getErrorMessage() property error messages} that indicate which properties failed
     * to get updated and why. This allows you to indicate all the errors that occurred, in case more than one property
     * was invalid or could not be updated.</p>
     *
     * <p>Note that this method should not throw any exceptions; instead, all error conditions should be indicated in
     * the report.</p>
     *
     * @param report
     */
    void updateResourceConfiguration(ConfigurationUpdateReport report);
}