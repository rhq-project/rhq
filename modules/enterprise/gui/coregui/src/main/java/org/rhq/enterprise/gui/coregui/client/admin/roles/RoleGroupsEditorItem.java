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
package org.rhq.enterprise.gui.coregui.client.admin.roles;

import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.components.SimpleCollapsiblePanel;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupsDataSource;

import com.smartgwt.client.types.DragDataAction;
import com.smartgwt.client.types.DragTrackerMode;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.TransferImgButton;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VStack;

/**
 * @author Greg Hinkle
 */
public class RoleGroupsEditorItem extends CanvasItem {

    private PageList<ResourceGroup> assignedGroups;

    private ListGrid assignedGroupGrid;
    private ListGrid availableGroupGrid;

    public RoleGroupsEditorItem(String name, String title) {
        super(name, title);
        setCanvas(new SimpleCollapsiblePanel("Assigned Groups", buildForm()));
    }

    private Canvas buildForm() {

        HLayout layout = new HLayout(10);

        availableGroupGrid = new ListGrid();
        availableGroupGrid.setMinHeight(350);
        availableGroupGrid.setCanDragRecordsOut(true);
        availableGroupGrid.setDragTrackerMode(DragTrackerMode.RECORD);
        availableGroupGrid.setDragDataAction(DragDataAction.MOVE);
        availableGroupGrid.setDataSource(new ResourceGroupsDataSource());
        availableGroupGrid.setAutoFetchData(true);
        availableGroupGrid.setFields(new ListGridField("id",50), new ListGridField("name"));

        layout.addMember(availableGroupGrid);

        VStack moveButtonStack = new VStack(10);
        moveButtonStack.setWidth(50);

        TransferImgButton addButton = new TransferImgButton(TransferImgButton.RIGHT);
        TransferImgButton removeButton = new TransferImgButton(TransferImgButton.LEFT);
        TransferImgButton addAllButton = new TransferImgButton(TransferImgButton.RIGHT_ALL);
        TransferImgButton removeAllButton = new TransferImgButton(TransferImgButton.LEFT_ALL);

        moveButtonStack.addMember(addButton);
        moveButtonStack.addMember(removeButton);
        moveButtonStack.addMember(addAllButton);
        moveButtonStack.addMember(removeAllButton);

        layout.addMember(moveButtonStack);

        assignedGroupGrid = new ListGrid();
        assignedGroupGrid.setMinHeight(350);
        assignedGroupGrid.setCanDragRecordsOut(true);
        assignedGroupGrid.setCanAcceptDroppedRecords(true);
        assignedGroupGrid.setDataSource(new ResourceGroupsDataSource());
        assignedGroupGrid.setFields(new ListGridField("id", 50), new ListGridField("name"));

        layout.addMember(assignedGroupGrid);


        return layout;
    }

    public void setGroups(PageList<ResourceGroup> assignedGroups) {
        this.assignedGroups = assignedGroups;

        assignedGroupGrid.setData(ResourceGroupsDataSource.buildRecords(assignedGroups));
    }

}
