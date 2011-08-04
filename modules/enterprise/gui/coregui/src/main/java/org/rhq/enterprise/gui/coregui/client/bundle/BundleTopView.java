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
package org.rhq.enterprise.gui.coregui.client.bundle;

import java.util.Set;

import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.authz.Permission;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoadedListener;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoader;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.bundle.list.BundleView;
import org.rhq.enterprise.gui.coregui.client.bundle.list.BundlesListView;
import org.rhq.enterprise.gui.coregui.client.bundle.tree.BundleTreeView;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.content.repository.tree.ContentRepositoryTreeView;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableSectionStack;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * This is the main bundle view with left hand side trees and right hand side list/details view.
 * 
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class BundleTopView extends LocatableHLayout implements BookmarkableView {

    public static final ViewName VIEW_ID = new ViewName("Bundles", MSG.view_bundle_bundles());

    private ViewId currentNextPath;

    private BundleTreeView bundleTreeView; // the tree of bundle destinations and versions 
    private VLayout contentCanvas; // the right-side canvas container
    private BundleView bundleView; // if the user is viewing an individual bundle, this is that right-side view
    private BundlesListView bundlesListView; // if the user is not viewing an indiv. bundle, this is the right-side list

    public BundleTopView(String locatorId) {
        super(locatorId);
        setOverflow(Overflow.AUTO);
        setWidth100();
        setHeight100();
    }

    public void renderView(final ViewPath viewPath) {
        new PermissionsLoader().loadExplicitGlobalPermissions(new PermissionsLoadedListener() {
            @Override
            public void onPermissionsLoaded(Set<Permission> permissions) {
                // if we haven't done it yet, build the view components
                if (bundleTreeView == null) {
                    boolean canManageInventory = permissions != null
                        && permissions.contains(Permission.MANAGE_INVENTORY);
                    boolean canManageBundles = permissions != null && permissions.contains(Permission.MANAGE_BUNDLE);

                    SectionStack sectionStack = new LocatableSectionStack(getLocatorId());
                    sectionStack.setShowResizeBar(true);
                    sectionStack.setVisibilityMode(VisibilityMode.MULTIPLE);
                    sectionStack.setWidth(250);
                    sectionStack.setHeight100();

                    SectionStackSection bundlesSection = new SectionStackSection(MSG.view_bundle_bundles());
                    bundleTreeView = new BundleTreeView(extendLocatorId("BundleTree"), canManageBundles);
                    bundlesSection.addItem(bundleTreeView);
                    sectionStack.addSection(bundlesSection);

                    // we only show repositories if the user has the global manage_inventory perms since that is required
                    if (canManageInventory) {
                        SectionStackSection repositoriesSection = new SectionStackSection(MSG
                            .common_title_repositories());
                        ContentRepositoryTreeView repoTree = new ContentRepositoryTreeView(extendLocatorId("RepoTree"));
                        repositoriesSection.addItem(repoTree);
                        sectionStack.addSection(repositoriesSection);
                    }

                    // TODO: we aren't doing anything with providers yet
                    // SectionStackSection providersSection = new SectionStackSection(MSG.common_title_providers());
                    // sectionStack.addSection(providersSection);

                    addMember(sectionStack);

                    contentCanvas = new VLayout();
                    contentCanvas.setWidth100();
                    contentCanvas.setHeight100();
                    addMember(contentCanvas);
                }

                if (viewPath.isRefresh()) {
                    bundleTreeView.refresh();
                }

                bundleTreeView.selectPath(viewPath);

                if (viewPath.isEnd()) {
                    if (currentNextPath == null && bundlesListView != null) {
                        bundlesListView.refresh();
                    } else {
                        currentNextPath = null;
                        bundlesListView = new BundlesListView(extendLocatorId("BundleList"), permissions);
                        setContent(bundlesListView);
                    }
                } else {
                    if (!viewPath.getNext().equals(currentNextPath)) {
                        currentNextPath = viewPath.getNext();
                        bundleView = new BundleView(extendLocatorId("Bundle"), permissions);
                        setContent(bundleView);
                        bundleView.renderView(viewPath.next());
                    } else {
                        bundleView.renderView(viewPath.next());
                    }
                }
            }
        });
    }

    private void setContent(Canvas newContent) {
        SeleniumUtility.destroyMembers(contentCanvas);

        contentCanvas.addMember(newContent);
        contentCanvas.markForRedraw();
    }

}
