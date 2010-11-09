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

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.enterprise.gui.coregui.client.components.table.AbstractTableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.BooleanCellFormatter;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableActionEnablement;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * A table that lists all users and provides the ability to view details of or delete those users and to create new
 * users.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class UsersView extends TableSection {
    public static final String VIEW_ID = "Users";

    private static final String TITLE = "Users";

    private static final int ID_OVERLORD = 1;
    private static final int ID_RHQADMIN = 2;

    private static final String HEADER_ICON = "global/User_24.png";

    public UsersView(String locatorId) {
        super(locatorId, TITLE);

        final UsersDataSource datasource = UsersDataSource.getInstance();

        setDataSource(datasource);
        setHeaderIcon(HEADER_ICON);
    }

    @Override
    public void setDataSource(RPCDataSource dataSource) {
        super.setDataSource(dataSource);
        // TODO: Remove this once the setFields() bug has been resolved.
        ListGrid grid = getListGrid();
        if (grid != null) {
            grid.setCellFormatter(new CellFormatter() {
                public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                     if (value instanceof Boolean) {
                         return new BooleanCellFormatter().format(value, record, rowNum, colNum);
                     } else {
                         return String.valueOf(value);
                     }
                }
            });
        }
    }

    @Override
    protected void configureTable() {
        final ListGrid grid = getListGrid();
        
        ListGridField nameField = new ListGridField(UsersDataSource.Field.NAME, 120);

        ListGridField hasPrincipalField = new ListGridField(UsersDataSource.Field.HAS_PRINCIPAL, 50);
        hasPrincipalField.setCellFormatter(new BooleanCellFormatter());

        ListGridField firstNameField = new ListGridField(UsersDataSource.Field.FIRST_NAME, 120);

        ListGridField lastNameField = new ListGridField(UsersDataSource.Field.LAST_NAME, 120);

        ListGridField departmentField = new ListGridField(UsersDataSource.Field.DEPARTMENT, 120);

        ListGridField phoneNumberField = new ListGridField(UsersDataSource.Field.PHONE_NUMBER, 120);

        ListGridField emailAddressField = new ListGridField(UsersDataSource.Field.EMAIL_ADDRESS, 120);

        ListGridField activeField = new ListGridField(UsersDataSource.Field.FACTIVE, 50);
        activeField.setCellFormatter(new BooleanCellFormatter());

        // TODO: Uncomment this once the setFields() bug has been resolved.
        //setListGridFields(nameField, hasPrincipalField, firstNameField, lastNameField, departmentField,
        //    phoneNumberField, emailAddressField, activeField);
        
        addTableAction(extendLocatorId("Delete"), "Delete",
            "Are you sure you want to delete # users?", new TableAction() {
                public boolean isEnabled(ListGridRecord[] selection) {
                    int count = selection.length;
                    if (count == 0) {
                        return false;
                    }

                    for (ListGridRecord record : selection) {
                        int id = record.getAttributeAsInt("id");
                        if (id == ID_OVERLORD || id == ID_RHQADMIN) {
                            // The superuser and rhqadmin users cannot be deleted.
                            return false;
                        }
                    }
                    return true;
                }

                public void executeAction(ListGridRecord[] selection) {
                    grid.removeSelectedData();
                }
            });

        addTableAction(extendLocatorId("New"), "New", new AbstractTableAction(TableActionEnablement.ALWAYS) {
            public void executeAction(ListGridRecord[] selection) {
                newDetails();
            }
        });
    }

    public Canvas getDetailsView(int id) {
        final UserEditView userEditor = new UserEditView(extendLocatorId("Detail"));

        return userEditor;
    }
}
