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
package org.rhq.core.pc;

/**
 * An interface that all plugin container services and managers will implement. It mainly is to support the lifecycle of
 * the container services so the {@link PluginContainer} can start and stop them when it, itself, starts and stops.
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public interface ContainerService {
    /**
     * Initializes the container service which will effectively tell the service to start doing its work.
     * Implementations of this interface can be assured that prior to this method being called, a non-<code>null</code>
     * configuration will be set via a call to {@link #setConfiguration(PluginContainerConfiguration)}.
     */
    void initialize();

    /**
     * Stops the container service which effectively releases all runtime resources such as running threads.
     */
    void shutdown();

    /**
     * Informs the container service how it should be configured by providing the full plugin container configuration.
     * The plugin container will ensure it passes in a non-<code>null</code> configuration object so implementations of
     * this interface should never have to worry about a <code>null</code> <code>configuration</code> parameter value.
     *
     * @param configuration
     */
    void setConfiguration(PluginContainerConfiguration configuration);
}