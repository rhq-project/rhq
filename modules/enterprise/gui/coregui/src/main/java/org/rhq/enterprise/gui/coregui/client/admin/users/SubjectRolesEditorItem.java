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
package org.rhq.enterprise.gui.coregui.client.admin.users;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.admin.roles.RolesDataSource;
import org.rhq.enterprise.gui.coregui.client.components.SimpleCollapsiblePanel;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupsDataSource;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.DSOperationType;
import com.smartgwt.client.types.DragDataAction;
import com.smartgwt.client.types.DragTrackerMode;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.TransferImgButton;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VStack;

import java.util.List;
import java.util.Set;

/**
 * @author Greg Hinkle
 */
public class SubjectRolesEditorItem extends CanvasItem {

    private Set<Role> assignedRoles;
    private Subject subject;

    private ListGrid assignedRoleGrid;
    private ListGrid availableRoleGrid;

    public SubjectRolesEditorItem(String name, String title) {
        super(name, title);
        setShowTitle(false);
        setColSpan(2);
//        setWidth("90%");
        setCanvas(new SimpleCollapsiblePanel("Assigned Roles", buildForm()));
    }

    private Canvas buildForm() {

        HLayout layout = new HLayout(10);

        availableRoleGrid = new ListGrid();
        availableRoleGrid.setHeight(250);
        availableRoleGrid.setCanDragRecordsOut(true);
        availableRoleGrid.setDragTrackerMode(DragTrackerMode.RECORD);
        availableRoleGrid.setDragDataAction(DragDataAction.MOVE);
        availableRoleGrid.setDataSource(RolesDataSource.getInstance());
        availableRoleGrid.setAutoFetchData(true);
        availableRoleGrid.setFields(new ListGridField("id", 50), new ListGridField("name"));

        layout.addMember(availableRoleGrid);

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

        assignedRoleGrid = new ListGrid();
        assignedRoleGrid.setHeight(250);
        assignedRoleGrid.setCanDragRecordsOut(true);
        assignedRoleGrid.setCanAcceptDroppedRecords(true);
//        assignedRoleGrid.setDataSource(RolesDataSource.getInstance());
        assignedRoleGrid.setFields(new ListGridField("id", 50), new ListGridField("name"));

        layout.addMember(assignedRoleGrid);


        return layout;
    }

    public void setRoles(Set<Role> assignedRoles) {
        this.assignedRoles = assignedRoles;

        assignedRoleGrid.setData(RolesDataSource.getInstance().buildRecords(assignedRoles));
    }

    public Set<Role> getRoles() {
        return this.assignedRoles;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
        if (subject != null) {
            assignedRoleGrid.setCriteria(new Criteria("subjectId", String.valueOf(subject.getId())));
        }
    }
}