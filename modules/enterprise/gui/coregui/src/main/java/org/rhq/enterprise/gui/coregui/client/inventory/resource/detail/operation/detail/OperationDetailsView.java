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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.detail;

import java.util.Date;

import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;

/**
 * @author Greg Hinkle
 */
public class OperationDetailsView extends VLayout {

    OperationDefinition definition;
    ResourceOperationHistory operationHistory;

    public OperationDetailsView(OperationDefinition definition, ResourceOperationHistory operationHistory) {
        this.definition = definition;
        this.operationHistory = operationHistory;
    }

    @Override
    protected void onDraw() {
        super.onDraw();


        // Information Form

        DynamicForm form = new DynamicForm();

        StaticTextItem operationItem = new StaticTextItem("operation", "Operation");
        operationItem.setValue(definition.getName());


        StaticTextItem submittedItem = new StaticTextItem("submitted", "Date Submitted");
        submittedItem.setValue(new Date(operationHistory.getStartedTime()));

        StaticTextItem completedItem = new StaticTextItem("completed", "Date Completed");
        completedItem.setValue(new Date(operationHistory.getStartedTime() + operationHistory.getDuration()));


        StaticTextItem requesterItem = new StaticTextItem("requester", "Requester");
        requesterItem.setValue(operationHistory.getSubjectName());

        StaticTextItem statusItem = new StaticTextItem("status", "Status");
        statusItem.setValue(operationHistory.getStatus().name());

        /*
        Operation:  	View Process List
        Date Submitted: 	3/11/10, 12:24:02 PM, EST
        Date Completed: 	3/11/10, 12:24:03 PM, EST
        Requester: 	rhqadmin
        Status: 	Success
        */

        form.setItems(operationItem, submittedItem, completedItem, requesterItem,statusItem);

        addMember(form);

        // Results configuration view


        if (operationHistory.getResults() != null) {

            ConfigurationEditor resultsEditor = new ConfigurationEditor(definition.getResultsConfigurationDefinition(), operationHistory.getResults());

            addMember(resultsEditor);

        }
    }



    public static void displayDetailsDialog(ResourceOperationHistory operationHistory) {

        OperationDetailsView detailsView = new OperationDetailsView(operationHistory.getOperationDefinition(), operationHistory);


        Window window = new Window();
        window.setTitle(operationHistory.getOperationDefinition().getDisplayName() + " History");
        window.setWidth(900);
        window.setHeight(900);
        window.setIsModal(true);
        window.setShowModalMask(true);
        window.setCanDragResize(true);
        window.centerInPage();
        window.addItem(detailsView);
        window.show();

    }

}
