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

 import java.util.Collection;

 import javax.el.MethodExpression;
 import javax.el.ValueExpression;
 import javax.faces.component.UIComponent;
 import javax.faces.component.UIInput;
 import javax.faces.component.UIParameter;
 import javax.faces.component.html.HtmlCommandLink;
 import javax.faces.component.html.HtmlPanelGrid;
 import javax.faces.component.html.HtmlPanelGroup;
 import javax.faces.component.html.HtmlSelectBooleanCheckbox;
 import javax.faces.context.FacesContext;

 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.jetbrains.annotations.NotNull;
 import org.rhq.core.domain.configuration.AbstractPropertyMap;
 import org.rhq.core.domain.configuration.Property;
 import org.rhq.core.domain.configuration.PropertyList;
 import org.rhq.core.domain.configuration.PropertyMap;
 import org.rhq.core.domain.configuration.PropertySimple;
 import org.rhq.core.domain.configuration.definition.PropertyDefinition;
 import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
 import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
 import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
 import org.rhq.core.gui.RequestParameterNameConstants;
 import org.rhq.core.gui.configuration.helper.PropertyRenderingUtility;
 import org.rhq.core.gui.configuration.propset.ConfigurationSetComponent;
 import org.rhq.core.gui.util.FacesComponentUtility;
 import org.rhq.core.gui.util.FacesExpressionUtility;
 import org.rhq.core.gui.util.PropertyIdGeneratorUtility;
 import org.richfaces.component.html.HtmlModalPanel;
 import org.ajax4jsf.component.html.HtmlAjaxCommandLink;

 /**
 * A factory that generates a tree of JSF components that depicts a given collection of JON {@link Property}s.
 *
 * @author Ian Springer
 */
public abstract class AbstractPropertyBagUIComponentTreeFactory {
    private final Log LOG = LogFactory.getLog(AbstractPropertyBagUIComponentTreeFactory.class);

    static final String DELETE_LIST_MEMBER_PROPERTY_FUNCTION = "DELETE_LIST_MEMBER_PROPERTY";
    static final String DELETE_OPEN_MAP_MEMBER_PROPERTY_FUNCTION = "DELETE_OPEN_MAP_MEMBER_PROPERTY";

    static final String PARAM_ID_SUFFIX = "-param";
    static final String PANEL_ID_SUFFIX = "-panel";

    protected static final String PROPERTY_SIMPLE_OVERRIDE_ACCESSOR_SUFFIX = "override";
    protected static final String PROPERTY_SIMPLE_VALUE_ACCESSOR_SUFFIX = "stringValue";
    protected static final String PROPERTY_LIST_VALUE_ACCESSOR_SUFFIX = "list";
    protected static final String PROPERTY_MAP_VALUE_ACCESSOR_SUFFIX = "map";

    private static final String VIEW_MAP_BUTTON_TITLE = "View Details";
    private static final String VIEW_MAP_BUTTON_LABEL = "View";
    private static final String EDIT_MAP_BUTTON_TITLE = "Edit";
    private static final String EDIT_MAP_BUTTON_LABEL = "Edit";
    private static final String DELETE_MAP_BUTTON_LABEL = "Delete";
    private static final String DELETE_MAP_BUTTON_TITLE = "Delete";
    private static final String ADD_NEW_MAP_BUTTON_LABEL = "Add New";
    private static final String ADD_NEW_MAP_BUTTON_TITLE = "Add New";
    private static final String MEMBER_VALUES_BUTTON_LABEL = "Members";
    private static final String MEMBER_VALUES_BUTTON_TITLE = "Members";

    private static final String NESTED_PROPERTIES_TABLE_STYLE_CLASS = "nested-properties-table";
    private static final String PROPERTIES_TABLE_HEADER_CELL_STYLE_CLASS = "properties-table-header-cell";
    private static final String NESTED_PROPERTIES_TABLE_HEADER_CELL_STYLE_CLASS = "nested-properties-table-header-cell";
    private static final String NESTED_PROPERTIES_TABLE_INDENT_CELL_STYLE_CLASS = "nested-properties-table-indent-cell";    
    private static final String PROPERTY_ERROR_CELL_STYLE_CLASS = "property-error-cell";
    private static final String OPENMAP_PROPERTIES_TABLE_STYLE_CLASS = "openmap-properties-table";
    private static final String OPENMAP_PROPERTY_DISPLAY_NAME_CELL_STYLE_CLASS = "openmap-property-display-name-cell";
    private static final String OPENMAP_PROPERTY_VALUE_CELL_STYLE_CLASS = "openmap-property-value-cell";
    private static final String OPENMAP_PROPERTY_ACTIONS_CELL_STYLE_CLASS = "openmap-property-actions-cell";
    private static final String OPENMAP_PROPERTY_ERROR_CELL_STYLE_CLASS = "openmap-property-error-cell";
    private static final String LIST_PROPERTY_DISPLAY_NAME_CELL_STYLE_CLASS = "list-property-display-name-cell";
    private static final String LIST_PROPERTY_VALUE_CELL_STYLE_CLASS = "list-property-value-cell";
    private static final String LIST_PROPERTY_DESCRIPTION_CELL_STYLE_CLASS = "list-property-description-cell";
    private static final String LIST_PROPERTY_ENABLED_CELL_STYLE_CLASS = "list-property-enabled-cell";
    private static final String LIST_PROPERTY_CHILDREN_CELL_STYLE_CLASS = "list-property-children-cell";        
    private static final String PROPERTY_MAP_SUMMARY_TABLE_STYLE_CLASS = "property-map-summary-table";
    private static final String PROPERTY_MAP_SUMMARY_DATA_HEADER_CELL_STYLE_CLASS = "property-map-summary-data-header-cell";
    private static final String PROPERTY_MAP_SUMMARY_HEADER_TEXT_STYLE_CLASS = "property-map-summary-header-text";
    private static final String PROPERTY_MAP_SUMMARY_BUTTONS_CELL_STYLE_CLASS = "property-map-summary-buttons-cell";
    private static final String PROPERTY_MAP_SUMMARY_DATA_CELL_STYLE_CLASS = "property-map-summary-data-cell";
    private static final String PROPERTY_MAP_SUMMARY_DATA_TEXT_STYLE_CLASS = "property-map-summary-data-text";
    private static final String PROPERTY_MAP_SUMMARY_BUTTON_FOOTER_STYLE_CLASS = "property-buttonfooterrow";    
    private static final String BUTTONS_TABLE_STYLE_CLASS = "buttons-table";    
    private static final String VALUES_DIFFER_TEXT_STYLE_CLASS = "values-differ-text";
    private static final String ROW_ODD_STYLE_CLASS = "OddRow";
    private static final String ROW_EVEN_STYLE_CLASS = "EvenRow";

