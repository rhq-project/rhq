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

import java.util.HashSet;
import java.util.Set;

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
import org.rhq.coregui.client.BookmarkableView;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.IconEnum;
import org.rhq.coregui.client.PermissionsLoadedListener;
import org.rhq.coregui.client.PermissionsLoader;
import org.rhq.coregui.client.ViewId;
import org.rhq.coregui.client.ViewPath;
import org.rhq.coregui.client.bundle.BundleTopView;
import org.rhq.coregui.client.bundle.deploy.BundleDeployWizard;
import org.rhq.coregui.client.bundle.deployment.BundleDeploymentView;
import org.rhq.coregui.client.bundle.destination.BundleDestinationListView;
import org.rhq.coregui.client.bundle.destination.BundleDestinationView;
import org.rhq.coregui.client.bundle.group.BundleGroupsListView;
import org.rhq.coregui.client.bundle.version.BundleVersionListView;
import org.rhq.coregui.client.bundle.version.BundleVersionView;
import org.rhq.coregui.client.components.HeaderLabel;
import org.rhq.coregui.client.components.buttons.BackButton;
import org.rhq.coregui.client.components.table.Table;
import org.rhq.coregui.client.components.tagging.TagEditorView;
import org.rhq.coregui.client.components.tagging.TagsChangedCallback;
import org.rhq.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.StringUtility;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.message.Message;

public class BundleView extends EnhancedVLayout implements BookmarkableView {
    private DynamicForm form;

    private int bundleBeingViewed = 0;
    private HeaderLabel headerLabel;
    private Table bundleVersionsTable;
    private TabSet tabs;
    private Tab versionsTab;
    private Tab destinationsTab;
    private Tab bundleGroupsTab;

    private Set<Permission> globalPermissions;
    private int permissionCheckBundleId = 0;
    private boolean canDelete;
    private boolean canDeploy;
    private boolean canTag;

    private BundleGWTServiceAsync bundleManager = GWTServiceLookup.getBundleService();
    private Bundle bundle;

    public BundleView(Set<Permission> perms) {
        super();
        this.globalPermissions = perms;
        setWidth100();
        setHeight100();
        setMargin(10);
        setOverflow(Overflow.AUTO);
    }

