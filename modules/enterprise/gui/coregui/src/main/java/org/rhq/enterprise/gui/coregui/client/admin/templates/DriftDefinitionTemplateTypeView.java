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
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.Timer;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.layout.Layout;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeGridField;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceTypeTemplateCountComposite;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoadedListener;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoader;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.TitleBar;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.drift.DriftDefinitionTemplatesView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Jay Shaughnessy
 *
 */
public class DriftDefinitionTemplateTypeView extends ResourceTypeTreeView {

    public static final ViewName VIEW_ID = new ViewName("DriftDefTemplates", MSG.view_adminConfig_driftDefTemplates());
    public static final String VIEW_PATH = ResourceTypeTreeView.VIEW_PATH + VIEW_ID;

    public static final String ATTR_PLUGIN_TEMPLATES = "pluginTemplates";
    public static final String ATTR_USER_TEMPLATES = "userTemplates";

    private Layout canvas;

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
    protected void editTemplates(final ResourceType type, final ViewPath viewPath) {
        new PermissionsLoader().loadExplicitGlobalPermissions(new PermissionsLoadedListener() {

            public void onPermissionsLoaded(Set<Permission> permissions) {

                Layout templatesCanvas = getCanvas();
                DriftDefinitionTemplatesView view = new DriftDefinitionTemplatesView(extendLocatorId("DriftTemplates"),
                    type, permissions.contains(Permission.MANAGE_SETTINGS));

                renderTemplateView(templatesCanvas, view, viewPath);
            }
        });
    }

    protected EnumSet<ResourceTypeRepository.MetadataType> getTypeMetadataTypes() {
        return EnumSet.of(ResourceTypeRepository.MetadataType.driftDefinitionTemplates);
    }

    private void renderTemplateView(final Layout defsHolderLayout, final DriftDefinitionTemplatesView defsView,
        final ViewPath viewPath) {

        // Don't show our back button if we are going to a template details pane which has its own back button, in
        // which case viewPath.viewsLeft() would return 1.
        boolean showBackButton = (viewPath.viewsLeft() == 0);
        prepareSubCanvas(defsHolderLayout, defsView, showBackButton);

        new Timer() {
            final long startTime = System.currentTimeMillis();

            public void run() {
                if (defsView.isInitialized()) {
                    defsView.renderView(viewPath.next());
                    switchToCanvas(DriftDefinitionTemplateTypeView.this, defsHolderLayout);

                } else {
                    long elapsedMillis = System.currentTimeMillis() - startTime;
                    if (elapsedMillis < 10000) {
                        schedule(100); // Reschedule the timer.

                    } else {
                        Log.error("Initialization of " + defsView.getClass().getName() + " timed out.");
                        // Make a last-ditch attempt to call renderView() even though the view may not be initialized.
                        defsView.renderView(viewPath.next());
                        switchToCanvas(DriftDefinitionTemplateTypeView.this, defsHolderLayout);
                    }
                }
            }
        }.run(); // fire the timer immediately
    }

    private Layout getCanvas() {
        if (this.canvas == null) {
            LocatableVLayout layout = new LocatableVLayout(extendLocatorId("metricTemplateLayout"));
            layout.setHeight100();
            layout.setWidth100();
            layout.setMargin(5);
            this.canvas = layout;
        }

        return this.canvas;
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

                setAttribute(ATTR_PLUGIN_TEMPLATES, composite.getPluginDriftTemplates());
                setAttribute(ATTR_USER_TEMPLATES, composite.getUserDriftTemplates());
                // If the type has no plugin templates then drift monitoring is not enabled for the type
                if (0 == composite.getPluginDriftTemplates()) {
                    setAttribute(ATTRIB_EDIT, ImageManager.getEditDisabledIcon());
                }
            }
        }

        @Override
        ResourceTypeTreeNode getTreeNodeInstance(ResourceTypeTemplateCountComposite composite, String plugin) {

            return new TreeNode(composite, plugin);
        }

        public static class TreeNode extends ResourceTypeTreeNode {

            public TreeNode(ResourceTypeTemplateCountComposite composite, String plugin) {

                super(composite, plugin);

                setAttribute(ATTR_PLUGIN_TEMPLATES, composite.getPluginDriftTemplates());
                setAttribute(ATTR_USER_TEMPLATES, composite.getUserDriftTemplates());
                // If the type has no plugin templates then drift monitoring is not enabled for the type
                if (0 == composite.getPluginDriftTemplates()) {
                    setAttribute(ATTRIB_EDIT, ImageManager.getEditDisabledIcon());
                }
            }
        }

    }

}
