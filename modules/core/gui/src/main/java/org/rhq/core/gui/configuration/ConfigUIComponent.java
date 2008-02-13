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
package org.rhq.core.gui.configuration;

import java.util.UUID;
import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import org.jetbrains.annotations.Nullable;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.gui.util.FacesComponentIdFactory;
import org.rhq.core.gui.util.FacesComponentUtility;

/**
 * A component that represents a JON configuration and its associated definition.
 *
 * @author Ian Springer
 */
public class ConfigUIComponent extends UIComponentBase implements FacesComponentIdFactory {
    public static final String COMPONENT_TYPE = "org.jboss.on.Config";
    public static final String COMPONENT_FAMILY = "org.jboss.on.Config";

    private static final String CONFIGURATION_ATTRIBUTE = "configuration";
    private static final String CONFIGURATION_DEFINITION_ATTRIBUTE = "configurationDefinition";
    private static final String NULL_CONFIGURATION_DEFINITION_MESSAGE_ATTRIBUTE = "nullConfigurationDefinitionMessage";
    private static final String NULL_CONFIGURATION_MESSAGE_ATTRIBUTE = "nullConfigurationMessage";
    private static final String NULL_CONFIGURATION_STYLE_ATTRIBUTE = "nullConfigurationStyle";
    private static final String LIST_NAME_ATTRIBUTE = "listName";
    private static final String LIST_INDEX_ATTRIBUTE = "listIndex";

    private ConfigurationDefinition configurationDefinition;
    private Configuration configuration;
    private boolean readOnly;
    private String listName;
    private Integer listIndex;
    private String nullConfigurationDefinitionMessage;
    private String nullConfigurationMessage;
    private String nullConfigurationStyle;
    private boolean prevalidate;
    private boolean aggregate;

    public String createUniqueId() {
        return UNIQUE_ID_PREFIX + UUID.randomUUID();
    }

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    @Nullable
    public Configuration getConfiguration() {
        if (this.configuration == null) {
            this.configuration = FacesComponentUtility.getExpressionAttribute(this, CONFIGURATION_ATTRIBUTE,
                Configuration.class);
        }

        return this.configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    @Nullable
    public ConfigurationDefinition getConfigurationDefinition() {
        if (this.configurationDefinition == null) {
            this.configurationDefinition = FacesComponentUtility.getExpressionAttribute(this,
                CONFIGURATION_DEFINITION_ATTRIBUTE, ConfigurationDefinition.class);
        }

        return this.configurationDefinition;
    }

    public void setConfigurationDefinition(ConfigurationDefinition configurationDefinition) {
        this.configurationDefinition = configurationDefinition;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public String getListName() {
        if (this.listName == null) {
            this.listName = FacesComponentUtility.getExpressionAttribute(this, LIST_NAME_ATTRIBUTE, String.class);
        }

        return listName;
    }

    public void setListName(String listName) {
        this.listName = listName;
    }

    public Integer getListIndex() {
        if (this.listIndex == null) {
            this.listIndex = FacesComponentUtility.getExpressionAttribute(this, LIST_INDEX_ATTRIBUTE, Integer.class);
        }

        return listIndex;
    }

    public void setListIndex(Integer listIndex) {
        this.listIndex = listIndex;
    }

    public String getConfigurationExpressionString() {
        //noinspection deprecation
        return getValueBinding(CONFIGURATION_ATTRIBUTE).getExpressionString();
    }

    public String getNullConfigurationStyle() {
        if (this.nullConfigurationStyle == null) {
            this.nullConfigurationStyle = FacesComponentUtility.getExpressionAttribute(this,
                NULL_CONFIGURATION_STYLE_ATTRIBUTE, String.class);
        }

        return this.nullConfigurationStyle;
    }

    public void setNullConfigurationStyle(String nullConfigurationStyle) {
        this.nullConfigurationStyle = nullConfigurationStyle;
    }

    public String getNullConfigurationDefinitionMessage() {
        if (this.nullConfigurationDefinitionMessage == null) {
            this.nullConfigurationDefinitionMessage = FacesComponentUtility.getExpressionAttribute(this,
                NULL_CONFIGURATION_DEFINITION_MESSAGE_ATTRIBUTE, String.class);
        }

        return this.nullConfigurationDefinitionMessage;
    }

    public void setNullConfigurationDefinitionMessage(String nullConfigurationDefinitionMessage) {
        this.nullConfigurationDefinitionMessage = nullConfigurationDefinitionMessage;
    }

    public String getNullConfigurationMessage() {
        if (this.nullConfigurationMessage == null) {
            this.nullConfigurationMessage = FacesComponentUtility.getExpressionAttribute(this,
                NULL_CONFIGURATION_MESSAGE_ATTRIBUTE, String.class);
        }

        return this.nullConfigurationMessage;
    }

    public void setNullConfigurationMessage(String nullConfigurationMessage) {
        this.nullConfigurationMessage = nullConfigurationMessage;
    }

    public boolean isPrevalidate() {
        return prevalidate;
    }

    public void setPrevalidate(boolean prevalidate) {
        this.prevalidate = prevalidate;
    }

    public boolean isAggregate() {
        return aggregate;
    }

    public void setAggregate(boolean aggregate) {
        this.aggregate = aggregate;
    }

    private Object[] stateValues;

    @Override
    public Object saveState(FacesContext facesContext) {
        if (this.stateValues == null) {
            this.stateValues = new Object[8];
        }

        this.stateValues[0] = super.saveState(facesContext);
        this.stateValues[1] = this.configurationDefinition;
        this.stateValues[2] = this.configuration;
        this.stateValues[3] = this.readOnly;
        this.stateValues[4] = this.listName;
        this.stateValues[5] = this.listIndex;
        this.stateValues[6] = this.prevalidate;
        this.stateValues[7] = this.aggregate;
        return this.stateValues;
    }

    @Override
    public void restoreState(FacesContext facesContext, Object stateValues) {
        this.stateValues = (Object[]) stateValues;
        super.restoreState(facesContext, this.stateValues[0]);
        this.configurationDefinition = (ConfigurationDefinition) this.stateValues[1];
        this.configuration = (Configuration) this.stateValues[2];
        this.readOnly = (Boolean) this.stateValues[3];
        this.listName = (String) this.stateValues[4];
        this.listIndex = (Integer) this.stateValues[5];
        this.prevalidate = (Boolean) this.stateValues[6];
        this.aggregate = (Boolean) this.stateValues[7];
    }

    /*
     * private MethodExpression getMethodExpression(String name) {   if (getValueExpression(name) == null)      return
     * null;   //noinspection deprecation   String expressionString = getValueBinding(name).getExpressionString();
     * return FacesExpressionUtility.createMethodExpression(expressionString, String.class, new Class[0]); }
     */

}