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
package org.rhq.enterprise.gui.coregui.client.report.operation;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.enterprise.gui.coregui.client.PopupWindow;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.operation.OperationHistoryView;

/**
 * @author Ian Springer
 * @author Jay Shaughnessy
 */
public class SubsystemOperationHistoryListView extends OperationHistoryView {

    public SubsystemOperationHistoryListView(String locatorId, boolean hasControlPermission) {
        super(locatorId, OperationHistoryView.SUBSYSTEM_VIEW_ID.getTitle(), EntityContext.forSubsystemView(),
            hasControlPermission);
    }

    @Override
    protected void configureTable() {
        super.configureTable();
        addExportAction();
    }

    private void addExportAction() {
        addTableAction("Export", "Export", new TableAction() {
            @Override
            public boolean isEnabled(ListGridRecord[] selection) {
                return true;
            }

            @Override
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                final PopupWindow exportWindow = new PopupWindow("exportSettings", null);

                VLayout layout = new VLayout();
                layout.setTitle("Export Settings");

                HLayout headerLayout = new HLayout();
                headerLayout.setAlign(Alignment.CENTER);
                Label header = new Label();
                header.setContents("Export Settings");
                header.setWidth100();
                header.setHeight(40);
                header.setPadding(20);
                //header.setStyleName("HeaderLabel");
                headerLayout.addMember(header);
                layout.addMember(headerLayout);

                HLayout formLayout = new HLayout();
                formLayout.setAlign(VerticalAlignment.TOP);

                DynamicForm form = new DynamicForm();

                SelectItem formatsList = new SelectItem("Format", "Format");
                formatsList.setValueMap("CSV", "XML");

                form.setItems(formatsList);
                formLayout.addMember(form);
                layout.addMember(formLayout);

                ToolStrip buttonBar = new ToolStrip();
                buttonBar.setAlign(Alignment.RIGHT);

                IButton finishButton = new IButton("Finish", new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        exportWindow.setVisible(false);
                        exportWindow.destroy();
                    }
                });
                buttonBar.addMember(finishButton);
                layout.addMember(buttonBar);

                exportWindow.addItem(layout);
                exportWindow.show();
                refreshTableInfo();
            }
        });
    }
}
