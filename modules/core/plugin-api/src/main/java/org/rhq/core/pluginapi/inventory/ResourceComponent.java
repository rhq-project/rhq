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
package org.rhq.core.pluginapi.inventory;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.content.ContentFacet;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;

/**
 * The plugin component that wraps a {@link Resource}. This component allows you to work with the actual resource, such
 * as determining its availability and setting its configuration, among other things. Resource components have a
 * lifecycle controlled by the plugin container - see {@link #start(ResourceContext)} and {@link #stop()}.
 *
 * <p>Implementations of this interface can also implement one or more of the different functionality facets which will
 * allow the component to support different features such as measureability, configurability and others. These
 * functionality facets are:</p>
 *
 * <ul>
 *   <li>{@link MeasurementFacet}</li>
 *   <li>{@link ConfigurationFacet}</li>
 *   <li>{@link OperationFacet}</li>
 *   <li>{@link DeleteResourceFacet}</li>
 *   <li>{@link CreateChildResourceFacet}</li>
 *   <li>{@link ContentFacet}</li>
 * </ul>
 *
 * @param <T> the parent resource component type for this component. This means you can nest a hierarchy of resource
 *            components that mimic the resource type hierarchy as defined in a plugin deployment descriptor.
 */
public interface ResourceComponent<T extends ResourceComponent> {
    /**
     * Initializes the resource component, possibly connecting to the managed resource itself. The implementation can be
     * custom to the plugin or resource, but the idea is that a resource component can create a single connection to
     * their resource here, and maintain that connection throughout the life of the component (i.e. until the time when
     * {@link #stop()} is called).
     *
     * <p>This method typically examines the plugin configuration from {@link ResourceContext#getPluginConfiguration()}
     * and uses that information to connect to the managed resource. If this method finds that the plugin configuration
     * is invalid which causes the connection to the managed resource to fail, then this method should throw
     * {@link InvalidPluginConfigurationException}. This exception typically should not be thrown if the connection
     * failed for some reason other than an invalid plugin configuration (e.g. in the case the managed resource is
     * simply not running now) because usually those conditions can be tracked as part of the resource's
     * {@link #getAvailability() availability} data.</p>
     *
     * <p>Note that this method does <b>not</b> imply that the actual managed resource should be started; this only
     * starts the plugin's resource component. To start the actual resource, plugins must utilize the
     * {@link OperationFacet} facet to provide an operation to start it.</p>
     *
     * @param  context the context for this resource which includes information about the resource
     *
     * @throws InvalidPluginConfigurationException if the resource component failed to start because it could not
     *                                             connect to the resource due to a bad plugin configuration
     * @throws Exception                           any other exception that causes the component to fail to start
     */
    void start(ResourceContext<T> context) throws InvalidPluginConfigurationException, Exception;

    /**
     * Stops the resource component, which usually indicates the plugin container itself is shutting down. This method
     * is used to allow the component to disconnect from its managed resource and to clean up anything else as
     * appropriate.
     *
     * <p>Note that this does <b>not</b> imply that the actual managed resource should be stopped; this only stops the
     * plugin's resource component. To stop the actual resource, plugins must utilize the {@link OperationFacet} facet
     * to provide an operation to stop it.</p>
     */
    void stop();

    /**
     * The plugin container will occasionally call this method at the server level to see if the server is available.
     * This method is intended to attempt a remote connection to the resource. When a sever is in the down state, no
     * other component collections will go through (TODO GH: None?, what about offline config). This also shuts down
     * access at the child level so if a JBoss instance is down, for example, we won't try to collect metrics on an EJB
     * running inside it. Availability for all child resources would automatically be set to
     * {@link AvailabilityType#DOWN down}.
     *
     * @return {@link AvailabilityType#UP} if the resource can be accessed; otherwise {@link AvailabilityType#DOWN}
     */
    AvailabilityType getAvailability();
}