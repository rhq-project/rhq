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
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;
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
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertyGroupDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.Messages;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;

/**
 * @author Greg Hinkle
 */
public class ConfigurationComparisonView extends VLayout {
    private static final String ATTRIB_ALL_SAME = "consistent";

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
                if (listGridRecord == null || listGridRecord.getAttributeAsBoolean(ATTRIB_ALL_SAME)) {
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
                    } else if (listGridRecord.getAttributeAsBoolean(ATTRIB_ALL_SAME)) {
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

        parent.setAttribute(ATTRIB_ALL_SAME, true);
        for (PropertyDefinition definition : definitions) {
            if (definition instanceof PropertyDefinitionSimple) {
                ArrayList<PropertySimple> properties = new ArrayList<PropertySimple>();
                for (AbstractPropertyMap map : maps) {
                    properties.add(map.getSimple(definition.getName()));
                }
                ComparisonTreeNode node = new ComparisonTreeNode((PropertyDefinitionSimple) definition, properties,
                    titles);
                if (!node.getAttributeAsBoolean(ATTRIB_ALL_SAME)) {
                    parent.setAttribute(ATTRIB_ALL_SAME, false);
                }
                children.add(node);
            } else if (definition instanceof PropertyDefinitionMap) {
                PropertyDefinitionMap defMap = (PropertyDefinitionMap) definition;
                TreeNode mapNode = new TreeNode(defMap.getDisplayName());
                ArrayList<PropertyMap> properties = new ArrayList<PropertyMap>();
                for (AbstractPropertyMap map : maps) {
                    properties.add((PropertyMap) map);
                }
                buildNode(mapNode, defMap.getOrderedPropertyDefinitions(), properties);
                if (!mapNode.getAttributeAsBoolean(ATTRIB_ALL_SAME)) {
                    parent.setAttribute(ATTRIB_ALL_SAME, false);
                }
                children.add(mapNode);
            } else if (definition instanceof PropertyDefinitionList) {
                PropertyDefinitionList defList = (PropertyDefinitionList) definition;
                TreeNode listNode = new TreeNode(defList.getDisplayName());
                listNode.setAttribute(ATTRIB_ALL_SAME, true);
                if (defList.getMemberDefinition() instanceof PropertyDefinitionMap) { // support list-o-maps only
                    PropertyDefinition memberDef = defList.getMemberDefinition();
                    Collection<PropertyDefinition> memberDefColl = new ArrayList<PropertyDefinition>(1);
                    memberDefColl.add(memberDef);

                    int max = 0; // will be the largest size of any of our lists that are being compared
                    for (AbstractPropertyMap map : maps) {
                        try {
                            int size = map.getList(defList.getName()).getList().size();
                            if (size > max) {
                                max = size;
                            }
                        } catch (Throwable t) {
                            // paranoia - just skip so we don't kill entire compare window if our config doesn't have proper list-o-map
                        }
                    }
                    ArrayList<TreeNode> innerChildren = new ArrayList<TreeNode>();
                    for (int i = 0; i < max; i++) {
                        TreeNode listItemNode = new TreeNode(String.valueOf(i));
                        ArrayList<PropertyMap> properties = new ArrayList<PropertyMap>();
                        for (AbstractPropertyMap map : maps) {
                            try {
                                List<Property> list = map.getList(defList.getName()).getList();
                                if (list.size() < (i + 1)) {
                                    properties.add(new PropertyMap()); // this list didn't have an i-th item, just use an empty map
                                } else {
                                    properties.add((PropertyMap) list.get(i));
                                }
                            } catch (Throwable t) {
                                // paranoia - just skip so we don't kill entire compare window if our config doesn't have proper list-o-map
                                properties.add(new PropertyMap());
                            }
                        }
                        buildNode(listItemNode, memberDefColl, properties);
                        if (!listItemNode.getAttributeAsBoolean(ATTRIB_ALL_SAME)) {
                            parent.setAttribute(ATTRIB_ALL_SAME, false);
                            listNode.setAttribute(ATTRIB_ALL_SAME, false); // any diffs always causes this to indicate the diff
                        }
                        innerChildren.add(listItemNode);
                    }
                    listNode.setChildren(innerChildren.toArray(new TreeNode[innerChildren.size()]));
                }
                children.add(listNode);
            }
        }
        parent.setChildren(children.toArray(new TreeNode[children.size()]));
    }

    public static void displayComparisonDialog(final ArrayList<? extends AbstractResourceConfigurationUpdate> configs) {
        AbstractResourceConfigurationUpdate theFirstUpdateItem = configs.get(0);
        int resourceId = theFirstUpdateItem.getResource().getResourceType().getId();
        final boolean isResourceConfig = theFirstUpdateItem instanceof ResourceConfigurationUpdate;
        ResourceTypeRepository.Cache.getInstance().getResourceTypes(
            resourceId,
            isResourceConfig ? EnumSet.of(ResourceTypeRepository.MetadataType.resourceConfigurationDefinition)
                : EnumSet.of(ResourceTypeRepository.MetadataType.pluginConfigurationDefinition),
            new ResourceTypeRepository.TypeLoadedCallback() {

                public void onTypesLoaded(ResourceType type) {

                    ConfigurationDefinition definition;
                    if (isResourceConfig) {
                        definition = type.getResourceConfigurationDefinition();
                    } else {
                        definition = type.getPluginConfigurationDefinition();
                    }

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
        final Window dialog = new Window();
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
            public void onCloseClick(CloseClickEvent event) {
                dialog.markForDestroy();
            }
        });
        dialog.show();
    }

    private static class ComparisonTreeNode extends TreeNode {

        private ComparisonTreeNode(PropertyDefinitionSimple definition, List<PropertySimple> properties,
            List<String> titles) {
            super(definition.getDisplayName());
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
                setAttribute(ATTRIB_ALL_SAME, allTheSame);
            }
        }
    }
}
