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
package org.rhq.core.gui.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.el.ValueExpression;
import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIOutput;
import javax.faces.component.UIParameter;
import javax.faces.component.UIForm;
import javax.faces.component.html.HtmlColumn;
import javax.faces.component.html.HtmlCommandButton;
import javax.faces.component.html.HtmlCommandLink;
import javax.faces.component.html.HtmlDataTable;
import javax.faces.component.html.HtmlForm;
import javax.faces.component.html.HtmlGraphicImage;
import javax.faces.component.html.HtmlMessage;
import javax.faces.component.html.HtmlOutputLabel;
import javax.faces.component.html.HtmlOutputLink;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.component.html.HtmlPanelGrid;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.richfaces.component.html.HtmlSeparator;
import org.richfaces.component.html.HtmlSimpleTogglePanel;
import org.richfaces.component.html.HtmlRichMessage;

 /**
 * A set of utility methods for working with JSF {@link UIComponent}s.
 *
 * @author Ian Springer
 */
public abstract class FacesComponentUtility {
    public static final String NO_STYLE_CLASS = null;

    private static final String DISABLED_ATTRIBUTE_NAME = "disabled";
    private static final String READONLY_ATTRIBUTE = "readonly";
    private static final String UNSET_ATTRIBUTE = "unset";
    private static final String OVERRIDE_ATTRIBUTE = "override";

    @NotNull
    public static HtmlPanelGroup createBlockPanel(FacesComponentIdFactory idFactory, String styleClass) {
        HtmlPanelGroup panel = createComponent(HtmlPanelGroup.class, idFactory);
        panel.setLayout("block");
        panel.setStyleClass(styleClass);
        return panel;
    }

    @NotNull
    public static HtmlPanelGroup addBlockPanel(@NotNull
    UIComponent parent, FacesComponentIdFactory idFactory, String styleClass) {
        HtmlPanelGroup panel = createBlockPanel(idFactory, styleClass);
        parent.getChildren().add(panel);
        return panel;
    }

    @NotNull
    public static HtmlPanelGroup addInlinePanel(@NotNull
    UIComponent parent, FacesComponentIdFactory idFactory, String styleClass) {
        HtmlPanelGroup panel = createComponent(HtmlPanelGroup.class, idFactory);
        panel.setStyleClass(styleClass);
        parent.getChildren().add(panel);
        return panel;
    }

    @NotNull
    public static HtmlSimpleTogglePanel addSimpleTogglePanel(@NotNull
    UIComponent parent, FacesComponentIdFactory idFactory, String label) {
        HtmlSimpleTogglePanel panel = createComponent(HtmlSimpleTogglePanel.class, idFactory);
        panel.setLabel(label);

        //panel.setAjaxSingle(true);
        panel.setSwitchType("client");
        parent.getChildren().add(panel);
        return panel;
    }

    @NotNull
    public static HtmlSeparator addSeparator(@NotNull
    UIComponent parent, FacesComponentIdFactory idFactory) {
        HtmlSeparator separator = createComponent(HtmlSeparator.class, idFactory);
        parent.getChildren().add(separator);
        return separator;
    }

    @NotNull
    public static HtmlOutputText addOutputText(@NotNull
    UIComponent parent, FacesComponentIdFactory idFactory, CharSequence value, String styleClass) {
        HtmlOutputText text = createComponent(HtmlOutputText.class, idFactory);
        text.setValue(value);
        text.setStyleClass(styleClass);
        parent.getChildren().add(text);
        return text;
    }

    @NotNull
    public static HtmlOutputText createOutputText(FacesComponentIdFactory idFactory, CharSequence value,
        String styleClass) {
        HtmlOutputText text = createComponent(HtmlOutputText.class, idFactory);
        if (value != null)
            value = value.toString().trim();
        text.setValue(value);
        text.setStyleClass(styleClass);
        return text;
    }

    @NotNull
    public static HtmlColumn addColumn(@NotNull
    UIComponent parent, FacesComponentIdFactory idFactory, CharSequence headerText, String headerStyle) {
        HtmlColumn column = createComponent(HtmlColumn.class, idFactory);
        HtmlOutputText header = new HtmlOutputText();
        header.setValue(headerText);
        header.setStyleClass(headerStyle);
        column.setHeader(header);
        parent.getChildren().add(column);
        return column;
    }

