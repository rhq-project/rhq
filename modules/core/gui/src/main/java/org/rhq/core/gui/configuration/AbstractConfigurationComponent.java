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

import java.util.UUID;

import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.gui.util.FacesComponentIdFactory;
import org.rhq.core.gui.util.FacesComponentUtility;
import org.rhq.core.gui.configuration.propset.ConfigurationSetComponent;

/**
 * An abstract base class for the {@link ConfigUIComponent} and the {@link ConfigurationSetComponent} JSF component
 * classes.
 *
 * @author Ian Springer
 */
public abstract class AbstractConfigurationComponent extends UIComponentBase implements FacesComponentIdFactory
{
    private static final String NULL_CONFIGURATION_DEFINITION_MESSAGE_ATTRIBUTE = "nullConfigurationDefinitionMessage";
    private static final String NULL_CONFIGURATION_MESSAGE_ATTRIBUTE = "nullConfigurationMessage";
    private static final String NULL_CONFIGURATION_STYLE_ATTRIBUTE = "nullConfigurationStyle";
    private static final String LIST_NAME_ATTRIBUTE = "listName";
    private static final String LIST_INDEX_ATTRIBUTE = "listIndex";
    private static final String READ_ONLY_ATTRIBUTE = "readOnly";
    private static final String FULLY_EDITABLE_ATTRIBUTE = "fullyEditable";

    private Boolean readOnly;
    private Boolean fullyEditable;
    private String listName;
    private Integer listIndex;
    private String nullConfigurationDefinitionMessage;
    private String nullConfigurationMessage;
    private String nullConfigurationStyle;
    private boolean prevalidate;
    private boolean aggregate;

    public abstract ConfigurationDefinition getConfigurationDefinition();

    public abstract Configuration getConfiguration();

    public abstract String getConfigurationExpressionString();

    public String createUniqueId()
    {
        return UNIQUE_ID_PREFIX + UUID.randomUUID();
    }

    public boolean isReadOnly()
    {
        if (isFullyEditable())
            return false;

        if (this.readOnly == null)
        {
            this.readOnly = FacesComponentUtility.getExpressionAttribute(this, READ_ONLY_ATTRIBUTE, Boolean.class);
        }

        return (this.readOnly != null) ? this.readOnly : false;
    }

    public void setReadOnly(boolean readOnly)
    {
        this.readOnly = readOnly;
    }

    public boolean isFullyEditable()
    {
        if (this.fullyEditable == null)
        {
            this.fullyEditable = FacesComponentUtility.getExpressionAttribute(this, FULLY_EDITABLE_ATTRIBUTE,
                    Boolean.class);
        }

        return (this.fullyEditable != null) ? this.fullyEditable : false;
    }

    public void setFullyEditable(boolean fullyEditable)
    {
        this.fullyEditable = fullyEditable;
    }

    public String getListName()
    {
        if (this.listName == null)
        {
            this.listName = FacesComponentUtility.getExpressionAttribute(this, LIST_NAME_ATTRIBUTE, String.class);
        }

        return this.listName;
    }

    public void setListName(String listName)
    {
        this.listName = listName;
    }

    public Integer getListIndex()
    {
        if (this.listIndex == null)
        {
            this.listIndex = FacesComponentUtility.getExpressionAttribute(this, LIST_INDEX_ATTRIBUTE, Integer.class);
        }

        return this.listIndex;
    }

    public void setListIndex(Integer listIndex)
    {
        this.listIndex = listIndex;
    }

    public String getNullConfigurationStyle()
    {
        if (this.nullConfigurationStyle == null)
        {
            this.nullConfigurationStyle = FacesComponentUtility.getExpressionAttribute(this,
                    NULL_CONFIGURATION_STYLE_ATTRIBUTE, String.class);
        }

        return this.nullConfigurationStyle;
    }

    public void setNullConfigurationStyle(String nullConfigurationStyle)
    {
        this.nullConfigurationStyle = nullConfigurationStyle;
    }

    public String getNullConfigurationDefinitionMessage()
    {
        if (this.nullConfigurationDefinitionMessage == null)
        {
            this.nullConfigurationDefinitionMessage = FacesComponentUtility.getExpressionAttribute(this,
                    NULL_CONFIGURATION_DEFINITION_MESSAGE_ATTRIBUTE, String.class);
        }

        return this.nullConfigurationDefinitionMessage;
    }

    public void setNullConfigurationDefinitionMessage(String nullConfigurationDefinitionMessage)
    {
        this.nullConfigurationDefinitionMessage = nullConfigurationDefinitionMessage;
    }

    public String getNullConfigurationMessage()
    {
        if (this.nullConfigurationMessage == null)
        {
            this.nullConfigurationMessage = FacesComponentUtility.getExpressionAttribute(this,
                    NULL_CONFIGURATION_MESSAGE_ATTRIBUTE, String.class);
        }

        return this.nullConfigurationMessage;
    }

    public void setNullConfigurationMessage(String nullConfigurationMessage)
    {
        this.nullConfigurationMessage = nullConfigurationMessage;
    }

    public boolean isPrevalidate()
    {
        return prevalidate;
    }

    public void setPrevalidate(boolean prevalidate)
    {
        this.prevalidate = prevalidate;
    }

    public boolean isAggregate()
    {
        return aggregate;
    }

    public void setAggregate(boolean aggregate)
    {
        this.aggregate = aggregate;
    }

    private Object[] stateValues;

    @Override
    public Object saveState(FacesContext facesContext)
    {
        if (this.stateValues == null)
        {
            this.stateValues = new Object[7];
        }

        this.stateValues[0] = super.saveState(facesContext);
        this.stateValues[1] = this.readOnly;
        this.stateValues[2] = this.fullyEditable;
        this.stateValues[3] = this.listName;
        this.stateValues[4] = this.listIndex;
        this.stateValues[5] = this.prevalidate;
        this.stateValues[6] = this.aggregate;
        return this.stateValues;
    }

    @Override
    public void restoreState(FacesContext facesContext, Object stateValues)
    {
        this.stateValues = (Object[])stateValues;
        super.restoreState(facesContext, this.stateValues[0]);
        this.readOnly = (Boolean)this.stateValues[1];
        this.fullyEditable = (Boolean)this.stateValues[2];
        this.listName = (String)this.stateValues[3];
        this.listIndex = (Integer)this.stateValues[4];
        this.prevalidate = (Boolean)this.stateValues[5];
        this.aggregate = (Boolean)this.stateValues[6];
    }

    /*
    * private MethodExpression getMethodExpression(String name) {   if (getValueExpression(name) == null)      return
    * null;   //noinspection deprecation   String expressionString = getValueBinding(name).getExpressionString();
    * return FacesExpressionUtility.createMethodExpression(expressionString, String.class, new Class[0]); }
    */

}