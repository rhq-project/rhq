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
package org.rhq.core.gui.configuration.helper;

import java.util.List;

import javax.el.ValueExpression;
import javax.faces.component.UIInput;
import javax.faces.component.UISelectItem;
import javax.faces.component.UISelectOne;
import javax.faces.component.html.HtmlInputSecret;
import javax.faces.component.html.HtmlInputText;
import javax.faces.component.html.HtmlInputTextarea;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.component.html.HtmlSelectOneRadio;

import org.jetbrains.annotations.NotNull;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionEnumeration;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.gui.converter.PropertySimpleValueConverter;
import org.rhq.core.gui.util.FacesComponentUtility;
import org.rhq.core.gui.util.FacesExpressionUtility;
import org.rhq.core.gui.util.PropertyIdGeneratorUtility;
import org.rhq.core.gui.validator.PropertySimpleValueValidator;

/**
 * @author Ian Springer
 */
public class PropertySimpleRenderingUtility
{
    private static final String INPUT_TEXT_WIDTH_STYLE = "width:185px;";
    private static final int INPUT_TEXT_COMPONENT_WIDTH = 30;
    private static final int INPUT_TEXTAREA_COMPONENT_ROWS = 4;

    /**
     * Enums with a size equal to or greater than this threshold will be rendered as list boxes, rather than radios.
     */
    private static final int LISTBOX_THRESHOLD_ENUM_SIZE = 6;

    @NotNull
    public static UIInput createInputForSimpleProperty(PropertyDefinitionSimple propertyDefinitionSimple,
                                                       PropertySimple propertySimple,
                                                       ValueExpression propertyValueExpression,
                                                       Integer listIndex,
                                                       boolean readOnly) {
        UIInput input;
        switch (propertyDefinitionSimple.getType()) {
        case BOOLEAN: {
            input = createInputForBooleanProperty();
            break;
        }

        case LONG_STRING: {
            input = createInputForLongStringProperty();
            break;
        }

        case PASSWORD: {
            input = createInputForPasswordProperty();
            break;
        }

        default: {
            if (isEnum(propertyDefinitionSimple)) {
                input = createInputForEnumProperty(propertyDefinitionSimple);
            } else {
                input = createInputForStringProperty();
            }
            // TODO (ips, 01/21/09): Figure out some way to do this for the propertySet component, where propertySimple will
            //      be null. Perhaps perform this logic in PropertySetRenderer.encodeEnd() after the UIInputs have been
            //      rendered.
            if (propertySimple != null)
                addTitleAttribute(input, propertySimple.getStringValue());
        }
        }

        boolean isUnset = isUnset(propertyDefinitionSimple, propertySimple);
        boolean isReadOnly = isReadOnly(readOnly, propertyDefinitionSimple, propertySimple);

        // Find the actual property corresponding to this property def, and use that to create the component id.
        // TODO (ips, 01/21/09): Generate id using PropertyDefinition instead of Property, since Property will be null
        //      for the propertySet component.
        if (propertySimple != null) {
            String propertyId = PropertyIdGeneratorUtility.getIdentifier(propertySimple, listIndex);
            input.setId(propertyId);
        }

        input.setValueExpression("value", propertyValueExpression);

        // It's important to set the label attribute, since we include it in our validation error messages.
        input.getAttributes().put("label", propertyDefinitionSimple.getDisplayName());

        // The below adds an inert attribute to the input that contains the name of the associated property - useful
        // for debugging (i.e. when viewing source of the page or using a JavaScript debugger).
        input.getAttributes().put("ondblclick", "//" + propertyDefinitionSimple.getName());

        FacesComponentUtility.setUnset(input, isUnset);
        FacesComponentUtility.setReadonly(input, isReadOnly);

        addValidatorsAndConverter(input, propertyDefinitionSimple, readOnly);

        return input;
    }



    @NotNull
    public static UIInput createInputForSimpleProperty(PropertySimple propertySimple, String valueExpressionFormat, 
                                                       boolean readOnly) {
        UIInput input = createInputForStringProperty();
        setInputValueExpression(input, propertySimple.getName(), valueExpressionFormat);
        FacesComponentUtility.setReadonly(input, readOnly);
        addTitleAttribute(input, propertySimple.getStringValue());
        return input;
    }