    private AbstractConfigurationComponent config;
    private Collection<PropertyDefinition> propertyDefinitions;
    private AbstractPropertyMap propertyMap;
    private boolean topLevel;
    private String valueExpressionFormat;
    private String overrideExpressionFormat;
    private boolean isAggregate;
    private HtmlModalPanel memberValuesModalPanel;

     public AbstractPropertyBagUIComponentTreeFactory(AbstractConfigurationComponent config,
        Collection<PropertyDefinition> propertyDefinitions, AbstractPropertyMap propertyMap, boolean topLevel,
        String valueExpressionFormat) {
        this.config = config;
        this.propertyDefinitions = propertyDefinitions;
        this.propertyMap = propertyMap;
        this.topLevel = topLevel;
        this.valueExpressionFormat = valueExpressionFormat;
        this.overrideExpressionFormat = getOverrideExpressionFormat(valueExpressionFormat);
        this.isAggregate = (this.config instanceof ConfigurationSetComponent);
        if (isAggregate) {
            ConfigurationSetComponent configurationSetComponent = (ConfigurationSetComponent)this.config;
            this.memberValuesModalPanel = configurationSetComponent.getMemberValuesModalPanel();
        }
    }

    private String getOverrideExpressionFormat(String valueExpression) {
        // replace '.stringValue}' suffix with '.override}'
        int index = valueExpression.lastIndexOf(".");
        return valueExpression.substring(0, index) + "." + PROPERTY_SIMPLE_OVERRIDE_ACCESSOR_SUFFIX + "}";
    }

    protected Integer getListIndex() {
        return null;
    }

    public UIComponent createUIComponentTree(String rowStyleClass) {
        HtmlPanelGroup rootPanel = FacesComponentUtility.createBlockPanel(this.config,
            FacesComponentUtility.NO_STYLE_CLASS);
        if (!this.propertyDefinitions.isEmpty() || (this.propertyMap instanceof PropertyMap)) {
            if (!this.propertyDefinitions.isEmpty()) {
                String tableStyleClass = this.topLevel ? CssStyleClasses.PROPERTIES_TABLE
                    : NESTED_PROPERTIES_TABLE_STYLE_CLASS;
                FacesComponentUtility.addVerbatimText(rootPanel, "\n\n<table class='" + tableStyleClass + "'>");
                int rowCount = 0;
                boolean alternateRowStyles = (rowStyleClass == null);
                if (containsSimpleProperties()) {
                    addSimplePropertiesTableHeaders(rootPanel); // the Name/Unset/Value/Description headers
                }

                // TODO: Display simple props before lists and maps, so the simple props immediately follow the
                //       Name/Unset/Value/Description header.
                for (PropertyDefinition propertyDefinition : this.propertyDefinitions) {
                    rowStyleClass = getRowStyleClass(rowStyleClass, rowCount, alternateRowStyles);
                    addProperty(rootPanel, propertyDefinition, rowStyleClass);
                    rowCount++;
                }
            } else {
                // If the map contains no member definitions, it means the map permits arbitrary simple properties as
                // members. We refer to such a map as an "open map."
                PropertyDefinitionMap propertyDefinitionMap = this.config.getConfigurationDefinition()
                    .getPropertyDefinitionMap(((PropertyMap) this.propertyMap).getName());
                FacesComponentUtility.addVerbatimText(rootPanel, "\n\n<table class='"
                    + OPENMAP_PROPERTIES_TABLE_STYLE_CLASS + "'>");
                addOpenMapMemberPropertiesTableHeaders(rootPanel, propertyDefinitionMap);
                for (Property property : this.propertyMap.getMap().values()) {
                    if (!(property instanceof PropertySimple)) {
                        throw new IllegalStateException("Open map " + this.propertyMap
                            + " contains non-simple property: " + property);
                    }

                    PropertySimple propertySimple = (PropertySimple) property;

                    // NOTE: Row colors never alternate for maps.
                    addOpenMapMemberProperty(rootPanel, propertyDefinitionMap, propertySimple, rowStyleClass);
                }

                addNewOpenMapMemberPropertyRow(rootPanel, propertyDefinitionMap);
            }

            FacesComponentUtility.addVerbatimText(rootPanel, "</table>");
        }

        return rootPanel;
    }

    private String getRowStyleClass(String rowStyleClass, int rowCount, boolean alternateRowStyles) {
        if (alternateRowStyles) {
            // add the odd/even row style
            rowStyleClass = ((rowCount % 2) == 0) ? ROW_ODD_STYLE_CLASS : ROW_EVEN_STYLE_CLASS;
        }

        return rowStyleClass;
    }

