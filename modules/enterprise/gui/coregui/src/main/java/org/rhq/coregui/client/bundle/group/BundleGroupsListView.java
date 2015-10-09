/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.coregui.client.bundle.group;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.events.DoubleClickEvent;
import com.smartgwt.client.widgets.events.DoubleClickHandler;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.bundle.BundleGroup;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.IconEnum;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.components.table.AbstractTableAction;
import org.rhq.coregui.client.components.table.TableActionEnablement;
import org.rhq.coregui.client.components.table.TableSection;
import org.rhq.coregui.client.components.view.HasViewName;
import org.rhq.coregui.client.components.view.ViewName;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.ErrorHandler;
import org.rhq.coregui.client.util.StringUtility;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton.ButtonColor;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.message.Message.Severity;

/**
 * Shows a list of bundle groups in the system. The list gives you some actions but proper permissions are required.
 *
 * @author Jay Shaughnessy
 */
public class BundleGroupsListView extends TableSection<BundleGroupsDataSource> implements HasViewName {

    public static final ViewName VIEW_ID = new ViewName("BundleGroups", MSG.common_title_bundleGroups(),
        IconEnum.BUNDLE_GROUP);

    private final Set<Permission> globalPermissions;

    /**
     * Creates a new list view.
     *
     * @param globalPermissions if null, no buttons will be active, otherwise normal authz in place
     */
    public BundleGroupsListView(Set<Permission> globalPermissions) {
        this(null, globalPermissions);
    }

    public BundleGroupsListView(Criteria criteria, Set<Permission> globalpermissions) {
        super(MSG.common_title_bundleGroups(), criteria);
        this.globalPermissions = (null != globalpermissions) ? globalpermissions : new HashSet<Permission>();
        setTitleIcon(IconEnum.BUNDLE_GROUP.getIcon24x24Path());
        setDataSource(new BundleGroupsDataSource());
    }

    @Override
    protected void configureTable() {
        ListGridField idField = new ListGridField(BundleGroupsDataSource.FIELD_ID, MSG.common_title_id());
        idField.setType(ListGridFieldType.INTEGER);
        idField.setWidth("50");

        ListGridField nameField = new ListGridField(BundleGroupsDataSource.FIELD_NAME, MSG.common_title_name());
        nameField.setWidth("33%");
        nameField.setCellFormatter(new CellFormatter() {
            public String format(Object value, ListGridRecord record, int i, int i1) {
                return "<a href=\"" + record.getAttribute(BundleGroupsDataSource.FIELD_NAMELINK) + "\">"
                    + StringUtility.escapeHtml(String.valueOf(value)) + "</a>";
            }
        });

        ListGridField descField = new ListGridField(BundleGroupsDataSource.FIELD_DESCRIPTION,
            MSG.common_title_description());
        descField.setWidth("*");
        descField.setCellFormatter(new CellFormatter() {
            public String format(Object value, ListGridRecord record, int i, int i1) {
                return StringUtility.escapeHtml(String.valueOf(value));
            }
        });

        setListGridFields(idField, nameField, descField);

        setListGridDoubleClickHandler(new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent event) {
                ListGrid listGrid = (ListGrid) event.getSource();
                ListGridRecord[] selectedRows = listGrid.getSelectedRecords();
                if (selectedRows != null && selectedRows.length == 1) {
                    String selectedId = selectedRows[0].getAttribute(BundleGroupsDataSource.FIELD_ID);
                    CoreGUI.goToView(LinkManager.getBundleGroupLink(Integer.valueOf(selectedId)));
                }
            }
        });

        boolean hasAuthz = globalPermissions.contains(Permission.MANAGE_BUNDLE_GROUPS);

        addTableAction(MSG.common_button_new(), null, ButtonColor.BLUE, new AbstractTableAction(
            (hasAuthz) ? TableActionEnablement.ALWAYS : TableActionEnablement.NEVER) {
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                newDetails();
            }
        });

        addTableAction(MSG.common_button_delete(), MSG.view_bundleGroup_deleteConfirm(), ButtonColor.RED,
            new AbstractTableAction((hasAuthz) ? TableActionEnablement.ANY : TableActionEnablement.NEVER) {
                public void executeAction(ListGridRecord[] selections, Object actionValue) {
                    if (selections == null || selections.length == 0) {
                        return;
                    }

                    BundleGroupsDataSource ds = getDataSource();
                    final ArrayList<String> doomedNames = new ArrayList<String>(selections.length);
                    int[] doomedIds = new int[selections.length];
                    int i = 0;
                    for (ListGridRecord selection : selections) {
                        BundleGroup object = ds.copyValues(selection);
                        doomedNames.add(object.getName());
                        doomedIds[i++] = object.getId();
                    }

                    GWTServiceLookup.getBundleService().deleteBundleGroups(doomedIds, new AsyncCallback<Void>() {
                        public void onFailure(Throwable caught) {
                            String names = doomedNames.toString();
                            String error = ErrorHandler.getAllMessages(caught);
                            Message m = new Message(MSG.view_bundleGroup_deletesFailure(), names + "<br/>\n" + error,
                                Severity.Error);
                            CoreGUI.getMessageCenter().notify(m);
                        }

                        public void onSuccess(Void result) {
                            Message m = new Message(MSG.view_bundleGroup_deletesSuccessful(), doomedNames.toString(),
                                Severity.Info);
                            CoreGUI.getMessageCenter().notify(m);
                            CoreGUI.refresh();
                        }
                    });
                }
            });
    }

    @Override
    public void newDetails() {
        // protect against the fact that we may not have a basepath set if we have not been navigated to directly
        // (for example, we may be in a section stack)
        if (null != getBasePath()) {
            super.newDetails();
        } else {
            CoreGUI.goToView(LinkManager.getBundleGroupLink(0));
        }
    }

    @Override
    public Canvas getDetailsView(Integer bundleGroupId) {
        return new BundleGroupEditView(globalPermissions, bundleGroupId);
    }

    @Override
    public ViewName getViewName() {
        return VIEW_ID;
    }

}