    private void viewBundle(final Bundle bundle, final ViewId nextViewId) {
        // Whenever a new view request comes in, make sure to clean house to avoid ID conflicts for sub-widgets
        this.destroyMembers();

        this.bundle = bundle;

        BackButton backButton = new BackButton(MSG.view_bundle_list_backToAll(), BundleTopView.VIEW_ID.getName());
        headerLabel = new HeaderLabel(IconEnum.BUNDLE.getIcon24x24Path(), StringUtility.escapeHtml(bundle.getName()));
        tabs = new TabSet();
        versionsTab = createVersionsTab();
        destinationsTab = createDestinationsTab();
        bundleGroupsTab = createBundleGroupsTab();
        tabs.addTab(versionsTab);
        tabs.addTab(destinationsTab);
        tabs.addTab(bundleGroupsTab);

        addMember(backButton);
        addMember(headerLabel);

        //conditionally add tags. Defaults to true, not available in JON builds.
        if (CoreGUI.isTagsEnabledForUI()) {
            addMember(createTagEditor());
        }
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
        boolean readOnly = !canTag;
        TagEditorView tagEditor = new TagEditorView(bundle.getTags(), readOnly, new TagsChangedCallback() {
            public void tagsChanged(HashSet<Tag> tags) {
                GWTServiceLookup.getTagService().updateBundleTags(bundleBeingViewed, tags, new AsyncCallback<Void>() {
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
        Tab destinationsTab = new Tab(MSG.view_bundle_destinations());
        Criteria criteria = new Criteria();
        criteria.addCriteria("bundleId", bundle.getId());
        destinationsTab.setPane(new BundleDestinationListView(criteria));
        return destinationsTab;
    }

    private Tab createVersionsTab() {
        Tab versionsTab = new Tab(MSG.view_bundle_versions());
        Criteria criteria = new Criteria();
        criteria.addCriteria("bundleId", bundleBeingViewed);
        bundleVersionsTable = new BundleVersionListView(criteria);
        versionsTab.setPane(bundleVersionsTable);
        return versionsTab;
    }

    private Tab createBundleGroupsTab() {
        Tab bundleGroupsTab = new Tab(MSG.common_title_bundleGroups());
        Criteria criteria = new Criteria();
        criteria.addCriteria("bundleIds", new Integer[] { bundle.getId() });
        bundleGroupsTab.setPane(new BundleGroupsListView(criteria, (Set<Permission>) null));
        return bundleGroupsTab;
    }

    private DynamicForm createSummaryForm() {

        form = new DynamicForm();
        form.setWidth100();
        form.setColWidths("20%", "40%", "40%");
        form.setNumCols(3);
        form.setAutoHeight();
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

        StaticTextItem destinationsCountItem = new StaticTextItem("destinationsCount",
            MSG.view_bundle_list_destinationsCount());
        destinationsCountItem.setValue(bundle.getDestinations() != null ? bundle.getDestinations().size() : 0);

        StaticTextItem descriptionItem = new StaticTextItem("description", MSG.common_title_description());
        descriptionItem.setValue(StringUtility.escapeHtml(bundle.getDescription()));

        form.setFields(versionCountItem, actionItem, destinationsCountItem, descriptionItem);

        return form;
    }

    private Canvas getActionCanvas() {
        VLayout layout = new EnhancedVLayout(10);

        IButton deployButton = new EnhancedIButton(MSG.view_bundle_deploy());
        deployButton.setIcon(IconEnum.BUNDLE_DEPLOY.getIcon16x16Path());
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

        IButton deleteButton = new EnhancedIButton(MSG.common_button_delete());
        deleteButton.setIcon(IconEnum.BUNDLE_DELETE.getIcon16x16Path());
        deleteButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                SC.ask(MSG.view_bundle_deleteConfirm(), new BooleanCallback() {
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
                                    CoreGUI.goToView("Bundles", true); // Bundle is deleted, go back to all bundles view
                                }
                            });
                        }
                    }
                });
            }
        });
        layout.addMember(deleteButton);

        deployButton.setDisabled(!canDeploy);
        deleteButton.setDisabled(!canDelete);

        return layout;
    }

    public void renderView(final ViewPath viewPath) {
        final int bundleId = Integer.parseInt(viewPath.getCurrent().getPath());

        // if we have already determined permissions for this bundle, just proceed
        if (permissionCheckBundleId == bundleId) {
            authorizedRenderView(bundleId, viewPath);
            return;
        }

        // check necessary global permissions
        canDelete = globalPermissions.contains(Permission.DELETE_BUNDLES);
        canDeploy = globalPermissions.contains(Permission.DEPLOY_BUNDLES);
        canTag = globalPermissions.contains(Permission.CREATE_BUNDLES);

        // If the user has global perms to enable/render any of the views then proceed, otherwise, we
        // need to see what bundle level perms he has.
        if (canDelete && canDeploy && canTag) {
            authorizedRenderView(bundleId, viewPath);

        } else {
            new PermissionsLoader().loadBundlePermissions(bundleId, new PermissionsLoadedListener() {
                @Override
                public void onPermissionsLoaded(Set<Permission> bundlePermissions) {
                    canDelete = canDelete || bundlePermissions.contains(Permission.DELETE_BUNDLES_FROM_GROUP);
                    canDeploy = canDeploy || bundlePermissions.contains(Permission.DEPLOY_BUNDLES_TO_GROUP);
                    canTag = canTag || bundlePermissions.contains(Permission.CREATE_BUNDLES_IN_GROUP);

                    authorizedRenderView(bundleId, viewPath);
                }
            });
        }
    }

    private void authorizedRenderView(final int bundleId, final ViewPath viewPath) {
        permissionCheckBundleId = bundleId;

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
                            if (result == null || result.isEmpty()) {
                                CoreGUI.getMessageCenter().notify(
                                    new Message(MSG.view_bundle_list_error4(), Message.Severity.Error));
                                return;
                            }
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
                    BundleVersionView view = new BundleVersionView(canDelete, canDeploy, canTag);
                    addMember(view);
                    view.renderView(viewPath.next());
                }
            } else if (viewPath.getCurrent().getPath().equals("deployments")) {
                if (viewPath.isEnd()) {
                    // today we do not have an uber-view showing all deployments for a bundle.
                    // if we did, it would show all deployments to all destinations for all bundle versions.
                    // because that would be a very large list with a lot of stuff to show, it was deemed
                    // too complex to be useful for users. thus, we have no uber-deployments view. If we did,
                    // we would render it here.
                } else {
                    // a specific deployment
                    //removeMembers(getMembers());
                    BundleDeploymentView view = new BundleDeploymentView(canDelete, canDeploy, canTag);
                    addMember(view);
                    view.renderView(viewPath.next());
                }
            } else if (viewPath.getCurrent().getPath().equals("destinations")) {
                if (!viewPath.isEnd()) {
                    // a specific destination
                    BundleDestinationView view = new BundleDestinationView(canDelete, canDeploy, canTag);
                    addMember(view);
                    view.renderView(viewPath.next());
                }
            }
        }
    }

}