    private void addSimplePropertiesTableHeaders(UIComponent parent) {
        String headerCellStyleClass = this.topLevel ? PROPERTIES_TABLE_HEADER_CELL_STYLE_CLASS
            : NESTED_PROPERTIES_TABLE_HEADER_CELL_STYLE_CLASS;

        FacesComponentUtility.addVerbatimText(parent, "\n\n<tr>");

        FacesComponentUtility.addVerbatimText(parent, "<th class='" + headerCellStyleClass + "'>");
        FacesComponentUtility.addOutputText(parent, this.config, "Name", FacesComponentUtility.NO_STYLE_CLASS);
        FacesComponentUtility.addVerbatimText(parent, "</th>");

        // TODO: Get rid of the Override column, once the new group config stuff is operational.
        if (config.isAggregate()) {
            FacesComponentUtility.addVerbatimText(parent, "<th class='" + headerCellStyleClass + "'>");
            FacesComponentUtility.addOutputText(parent, this.config, "Override", FacesComponentUtility.NO_STYLE_CLASS);
            FacesComponentUtility.addVerbatimText(parent, "</th>");
        }

        FacesComponentUtility.addVerbatimText(parent, "<th class='" + headerCellStyleClass + "'>");
        FacesComponentUtility.addOutputText(parent, this.config, "Unset", FacesComponentUtility.NO_STYLE_CLASS);
        FacesComponentUtility.addVerbatimText(parent, "</th>");

        FacesComponentUtility.addVerbatimText(parent, "<th class='" + headerCellStyleClass + "'>");
        FacesComponentUtility.addOutputText(parent, this.config, "Value", FacesComponentUtility.NO_STYLE_CLASS);
        FacesComponentUtility.addVerbatimText(parent, "</th>");

        FacesComponentUtility.addVerbatimText(parent, "<th class='" + headerCellStyleClass + "'>");
        FacesComponentUtility.addOutputText(parent, this.config, "Description", FacesComponentUtility.NO_STYLE_CLASS);
        FacesComponentUtility.addVerbatimText(parent, "</th>");

        FacesComponentUtility.addVerbatimText(parent, "</tr>");
    }

    private boolean containsSimpleProperties() {
        boolean containsSimples = false;
        for (PropertyDefinition propertyDefinition : this.propertyDefinitions) {
            if (propertyDefinition instanceof PropertyDefinitionSimple) {
                containsSimples = true;
                break;
            }
        }

        return containsSimples;
    }

    private void addOpenMapMemberPropertiesTableHeaders(UIComponent parent, PropertyDefinitionMap propertyDefinitionMap) {
        String headerCellStyleClass = this.topLevel ? PROPERTIES_TABLE_HEADER_CELL_STYLE_CLASS
            : NESTED_PROPERTIES_TABLE_HEADER_CELL_STYLE_CLASS;

        FacesComponentUtility.addVerbatimText(parent, "\n\n<tr>");

        FacesComponentUtility.addVerbatimText(parent, "<th class='" + headerCellStyleClass + "'>");
        FacesComponentUtility.addOutputText(parent, this.config, "Name", FacesComponentUtility.NO_STYLE_CLASS);
        FacesComponentUtility.addVerbatimText(parent, "</th>");

        FacesComponentUtility.addVerbatimText(parent, "<th class='" + headerCellStyleClass + "'>");
        FacesComponentUtility.addOutputText(parent, this.config, "Value", FacesComponentUtility.NO_STYLE_CLASS);
        FacesComponentUtility.addVerbatimText(parent, "</th>");

        if (!isReadOnly(propertyDefinitionMap)) {
            FacesComponentUtility.addVerbatimText(parent, "<th class='" + headerCellStyleClass + "'>");
            FacesComponentUtility.addOutputText(parent, this.config, "Actions", FacesComponentUtility.NO_STYLE_CLASS);
            FacesComponentUtility.addVerbatimText(parent, "</th>");
        }

        FacesComponentUtility.addVerbatimText(parent, "</tr>");
    }

    protected AbstractConfigurationComponent getConfigurationComponent() {
        return config;
    }

    private void addProperty(UIComponent parent, PropertyDefinition propertyDefinition, String rowStyleClass) {
        if (propertyDefinition instanceof PropertyDefinitionSimple) {
            addSimpleProperty(parent, (PropertyDefinitionSimple) propertyDefinition, rowStyleClass);
        } else if (propertyDefinition instanceof PropertyDefinitionList) {
            addListProperty(parent, (PropertyDefinitionList) propertyDefinition, rowStyleClass);
        } else if (propertyDefinition instanceof PropertyDefinitionMap) {
            addMapProperty(parent, (PropertyDefinitionMap) propertyDefinition, rowStyleClass);
        } else {
            throw new IllegalStateException("Unsupported subclass of " + PropertyDefinition.class.getName() + ".");
        }
    }

    private void addSimpleProperty(UIComponent parent, PropertyDefinitionSimple propertyDefinitionSimple,
        String rowStyleClass) {
        addDebug(parent, true, ".addSimpleProperty()");

        FacesComponentUtility.addVerbatimText(parent, "\n\n<tr class='" + rowStyleClass + "'>");

        // generate some elements ahead of time, so that dependent
        PropertySimple propertySimple = this.propertyMap.getSimple(propertyDefinitionSimple.getName());
        ValueExpression propertyValueExpression = createPropertyValueExpression(propertySimple.getName(),
                this.valueExpressionFormat);
        // TODO: Only create input when it's actually going to be displayed.
        UIInput input = PropertyRenderingUtility.createInputForSimpleProperty(propertyDefinitionSimple,
                propertySimple, propertyValueExpression, getListIndex(), this.isAggregate, this.config.isReadOnly(),
                this.config.isFullyEditable(), this.config.isPrevalidate());
        // done generating

        FacesComponentUtility.addVerbatimText(parent, "<td class='" + CssStyleClasses.PROPERTY_DISPLAY_NAME_CELL + "'>");
        PropertyRenderingUtility.addPropertyDisplayName(parent, propertyDefinitionSimple, this.config.isReadOnly());
        FacesComponentUtility.addVerbatimText(parent, "</td>");

        if (config.isAggregate()) {
            FacesComponentUtility.addVerbatimText(parent, "<td class='" + CssStyleClasses.PROPERTY_ENABLED_CELL + "'>");
            addPropertyOverrideControl(parent, propertyDefinitionSimple, input);
            FacesComponentUtility.addVerbatimText(parent, "</td>");
        }

        FacesComponentUtility.addVerbatimText(parent, "<td class='" + CssStyleClasses.PROPERTY_ENABLED_CELL + "'>");
        PropertyRenderingUtility.addUnsetControl(parent, propertyDefinitionSimple, propertySimple, input,
                this.isAggregate, this.config.isReadOnly(), this.config.isFullyEditable());
        FacesComponentUtility.addVerbatimText(parent, "</td>");

        FacesComponentUtility.addVerbatimText(parent, "<td class='" + CssStyleClasses.PROPERTY_VALUE_CELL + "'>");
        addPropertySimpleValue(parent, propertySimple, input);
        FacesComponentUtility.addVerbatimText(parent, "<br/>");
        PropertyRenderingUtility.addMessageComponentForInput(parent, input);
        FacesComponentUtility.addVerbatimText(parent, "</td>");

        FacesComponentUtility.addVerbatimText(parent, "<td class='" + CssStyleClasses.PROPERTY_DESCRIPTION_CELL + "'>");
        PropertyRenderingUtility.addPropertyDescription(parent, propertyDefinitionSimple);
        FacesComponentUtility.addVerbatimText(parent, "</td>");

        FacesComponentUtility.addVerbatimText(parent, "</tr>");

        addDebug(parent, false, ".addSimpleProperty()");
    }

