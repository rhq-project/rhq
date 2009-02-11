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
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIParameter;
import javax.faces.component.UIForm;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.component.html.HtmlOutputLink;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;
import javax.el.ValueExpression;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.rhq.core.gui.configuration.helper.PropertyRenderingUtility;
import org.rhq.core.gui.configuration.propset.ConfigurationSetComponent;
import org.rhq.core.gui.configuration.propset.PropertySetComponent;
import org.rhq.core.gui.configuration.propset.ConfigurationSet;
import org.rhq.core.gui.util.FacesComponentUtility;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.gui.util.PropertyIdGeneratorUtility;
import org.rhq.core.gui.util.FacesExpressionUtility;
import org.richfaces.component.html.HtmlSimpleTogglePanel;
import org.richfaces.component.html.HtmlModalPanel;
import org.ajax4jsf.component.html.HtmlAjaxCommandLink;

/**
 * A renderer that renders an {@link AbstractConfigurationComponent} component as XHTML.
 *
 * @author Ian Springer
 */
public class ConfigRenderer extends Renderer {
    public static final String PROPERTY_SET_COMPONENT_ID = "rhq_propSet";

    protected static final String NOTE_PANEL_STYLE_CLASS = "note-panel";
    protected static final String PROPERTY_GROUP_HEADER_STYLE_CLASS = "BlockTitle";
    protected static final String PROPERTY_GROUP_BODY_STYLE_CLASS = "BlockContent";
    protected static final String UNGROUPED_PROPERTIES_STYLE_CLASS = "BlockContent";
    protected static final String REQUIRED_MARKER_TEXT_STYLE_CLASS = "required-marker-text";
    protected static final String GROUP_DESCRIPTION_PANEL_STYLE_CLASS = "group-description-panel";
    protected static final String GROUP_DESCRIPTION_TEXT_PANEL_STYLE_CLASS = "group-description-text-panel";

    private static final String JAVASCRIPT_INCLUDES = "\n<script type='text/javascript' src='/js/rhq.js'></script>\n\n";

    private final Log LOG = LogFactory.getLog(ConfigRenderer.class);
    private static final String INIT_INPUTS_JAVA_SCRIPT_COMPONENT_ID_SUFFIX = "-initInputsJavaScript";

