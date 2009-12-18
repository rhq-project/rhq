package org.rhq.core.gui.configuration;

import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.UIPanel;
import javax.faces.component.html.HtmlPanelGroup;

import org.richfaces.component.html.HtmlSimpleTogglePanel;

import org.rhq.core.domain.configuration.definition.PropertyGroupDefinition;
import org.rhq.core.gui.configuration.helper.PropertyRenderingUtility;
import org.rhq.core.gui.configuration.propset.ConfigurationSetComponent;
import org.rhq.core.gui.util.FacesComponentIdFactory;
import org.rhq.core.gui.util.FacesComponentUtility;

public class StructuredConfigUIComponent extends UIPanel {

    FacesComponentIdFactory idFactory;
    boolean isGroup;

    public StructuredConfigUIComponent(AbstractConfigurationComponent configurationComponent) {
        super();
        this.idFactory = configurationComponent;
        this.isGroup = configurationComponent.isGroup();
        if (!configurationComponent.isReadOnly())
            addRequiredNotationsKey();

        if (configurationComponent.getListName() != null) {
            if (configurationComponent.getListIndex() == null) {
                // No index specified means we should add a new map to the list.
                configurationComponent.setListIndex(ConfigRenderer.addNewMap(configurationComponent));
            }
            // Update member Configurations if this is a group config.
            updateGroupMemberConfigurations(configurationComponent);
            addListMemberProperty(configurationComponent, configurationComponent.getListIndex());
        } else {
            addNonGroupedProperties(configurationComponent);
            addGroupedProperties(configurationComponent);
        }
        String id = ConfigRenderer.getInitInputsJavaScriptComponentId(configurationComponent);
        PropertyRenderingUtility.addInitInputsJavaScript(this, id, configurationComponent.isFullyEditable(), false);
    }

    public void addRequiredNotationsKey() {
        ConfigRenderer.addDebug(this, true, ".addNotePanel()");
        HtmlPanelGroup footnotesPanel = FacesComponentUtility.addBlockPanel(this, idFactory,
            ConfigRenderer.NOTE_PANEL_STYLE_CLASS);
        FacesComponentUtility.addOutputText(footnotesPanel, idFactory, "*",
            ConfigRenderer.REQUIRED_MARKER_TEXT_STYLE_CLASS);
        FacesComponentUtility.addOutputText(footnotesPanel, idFactory, " denotes a required field.",
            FacesComponentUtility.NO_STYLE_CLASS);
        if (isGroup) {
            HtmlPanelGroup overridePanel = FacesComponentUtility.addBlockPanel(this, idFactory,
                ConfigRenderer.NOTE_PANEL_STYLE_CLASS);
            FacesComponentUtility.addOutputText(overridePanel, idFactory,
                "note: if override is not checked, that property will not be altered on any group members",
                FacesComponentUtility.NO_STYLE_CLASS);
        }
        ConfigRenderer.addDebug(this, false, ".addNotePanel()");
    }

    @Override
    public String getFamily() {
        // TODO Auto-generated method stub
        return "rhq";
    }

    private void addListMemberProperty(AbstractConfigurationComponent configurationComponent, Integer listIndex) {
        HtmlPanelGroup propertiesPanel = FacesComponentUtility.addBlockPanel(this, idFactory,
            ConfigRenderer.UNGROUPED_PROPERTIES_STYLE_CLASS);
        propertiesPanel.getChildren()
            .add(
                new MapInListUIComponentTreeFactory(configurationComponent, configurationComponent.getListName(),
                    listIndex).createUIComponentTree(null));
    }

    private void updateGroupMemberConfigurations(AbstractConfigurationComponent configurationComponent) {
        if (configurationComponent instanceof ConfigurationSetComponent) {
            ConfigurationSetComponent configurationSetComponent = ((ConfigurationSetComponent) configurationComponent);
            configurationSetComponent.getConfigurationSet().applyGroupConfiguration();
        }
    }

