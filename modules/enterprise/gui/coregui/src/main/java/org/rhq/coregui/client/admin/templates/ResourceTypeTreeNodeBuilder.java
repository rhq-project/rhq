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
package org.rhq.coregui.client.admin.templates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.TreeModelType;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;

import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceTypeTemplateCountComposite;
import org.rhq.core.domain.util.ResourceTypeUtility;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.Messages;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.gwt.ResourceTypeGWTServiceAsync;

/**
 * A helper class for {@link ResourceTypeTreeView}.
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public abstract class ResourceTypeTreeNodeBuilder {
    static private final Messages MSG = CoreGUI.getMessages();

    public static final String ATTRIB_ID = "id";
    public static final String ATTRIB_CHILDREN = "children";
    public static final String ATTRIB_NAME = "name";
    public static final String ATTRIB_PLUGIN = "plugin";
    public static final String ATTRIB_CATEGORY = "category";
    public static final String ATTRIB_EDIT = "edit";

    abstract ResourceTypeListGridRecord getGridRecordInstance(ResourceTypeTemplateCountComposite composite);

    abstract ResourceTypeTreeNode getTreeNodeInstance(ResourceTypeTemplateCountComposite composite, String plugin);

    public ResourceTypeTreeNodeBuilder(final ListGrid platformsGrid, final ListGrid platformServicesGrid,
        final TreeGrid serversGrid) {
        ResourceTypeGWTServiceAsync resourceTypeService = GWTServiceLookup.getResourceTypeGWTService(30000);

        resourceTypeService
            .getTemplateCountCompositeMap(new AsyncCallback<Map<Integer, ResourceTypeTemplateCountComposite>>() {

                @Override
                public void onSuccess(Map<Integer, ResourceTypeTemplateCountComposite> result) {
                    // result contains all of our resource types, including the parent hierarchy
                    HashSet<ResourceTypeListGridRecord> platformsRecords;
                    HashSet<ResourceTypeListGridRecord> platformServicesRecords;
                    HashMap<Integer, ResourceTypeTreeNode> serversNodes; // all server nodes (top level and below)
                    HashSet<Integer> topServers; // those servers that are at the root of the tree
                    HashMap<Integer, ArrayList<Integer>> childrenGraph; // defines the children of all server nodes

                    platformsRecords = new HashSet<ResourceTypeListGridRecord>();
                    platformServicesRecords = new HashSet<ResourceTypeListGridRecord>();
                    serversNodes = new HashMap<Integer, ResourceTypeTreeNode>();
                    topServers = new HashSet<Integer>();
                    childrenGraph = new HashMap<Integer, ArrayList<Integer>>();

                    for (ResourceTypeTemplateCountComposite composite : result.values()) {
                        ResourceType type = composite.getType();
                        Set<ResourceType> parentTypes = type.getParentResourceTypes();
                        if (isEmpty(parentTypes)) {
                            if (type.getCategory() == ResourceCategory.PLATFORM) {
                                // no parents but is a platform - these are our main, top-level platforms
                                platformsRecords.add(getGridRecordInstance(composite));
                            } else {
                                // no parents but not a platform - these are our top-level servers
                                ResourceTypeTreeNode node = getTreeNodeInstance(composite, type.getPlugin());
                                topServers.add(node.getResourceTypeId());
                                serversNodes.put(node.getResourceTypeId(), node);
                            }
                        } else {
                            // has parents; if all the direct parents are top level platforms
                            // and the category is service, consider it a "special" platform service
                            boolean isPlatformService = true; // assume its one, unless one of its parents is not a top level platform
                            if (type.getCategory() == ResourceCategory.SERVICE) {
                                for (ResourceType parentType : parentTypes) {
                                    // if one of its parents is not a platform or one of its parent has parents itself,
                                    // then this is not a platform service
                                    if ((parentType.getCategory() != ResourceCategory.PLATFORM)
                                        || !isEmpty(parentType.getParentResourceTypes())) {
                                        isPlatformService = false;
                                        break;
                                    }
                                }
                            } else {
                                isPlatformService = false; // can't be a platform service, its not in the SERVICE category
                            }

                            if (isPlatformService) {
                                platformServicesRecords.add(getGridRecordInstance(composite));
                            } else {
                                // In some cases, a top level server is limited to which platforms it can run on.
                                // Therefore, the parents will not be null/empty (as would be the case if the top level
                                // server can run on ALL platforms), but instead it will have the subset of platforms
                                // the type is valid on. But its the same type - so we only want to show it once.
                                // This is what gotPlatform tracks - whether we saw a parent platform or not.
                                //
                                // But we also have the case where a server type can run inside multiple parent server types.
                                // We want to show these under all their parents to make it easier for the user to find them.
                                boolean gotPlatform = false;
                                for (ResourceType parentType : type.getParentResourceTypes()) {
                                    boolean isParentAPlatform = (parentType.getCategory() == ResourceCategory.PLATFORM && isEmpty(parentType
                                        .getParentResourceTypes()));
                                    if (!isParentAPlatform || !gotPlatform) {
                                        int parentId = parentType.getId();
                                        String parentIdString = String.valueOf(parentId);
                                        ResourceTypeTreeNode node = getTreeNodeInstance(composite, parentIdString);
                                        serversNodes.put(node.getResourceTypeId(), node);
                                        if (isParentAPlatform) {
                                            topServers.add(node.getResourceTypeId());
                                        } else {
                                            // we are a child to other type, add it to the list of children
                                            ArrayList<Integer> childList = childrenGraph.get(parentId);
                                            if (childList == null) {
                                                childList = new ArrayList<Integer>();
                                                childrenGraph.put(parentId, childList);
                                            }
                                            childList.add(node.getResourceTypeId());
                                        }
                                    }
                                    if (isParentAPlatform) {
                                        gotPlatform = true;
                                    }
                                }
                            }
                        }
                    }

                    // now set up our UI components to show the data
                    platformsGrid.setSortField(ATTRIB_NAME);
                    platformServicesGrid.setSortField(ATTRIB_NAME);
                    serversGrid.setSortField(ATTRIB_NAME);

                    platformsGrid.setData(platformsRecords.toArray(new ListGridRecord[platformsRecords.size()]));
                    platformServicesGrid.setData(platformServicesRecords
                        .toArray(new ListGridRecord[platformServicesRecords.size()]));
                    Tree tree = new Tree();
                    tree.setModelType(TreeModelType.CHILDREN);
                    tree.setChildrenProperty(ATTRIB_CHILDREN);
                    TreeNode rootNode = new TreeNode("0");
                    tree.setRoot(rootNode);

                    for (Integer topServerId : topServers) {
                        ResourceTypeTreeNode topServerNode = serversNodes.get(topServerId);
                        topServerNode = topServerNode.copy();
                        fillHierarchy(topServerNode, serversNodes, childrenGraph);
                        tree.add(topServerNode, rootNode);
                    }
                    serversGrid.setData(tree);
                }

                private void fillHierarchy(ResourceTypeTreeNode node,
                    HashMap<Integer, ResourceTypeTreeNode> serversNodes,
                    HashMap<Integer, ArrayList<Integer>> childrenGraph) {

                    if (node.getChildren().length > 0) {
                        return; // we've already populated this node's children before; nothing to do
                    }

                    ArrayList<Integer> childrenIds = childrenGraph.get(node.getResourceTypeId());
                    if (childrenIds != null) {
                        for (Integer childrenId : childrenIds) {
                            ResourceTypeTreeNode childNode = serversNodes.get(childrenId);
                            if (childNode != null) { // this should never be null, but this let's us continue if we have a bug
                                childNode = childNode.copy();
                                fillHierarchy(childNode, serversNodes, childrenGraph);
                                node.addChild(childNode);
                            }
                        }
                    }

                    return;
                }

                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.widget_typeTree_loadFail(), caught);
                }
            });
    }

    private boolean isEmpty(Set<ResourceType> set) {
        return set == null || set.isEmpty();
    }

    public static class ResourceTypeListGridRecord extends ListGridRecord {

        private int id;

        protected ResourceTypeListGridRecord(ResourceTypeTemplateCountComposite composite) {
            ResourceType resourceType = composite.getType();
            this.id = resourceType.getId();

            setAttribute(ATTRIB_ID, String.valueOf(id));
            setAttribute(ATTRIB_NAME, ResourceTypeUtility.displayName(resourceType));
            setAttribute(ATTRIB_PLUGIN, resourceType.getPlugin());
            setAttribute(ATTRIB_CATEGORY, resourceType.getCategory().name());
            setAttribute(ATTRIB_EDIT, ImageManager.getEditIcon());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ResourceTypeListGridRecord)) {
                return false;
            }
            return (this.id == ((ResourceTypeListGridRecord) o).id);
        }

        @Override
        public int hashCode() {
            return 31 * id;
        }
    }

    public static class ResourceTypeTreeNode extends TreeNode {

        private int id;
        private String parentId;
        private TreeNode[] children;

        private ResourceTypeTreeNode() {
            // for use by copy() method
        }

        protected ResourceTypeTreeNode(ResourceTypeTemplateCountComposite composite, String parentId) {
            ResourceType resourceType = composite.getType();

            this.id = resourceType.getId();
            this.parentId = parentId;

            setAttribute(ATTRIB_ID, id);
            setAttribute(ATTRIB_NAME, ResourceTypeUtility.displayName(resourceType));
            setAttribute(ATTRIB_PLUGIN, resourceType.getPlugin());
            setAttribute(ATTRIB_CATEGORY, resourceType.getCategory().name());
            setAttribute(ATTRIB_EDIT, ImageManager.getEditIcon());
            setChildren(new TreeNode[0]);
        }

        public int getResourceTypeId() {
            return this.id;
        }

        public TreeNode[] getChildren() {
            return this.children;
        }

        @Override
        public void setChildren(TreeNode[] children) {
            this.children = children;
            super.setChildren(children);
        }

        public void addChild(TreeNode newChild) {
            TreeNode[] newChildren = new TreeNode[this.children.length + 1];
            System.arraycopy(this.children, 0, newChildren, 0, this.children.length);
            newChildren[this.children.length] = newChild;
            setChildren(newChildren);
        }

        // clone this object and return it - subclasses should override this to copy their own attributes
        public ResourceTypeTreeNode copy() {
            ResourceTypeTreeNode dup = new ResourceTypeTreeNode();
            dup.id = this.id;
            dup.parentId = this.parentId;
            dup.children = this.children;

            dup.setAttribute(ATTRIB_ID, this.getAttributeAsInt(ATTRIB_ID));
            dup.setAttribute(ATTRIB_NAME, this.getAttribute(ATTRIB_NAME));
            dup.setAttribute(ATTRIB_PLUGIN, this.getAttribute(ATTRIB_PLUGIN));
            dup.setAttribute(ATTRIB_CATEGORY, this.getAttribute(ATTRIB_CATEGORY));
            dup.setAttribute(ATTRIB_EDIT, this.getAttribute(ATTRIB_EDIT));

            return dup;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ResourceTypeTreeNode)) {
                return false;
            }

            ResourceTypeTreeNode that = (ResourceTypeTreeNode) o;

            if (this.id != that.id) {
                return false;
            }
            if (this.parentId == null) {
                return that.parentId == null;
            }
            return this.parentId.equals(that.parentId);
        }

        @Override
        public int hashCode() {
            int result = 31;
            result = result * id;
            result = result + (parentId != null ? parentId.hashCode() : 0);
            return result;
        }
    }
}