     private void addPropertySimpleValue(UIComponent parent, PropertySimple propertySimple, UIInput input)
     {
         if (this.isAggregate) {
             if (propertySimple.getOverride() != null && propertySimple.getOverride()) {
                 parent.getChildren().add(input);
             } else {
                 FacesComponentUtility.addOutputText(parent, null, "Member Values Differ",
                         VALUES_DIFFER_TEXT_STYLE_CLASS);
             }
             HtmlAjaxCommandLink ajaxCommandLink = FacesComponentUtility.createComponent(HtmlAjaxCommandLink.class);
             parent.getChildren().add(ajaxCommandLink);
             //ajaxCommandLink.setImmediate(true);
             if (this.memberValuesModalPanel != null)
                 ajaxCommandLink.setOncomplete("Richfaces.showModalPanel('" +
                     this.memberValuesModalPanel.getClientId(FacesContext.getCurrentInstance()) + "');");
             //String propertySetFormId = this.config.getId() + "PropertySetForm";
             //ajaxCommandLink.setReRender(":" + propertySetFormId + ":rhq_propSet");
             //ajaxCommandLink.setReRender("rhq_configSet");
             ajaxCommandLink.setTitle(MEMBER_VALUES_BUTTON_TITLE);
             FacesComponentUtility.addParameter(ajaxCommandLink, null, "propertyExpressionString",
                input.getValueExpression("value").getExpressionString());
             FacesComponentUtility.addParameter(ajaxCommandLink, null, "refresh",
                     ConfigRenderer.PROPERTY_SET_COMPONENT_ID);
             FacesComponentUtility.addButton(ajaxCommandLink, MEMBER_VALUES_BUTTON_LABEL, CssStyleClasses.BUTTON_SMALL);
         } else {
             parent.getChildren().add(input);
         }
     }

     private void addOpenMapMemberProperty(HtmlPanelGroup parent, PropertyDefinitionMap propertyDefinitionMap,
        PropertySimple propertySimple, String rowStyleClass) {
        addDebug(parent, true, ".addOpenMapMemberProperty()");
        String mapName = ((PropertyMap) this.propertyMap).getName();
        String memberName = propertySimple.getName();

        NullComponent wrapper = new NullComponent();
        parent.getChildren().add(wrapper);
        String wrapperId = PropertyIdGeneratorUtility.getIdentifier(propertySimple, getListIndex(), PANEL_ID_SUFFIX);
        wrapper.setId(wrapperId);

        FacesComponentUtility.addVerbatimText(wrapper, "\n\n<tr class='" + rowStyleClass + "'>");

        FacesComponentUtility.addVerbatimText(wrapper, "<td class='" + OPENMAP_PROPERTY_DISPLAY_NAME_CELL_STYLE_CLASS
            + "'>");
        FacesComponentUtility.addOutputText(wrapper, this.config, propertySimple.getName(),
            CssStyleClasses.PROPERTY_DISPLAY_NAME_TEXT);
        FacesComponentUtility.addVerbatimText(wrapper, "</td>");

        FacesComponentUtility.addVerbatimText(wrapper, "<td class='" + OPENMAP_PROPERTY_VALUE_CELL_STYLE_CLASS + "'>");
         // TODO: Only create input when it's actually going to be displayed.
         UIInput input = PropertyRenderingUtility.createInputForSimpleProperty(propertySimple,
                this.valueExpressionFormat, this.config.isReadOnly());
        addPropertySimpleValue(wrapper, propertySimple, input);
        FacesComponentUtility.addVerbatimText(wrapper, "</td>");

        if (!isReadOnly(propertyDefinitionMap)) {
            // add Actions column w/ delete button
            FacesComponentUtility.addVerbatimText(wrapper, "<td class='" + OPENMAP_PROPERTY_ACTIONS_CELL_STYLE_CLASS
                + "'>");
            HtmlCommandLink deleteLink = FacesComponentUtility.addCommandLink(wrapper, this.config);
            deleteLink.setTitle(DELETE_MAP_BUTTON_TITLE);
            //deleteLink.setImmediate(true); // skip validation (we only want to validate upon Save)

            FacesComponentUtility.addParameter(deleteLink, this.config, RequestParameterNameConstants.FUNCTION_PARAM,
                DELETE_OPEN_MAP_MEMBER_PROPERTY_FUNCTION);
            FacesComponentUtility.addParameter(deleteLink, this.config, RequestParameterNameConstants.MAP_NAME_PARAM,
                mapName);
            FacesComponentUtility.addParameter(deleteLink, this.config,
                RequestParameterNameConstants.MEMBER_NAME_PARAM, memberName);

            FacesComponentUtility.addButton(deleteLink, DELETE_MAP_BUTTON_LABEL, CssStyleClasses.BUTTON_SMALL);
            FacesComponentUtility.addVerbatimText(wrapper, "</td>");
        }

        FacesComponentUtility.addVerbatimText(wrapper, "</tr>");
        addDebug(wrapper, false, ".addOpenMapMemberProperty()");
    }

