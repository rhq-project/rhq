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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.gui.configuration.helper.ConfigurationExpressionUtility;
import org.rhq.core.gui.configuration.helper.PropertyRenderingUtility;
import org.rhq.core.gui.configuration.CssStyleClasses;
import org.rhq.core.gui.configuration.ConfigRenderer;
import org.rhq.core.gui.util.FacesComponentUtility;
import org.rhq.core.gui.util.FacesExpressionUtility;
import org.rhq.core.gui.util.FacesContextUtility;

/**
 * @author Ian Springer
 */
public class PropertySetRenderer extends Renderer
{
    static final String RENDERER_TYPE = "org.rhq.PropertySet";
    
    private static final String INIT_INPUTS_JAVA_SCRIPT_COMPONENT_ID_SUFFIX = "-initInputsJavaScript";

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

        String id = getInitInputsJavaScriptComponentId(propertySetComponent);
        UIComponent initInputsJavaScriptComponent = propertySetComponent.findComponent(id);
        if (initInputsJavaScriptComponent != null) {
            FacesComponentUtility.detachComponent(initInputsJavaScriptComponent);
            PropertyRenderingUtility.addInitInputsJavaScript(propertySetComponent, id, false, true);
        }
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
        PropertySetComponent propertySetComponent = (PropertySetComponent) component;
        validateAttributes(propertySetComponent);

        // If it's an AJAX request for this component, apply the aggregate config to the member configs
        // and clear our child components. NOTE: This can *not* be done in decode(), because the model
        // will not have been updated yet with any changes made to child UIInput values. 
        String refresh = FacesContextUtility.getOptionalRequestParameter("refresh");
        if (refresh != null && refresh.equals(ConfigRenderer.PROPERTY_SET_COMPONENT_ID)) {
            propertySetComponent.getConfigurationSet().applyAggregateConfiguration();
            component.getChildren().clear();
        }

        ResponseWriter writer = facesContext.getResponseWriter();
        String clientId = component.getClientId(facesContext);
        writer.write('\n');
        writer.writeComment("********** Start of " + component.getClass().getSimpleName() + " component **********");
        writer.startElement("div", component);
        writer.writeAttribute("id", clientId, "clientId");

        // Only create the child components the first time around (i.e. once per JSF lifecycle).
        if (component.getChildCount() != 0)
            return;

        // TODO: Add support for open map member properties, which do not have an associated definition.
        PropertyDefinitionSimple propertyDefinitionSimple = propertySetComponent.getPropertyDefinition();
        if (propertyDefinitionSimple == null)
            return;

        addPropertyDisplayNameAndDescription(propertySetComponent, propertyDefinitionSimple);

        // The below panel is a placeholder. We'll add children to it later once we know the client id's of the
        // property value inputs.
        HtmlPanelGroup setAllToSameValueControlPanel = FacesComponentUtility.addBlockPanel(propertySetComponent, null, null);

        FacesComponentUtility.addVerbatimText(propertySetComponent, "<br/><br/>\n");
        
        FacesComponentUtility.addVerbatimText(propertySetComponent, "\n\n<table class='"
                + CssStyleClasses.MEMBER_PROPERTIES_TABLE + "'>");
        addPropertiesTableHeaderRow(propertySetComponent);

        List<PropertyInfo> propertyInfos = createPropertyInfos(propertySetComponent, propertyDefinitionSimple);
        for (PropertyInfo propertyInfo : propertyInfos)
            addPropertyRow(propertySetComponent, propertyDefinitionSimple, propertyInfo, null);

        FacesComponentUtility.addVerbatimText(propertySetComponent, "</table>\n");
        FacesComponentUtility.addVerbatimText(propertySetComponent, "<br/>\n");

        boolean configReadOnly = propertySetComponent.getReadOnly() != null && propertySetComponent.getReadOnly();
        if (!PropertyRenderingUtility.isReadOnly(propertyDefinitionSimple, null, configReadOnly, false))
            addSetAllToSameValueControls(propertySetComponent, setAllToSameValueControlPanel, propertyInfos);
                
