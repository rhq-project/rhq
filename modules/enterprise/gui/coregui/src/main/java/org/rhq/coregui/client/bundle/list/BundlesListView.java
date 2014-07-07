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
package org.rhq.coregui.client.bundle.list;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.events.DoubleClickEvent;
import com.smartgwt.client.widgets.events.DoubleClickHandler;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.composite.BundleWithLatestVersionComposite;
import org.rhq.core.domain.criteria.BundleCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.IconEnum;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.bundle.create.BundleCreateWizard;
import org.rhq.coregui.client.bundle.deploy.BundleDeployWizard;
import org.rhq.coregui.client.components.table.BundleAuthorizedTableAction;
import org.rhq.coregui.client.components.table.RecordExtractor;
import org.rhq.coregui.client.components.table.RoleAuthorizedTableAction;
import org.rhq.coregui.client.components.table.Table;
import org.rhq.coregui.client.components.table.TableActionEnablement;
import org.rhq.coregui.client.components.table.Table.TableActionInfo.ButtonColor;
import org.rhq.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.ErrorHandler;
import org.rhq.coregui.client.util.StringUtility;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.message.Message.Severity;

/**
 * Shows a list of bundles in the system. The list gives you some actions like new, delete and deploy but
 * only if you provide the proper permissions. If you give null permissions, no action buttons are shown.
 * 
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class BundlesListView extends Table<BundlesWithLatestVersionDataSource> {

    private final Set<Permission> globalPermissions;

    /**
     * Creates a new list view.
     * 
     * @param perms if perms is null, no button actions will be shown in the table
     */
    public BundlesListView(Set<Permission> perms) {
        this(null, perms);
    }

    public BundlesListView(Criteria criteria, Set<Permission> perms) {
        super(MSG.common_title_bundles(), criteria, IconEnum.BUNDLE.getIcon24x24Path());
        this.globalPermissions = perms;
        setDataSource(new BundlesWithLatestVersionDataSource());
    }

    @Override
    protected void configureTable() {
        ListGridField idField = new ListGridField(BundlesWithLatestVersionDataSource.FIELD_ID, MSG.common_title_id());
        idField.setType(ListGridFieldType.INTEGER);
        idField.setWidth("50");

        ListGridField nameField = new ListGridField(BundlesWithLatestVersionDataSource.FIELD_NAME,
            MSG.common_title_name());
        nameField.setWidth("33%");
        nameField.setCellFormatter(new CellFormatter() {
            public String format(Object value, ListGridRecord record, int i, int i1) {
                return "<a href=\"" + record.getAttribute(BundlesWithLatestVersionDataSource.FIELD_NAMELINK) + "\">"
                    + StringUtility.escapeHtml(String.valueOf(value)) + "</a>";
            }
        });

        ListGridField descField = new ListGridField(BundlesWithLatestVersionDataSource.FIELD_DESCRIPTION,
            MSG.common_title_description());
        descField.setWidth("33%");
        descField.setCellFormatter(new CellFormatter() {
            public String format(Object value, ListGridRecord record, int i, int i1) {
                return StringUtility.escapeHtml(String.valueOf(value));
            }
        });

        ListGridField latestVersionField = new ListGridField(BundlesWithLatestVersionDataSource.FIELD_LATEST_VERSION,
            MSG.view_bundle_latestVersion());
        latestVersionField.setWidth("20%");
        latestVersionField.setAlign(Alignment.CENTER);

        ListGridField versionsCountField = new ListGridField(BundlesWithLatestVersionDataSource.FIELD_VERSIONS_COUNT,
            MSG.view_bundle_list_versionsCount());
        versionsCountField.setType(ListGridFieldType.INTEGER);
        versionsCountField.setWidth("*");
        versionsCountField.setAlign(Alignment.CENTER);

        setListGridFields(idField, nameField, descField, latestVersionField, versionsCountField);

        setListGridDoubleClickHandler(new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent event) {
                ListGrid listGrid = (ListGrid) event.getSource();
                ListGridRecord[] selectedRows = listGrid.getSelectedRecords();
                if (selectedRows != null && selectedRows.length == 1) {
                    String selectedId = selectedRows[0].getAttribute(BundlesWithLatestVersionDataSource.FIELD_ID);
                    CoreGUI.goToView(LinkManager.getBundleLink(Integer.valueOf(selectedId)));
                }
            }
        });

        // only show the buttons if we were given a set of permissions - passing in null is a way to say you only want the list, no actions 
        if (this.globalPermissions != null) {
            boolean hasGlobalDelete = globalPermissions.contains(Permission.DELETE_BUNDLES);

            addTableAction(MSG.common_button_new(), null, ButtonColor.BLUE, new RoleAuthorizedTableAction(
                BundlesListView.this, Permission.CREATE_BUNDLES, Permission.CREATE_BUNDLES_IN_GROUP) {

                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    new BundleCreateWizard(globalPermissions).startWizard();
                    // we can refresh the table buttons immediately since the wizard is a dialog, the
                    // user can't access enabled buttons anyway.
                    BundlesListView.this.refreshTableInfo();
                }
            });

            addTableAction(MSG.common_button_delete(), MSG.view_bundle_list_deleteConfirm(), ButtonColor.RED,
                new BundleAuthorizedTableAction(BundlesListView.this, TableActionEnablement.ANY,
                    (hasGlobalDelete ? null : Permission.DELETE_BUNDLES_FROM_GROUP), new RecordExtractor<Integer>() {
                        public Collection<Integer> extract(Record[] records) {
                            List<Integer> result = new ArrayList<Integer>(records.length);
                            for (Record record : records) {
                                result.add(record.getAttributeAsInt("id"));
                            }
                            return result;
                        }
                    }) {

                    public void executeAction(ListGridRecord[] selections, Object actionValue) {
                        if (selections == null || selections.length == 0) {
                            return;
                        }

                        BundlesWithLatestVersionDataSource ds = (BundlesWithLatestVersionDataSource) getDataSource();
                        final ArrayList<String> doomedNames = new ArrayList<String>(selections.length);
                        int[] doomedIds = new int[selections.length];
                        int i = 0;
                        for (ListGridRecord selection : selections) {
                            BundleWithLatestVersionComposite object = ds.copyValues(selection);
                            doomedNames.add(object.getBundleName());
                            doomedIds[i++] = object.getBundleId();
                        }

                        BundleGWTServiceAsync bundleManager = GWTServiceLookup.getBundleService();
                        bundleManager.deleteBundles(doomedIds, new AsyncCallback<Void>() {
                            public void onFailure(Throwable caught) {
                                String names = doomedNames.toString();
                                String error = ErrorHandler.getAllMessages(caught);
                                Message m = new Message(MSG.view_bundle_list_deletesFailure(), names + "<br/>\n"
                                    + error, Severity.Error);
                                CoreGUI.getMessageCenter().notify(m);
                            }

                            public void onSuccess(Void result) {
                                Message m = new Message(MSG.view_bundle_list_deletesSuccessful(), doomedNames
                                    .toString(), Severity.Info);
                                CoreGUI.getMessageCenter().notify(m);
                                CoreGUI.refresh();
                            }
                        });
                    }
                });

            addTableAction(MSG.view_bundle_deploy(), null, ButtonColor.GRAY, new RoleAuthorizedTableAction(
                BundlesListView.this, TableActionEnablement.SINGLE, Permission.DEPLOY_BUNDLES,
                Permission.DEPLOY_BUNDLES_TO_GROUP) {

                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    BundlesWithLatestVersionDataSource ds = (BundlesWithLatestVersionDataSource) getDataSource();
                    final BundleWithLatestVersionComposite object = ds.copyValues(selection[0]);
                    BundleCriteria bc = new BundleCriteria();
                    bc.addFilterId(object.getBundleId());
                    BundleGWTServiceAsync bundleManager = GWTServiceLookup.getBundleService();
                    bundleManager.findBundlesByCriteria(bc, new AsyncCallback<PageList<Bundle>>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError(
                                MSG.view_bundle_list_loadFailure(object.getBundleName()), caught);
                        }

                        public void onSuccess(PageList<Bundle> result) {
                            if (result == null || result.size() != 1) {
                                CoreGUI.getMessageCenter().notify(
                                    new Message(MSG.view_bundle_list_singleLoadFailure(object.getBundleName()),
                                        Severity.Error));
                                return;
                            }
                            new BundleDeployWizard(result.get(0).getId()).startWizard();
                            // we can refresh the table buttons immediately since the wizard is a dialog, the
                            // user can't access enabled buttons anyway.
                            BundlesListView.this.refreshTableInfo();
                        }
                    });
                }
            });
        }
    }
}
