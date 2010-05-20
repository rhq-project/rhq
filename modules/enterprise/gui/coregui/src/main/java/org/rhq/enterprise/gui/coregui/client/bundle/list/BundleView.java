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

import java.util.HashSet;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.SelectionAppearance;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;

import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.criteria.BundleCriteria;
import org.rhq.core.domain.tagging.Tag;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.Breadcrumb;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.bundle.deployment.BundleDeploymentView;
import org.rhq.enterprise.gui.coregui.client.bundle.destination.BundleDestinationListView;
import org.rhq.enterprise.gui.coregui.client.bundle.destination.BundleDestinationView;
import org.rhq.enterprise.gui.coregui.client.bundle.version.BundleVersionView;
import org.rhq.enterprise.gui.coregui.client.components.HeaderLabel;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.tagging.TagEditorView;
import org.rhq.enterprise.gui.coregui.client.components.tagging.TagsChangedCallback;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

public class BundleView extends VLayout implements BookmarkableView {

    private int bundleBeingViewed = 0;
    private HeaderLabel headerLabel;
    DynamicForm form;
    private Table bundleVersionsTable;

    private Bundle bundle;

    public BundleView() {
        super();
        setWidth100();
        setHeight100();
        setPadding(10);
        setOverflow(Overflow.AUTO);
    }

    @Override
    protected void onInit() {
        super.onInit();

    }

    public void viewBundle(Bundle bundle, ViewId nextViewId) {
        removeMembers(getMembers());

        this.bundle = bundle;

        headerLabel = new HeaderLabel("subsystems/bundle/Bundle_24.png", bundle.getName());


        TabSet tabs = new TabSet();
        Tab summaryTab = createSummaryTab();
        tabs.addTab(summaryTab);

        Tab versionsTab = createVersionsTab();
        tabs.addTab(versionsTab);

        Tab deploymentsTab = createDestinationsTab();
        tabs.addTab(deploymentsTab);

        addMember(headerLabel);
        addMember(tabs);

        if (nextViewId != null) {
            if (nextViewId.getPath().equals("versions")) {
                tabs.selectTab(versionsTab);
            } else if (nextViewId.getPath().equals("desinations")) {
                tabs.selectTab(deploymentsTab);
            }
        }

        markForRedraw();
    }

    private Tab createDestinationsTab() {
        Tab destinationsTab = new Tab("Destinations");


        destinationsTab.setPane(new BundleDestinationListView(bundle.getId()));

        return destinationsTab;
    }

