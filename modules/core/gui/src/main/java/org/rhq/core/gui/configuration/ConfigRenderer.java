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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIParameter;
import javax.faces.component.UISelectItem;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.component.html.HtmlSelectOneRadio;
import javax.faces.context.FacesContext;
import javax.faces.render.Renderer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.richfaces.component.html.HtmlSimpleTogglePanel;

import org.rhq.core.clientapi.agent.configuration.ConfigurationUtility;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertyGroupDefinition;
import org.rhq.core.gui.RequestParameterNameConstants;
import org.rhq.core.gui.util.FacesComponentUtility;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.gui.util.PropertyIdGeneratorUtility;

/**
 * A renderer that renders a {@link ConfigUIComponent} component as XHTML.
 *
 * @author Ian Springer
 */
public class ConfigRenderer extends Renderer {
    protected static final String NOTE_PANEL_STYLE_CLASS = "note-panel";
    protected static final String PROPERTY_GROUP_HEADER_STYLE_CLASS = "BlockTitle";
    protected static final String PROPERTY_GROUP_BODY_STYLE_CLASS = "BlockContent";
    protected static final String UNGROUPED_PROPERTIES_STYLE_CLASS = "BlockContent";
    protected static final String REQUIRED_MARKER_TEXT_STYLE_CLASS = "required-marker-text";
    protected static final String GROUP_DESCRIPTION_PANEL_STYLE_CLASS = "group-description-panel";
    protected static final String GROUP_DESCRIPTION_TEXT_PANEL_STYLE_CLASS = "group-description-text-panel";

    private static final String JAVASCRIPT_INCLUDES = "\n<script type='text/javascript' src='/js/rhq.js'></script>\n\n";

    private final Log LOG = LogFactory.getLog(ConfigRenderer.class);

    /**
     * Decode request parameters for the given {@link ConfigUIComponent}.
     *
     * @param facesContext the JSF context for the current request
     * @param component    the {@link ConfigUIComponent} for which request parameters should be decoded
     */
    @Override
    public void decode(FacesContext facesContext, UIComponent component) {
        ConfigUIComponent config = (ConfigUIComponent) component;
        validateAttributes(config);
        String function = FacesContextUtility.getOptionalRequestParameter(RequestParameterNameConstants.FUNCTION_PARAM);
        if (function != null) {
            if (function.equals(AbstractPropertyBagUIComponentTreeFactory.DELETE_LIST_MEMBER_PROPERTY_FUNCTION)) {
                deleteListMemberProperty(config);
            } else if (function
                .equals(AbstractPropertyBagUIComponentTreeFactory.DELETE_OPEN_MAP_MEMBER_PROPERTY_FUNCTION)) {
                deleteOpenMapMemberProperty(config);
            }
        }
    }

    /**
     * Encode the beginning of the given {@link ConfigUIComponent}.
     *
     * @param facesContext the JSF context for the current request
     * @param component    the {@link ConfigUIComponent} to be encoded
     */
    @Override
    public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException {
        ConfigUIComponent config = (ConfigUIComponent) component;

        // Only create the child components the first time around (i.e. once per JSF lifecycle).
        if (config.getChildCount() == 0) {
            if ((config.getConfigurationDefinition() == null)
                || ((config.getConfiguration() != null) && config.getConfiguration().getMap().isEmpty())) {
                String styleClass = (config.getNullConfigurationStyle() == null) ? "ErrorBlock" : config
                    .getNullConfigurationStyle();
                HtmlPanelGroup messagePanel = FacesComponentUtility.addBlockPanel(config, config, styleClass);
                FacesComponentUtility.addVerbatimText(messagePanel, config.getNullConfigurationDefinitionMessage());
                return;
            }

            if (config.getConfiguration() == null) {
                HtmlPanelGroup messagePanel = FacesComponentUtility.addBlockPanel(config, config, "WarnBlock");
                FacesComponentUtility.addVerbatimText(messagePanel, config.getNullConfigurationMessage());
                return;
            }

            ConfigurationUtility.normalizeConfiguration(config.getConfiguration(), config.getConfigurationDefinition());

            FacesComponentUtility.addVerbatimText(config, JAVASCRIPT_INCLUDES);
            if (!config.isReadOnly()) {
                addRequiredNotationsKey(config);
            }

            if (config.getListName() != null) {
                if (config.getListIndex() == null) {
                    // No index specified means we should add a new map to the list.
                    config.setListIndex(addNewMap(config));
                }

                addListMemberProperty(config);
            } else {
                addConfiguration(config);
            }

            addInitInputsJavaScript(config);
        }
    }