    @NotNull
    public static HtmlOutputText addVerbatimText(@NotNull
    UIComponent parent, CharSequence html) {
        // NOTE: Do NOT set the id on the HtmlOutputText, otherwise it will be rendered as a span tag!
        HtmlOutputText outputText = new HtmlOutputText();
        outputText.setEscape(false);
        outputText.setValue(html);
        parent.getChildren().add(outputText);
        return outputText;
    }

    @NotNull
    public static UIOutput addButton(UIComponent parent, String label, String styleClass) {
        return FacesComponentUtility.addVerbatimText(parent, "<button class=\"" + styleClass + "\">" + label
            + "</button>");
    }

    @NotNull
    public static UIOutput addJavaScript(@NotNull
    UIComponent parent, FacesComponentIdFactory idFactory, @Nullable
    CharSequence src, @Nullable
    CharSequence script) {
        UIOutput output = createComponent(UIOutput.class, idFactory);
        output.getAttributes().put("escape", Boolean.FALSE);
        StringBuilder value = new StringBuilder();
        value.append("\n<script language=\"JavaScript\" type=\"text/javascript\"");
        if (src != null) {
            value.append(" src=\"").append(src).append("\"");
        }

        value.append(">");
        if (script != null) {
            value.append("\n").append(script).append("\n");
        }

        value.append("</script>\n");
        output.setValue(value);
        parent.getChildren().add(output);
        return output;
    }

    @NotNull
    public static <T extends UIComponent> List<T> getDescendantsOfType(UIComponent component, Class<T> componentClass) {
        List<T> results = new ArrayList<T>();
        getDescendantsOfType(component, componentClass, results);
        return results;
    }

