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
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.gui.configuration.propset.ConfigurationSet;
import org.rhq.core.gui.configuration.propset.ConfigurationSetMember;
import org.rhq.enterprise.gui.common.Outcomes;

/**
 * @author Ian Springer
 */
public abstract class AbstractTestConfigurationUIBean {
    public static final String [] LABELS = new String[] { "AAA", "ZZZ", "BBB", "YYY","AAA", "AAA", "ZZZ", "ZZZ", "YYY", "BBB"};

    public static final int GROUP_SIZE = 100;

    private ConfigurationDefinition configurationDefinition;
    private Configuration configuration;
    private List<Property> properties;
    private ConfigurationSet configurationSet;

    protected AbstractTestConfigurationUIBean()
    {
        this.configurationDefinition = TestConfigurationFactory.createConfigurationDefinition();
        this.configuration = TestConfigurationFactory.createConfiguration();
        List<ConfigurationSetMember> members = new ArrayList(GROUP_SIZE);
        for (int i = 0; i < GROUP_SIZE; i++) {
            Configuration configuration = this.configuration.deepCopy(true);
            configuration.setId(i + 1);
            configuration.getSimple("String1").setStringValue(UUID.randomUUID().toString());
            configuration.getSimple("Integer").setStringValue(String.valueOf(i + 1));
            configuration.getSimple("Boolean").setStringValue(String.valueOf(i % 2 == 0));
            if (i == 0)
                configuration.getMap("OpenMapOfSimples").put(new PropertySimple("PROCESSOR_CORES", "4"));
            ConfigurationSetMember memberInfo =
                    new ConfigurationSetMember(LABELS[GROUP_SIZE % LABELS.length], configuration);
            members.add(memberInfo);
        }
        this.configurationSet = new ConfigurationSet(this.configurationDefinition, members);
        // Unwrap the Hibernate proxy objects, which Facelets appears not to be able to handle.
        this.properties = new ArrayList<Property>(this.configuration.getProperties());
    }

    public String finish() {
        return Outcomes.SUCCESS;
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

    public ConfigurationSet getConfigurationSet()
    {
        return configurationSet;
    }

    public void setConfigurationSet(ConfigurationSet configurationSet)
    {
        this.configurationSet = configurationSet;
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