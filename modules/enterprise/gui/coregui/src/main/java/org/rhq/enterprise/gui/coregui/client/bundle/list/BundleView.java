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
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.SelectionAppearance;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;

import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.composite.BundleWithLatestVersionComposite;
import org.rhq.core.domain.criteria.BundleCriteria;
import org.rhq.core.domain.tagging.Tag;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.Breadcrumb;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.bundle.deploy.BundleDeployWizard;
import org.rhq.enterprise.gui.coregui.client.bundle.deployment.BundleDeploymentView;
import org.rhq.enterprise.gui.coregui.client.bundle.destination.BundleDestinationListView;
import org.rhq.enterprise.gui.coregui.client.bundle.destination.BundleDestinationView;
import org.rhq.enterprise.gui.coregui.client.bundle.version.BundleVersionListView;
import org.rhq.enterprise.gui.coregui.client.bundle.version.BundleVersionView;
import org.rhq.enterprise.gui.coregui.client.components.HeaderLabel;
import org.rhq.enterprise.gui.coregui.client.components.buttons.BackButton;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.components.tagging.TagEditorView;
import org.rhq.enterprise.gui.coregui.client.components.tagging.TagsChangedCallback;
import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

public class BundleView extends VLayout implements BookmarkableView {

    private int bundleBeingViewed = 0;
    private HeaderLabel headerLabel;
    DynamicForm form;
    private Table bundleVersionsTable;

    private BundleGWTServiceAsync bundleManager = GWTServiceLookup.getBundleService();

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

        addMember(new BackButton("Back to All Bundles", "Bundles"));


        headerLabel = new HeaderLabel("subsystems/bundle/Bundle_24.png", bundle.getName());

        addMember(headerLabel);

        addMember(createSummaryForm());

        TabSet tabs = new TabSet();

        Tab versionsTab = createVersionsTab();
        tabs.addTab(versionsTab);

        Tab deploymentsTab = createDestinationsTab();
        tabs.addTab(deploymentsTab);

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

        Criteria criteria = new Criteria();
        criteria.addCriteria("bundleId", bundle.getId());

        destinationsTab.setPane(new BundleDestinationListView(criteria));

        return destinationsTab;
    }

    private Tab createVersionsTab() {
        Tab versionsTab = new Tab("Versions");

        Criteria criteria = new Criteria();
        criteria.addCriteria("bundleId", bundleBeingViewed);

        bundleVersionsTable = new BundleVersionListView(criteria);

        versionsTab.setPane(bundleVersionsTable);

        return versionsTab;
    }

    private DynamicForm createSummaryForm() {

        form = new DynamicForm();
        form.setWidth100();
        form.setColWidths("20%","30%","25%","25%");
        form.setNumCols(4);
        form.setWrapItemTitles(false);
        form.setPadding(10);

        StaticTextItem descriptionItem = new StaticTextItem("description", "Description");
        descriptionItem.setWrap(false);
        descriptionItem.setValue(bundle.getDescription());

        StaticTextItem versionCountItem = new StaticTextItem("versionCount", "Version Count");
        versionCountItem.setValue(bundle.getBundleVersions() != null ? bundle.getBundleVersions().size() : 0);

        StaticTextItem destinationsCountItem = new StaticTextItem("destinationsCount", "Destinations Count");
        destinationsCountItem.setValue(bundle.getDestinations() != null ? bundle.getDestinations().size() : 0);

        form.setFields(descriptionItem, getTagItem(), getActionItem(), versionCountItem, destinationsCountItem);

        return form;
    }

    private CanvasItem getTagItem() {

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
        tagEditor.setVertical(true);

        CanvasItem tagItem = new CanvasItem("tags");
        tagItem.setShowTitle(false);
        tagItem.setRowSpan(3);
        tagItem.setCanvas(tagEditor);

        return tagItem;
    }

    private CanvasItem getActionItem() {
        VLayout layout = new VLayout(10);

        IButton deleteButton = new IButton("Delete");
        deleteButton.setIcon("subsystems/bundle/BundleAction_Delete_16.png");
        deleteButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                SC.ask("Are you sure you want to delete this bundle?", new BooleanCallback() {
                    public void execute(Boolean aBoolean) {
                        if (aBoolean) {
                            bundleManager.deleteBundle(bundleBeingViewed, new AsyncCallback<Void>() {
                                public void onFailure(Throwable caught) {
                                    CoreGUI.getErrorHandler().handleError(
                                            "Failed to delete bundle [" + bundle.getName() + "]", caught);
                                }

                                public void onSuccess(Void result) {
                                    CoreGUI.getMessageCenter().notify(
                                            new Message("Deleted bundle [" + bundle.getName() + "]", Message.Severity.Info));
                                }
                            });
                        }
                    }
                });
            }
        });


        IButton deployButton = new IButton("Deploy");
        deployButton.setIcon("subsystems/bundle/BundleAction_Deploy_16.png");
        deployButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {

                // can change this back to SINGLE selection when we feel like it. currently allowing the wizard to
                // select the bundle.

                BundleCriteria bc = new BundleCriteria();
                bc.addFilterId(bundle.getId());
                BundleGWTServiceAsync bundleManager = GWTServiceLookup.getBundleService();
                bundleManager.findBundlesByCriteria(bc, new AsyncCallback<PageList<Bundle>>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(
                                "Failed to load bundle to deploy [" + bundle.getName() + "]", caught);
                    }

                    public void onSuccess(PageList<Bundle> result) {
                        if (result == null || result.size() != 1) {
                            CoreGUI.getMessageCenter().notify(
                                    new Message("Failed to get single bundle to deploy [" + bundle.getName() + "]",
                                            Message.Severity.Error));
                            return;
                        }
                        new BundleDeployWizard(result.get(0).getId()).startBundleWizard();
                    }
                });
            }
        });

        layout.addMember(deleteButton);
        layout.addMember(deployButton);


        CanvasItem actionItem = new CanvasItem("actions");
        actionItem.setRowSpan(3);
        actionItem.setShowTitle(false);
        actionItem.setCanvas(layout);
        return actionItem;
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
                criteria.fetchDestinations(true);
                criteria.fetchTags(true);

                GWTServiceLookup.getBundleService().findBundlesByCriteria(criteria,
                        new AsyncCallback<PageList<Bundle>>() {
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError("Failed to load bundle", caught);
                            }

                            public void onSuccess(PageList<Bundle> result) {
                                Bundle bundle = result.get(0);
                                viewId.getBreadcrumbs().set(0, new Breadcrumb(String.valueOf(bundle.getId()), bundle.getName()));
                                viewBundle(bundle, viewPath.getCurrent());
//                                viewId.getBreadcrumbs().add(new Breadcrumb(String.valueOf(bundle.getId()), bundle.getName()));
                                CoreGUI.refreshBreadCrumbTrail();
                            }
                        });
            }
        } else {
            bundleBeingViewed = 0;
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
