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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;

/**
 * @author Ian Springer
 */
public abstract class AbstractTestConfigurationUIBean {
    protected static final String SUCCESS_OUTCOME = "success";
    protected static final String FAILURE_OUTCOME = "failure";

    private ConfigurationDefinition configurationDefinition;
    private Configuration configuration;

    protected AbstractTestConfigurationUIBean() {
        this.configurationDefinition = TestConfigurationFactory.createConfigurationDefinition();
        this.configuration = TestConfigurationFactory.createConfiguration();
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

    public String getNullConfigurationDefinitionMessage() {
        return "Test config def is null (should never happen).";
    }

    public String getNullConfigurationMessage() {
        return "Test config is null (should never happen).";
    }
}