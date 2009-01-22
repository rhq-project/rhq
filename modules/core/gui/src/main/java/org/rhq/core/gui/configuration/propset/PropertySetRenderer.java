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

import java.io.IOException;
import java.util.LinkedList;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import javax.faces.render.Renderer;

import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.gui.configuration.helper.ConfigurationExpressionUtility;
import org.rhq.core.gui.configuration.helper.PropertySimpleRenderingUtility;
import org.rhq.core.gui.util.FacesComponentUtility;
import org.rhq.core.gui.util.FacesExpressionUtility;
import org.richfaces.component.html.HtmlColumn;
import org.richfaces.component.html.HtmlDataTable;
import org.richfaces.component.html.HtmlRichMessage;
import org.richfaces.model.Ordering;

/**
 * @author Ian Springer
 */
public class PropertySetRenderer extends Renderer
{
    /**
     * Decode any new state from request parameters for the given {@link PropertySetComponent}.
     *
     * @param facesContext the JSF context for the current request
     * @param component    the {@link PropertySetComponent} for which request parameters should be decoded
     */
    @Override
    public void decode(FacesContext facesContext, UIComponent component) {
        PropertySetComponent propertySetComponent = (PropertySetComponent) component;
        validateAttributes(propertySetComponent);
        // TODO: anything?
    }

    /**
     * Render the beginning of the given {@link PropertySetComponent}.
     *
     * @param facesContext the JSF context for the current request
     * @param component    the {@link PropertySetComponent} to be encoded
     */
    @Override
    public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException
    {
        // Only create the child components the first time around (i.e. once per JSF lifecycle).
        if (component.getChildCount() != 0)
            return;
        
        PropertySetComponent propertySetComponent = (PropertySetComponent) component;
        validateAttributes(propertySetComponent);

        createDataTable(facesContext, propertySetComponent);

        /*
        String configurationInfosExpressionString = configurationInfosExpression.getExpressionString();
        String configurationExpressionStringFormat = "#{"
                + FacesExpressionUtility.unwrapExpressionString(configurationInfosExpressionString)
                + "[%d].configuration}";String propertyExpressionStringFormat = createPropertyExpressionString(configurationExpressionStringFormat,
        propertyDefinition, propertySetComponent.getListIndex());
        //noinspection ConstantConditions
        for (int i = 0; i < propertySetComponent.getConfigurationGroupMemberInfos().size(); i++)
        {
            @SuppressWarnings({"ConstantConditions"})
            ConfigurationGroupMemberInfo memberInfo = propertySetComponent.getConfigurationGroupMemberInfos().get(i);
            FacesComponentUtility.addOutputText(propertySetComponent, null, memberInfo.getLabel(), null);
            propertyExpressionString = String.format(propertyExpressionStringFormat, i);
            PropertySimple property = FacesExpressionUtility.getValue(propertyExpressionString, PropertySimple.class);
            propertyValueExpression = createPropertyValueExpression(propertyExpressionString);
            input = PropertySimpleRenderingUtility.createInputForSimpleProperty(propertyDefinition,
                    property, propertyValueExpression, null, false);
            propertySetComponent.getChildren().add(input);
            FacesComponentUtility.addVerbatimText(propertySetComponent, "<br/>\n");
        }
        */
    }

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException
    {
        super.encodeEnd(context, component);
    }
    
    private void createDataTable(FacesContext facesContext, PropertySetComponent propertySetComponent)
    {
        HtmlDataTable dataTable = new HtmlDataTable();
        dataTable.setId(createUniqueId(facesContext));
        propertySetComponent.getChildren().add(dataTable);
        ValueExpression configurationInfosExpression = propertySetComponent.getValueExpression(
                PropertySetComponent.CONFIGURATION_GROUP_MEMBER_INFOS_ATTRIBUTE);
        dataTable.setValueExpression("value", configurationInfosExpression);
        dataTable.setVar("memberInfo");
        dataTable.setRows(15);
        dataTable.setSortMode("multiple");
        UIOutput tableHeader = new UIOutput();
        tableHeader.setId(createUniqueId(facesContext));
        dataTable.getFacets().put("header", tableHeader);
        tableHeader.setValue("Group Member Values");

        HtmlColumn labelColumn = createLabelColumn(facesContext);
        dataTable.getChildren().add(labelColumn);

        PropertyDefinitionSimple propertyDefinition = propertySetComponent.getPropertyDefinition();
        String configurationExpressionString = "#{memberInfo.configuration}";
        HtmlColumn valueColumn = createValueColumn(facesContext, propertySetComponent, propertyDefinition,
                configurationExpressionString);
        dataTable.getChildren().add(valueColumn);
    }