    /**
     * Adds a single row property
     */
    private void addListProperty(UIComponent parent, PropertyDefinitionList listPropertyDefinition, String rowStyleClass) {
        addDebug(parent, true, ".addListProperty()");
        if (!this.topLevel) {
            throw new IllegalStateException("Lists are only supported at the top level of a Configuration.");
        }

        PropertyList listProperty = this.propertyMap.getList(listPropertyDefinition.getName());

        FacesComponentUtility.addVerbatimText(parent, "\n\n<tr class='" + rowStyleClass + "'>");

        FacesComponentUtility.addVerbatimText(parent, "<td class='" + LIST_PROPERTY_DISPLAY_NAME_CELL_STYLE_CLASS
            + "'>");
        PropertyRenderingUtility.addPropertyDisplayName(parent, listPropertyDefinition, this.config.isReadOnly());
        FacesComponentUtility.addVerbatimText(parent, "</td>");

        FacesComponentUtility.addVerbatimText(parent, "<td class='" + LIST_PROPERTY_ENABLED_CELL_STYLE_CLASS + "' />");

        FacesComponentUtility.addVerbatimText(parent, "<td class='" + LIST_PROPERTY_VALUE_CELL_STYLE_CLASS + "' />");

        FacesComponentUtility
            .addVerbatimText(parent, "<td class='" + LIST_PROPERTY_DESCRIPTION_CELL_STYLE_CLASS + "'>");
        PropertyRenderingUtility.addPropertyDescription(parent, listPropertyDefinition);
        FacesComponentUtility.addVerbatimText(parent, "</td>");

        FacesComponentUtility.addVerbatimText(parent, "</tr>");

        PropertyDefinition listMemberPropertyDefinition = listPropertyDefinition.getMemberDefinition();
        if (listMemberPropertyDefinition instanceof PropertyDefinitionMap) {
            addListMemberMapProperties(parent, listProperty, (PropertyDefinitionMap) listMemberPropertyDefinition,
                rowStyleClass);
        } else {
            addErrorRow(parent, "Rendering of lists of " + listMemberPropertyDefinition.getClass().getSimpleName()
                + " properties is not yet supported. Here's the list's toString() value for now: "
                + ((listProperty != null) ? listProperty.getList() : null), rowStyleClass);
        }

        addDebug(parent, false, ".addListProperty()");
    }

    private void addListMemberMapProperties(UIComponent parent, PropertyList listProperty,
        PropertyDefinitionMap listMemberPropertyDefinition, String rowStyleClass) {
        addDebug(parent, true, ".addListMemberMapProperties()");
        PropertyDefinitionMap listMemberMapPropertyDefinition = listMemberPropertyDefinition;
        validateMapDefinition(listMemberMapPropertyDefinition);

        FacesComponentUtility.addVerbatimText(parent, "\n\n<tr class='" + rowStyleClass + "'>\n");
        FacesComponentUtility.addVerbatimText(parent, "<td colspan='4' class='"
            + LIST_PROPERTY_CHILDREN_CELL_STYLE_CLASS + "'>");

        Collection<PropertyDefinition> mapSummaryPropertyDefinitions = listMemberMapPropertyDefinition
            .getSummaryPropertyDefinitions();

        // Start the "List of maps" table
        FacesComponentUtility.addVerbatimText(parent, "\n<!-- start list of maps -->\n");
        FacesComponentUtility.addVerbatimText(parent, "<table class='" + PROPERTY_MAP_SUMMARY_TABLE_STYLE_CLASS
            + "'>\n");
        FacesComponentUtility.addVerbatimText(parent, "<tbody>\n");

        // Add the row for the headers
        FacesComponentUtility.addVerbatimText(parent, "<tr>\n");

        // Add column header cells...
        for (PropertyDefinition summaryPropertyDefinition : listMemberMapPropertyDefinition
            .getSummaryPropertyDefinitions()) {
            // We can safely cast here, because we already validated the map member definitions.
            PropertyDefinitionSimple mapMemberSimplePropertyDefinition = (PropertyDefinitionSimple) listMemberMapPropertyDefinition
                .get(summaryPropertyDefinition.getName());
            FacesComponentUtility.addVerbatimText(parent, "<th class='"
                + PROPERTY_MAP_SUMMARY_DATA_HEADER_CELL_STYLE_CLASS + "'>");
            FacesComponentUtility.addOutputText(parent, this.config,
                mapMemberSimplePropertyDefinition.getDisplayName(), PROPERTY_MAP_SUMMARY_HEADER_TEXT_STYLE_CLASS);
            FacesComponentUtility.addVerbatimText(parent, "</th>\n");
        }

        // add the "Actions" header
        FacesComponentUtility.addVerbatimText(parent, "<th class='" + PROPERTY_MAP_SUMMARY_DATA_HEADER_CELL_STYLE_CLASS
            + "'>");
        FacesComponentUtility.addOutputText(parent, this.config, "Actions",
            PROPERTY_MAP_SUMMARY_HEADER_TEXT_STYLE_CLASS);
        FacesComponentUtility.addVerbatimText(parent, "</th>\n");
        FacesComponentUtility.addVerbatimText(parent, "</tr>\n");

        // Add body cells...
        // NOTE: We only display the View button if the config component is read-only. If the list is read-only, members
        //       cannot be added or deleted, but existing members can still be edited.
        String viewEditButtonLabel = (this.config.isReadOnly()) ? VIEW_MAP_BUTTON_LABEL : EDIT_MAP_BUTTON_LABEL;
        String viewEditButtonTitle = (this.config.isReadOnly()) ? VIEW_MAP_BUTTON_TITLE : EDIT_MAP_BUTTON_TITLE;
        for (int index = 0; index < listProperty.getList().size(); index++) {
            addListMemberMapProperty(parent, listProperty, listMemberMapPropertyDefinition, viewEditButtonLabel,
                viewEditButtonTitle, index);
        }
        //HtmlPanelGrid buttonsPanelGrid = FacesComponentUtility.addPanelGrid(parent, this.configurationUIBean, 1, BUTTONS_TABLE_STYLE_CLASS);

        if (!isReadOnly(listMemberMapPropertyDefinition.getParentPropertyListDefinition())) {
            FacesComponentUtility.addVerbatimText(parent, "<tr>\n");
            FacesComponentUtility.addVerbatimText(parent, "<td colspan='" + (mapSummaryPropertyDefinitions.size() + 1)
                + "' class='" + PROPERTY_MAP_SUMMARY_BUTTON_FOOTER_STYLE_CLASS + "'>");

            // add-new button
            HtmlCommandLink addNewLink = FacesComponentUtility.addCommandLink(parent, this.config);
            addNewLink.setTitle(ADD_NEW_MAP_BUTTON_TITLE);
            MethodExpression actionExpression = FacesExpressionUtility.createMethodExpression(
                "#{ConfigHelperUIBean.addNewMap}", String.class, new Class[0]);
            addNewLink.setActionExpression(actionExpression);
            FacesComponentUtility.addParameter(addNewLink, this.config, RequestParameterNameConstants.LIST_NAME_PARAM,
                listProperty.getName());
            FacesComponentUtility.addParameter(addNewLink, this.config, RequestParameterNameConstants.LIST_INDEX_PARAM,
                String.valueOf(listProperty.getList().size()));
            FacesComponentUtility.addButton(addNewLink, ADD_NEW_MAP_BUTTON_LABEL, CssStyleClasses.BUTTON_SMALL);
            FacesComponentUtility.addVerbatimText(parent, "</td></tr>\n");
        }

        FacesComponentUtility.addVerbatimText(parent, "</tbody></table>\n\n");
        FacesComponentUtility.addVerbatimText(parent, "<!-- end List of Maps -->\n\n");

        FacesComponentUtility.addVerbatimText(parent, "</td></tr>");
        addDebug(parent, false, ".addListMemberMapProperties()");
    }