    private Tab createVersionsTab() {
        Tab versionsTab = new Tab("Versions");

        bundleVersionsTable = new Table();
        bundleVersionsTable.setHeight100();

        BundleVersionDataSource bundleVersionsDataSource = new BundleVersionDataSource();
        bundleVersionsTable.setDataSource(bundleVersionsDataSource);

        bundleVersionsTable.getListGrid().getField("id").setWidth("60");
        bundleVersionsTable.getListGrid().getField("name").setWidth("25%");
        bundleVersionsTable.getListGrid().getField("name").setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                return "<a href=\"#Bundles/Bundle/" + bundle.getId() + "/versions/" + listGridRecord.getAttribute("id") + "\">" + o + "</a>";
            }
        });

        bundleVersionsTable.getListGrid().getField("version").setWidth("10%");
        bundleVersionsTable.getListGrid().getField("fileCount").setWidth("10%");
        bundleVersionsTable.getListGrid().getField("description").setWidth("*");

        bundleVersionsTable.getListGrid().setSelectionType(SelectionStyle.NONE);
        bundleVersionsTable.getListGrid().setSelectionAppearance(SelectionAppearance.ROW_STYLE);

        versionsTab.setPane(bundleVersionsTable);

        // versions tab
        BundleVersionDataSource bvDataSource;
        bvDataSource = (BundleVersionDataSource) bundleVersionsTable.getDataSource();
        bvDataSource.setBundleId(bundleBeingViewed);
        bvDataSource.fetchData();
        bundleVersionsTable.getListGrid().invalidateCache(); // TODO: is there a better way to refresh?

        return versionsTab;
    }

    private Tab createSummaryTab() {
        Tab summaryTab = new Tab("Summary");

        form = new DynamicForm();
        form.setWidth("50%");
        form.setWrapItemTitles(false);
        form.setPadding(10);

        StaticTextItem descriptionItem = new StaticTextItem("description", "Description");
        descriptionItem.setWrap(false);

        StaticTextItem versionCountItem = new StaticTextItem("versionCount", "Version Count");


        StaticTextItem latestVersionItem = new StaticTextItem("latestVersion", "Latest Version");
        latestVersionItem.setWrap(false);


        StaticTextItem liveDeployments = new StaticTextItem("liveDeployments", "Live Deployments");


        form.setFields(descriptionItem, versionCountItem, latestVersionItem, liveDeployments);


        form.setValue("description", bundle.getDescription());
        form.setValue("versionCount", bundle.getBundleVersions() != null ? bundle.getBundleVersions().size() : 0);


        HLayout layout = new HLayout();
        layout.setWidth100();

        layout.addMember(form);

        TagEditorView tagEditor = new TagEditorView(bundle.getTags(), false, new TagsChangedCallback() {
            public void tagsChanged(HashSet<Tag> tags) {
                GWTServiceLookup.getTagService().updateBundleTags(bundleBeingViewed, tags, new AsyncCallback<Void>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to update bundle's tags", caught);
                    }

                    public void onSuccess(Void result) {
                        CoreGUI.getMessageCenter().notify(new Message("Bundle tags updated", Message.Severity.Info));
                    }
                });
            }
        });
//        tagEditor.setAlwaysEdit(true);
        tagEditor.setVertical(true);
        layout.addMember(tagEditor);


        summaryTab.setPane(layout);


        return summaryTab;
    }



    public void renderView(final ViewPath viewPath) {
        int bundleId = Integer.parseInt(viewPath.getCurrent().getPath());

        final ViewId viewId = viewPath.getCurrent();

        viewPath.next();
        if (viewPath.isEnd() || viewPath.isNextEnd()) {

            if (bundleBeingViewed != bundleId) {
                bundleBeingViewed = bundleId;

                BundleCriteria criteria = new BundleCriteria();
                criteria.addFilterId(bundleId);
                criteria.fetchBundleVersions(true);
                criteria.fetchTags(true);

                GWTServiceLookup.getBundleService().findBundlesByCriteria(criteria,
                        new AsyncCallback<PageList<Bundle>>() {
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError("Failed to load bundle", caught);
                            }

                            public void onSuccess(PageList<Bundle> result) {
                                Bundle bundle = result.get(0);
                                viewId.getBreadcrumbs().set(0,new Breadcrumb(String.valueOf(bundle.getId()), bundle.getName()));
                                viewBundle(bundle, viewPath.getCurrent());
                                viewId.getBreadcrumbs().add(new Breadcrumb(String.valueOf(bundle.getId()), bundle.getName()));
                                CoreGUI.refreshBreadCrumbTrail();
                            }
                        });
            }
        } else {
            if (viewPath.getCurrent().getPath().equals("versions")) {
                if (viewPath.isEnd()) {

                    // versions list screen
                } else {
                    // one version
                    removeMembers(getMembers());
                    BundleVersionView view = new BundleVersionView();
                    addMember(view);
                    view.renderView(viewPath.next());
                }
            } else if (viewPath.getCurrent().getPath().equals("deployments")) {
                if (viewPath.isEnd()) {

                    // versions list screen
                } else {
                    // one version
                    removeMembers(getMembers());
                    BundleDeploymentView view = new BundleDeploymentView();
                    addMember(view);
                    view.renderView(viewPath.next());
                }
            } else if (viewPath.getCurrent().getPath().equals("destinations")) {
                if (viewPath.isEnd()) {

                    // versions list screen
                } else {
                    // one version
                    removeMembers(getMembers());
                    BundleDestinationView view = new BundleDestinationView();
                    addMember(view);
                    view.renderView(viewPath.next());
                }
            }
        }
    }
}
