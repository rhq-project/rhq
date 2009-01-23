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
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.gui.configuration.propset.ConfigurationGroupMemberInfo;
import org.rhq.enterprise.gui.common.Outcomes;

/**
 * @author Ian Springer
 */
public abstract class AbstractTestConfigurationUIBean {
    private ConfigurationDefinition configurationDefinition;
    private Configuration configuration;
    private List<Property> properties;
    private List<ConfigurationGroupMemberInfo> memberInfos;
    public static final String [] LABELS = new String[] { "AAA", "ZZZ", "BBB", "YYY","AAA", "AAA", "ZZZ", "ZZZ", "YYY", "BBB"};

    protected AbstractTestConfigurationUIBean()
    {
        this.configurationDefinition = TestConfigurationFactory.createConfigurationDefinition();
        this.configuration = TestConfigurationFactory.createConfiguration();
        this.memberInfos = new ArrayList(10);
        for (int i = 0; i < 10; i++) {
            Configuration configuration = this.configuration.deepCopy(true);
            configuration.setId(i + 1);
            configuration.getSimple("String1").setStringValue(UUID.randomUUID().toString());
            configuration.getSimple("Integer").setStringValue(String.valueOf(i + 1));
            configuration.getSimple("Boolean").setStringValue(String.valueOf(i % 2 == 0));
            ConfigurationGroupMemberInfo memberInfo =
                    new ConfigurationGroupMemberInfo(LABELS[i], configuration);
            this.memberInfos.add(memberInfo);
        }
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

    public List<ConfigurationGroupMemberInfo> getMemberInfos()
    {
        return memberInfos;
    }

    public void setMemberInfos(List<ConfigurationGroupMemberInfo> memberInfos)
    {
        this.memberInfos = memberInfos;
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