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
package org.rhq.coregui.client.admin.roles;

import java.util.ArrayList;
import java.util.List;

import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.CellClickEvent;
import com.smartgwt.client.widgets.grid.events.CellClickHandler;

import org.rhq.coregui.client.IconEnum;
import org.rhq.coregui.client.admin.AdministrationView;
import org.rhq.coregui.client.components.table.EscapedHtmlCellFormatter;
import org.rhq.coregui.client.components.table.TableAction;
import org.rhq.coregui.client.components.table.TableSection;
import org.rhq.coregui.client.components.view.HasViewName;
import org.rhq.coregui.client.components.view.ViewName;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton.ButtonColor;

/**
 * A table that lists all roles and provides the ability to view details of or delete those roles and to create new
 * roles. 
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class RolesView extends TableSection<RolesDataSource> implements HasViewName {

    public static final ViewName VIEW_ID = new ViewName("Roles", MSG.common_title_roles(), IconEnum.ROLES);

    public static final String VIEW_PATH = AdministrationView.VIEW_ID + "/"
        + AdministrationView.SECTION_SECURITY_VIEW_ID + "/" + VIEW_ID;

    private boolean hasManageSecurity;

    public RolesView(boolean hasManageSecurity) {
        super(null);

        final RolesDataSource datasource = RolesDataSource.getInstance();
        setDataSource(datasource);
        setEscapeHtmlInDetailsLinkColumn(true);

        this.hasManageSecurity = hasManageSecurity;
    }

    @Override
    protected void configureTable() {
        updateSelectionStyle();
        getListGrid().addCellClickHandler(new CellClickHandler() {
            public void onCellClick(CellClickEvent event) {
                updateSelectionStyle();
            }
        });

        List<ListGridField> fields = createFields();
        setListGridFields(fields.toArray(new ListGridField[fields.size()]));

        addTableAction(MSG.common_button_new(), ButtonColor.BLUE, createNewAction());
        addTableAction(MSG.common_button_delete(), getDeleteConfirmMessage(), ButtonColor.RED, createDeleteAction());

        super.configureTable();
    }

    @Override
    public void refresh() {
        super.refresh();

        updateSelectionStyle();
    }

    @Override
    protected boolean isDetailsEnabled() {
        // Non-superusers cannot view or edit roles.
        return this.hasManageSecurity;
    }

    private List<ListGridField> createFields() {
        List<ListGridField> fields = new ArrayList<ListGridField>();

        ListGridField nameField = new ListGridField(RolesDataSource.Field.NAME, 150);
        fields.add(nameField);

        ListGridField descriptionField = new ListGridField(RolesDataSource.Field.DESCRIPTION);
        descriptionField.setCellFormatter(new EscapedHtmlCellFormatter());
        fields.add(descriptionField);

        return fields;
    }

    private TableAction createNewAction() {
        return new TableAction() {
            public boolean isEnabled(ListGridRecord[] selection) {
                return hasManageSecurity;
            }

            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                newDetails();
            }
        };
    }

    private TableAction createDeleteAction() {
        return new TableAction() {
            public boolean isEnabled(ListGridRecord[] selection) {
                if (!hasManageSecurity) {
                    return false;
                }

                int count = selection.length;
                if (count == 0) {
                    return false;
                }

                for (ListGridRecord record : selection) {
                    int roleId = record.getAttributeAsInt(RolesDataSource.Field.ID);
                    if (RolesDataSource.isSystemRoleId(roleId)) {
                        // The superuser role cannot be deleted.
                        return false;
                    }
                }
                return true;
            }

            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                deleteSelectedRecords();
            }
        };
    }

    private void updateSelectionStyle() {
        if (!hasManageSecurity) {
            getListGrid().deselectAllRecords();
        }
        SelectionStyle selectionStyle = hasManageSecurity ? getDefaultSelectionStyle() : SelectionStyle.NONE;
        getListGrid().setSelectionType(selectionStyle);
    }

    @Override
    public Canvas getDetailsView(Integer roleId) {
        return new RoleEditView(roleId);
    }

    @Override
    protected String getDeleteConfirmMessage() {
        return MSG.common_msg_deleteConfirm(MSG.common_label_roles());
    }

    @Override
    public ViewName getViewName() {
        return VIEW_ID;
    }
}