    /**
     * Decode request parameters for the given {@link ConfigUIComponent}.
     *
     * @param facesContext the JSF context for the current request
     * @param component    the {@link ConfigUIComponent} for which request parameters should be decoded
     */
    @Override
    public void decode(FacesContext facesContext, UIComponent component) {
        AbstractConfigurationComponent configurationComponent = (AbstractConfigurationComponent) component;
        validateAttributes(configurationComponent);
        
        String function = FacesContextUtility.getOptionalRequestParameter(RequestParameterNameConstants.FUNCTION_PARAM);
        if (function != null) {
            if (function.equals(AbstractPropertyBagUIComponentTreeFactory.DELETE_LIST_MEMBER_PROPERTY_FUNCTION)) {
                deleteListMemberProperty(configurationComponent);
            } else if (function
                .equals(AbstractPropertyBagUIComponentTreeFactory.DELETE_OPEN_MAP_MEMBER_PROPERTY_FUNCTION)) {
                deleteOpenMapMemberProperty(configurationComponent);
            }
        }

        if (configurationComponent.getConfiguration() != null) {
            String id = getInitInputsJavaScriptComponentId(configurationComponent);
            UIComponent initInputsJavaScriptComponent = configurationComponent.findComponent(id);
            if (initInputsJavaScriptComponent != null) {
                FacesComponentUtility.detachComponent(initInputsJavaScriptComponent);
                PropertyRenderingUtility.addInitInputsJavaScript(configurationComponent, id, configurationComponent.isFullyEditable(), true);
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
        AbstractConfigurationComponent configurationComponent = (AbstractConfigurationComponent) component;

        // If it's an AJAX request for this component, recalculate the aggregate config from the member configs
        // and clear our child components. NOTE: This can *not* be done in decode(), because the model
        // will not have been updated yet with any changes made to child UIInput values.
        String refresh = FacesContextUtility.getOptionalRequestParameter("refresh");
        if (refresh != null && refresh.equals(configurationComponent.getId())) {
            if (configurationComponent instanceof ConfigurationSetComponent) {
                ConfigurationSet configurationSet = ((ConfigurationSetComponent)configurationComponent).getConfigurationSet();
                //noinspection ConstantConditions
                configurationSet.calculateAggregateConfiguration();
            }
            component.getChildren().clear();
        }

        ResponseWriter writer = facesContext.getResponseWriter();
        String clientId = component.getClientId(facesContext);
        writer.write('\n');
        writer.writeComment("********** Start of " + component.getClass().getSimpleName() + " component **********");
        writer.startElement("div", component);
        writer.writeAttribute("id", clientId, "clientId");

        // Only create the child components the first time around (i.e. once per JSF lifecycle).
        if (component.getChildCount() == 0)
            addChildComponents(configurationComponent);
    }

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException
    {
        ResponseWriter writer = context.getResponseWriter();
        writer.writeText("\n", component, null);
        writer.endElement("div");
        writer.writeComment("********** End of " + component.getClass().getSimpleName() + " component **********");
    }

    public void addChildComponents(AbstractConfigurationComponent configurationComponent)
    {
        if ((configurationComponent.getConfigurationDefinition() == null)
            || ((configurationComponent.getConfiguration() != null) && configurationComponent.getConfiguration().getMap().isEmpty())) {
            String styleClass = (configurationComponent.getNullConfigurationStyle() == null) ? "ErrorBlock" : configurationComponent
                .getNullConfigurationStyle();
            HtmlPanelGroup messagePanel = FacesComponentUtility.addBlockPanel(configurationComponent, configurationComponent, styleClass);
            FacesComponentUtility.addVerbatimText(messagePanel, configurationComponent.getNullConfigurationDefinitionMessage());
            return;
        }

        if (configurationComponent.getConfiguration() == null) {
            HtmlPanelGroup messagePanel = FacesComponentUtility.addBlockPanel(configurationComponent, configurationComponent, "WarnBlock");
            FacesComponentUtility.addVerbatimText(messagePanel, configurationComponent.getNullConfigurationMessage());
            return;
        }

        ConfigurationUtility.normalizeConfiguration(configurationComponent.getConfiguration(), configurationComponent.getConfigurationDefinition());

        if (configurationComponent instanceof ConfigurationSetComponent) {
            ConfigurationSetComponent configurationSetComponent = (ConfigurationSetComponent)configurationComponent;
            addMemberValuesModalPanel(configurationSetComponent);
        }

        FacesComponentUtility.addVerbatimText(configurationComponent, JAVASCRIPT_INCLUDES);

        if (!configurationComponent.isReadOnly())
            addRequiredNotationsKey(configurationComponent);

        if (configurationComponent.getListName() != null) {
            if (configurationComponent.getListIndex() == null) {
                // No index specified means we should add a new map to the list.
                configurationComponent.setListIndex(addNewMap(configurationComponent));
            }

            addListMemberProperty(configurationComponent);
        } else {
            addConfiguration(configurationComponent);
        }

        String id = getInitInputsJavaScriptComponentId(configurationComponent);
        PropertyRenderingUtility.addInitInputsJavaScript(configurationComponent, id, configurationComponent.isFullyEditable(), false);
    }

    private void addListMemberProperty(AbstractConfigurationComponent config) {
        AbstractPropertyBagUIComponentTreeFactory propertyListUIComponentTreeFactory = new MapInListUIComponentTreeFactory(
            config, config.getListName(), config.getListIndex());
        HtmlPanelGroup propertiesPanel = FacesComponentUtility.addBlockPanel(config, config,
            UNGROUPED_PROPERTIES_STYLE_CLASS);
        propertiesPanel.getChildren().add(propertyListUIComponentTreeFactory.createUIComponentTree(null));
    }

    private void deleteListMemberProperty(AbstractConfigurationComponent config) {
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

    private void correctListIndexesInSiblingMembers(AbstractConfigurationComponent config, PropertyList propertyList,
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

    private void deleteOpenMapMemberProperty(AbstractConfigurationComponent config) {
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

    private void addRequiredNotationsKey(AbstractConfigurationComponent config) {
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

    private void addConfiguration(AbstractConfigurationComponent config) {
        addNonGroupedProperties(config);
        addGroupedProperties(config);
    }

    private void addNonGroupedProperties(AbstractConfigurationComponent config) {
        addDebug(config, true, ".addNonGroupedProperties()");
        HtmlPanelGroup propertiesPanel = FacesComponentUtility.addBlockPanel(config, config,
            UNGROUPED_PROPERTIES_STYLE_CLASS);
        AbstractPropertyBagUIComponentTreeFactory propertyListUIComponentTreeFactory = new GroupUIComponentTreeFactory(
            config, GroupUIComponentTreeFactory.NO_GROUP);
        propertiesPanel.getChildren().add(propertyListUIComponentTreeFactory.createUIComponentTree(null));
        addDebug(config, false, ".addNonGroupedProperties()");
    }

    private void addGroupedProperties(AbstractConfigurationComponent config) {
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

    private HtmlSimpleTogglePanel addGroupPanel(AbstractConfigurationComponent config, PropertyGroupDefinition group) {
        addDebug(config, true, ".addGroupPanel()");
        HtmlSimpleTogglePanel groupPanel = FacesComponentUtility.addSimpleTogglePanel(config, config, null);
        // TODO: On AJAX requests, set "opened" attribute to its previous state.
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

    private void validateAttributes(AbstractConfigurationComponent configurationComponent) {
        // TODO: Add back attribute validation - remember config and configSet components require different attributes.
        /*if (configurationComponent.getValueExpression("configurationDefinition") == null) {
            throw new IllegalStateException("The " + configurationComponent.getClass().getName()
                + " component requires a 'configurationDefinition' attribute.");
        }

        if (configurationComponent.getValueExpression("configuration") == null) {
            throw new IllegalStateException("The " + configurationComponent.getClass().getName()
                + " component requires a 'configuration' attribute.");
        }*/
    }

    private int addNewMap(AbstractConfigurationComponent config) {
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

    private String getInitInputsJavaScriptComponentId(AbstractConfigurationComponent configUIComponent)
    {
        return configUIComponent.getId() + INIT_INPUTS_JAVA_SCRIPT_COMPONENT_ID_SUFFIX;
    }

     private HtmlModalPanel addMemberValuesModalPanel(ConfigurationSetComponent configurationSetComponent)
     {
         //UIForm mainForm = FacesComponentUtility.getEnclosingForm(configurationSetComponent);
         HtmlModalPanel modalPanel = FacesComponentUtility.createComponent(HtmlModalPanel.class);
         // The below setting is critical - it allows us to be enclosed in a form (in this case, the form surrounding
         // the config component) (see https://jira.jboss.org/jira/browse/RF-5588).
         modalPanel.setDomElementAttachment("form");

         modalPanel.setId(ConfigurationSetComponent.getMemberValuesModalPanelId(configurationSetComponent));
         modalPanel.setWidth(400);
         modalPanel.setHeight(650);
         // TODO: Add vertical scrollbar to the modal panel
         //       (see http://jboss.org/file-access/default/members/jbossrichfaces/freezone/docs/development/faq/en/html_single/faq.html, item 1.32).
         //       Horizontal scrollbar shouldn't be necessary, since the content has a fixed width.

         configurationSetComponent.getChildren().add(modalPanel);

         // Insert the modal panel as a sibling of the main form, just after the main form in the tree.
         /*int index = getComponentIndex(mainForm);
         mainForm.getParent().getChildren().add(index + 1, modalPanel);
         HtmlForm form = new HtmlForm();
         form.setId(configurationSetComponent.getId() + "PropertySetForm");
         modalPanel.getChildren().add(form);
         form.setOnsubmit("prepareInputsForSubmission(this)");*/
         PropertySetComponent propertySet = new PropertySetComponent();
         propertySet.setId(PROPERTY_SET_COMPONENT_ID);
         //form.getChildren().add(propertySet);
         modalPanel.getChildren().add(propertySet);
         propertySet.setReadOnly(configurationSetComponent.isReadOnly());
         propertySet.setListIndex(configurationSetComponent.getListIndex());
         ValueExpression valueExpression = FacesExpressionUtility.createValueExpression("#{param.propertyExpressionString}",
                 String.class);
         propertySet.setValueExpression(PropertySetComponent.PROPERTY_EXPRESSION_STRING_ATTRIBUTE, valueExpression);
         // The below can be uncommented in order for the "propertyExpressionValue" attribute to have a valid value
         // on the initial page load (i.e. for testing purposes).
         //propertySet.getAttributes().put(PropertySetComponent.PROPERTY_EXPRESSION_STRING_ATTRIBUTE,
         //            "#{EditTestConfigurationUIBean.configurationSet.aggregateConfiguration.map['String1'].stringValue}");

         propertySet.setValueExpression(PropertySetComponent.CONFIGURATION_SET_ATTRIBUTE,
                     configurationSetComponent.getValueExpression(ConfigurationSetComponent.CONFIGURATION_SET_ATTRIBUTE));

         String modalPanelClientId = modalPanel.getClientId(FacesContext.getCurrentInstance());

         HtmlAjaxCommandLink ajaxCommandLink = FacesComponentUtility.createComponent(HtmlAjaxCommandLink.class);
         //form.getChildren().add(ajaxCommandLink);
         modalPanel.getChildren().add(ajaxCommandLink);
         //ajaxCommandLink.setImmediate(true);
         //ajaxCommandLink.setOncomplete("Richfaces.hideModalPanel('" + modalPanelClientId + "');");

         //ajaxCommandLink.setReRender(mainForm.getId() + ":" + configurationSetComponent.getId());
         //ajaxCommandLink.setReRender("rhq_configSet");
         //MethodExpression actionExpression = FacesExpressionUtility.createMethodExpression(
         //   "#{EditTestConfigurationUIBean.updateConfiguration}", String.class, new Class[0]);
         //ajaxCommandLink.setActionExpression(actionExpression);
         ajaxCommandLink.setTitle("OK");
         FacesComponentUtility.addParameter(ajaxCommandLink, null, "refresh", configurationSetComponent.getId());
         FacesComponentUtility.addButton(ajaxCommandLink, "OK", CssStyleClasses.BUTTON_SMALL);

         //HtmlOutputLink closeModalLink = FacesComponentUtility.addOutputLink(form, null, "#");
         HtmlOutputLink closeModalLink = FacesComponentUtility.addOutputLink(modalPanel, null, "#");
         closeModalLink.setOnclick("Richfaces.hideModalPanel('" + modalPanelClientId + "'); return false;");
         closeModalLink.setTitle("Cancel");
         FacesComponentUtility.addButton(closeModalLink, "Cancel", CssStyleClasses.BUTTON_SMALL);
         
         return modalPanel;
     }

    private static int getComponentIndex(UIForm component)
    {
        if (component.getParent() == null)
            return -1;
        List<UIComponent> children = component.getParent().getChildren();
        for (int i = 0; i < children.size(); i++) {                    
            if (children.get(i) == component)
                return i;
        }
        throw new IllegalStateException("Unable to determine index of component " + component);
    }
}