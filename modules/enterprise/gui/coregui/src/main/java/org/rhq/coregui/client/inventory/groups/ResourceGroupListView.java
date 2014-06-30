/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.coregui.client.inventory.groups;

import static org.rhq.coregui.client.inventory.groups.ResourceGroupDataSourceField.AVAIL_CHILDREN;
import static org.rhq.coregui.client.inventory.groups.ResourceGroupDataSourceField.AVAIL_DESCENDANTS;
import static org.rhq.coregui.client.inventory.groups.ResourceGroupDataSourceField.CATEGORY;
import static org.rhq.coregui.client.inventory.groups.ResourceGroupDataSourceField.DESCRIPTION;
import static org.rhq.coregui.client.inventory.groups.ResourceGroupDataSourceField.NAME;
import static org.rhq.coregui.client.inventory.groups.ResourceGroupDataSourceField.PLUGIN;
import static org.rhq.coregui.client.inventory.groups.ResourceGroupDataSourceField.TYPE;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.events.DoubleClickEvent;
import com.smartgwt.client.widgets.events.DoubleClickHandler;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.search.SearchSubsystem;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.PopupWindow;
import org.rhq.coregui.client.components.table.AbstractTableAction;
import org.rhq.coregui.client.components.table.AuthorizedTableAction;
import org.rhq.coregui.client.components.table.IconField;
import org.rhq.coregui.client.components.table.Table;
import org.rhq.coregui.client.components.table.TableActionEnablement;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.gwt.ResourceGroupGWTServiceAsync;
import org.rhq.coregui.client.inventory.groups.wizard.GroupCreateWizard;
import org.rhq.coregui.client.inventory.resource.detail.inventory.ResourceResourceGroupsView;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.message.Message.Severity;

/**
 * @author Greg Hinkle
 * @author Joseph Marques
 */
public class ResourceGroupListView extends Table<ResourceGroupCompositeDataSource> {

    private static final String DEFAULT_TITLE = MSG.common_title_resourceGroups();

    // our static factory method will set this to a non-null resource ID if the user can modify that resource's group membership
    private Integer resourceIdToModify = null;

    private boolean showDeleteButton = true;
    private boolean showNewButton = true;

    public ResourceGroupListView() {
        this(DEFAULT_TITLE);

    }

    public ResourceGroupListView(String title) {
        this(null, title, null);
    }

    public ResourceGroupListView(Criteria criteria, String title, String headerIcon) {
        super(title, criteria, headerIcon);

        final ResourceGroupCompositeDataSource datasource = ResourceGroupCompositeDataSource.getInstance();
        setDataSource(datasource);
        setStyleName("resourcegrouplist");
    }

    public ResourceGroupListView(Criteria criteria) {
        this(criteria, null, null);
    }

    public void setShowDeleteButton(boolean showDeleteButton) {
        this.showDeleteButton = showDeleteButton;
    }

    public void setShowNewButton(boolean showNewButton) {
        this.showNewButton = showNewButton;
    }