    private void addListMemberMapProperty(UIComponent parent, PropertyList listProperty,
        PropertyDefinitionMap listMemberMapPropertyDefinition, String viewEditButtonLabel, String viewEditButtonTitle,
        int index) {
        addDebug(parent, true, ".addListMemberMapProperty()");
        Property listMemberProperty = listProperty.getList().get(index);
        String listName = listProperty.getName();
        if (!(listMemberProperty instanceof PropertyMap)) {
            throw new IllegalStateException("Property '" + listName
                + "' is defined as a list of maps but contains one or more non-map members.");
        }

        PropertyMap listMemberPropertyMap = (PropertyMap) listMemberProperty;

        HtmlPanelGroup panel = FacesComponentUtility.addBlockPanel(parent, getConfigurationComponent(),
            FacesComponentUtility.NO_STYLE_CLASS);
        String listIndex = String.valueOf(index);
        String panelId = PropertyIdGeneratorUtility.getIdentifier(listMemberProperty, index, PANEL_ID_SUFFIX);
        panel.setId(panelId);

        FacesComponentUtility.addVerbatimText(panel, "<tr>\n");

        // add the simple property data cells
        addPropertyMapSummaryDataCells(panel, listMemberMapPropertyDefinition, listMemberPropertyMap);

        int numberOfButtons = 1;
        if (!this.config.isReadOnly()) {
            numberOfButtons++;
        }

        // add the Actions cell
        FacesComponentUtility.addVerbatimText(panel, "<td class='" + PROPERTY_MAP_SUMMARY_BUTTONS_CELL_STYLE_CLASS
            + "'>");
        HtmlPanelGrid buttonsPanelGrid = FacesComponentUtility.addPanelGrid(panel, this.config, numberOfButtons,
            BUTTONS_TABLE_STYLE_CLASS);

        // view/edit button
        HtmlCommandLink viewEditLink = FacesComponentUtility.addCommandLink(buttonsPanelGrid, this.config);
        viewEditLink.setTitle(viewEditButtonTitle);
        MethodExpression actionExpression = FacesExpressionUtility.createMethodExpression(
            "#{ConfigHelperUIBean.accessMap}", String.class, new Class[0]);
        viewEditLink.setActionExpression(actionExpression);
        FacesComponentUtility.addParameter(viewEditLink, this.config, RequestParameterNameConstants.LIST_NAME_PARAM,
            listName);
        FacesComponentUtility.addParameter(viewEditLink, this.config, RequestParameterNameConstants.LIST_INDEX_PARAM,
            listIndex);
        int configId = this.config.getConfiguration().getId();
        FacesComponentUtility.addParameter(viewEditLink, this.config, RequestParameterNameConstants.CONFIG_ID_PARAM,
            String.valueOf(configId));
        FacesComponentUtility.addButton(viewEditLink, viewEditButtonLabel, CssStyleClasses.BUTTON_SMALL);

        if (!isReadOnly(listMemberMapPropertyDefinition.getParentPropertyListDefinition())) {
            // delete button
            HtmlCommandLink deleteLink = FacesComponentUtility.addCommandLink(buttonsPanelGrid, this.config);
            deleteLink.setTitle(DELETE_MAP_BUTTON_TITLE);
            //deleteLink.setImmediate(true);

            FacesComponentUtility.addParameter(deleteLink, this.config, RequestParameterNameConstants.FUNCTION_PARAM,
                DELETE_LIST_MEMBER_PROPERTY_FUNCTION);
            FacesComponentUtility.addParameter(deleteLink, this.config, RequestParameterNameConstants.LIST_NAME_PARAM,
                listName);
            UIParameter listIndexParam = FacesComponentUtility.addParameter(deleteLink, this.config,
                RequestParameterNameConstants.LIST_INDEX_PARAM, listIndex);
            String paramId = PropertyIdGeneratorUtility.getIdentifier(listMemberProperty, index, PARAM_ID_SUFFIX);
            listIndexParam.setId(paramId);

            FacesComponentUtility.addButton(deleteLink, DELETE_MAP_BUTTON_LABEL, CssStyleClasses.BUTTON_SMALL);
        }

        FacesComponentUtility.addVerbatimText(panel, "</td>\n");

        FacesComponentUtility.addVerbatimText(panel, "</tr>\n");
        addDebug(parent, false, ".addListMemberMapProperty()");
    }

