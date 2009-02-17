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
package org.rhq.core.gui.configuration.propset;

import java.util.UUID;

import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;

import org.jetbrains.annotations.Nullable;
import org.rhq.core.gui.util.FacesComponentUtility;
import org.rhq.core.gui.util.FacesComponentIdFactory;
import org.rhq.core.gui.util.FacesExpressionUtility;
import org.rhq.core.gui.configuration.helper.ConfigurationUtility;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.PropertySimple;

/**
 * @author Ian Springer
 */
public class PropertySetComponent extends UIComponentBase implements FacesComponentIdFactory
{
    private static final String COMPONENT_FAMILY = "rhq";
    public static final String COMPONENT_TYPE = "org.rhq.PropertySet";

    public static final String PROPERTY_EXPRESSION_STRING_ATTRIBUTE = "propertyExpressionString";
    public static final String CONFIGURATION_SET_ATTRIBUTE = "configurationSet";

    private Boolean readOnly;    
    private Integer listIndex;

    public PropertySetComponent()
    {
        setRendererType(PropertySetRenderer.RENDERER_TYPE);
    }

    public String createUniqueId() {
        return UNIQUE_ID_PREFIX + UUID.randomUUID();
    }

    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    @Nullable
    public PropertyDefinitionSimple getPropertyDefinition() {
        PropertySimple propertySimple = getProperty();
        ConfigurationDefinition configurationDefinition = getConfigurationSet().getConfigurationDefinition();
        PropertyDefinition propertyDefinition = ConfigurationUtility.getPropertyDefinitionForProperty(propertySimple,
                configurationDefinition);
        return (PropertyDefinitionSimple)propertyDefinition;
    }

    @Nullable
    public PropertySimple getProperty() {
        //noinspection UnnecessaryLocalVariable
        String propertyExpressionString = FacesComponentUtility.getExpressionAttribute(this,
                PROPERTY_EXPRESSION_STRING_ATTRIBUTE, String.class);
        if (propertyExpressionString == null || propertyExpressionString.equals(""))
            propertyExpressionString = (String)getAttributes().get(PROPERTY_EXPRESSION_STRING_ATTRIBUTE);
        if (propertyExpressionString == null || propertyExpressionString.equals(""))
            return null;
        propertyExpressionString = FacesExpressionUtility.unwrapExpressionString(propertyExpressionString);
        propertyExpressionString = "#{" + propertyExpressionString.replace(".stringValue", "") + "}";
        PropertySimple propertySimple = FacesExpressionUtility.getValue(propertyExpressionString, PropertySimple.class);
        return propertySimple;
    }

    @Nullable
    public ConfigurationSet getConfigurationSet() {
        //noinspection UnnecessaryLocalVariable
        ConfigurationSet configurationSet = FacesComponentUtility.getExpressionAttribute(this,
                CONFIGURATION_SET_ATTRIBUTE, ConfigurationSet.class);
        if (configurationSet == null)
            throw new IllegalStateException("The '" + CONFIGURATION_SET_ATTRIBUTE + "' attribute is required.");   
        return configurationSet;
    }

    public Boolean getReadOnly()
    {
        return readOnly;
    }

    public void setReadOnly(Boolean readOnly)
    {
        this.readOnly = readOnly;
    }

    public Integer getListIndex()
    {
        return listIndex;
    }

    public void setListIndex(Integer listIndex)
    {
        this.listIndex = listIndex;
    }

    private Object[] stateValues;

    @Override
    public Object saveState(FacesContext facesContext) {
        if (this.stateValues == null)
            this.stateValues = new Object[3];
        this.stateValues[0] = super.saveState(facesContext);
        this.stateValues[1] = this.readOnly;
        this.stateValues[2] = this.listIndex;
        return this.stateValues;
    }

    @Override
    public void restoreState(FacesContext facesContext, Object stateValues) {
        this.stateValues = (Object[]) stateValues;
        super.restoreState(facesContext, this.stateValues[0]);
        this.readOnly = (Boolean) this.stateValues[1];
        this.listIndex = (Integer) this.stateValues[2];
    }
}