        String id = getInitInputsJavaScriptComponentId(propertySetComponent);
        PropertyRenderingUtility.addInitInputsJavaScript(propertySetComponent, id, false, false);
    }

    private void addSetAllToSameValueControls(PropertySetComponent propertySetComponent, HtmlPanelGroup setAllToSameValueControlPanel, List<PropertyInfo> propertyInfos)
    {
        String masterInputId = propertySetComponent.getId() + ":setAllToSameValue";

        String functionName = "setAllToSameValue_" + propertySetComponent.getId();
        StringBuilder script = new StringBuilder();
        script.append("function ").append(functionName).append("() {\n");
        script.append("var valueInputArray = new Array(");
        for (PropertyInfo propertyInfo : propertyInfos)
            for (String htmlDomReference : PropertyRenderingUtility.getHtmlDomReferences(propertyInfo.getInput()))
                script.append(htmlDomReference).append(", ");
        script.delete(script.length() - 2, script.length()); // chop off the extra ", "
        script.append(");\n");
        script.append("setInputsToValue(valueInputArray, document.getElementById('");
        script.append(masterInputId).append("').value);\n");
        script.append("}\n");
        FacesComponentUtility.addJavaScript(setAllToSameValueControlPanel, null, null, script);

        StringBuilder html = new StringBuilder();
        html.append("Set All Values To: <input id='").append(masterInputId).append("' type='text' ");
        html.append("autocomplete='off' maxLength='").append(PropertySimple.MAX_VALUE_LENGTH).append("' ");
        html.append("class='").append(CssStyleClasses.PROPERTY_VALUE_INPUT).append("'/>");
        html.append("<button type='button' onClick='").append(functionName).append("()'>OK</button>");
        FacesComponentUtility.addVerbatimText(setAllToSameValueControlPanel, html);
    }

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException
    {
        ResponseWriter writer = context.getResponseWriter();
        writer.writeText("\n", component, null);
        writer.endElement("div");
        writer.writeComment("********** End of " + component.getClass().getSimpleName() + " component **********");
    }

    private void addPropertyDisplayNameAndDescription(PropertySetComponent propertySetComponent,
                                                      PropertyDefinitionSimple propertyDefinitionSimple)
    {
        FacesComponentUtility.addVerbatimText(propertySetComponent, "<br/>\n");
        if (propertyDefinitionSimple != null) {
            PropertyRenderingUtility.addPropertyDisplayName(propertySetComponent, propertyDefinitionSimple,
                    propertySetComponent.getReadOnly());
            FacesComponentUtility.addVerbatimText(propertySetComponent, "-");
            PropertyRenderingUtility.addPropertyDescription(propertySetComponent, propertyDefinitionSimple);            
            FacesComponentUtility.addVerbatimText(propertySetComponent, "<br/><br/>\n");
        }
    }

    private List<PropertyInfo> createPropertyInfos(PropertySetComponent propertySetComponent,
                                                   PropertyDefinitionSimple propertyDefinitionSimple)
    {
        ValueExpression configurationInfosExpression = propertySetComponent.getValueExpression(
                PropertySetComponent.CONFIGURATION_SET_ATTRIBUTE);
        String configurationInfosExpressionString = configurationInfosExpression.getExpressionString();
        String configurationExpressionStringFormat = "#{"
                + FacesExpressionUtility.unwrapExpressionString(configurationInfosExpressionString)
                + ".members[%d].configuration}";

        String propertyExpressionStringFormat = createPropertyExpressionString(configurationExpressionStringFormat,
                propertyDefinitionSimple, propertySetComponent.getListIndex());
        //noinspection ConstantConditions
        List<ConfigurationSetMember> configurationSetMembers = propertySetComponent.getConfigurationSet().getMembers();
        List<PropertyInfo> propertyInfos = new ArrayList(configurationSetMembers.size());
        for (int i = 0; i < configurationSetMembers.size(); i++)
        {
            @SuppressWarnings({"ConstantConditions"})
            ConfigurationSetMember memberInfo = configurationSetMembers.get(i);
            String propertyExpressionString = String.format(propertyExpressionStringFormat, i);
            PropertySimple property = FacesExpressionUtility.getValue(propertyExpressionString, PropertySimple.class);
            ValueExpression propertyValueExpression = createPropertyValueExpression(propertyExpressionString);
            PropertyInfo propertyInfo = new PropertyInfo(memberInfo.getLabel(), property, propertyValueExpression);
            propertyInfos.add(propertyInfo);

        }
        Collections.sort(propertyInfos);
        return propertyInfos;
    }

    private void addPropertiesTableHeaderRow(PropertySetComponent propertySetComponent)
    {
        FacesComponentUtility.addVerbatimText(propertySetComponent, "\n\n<tr>");

        FacesComponentUtility.addVerbatimText(propertySetComponent, "<th class='" + CssStyleClasses.PROPERTIES_TABLE_HEADER_CELL + "'>");
        FacesComponentUtility.addOutputText(propertySetComponent, null, "Member", FacesComponentUtility.NO_STYLE_CLASS);
        FacesComponentUtility.addVerbatimText(propertySetComponent, "</th>");

        FacesComponentUtility.addVerbatimText(propertySetComponent, "<th class='" + CssStyleClasses.PROPERTIES_TABLE_HEADER_CELL + "'>");
        FacesComponentUtility.addOutputText(propertySetComponent, null, "Unset", FacesComponentUtility.NO_STYLE_CLASS);
        FacesComponentUtility.addVerbatimText(propertySetComponent, "</th>");

        FacesComponentUtility.addVerbatimText(propertySetComponent, "<th class='" + CssStyleClasses.PROPERTIES_TABLE_HEADER_CELL + "'>");
        FacesComponentUtility.addOutputText(propertySetComponent, null, "Value", FacesComponentUtility.NO_STYLE_CLASS);
        FacesComponentUtility.addVerbatimText(propertySetComponent, "</th>");

        FacesComponentUtility.addVerbatimText(propertySetComponent, "</tr>");
    }

    private void validateAttributes(PropertySetComponent propertySetComponent) {
        /*if (propertySetComponent.getValueExpression(PropertySetComponent.PROPERTY_DEFINITION_ATTRIBUTE) == null) {
            throw new IllegalStateException("The " + propertySetComponent.getClass().getName()
                + " component requires a '" + PropertySetComponent.PROPERTY_DEFINITION_ATTRIBUTE + "' attribute.");
        }*/
        if (propertySetComponent.getValueExpression(PropertySetComponent.CONFIGURATION_SET_ATTRIBUTE) == null) {
            throw new IllegalStateException("The " + propertySetComponent.getClass().getName()
                + " component requires a '" + PropertySetComponent.CONFIGURATION_SET_ATTRIBUTE + "' attribute.");
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

    private String getInitInputsJavaScriptComponentId(PropertySetComponent propertySetComponent)
    {
        return propertySetComponent.getId() + INIT_INPUTS_JAVA_SCRIPT_COMPONENT_ID_SUFFIX;
    }

    private static void addPropertyRow(PropertySetComponent propertySetComponent,
                                       PropertyDefinitionSimple propertyDefinitionSimple,
                                       PropertyInfo propertyInfo, String rowStyleClass) {

        FacesComponentUtility.addVerbatimText(propertySetComponent, "\n\n<tr class='" + rowStyleClass + "'>");

        UIInput input = PropertyRenderingUtility.createInputForSimpleProperty(
                propertyDefinitionSimple, propertyInfo.getProperty(),
                propertyInfo.getPropertyValueExpression(), propertySetComponent.getListIndex(),
                propertySetComponent.getReadOnly(),
                false, true);
        propertyInfo.setInput(input);

        FacesComponentUtility.addVerbatimText(propertySetComponent, "<td class='" + CssStyleClasses.PROPERTY_DISPLAY_NAME_CELL + "'>"); // TODO: CSS
        FacesComponentUtility.addOutputText(propertySetComponent, null, propertyInfo.getLabel(), null);
        FacesComponentUtility.addVerbatimText(propertySetComponent, "</td>");

        FacesComponentUtility.addVerbatimText(propertySetComponent, "<td class='" + CssStyleClasses.PROPERTY_ENABLED_CELL + "'>");
        PropertyRenderingUtility.addUnsetControl(propertySetComponent, propertyDefinitionSimple,
                    propertyInfo.getProperty(), input, propertySetComponent.getReadOnly(), false);
        FacesComponentUtility.addVerbatimText(propertySetComponent, "</td>");

        FacesComponentUtility.addVerbatimText(propertySetComponent, "<td class='" + CssStyleClasses.PROPERTY_VALUE_CELL + "'>");
        propertySetComponent.getChildren().add(input);
        FacesComponentUtility.addVerbatimText(propertySetComponent, "<br/>");
        PropertyRenderingUtility.addMessageComponentForInput(propertySetComponent, input);
        FacesComponentUtility.addVerbatimText(propertySetComponent, "</td>");

        FacesComponentUtility.addVerbatimText(propertySetComponent, "</tr>");
    }

    class PropertyInfo implements Comparable<PropertyInfo> {
        private String label;
        private PropertySimple property;
        private ValueExpression propertyValueExpression;
        private UIInput input;

        PropertyInfo(String label, PropertySimple property, ValueExpression propertyValueExpression)
        {
            // Ensure this.label will never be null, so our compareTo() impl doesn't need to deal with null labels.
            this.label = (label != null) ? label : "";
            this.property = property;
            this.propertyValueExpression = propertyValueExpression;
        }

        public String getLabel()
        {
            return label;
        }

        public PropertySimple getProperty()
        {
            return property;
        }

        public ValueExpression getPropertyValueExpression()
        {
            return propertyValueExpression;
        }

        public UIInput getInput()
        {
            return input;
        }

        public void setInput(UIInput input)
        {
            this.input = input;
        }

        public int compareTo(PropertyInfo that)
        {
            int result = this.label.compareTo(that.label); // NOTE: this.label will never be null.
            if (result == 0)
                if (this.property.getStringValue() != null && that.property.getStringValue() != null)
                    //noinspection ConstantConditions
                    result = this.property.getStringValue().compareTo(that.property.getStringValue());
                else if (this.property.getStringValue() != null && that.property.getStringValue() == null)
                    result = 0;
                else
                    // Show properties with null values (i.e. unset properties) last.
                    result = (this.property.getStringValue() == null) ? 1 : -1;
            return result;
        }
    }
}
