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
import org.rhq.enterprise.gui.coregui.client.IconEnum;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoadedListener;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoader;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.bundle.group.BundleGroupEditView;
import org.rhq.enterprise.gui.coregui.client.bundle.list.BundleView;
import org.rhq.enterprise.gui.coregui.client.bundle.tree.BundleTreeView;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedHLayout;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedUtility;

/**
 * This is the main bundle view with left hand side trees and right hand side list/details view.
 * 
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class BundleTopView extends EnhancedHLayout implements BookmarkableView {

    public static final ViewName VIEW_ID = new ViewName("Bundles", MSG.common_title_bundles(), IconEnum.BUNDLE);

    private BundleTreeView bundleTreeView; // the tree of bundle destinations and versions 
    private VLayout contentCanvas; // the right-side canvas container
    private BundleSectionView bundleSectionView; // if the user is not viewing bundle or bundle group detail, this is the RHS view
    private BundleView bundleView; // if the user is viewing bundle detail, this is the RHS view
    private BundleGroupEditView bundleGroupView; // if the user is viewing bundle group detail, this is the RHS view
    private ViewId currentBundleViewId;
    private ViewId currentBundleGroupViewId;

    public BundleTopView() {
        super();
        setOverflow(Overflow.AUTO);
        setWidth100();
        setHeight100();
    }

    public void renderView(final ViewPath viewPath) {
        new PermissionsLoader().loadExplicitGlobalPermissions(new PermissionsLoadedListener() {
            @Override
            public void onPermissionsLoaded(Set<Permission> globalPermissions) {
                // if we haven't done it yet, build the view components
                if (bundleTreeView == null) {

                    SectionStack sectionStack = new SectionStack();
                    sectionStack.setShowResizeBar(true);
                    sectionStack.setVisibilityMode(VisibilityMode.MULTIPLE);
                    sectionStack.setWidth(250);
                    sectionStack.setHeight100();

                    SectionStackSection bundlesSection = new SectionStackSection(MSG.common_title_bundles());
                    bundleTreeView = new BundleTreeView();
                    bundlesSection.addItem(bundleTreeView);
                    sectionStack.addSection(bundlesSection);

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

                if (viewPath.isEnd() || viewPath.isNextEnd()) {
                    // We are navigating to the section view (ignore any trailing segment without an ID) 
                    if (bundleView == null && bundleGroupView == null && bundleSectionView != null) {
                        bundleSectionView.refresh();
                    } else {
                        bundleSectionView = new BundleSectionView(globalPermissions);
                        if (!viewPath.isEnd()) {
                            if (viewPath.getCurrent().getPath().equals("BundleGroup")) {
                                bundleSectionView.setExpansion(false, true);
                            } else {
                                bundleSectionView.setExpansion(true, false);
                            }
                        }
                        setContent(bundleSectionView);
                    }
                } else {
                    // we are navigating to bundle detail or bundle group detail
                    String currentPath = viewPath.getCurrent().getPath();
                    ViewPath nextViewPath = viewPath.next(); // the ID segment

                    if ("Bundle".equals(currentPath)) {
                        // set new bundle detail if we are changing detail
                        if (!nextViewPath.getCurrent().equals(currentBundleViewId)) {
                            // only  cache the bundle id if bundle detail is the target view
                            currentBundleViewId = nextViewPath.isEnd() ? nextViewPath.getCurrent() : null;
                            bundleView = new BundleView(globalPermissions);
                            bundleGroupView = null;
                            currentBundleGroupViewId = null;
                        }
                        setContent(bundleView);
                        bundleView.renderView(nextViewPath);

                    } else if ("BundleGroup".equals(currentPath)) {
                        // set new bundle detail if we are changing detail
                        if (!nextViewPath.getCurrent().equals(currentBundleGroupViewId)) {
                            // only  cache the bundle id if bundle detail is the target view                            
                            currentBundleGroupViewId = nextViewPath.isEnd() ? nextViewPath.getCurrent() : null;
                            bundleGroupView = new BundleGroupEditView(globalPermissions, Integer.parseInt(nextViewPath
                                .getCurrent().getPath()));
                            bundleView = null;
                            currentBundleViewId = null;
                        }
                        setContent(bundleGroupView);
                        bundleGroupView.renderView(nextViewPath);
                    }
                }
            }
        });
    }

    private void setContent(Canvas newContent) {
        EnhancedUtility.destroyMembers(contentCanvas);

        contentCanvas.addMember(newContent);
        contentCanvas.markForRedraw();
    }

}
