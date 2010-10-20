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
package org.rhq.enterprise.gui.coregui.client.admin.templates;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;

import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceTypeTemplateCountComposite;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceTypeGWTServiceAsync;

/**
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class ResourceTypeTreeNodeBuilder {

    public static final String ATTRIB_ID = "id";
    public static final String ATTRIB_PARENT_ID = "parentId";
    public static final String ATTRIB_NAME = "name";
    public static final String ATTRIB_PLUGIN = "plugin";
    public static final String ATTRIB_CATEGORY = "category";
    public static final String ATTRIB_ENABLED_METRIC_TEMPLATES = "enabledMetricTemplates";
    public static final String ATTRIB_DISABLED_METRIC_TEMPLATES = "disabledMetricTemplates";
    public static final String ATTRIB_ENABLED_ALERT_TEMPLATES = "enabledAlertTemplates";
    public static final String ATTRIB_DISABLED_ALERT_TEMPLATES = "disabledAlertTemplates";

    public ResourceTypeTreeNodeBuilder(final ListGrid platformsGrid, final ListGrid platformServicesGrid,
        final TreeGrid serversGrid) {
        ResourceTypeGWTServiceAsync resourceTypeService = GWTServiceLookup.getResourceTypeGWTService();

        resourceTypeService
            .getTemplateCountCompositeMap(new AsyncCallback<Map<Integer, ResourceTypeTemplateCountComposite>>() {

                @Override
                public void onSuccess(Map<Integer, ResourceTypeTemplateCountComposite> result) {
                    HashSet<ResourceTypeListGridRecord> platformsRecords;
                    HashSet<ResourceTypeListGridRecord> platformServicesRecords;
                    HashSet<ResourceTypeTreeNode> treeNodes;

                    platformsRecords = new HashSet<ResourceTypeListGridRecord>();
                    platformServicesRecords = new HashSet<ResourceTypeListGridRecord>();
                    treeNodes = new HashSet<ResourceTypeTreeNode>();

                    for (ResourceTypeTemplateCountComposite composite : result.values()) {
                        ResourceType type = composite.getType();
                        Set<ResourceType> parentTypes = type.getParentResourceTypes();
                        if (isEmpty(parentTypes)) {
                            if (type.getCategory() == ResourceCategory.PLATFORM) {
                                // no parents but is a platform - these are our main, top-level platforms
                                platformsRecords.add(new ResourceTypeListGridRecord(composite));
                            } else {
                                // no parents but not a platform - these are our top-level servers
                                treeNodes.add(new ResourceTypeTreeNode(composite, type.getPlugin()));
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
                                platformServicesRecords.add(new ResourceTypeListGridRecord(composite));
                            } else {
                                // in some cases, a top level server is limited to which platforms it can run on.
                                // therefore, the parents will not be null/empty (as would be the case if the top level
                                // server can run on ALL platforms), but instead it will have the subset of platforms
                                // the type is valid on. But its the same type - so we only want to show it once. Therefore,
                                // once we see a parent that is a top level platform, we don't add the type again for other
                                // top level platforms. That's what gotPlatform boolean tracks.
                                boolean gotPlatform = false;
                                for (ResourceType parentType : type.getParentResourceTypes()) {
                                    boolean isPlatform = (parentType.getCategory() == ResourceCategory.PLATFORM && isEmpty(parentType
                                        .getParentResourceTypes()));
                                    if (!isPlatform || !gotPlatform) {
                                        treeNodes.add(new ResourceTypeTreeNode(composite, String.valueOf(parentType
                                            .getId())));
                                    }
                                    if (isPlatform) {
                                        gotPlatform = true;
                                    }
                                }
                            }
                        }
                    }

                    platformsGrid.setData(platformsRecords.toArray(new ListGridRecord[platformsRecords.size()]));
                    platformServicesGrid.setData(platformServicesRecords
                        .toArray(new ListGridRecord[platformServicesRecords.size()]));
                    serversGrid.getTree().linkNodes(treeNodes.toArray(new TreeNode[treeNodes.size()]));
                }

                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Failed to load resource types", caught);
                }
            });
    }

    private boolean isEmpty(Set<ResourceType> set) {
        return set == null || set.isEmpty();
    }

    public static class ResourceTypeListGridRecord extends ListGridRecord {

        private ResourceType resourceType;

        private ResourceTypeListGridRecord(ResourceTypeTemplateCountComposite composite) {
            this.resourceType = composite.getType();

            setAttribute(ATTRIB_ID, String.valueOf(resourceType.getId()));
            setAttribute(ATTRIB_NAME, resourceType.getName());
            setAttribute(ATTRIB_PLUGIN, resourceType.getPlugin());
            setAttribute(ATTRIB_CATEGORY, resourceType.getCategory().getDisplayName());
            setAttribute(ATTRIB_ENABLED_ALERT_TEMPLATES, composite.getEnabledAlertCount());
            setAttribute(ATTRIB_DISABLED_ALERT_TEMPLATES, composite.getDisabledAlertCount());
            setAttribute(ATTRIB_ENABLED_METRIC_TEMPLATES, composite.getEnabledMetricCount());
            setAttribute(ATTRIB_DISABLED_METRIC_TEMPLATES, composite.getDisabledMetricCount());
        }

        public ResourceType getResourceType() {
            return resourceType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ResourceTypeTreeNode)) {
                return false;
            }
            return (this.resourceType.getId() == ((ResourceTypeTreeNode) o).resourceType.getId());
        }

        @Override
        public int hashCode() {
            return 31 * resourceType.getId();
        }
    }

    public static class ResourceTypeTreeNode extends TreeNode {

        private ResourceType resourceType;
        private String id;
        private String parentId;

        private ResourceTypeTreeNode(ResourceTypeTemplateCountComposite composite, String parentId) {
            this.resourceType = composite.getType();

            String id = String.valueOf(resourceType.getId());
            setID(id);
            this.id = id;

            setParentID(parentId);
            this.parentId = parentId;

            setAttribute(ATTRIB_ID, id);
            setAttribute(ATTRIB_PARENT_ID, parentId);
            setAttribute(ATTRIB_NAME, resourceType.getName());
            setAttribute(ATTRIB_PLUGIN, resourceType.getPlugin());
            setAttribute(ATTRIB_CATEGORY, resourceType.getCategory().getDisplayName());
            setAttribute(ATTRIB_ENABLED_ALERT_TEMPLATES, composite.getEnabledAlertCount());
            setAttribute(ATTRIB_DISABLED_ALERT_TEMPLATES, composite.getDisabledAlertCount());
            setAttribute(ATTRIB_DISABLED_METRIC_TEMPLATES, composite.getEnabledMetricCount());
            setAttribute(ATTRIB_ENABLED_METRIC_TEMPLATES, composite.getDisabledMetricCount());

            setIsFolder(true);
        }

        public ResourceType getResourceType() {
            return resourceType;
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

            if (!this.id.equals(that.id)) {
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
            result = result * id.hashCode();
            result = result + (parentId != null ? parentId.hashCode() : 0);
            return result;
        }
    }
}
