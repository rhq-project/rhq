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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.el.ValueExpression;
import javax.faces.application.FacesMessage;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIOutput;
import javax.faces.component.UISelectItem;
import javax.faces.component.UISelectOne;
import javax.faces.component.html.HtmlInputSecret;
import javax.faces.component.html.HtmlInputText;
import javax.faces.component.html.HtmlInputTextarea;
import javax.faces.component.html.HtmlSelectBooleanCheckbox;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.component.html.HtmlSelectOneRadio;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionEnumeration;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.gui.configuration.CssStyleClasses;
import org.rhq.core.gui.converter.PropertySimpleValueConverter;
import org.rhq.core.gui.util.FacesComponentUtility;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.gui.util.PropertyIdGeneratorUtility;
import org.rhq.core.gui.validator.PropertySimpleValueValidator;

/**
 * @author Ian Springer
 */
public class PropertyRenderingUtility {
    private static final int INPUT_TEXT_COMPONENT_WIDTH = 30;
    private static final int INPUT_TEXTAREA_COMPONENT_ROWS = 4;
    private static final String ERROR_MSG_STYLE_CLASS = "error-msg";

    /**
     * Enums with a size equal to or greater than this threshold will be rendered as list boxes, rather than radios.
     */
    private static final int LISTBOX_THRESHOLD_ENUM_SIZE = 6;

    private static final String UNIQUE_ID_PREFIX = "rhq_id";

    @NotNull
    public static UIInput createInputForSimpleProperty(PropertyDefinitionSimple propertyDefinitionSimple,
        PropertySimple propertySimple, ValueExpression propertyValueExpression, Integer listIndex,
        boolean configIsAggregate, boolean configReadOnly, boolean configFullyEditable, boolean prevalidate) {
        UIInput input = createInput(propertyDefinitionSimple);

        if (propertySimple != null)
            addTitleAttribute(input, propertySimple.getStringValue());

        boolean isUnset = isUnset(propertyDefinitionSimple, propertySimple, configIsAggregate);
        boolean isReadOnly = isReadOnly(propertyDefinitionSimple, propertySimple, configReadOnly, configFullyEditable);

        String propertyId = PropertyIdGeneratorUtility.getIdentifier(propertySimple, listIndex);
        input.setId(propertyId);

        input.setValueExpression("value", propertyValueExpression);

        FacesComponentUtility.setUnset(input, isUnset);
        FacesComponentUtility.setReadonly(input, isReadOnly);

        addValidatorsAndConverter(input, propertyDefinitionSimple, configReadOnly);

        addErrorMessages(input, propertyDefinitionSimple, propertySimple, prevalidate);
        return input;
    }

    public static UIInput createInput(PropertyDefinitionSimple propertyDefinitionSimple) {
        UIInput input;
        PropertySimpleType type = (propertyDefinitionSimple != null) ? propertyDefinitionSimple.getType()
            : PropertySimpleType.STRING;
        switch (type) {
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
            if (propertyDefinitionSimple != null && isEnum(propertyDefinitionSimple)) {
                input = createInputForEnumProperty(propertyDefinitionSimple);
            } else {
                input = createInputForStringProperty();
            }
        }
        }
        input.setId(UNIQUE_ID_PREFIX + UUID.randomUUID());