    private static UIInput createInputForBooleanProperty() {
        // <h:selectOneRadio id="#{identifier}" value="#{beanValue}" layout="pageDirection" styleClass="radiolabels">
        //    <f:selectItems value="#{itemValues}"></f:selectItems>
        // </h:selectOneRadio>
        HtmlSelectOneRadio selectOneRadio = FacesComponentUtility
            .createComponent(HtmlSelectOneRadio.class, null);
        selectOneRadio.setLayout("lineDirection");
        // TODO: We may want to use CSS to get less space between the radio buttons
        //      (see http://jira.jboss.com/jira/browse/JBMANCON-21).

        UISelectItem selectItem = FacesComponentUtility.createComponent(UISelectItem.class, null);
        selectItem.setItemLabel("Yes");
        selectItem.setItemValue("true");
        selectOneRadio.getChildren().add(selectItem);
        selectItem = FacesComponentUtility.createComponent(UISelectItem.class, null);
        selectItem.setItemLabel("No");
        selectItem.setItemValue("false");
        selectOneRadio.getChildren().add(selectItem);
        return selectOneRadio;
    }

    // <h:selectOneRadio id="#{identifier}" value="#{beanValue}" layout="pageDirection" styleClass="radiolabels">
    //    <f:selectItems value="#{itemValues}"></f:selectItems>
    // </h:selectOneRadio>
    private static UIInput createInputForEnumProperty(PropertyDefinitionSimple propertyDefinitionSimple) {
        UISelectOne selectOne;
        if (propertyDefinitionSimple.getEnumeratedValues().size() >= LISTBOX_THRESHOLD_ENUM_SIZE) {
            // Use a drop down menu for larger enums...
            HtmlSelectOneMenu menu = FacesComponentUtility.createComponent(HtmlSelectOneMenu.class, null);

            // TODO: Use CSS to set the width of the menu.
            selectOne = menu;
        } else {
            // ...and a radio for smaller ones.
            HtmlSelectOneRadio radio = FacesComponentUtility.createComponent(HtmlSelectOneRadio.class, null);
            radio.setLayout("pageDirection");

            // TODO: We may want to use CSS to get less space between the radio buttons
            //      (see http://jira.jboss.com/jira/browse/JBMANCON-21).
            selectOne = radio;
        }

        List<PropertyDefinitionEnumeration> options = propertyDefinitionSimple.getEnumeratedValues();
        for (PropertyDefinitionEnumeration option : options) {
            UISelectItem selectItem = FacesComponentUtility.createComponent(UISelectItem.class, null);
            selectItem.setItemLabel(option.getName());
            selectItem.setItemValue(option.getValue());
            selectOne.getChildren().add(selectItem);
        }

        return selectOne;
    }

    private static UIInput createInputForStringProperty() {
        HtmlInputText inputText = FacesComponentUtility.createComponent(HtmlInputText.class, null);

        //TODO: check if this has units, then apply the correct style
        inputText.setStyle(INPUT_TEXT_WIDTH_STYLE);
        //      inputText.setStyle(INPUT_TEXT_WIDTH_STYLE_WITH_UNITS);
        inputText.setMaxlength(PropertySimple.MAX_VALUE_LENGTH);

        // Disable browser auto-completion.
        inputText.setAutocomplete("off");
        return inputText;
    }

    private static UIInput createInputForPasswordProperty() {
        HtmlInputSecret inputSecret = FacesComponentUtility.createComponent(HtmlInputSecret.class, null);
        inputSecret.setStyle(INPUT_TEXT_WIDTH_STYLE);
        inputSecret.setMaxlength(PropertySimple.MAX_VALUE_LENGTH);

        // TODO: Remove the below line, as it's not secure, and improve support for displaying/validating password fields.
        inputSecret.setRedisplay(true);

        // Disable browser auto-completion.
        inputSecret.setAutocomplete("off");
        return inputSecret;
    }

    private static UIInput createInputForLongStringProperty() {
        HtmlInputTextarea inputTextarea = FacesComponentUtility.createComponent(HtmlInputTextarea.class, null);
        inputTextarea.setRows(INPUT_TEXTAREA_COMPONENT_ROWS);
        inputTextarea.setStyle(INPUT_TEXT_WIDTH_STYLE);
        return inputTextarea;
    }

