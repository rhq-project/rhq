/*
 * Jopr Management Platform
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
package org.rhq.plugins.jbossas5;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.plugins.jbossas5.connection.ProfileServiceConnection;
import org.rhq.plugins.jbossas5.util.ConversionUtils;
import org.rhq.plugins.jbossas5.util.DebugUtils;
import org.rhq.plugins.jbossas5.util.ResourceComponentUtils;

import org.jboss.managed.api.ManagedProperty;

/**
 * @author Ian Springer
 */
public abstract class AbstractManagedComponent implements ProfileServiceComponent<ProfileServiceComponent>,
        ConfigurationFacet
{
    private ResourceContext<ProfileServiceComponent> resourceContext;
    private String resourceDescription;

    public void start(ResourceContext<ProfileServiceComponent> resourceContext) throws Exception
    {
        this.resourceContext = resourceContext;
        this.resourceDescription = this.resourceContext.getResourceType()
                + " Resource with key [" + this.resourceContext.getResourceKey() + "]";
    }

    public void stop()
    {
        return;
    }

    // ConfigurationComponent Implementation  --------------------------------------------

    public Configuration loadResourceConfiguration()
    {
        Configuration resourceConfig;
        try
        {
            Map<String, ManagedProperty> managedProperties = getManagedProperties();
            Map<String, PropertySimple> customProps =
                    ResourceComponentUtils.getCustomProperties(this.resourceContext.getPluginConfiguration());
            if (getLog().isDebugEnabled()) getLog().debug("*** AFTER LOAD:\n"
                    + DebugUtils.convertPropertiesToString(managedProperties));
            resourceConfig = ConversionUtils.convertManagedObjectToConfiguration(managedProperties,
                    customProps, this.resourceContext.getResourceType());
        }
        catch (Exception e)
        {
            getLog().error("Failed to load configuration for " + this.resourceDescription + ".", e);
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
        return resourceConfig;
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport configurationUpdateReport)
    {
        Configuration resourceConfig = configurationUpdateReport.getConfiguration();
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        try
        {
            Map<String, ManagedProperty> managedProperties = getManagedProperties();
            Map<String, PropertySimple> customProps = ResourceComponentUtils.getCustomProperties(pluginConfig);
            if (getLog().isDebugEnabled()) getLog().debug("*** BEFORE UPDATE:\n"
                    + DebugUtils.convertPropertiesToString(managedProperties));
            ConversionUtils.convertConfigurationToManagedProperties(managedProperties, resourceConfig,
                    this.resourceContext.getResourceType(), customProps);
            if (getLog().isDebugEnabled()) getLog().debug("*** AFTER UPDATE:\n"
                    + DebugUtils.convertPropertiesToString(managedProperties));
            updateComponent();
            configurationUpdateReport.setStatus(ConfigurationUpdateStatus.SUCCESS);
        }
        catch (Exception e)
        {
            getLog().error("Failed to update configuration for " + this.resourceDescription + ".", e);
            configurationUpdateReport.setStatus(ConfigurationUpdateStatus.FAILURE);
            configurationUpdateReport.setErrorMessage(ThrowableUtil.getAllMessages(e));
        }
    }

    protected abstract Map<String, ManagedProperty> getManagedProperties() throws Exception;

    protected abstract void updateComponent() throws Exception;

    protected abstract Log getLog();

    protected ResourceContext<ProfileServiceComponent> getResourceContext()
    {
        return this.resourceContext;
    }

    public ProfileServiceConnection getConnection()
    {
        return this.resourceContext.getParentResourceComponent().getConnection();
    }

    protected String getResourceDescription()
    {
        return resourceDescription;
    }
}
