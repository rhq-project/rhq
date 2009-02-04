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
package org.rhq.core.gui.configuration;

 import org.jetbrains.annotations.Nullable;
 import org.rhq.core.domain.configuration.Configuration;
 import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
 import org.rhq.core.gui.util.FacesComponentIdFactory;
 import org.rhq.core.gui.util.FacesComponentUtility;

/**
 * A component that represents an RHQ configuration and its associated definition.
 *
 * @author Ian Springer
 */
public class ConfigUIComponent extends AbstractConfigurationComponent implements FacesComponentIdFactory {
    public static final String COMPONENT_TYPE = "org.jboss.on.Config";    

    private static final String CONFIGURATION_DEFINITION_ATTRIBUTE = "configurationDefinition";
    private static final String CONFIGURATION_ATTRIBUTE = "configuration";

    @Nullable
    public ConfigurationDefinition getConfigurationDefinition() {
        //noinspection UnnecessaryLocalVariable
        ConfigurationDefinition configDef = FacesComponentUtility.getExpressionAttribute(this,
            CONFIGURATION_DEFINITION_ATTRIBUTE, ConfigurationDefinition.class);
        return configDef;
    }

    @Nullable
    public Configuration getConfiguration() {
        //noinspection UnnecessaryLocalVariable
        Configuration config = FacesComponentUtility.getExpressionAttribute(this, CONFIGURATION_ATTRIBUTE,
            Configuration.class);
        return config;
    }

    public String getConfigurationDefinitionExpressionString()
    {
        return getValueExpression(CONFIGURATION_DEFINITION_ATTRIBUTE).getExpressionString();
    }

    public String getConfigurationExpressionString() {
        return getValueExpression(CONFIGURATION_ATTRIBUTE).getExpressionString();
    }
}
