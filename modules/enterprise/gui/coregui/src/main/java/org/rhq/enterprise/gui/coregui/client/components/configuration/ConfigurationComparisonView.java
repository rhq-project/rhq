/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.components.configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.events.CloseClientEvent;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeGridField;
import com.smartgwt.client.widgets.tree.TreeNode;

import org.rhq.core.domain.configuration.AbstractPropertyMap;
import org.rhq.core.domain.configuration.AbstractResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertyGroupDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.Messages;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableWindow;

/**
 * @author Greg Hinkle
 */
public class ConfigurationComparisonView extends VLayout {
    private static final Messages MSG = CoreGUI.getMessages();

    private ConfigurationDefinition definition;
    private List<Configuration> configs;
    private List<String> titles;

    public ConfigurationComparisonView(ConfigurationDefinition definition, List<Configuration> configs,
        List<String> titles) {
        this.definition = definition;
        this.configs = configs;
        this.titles = titles;

        setWidth100();
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        TreeGrid treeGrid = new TreeGrid();
        treeGrid.setWidth100();

        treeGrid.setLoadDataOnDemand(false);

        TreeGridField[] fields = new TreeGridField[2 + titles.size()];

        TreeGridField nameField = new TreeGridField("name", MSG.common_title_name(), 250);
        nameField.setFrozen(true);
        nameField.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                if (listGridRecord == null || listGridRecord.getAttributeAsBoolean("consistent")) {
                    return String.valueOf(o);
                } else {
                    return "<span style=\"color: red;\">" + String.valueOf(o) + "</span>";
                }
            }
        });

        TreeGridField typeField = new TreeGridField("type", MSG.common_title_type(), 80);

        fields[0] = nameField;
        fields[1] = typeField;

        int i = 2;
        for (String title : titles) {
            TreeGridField columnField = new TreeGridField(title, title, 150);
            columnField.setCellFormatter(new CellFormatter() {
                public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                    if (!(listGridRecord instanceof ComparisonTreeNode)) {
                        return "";
                    } else if (listGridRecord.getAttributeAsBoolean("consistent")) {
                        return String.valueOf(o);
                    } else {
                        return "<span style=\"color: red;\">" + String.valueOf(o) + "</span>";
                    }
                }
            });
            fields[i++] = columnField;
        }

        treeGrid.setFields(fields);

        treeGrid.setData(buildTree());

        addMember(treeGrid);
    }

    protected Tree buildTree() {
        Tree tree = new Tree();

        TreeNode root = new TreeNode(MSG.view_configCompare_configCompare());

        ArrayList<TreeNode> children = new ArrayList<TreeNode>();

        List<PropertyDefinition> nonGroupDefs = definition.getNonGroupedProperties();
        if (nonGroupDefs != null && !nonGroupDefs.isEmpty()) {
            TreeNode groupNode = new TreeNode(MSG.common_title_generalProp());
            buildNode(groupNode, nonGroupDefs, configs);
            children.add(groupNode);
        }

        for (PropertyGroupDefinition group : definition.getGroupDefinitions()) {

            TreeNode groupNode = new TreeNode(group.getDisplayName());

            buildNode(groupNode, definition.getPropertiesInGroup(group.getName()), configs);
            children.add(groupNode);
        }

        root.setChildren(children.toArray(new TreeNode[children.size()]));

        tree.setRoot(root);
        return tree;
    }

    private void buildNode(TreeNode parent, Collection<PropertyDefinition> definitions,
        List<? extends AbstractPropertyMap> maps) {
        ArrayList<TreeNode> children = new ArrayList<TreeNode>();

        parent.setAttribute("consistent", true);
        for (PropertyDefinition definition : definitions) {
            if (definition instanceof PropertyDefinitionSimple) {

                ArrayList<PropertySimple> properties = new ArrayList<PropertySimple>();
                for (AbstractPropertyMap map : maps) {
                    properties.add(map.getSimple(definition.getName()));
                }
                ComparisonTreeNode node = new ComparisonTreeNode((PropertyDefinitionSimple) definition, properties,
                    titles);
                if (!node.getAttributeAsBoolean("consistent")) {
                    parent.setAttribute("consistent", false);
                }
                children.add(node);
            }

            // TODO Add support for maps and lists of maps
        }
        parent.setChildren(children.toArray(new TreeNode[children.size()]));
    }

    public static void displayComparisonDialog(final ArrayList<? extends AbstractResourceConfigurationUpdate> configs) {
        int resourceId = configs.get(0).getResource().getResourceType().getId();
        ResourceTypeRepository.Cache.getInstance().getResourceTypes(resourceId,
            EnumSet.of(ResourceTypeRepository.MetadataType.resourceConfigurationDefinition),
            new ResourceTypeRepository.TypeLoadedCallback() {

                public void onTypesLoaded(ResourceType type) {

                    ConfigurationDefinition definition = type.getResourceConfigurationDefinition();

                    ArrayList<Configuration> configurations = new ArrayList<Configuration>();
                    ArrayList<String> titles = new ArrayList<String>();
                    for (AbstractResourceConfigurationUpdate update : configs) {
                        configurations.add(update.getConfiguration());
                        titles.add(String.valueOf(update.getId()));
                    }
                    displayComparisonDialog(definition, configurations, titles);
                }
            });
    }

    public static void displayComparisonDialog(ConfigurationDefinition definition,
        ArrayList<Configuration> configurations, ArrayList<String> titles) {

        ConfigurationComparisonView view = new ConfigurationComparisonView(definition, configurations, titles);
        final Window dialog = new LocatableWindow("configCompare");
        dialog.setTitle(MSG.view_configCompare_comparingConfigs());
        dialog.setShowMinimizeButton(true);
        dialog.setShowMaximizeButton(true);
        dialog.setWidth(700);
        dialog.setHeight(500);
        dialog.setIsModal(true);
        dialog.setShowModalMask(true);
        dialog.setShowResizer(true);
        dialog.setShowFooter(true);
        dialog.setCanDragResize(true);
        dialog.setAutoCenter(true);
        dialog.centerInPage();
        dialog.addItem(view);
        dialog.addCloseClickHandler(new CloseClickHandler() {
            @Override
            public void onCloseClick(CloseClientEvent event) {
                dialog.markForDestroy();
            }
        });
        dialog.show();
    }

    private static class ComparisonTreeNode extends TreeNode {

        PropertyDefinitionSimple definition;
        List<PropertySimple> properties;

        private ComparisonTreeNode(PropertyDefinitionSimple definition, List<PropertySimple> properties,
            List<String> titles) {
            super(definition.getDisplayName());

            this.definition = definition;
            this.properties = properties;

            setAttribute("type", definition.getType().name());

            int i = 0;
            boolean allTheSame = true;
            String commonValue = null;
            for (PropertySimple prop : properties) {

                String value = prop != null ? prop.getStringValue() : null;

                if (i == 0) {
                    commonValue = value;
                } else if (allTheSame && commonValue == null && value != null
                    || (commonValue != null && !commonValue.equals(value))) {
                    allTheSame = false;
                }
                setAttribute(titles.get(i++), value);

                setAttribute("consistent", allTheSame);
            }
        }
    }
}
