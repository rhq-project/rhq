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

import static org.rhq.core.domain.measurement.AvailabilityType.DOWN;
import static org.rhq.core.domain.measurement.AvailabilityType.UP;

import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.apache.util.PluginUtility;
import org.rhq.plugins.www.util.WWWUtils;

/**
 * Represents a &lt;Location&gt; section in the apache configuration.
 *
 * @author Lukas Krejci
 * @author Jeremie Lagarde
 */
public class ApacheLocationComponent implements ResourceComponent<ApacheVirtualHostServiceComponent>,
    ConfigurationFacet, DeleteResourceFacet {

    private static final Log LOG = LogFactory.getLog(ApacheLocationComponent.class);

    public static final String REGEXP_PROP = "regexp";
    public static final String HANDLER_PROP = "handler";
    public static final String URL_PROP = "url";

    ResourceContext<ApacheVirtualHostServiceComponent> resourceContext;

    public void start(ResourceContext<ApacheVirtualHostServiceComponent> context)
        throws InvalidPluginConfigurationException, Exception {
        resourceContext = context;
    }

    public void stop() {
    }

    public AvailabilityType getAvailability() {
        boolean available;
        try {
            URL url = new URL(getURL());
            if (url != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Trying to ping the location for availability: " + url);
                }
                int timeout = PluginUtility.getAvailabilityFacetTimeout();
                int code = WWWUtils.getResponseCode(url);
                if (code >= 200 && code <300) {
                    available = true;
                } else {
                    available = false;
                }
            } else {
                // not configured location on regexp location
                available = true;
            }
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Exception while checking availability.", e);
            }
            available = false;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Availability determined: " + (available ? UP : DOWN));
        }

        return (available) ? UP : DOWN;
    }

    /* (non-Javadoc)
     * @see org.rhq.core.pluginapi.configuration.ConfigurationFacet#loadResourceConfiguration()
     */
    public Configuration loadResourceConfiguration() throws Exception {
        return resourceContext.getResourceType().getResourceConfigurationDefinition().getDefaultTemplate()
            .createConfiguration();
    }

    /* (non-Javadoc)
     * @see org.rhq.core.pluginapi.configuration.ConfigurationFacet#updateResourceConfiguration(org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport)
     */
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
    }

    /* (non-Javadoc)
     * @see org.rhq.core.pluginapi.inventory.DeleteResourceFacet#deleteResource()
     */
    public void deleteResource() throws Exception {
    }

    public String getURL() {
        PropertySimple property = (PropertySimple) resourceContext.getPluginConfiguration().get(URL_PROP);
        if (property != null)
            return property.getStringValue();
        else
            return null;
    }
}
