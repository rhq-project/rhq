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
import java.util.Set;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.criteria.BundleCriteria;
import org.rhq.core.domain.tagging.Tag;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.bundle.BundleTopView;
import org.rhq.enterprise.gui.coregui.client.bundle.deploy.BundleDeployWizard;
import org.rhq.enterprise.gui.coregui.client.bundle.deployment.BundleDeploymentView;
import org.rhq.enterprise.gui.coregui.client.bundle.destination.BundleDestinationListView;
import org.rhq.enterprise.gui.coregui.client.bundle.destination.BundleDestinationView;
import org.rhq.enterprise.gui.coregui.client.bundle.version.BundleVersionListView;
import org.rhq.enterprise.gui.coregui.client.bundle.version.BundleVersionView;
import org.rhq.enterprise.gui.coregui.client.components.HeaderLabel;
import org.rhq.enterprise.gui.coregui.client.components.buttons.BackButton;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.tagging.TagEditorView;
import org.rhq.enterprise.gui.coregui.client.components.tagging.TagsChangedCallback;
import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTab;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTabSet;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

public class BundleView extends LocatableVLayout implements BookmarkableView {
    private LocatableDynamicForm form;

    private int bundleBeingViewed = 0;
    private HeaderLabel headerLabel;
    private Table bundleVersionsTable;
    private TabSet tabs;
    private Tab versionsTab;
    private Tab destinationsTab;

    private BundleGWTServiceAsync bundleManager = GWTServiceLookup.getBundleService();
    private Bundle bundle;
    private final boolean canManageBundles;

    public BundleView(String locatorId, Set<Permission> perms) {
        super(locatorId);
        this.canManageBundles = (perms != null) ? perms.contains(Permission.MANAGE_BUNDLE) : false;
        setWidth100();
        setHeight100();
        setMargin(10);
        setOverflow(Overflow.AUTO);
    }

    public void viewBundle(Bundle bundle, ViewId nextViewId) {
        // Whenever a new view request comes in, make sure to clean house to avoid ID conflicts for sub-widgets
        this.destroyMembers();

        this.bundle = bundle;

        BackButton backButton = new BackButton(extendLocatorId("BackButton"), MSG.view_bundle_list_backToAll(),
            BundleTopView.VIEW_ID.getTitle());
        headerLabel = new HeaderLabel("subsystems/bundle/Bundle_24.png", bundle.getName());
        tabs = new LocatableTabSet(getLocatorId());
        versionsTab = createVersionsTab();
        destinationsTab = createDestinationsTab();
        tabs.addTab(versionsTab);
        tabs.addTab(destinationsTab);

        addMember(backButton);
        addMember(headerLabel);
        addMember(createTagEditor());
        addMember(createSummaryForm());
        addMember(tabs);

        // select the correct tab based on what URL the user is going to (based on what tree node was selected)
        if ((null == nextViewId) || (nextViewId.getPath().equals("versions"))) {
            tabs.selectTab(versionsTab);
        } else if (nextViewId.getPath().equals("destinations")) {
            tabs.selectTab(destinationsTab);
        }

        markForRedraw();
    }