    @Override
    protected void configureTable() {
        ListGridField idField = new ListGridField("id", MSG.common_title_id());
        idField.setWidth(50);

        IconField categoryField = new IconField(CATEGORY.propertyName());
        categoryField.setCellFormatter(new CellFormatter() {
            public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                String categoryName = (String) value;
                GroupCategory category = GroupCategory.valueOf(categoryName);
                String icon = ImageManager.getGroupIcon(category);
                return "<img src=\"" + ImageManager.getFullImagePath(icon) + "\" />";
            }
        });
        categoryField.setShowHover(true);
        categoryField.setHoverCustomizer(new HoverCustomizer() {
            public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                String categoryName = record.getAttribute(CATEGORY.propertyName());
                GroupCategory category = GroupCategory.valueOf(categoryName);
                String displayName;
                switch (category) {
                case COMPATIBLE:
                    displayName = MSG.view_group_summary_compatible();
                    break;
                case MIXED:
                    displayName = MSG.view_group_summary_mixed();
                    break;
                default:
                    throw new IllegalStateException("Unknown group category: " + category);
                }
                return displayName;
            }
        });

        ListGridField nameField = new ListGridField(NAME.propertyName(), NAME.title());
        nameField.setWidth("40%");

        nameField.setCellFormatter(new CellFormatter() {
            public String format(Object value, ListGridRecord record, int i, int i1) {
                String groupId = record.getAttribute("id");
                String groupUrl = LinkManager.getResourceGroupLink(Integer.valueOf(groupId));
                return LinkManager.getHref(groupUrl, value.toString());
            }
        });

        ListGridField descriptionField = new ListGridField(DESCRIPTION.propertyName(), DESCRIPTION.title());
        descriptionField.setWidth("28%");

        ListGridField typeNameField = new ListGridField(TYPE.propertyName(), TYPE.title());
        typeNameField.setWidth("14%");

        ListGridField pluginNameField = new ListGridField(PLUGIN.propertyName(), PLUGIN.title());
        pluginNameField.setWidth("8%");

        ListGridField availabilityChildrenField = new ListGridField(AVAIL_CHILDREN.propertyName(),
            AVAIL_CHILDREN.title(), 185); // 185 due to the html in ResourceGroupCompositeDataSource.getAlignedAvailabilityResults
        availabilityChildrenField.setCanSortClientOnly(true);
        availabilityChildrenField.setCanGroupBy(false);
        availabilityChildrenField.setWrap(false);
        availabilityChildrenField.setAlign(Alignment.LEFT);

        ListGridField availabilityDescendantsField = new ListGridField(AVAIL_DESCENDANTS.propertyName(),
            AVAIL_DESCENDANTS.title(), 185); // 185 due to the html in ResourceGroupCompositeDataSource.getAlignedAvailabilityResults
        availabilityDescendantsField.setCanSortClientOnly(true);
        availabilityDescendantsField.setCanGroupBy(false);
        availabilityDescendantsField.setWrap(false);
        availabilityDescendantsField.setAlign(Alignment.LEFT);

        setListGridFields(idField, categoryField, nameField, descriptionField, typeNameField, pluginNameField,
            availabilityChildrenField, availabilityDescendantsField);

        if (this.showDeleteButton) {
            addTableAction(MSG.common_button_delete(), MSG.common_msg_areYouSure(), new AuthorizedTableAction(this,
                TableActionEnablement.ANY, Permission.MANAGE_INVENTORY) {
                public void executeAction(ListGridRecord[] selections, Object actionValue) {
                    int[] groupIds = new int[selections.length];
                    int index = 0;
                    for (ListGridRecord selection : selections) {
                        groupIds[index++] = selection.getAttributeAsInt("id");
                    }
                    ResourceGroupGWTServiceAsync resourceGroupManager = GWTServiceLookup.getResourceGroupService();

                    resourceGroupManager.deleteResourceGroups(groupIds, new AsyncCallback<Void>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError(MSG.view_inventory_groups_deleteFailed(), caught);
                            refreshTableInfo();
                        }

                        public void onSuccess(Void result) {
                            CoreGUI.getMessageCenter().notify(
                                new Message(MSG.view_inventory_groups_deleteSuccessful(), Severity.Info));
                            refresh(true);
                        }
                    });
                }
            });
        }

        if (this.showNewButton) {
            addTableAction(MSG.common_button_new(), new AuthorizedTableAction(this, Permission.MANAGE_INVENTORY) {
                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    GroupCategory category = null;
                    String categoryString = getInitialCriteria() == null ? null : getInitialCriteria().getAttribute(
                        ResourceGroupDataSourceField.CATEGORY.propertyName());
                    if (categoryString != null) {
                        category = GroupCategory.COMPATIBLE.name().equals(categoryString) ? GroupCategory.COMPATIBLE
                            : GroupCategory.MIXED;
                    }

                    new GroupCreateWizard(ResourceGroupListView.this, category).startWizard();
                    // we can refresh the table buttons immediately since the wizard is a dialog, the
                    // user can't access enabled buttons anyway.
                    ResourceGroupListView.this.refreshTableInfo();
                }
            });
        }

        if (this.resourceIdToModify != null) {
            addTableAction(MSG.view_tabs_common_group_membership() + "...", new AbstractTableAction(
                TableActionEnablement.ALWAYS) {
                @Override
                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    ResourceResourceGroupsView membershipView = new ResourceResourceGroupsView(
                        ResourceGroupListView.this.resourceIdToModify.intValue());

                    final PopupWindow winModal = new PopupWindow(membershipView);
                    winModal.setTitle(MSG.view_tabs_common_group_membership());
                    winModal.setWidth(700);
                    winModal.setHeight(450);
                    winModal.addCloseClickHandler(new CloseClickHandler() {
                        public void onCloseClick(CloseClickEvent event) {
                            refreshTableInfo(); // make sure we re-enable the footer buttons in case user canceled
                        }
                    });

                    membershipView.setSaveButtonHandler(new ClickHandler() {
                        @Override
                        public void onClick(ClickEvent event) {
                            winModal.markForDestroy();
                            CoreGUI.refresh();
                        }
                    });

                    winModal.show();
                }
            });
        }

        setListGridDoubleClickHandler(new DoubleClickHandler() {
            public void onDoubleClick(DoubleClickEvent event) {
                ListGrid listGrid = (ListGrid) event.getSource();
                ListGridRecord[] selectedRows = listGrid.getSelectedRecords();
                if (selectedRows != null && selectedRows.length == 1) {
                    String selectedId = selectedRows[0].getAttribute("id");
                    CoreGUI.goToView(LinkManager.getResourceGroupLink(Integer.valueOf(selectedId)));
                }
            }
        });
    }

    // -------- Static Utility loaders ------------

    public static ResourceGroupListView getGroupsOf(int explicitResourceId, boolean canModifyMembership) {

        ResourceGroupListView view = new ResourceGroupListView(new Criteria("explicitResourceId",
            String.valueOf(explicitResourceId)), MSG.common_title_resourceGroups(), null);
        if (canModifyMembership) {
            view.resourceIdToModify = Integer.valueOf(explicitResourceId);
        }

        return view;
    }

    @Override
    protected SearchSubsystem getSearchSubsystem() {
        return SearchSubsystem.GROUP;
    }

}
