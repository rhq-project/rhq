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
package org.rhq.core.gui.configuration.propset;

import javax.faces.component.UIForm;

import org.jetbrains.annotations.Nullable;
import org.richfaces.component.html.HtmlModalPanel;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.gui.configuration.AbstractConfigurationComponent;
import org.rhq.core.gui.util.FacesComponentIdFactory;
import org.rhq.core.gui.util.FacesComponentUtility;
import org.rhq.core.gui.util.FacesExpressionUtility;

/**
 * A component that represents a set of RHQ Configurations which share the same ConfigurationDefinition.
 *
 * @author Ian Springer
 */
public class ConfigurationSetComponent extends AbstractConfigurationComponent implements FacesComponentIdFactory {
    public static final String COMPONENT_TYPE = "org.rhq.ConfigurationSet";
    public static final String COMPONENT_FAMILY = "rhq";

    public static final String CONFIGURATION_SET_ATTRIBUTE = "configurationSet";

    @Nullable
    public Configuration getConfiguration() {
        ConfigurationSet configurationSet = FacesComponentUtility.getExpressionAttribute(this,
            CONFIGURATION_SET_ATTRIBUTE, ConfigurationSet.class);
        return (configurationSet != null) ? configurationSet.getGroupConfiguration() : null;
    }

    @Nullable
    public ConfigurationDefinition getConfigurationDefinition() {
        ConfigurationSet configurationSet = FacesComponentUtility.getExpressionAttribute(this,
            CONFIGURATION_SET_ATTRIBUTE, ConfigurationSet.class);
        return (configurationSet != null) ? configurationSet.getConfigurationDefinition() : null;
    }

    public String getConfigurationDefinitionExpressionString() {
        String configurationSetExpressionString = getValueExpression(CONFIGURATION_SET_ATTRIBUTE).getExpressionString();
        return "#{" + FacesExpressionUtility.unwrapExpressionString(configurationSetExpressionString)
            + ".configurationDefinition}";
    }

    public String getConfigurationExpressionString() {
        String configurationSetExpressionString = getValueExpression(CONFIGURATION_SET_ATTRIBUTE).getExpressionString();
        return "#{" + FacesExpressionUtility.unwrapExpressionString(configurationSetExpressionString)
            + ".groupConfiguration}";
    }

    @Nullable
    public ConfigurationSet getConfigurationSet() {
        //noinspection UnnecessaryLocalVariable
        ConfigurationSet configurationSet = FacesComponentUtility.getExpressionAttribute(this,
            CONFIGURATION_SET_ATTRIBUTE, ConfigurationSet.class);
        return configurationSet;
    }

    public static String getPropSetModalPanelId(ConfigurationSetComponent configurationSetComponent) {
        return configurationSetComponent.getId() + "PropSetModalPanel";
    }

    public static String getPropSetFormId(ConfigurationSetComponent configurationSetComponent) {
        return configurationSetComponent.getId() + "PropSetForm";
    }

    @Nullable
    public HtmlModalPanel getPropSetModalPanel() {
        UIForm configSetForm = FacesComponentUtility.getEnclosingForm(this);
        return (HtmlModalPanel) configSetForm.getParent().findComponent(getPropSetModalPanelId(this));
    }
}