        if (propertyDefinitionSimple != null) {
            // It's important to set the label attribute, since we include it in our validation error messages.
            input.getAttributes().put("label", propertyDefinitionSimple.getDisplayName());

            // The below adds an inert attribute to the input that contains the name of the associated property - useful
            // for debugging (i.e. when viewing source of the page or using a JavaScript debugger).
            input.getAttributes().put("ondblclick", "//" + propertyDefinitionSimple.getName());
        }
        return input;
    }

    @NotNull
    public static UIInput createInputForSimpleProperty(PropertySimple propertySimple, ValueExpression valueExpression,
        boolean readOnly) {
        UIInput input = createInputForStringProperty();
        input.setValueExpression("value", valueExpression);
        FacesComponentUtility.setReadonly(input, readOnly);
        addTitleAttribute(input, propertySimple.getStringValue());
        return input;
    }

    public static void addMessageComponentForInput(UIComponent parent, UIInput input) {
        // <h:message for="#{input-component-id}" showDetail="true" errorClass="error-msg" />
        FacesComponentUtility.addMessage(parent, null, input.getId(), ERROR_MSG_STYLE_CLASS);
        // TODO: specify a component id
    }

    public static HtmlSelectBooleanCheckbox addUnsetControl(UIComponent parent, PropertyDefinitionSimple propertyDefinitionSimple,
        PropertySimple propertySimple, Integer listIndex, UIInput valueInput, boolean configIsAggregate, boolean configReadOnly,
        boolean configFullyEditable) {
        
        HtmlSelectBooleanCheckbox unsetCheckbox = FacesComponentUtility.createComponent(
            HtmlSelectBooleanCheckbox.class, null);
        if (propertySimple != null) {
            String unsetCheckboxId = PropertyIdGeneratorUtility.getIdentifier(propertySimple, listIndex, "Unset");
            unsetCheckbox.setId(unsetCheckboxId);
        }
        parent.getChildren().add(unsetCheckbox);
        unsetCheckbox.setValue(isUnset(propertyDefinitionSimple, propertySimple, configIsAggregate));
        if (isReadOnly(propertyDefinitionSimple, propertySimple, configReadOnly, configFullyEditable)
            || isAggregateWithDifferingValues(propertySimple, configIsAggregate)) {
            FacesComponentUtility.setDisabled(unsetCheckbox, true);
        } else {
            // Add JavaScript that will disable/enable the corresponding input element when the unset checkbox is
            // checked/unchecked.
            // IMPORTANT: We must use document.formName.inputName, rather than document.getElementById('inputId'),
            //            to reference the HTML DOM element, because the id of the HTML DOM input element is not the same as the
            //            id of the corresponding JSF input component in some cases (e.g. radio buttons). However, the
            //            name property that JSF renders on the HTML DOM input element does always match the JSF
            //            component id. (ips, 05/31/07)
            StringBuilder onchange = new StringBuilder();
            for (String htmlDomReference : getHtmlDomReferences(valueInput)) {
                onchange.append("setInputUnset(").append(htmlDomReference).append(", this.checked);");
            }
            unsetCheckbox.setOnchange(onchange.toString());
        }
        return unsetCheckbox;
    }

    public static void addInitInputsJavaScript(UIComponent parent, String componentId, boolean configFullyEditable,
        boolean postBack) {
        List<UIInput> inputs = FacesComponentUtility.getDescendantsOfType(parent, UIInput.class);
        List<UIInput> overrideInputs = new ArrayList<UIInput>();
        List<UIInput> unsetInputs = new ArrayList<UIInput>();
        List<UIInput> readOnlyInputs = new ArrayList<UIInput>();
        for (UIInput input : inputs) {
            // readOnly components can not be overridden - by this point, the override
            // status should have only been set if the component *was not* readOnly
            //if (FacesComponentUtility.isOverride(input)) {
            //    overrideInputs.add(input);
            //}

            if (postBack) {
                boolean inputIsNull = PropertySimpleValueConverter.NULL_INPUT_VALUE.equals(input.getSubmittedValue());
                FacesComponentUtility.setUnset(input, inputIsNull);
            }
            if (FacesComponentUtility.isUnset(input)) {
                unsetInputs.add(input);
            }

            if (!configFullyEditable && FacesComponentUtility.isReadonly(input)) {
                readOnlyInputs.add(input);
            }
        }

        StringBuilder script = new StringBuilder();

        if (!overrideInputs.isEmpty()) {
            script.append("var overrideInputArray = new Array(");
            for (UIInput input : overrideInputs) {
                for (String htmlDomReference : getHtmlDomReferences(input)) {
                    script.append(htmlDomReference).append(", ");
                }
            }

            script.delete(script.length() - 2, script.length()); // chop off the extra ", "
            script.append(");\n");

            // do it this way instead of DISABLED attribute via code, because if DISABLED is used and
            // even if javascript later enables them, JSF won't submit them as part of the component
            script.append("setInputsOverride(overrideInputArray, false);\n");
        }

        if (!unsetInputs.isEmpty()) {
            script.append("var unsetInputArray = new Array(");
            for (UIInput input : unsetInputs) {
                for (String htmlDomReference : getHtmlDomReferences(input)) {
                    script.append(htmlDomReference).append(", ");
                }
            }

            script.delete(script.length() - 2, script.length()); // chop off the extra ", "
            script.append(");\n");

            // do it this way instead of DISABLED attribute via code, because if DISABLED is used and
            // even if javascript later enables them, JSF won't submit them as part of the component
            script.append("unsetInputs(unsetInputArray);\n");
        }

        if (!readOnlyInputs.isEmpty()) {
            script.append("var readOnlyInputArray = new Array(");
            for (UIInput input : readOnlyInputs) {
                for (String htmlDomReference : getHtmlDomReferences(input)) {
                    script.append(htmlDomReference).append(", ");
                }
            }

            script.delete(script.length() - 2, script.length()); // chop off the extra ", "
            script.append(");\n");
            script.append("writeProtectInputs(readOnlyInputArray);\n");
        }

        UIOutput uiOutput = FacesComponentUtility.addJavaScript(parent, null, null, script);

        uiOutput.setId(componentId);
    }

    public static List<String> getHtmlDomReferences(UIComponent component) {
        List<String> htmlDomReferences = new ArrayList<String>();
        if (component instanceof HtmlSelectOneRadio) {
            String clientId = component.getClientId(FacesContext.getCurrentInstance());
            int selectItemCount = 0;
            for (UIComponent child : component.getChildren()) {
                if (child instanceof UISelectItem) {
                    String selectItemClientId = clientId + NamingContainer.SEPARATOR_CHAR + selectItemCount++;
                    htmlDomReferences.add(getHtmlDomReference(selectItemClientId));
                } else {
                    throw new IllegalStateException(
                        "HtmlSelectOneRadio component has a child that is not a UISelectItem.");
                }
            }
        } else {
            String clientId = component.getClientId(FacesContext.getCurrentInstance());
            htmlDomReferences.add(getHtmlDomReference(clientId));
        }

        return htmlDomReferences;
    }

    // <h:outputLabel value="DISPLAY_NAME" styleClass="..." />
    public static void addPropertyDisplayName(UIComponent parent, PropertyDefinition propertyDefinition,
        Property property, boolean configReadOnly) {
        String displayName = (propertyDefinition != null) ? propertyDefinition.getDisplayName() : property.getName();
        FacesComponentUtility.addOutputText(parent, null, displayName, CssStyleClasses.PROPERTY_DISPLAY_NAME_TEXT);
        if (!configReadOnly && propertyDefinition != null && propertyDefinition.isRequired()
            && (propertyDefinition instanceof PropertyDefinitionSimple)) {
            // Print a required marker next to required simples.
            // Ignore the required field for maps and lists, as it is has no significance for them.
            FacesComponentUtility.addOutputText(parent, null, " * ", CssStyleClasses.REQUIRED_MARKER_TEXT);
        }
    }

    public static void addPropertyDescription(UIComponent parent, PropertyDefinition propertyDefinition) {
        // <span class="description">DESCRIPTION</span>
        if (propertyDefinition.getDescription() == null || propertyDefinition.getDescription().trim().equals("")) {
            FacesComponentUtility
                .addOutputText(parent, null, "<No Description Available>", CssStyleClasses.DESCRIPTION);
        } else {
            FacesComponentUtility.addOutputText(parent, null, propertyDefinition.getDescription(),
                CssStyleClasses.DESCRIPTION);
        }
    }

    static String getHtmlDomReference(String clientId) {
        return "document.getElementById('" + clientId + "')";
    }

    private static UIInput createInputForBooleanProperty() {
        // <h:selectOneRadio id="#{identifier}" value="#{beanValue}" layout="pageDirection" styleClass="radiolabels">
        //    <f:selectItems value="#{itemValues}"></f:selectItems>
        // </h:selectOneRadio>
        HtmlSelectOneRadio selectOneRadio = FacesComponentUtility.createComponent(HtmlSelectOneRadio.class, null);
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
        inputText.setStyleClass(CssStyleClasses.PROPERTY_VALUE_INPUT);
        //      inputText.setStyle(INPUT_TEXT_WIDTH_STYLE_WITH_UNITS);
        inputText.setMaxlength(PropertySimple.MAX_VALUE_LENGTH);

        // Disable browser auto-completion.
        inputText.setAutocomplete("off");
        return inputText;
    }

    private static UIInput createInputForPasswordProperty() {
        HtmlInputSecret inputSecret = FacesComponentUtility.createComponent(HtmlInputSecret.class, null);
        inputSecret.setStyleClass(CssStyleClasses.PROPERTY_VALUE_INPUT);
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
        inputTextarea.setStyleClass(CssStyleClasses.PROPERTY_VALUE_INPUT);
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

    private static void addErrorMessages(UIInput input, @Nullable
    PropertyDefinitionSimple propertyDefinitionSimple, PropertySimple propertySimple, boolean prevalidate) {
        if (prevalidate) {
            // Pre-validate the property's value, in case the PC sent us an invalid live config.
            PropertySimpleValueValidator validator = new PropertySimpleValueValidator(propertyDefinitionSimple);
            //PropertySimple propertySimple = this.propertyMap.getSimple(propertyDefinitionSimple.getName());
            prevalidatePropertyValue(input, propertySimple, validator);
        }
        // If there is a PC-detected error associated with the property, associate it with the input.
        addPluginContainerDetectedErrorMessage(input, propertySimple);
    }

    private static void prevalidatePropertyValue(UIInput propertyValueInput, PropertySimple propertySimple,
        PropertySimpleValueValidator validator) {
        FacesContext facesContext = FacesContextUtility.getFacesContext();
        try {
            String value = (propertySimple != null) ? propertySimple.getStringValue() : null;
            validator.validate(facesContext, propertyValueInput, value);
        } catch (ValidatorException e) {
            // NOTE: It's vital to pass the client id, *not* the component id, to addMessage().
            facesContext.addMessage(propertyValueInput.getClientId(facesContext), e.getFacesMessage());
        }
    }

    private static void addPluginContainerDetectedErrorMessage(UIInput input, PropertySimple propertySimple) {
        String errorMsg = (propertySimple != null) ? propertySimple.getErrorMessage() : null;
        if ((errorMsg != null) && !errorMsg.equals("")) {
            FacesContext facesContext = FacesContextUtility.getFacesContext();
            FacesMessage facesMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR, errorMsg, null);

            // NOTE: It's vital to pass the client id, *not* the component id, to addMessage().
            facesContext.addMessage(input.getClientId(facesContext), facesMsg);
        }
    }

    private static void addTitleAttribute(UIInput input, String propertyValue) {
        if (input instanceof HtmlInputText) {
            //         TODO: will this still work now that we use the style def'n "width:185px;" to define the input field width?
            // For text inputs with values that are too long to fit in the input text field, add a "title" attribute set to
            // the value, so the user can see the untruncated value via a tooltip.
            // (see http://jira.jboss.com/jira/browse/JBNADM-1608)
            HtmlInputText inputText = (HtmlInputText) input;
            if ((propertyValue != null) && (propertyValue.length() > INPUT_TEXT_COMPONENT_WIDTH))
                inputText.setTitle(propertyValue);
            inputText.setOnchange("setInputTitle(this)");
        }
    }

    private static boolean isUnset(PropertyDefinitionSimple propertyDefinitionSimple, PropertySimple propertySimple,
        boolean configIsAggregate) {
        if (isAggregateWithDifferingValues(propertySimple, configIsAggregate))
            // Properties from aggregate configs that have differing values should not be marked unset.
            return false;
        if (propertySimple == null)
            return false;
        return ((propertyDefinitionSimple == null || !propertyDefinitionSimple.isRequired()) && propertySimple
            .getStringValue() == null);
    }

    public static boolean isReadOnly(PropertyDefinitionSimple propertyDefinitionSimple, PropertySimple propertySimple,
        boolean configReadOnly, boolean configFullyEditable) {
        // A fully editable config overrides any other means of setting read-only.
        return (!configFullyEditable && (configReadOnly || ((propertyDefinitionSimple != null && propertyDefinitionSimple
            .isReadOnly()) && (propertySimple == null || !isInvalidRequiredProperty(propertyDefinitionSimple,
            propertySimple)))));
    }

    public static boolean isAggregateWithDifferingValues(PropertySimple propertySimple, boolean configIsAggregate) {
        return configIsAggregate && (propertySimple.getOverride() == null || !propertySimple.getOverride());
    }

    private static boolean isInvalidRequiredProperty(PropertyDefinitionSimple propertyDefinitionSimple,
        PropertySimple propertySimple) {
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
