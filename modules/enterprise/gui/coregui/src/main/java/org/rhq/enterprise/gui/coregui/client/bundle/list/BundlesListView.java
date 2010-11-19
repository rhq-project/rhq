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
package org.rhq.enterprise.gui.coregui.client.bundle.list;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.composite.BundleWithLatestVersionComposite;
import org.rhq.core.domain.criteria.BundleCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.bundle.create.BundleCreateWizard;
import org.rhq.enterprise.gui.coregui.client.bundle.deploy.BundleDeployWizard;
import org.rhq.enterprise.gui.coregui.client.components.table.AbstractTableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableActionEnablement;
import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;

/**
 * @author Greg Hinkle
 */
public class BundlesListView extends Table {

    public BundlesListView(String locatorId) {
        this(locatorId, null);
    }

    public BundlesListView(String locatorId, Criteria criteria) {
        super(locatorId, MSG.view_bundle_bundles(), criteria);
        setHeaderIcon("subsystems/bundle/Bundle_24.png");
        setDataSource(new BundlesWithLatestVersionDataSource());
    }

    @Override
    protected void configureTable() {
        getListGrid().getField("id").setWidth("60");
        getListGrid().getField("link").setWidth("25%");
        getListGrid().getField("link").setType(ListGridFieldType.LINK);
        getListGrid().getField("link").setTarget("_self");

        //        getListGrid().getField("name").setCellFormatter(new CellFormatter() {
        //            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
        //                return "";//<a href=\"#Bundles/Bundle/" + listGridRecord.getAttribute("id") + "\">" + o + "</a>";
        //            }
        //        });

        getListGrid().getField("description").setWidth("25%");
        getListGrid().getField("latestVersion").setWidth("25%");
        getListGrid().getField("versionsCount").setWidth("*");

        addTableAction(extendLocatorId("New"), MSG.common_button_new(), null, new AbstractTableAction() {
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                new BundleCreateWizard().startWizard();
            }
        });

        addTableAction(extendLocatorId("Delete"), MSG.common_button_delete(), MSG.common_msg_areYouSure(),
            new AbstractTableAction(TableActionEnablement.ANY) {
                public void executeAction(ListGridRecord[] selections, Object actionValue) {
                    BundlesWithLatestVersionDataSource ds = (BundlesWithLatestVersionDataSource) getDataSource();
                    for (ListGridRecord selection : selections) {
                        BundleGWTServiceAsync bundleManager = GWTServiceLookup.getBundleService();
                        final BundleWithLatestVersionComposite object = ds.copyValues(selection);
                        bundleManager.deleteBundle(object.getBundleId(), new AsyncCallback<Void>() {
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError(
                                    MSG.view_bundle_list_deleteFailure(object.getBundleName()), caught);
                            }

                            public void onSuccess(Void result) {
                                CoreGUI.getMessageCenter().notify(
                                    new Message(MSG.view_bundle_list_deleteSuccessful(object.getBundleName()),
                                        Severity.Info));

                                CoreGUI.refresh();
                            }
                        });
                    }
                }
            });

        // can change this back to SINGLE selection when we feel like it. currently allowing the wizard to
        // select the bundle.
        addTableAction(extendLocatorId("Deploy"), MSG.view_bundle_deploy(), null, new AbstractTableAction() {
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                if (selection.length == 0) {
                    new BundleDeployWizard().startWizard();
                    return;
                }

                BundlesWithLatestVersionDataSource ds = (BundlesWithLatestVersionDataSource) getDataSource();
                final BundleWithLatestVersionComposite object = ds.copyValues(selection[0]);
                BundleCriteria bc = new BundleCriteria();
                bc.addFilterId(object.getBundleId());
                BundleGWTServiceAsync bundleManager = GWTServiceLookup.getBundleService();
                bundleManager.findBundlesByCriteria(bc, new AsyncCallback<PageList<Bundle>>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_bundle_list_loadFailure(object.getBundleName()),
                            caught);
                    }

                    public void onSuccess(PageList<Bundle> result) {
                        if (result == null || result.size() != 1) {
                            CoreGUI.getMessageCenter().notify(
                                new Message(MSG.view_bundle_list_singleLoadFailure(object.getBundleName()),
                                    Severity.Error));
                            return;
                        }
                        new BundleDeployWizard(result.get(0).getId()).startWizard();
                    }
                });
            }
        });


    }

    public int getMatches() {
        return this.getListGrid().getTotalRows();
    }
}
