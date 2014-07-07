/*
 * RHQ Management Platform
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.rhq.coregui.client.inventory.groups.detail.inventory;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.components.table.AbstractTableAction;
import org.rhq.coregui.client.components.table.TableActionEnablement;
import org.rhq.coregui.client.components.table.Table.TableActionInfo.ButtonColor;
import org.rhq.coregui.client.inventory.resource.ResourceDatasource;
import org.rhq.coregui.client.inventory.resource.ResourceSearchView;
import org.rhq.coregui.client.util.Log;

/**
 * The content pane for the group Inventory>Members subtab.
 *
 * @author Jay Shaughnessy
 */
public class MembersView extends ResourceSearchView {

    private int groupId;
    private boolean canModifyMembers;

    public MembersView(int groupId, boolean canModifyMembers) {
        super(new Criteria(ResourceDatasource.FILTER_GROUP_ID, String.valueOf(groupId)), MSG
            .view_inventory_resources_members(), null);
        setInitialCriteriaFixed(true);
        this.canModifyMembers = canModifyMembers;
        this.groupId = groupId;
    }

    @Override
    protected void configureTable() {
        super.configureTable();

        Log.debug("MembersView canModifyMembers: " + canModifyMembers);
        if (canModifyMembers) {
            addTableAction(MSG.view_groupInventoryMembers_button_updateMembership(), ButtonColor.BLUE,
                new AbstractTableAction(                TableActionEnablement.ALWAYS) {
                @Override
                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    final Window winModal = new Window();
                    winModal.setTitle(MSG.view_groupInventoryMembers_title_updateMembership());
                    winModal.setOverflow(Overflow.VISIBLE);
                    winModal.setShowMinimizeButton(false);
                    winModal.setIsModal(true);
                    winModal.setShowModalMask(true);
                    winModal.setWidth(700);
                    winModal.setHeight(450);
                    winModal.setAutoCenter(true);
                    winModal.setShowResizer(true);
                    winModal.setCanDragResize(true);
                    winModal.centerInPage();
                    winModal.addCloseClickHandler(new CloseClickHandler() {
                        @Override
                        public void onCloseClick(CloseClickEvent event) {
                            winModal.markForDestroy();
                            MembersView.this.refreshTableInfo();
                        }
                    });

                    ResourceGroupMembershipView membershipView = new ResourceGroupMembershipView(
                        MembersView.this.groupId);

                    membershipView.setSaveButtonHandler(new ClickHandler() {

                        public void onClick(ClickEvent event) {
                            winModal.markForDestroy();
                            MembersView.this.refreshTableInfo();
                            CoreGUI.refresh();
                        }
                    });

                    membershipView.setCancelButtonHandler(new ClickHandler() {

                        public void onClick(ClickEvent event) {
                            winModal.destroy();
                            MembersView.this.refreshTableInfo();
                        }
                    });

                    winModal.addItem(membershipView);
                    winModal.show();
                }
            });
        }
    }
}
