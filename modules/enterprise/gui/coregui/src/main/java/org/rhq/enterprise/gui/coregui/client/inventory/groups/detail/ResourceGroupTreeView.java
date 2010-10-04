/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;
import com.smartgwt.client.widgets.tree.events.NodeContextClickEvent;
import com.smartgwt.client.widgets.tree.events.NodeContextClickHandler;

import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.resource.ResourceSubCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ClusterKey;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ClusterFlyweight;
import org.rhq.core.domain.resource.group.composite.ClusterKeyFlyweight;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.tree.EnhancedTreeNode;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.StringUtility;
import org.rhq.enterprise.gui.coregui.client.util.TreeUtility;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class ResourceGroupTreeView extends LocatableVLayout implements BookmarkableView {

    private TreeGrid treeGrid;

    private ViewId currentViewId;
    private int rootGroupId;
    private int selectedGroupId;

    private ResourceGroupTreeContextMenu contextMenu;

    private ResourceGroup rootResourceGroup;
    private Map<Integer, ResourceType> typeMap;
    private ResourceGroup selectedGroup;

    public ResourceGroupTreeView(String locatorId) {
        super(locatorId);

        setWidth(250);
        setHeight100();
    }

    @Override
    protected void onInit() {
        super.onInit();

        this.treeGrid = new TreeGrid();
        this.treeGrid.setWidth100();
        this.treeGrid.setHeight100();
        treeGrid.setAnimateFolders(false);
        treeGrid.setSelectionType(SelectionStyle.SINGLE);
        treeGrid.setShowRollOver(false);
        treeGrid.setSortField("name");
        treeGrid.setShowHeader(false);

        addMember(this.treeGrid);

        contextMenu = new ResourceGroupTreeContextMenu();
        treeGrid.setContextMenu(contextMenu);

        treeGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            @Override
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                if (selectionEvent.getState()) {
                    Record selectedNode = selectionEvent.getRecord();
                    com.allen_sauer.gwt.log.client.Log.info("Node selected in tree: " + selectedNode);
                    ResourceType type = (ResourceType) selectedNode.getAttributeAsObject("resourceType");
                    if (type != null) {
                        // It's a cluster group node, not a subcategory node or an autoTypeGroup node.
                        ClusterKey key = (ClusterKey) selectedNode.getAttributeAsObject("key");
                        if (key == null) {
                            // The root group was selected.
                            String groupId = selectedNode.getAttribute("id");
                            com.allen_sauer.gwt.log.client.Log.debug("Selecting group [" + groupId + "]...");
                            String viewPath = ResourceGroupTopView.VIEW_ID + "/" + groupId;
                            String currentViewPath = History.getToken();
                            if (!currentViewPath.startsWith(viewPath)) {
                                CoreGUI.goToView(viewPath);
                            }
                        } else {
                            com.allen_sauer.gwt.log.client.Log.debug("Selecting cluster group [" + key + "]...");
                            selectClusterGroup(key);
                        }
                    }
                }
            }
        });

        treeGrid.addNodeContextClickHandler(new NodeContextClickHandler() {
            public void onNodeContextClick(final NodeContextClickEvent event) {
                event.getNode();
                event.cancel();

                contextMenu.showContextMenu(event.getNode());
            }
        });

    }

    public void setSelectedGroup(final int groupId) {
        this.selectedGroupId = groupId;

        ResourceGroupCriteria criteria = new ResourceGroupCriteria();
        criteria.addFilterId(groupId);
        criteria.addFilterVisible(null);
        criteria.fetchResourceType(true);

        GWTServiceLookup.getResourceGroupService().findResourceGroupsByCriteria(criteria,
            new AsyncCallback<PageList<ResourceGroup>>() {
                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Failed to load group with id [" + groupId + "].", caught);
                }

                @Override
                public void onSuccess(PageList<ResourceGroup> result) {
                    ResourceGroup group = result.get(0);
                    ResourceGroupTreeView.this.selectedGroup = group;

                    GroupCategory groupCategory = group.getGroupCategory();
                    switch (groupCategory) {
                    case MIXED:
                        ResourceGroupTreeView.this.rootResourceGroup = group;
                        ResourceGroupTreeView.this.rootGroupId = rootResourceGroup.getId();
                        TreeNode fakeRoot = new TreeNode("fakeRootNode");
                        TreeNode rootNode = new TreeNode(rootResourceGroup.getName());
                        rootNode.setID(String.valueOf(rootResourceGroup.getId())); //getClusterKey().toString());
                        fakeRoot.setChildren(new TreeNode[] { rootNode });
                        Tree tree = new Tree();
                        tree.setRoot(fakeRoot);
                        treeGrid.setData(tree);
                        treeGrid.markForRedraw();
                        break;
                    case COMPATIBLE:
                        if (group.getClusterResourceGroup() == null) {
                            // This is a straight up compatible group.
                            ResourceGroupTreeView.this.rootResourceGroup = group;
                        } else {
                            // This is a cluster group beneath a real recursive compatible group.
                            ResourceGroupTreeView.this.rootResourceGroup = group.getClusterResourceGroup();
                        }
                        loadGroup(ResourceGroupTreeView.this.rootResourceGroup.getId());
                        break;
                    }
                }
            });
    }

    private void loadGroup(int groupId) {
        if (groupId == this.rootGroupId) {
            // Still looking at the same compat-recursive tree

            // TODO reselect tree to selected node
            TreeNode selectedNode;
            if (this.selectedGroup.getClusterKey() != null) {
                selectedNode = treeGrid.getTree().findById(this.selectedGroup.getClusterKey());
            } else {
                selectedNode = treeGrid.getTree().findById(String.valueOf(this.selectedGroup.getId()));
            }
            TreeNode[] parents = treeGrid.getTree().getParents(selectedNode);
            treeGrid.getTree().openFolders(parents);
            treeGrid.getTree().openFolder(selectedNode);
            treeGrid.selectRecord(selectedNode);

        } else {
            this.rootGroupId = groupId;
            GWTServiceLookup.getClusterService().getClusterTree(groupId, new AsyncCallback<ClusterFlyweight>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Failed to load group tree.", caught);
                }

                public void onSuccess(ClusterFlyweight result) {
                    loadTreeTypes(result);
                }
            });
        }
    }

    private void loadTreeTypes(final ClusterFlyweight root) {
        Set<Integer> typeIds = new HashSet<Integer>();
        typeIds.add(this.rootResourceGroup.getResourceType().getId());
        getTreeTypes(root, typeIds);

        ResourceTypeRepository.Cache.getInstance().getResourceTypes(typeIds.toArray(new Integer[typeIds.size()]),
            EnumSet.of(ResourceTypeRepository.MetadataType.subCategory),
            new ResourceTypeRepository.TypesLoadedCallback() {
                @Override
                public void onTypesLoaded(Map<Integer, ResourceType> types) {
                    ResourceGroupTreeView.this.typeMap = types;
                    loadTree(root);
                }
            });
    }

    private void selectClusterGroup(ClusterKey key) {
        GWTServiceLookup.getClusterService().createAutoClusterBackingGroup(key, true,
            new AsyncCallback<ResourceGroup>() {
                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Failed to create or update auto cluster group", caught);
                }

                @Override
                public void onSuccess(ResourceGroup result) {
                    int groupId = result.getId();
                    History.newItem(ResourceGroupTopView.VIEW_ID + "/" + groupId);
                }
            });
    }

    private void loadTree(ClusterFlyweight root) {
        TreeNode fakeRoot = new TreeNode("fakeRootNode");

        TreeNode rootNode = new TreeNode(rootResourceGroup.getName());
        rootNode.setID(String.valueOf(root.getGroupId())); //getClusterKey().toString());

        ResourceType rootResourceType = typeMap.get(rootResourceGroup.getResourceType().getId());
        rootNode.setAttribute("resourceType", rootResourceType);
        String icon = "types/" + rootResourceType.getCategory().getDisplayName() + "_up_16.png";
        rootNode.setIcon(icon);

        fakeRoot.setChildren(new TreeNode[] { rootNode });

        ClusterKey rootKey = new ClusterKey(root.getGroupId());
        loadTree(rootNode, root, rootKey);
                
        Tree tree = new Tree();

        tree.setRoot(fakeRoot);
        //TreeUtility.printTree(tree);

        treeGrid.setData(tree);
        treeGrid.markForRedraw();
    }

    public void loadTree(TreeNode parentNode, ClusterFlyweight parentClusterGroup, ClusterKey parentKey) {
        if (!parentClusterGroup.getChildren().isEmpty()) {
            // First pass - group the children by type.
            Map<ResourceType, List<ClusterFlyweight>> childrenByType = new HashMap<ResourceType, List<ClusterFlyweight>>();
            for (ClusterFlyweight child : parentClusterGroup.getChildren()) {
                ClusterKeyFlyweight keyFlyweight = child.getClusterKey();

                ResourceType type = this.typeMap.get(keyFlyweight.getResourceTypeId());
                List<ClusterFlyweight> children = childrenByType.get(type);
                if (children == null) {
                    children = new ArrayList<ClusterFlyweight>();
                    childrenByType.put(type, children);
                }
                children.add(child);
            }

            // Second pass - process each of the sets of like-typed children created in the first pass.
            List<TreeNode> childNodes = new ArrayList<TreeNode>();
            Map<String, TreeNode> subCategoryNodesByName = new HashMap<String, TreeNode>();
            Map<String, List<TreeNode>> subCategoryChildrenByName = new HashMap<String, List<TreeNode>>();
            for (ResourceType childType : childrenByType.keySet()) {
                List<ClusterFlyweight> children = childrenByType.get(childType);
                List<TreeNode> nodesByType = new ArrayList<TreeNode>();
                for (ClusterFlyweight child : children) {
                    TreeNode node = createClusterGroupNode(parentKey, childType, child);
                    nodesByType.add(node);

                    if (!child.getChildren().isEmpty()) {
                        // Recurse.
                        ClusterKey key = (ClusterKey) node.getAttributeAsObject("key");
                        loadTree(node, child, key);
                    }
                }

                // Insert an autoTypeGroup node if the type is not a singleton.
                if (!childType.isSingleton()) {
                    TreeNode autoTypeGroupNode = createAutoTypeGroupNode(childType, nodesByType);
                    nodesByType.clear();
                    nodesByType.add(autoTypeGroupNode);
                }

                // Insert subcategory node(s) if the type has a subcategory.
                ResourceSubCategory subcategory = childType.getSubCategory();
                if (subcategory != null) {
                    TreeNode lastSubcategoryNode = null;

                    ResourceSubCategory currentSubCategory = subcategory;
                    boolean currentSubcategoryNodeCreated = false;
                    do {
                        TreeNode currentSubcategoryNode = subCategoryNodesByName.get(currentSubCategory.getName());
                        if (currentSubcategoryNode == null) {
                            currentSubcategoryNode = new EnhancedTreeNode(currentSubCategory.getName());
                            // Note, subcategory names are typically already plural, so there's no need to pluralize them.
                            currentSubcategoryNode.setTitle(currentSubCategory.getDisplayName());
                            currentSubcategoryNode.setIsFolder(true);
                            subCategoryNodesByName.put(currentSubCategory.getName(), currentSubcategoryNode);
                            subCategoryChildrenByName.put(currentSubCategory.getName(), new ArrayList<TreeNode>());

                            if (currentSubCategory.getParentSubCategory() == null) {
                                // It's a root subcategory - add a node for it to the tree.
                                childNodes.add(currentSubcategoryNode);
                            }

                            currentSubcategoryNodeCreated = true;
                        }

                        if (lastSubcategoryNode != null) {
                            List<TreeNode> currentSubcategoryChildren = subCategoryChildrenByName.get(currentSubcategoryNode.getName());
                            currentSubcategoryChildren.add(lastSubcategoryNode);
                        }
                        lastSubcategoryNode = currentSubcategoryNode;
                    } while (currentSubcategoryNodeCreated &&
                             (currentSubCategory = currentSubCategory.getParentSubCategory()) != null);

                    List<TreeNode> subcategoryChildren = subCategoryChildrenByName.get(subcategory.getName());
                    subcategoryChildren.addAll(nodesByType);
                } else {                    
                    childNodes.addAll(nodesByType);
                }
            }
            
            for (String subcategoryName : subCategoryNodesByName.keySet()) {
                TreeNode subcategoryNode = subCategoryNodesByName.get(subcategoryName);
                List<TreeNode> subcategoryChildren = subCategoryChildrenByName.get(subcategoryName);
                subcategoryNode.setChildren(subcategoryChildren.toArray(new TreeNode[subcategoryChildren.size()]));
            }
            
            parentNode.setChildren(childNodes.toArray(new TreeNode[childNodes.size()]));
        }
    }

    private TreeNode createClusterGroupNode(ClusterKey parentKey, ResourceType type, ClusterFlyweight child) {
        TreeNode node = new EnhancedTreeNode(child.getName());

        ClusterKeyFlyweight keyFlyweight = child.getClusterKey();
        ClusterKey key = new ClusterKey(parentKey, keyFlyweight.getResourceTypeId(), keyFlyweight.getResourceKey());
        String id = key.getKey();
        node.setID(id);
        node.setAttribute("key", key);
        node.setAttribute("resourceType", type);
        node.setIsFolder(!child.getChildren().isEmpty());

        String icon = "types/" + type.getCategory().getDisplayName() + "_up_16.png";
        node.setIcon(icon);
        return node;
    }

    private TreeNode createAutoTypeGroupNode(ResourceType type, List<TreeNode> memberNodes) {
        String name = StringUtility.pluralize(type.getName());
        TreeNode autoTypeGroupNode = new EnhancedTreeNode(name);
        autoTypeGroupNode.setIsFolder(true);
        autoTypeGroupNode.setChildren(memberNodes.toArray(new TreeNode[memberNodes.size()]));
        return autoTypeGroupNode;
    }

    public void renderView(ViewPath viewPath) {
        this.currentViewId = viewPath.getCurrent();
        if (this.currentViewId != null) {
            int groupId = Integer.parseInt(this.currentViewId.getPath());
            setSelectedGroup(groupId);
        }
    }

    private void getTreeTypes(ClusterFlyweight clusterFlyweight, Set<Integer> typeIds) {
        if (clusterFlyweight.getClusterKey() != null) {
            typeIds.add(clusterFlyweight.getClusterKey().getResourceTypeId());
        }

        for (ClusterFlyweight child : clusterFlyweight.getChildren()) {
            getTreeTypes(child, typeIds);
        }
    }
}