    private void addPropertyMapSummaryDataCells(UIComponent parent,
        PropertyDefinitionMap listMemberMapPropertyDefinition, PropertyMap listMemberMapProperty) {
        for (PropertyDefinition summaryPropertyDefinition : listMemberMapPropertyDefinition
            .getSummaryPropertyDefinitions()) {
            Property mapMemberProperty = listMemberMapProperty.get(summaryPropertyDefinition.getName());
            if (!(mapMemberProperty instanceof PropertySimple)) {
                throw new IllegalStateException("Property '" + mapMemberProperty.getName()
                    + "' is defined as a map of simples but contains one or more non-simple members.");
            }

            PropertySimple mapMemberSimpleProperty = (PropertySimple) mapMemberProperty;
            FacesComponentUtility.addVerbatimText(parent, "<td class='" + PROPERTY_MAP_SUMMARY_DATA_CELL_STYLE_CLASS
                + "'>");
            if (mapMemberSimpleProperty.getStringValue() == null) {
                FacesComponentUtility.addOutputText(parent, this.config, "not set", CssStyleClasses.REQUIRED_MARKER_TEXT);
            } else {
                FacesComponentUtility.addOutputText(parent, this.config, mapMemberSimpleProperty.getStringValue(),
                    PROPERTY_MAP_SUMMARY_DATA_TEXT_STYLE_CLASS);
            }

            FacesComponentUtility.addVerbatimText(parent, "</td>\n");
        }
    }

    private void validateMapDefinition(PropertyDefinitionMap propertyDefinitionMap) {
        for (PropertyDefinition mapMemberPropertyDefinition : propertyDefinitionMap.getPropertyDefinitions().values()) {
            if (!(mapMemberPropertyDefinition instanceof PropertyDefinitionSimple)) {
                throw new IllegalStateException("Only maps of simple properties are supported.");
            }
        }
    }

    private void addMapProperty(UIComponent parent, PropertyDefinitionMap propertyDefinitionMap, String rowStyleClass) {
        if (!this.topLevel) {
            throw new IllegalStateException(
                "Maps are only supported at the top level of a Configuration or within a top-level List.");
        }

        validateMapDefinition(propertyDefinitionMap);

        FacesComponentUtility.addVerbatimText(parent, "\n\n<tr class='" + rowStyleClass + "'>");

        FacesComponentUtility.addVerbatimText(parent, "<td class='" + LIST_PROPERTY_DISPLAY_NAME_CELL_STYLE_CLASS
            + "'>");
        PropertyRenderingUtility.addPropertyDisplayName(parent, propertyDefinitionMap, this.config.isReadOnly());
        FacesComponentUtility.addVerbatimText(parent, "</td>");

        FacesComponentUtility.addVerbatimText(parent, "<td class='" + LIST_PROPERTY_ENABLED_CELL_STYLE_CLASS + "' />");

        FacesComponentUtility.addVerbatimText(parent, "<td class='" + LIST_PROPERTY_VALUE_CELL_STYLE_CLASS + "' />");

        FacesComponentUtility
            .addVerbatimText(parent, "<td class='" + LIST_PROPERTY_DESCRIPTION_CELL_STYLE_CLASS + "'>");
        PropertyRenderingUtility.addPropertyDescription(parent, propertyDefinitionMap);
        FacesComponentUtility.addVerbatimText(parent, "</td>");

        FacesComponentUtility.addVerbatimText(parent, "</tr>");

        addMapMemberProperties(parent, propertyDefinitionMap.getName(), rowStyleClass);
    }

    private void addMapMemberProperties(UIComponent parent, String mapName, String rowStyleClass) {
        FacesComponentUtility.addVerbatimText(parent, "\n\n<tr class='" + rowStyleClass + "'>\n");
        FacesComponentUtility.addVerbatimText(parent, "<td colspan='4' class='"
            + LIST_PROPERTY_CHILDREN_CELL_STYLE_CLASS + "'>");
        HtmlPanelGrid spacerPanelGrid = FacesComponentUtility.addPanelGrid(parent, this.config, 2,
            FacesComponentUtility.NO_STYLE_CLASS);
        spacerPanelGrid.setWidth("100%");
        spacerPanelGrid.setColumnClasses(NESTED_PROPERTIES_TABLE_INDENT_CELL_STYLE_CLASS + ","
            + FacesComponentUtility.NO_STYLE_CLASS);
        FacesComponentUtility.addBlockPanel(spacerPanelGrid, this.config, FacesComponentUtility.NO_STYLE_CLASS);
        AbstractPropertyBagUIComponentTreeFactory propertyListUIComponentTreeFactory = new MapInConfigurationUIComponentTreeFactory(
            this.config, mapName);
        spacerPanelGrid.getChildren().add(
            propertyListUIComponentTreeFactory.createUIComponentTree(ROW_EVEN_STYLE_CLASS));
        parent.getChildren().add(spacerPanelGrid);
        FacesComponentUtility.addVerbatimText(parent, "</td></tr>\n");
    }

    private void addNewOpenMapMemberPropertyRow(UIComponent parent, PropertyDefinitionMap propertyDefinitionMap) {
        if (!isReadOnly(propertyDefinitionMap)) {
            FacesComponentUtility.addVerbatimText(parent, "\n\n<tr><td colspan='3' class='"
                + PROPERTY_MAP_SUMMARY_BUTTON_FOOTER_STYLE_CLASS + "'>");

            // add-new button
            HtmlCommandLink addNewLink = FacesComponentUtility.addCommandLink(parent, this.config);
            addNewLink.setTitle(ADD_NEW_MAP_BUTTON_TITLE);

            //addNewLink.setImmediate(true); // skip validation (we only want to validate upon Save)
            MethodExpression actionExpression = FacesExpressionUtility.createMethodExpression(
                "#{ConfigHelperUIBean.addNewOpenMapMemberProperty}", String.class, new Class[0]);
            addNewLink.setActionExpression(actionExpression);
            FacesComponentUtility.addParameter(addNewLink, this.config, RequestParameterNameConstants.MAP_NAME_PARAM,
                propertyDefinitionMap.getName());
            FacesComponentUtility.addButton(addNewLink, ADD_NEW_MAP_BUTTON_LABEL, CssStyleClasses.BUTTON_SMALL);
            FacesComponentUtility.addVerbatimText(parent, "</td></tr>");
        }
    }

