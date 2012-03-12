/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.gui.coregui.client.drift;

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

public class SubsystemResourceDriftView extends DriftHistoryView {
    public SubsystemResourceDriftView(String locatorId, boolean hasWriteAccess) {
        super(locatorId, MSG.common_title_recent_drifts(), EntityContext.forSubsystemView(), hasWriteAccess);
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