    private HtmlColumn createValueColumn(FacesContext facesContext, PropertySetComponent propertySetComponent, PropertyDefinitionSimple propertyDefinition, String configurationExpressionString)
    {
        HtmlColumn valueColumn = new HtmlColumn();
        valueColumn.setId(createUniqueId(facesContext));
        String propertyExpressionString = createPropertyExpressionString(configurationExpressionString,
                propertyDefinition, propertySetComponent.getListIndex());
        ValueExpression propertyValueExpression = createPropertyValueExpression(propertyExpressionString);
        valueColumn.setValueExpression("sortBy", propertyValueExpression);
        valueColumn.setSortOrder(Ordering.ASCENDING);
        UIOutput valueColumnHeader = new UIOutput();
        valueColumnHeader.setId(createUniqueId(facesContext));
        valueColumn.getFacets().put("header", valueColumnHeader);
        valueColumnHeader.setValue("Value");
        HtmlRichMessage message = new HtmlRichMessage();
        message.setId(createUniqueId(facesContext));
        valueColumn.getChildren().add(message);
        FacesComponentUtility.addVerbatimText(propertySetComponent, "<br/>\n");
        UIInput input = PropertySimpleRenderingUtility.createInputForSimpleProperty(propertyDefinition,
                    null, propertyValueExpression, null, false);
        input.setId(createUniqueId(facesContext));
        valueColumn.getChildren().add(input);
        message.setFor(input.getId());
        return valueColumn;
    }

    private HtmlColumn createLabelColumn(FacesContext facesContext)
    {
        HtmlColumn labelColumn = new HtmlColumn();
        labelColumn.setId(createUniqueId(facesContext));

        labelColumn.setSortOrder(Ordering.ASCENDING);
        ValueExpression labelExpression = FacesExpressionUtility.createValueExpression("#{memberInfo.label}",
                String.class);
        labelColumn.setValueExpression("sortBy", labelExpression);
        UIOutput labelColumnHeader = new UIOutput();
        labelColumnHeader.setId(createUniqueId(facesContext));
        labelColumn.getFacets().put("header", labelColumnHeader);
        labelColumnHeader.setValue("Member");
        UIOutput label = new UIOutput();
        label.setId(createUniqueId(facesContext));
        labelColumn.getChildren().add(label);
        label.setValueExpression("value", labelExpression);
        return labelColumn;
    }

    private static String createUniqueId(FacesContext facesContext)
    {
        return facesContext.getViewRoot().createUniqueId();
    }

    private void validateAttributes(PropertySetComponent propertySetComponent) {
        if (propertySetComponent.getValueExpression(PropertySetComponent.PROPERTY_DEFINITION_ATTRIBUTE) == null) {
            throw new IllegalStateException("The " + propertySetComponent.getClass().getName()
                + " component requires a '" + PropertySetComponent.PROPERTY_DEFINITION_ATTRIBUTE + "' attribute.");
        }

        if (propertySetComponent.getValueExpression(PropertySetComponent.CONFIGURATION_GROUP_MEMBER_INFOS_ATTRIBUTE) == null) {
            throw new IllegalStateException("The " + propertySetComponent.getClass().getName()
                + " component requires a '" + PropertySetComponent.CONFIGURATION_GROUP_MEMBER_INFOS_ATTRIBUTE + "' attribute.");
        }
    }

    private static String createPropertyExpressionString(String configurationExpressionString,
                                                      PropertyDefinitionSimple propertyDefinitionSimple,
                                                      Integer listIndex)
    {
        StringBuilder stringBuilder = new StringBuilder("#{");
        stringBuilder.append(FacesExpressionUtility.unwrapExpressionString(configurationExpressionString));
        LinkedList<PropertyDefinition> propertyDefinitionHierarchy =
                ConfigurationExpressionUtility.getPropertyDefinitionHierarchy(propertyDefinitionSimple);
        for (PropertyDefinition propertyDefinition : propertyDefinitionHierarchy)
        {
            PropertyDefinition parentPropertyDefinition =
                    ConfigurationExpressionUtility.getParentPropertyDefinition(propertyDefinition);
            stringBuilder.append(".");
            if (parentPropertyDefinition == null || parentPropertyDefinition instanceof PropertyDefinitionMap)
            {
                // top-level or map member property
                stringBuilder.append("map['").append(propertyDefinition.getName()).append("']");
            } else {
                // list member property
                stringBuilder.append("list[").append(listIndex).append("]");
            }
        }
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    private static ValueExpression createPropertyValueExpression(String propertyExpressionString)
    {
        StringBuilder stringBuilder = new StringBuilder("#{");
        stringBuilder.append(FacesExpressionUtility.unwrapExpressionString(propertyExpressionString));
        stringBuilder.append(".stringValue}");
        String propertyValueExpressionString = stringBuilder.toString();
        return FacesExpressionUtility.createValueExpression(propertyValueExpressionString, String.class);
    }
}