    private void addErrorRow(UIComponent parent, String errorMsg, String rowStyleClass) {
        addDebug(parent, true, ".addErrorRow()");
        FacesComponentUtility.addVerbatimText(parent, "\n\n<tr class='" + rowStyleClass + "'><td colspan='4' class='"
            + PROPERTY_ERROR_CELL_STYLE_CLASS + "'><h4>" + errorMsg + "</h4></td></tr>");
        addDebug(parent, false, ".addErrorRow()");
    }

    @NotNull
    private HtmlSelectBooleanCheckbox createInputOverrideForSimpleProperty(
        PropertyDefinitionSimple propertyDefinitionSimple) {
        HtmlSelectBooleanCheckbox input = FacesComponentUtility.createComponent(HtmlSelectBooleanCheckbox.class,
            this.config);

        // Find the actual property corresponding to this property def, and use that to create the component id.
        Property property = this.propertyMap.get(propertyDefinitionSimple.getName());
        if (property != null) {
            // add suffix to prevent collision with value input identifier for property
            String propertyId = PropertyIdGeneratorUtility.getIdentifier(property, getListIndex(), "override");
            input.setId(propertyId);

            Boolean shouldOverride = ((PropertySimple) property).getOverride();
            if (shouldOverride == null) {
                FacesComponentUtility.setOverride(input, false);
            } else {
                FacesComponentUtility.setOverride(input, shouldOverride);
            }
        }

        setInputOverrideExpression(input, propertyDefinitionSimple.getName());

        return input;
    }

    private void addPropertyOverrideControl(UIComponent parent, PropertyDefinitionSimple propertyDefinitionSimple,
                                            UIInput valueInput) {
        HtmlSelectBooleanCheckbox overrideCheckbox = createInputOverrideForSimpleProperty(propertyDefinitionSimple);
        parent.getChildren().add(overrideCheckbox);
        overrideCheckbox.setValue(isOverride(propertyDefinitionSimple));
        if (isReadOnly(propertyDefinitionSimple)) {
            FacesComponentUtility.setDisabled(overrideCheckbox, true);
        }

        /* TODO: use unsetCheckbox and valueInput to implement the following logic against dom elements in javascript:
         *
         * if override input is not checked, disable unsetCheckbox and dissble valueInput dom elements   * if override
         * input is checked    , enabled unsetCheckbox   ** only cascade enable valueInput dom elements if the
         * unsetCheckbox is NOT unset
         */
        StringBuilder onchange = new StringBuilder();
        for (String valueInputHtmlDomReference : PropertyRenderingUtility.getHtmlDomReferences(valueInput)) {
            onchange.append("setInputOverride(").append(valueInputHtmlDomReference).append(", this.checked);");
        }

        overrideCheckbox.setOnchange(onchange.toString());
    }

    /**
     * Binds the value of the specified UIInput to an EL expression corresponding to the Configuration property's
     * override attribute with the specified name.
     */
    @SuppressWarnings("deprecation")
    private void setInputOverrideExpression(HtmlSelectBooleanCheckbox input, String propertyName) {
        // e.g.: #{configuration.simpleProperties['useJavaContext'].override}
        String expression = String.format(this.overrideExpressionFormat, propertyName);
        ValueExpression valueExpression = FacesExpressionUtility.createValueExpression(expression, Boolean.class);
        input.setValueExpression("value", valueExpression);
    }

    private Boolean isOverride(PropertyDefinition propertyDefinition) {
        if (!(propertyDefinition instanceof PropertyDefinitionSimple)) {
            return false;
        }

        Property property = this.propertyMap.get(propertyDefinition.getName());
        if (property == null) {
            return false;
        }

        Boolean override = ((PropertySimple) property).getOverride();
        return ((override != null) && override);
    }

    private boolean isReadOnly(PropertyDefinition propertyDefinition) {
        // A fully editable config overrides any other means of setting read only.
        return (!this.config.isFullyEditable() &&
                (this.config.isReadOnly() || (propertyDefinition.isReadOnly() && !isInvalidRequiredProperty(propertyDefinition))));
    }

    private boolean isInvalidRequiredProperty(PropertyDefinition propertyDefinition) {
        boolean isInvalidRequiredProperty = false;

        if ((propertyDefinition instanceof PropertyDefinitionSimple) && propertyDefinition.isRequired()) {
            PropertySimple propertySimple = this.propertyMap.getSimple(propertyDefinition.getName());
            String errorMessage = propertySimple.getErrorMessage();

            if ((null == propertySimple.getStringValue()) || "".equals(propertySimple.getStringValue())
                || ((null != errorMessage) && (!"".equals(errorMessage.trim())))) {
                // Required properties with no value, or an invalid value (assumed if we see an error message) should never
                // be set to read-only, otherwise the user will have no way to give the property a value and thereby
                // get things to a valid state.
                isInvalidRequiredProperty = true;
            }
        }

        return isInvalidRequiredProperty;
    }

    private boolean isUnset(PropertyDefinitionSimple propertyDefinitionSimple) {
        return !propertyDefinitionSimple.isRequired()
            && ((this.propertyMap.get(propertyDefinitionSimple.getName()) == null) || (this.propertyMap.getSimple(
                propertyDefinitionSimple.getName()).getStringValue() == null));
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

    /**
     * Binds the value of the specified UIInput to an EL expression corresponding to the Configuration property with the
     * specified name.
     */
    @SuppressWarnings( { "JavaDoc" })
    private static ValueExpression createPropertyValueExpression(String propertyName, String valueExpressionFormat) {
        // e.g.: #{configuration.simpleProperties['useJavaContext'].stringValue}
        String expression = String.format(valueExpressionFormat, propertyName);
        ValueExpression valueExpression = FacesExpressionUtility.createValueExpression(expression, String.class);
        return valueExpression;
    }

}