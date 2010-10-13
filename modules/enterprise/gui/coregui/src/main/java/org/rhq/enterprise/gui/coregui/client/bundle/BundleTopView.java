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

import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.bundle.list.BundleView;
import org.rhq.enterprise.gui.coregui.client.bundle.list.BundlesListView;
import org.rhq.enterprise.gui.coregui.client.bundle.tree.BundleTreeView;
import org.rhq.enterprise.gui.coregui.client.content.repository.tree.ContentRepositoryTreeView;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableSectionStack;

/**
 * @author Greg Hinkle
 */
public class BundleTopView extends LocatableHLayout implements BookmarkableView {
    public static final String VIEW_ID = "Bundles";

    private BundleTreeView bundleTreeView;

    private VLayout contentCanvas;

    private ViewId currentNextPath;
    private BundleView bundleView;
    private BundlesListView bundlesListView;

    public BundleTopView(String locatorId) {
        super(locatorId);
        setOverflow(Overflow.AUTO);
        setWidth100();
        setHeight100();
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        SectionStack sectionStack = new LocatableSectionStack(getLocatorId());
        sectionStack.setShowResizeBar(true);
        sectionStack.setVisibilityMode(VisibilityMode.MULTIPLE);
        sectionStack.setWidth(250);
        sectionStack.setHeight100();

        SectionStackSection bundlesSection = new SectionStackSection("Bundles");
        bundleTreeView = new BundleTreeView(extendLocatorId("BundleTree"));
        bundlesSection.addItem(bundleTreeView);
        sectionStack.addSection(bundlesSection);

        SectionStackSection repositoriesSection = new SectionStackSection("Repositories");
        ContentRepositoryTreeView repoTree = new ContentRepositoryTreeView(extendLocatorId("RepoTree"));
        repositoriesSection.addItem(repoTree);
        sectionStack.addSection(repositoriesSection);

        SectionStackSection providersSection = new SectionStackSection("Providers");
        sectionStack.addSection(providersSection);

        addMember(sectionStack);

        contentCanvas = new VLayout();
        contentCanvas.setWidth100();
        contentCanvas.setHeight100();
        addMember(contentCanvas);
    }

    public void setContent(Canvas newContent) {
        for (Canvas c : contentCanvas.getMembers()) {
            c.destroy();
        }
        contentCanvas.addMember(newContent);
        contentCanvas.markForRedraw();
    }

    public void renderView(ViewPath viewPath) {

        if (viewPath.isRefresh()) {
            bundleTreeView.refresh();
        }

        bundleTreeView.selectPath(viewPath);

        if (viewPath.isEnd()) {
            if (currentNextPath == null && bundlesListView != null) {
                // refresh
                com.allen_sauer.gwt.log.client.Log.info("Refreshing BundleTopView");

                bundlesListView.refresh();
            } else {
                currentNextPath = null;
                this.bundlesListView = new BundlesListView(extendLocatorId("BundleList"));
                setContent(this.bundlesListView);
            }
        } else {
            viewPath.getCurrent().getBreadcrumbs().clear();
            if (!viewPath.getNext().equals(currentNextPath)) {
                currentNextPath = viewPath.getNext();
                bundleView = new BundleView(extendLocatorId("Bundle"));
                setContent(bundleView);
                bundleView.renderView(viewPath.next());
            } else {
                bundleView.renderView(viewPath.next());
            }
        }
    }
}