    private TagEditorView createTagEditor() {
        boolean readOnly = !this.canManageBundles;
        TagEditorView tagEditor = new TagEditorView(extendLocatorId("TagEditor"), bundle.getTags(), readOnly,
            new TagsChangedCallback() {
                public void tagsChanged(HashSet<Tag> tags) {
                    GWTServiceLookup.getTagService().updateBundleTags(bundleBeingViewed, tags,
                        new AsyncCallback<Void>() {
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError(MSG.view_bundle_list_tagUpdateFailure(), caught);
                            }

                            public void onSuccess(Void result) {
                                CoreGUI.getMessageCenter().notify(
                                    new Message(MSG.view_bundle_list_tagUpdateSuccessful(), Message.Severity.Info));
                            }
                        });
                }
            });
        tagEditor.setAutoHeight();
        tagEditor.setExtraSpace(10);
        return tagEditor;
    }

    private Tab createDestinationsTab() {
        LocatableTab destinationsTab = new LocatableTab(extendLocatorId("Destinations"), MSG.view_bundle_destinations());
        Criteria criteria = new Criteria();
        criteria.addCriteria("bundleId", bundle.getId());
        destinationsTab.setPane(new BundleDestinationListView(destinationsTab.getLocatorId(), criteria));
        return destinationsTab;
    }

    private Tab createVersionsTab() {
        LocatableTab versionsTab = new LocatableTab(extendLocatorId("Versions"), MSG.view_bundle_versions());
        Criteria criteria = new Criteria();
        criteria.addCriteria("bundleId", bundleBeingViewed);
        bundleVersionsTable = new BundleVersionListView(versionsTab.getLocatorId(), criteria);
        versionsTab.setPane(bundleVersionsTable);
        return versionsTab;
    }

    private DynamicForm createSummaryForm() {

        form = new LocatableDynamicForm(extendLocatorId("Summary"));
        form.setWidth100();
        form.setColWidths("20%", "40%", "40%");
        form.setNumCols(3);
        form.setWrapItemTitles(false);
        form.setExtraSpace(10);
        form.setIsGroup(true);
        form.setGroupTitle(MSG.common_title_summary());
        form.setPadding(5);

        CanvasItem actionItem = new CanvasItem("actions");
        actionItem.setColSpan(1);
        actionItem.setRowSpan(3);
        actionItem.setShowTitle(false);
        actionItem.setCanvas(getActionCanvas());

        StaticTextItem versionCountItem = new StaticTextItem("versionCount", MSG.view_bundle_list_versionsCount());
        versionCountItem.setValue(bundle.getBundleVersions() != null ? bundle.getBundleVersions().size() : 0);

        StaticTextItem destinationsCountItem = new StaticTextItem("destinationsCount", MSG
            .view_bundle_list_destinationsCount());
        destinationsCountItem.setValue(bundle.getDestinations() != null ? bundle.getDestinations().size() : 0);

        StaticTextItem descriptionItem = new StaticTextItem("description", MSG.common_title_description());
        descriptionItem.setValue(bundle.getDescription());

        form.setFields(versionCountItem, actionItem, destinationsCountItem, descriptionItem);

        return form;
    }

    private Canvas getActionCanvas() {
        VLayout layout = new LocatableVLayout(form.extendLocatorId("Actions"), 10);

        IButton deployButton = new LocatableIButton(form.extendLocatorId("Deploy"), MSG.view_bundle_deploy());
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
                        CoreGUI.getErrorHandler().handleError(MSG.view_bundle_list_error1(bundle.getName()), caught);
                    }

                    public void onSuccess(PageList<Bundle> result) {
                        if (result == null || result.size() != 1) {
                            CoreGUI.getMessageCenter().notify(
                                new Message(MSG.view_bundle_list_error2(bundle.getName()), Message.Severity.Error));
                            return;
                        }
                        new BundleDeployWizard(result.get(0).getId()).startWizard();
                    }
                });
            }
        });
        layout.addMember(deployButton);

        IButton deleteButton = new LocatableIButton(form.extendLocatorId("Delete"), MSG.common_button_delete());
        deleteButton.setIcon("subsystems/bundle/BundleAction_Delete_16.png");
        deleteButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                SC.ask(MSG.view_bundle_list_deleteConfirm(), new BooleanCallback() {
                    public void execute(Boolean aBoolean) {
                        if (aBoolean) {
                            bundleManager.deleteBundle(bundleBeingViewed, new AsyncCallback<Void>() {
                                public void onFailure(Throwable caught) {
                                    CoreGUI.getErrorHandler().handleError(
                                        MSG.view_bundle_list_deleteFailure(bundle.getName()), caught);
                                }

                                public void onSuccess(Void result) {
                                    CoreGUI.getMessageCenter().notify(
                                        new Message(MSG.view_bundle_list_deleteSuccessful(bundle.getName()),
                                            Message.Severity.Info));
                                    History.newItem("Bundles"); // Bundle is deleted, go back to all bundles view
                                }
                            });
                        }
                    }
                });
            }
        });
        layout.addMember(deleteButton);

        if (!canManageBundles) {
            deployButton.setDisabled(true);
            deleteButton.setDisabled(true);
        }

        return layout;
    }

    public void renderView(final ViewPath viewPath) {
        int bundleId = Integer.parseInt(viewPath.getCurrent().getPath());

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
                            CoreGUI.getErrorHandler().handleError(MSG.view_bundle_list_error3(), caught);
                        }

                        public void onSuccess(PageList<Bundle> result) {
                            Bundle bundle = result.get(0);
                            viewBundle(bundle, viewPath.getCurrent());
                        }
                    });
            } else if (!viewPath.isEnd()) {
                String current = viewPath.getCurrent().getPath();
                if ("versions".equals(current)) {
                    tabs.selectTab(versionsTab);
                } else if ("destinations".equals(current)) {
                    tabs.selectTab(destinationsTab);
                }
                viewBundle(bundle, viewPath.getCurrent());
            }
        } else {
            // Although still relevant the bundle is no longer being viewed. Set to 0 for re-fetch if needed
            // also, destroy the current layout to make way for the new summary
            bundleBeingViewed = 0;
            this.destroyMembers();

            if (viewPath.getCurrent().getPath().equals("versions")) {
                if (!viewPath.isEnd()) {
                    // a specific version
                    BundleVersionView view = new BundleVersionView(extendLocatorId("Version"), canManageBundles);
                    addMember(view);
                    view.renderView(viewPath.next());
                }
            } else if (viewPath.getCurrent().getPath().equals("deployments")) {
                if (viewPath.isEnd()) {
                    // TODO: where to go? I don't think this is a valid url right now
                } else {
                    // a specific deployment
                    //removeMembers(getMembers());
                    BundleDeploymentView view = new BundleDeploymentView(extendLocatorId("Deployment"),
                        canManageBundles);
                    addMember(view);
                    view.renderView(viewPath.next());
                }
            } else if (viewPath.getCurrent().getPath().equals("destinations")) {
                if (!viewPath.isEnd()) {
                    // a specific destination
                    BundleDestinationView view = new BundleDestinationView(extendLocatorId("Destination"),
                        canManageBundles);
                    addMember(view);
                    view.renderView(viewPath.next());
                }
            }
        }
    }
}
