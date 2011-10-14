/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeGridField;

import org.rhq.core.domain.resource.composite.ResourceTypeTemplateCountComposite;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.TitleBar;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;

/**
 * @author Jay Shaughnessy
 *
 */
public class DriftDefinitionTemplateTypeView extends ResourceTypeTreeView {

    public static final ViewName VIEW_ID = new ViewName("DriftDefTemplates", MSG.view_adminConfig_driftDefTemplates());
    public static final String VIEW_PATH = ResourceTypeTreeView.VIEW_PATH + VIEW_ID;

    public static final String ATTR_PLUGIN_TEMPLATES = "pluginTemplates";
    public static final String ATTR_USER_TEMPLATES = "userTemplates";

    public DriftDefinitionTemplateTypeView(String locatorId) {
        super(locatorId);
    }

    @Override
    protected String getEditLink(String typeId) {

        if (null == typeId) {
            return LinkManager.getAdminTemplatesLink(VIEW_ID.getName());
        }

        return LinkManager.getAdminTemplatesEditLink(VIEW_ID.getName(), typeId);
    }

    @Override
    protected TitleBar getTitleBar() {

        return new TitleBar(this, MSG.view_adminConfig_driftDefTemplates(), ImageManager.getDriftIcon());
    }

    @Override
    protected Collection<ListGridField> getAdditionalListGridFields(boolean isTreeGrid) {
        List<ListGridField> fields = new ArrayList<ListGridField>(2);

        ListGridField pluginTemplatesField = (isTreeGrid) //
        ? new TreeGridField(ATTR_PLUGIN_TEMPLATES, MSG.view_adminTemplates_pluginTemplates())
            : new ListGridField(ATTR_PLUGIN_TEMPLATES, MSG.view_adminTemplates_pluginTemplates());
        pluginTemplatesField.setWidth("150");
        pluginTemplatesField.setAlign(Alignment.CENTER);
        fields.add(pluginTemplatesField);

        ListGridField userTemplatesField = (isTreeGrid) //
        ? new TreeGridField(ATTR_USER_TEMPLATES, MSG.view_adminTemplates_userTemplates())
            : new ListGridField(ATTR_USER_TEMPLATES, MSG.view_adminTemplates_userTemplates());
        userTemplatesField.setWidth("150");
        userTemplatesField.setAlign(Alignment.CENTER);
        fields.add(userTemplatesField);

        return fields;
    }

    @Override
    protected void editTemplates(int resourceTypeId, ViewPath viewPath) {
        // TODO Auto-generated method stub

    }

    @Override
    protected ResourceTypeTreeNodeBuilder getNodeBuilderInstance(ListGrid platformsList, ListGrid platformServicesList,
        TreeGrid serversTreeGrid) {

        return new NodeBuilder(platformsList, platformServicesList, serversTreeGrid);
    }

    public static class NodeBuilder extends ResourceTypeTreeNodeBuilder {

        public NodeBuilder(ListGrid platformsList, ListGrid platformServicesList, TreeGrid serversTreeGrid) {

            super(platformsList, platformServicesList, serversTreeGrid);
        }

        @Override
        ResourceTypeListGridRecord getGridRecordInstance(ResourceTypeTemplateCountComposite composite) {

            return new GridRecord(composite);
        }

        public static class GridRecord extends ResourceTypeListGridRecord {

            public GridRecord(ResourceTypeTemplateCountComposite composite) {

                super(composite);

                setAttribute(ATTR_PLUGIN_TEMPLATES, 0);
                setAttribute(ATTR_USER_TEMPLATES, 0);
            }
        }

        @Override
        ResourceTypeTreeNode getTreeNodeInstance(ResourceTypeTemplateCountComposite composite, String plugin) {

            return new TreeNode(composite, plugin);
        }

        public static class TreeNode extends ResourceTypeTreeNode {

            public TreeNode(ResourceTypeTemplateCountComposite composite, String plugin) {

                super(composite, plugin);

                setAttribute(ATTR_PLUGIN_TEMPLATES, 0);
                setAttribute(ATTR_USER_TEMPLATES, 0);
            }
        }

    }

}