    private static boolean isEnum(PropertyDefinitionSimple simplePropertyDefinition) {
        return !simplePropertyDefinition.getEnumeratedValues().isEmpty();
    }

    private static void addValidatorsAndConverter(UIInput input, PropertyDefinitionSimple propertyDefinitionSimple,
                                           boolean readOnly) {
        if (!readOnly) {
            input.setRequired(propertyDefinitionSimple.isRequired());
            input.addValidator(new PropertySimpleValueValidator(propertyDefinitionSimple));
            input.setConverter(new PropertySimpleValueConverter());
        }
    }

    private static String getSimplePropertyValue(PropertyDefinitionSimple propertyDefinitionSimple,
                                                 String valueExpressionFormat) {
        String valueExpression = String.format(valueExpressionFormat, propertyDefinitionSimple.getName());
        return FacesExpressionUtility.getValue(valueExpression, String.class);
    }

    private static void addTitleAttribute(UIInput input, String propertyValue) {
        if (input instanceof HtmlInputText) {
            //         TODO: will this still work now that we use the style def'n "width:185px;" to define the input field width?
            // For text inputs with values that are too long to fit in the input text field, add a "title" attribute set to
            // the value, so the user can see the untruncated value via a tooltip.
            // (see http://jira.jboss.com/jira/browse/JBNADM-1608)
            HtmlInputText inputText = (HtmlInputText) input;
            if ((propertyValue != null) && (propertyValue.length() > INPUT_TEXT_COMPONENT_WIDTH)) {
                inputText.setTitle(propertyValue);
            }

            inputText.setOnchange("setInputTitle(this)");
        }
    }

    /**
     * Binds the value of the specified UIInput to an EL expression corresponding to the Configuration property with the
     * specified name.
     */
    // TODO: Add support for properties inside lists.
    @SuppressWarnings( { "JavaDoc" })
    private static void setInputValueExpression(UIInput input, String propertyName, String valueExpressionFormat) {
        // e.g.: #{configuration.simpleProperties['useJavaContext'].stringValue}
        String expression = String.format(valueExpressionFormat, propertyName);
        ValueExpression valueExpression = FacesExpressionUtility.createValueExpression(expression, String.class);
        input.setValueExpression("value", valueExpression);
    }

    private static boolean isUnset(PropertyDefinitionSimple propertyDefinitionSimple, PropertySimple propertySimple) {
        // TODO (ips, 01/21/09): Figure out some way to do this for the propertySet component, where propertySimple will
        //      be null. Perhaps perform this logic in PropertySetRenderer.encodeEnd() after the UIInputs have been
        //      rendered.
        if (propertySimple == null)
            return false;
        return (!propertyDefinitionSimple.isRequired() && propertySimple.getStringValue() == null);
    }

    private static boolean isReadOnly(boolean readOnlyConfig, PropertyDefinitionSimple propertyDefinitionSimple,
                                      PropertySimple propertySimple) {
        // a fully editable config overrides any other means of setting read only
        return (readOnlyConfig || (propertyDefinitionSimple.isReadOnly() &&
                !isInvalidRequiredProperty(propertyDefinitionSimple, propertySimple)));
    }

    private static boolean isInvalidRequiredProperty(PropertyDefinitionSimple propertyDefinitionSimple,
                                                     PropertySimple propertySimple) {
        // TODO (ips, 01/21/09): Figure out some way to do this for the propertySet component, where propertySimple will
        //      be null. Perhaps perform this logic in PropertySetRenderer.encodeEnd() after the UIInputs have been
        //      rendered.
        if (propertySimple == null)
            return false;
        boolean isInvalidRequiredProperty = false;
        if (propertyDefinitionSimple.isRequired()) {
            String errorMessage = propertySimple.getErrorMessage();
            if ((null == propertySimple.getStringValue()) || "".equals(propertySimple.getStringValue())
                || ((null != errorMessage) && (!"".equals(errorMessage.trim())))) {
                // Required properties with no value, or an invalid value (assumed if we see an error message) should
                // never be set to read-only, otherwise the user will have no way to give the property a value and
                // thereby get things to a valid state.
                isInvalidRequiredProperty = true;
            }
        }
        return isInvalidRequiredProperty;
    }    
}
