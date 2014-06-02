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
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Dialog;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.events.ChangeEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangeHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.events.RecordClickEvent;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeGridField;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.resource.MissingPolicy;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceTypeTemplateCountComposite;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.IconEnum;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.PermissionsLoadedListener;
import org.rhq.coregui.client.PermissionsLoader;
import org.rhq.coregui.client.components.TitleBar;
import org.rhq.coregui.client.components.form.EnumSelectItem;
import org.rhq.coregui.client.components.view.ViewName;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.coregui.client.util.FormUtility;
import org.rhq.coregui.client.util.message.Message;

/**
 * Allows a user to set the policy for missing resources on resource types that support missing resources.
 *
 * @author Jay Shaughnessy
 */
public class MissingPolicyResourceTypesView extends ResourceTypeTreeView {

    public static final ViewName VIEW_ID = new ViewName("MissingResourcePolicy",
        MSG.view_adminConfig_missingResourcePolicy(), IconEnum.SERVICES);
    public static final String VIEW_PATH = ResourceTypeTreeView.VIEW_PATH + VIEW_ID;
    private static final String ATTR_POLICY = "policy";

    public MissingPolicyResourceTypesView() {
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
        return new TitleBar(MSG.view_adminConfig_missingResourcePolicy(),
            ImageManager.getResourceIcon(ResourceCategory.SERVICE));
    }

    @Override
    protected Collection<ListGridField> getAdditionalListGridFields(boolean isTreeGrid) {
        List<ListGridField> fields = new ArrayList<ListGridField>(1);

        ListGridField policyField = (isTreeGrid) ? new TreeGridField(ATTR_POLICY, MSG.common_title_policy())
            : new ListGridField(ATTR_POLICY, MSG.common_title_policy());
        policyField.setWidth("150");
        policyField.setAlign(Alignment.CENTER);
        policyField.setCanEdit(false);
        fields.add(policyField);

        return fields;
    }

    @Override
    protected void editTemplates(final ResourceType type, final RecordClickEvent event) {
        new PermissionsLoader().loadExplicitGlobalPermissions(new PermissionsLoadedListener() {
            public void onPermissionsLoaded(Set<Permission> permissions) {
                if (!permissions.contains(Permission.MANAGE_INVENTORY)) {
                    SC.warn(MSG.view_adminConfig_missingResourcePolicy_noperm());
                    return;
                }

                new MissingPolicyDialog(type, event).show();
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
                setAttribute(ATTR_POLICY, composite.getType().getMissingPolicy().name());
                if (!composite.getType().isSupportsMissingAvailabilityType()) {
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
                setAttribute(ATTR_POLICY, composite.getType().getMissingPolicy().name());
                if (!composite.getType().isSupportsMissingAvailabilityType()) {
                    setAttribute(ATTRIB_EDIT, ImageManager.getEditDisabledIcon());
                }
            }

            @Override
            public ResourceTypeTreeNode copy() {
                ResourceTypeTreeNode dup = super.copy();
                dup.setAttribute(ATTR_POLICY, this.getAttribute(ATTR_POLICY));
                return dup;
            }
        }
    }

    static public String getPolicyDisplay(MissingPolicy policy) {
        switch (policy) {
        case DOWN:
            return MSG.common_status_avail_down();
        case IGNORE:
            return MSG.view_adminConfig_missingResourcePolicy_ignore();
        default:
            return MSG.view_adminConfig_missingResourcePolicy_uninventory();
        }
    }

    private class MissingPolicyDialog extends Dialog {
        private EnumSelectItem policyItem;

        public MissingPolicyDialog(final ResourceType type, final RecordClickEvent recordClickEvent) {
            super();

            setIsModal(true);
            setTitle(AncestryUtil.getFormattedType(type));
            setShowToolbar(false);
            setDismissOnEscape(true);
            setDismissOnOutsideClick(true);
            setWidth(450); // wide enough to fit large type names in the title

            final DynamicForm form = new DynamicForm();

            policyItem = new EnumSelectItem(ATTR_POLICY, MSG.common_title_policy(), MissingPolicy.class, null, null);
            policyItem.setValue(type.getMissingPolicy().name());
            policyItem.setMultiple(false);
            policyItem.setShowTitle(true);
            policyItem.addChangeHandler(new ChangeHandler() {
                public void onChange(final ChangeEvent event) {
                    // by canceling the selection remain unchanged if the user does not confirm. If he does confirm
                    // the dialog gets destroyed anyway.
                    event.cancel();

                    final MissingPolicy newPolicy = MissingPolicy.valueOf((String) event.getValue());
                    String msg = MSG.view_adminConfig_missingResourcePolicy_confirm(getPolicyDisplay(newPolicy),
                        AncestryUtil.getFormattedType(type));

                    SC.ask(MSG.common_msg_areYouSure(), msg, new BooleanCallback() {
                        public void execute(Boolean value) {
                            if (Boolean.TRUE.equals(value)) {
                                // call server to flip flag on type
                                GWTServiceLookup.getResourceTypeGWTService().setResourceTypeMissingPolicy(type.getId(),
                                    newPolicy, new AsyncCallback<Void>() {
                                        public void onSuccess(Void result) {
                                            // this type reference is inside our cache so make sure we update it
                                            type.setMissingPolicy(newPolicy);

                                            String msg = MSG.view_adminConfig_missingResourcePolicy_success(
                                                getPolicyDisplay(newPolicy), type.getName());
                                            CoreGUI.getMessageCenter().notify(new Message(msg));

                                            // refresh the grid
                                            recordClickEvent.getRecord()
                                                .setAttribute(ATTR_POLICY, newPolicy.toString());
                                            recordClickEvent.getViewer().markForRedraw();

                                            MissingPolicyDialog.this.destroy();
                                        }

                                        public void onFailure(Throwable caught) {
                                            CoreGUI.getErrorHandler().handleError(
                                                MSG.view_adminConfig_missingResourcePolicy_failure(), caught);
                                            MissingPolicyDialog.this.destroy();
                                        }
                                    });
                            }
                        }
                    });
                }
            });

            FormUtility.addContextualHelp(policyItem, MSG.view_adminConfig_missingResourcePolicy_tooltip());
            form.setFields(policyItem);
            addItem(form);
        }

        @Override
        public void show() {
            super.show();
            policyItem.focusInItem();
        }
    }

}
