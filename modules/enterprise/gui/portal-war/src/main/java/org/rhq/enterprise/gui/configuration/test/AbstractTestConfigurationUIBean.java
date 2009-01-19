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
package org.rhq.enterprise.gui.configuration.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;

/**
 * @author Ian Springer
 */
public abstract class AbstractTestConfigurationUIBean {
    protected static final String SUCCESS_OUTCOME = "success";
    protected static final String FAILURE_OUTCOME = "failure";

    private ConfigurationDefinition configurationDefinition;
    private Configuration configuration;
    private Set<Configuration> configurations;
    private List<Property> properties;

    protected AbstractTestConfigurationUIBean() throws CloneNotSupportedException
    {
        this.configurationDefinition = TestConfigurationFactory.createConfigurationDefinition();
        this.configuration = TestConfigurationFactory.createConfiguration();
        for (int i = 0; i < 10; i++)
            this.configurations.add(this.configuration.clone());
        // Unwrap the Hibernate proxy objects, which Facelets appears not to be able to handle.
        this.properties = new ArrayList<Property>(this.configuration.getProperties());
    }

    @Nullable
    public ConfigurationDefinition getConfigurationDefinition() {
        return this.configurationDefinition;
    }

    public void setConfigurationDefinition(@NotNull
    ConfigurationDefinition configurationDefinition) {
        this.configurationDefinition = configurationDefinition;
    }

    @Nullable
    public Configuration getConfiguration() {
        return this.configuration;
    }

    public void setConfiguration(@NotNull
    Configuration configuration) {
        this.configuration = configuration;
    }

    public Set<Configuration> getConfigurations()
    {
        return configurations;
    }

    public void setConfigurations(Set<Configuration> configurations)
    {
        this.configurations = configurations;
    }

    public List<Property> getProperties()
    {
        return this.properties;
    }

    public String getNullConfigurationDefinitionMessage() {
        return "Test config def is null (should never happen).";
    }

    public String getNullConfigurationMessage() {
        return "Test config is null (should never happen).";
    }
}