    void addGroupedProperties(AbstractConfigurationComponent config) {
        ConfigRenderer.addDebug(config, true, ".addGroupedProperties()");
        List<PropertyGroupDefinition> groups = config.getConfigurationDefinition().getGroupDefinitions();
        for (PropertyGroupDefinition group : groups) {
            HtmlSimpleTogglePanel groupPanel = addGroupPanel(group);
            AbstractPropertyBagUIComponentTreeFactory propertyListUIComponentTreeFactory = new GroupUIComponentTreeFactory(
                config, group.getName());
            groupPanel.getChildren().add(propertyListUIComponentTreeFactory.createUIComponentTree(null));
        }

        ConfigRenderer.addDebug(config, false, ".addGroupedProperties()");
    }

    void addNonGroupedProperties(AbstractConfigurationComponent config) {

        ConfigRenderer.addDebug(this, true, ".addNonGroupedProperties()");
        HtmlPanelGroup propertiesPanel = FacesComponentUtility.addBlockPanel(this, idFactory,
            ConfigRenderer.UNGROUPED_PROPERTIES_STYLE_CLASS);
        AbstractPropertyBagUIComponentTreeFactory propertyListUIComponentTreeFactory = new GroupUIComponentTreeFactory(
            config, GroupUIComponentTreeFactory.NO_GROUP);

        UIComponent createUIComponentTree = propertyListUIComponentTreeFactory.createUIComponentTree(null);
        propertiesPanel.getChildren().add(createUIComponentTree);

        ConfigRenderer.addDebug(this, false, ".addNonGroupedProperties()");
    }

    HtmlSimpleTogglePanel addGroupPanel(PropertyGroupDefinition group) {
        ConfigRenderer.addDebug(this, true, ".addGroupPanel()");
        HtmlSimpleTogglePanel groupPanel = FacesComponentUtility.addSimpleTogglePanel(this, idFactory, null);
        // TODO: On AJAX requests, set "opened" attribute to its previous state.
        groupPanel.setOpened(!group.isDefaultHidden());
        groupPanel.setHeaderClass(ConfigRenderer.PROPERTY_GROUP_HEADER_STYLE_CLASS);
        groupPanel.setBodyClass(ConfigRenderer.PROPERTY_GROUP_BODY_STYLE_CLASS);

        // Custom header that includes the name and description
        HtmlPanelGroup headerPanel = FacesComponentUtility.createBlockPanel(idFactory, null);
        FacesComponentUtility.addOutputText(headerPanel, idFactory, group.getDisplayName(), null);
        FacesComponentUtility.addOutputText(headerPanel, idFactory, group.getDescription(),
            "group-description-text-panel");
        groupPanel.getFacets().put("header", headerPanel);

        // custom "close" widget
        HtmlPanelGroup closePanel = FacesComponentUtility.createBlockPanel(idFactory, null);
        closePanel.setStyle("text-align: right; font-weight: normal; font-size: 0.8em; whitespace: nowrap;");
        FacesComponentUtility
            .addGraphicImage(closePanel, idFactory, "/images/ico_trigger_wht_collapse.gif", "collapse");
        FacesComponentUtility.addOutputText(closePanel, idFactory, " Collapse", null);
        groupPanel.getFacets().put("closeMarker", closePanel);

        // custom "open" widget
        HtmlPanelGroup openPanel = FacesComponentUtility.createBlockPanel(idFactory, null);
        openPanel.setStyle("text-align: right; font-weight: normal; font-size: 0.8em; whitespace: nowrap;");
        FacesComponentUtility.addGraphicImage(openPanel, idFactory, "/images/ico_trigger_wht_expand.gif", "expand");
        FacesComponentUtility.addOutputText(openPanel, idFactory, " Expand", null);
        groupPanel.getFacets().put("openMarker", openPanel);
        ConfigRenderer.addDebug(this, true, ".addGroupPanel()");

        return groupPanel;
    }

}
