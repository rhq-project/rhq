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
import javax.faces.component.UIForm;
import javax.faces.component.UIInput;
import javax.faces.component.html.HtmlPanelGrid;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.component.html.HtmlSelectBooleanCheckbox;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;

import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.gui.configuration.ConfigRenderer;
import org.rhq.core.gui.configuration.CssStyleClasses;
import org.rhq.core.gui.configuration.helper.ConfigurationUtility;
import org.rhq.core.gui.configuration.helper.PropertyRenderingUtility;
import org.rhq.core.gui.util.FacesComponentUtility;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.gui.util.FacesExpressionUtility;
import org.rhq.core.util.sort.HumaneStringComparator;

/**
 * @author Ian Springer
 */
public class PropertySetRenderer extends Renderer {
    static final String RENDERER_TYPE = "org.rhq.PropertySet";

    private static final String INIT_INPUTS_JAVA_SCRIPT_COMPONENT_ID_SUFFIX = "-initInputsJavaScript";

    private static final String PROPERTY_ALL_TO_SAME_VALUE_CONTROLS_COLUMN_CLASSES = ","
        + CssStyleClasses.PROPERTY_VALUE_CELL_BORDERLESS + ",";

    private static final String OPTIONAL_PROPERTY_ALL_TO_SAME_VALUE_CONTROLS_COLUMN_CLASSES = ","
        + CssStyleClasses.PROPERTY_VALUE_CELL_BORDERLESS + ",,,";

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
    public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException {
        PropertySetComponent propertySetComponent = (PropertySetComponent) component;
        validateAttributes(propertySetComponent);

        // If it's an AJAX request for this component, apply the group config to the member configs
        // and clear our child components. NOTE: This can *not* be done in decode(), because the model
        // will not have been updated yet with any changes made to child UIInput values.
        String refresh = FacesContextUtility.getOptionalRequestParameter("refresh");
        if (refresh != null && refresh.equals(ConfigRenderer.PROPERTY_SET_COMPONENT_ID)) {
            propertySetComponent.getConfigurationSet().applyGroupConfigurationForView();
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

        PropertySimple propertySimple = propertySetComponent.getProperty();
        if (propertySimple == null)
            // This means the 'propertyExpressionString' request parameter was not specified, which means this is
            // the initial display of the main configSet page, which means there's nothing for us to render.
            return;
        PropertyDefinitionSimple propertyDefinitionSimple = propertySetComponent.getPropertyDefinition();

        addPropertyDisplayNameAndDescription(propertySetComponent, propertyDefinitionSimple, propertySimple);

        boolean configReadOnly = propertySetComponent.getReadOnly() != null && propertySetComponent.getReadOnly();
        boolean propIsReadOnly = PropertyRenderingUtility.isReadOnly(propertyDefinitionSimple.isReadOnly(),
            propertyDefinitionSimple.isRequired(), null, configReadOnly,false);
        HtmlPanelGroup setAllToSameValueControlPanel = null;
        if (!propIsReadOnly) {
            // NOTE: We'll add children to the below panel a bit later when we know the id's of the inputs.
            FacesComponentUtility.addVerbatimText(propertySetComponent, "<table class='"
                + CssStyleClasses.MEMBER_PROPERTIES_TABLE + "'><tr><td align='center'>");
            setAllToSameValueControlPanel = FacesComponentUtility.addBlockPanel(propertySetComponent, null, null);
            FacesComponentUtility.addVerbatimText(propertySetComponent, "</td></tr></table>");
            FacesComponentUtility.addVerbatimText(propertySetComponent, "<br/>\n");
        }

        FacesComponentUtility.addVerbatimText(propertySetComponent, "\n\n<table class='"
            + CssStyleClasses.MEMBER_PROPERTIES_TABLE + "'>");
        addPropertiesTableHeaderRow(propertySetComponent, propertyDefinitionSimple);

        List<PropertyInfo> propertyInfos = createPropertyInfos(propertySetComponent, propertySimple);

        for (int i = 0; i < propertyInfos.size(); i++) {
            PropertyInfo propertyInfo = propertyInfos.get(i);
            String rowStyleClass = ((i % 2) == 0) ? CssStyleClasses.ROW_ODD : CssStyleClasses.ROW_EVEN;
            addPropertyRow(propertySetComponent, propertyDefinitionSimple, propertyInfo, rowStyleClass);
        }

        FacesComponentUtility.addVerbatimText(propertySetComponent, "</table>\n");
        FacesComponentUtility.addVerbatimText(propertySetComponent, "<br/>\n");

        if (!propIsReadOnly)
            addSetAllToSameValueControls(propertySetComponent, propertyDefinitionSimple, setAllToSameValueControlPanel,
                propertyInfos);

        String id = getInitInputsJavaScriptComponentId(propertySetComponent);
        PropertyRenderingUtility.addInitInputsJavaScript(propertySetComponent, id, false, false);
    }

    private void addSetAllToSameValueControls(PropertySetComponent propertySetComponent,
        PropertyDefinitionSimple propertyDefinitionSimple, HtmlPanelGroup setAllToSameValueControlPanel,
        List<PropertyInfo> propertyInfos) {
        String masterInputId = propertySetComponent.getId() + "_setAllToSameValue";
        UIInput input = PropertyRenderingUtility.createInput(propertyDefinitionSimple);
        input.setId(masterInputId);
        // NOTE: Don't add the input to the component tree yet - we'll add it a bit later.

        HtmlPanelGrid panelGrid = FacesComponentUtility.createComponent(HtmlPanelGrid.class);
        if (isOptional(propertyDefinitionSimple)) {
            panelGrid.setColumns(5);
            panelGrid.setColumnClasses(OPTIONAL_PROPERTY_ALL_TO_SAME_VALUE_CONTROLS_COLUMN_CLASSES);
        } else {
            panelGrid.setColumns(3);
            panelGrid.setColumnClasses(PROPERTY_ALL_TO_SAME_VALUE_CONTROLS_COLUMN_CLASSES);
        }
        setAllToSameValueControlPanel.getChildren().add(panelGrid);

        FacesComponentUtility.addOutputText(panelGrid, null, "Set All Values To: ", null);

        // the property value input
        panelGrid.getChildren().add(input);

        // the unset checkbox (if property is optional)
        HtmlSelectBooleanCheckbox unsetCheckbox = null;
        if (isOptional(propertyDefinitionSimple)) {
            FacesComponentUtility.addOutputText(panelGrid, null, "Unset All: ", null);
            unsetCheckbox = PropertyRenderingUtility.addUnsetControl(panelGrid, propertyDefinitionSimple.isRequired(),
                propertyDefinitionSimple.isReadOnly(), null, propertySetComponent.getListIndex(), input, false,
                propertySetComponent.getReadOnly(), false);
        }

        // the 'APPLY' button
        UIForm form = FacesComponentUtility.getEnclosingForm(input);
        StringBuilder html = new StringBuilder();
        html.append("<button type='button' class='" + CssStyleClasses.BUTTON_SMALL + "' onclick='");
        if (isOptional(propertyDefinitionSimple)) {
            html.append("setAllValuesForOptionalProperty(new Array(");
            for (PropertyInfo propertyInfo : propertyInfos) {
                String inputHtmlDomReference = getHtmlDomReference(propertyInfo.getInput(), form, '"');
                html.append(inputHtmlDomReference).append(", ");
            }
            html.delete(html.length() - 2, html.length()); // chop off the extra ", "
            html.append("), new Array(");
            for (PropertyInfo propertyInfo : propertyInfos) {
                String unsetCheckboxHtmlDomReference = getHtmlDomReference(propertyInfo.getUnsetCheckbox(), form, '"');
                html.append(unsetCheckboxHtmlDomReference).append(", ");
            }
            html.delete(html.length() - 2, html.length()); // chop off the extra ", "
            String masterInputHtmlDomReference = getHtmlDomReference(input, form, '"');
            html.append("), getElementValue(").append(masterInputHtmlDomReference);
            String masterUnsetCheckboxHtmlDomReference = getHtmlDomReference(unsetCheckbox, form, '"');
            html.append("), ").append(masterUnsetCheckboxHtmlDomReference).append(".checked);");
        } else {
            /*html.append("setInputsUnset(new Array(");
            for (PropertyInfo propertyInfo : propertyInfos) {
                String inputHtmlDomReference = getHtmlDomReference(propertyInfo.getInput(), form, '"');
                html.append(inputHtmlDomReference).append(", ");
            }
            html.delete(html.length() - 2, html.length()); // chop off the extra ", "
            html.append("), false);");*/

            html.append("setInputsToValue(new Array(");
            for (PropertyInfo propertyInfo : propertyInfos) {
                String inputHtmlDomReference = getHtmlDomReference(propertyInfo.getInput(), form, '"');
                html.append(inputHtmlDomReference).append(", ");
            }
            html.delete(html.length() - 2, html.length()); // chop off the extra ", "
            html.append("), ");
            String masterInputHtmlDomReference = getHtmlDomReference(input, form, '"');
            html.append("getElementValue(").append(masterInputHtmlDomReference).append("));");
        }
        html.append("'>Apply</button>");

        FacesComponentUtility.addVerbatimText(panelGrid, html);
    }

    private static String getHtmlDomReference(UIInput input, UIForm form, char quoteChar) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        String inputHtmlDomReference = "document." + form.getClientId(facesContext) + "[" + quoteChar
            + input.getClientId(facesContext) + quoteChar + "]";
        return inputHtmlDomReference;
    }

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        writer.writeText("\n", component, null);
        writer.endElement("div");
        writer.writeComment("********** End of " + component.getClass().getSimpleName() + " component **********");
    }

    private void addPropertyDisplayNameAndDescription(PropertySetComponent propertySetComponent,
        PropertyDefinitionSimple propertyDefinitionSimple, PropertySimple propertySimple) {
        FacesComponentUtility.addVerbatimText(propertySetComponent, "<br/>\n");
        PropertyRenderingUtility.addPropertyDisplayName(propertySetComponent, propertyDefinitionSimple, propertySimple,
            propertySetComponent.getReadOnly());
        if (propertyDefinitionSimple != null) {
            FacesComponentUtility.addVerbatimText(propertySetComponent, " - ");
            PropertyRenderingUtility.addPropertyDescription(propertySetComponent, propertyDefinitionSimple);
        }
        FacesComponentUtility.addVerbatimText(propertySetComponent, "<br/><br/>\n");
    }

    private List<PropertyInfo> createPropertyInfos(PropertySetComponent propertySetComponent,
        PropertySimple groupPropertySimple) {
        ValueExpression configurationInfosExpression = propertySetComponent
            .getValueExpression(PropertySetComponent.CONFIGURATION_SET_ATTRIBUTE);
        String configurationInfosExpressionString = configurationInfosExpression.getExpressionString();
        String configurationExpressionStringFormat = "#{"
            + FacesExpressionUtility.unwrapExpressionString(configurationInfosExpressionString)
            + ".members[%d].configuration}";

        String propertyExpressionStringFormat = createPropertyExpressionFormat(configurationExpressionStringFormat,
            groupPropertySimple, propertySetComponent.getListIndex());
        String propertyValueExpressionStringFormat = "#{"
            + FacesExpressionUtility.unwrapExpressionString(propertyExpressionStringFormat) + ".stringValue}";
        //noinspection ConstantConditions
        List<ConfigurationSetMember> configurationSetMembers = propertySetComponent.getConfigurationSet().getMembers();
        List<PropertyInfo> propertyInfos = new ArrayList(configurationSetMembers.size());
        for (int i = 0; i < configurationSetMembers.size(); i++) {
            @SuppressWarnings( { "ConstantConditions" })
            ConfigurationSetMember memberInfo = configurationSetMembers.get(i);

            String propertyExpressionString = String.format(propertyExpressionStringFormat, i);
            PropertySimple propertySimple = FacesExpressionUtility.getValue(propertyExpressionString,
                PropertySimple.class);

            String propertyValueExpressionString = String.format(propertyValueExpressionStringFormat, i);
            ValueExpression propertyValueExpression = FacesExpressionUtility.createValueExpression(
                propertyValueExpressionString, String.class);

            PropertyInfo propertyInfo = new PropertyInfo(memberInfo.getLabel(), propertySimple, propertyValueExpression);
            propertyInfos.add(propertyInfo);
        }
        Collections.sort(propertyInfos);
        return propertyInfos;
    }

    private void addPropertiesTableHeaderRow(PropertySetComponent propertySetComponent,
        PropertyDefinitionSimple propertyDefinitionSimple) {
        FacesComponentUtility.addVerbatimText(propertySetComponent, "\n\n<tr>");

        FacesComponentUtility.addVerbatimText(propertySetComponent, "<th class='"
            + CssStyleClasses.PROPERTIES_TABLE_HEADER_CELL + "'>");
        FacesComponentUtility.addOutputText(propertySetComponent, null, "Member", FacesComponentUtility.NO_STYLE_CLASS);
        FacesComponentUtility.addVerbatimText(propertySetComponent, "</th>");

        if (isOptional(propertyDefinitionSimple)) {
            FacesComponentUtility.addVerbatimText(propertySetComponent, "<th class='"
                + CssStyleClasses.PROPERTIES_TABLE_HEADER_CELL + "'>");
            FacesComponentUtility.addOutputText(propertySetComponent, null, "Unset",
                FacesComponentUtility.NO_STYLE_CLASS);
            FacesComponentUtility.addVerbatimText(propertySetComponent, "</th>");
        }

        FacesComponentUtility.addVerbatimText(propertySetComponent, "<th class='"
            + CssStyleClasses.PROPERTIES_TABLE_HEADER_CELL + "'>");
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

    private static String createPropertyExpressionFormat(String configurationExpressionString,
        PropertySimple propertySimple, Integer listIndex) {
        StringBuilder stringBuilder = new StringBuilder("#{");
        stringBuilder.append(FacesExpressionUtility.unwrapExpressionString(configurationExpressionString));
        LinkedList<Property> propertyHierarchy = ConfigurationUtility.getPropertyHierarchy(propertySimple);
        for (Property property : propertyHierarchy) {
            Property parentProperty = ConfigurationUtility.getParentProperty(property);
            stringBuilder.append(".");
            if (parentProperty == null || parentProperty instanceof PropertyMap) {
                // top-level or map member property
                stringBuilder.append("map['").append(property.getName()).append("']");
            } else {
                // list member property
                stringBuilder.append("list[").append(listIndex).append("]");
            }
        }
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    private String getInitInputsJavaScriptComponentId(PropertySetComponent propertySetComponent) {
        return propertySetComponent.getId() + INIT_INPUTS_JAVA_SCRIPT_COMPONENT_ID_SUFFIX;
    }

    private static void addPropertyRow(PropertySetComponent propertySetComponent,
        PropertyDefinitionSimple propertyDefinitionSimple, PropertyInfo propertyInfo, String rowStyleClass) {

        FacesComponentUtility.addVerbatimText(propertySetComponent, "\n\n<tr class='" + rowStyleClass + "'>");

        // Member (i.e. label) column
        FacesComponentUtility.addVerbatimText(propertySetComponent, "<td class='"
            + CssStyleClasses.MEMBER_PROPERTY_LABEL_CELL + "'>");
        FacesComponentUtility.addOutputText(propertySetComponent, null, propertyInfo.getLabel(), null);
        FacesComponentUtility.addVerbatimText(propertySetComponent, "</td>");

        // Create the input, which is referenced by the unset control and rendered under the Value column.
        UIInput input;
        if (propertyDefinitionSimple != null)
            input = PropertyRenderingUtility.createInputForSimpleProperty(propertyDefinitionSimple, propertyInfo
                .getProperty(), propertyInfo.getPropertyValueExpression(), propertySetComponent.getListIndex(), false,
                propertySetComponent.getReadOnly(), false, true,null);
        else
            input = PropertyRenderingUtility.createInputForSimpleProperty(propertyInfo.getProperty(), propertyInfo
                .getPropertyValueExpression(), propertySetComponent.getReadOnly());
        propertyInfo.setInput(input);

        // Unset column (only if property is optional)
        if (isOptional(propertyDefinitionSimple)) {
            FacesComponentUtility.addVerbatimText(propertySetComponent, "<td class='"
                + CssStyleClasses.MEMBER_PROPERTY_UNSET_CELL + "'>");
            HtmlSelectBooleanCheckbox unsetCheckbox = PropertyRenderingUtility.addUnsetControl(propertySetComponent,
                propertyDefinitionSimple.isRequired(), propertyDefinitionSimple.isReadOnly(), propertyInfo.getProperty(),
                propertySetComponent.getListIndex(), input, false, propertySetComponent.getReadOnly(), false);
            propertyInfo.setUnsetCheckbox(unsetCheckbox);
            FacesComponentUtility.addVerbatimText(propertySetComponent, "</td>");
        }

        // Value column
        FacesComponentUtility.addVerbatimText(propertySetComponent, "<td class='"
            + CssStyleClasses.MEMBER_PROPERTY_VALUE_CELL + "'>");
        propertySetComponent.getChildren().add(input);
        FacesComponentUtility.addVerbatimText(propertySetComponent, "<br/>");
        PropertyRenderingUtility.addMessageComponentForInput(propertySetComponent, input);
        FacesComponentUtility.addVerbatimText(propertySetComponent, "</td>");

        FacesComponentUtility.addVerbatimText(propertySetComponent, "</tr>");
    }

    private static boolean isOptional(PropertyDefinitionSimple propertyDefinitionSimple) {
        return propertyDefinitionSimple == null || !propertyDefinitionSimple.isRequired();
    }

    class PropertyInfo implements Comparable<PropertyInfo> {
        private String label;
        private PropertySimple property;
        private ValueExpression propertyValueExpression;
        private UIInput input;
        private HtmlSelectBooleanCheckbox unsetCheckbox;

        PropertyInfo(String label, PropertySimple property, ValueExpression propertyValueExpression) {
            // Ensure this.label will never be null, so our compareTo() impl doesn't need to deal with null labels.
            this.label = (label != null) ? label : "";
            this.property = property;
            this.propertyValueExpression = propertyValueExpression;
        }

        public String getLabel() {
            return label;
        }

        public PropertySimple getProperty() {
            return property;
        }

        public ValueExpression getPropertyValueExpression() {
            return propertyValueExpression;
        }

        public UIInput getInput() {
            return input;
        }

        public void setInput(UIInput input) {
            this.input = input;
        }

        public HtmlSelectBooleanCheckbox getUnsetCheckbox() {
            return unsetCheckbox;
        }

        public void setUnsetCheckbox(HtmlSelectBooleanCheckbox unsetCheckbox) {
            this.unsetCheckbox = unsetCheckbox;
        }

        public int compareTo(PropertyInfo that) {
            return new HumaneStringComparator().compare(this.label, that.label);
        }
    }
}
