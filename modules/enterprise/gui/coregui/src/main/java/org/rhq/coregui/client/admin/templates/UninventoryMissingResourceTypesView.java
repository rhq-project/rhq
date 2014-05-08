/*
 * RHQ Management Platform
 * Copyright 2014, Red Hat Middleware LLC, and individual contributors
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

package org.rhq.coregui.client.admin.templates;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.events.RecordClickEvent;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeGridField;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceTypeTemplateCountComposite;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.IconEnum;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.PermissionsLoadedListener;
import org.rhq.coregui.client.PermissionsLoader;
import org.rhq.coregui.client.components.TitleBar;
import org.rhq.coregui.client.components.view.ViewName;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.message.Message;

/**
 * Allows a user to set the "uninventory missing resources" option on specific resource types.
 *
 * @author Jay Shaughnessy
 * @author John Mazzitelli
 */
public class UninventoryMissingResourceTypesView extends ResourceTypeTreeView {

    public static final ViewName VIEW_ID = new ViewName("UninventoryMissingResourceTypes",
        MSG.view_adminConfig_uninventoryMissingResourceTypes(), IconEnum.SERVICES);
    public static final String VIEW_PATH = ResourceTypeTreeView.VIEW_PATH + VIEW_ID;
    private static final String ATTR_ENABLED = "enabled";

    public UninventoryMissingResourceTypesView() {
        super();
    }

    @Override
    protected String getEditColumnTitle() {
        return MSG.view_adminConfig_changeTitle();
    }

    @Override
    protected String getEditLink(String typeId) {
        return null; // there is no separate edit canvas, we'll do it inline inside editTemplates
    }

    @Override
    protected TitleBar getTitleBar() {
        return new TitleBar(MSG.view_adminConfig_uninventoryMissingResourceTypes(),
            ImageManager.getResourceIcon(ResourceCategory.SERVICE));
    }

    @Override
    protected Collection<ListGridField> getAdditionalListGridFields(boolean isTreeGrid) {
        List<ListGridField> fields = new ArrayList<ListGridField>(1);

        ListGridField enabledTemplatesField = (isTreeGrid) ? new TreeGridField(ATTR_ENABLED, MSG.common_title_enabled())
            : new ListGridField(ATTR_ENABLED, MSG.common_title_enabled());
        enabledTemplatesField.setWidth("150");
        enabledTemplatesField.setAlign(Alignment.CENTER);
        // enabledTemplatesField.setPrompt(MSG.common_title_enabled());
        enabledTemplatesField.setType(ListGridFieldType.IMAGE);
        fields.add(enabledTemplatesField);

        return fields;
    }

    @Override
    protected void editTemplates(final ResourceType type, final RecordClickEvent event) {
        new PermissionsLoader().loadExplicitGlobalPermissions(new PermissionsLoadedListener() {
            public void onPermissionsLoaded(Set<Permission> permissions) {
                if (!permissions.contains(Permission.MANAGE_INVENTORY)) {
                    SC.warn(MSG.view_adminConfig_uninventoryMissingResourceTypes_noperm());
                    return;
                }

                String msg = type.isUninventoryMissing() ? MSG
                    .view_adminConfig_uninventoryMissingResourceTypes_confirmOff(type.getName()) : MSG
                    .view_adminConfig_uninventoryMissingResourceTypes_confirmOn(type.getName());

                SC.ask(MSG.common_msg_areYouSure(), msg, new BooleanCallback() {
                    public void execute(Boolean value) {
                        if (Boolean.TRUE.equals(value)) {
                            // call server to flip flag on type
                            GWTServiceLookup.getResourceTypeGWTService().setResourceTypeUninventoryMissingFlag(
                                type.getId(), !type.isUninventoryMissing(), new AsyncCallback<Void>() {
                                    public void onSuccess(Void result) {
                                        // this type reference is inside our cache so make sure we update it
                                        type.setUninventoryMissing(!type.isUninventoryMissing());

                                        String msg = type.isUninventoryMissing() ? MSG
                                            .view_adminConfig_uninventoryMissingResourceTypes_successOn(type.getName())
                                            : MSG.view_adminConfig_uninventoryMissingResourceTypes_successOff(type
                                                .getName());
                                        CoreGUI.getMessageCenter().notify(new Message(msg));

                                        // refresh the listgrid
                                        // (note: try as I might, could not figure out how to get the 1 listgrid to refresh, so do all of them here)
                                        CoreGUI.refresh();
                                    }

                                    public void onFailure(Throwable caught) {
                                        CoreGUI.getErrorHandler().handleError(
                                            MSG.view_adminConfig_uninventoryMissingResourceTypes_failure(), caught);
                                    }
                                });
                        }
                    }
                });
            }
        });
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
                setAttribute(ATTR_ENABLED, ImageManager.getAvailabilityIcon(composite.getType().isUninventoryMissing()));
            }
        }

        @Override
        ResourceTypeTreeNode getTreeNodeInstance(ResourceTypeTemplateCountComposite composite, String plugin) {
            return new TreeNode(composite, plugin);
        }

        public static class TreeNode extends ResourceTypeTreeNode {
            public TreeNode(ResourceTypeTemplateCountComposite composite, String plugin) {
                super(composite, plugin);
                setAttribute(ATTR_ENABLED, ImageManager.getAvailabilityIcon(composite.getType().isUninventoryMissing()));
            }

            @Override
            public ResourceTypeTreeNode copy() {
                ResourceTypeTreeNode dup = super.copy();
                dup.setAttribute(ATTR_ENABLED, this.getAttribute(ATTR_ENABLED));
                return dup;
            }
        }
    }
}
