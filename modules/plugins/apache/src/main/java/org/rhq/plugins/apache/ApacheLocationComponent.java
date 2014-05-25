/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.plugins.apache;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.platform.PlatformComponent;

/**
 * Represents a &lt;Location&gt; section in the apache configuration.
 * 
 * This class is a stub and is not implemented as of yet.
 * 
 * @author Lukas Krejci
 */
public class ApacheLocationComponent implements ResourceComponent<ApacheServerComponent>, ConfigurationFacet,
    DeleteResourceFacet {

    public static final String REGEXP_PROP = "regexp";

    ResourceContext<ApacheServerComponent> resourceContext;

    public void start(ResourceContext<ApacheServerComponent> context) throws InvalidPluginConfigurationException,
        Exception {
        // TODO Auto-generated method stub
        resourceContext = context;
    }

    public void stop() {
        // TODO Auto-generated method stub
    }

    public AvailabilityType getAvailability() {
        //TODO implement this
        return AvailabilityType.UP;
    }

    /* (non-Javadoc)
     * @see org.rhq.core.pluginapi.configuration.ConfigurationFacet#loadResourceConfiguration()
     */
    public Configuration loadResourceConfiguration() throws Exception {
        // TODO Auto-generated method stub
        return resourceContext.getResourceType().getResourceConfigurationDefinition().getDefaultTemplate()
            .createConfiguration();
    }

    /* (non-Javadoc)
     * @see org.rhq.core.pluginapi.configuration.ConfigurationFacet#updateResourceConfiguration(org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport)
     */
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        // TODO Auto-generated method stub
    }

    /* (non-Javadoc)
     * @see org.rhq.core.pluginapi.inventory.DeleteResourceFacet#deleteResource()
     */
    public void deleteResource() throws Exception {
        // TODO Auto-generated method stub

    }

}
