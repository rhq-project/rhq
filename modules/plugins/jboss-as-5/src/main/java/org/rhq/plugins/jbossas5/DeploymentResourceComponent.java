 /*
  * Jopr Management Platform
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
package org.rhq.plugins.jbossas5;

import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.plugins.jbossas5.factory.ProfileServiceFactory;
import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.managed.api.ManagedDeployment;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Component class for deployable resources like ear/war/jar/sar.
 *
 * @author Mark Spritzler
 */
public class DeploymentResourceComponent
        extends ContentDeploymentComponent
        implements ResourceComponent {
    private final Log LOG = LogFactory.getLog(DeploymentResourceComponent.class);

    //private final String DEPLOYMENT_PROPERTY_NAME = "deploymentName";

    private ResourceContext resourceContext;

    // ResourceComponent implementation
    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = resourceContext;
    }

    public void stop() {
        this.resourceContext = null;
    }

    public AvailabilityType getAvailability() {
        // TODO (ips, 11/10/08): Verify this is the correct way to check availablity.
        try {
            return (getManagedDeployment() != null) ? AvailabilityType.UP : AvailabilityType.DOWN;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ManagedDeployment getManagedDeployment() throws Exception {
        String resourceKey = this.resourceContext.getResourceKey();
        ManagementView managementView = ProfileServiceFactory.getCurrentProfileView();
        return managementView.getDeployment(resourceKey, ManagedDeployment.DeploymentPhase.APPLICATION);
    }
}