    @SuppressWarnings("unchecked")
    private static <T extends UIComponent> void getDescendantsOfType(UIComponent component, Class<T> componentClass,
        List<T> results) {
        for (int i = 0; i < component.getChildren().size(); i++) {
            UIComponent child = component.getChildren().get(i);
            if (componentClass.isAssignableFrom(child.getClass())) {
                results.add((T) child);
            }

            getDescendantsOfType(child, componentClass, results);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends UIComponent> T getAncestorOfType(UIComponent component, Class<T> componentClass) {
        if (component == null) {
            return null;
        }

        if (componentClass.isAssignableFrom(component.getClass())) {
            return (T) component;
        } else {
            return getAncestorOfType(component.getParent(), componentClass);
        }
    }

    @NotNull
    public static HtmlOutputLabel addLabel(@NotNull
    UIComponent parent, FacesComponentIdFactory idFactory, UIInput associatedInput, String value, String styleClass) {
        HtmlOutputLabel label = createComponent(HtmlOutputLabel.class, idFactory);
        label.setFor(associatedInput.getId());
        label.setValue(value);
        label.setStyleClass(styleClass);
        parent.getChildren().add(label);
        return label;
    }

    @NotNull
    public static HtmlGraphicImage addGraphicImage(@NotNull
    UIComponent parent, FacesComponentIdFactory idFactory, String url, String alt) {
        HtmlGraphicImage image = FacesComponentUtility.createComponent(HtmlGraphicImage.class, idFactory);
        image.setUrl(url);
        image.setAlt(alt);
        image.setTitle(alt);
        parent.getChildren().add(image);
        return image;
    }

    @NotNull
    public static HtmlOutputLink addOutputLink(@NotNull
    UIComponent parent, FacesComponentIdFactory idFactory, String value) {
        HtmlOutputLink link = FacesComponentUtility.createComponent(HtmlOutputLink.class, idFactory);
        link.setValue(value);
        parent.getChildren().add(link);
        return link;
    }

    @NotNull
    public static UIParameter addParameter(@NotNull
    UIComponent parent, FacesComponentIdFactory idFactory, String name, String value) {
        UIParameter parameter = createComponent(UIParameter.class, idFactory);
        parameter.setName(name);
        parameter.setValue(value);
        parent.getChildren().add(parameter);
        return parameter;
    }

    @NotNull
    public static HtmlForm addForm(@NotNull
    UIComponent parent, FacesComponentIdFactory idFactory) {
        HtmlForm form = createComponent(HtmlForm.class, idFactory);
        form.setPrependId(false);
        parent.getChildren().add(form);
        return form;
    }

    @NotNull
    public static HtmlCommandButton addCommandButton(@NotNull
    UIComponent parent, FacesComponentIdFactory idFactory, String value, String styleClass) {
        HtmlCommandButton button = createComponent(HtmlCommandButton.class, idFactory);
        button.setValue(value);
        button.setStyleClass(styleClass);
        parent.getChildren().add(button);
        return button;
    }

    @NotNull
    public static HtmlCommandLink addCommandLink(@NotNull
    UIComponent parent, FacesComponentIdFactory idFactory) {
        HtmlCommandLink link = FacesComponentUtility.createComponent(HtmlCommandLink.class, idFactory);
        parent.getChildren().add(link);
        return link;
    }

    @NotNull
    public static HtmlMessage addMessage(@NotNull
    UIComponent parent, FacesComponentIdFactory idFactory, String associatedComponentId, String styleClass) {
        HtmlMessage message = FacesComponentUtility.createComponent(HtmlMessage.class, idFactory);
        message.setFor(associatedComponentId);
        message.setShowDetail(true);
        message.setErrorClass(styleClass);
        message.setWarnClass(styleClass);
        message.setFatalClass(styleClass);
        message.setInfoClass(styleClass);
        parent.getChildren().add(message);
        return message;
    }

    @NotNull
    public static HtmlPanelGrid addPanelGrid(@NotNull
    UIComponent parent, FacesComponentIdFactory idFactory, int columns, String styleClass) {
        HtmlPanelGrid panelGrid = FacesComponentUtility.createComponent(HtmlPanelGrid.class, idFactory);
        panelGrid.setColumns(columns);
        panelGrid.setStyleClass(styleClass);
        parent.getChildren().add(panelGrid);
        return panelGrid;
    }

    @NotNull
    public static HtmlDataTable addDataTable(@NotNull
    UIComponent parent, FacesComponentIdFactory idFactory, String styleClass) {
        HtmlDataTable dataTable = FacesComponentUtility.createComponent(HtmlDataTable.class, idFactory);
        dataTable.setStyleClass(styleClass);
        parent.getChildren().add(dataTable);
        return dataTable;
    }

    @Nullable
    public static String getExpressionAttribute(@NotNull
    UIComponent component, @NotNull
    String attribName) {
        return getExpressionAttribute(component, attribName, String.class);
    }

    @Nullable
    public static <T> T getExpressionAttribute(@NotNull
    UIComponent component, @NotNull
    String attribName, @NotNull
    Class<T> expectedType) {
        ValueExpression valueExpression = component.getValueExpression(attribName);
        T attribValue = (valueExpression != null) ? FacesExpressionUtility.getValue(valueExpression, expectedType)
            : null;
        return attribValue;
    }

    @NotNull
    public static Map<String, String> getParameters(@NotNull
    UIComponent component) {
        // Use a LinkedHashMap, so the order of the parameters is maintained.
        Map<String, String> params = new LinkedHashMap<String, String>();
        List<UIComponent> children = component.getChildren();
        for (UIComponent child : children) {
            if (child instanceof UIParameter) {
                UIParameter param = (UIParameter) child;
                Object paramValue = param.getValue();
                params.put(param.getName(), (paramValue != null) ? paramValue.toString() : null);
            }
        }

        return params;
    }

    /**
     * Creates a {@link UIComponent} of the specified type.
     *
     * @param  componentClass the type of {@link UIComponent} to create
     *
     * @return a {@link UIComponent} of the specified type
     */
    public static <T extends UIComponent> T createComponent(Class<T> componentClass) {
        return createComponent(componentClass, null);
    }

    /**
     * Creates a {@link UIComponent} of the specified type.
     *
     * @param  componentClass the type of {@link UIComponent} to create
     * @param  idFactory      a factory to use to create a unique id for the new component
     *
     * @return a {@link UIComponent} of the specified type
     */
    @SuppressWarnings("unchecked")
    public static <T extends UIComponent> T createComponent(Class<T> componentClass, @Nullable
    FacesComponentIdFactory idFactory) {
        Application application = FacesContext.getCurrentInstance().getApplication();
        String componentType = getComponentType(componentClass);
        T component = (T) application.createComponent(componentType);
        if (idFactory == null)
            idFactory = new DefaultFacesComponentIdFactory();
        component.setId(idFactory.createUniqueId());
        return component;
    }

    /**
     * Sets the "disabled" attribute on the specified component to the provided value. NOTE: Not all components will
     * render this attribute (e.g. HtmlInputHidden does not render it).
     *
     * @param component a component
     * @param disabled  the new value for the "disabled" attribute
     */
    public static void setDisabled(UIComponent component, boolean disabled) {
        component.getAttributes().put(DISABLED_ATTRIBUTE_NAME, disabled);
    }

    /**
     * Gets the value of the "disabled" attribute on the specified component, or <code>false</code> if the component does
     * not expose the attribute.
     *
     * @param  component a component
     *
     * @return the value of the "disabled" attribute on the specified component, or <code>false</code> if the component
     *         does not expose the attribute
     */
    public static boolean isDisabled(UIComponent component) {
        Boolean disabled = (Boolean) component.getAttributes().get(DISABLED_ATTRIBUTE_NAME);
        return (disabled != null) && disabled;
    }

    /**
     * Sets the "readonly" attribute on the specified component to the provided value. NOTE: Only certain components
     * will render this attribute (e.g. HtmlInputText, HtmlInputSecret, and HtmlInputTextarea render it).
     *
     * @param input    an input component
     * @param readonly the new value for the "readonly" attribute
     */
    public static void setReadonly(UIInput input, boolean readonly) {
        input.getAttributes().put(READONLY_ATTRIBUTE, readonly);
    }

    /**
     * Gets the value of the "readonly" attribute on the specified component, or <code>false</code> if the component does
     * not expose the attribute.
     *
     * @param  component a component
     *
     * @return the value of the "readonly" attribute on the specified component, or <code>false</code> if the component
     *         does not expose the attribute
     */
    public static boolean isReadonly(UIComponent component) {
        Boolean readonly = (Boolean) component.getAttributes().get(READONLY_ATTRIBUTE);
        return (readonly != null) && readonly;
    }

    /**
     * Sets the "unset" attribute on the specified component to the provided value. NOTE: This is a RHQ-specific
     * attribute, which is only rendered for the Config component.
     *
     * @param input an input component
     * @param unset the new value for the "unset" attribute
     */
    public static void setUnset(UIInput input, boolean unset) {
        input.getAttributes().put(UNSET_ATTRIBUTE, unset);
    }

    /**
     * Gets the value of the "unset" attribute on the specified component, or <code>false</code> if the component does
     * not expose the attribute.
     *
     * @param  component a component
     *
     * @return the value of the "unset" attribute on the specified component, or <code>false</code> if the component does
     *         not expose the attribute
     */
    public static boolean isUnset(UIComponent component) {
        Boolean unset = (Boolean) component.getAttributes().get(UNSET_ATTRIBUTE);
        return (unset != null) && unset;
    }

    public static void setOverride(UIInput input, boolean override) {
        input.getAttributes().put(OVERRIDE_ATTRIBUTE, override);
    }

    public static boolean isOverride(UIComponent component) {
        Boolean override = (Boolean) component.getAttributes().get(OVERRIDE_ATTRIBUTE);
        return (override == null || override);
    }

    /**
     * Removes the specified component from its parent if it has one.
     *
     * @param component a JSF component
     */
    public static void detachComponent(UIComponent component) {
        UIComponent parent = component.getParent();
        if (parent != null) {
            Iterator<UIComponent> children = parent.getChildren().iterator();
            while (children.hasNext()) {
                UIComponent child = children.next();
                if ((child.getId() != null) && child.getId().equals(component.getId())) {
                    children.remove(); // remove the last child returned by next()
                    break;
                }
            }
        }
    }

    /**
     * Returns the enclosing UIForm component, if any, from the specified component's ancestry.
     *
     * @param component the component
     *
     * @return the enclosing UIForm component, if any, from the specified component's ancestry
     *
     * @throws IllegalArgumentException if more than one UIForm is found in the component's ancestry
     */
    @Nullable
     public static UIForm getEnclosingForm(UIComponent component)
         throws IllegalArgumentException
     {
         UIForm ret = null;
         while (component != null)
         {
             if (component instanceof UIForm)
             {
                 if (ret != null)
                 {
                     // Cannot have a doubly-nested form!
                     throw new IllegalArgumentException();
                 }
                 ret = (UIForm) component;
             }
             component = component.getParent();
         }
         return ret;
     }

    private static <T extends UIComponent> String getComponentType(Class<T> componentClass) {
        String componentType;
        try {
            componentType = (String) componentClass.getDeclaredField("COMPONENT_TYPE").get(null);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        return componentType;
    }

     private static class DefaultFacesComponentIdFactory implements FacesComponentIdFactory {
        public String createUniqueId()
        {
            return "rhq_" + UUID.randomUUID();
        }
    }
}