    private void addListMemberProperty(ConfigUIComponent config) {
        AbstractPropertyBagUIComponentTreeFactory propertyListUIComponentTreeFactory = new MapInListUIComponentTreeFactory(
            config, config.getListName(), config.getListIndex());
        HtmlPanelGroup propertiesPanel = FacesComponentUtility.addBlockPanel(config, config,
            UNGROUPED_PROPERTIES_STYLE_CLASS);
        propertiesPanel.getChildren().add(propertyListUIComponentTreeFactory.createUIComponentTree(null));
    }

    private void deleteListMemberProperty(ConfigUIComponent config) {
        String listName = FacesContextUtility
            .getRequiredRequestParameter(RequestParameterNameConstants.LIST_NAME_PARAM);
        PropertyList propertyList = config.getConfiguration().getList(listName);
        if (propertyList == null) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Configuration does not contain a list named '"
                + listName + "'.");
            return;
        }

        int listIndex = FacesContextUtility.getRequiredRequestParameter(RequestParameterNameConstants.LIST_INDEX_PARAM,
            Integer.class);
        if (listIndex >= propertyList.getList().size()) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "List '" + listName
                + "' does not contain an item at index " + listIndex + ".");
            return;
        }

        Property memberProperty = propertyList.getList().get(listIndex);
        String memberComponentId = PropertyIdGeneratorUtility.getIdentifier(memberProperty, listIndex,
            AbstractPropertyBagUIComponentTreeFactory.PANEL_ID_SUFFIX);
        UIComponent listMemberComponent = config.findComponent(memberComponentId);
        if (listMemberComponent == null) {
            throw new IllegalStateException("JSF component with id '" + memberComponentId + "' not found for list '"
                + listName + "' item " + listIndex + ".");
        }

        FacesComponentUtility.detachComponent(listMemberComponent);
        correctListIndexesInSiblingMembers(config, propertyList, listIndex);
        propertyList.getList().remove(listIndex);
        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Property at index " + listIndex
            + " deleted from list '" + listName + "'.");
    }

    private void correctListIndexesInSiblingMembers(ConfigUIComponent config, PropertyList propertyList,
        int deletedMemberIndex) {
        for (int oldIndex = deletedMemberIndex + 1; oldIndex < propertyList.getList().size(); oldIndex++) {
            int newIndex = oldIndex - 1;
            String listMemberComponentId = PropertyIdGeneratorUtility.getIdentifier(propertyList.getList()
                .get(oldIndex), oldIndex, AbstractPropertyBagUIComponentTreeFactory.PANEL_ID_SUFFIX);
            UIComponent listMemberComponent = config.findComponent(listMemberComponentId);
            String newListMemberComponentId = PropertyIdGeneratorUtility.getIdentifier(propertyList.getList().get(
                newIndex), newIndex, AbstractPropertyBagUIComponentTreeFactory.PANEL_ID_SUFFIX);
            listMemberComponent.setId(newListMemberComponentId);
            String listIndexParamComponentId = PropertyIdGeneratorUtility.getIdentifier(propertyList.getList().get(
                oldIndex), oldIndex, AbstractPropertyBagUIComponentTreeFactory.PARAM_ID_SUFFIX);
            UIParameter listIndexParam = (UIParameter) config.findComponent(listIndexParamComponentId);
            String newListIndexParamComponentId = PropertyIdGeneratorUtility.getIdentifier(propertyList.getList().get(
                newIndex), newIndex, AbstractPropertyBagUIComponentTreeFactory.PARAM_ID_SUFFIX);
            listIndexParam.setId(newListIndexParamComponentId);
            listIndexParam.setValue(newIndex);
        }
    }

    private void deleteOpenMapMemberProperty(ConfigUIComponent config) {
        String mapName = FacesContextUtility.getRequiredRequestParameter(RequestParameterNameConstants.MAP_NAME_PARAM);
        PropertyMap propertyMap = config.getConfiguration().getMap(mapName);
        if (propertyMap == null) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Configuration does not contain a map named '"
                + mapName + "'.");
            return;
        }

        String memberName = FacesContextUtility
            .getRequiredRequestParameter(RequestParameterNameConstants.MEMBER_NAME_PARAM);
        if (!propertyMap.getMap().containsKey(memberName)) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Map '" + mapName
                + "' does not contain a member property named '" + memberName + "'.");
            return;
        }

        Property memberProperty = propertyMap.getMap().get(memberName);
        String memberComponentId = PropertyIdGeneratorUtility.getIdentifier(memberProperty, null,
            AbstractPropertyBagUIComponentTreeFactory.PANEL_ID_SUFFIX);
        UIComponent mapMemberComponent = config.findComponent(memberComponentId);
        if (mapMemberComponent == null) {
            throw new IllegalStateException("JSF component with id '" + memberComponentId + "' not found for map '"
                + mapName + "' member property '" + memberName + "'.");
        }

        FacesComponentUtility.detachComponent(mapMemberComponent);
        propertyMap.getMap().remove(memberName);
        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Property '" + memberName + "' deleted from map '"
            + mapName + "'.");
    }

    private void addRequiredNotationsKey(ConfigUIComponent config) {
        addDebug(config, true, ".addNotePanel()");
        HtmlPanelGroup footnotesPanel = FacesComponentUtility.addBlockPanel(config, config, NOTE_PANEL_STYLE_CLASS);
        FacesComponentUtility.addOutputText(footnotesPanel, config, "*", REQUIRED_MARKER_TEXT_STYLE_CLASS);
        FacesComponentUtility.addOutputText(footnotesPanel, config, " denotes a required field.",
            FacesComponentUtility.NO_STYLE_CLASS);
        if (config.isAggregate()) {
            HtmlPanelGroup overridePanel = FacesComponentUtility.addBlockPanel(config, config, NOTE_PANEL_STYLE_CLASS);
            FacesComponentUtility.addOutputText(overridePanel, config,
                "note: if override is not checked, that property will not be altered on any group members",
                FacesComponentUtility.NO_STYLE_CLASS);
        }

        addDebug(config, false, ".addNotePanel()");
    }

    private void addConfiguration(ConfigUIComponent config) {
        addNonGroupedProperties(config);
        addGroupedProperties(config);
    }

    private void addNonGroupedProperties(ConfigUIComponent config) {
        addDebug(config, true, ".addNonGroupedProperties()");
        HtmlPanelGroup propertiesPanel = FacesComponentUtility.addBlockPanel(config, config,
            UNGROUPED_PROPERTIES_STYLE_CLASS);
        AbstractPropertyBagUIComponentTreeFactory propertyListUIComponentTreeFactory = new GroupUIComponentTreeFactory(
            config, GroupUIComponentTreeFactory.NO_GROUP);
        propertiesPanel.getChildren().add(propertyListUIComponentTreeFactory.createUIComponentTree(null));
        addDebug(config, false, ".addNonGroupedProperties()");
    }

    private void addGroupedProperties(ConfigUIComponent config) {
        addDebug(config, true, ".addGroupedProperties()");
        List<PropertyGroupDefinition> groups = config.getConfigurationDefinition().getGroupDefinitions();
        for (PropertyGroupDefinition group : groups) {
            HtmlSimpleTogglePanel groupPanel = addGroupPanel(config, group);
            AbstractPropertyBagUIComponentTreeFactory propertyListUIComponentTreeFactory = new GroupUIComponentTreeFactory(
                config, group.getName());
            groupPanel.getChildren().add(propertyListUIComponentTreeFactory.createUIComponentTree(null));
        }

        addDebug(config, false, ".addGroupedProperties()");
    }

    private HtmlSimpleTogglePanel addGroupPanel(ConfigUIComponent config, PropertyGroupDefinition group) {
        addDebug(config, true, ".addGroupPanel()");
        HtmlSimpleTogglePanel groupPanel = FacesComponentUtility.addSimpleTogglePanel(config, config, null);
        groupPanel.setOpened(!group.isDefaultHidden());
        groupPanel.setHeaderClass(PROPERTY_GROUP_HEADER_STYLE_CLASS);
        groupPanel.setBodyClass(PROPERTY_GROUP_BODY_STYLE_CLASS);

        // Custom header that includes the name and description
        HtmlPanelGroup headerPanel = FacesComponentUtility.createBlockPanel(config, null);
        FacesComponentUtility.addOutputText(headerPanel, config, group.getDisplayName(), null);
        FacesComponentUtility
            .addOutputText(headerPanel, config, group.getDescription(), "group-description-text-panel");
        groupPanel.getFacets().put("header", headerPanel);

        // custom "close" widget
        HtmlPanelGroup closePanel = FacesComponentUtility.createBlockPanel(config, null);
        closePanel.setStyle("text-align: right; font-weight: normal; font-size: 0.8em; whitespace: nowrap;");
        FacesComponentUtility.addGraphicImage(closePanel, config, "/images/ico_trigger_wht_collapse.gif", "collapse");
        FacesComponentUtility.addOutputText(closePanel, config, " Collapse", null);
        groupPanel.getFacets().put("closeMarker", closePanel);

        // custom "open" widget
        HtmlPanelGroup openPanel = FacesComponentUtility.createBlockPanel(config, null);
        openPanel.setStyle("text-align: right; font-weight: normal; font-size: 0.8em; whitespace: nowrap;");
        FacesComponentUtility.addGraphicImage(openPanel, config, "/images/ico_trigger_wht_expand.gif", "expand");
        FacesComponentUtility.addOutputText(openPanel, config, " Expand", null);
        groupPanel.getFacets().put("openMarker", openPanel);
        addDebug(config, true, ".addGroupPanel()");

        return groupPanel;
    }

    private void addInitInputsJavaScript(ConfigUIComponent config) {
        List<UIInput> inputs = FacesComponentUtility.getDescendantsOfType(config, UIInput.class);
        List<UIInput> overrideInputs = new ArrayList<UIInput>();
        List<UIInput> unsetInputs = new ArrayList<UIInput>();
        List<UIInput> readOnlyInputs = new ArrayList<UIInput>();
        for (UIInput input : inputs) {
            // readOnly components can not be overridden - by this point, the override
            // status should have only been set if the component *was not* readOnly
            if (FacesComponentUtility.isOverride(input)) {
                overrideInputs.add(input);
            }

            if (FacesComponentUtility.isUnset(input)) {
                unsetInputs.add(input);
            }

            if (!config.isFullyEditable() && FacesComponentUtility.isReadonly(input)) {
                readOnlyInputs.add(input);
            }
        }

        if (!overrideInputs.isEmpty()) {
            StringBuilder script = new StringBuilder();
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
            script.append("setInputsOverride(overrideInputArray, false);");
            FacesComponentUtility.addJavaScript(config, config, null, script);
        }

        if (!unsetInputs.isEmpty()) {
            StringBuilder script = new StringBuilder();
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
            script.append("unsetInputs(unsetInputArray);");
            FacesComponentUtility.addJavaScript(config, config, null, script);
        }

        if (!readOnlyInputs.isEmpty()) {
            StringBuilder script = new StringBuilder();
            script.append("var readOnlyInputArray = new Array(");
            for (UIInput input : readOnlyInputs) {
                for (String htmlDomReference : getHtmlDomReferences(input)) {
                    script.append(htmlDomReference).append(", ");
                }
            }

            script.delete(script.length() - 2, script.length()); // chop off the extra ", "
            script.append(");\n");
            script.append("writeProtectInputs(readOnlyInputArray);");
            FacesComponentUtility.addJavaScript(config, config, null, script);
        }
    }

    static List<String> getHtmlDomReferences(UIComponent component) {
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

    static String getHtmlDomReference(String clientId) {
        return "document.getElementById('" + clientId + "')";
    }

    private void validateAttributes(ConfigUIComponent config) {
        if (config.getValueExpression("configurationDefinition") == null) {
            throw new IllegalStateException("The " + config.getClass().getName()
                + " component requires a 'configurationDefinition' attribute.");
        }

        if (config.getValueExpression("configuration") == null) {
            throw new IllegalStateException("The " + config.getClass().getName()
                + " component requires a 'configuration' attribute.");
        }
    }

    private int addNewMap(ConfigUIComponent config) {
        String listName = config.getListName();
        PropertyDefinitionMap mapDefinition = (PropertyDefinitionMap) config.getConfigurationDefinition()
            .getPropertyDefinitionList(listName).getMemberDefinition();
        String mapName = mapDefinition.getName();
        PropertyMap newMap = new PropertyMap(mapName);
        Map<String, PropertyDefinition> mapMemberDefinitions = mapDefinition.getPropertyDefinitions();
        for (PropertyDefinition mapMemberDefinition : mapMemberDefinitions.values()) {
            PropertyDefinitionSimple simpleDefinition = (PropertyDefinitionSimple) mapMemberDefinition;
            newMap.put(new PropertySimple(simpleDefinition.getName(), (simpleDefinition.isRequired()) ? "" : null));
        }

        PropertyList list = config.getConfiguration().getList(listName);
        list.add(newMap);
        return list.getList().size() - 1;
    }

    /**
     * simple method that will add debugging HTML comments so that I can figure out which part of the HTML is generated
     * by which part of this class
     *
     * @param component  the component this comment should be added
     * @param start      true if this is the "START" comment, false if it is the "END" comment
     * @param methodName the name of the method this is calling from
     */

    private void addDebug(UIComponent component, boolean start, String methodName) {
        if (LOG.isDebugEnabled()) {
            StringBuilder msg = new StringBuilder("\n<!--");
            msg.append(start ? " START " : " END ");
            msg.append(this.getClass().getSimpleName());
            msg.append(methodName);
            msg.append(" -->\n ");
            FacesComponentUtility.addVerbatimText(component, msg);
        }
    }
}