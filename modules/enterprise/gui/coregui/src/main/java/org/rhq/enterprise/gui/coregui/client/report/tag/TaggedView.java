/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.gui.coregui.client.report.tag;

import java.util.ArrayList;

import com.smartgwt.client.data.Criteria;

import org.rhq.core.domain.tagging.Tag;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.bundle.deployment.BundleDeploymentListView;
import org.rhq.enterprise.gui.coregui.client.bundle.destination.BundleDestinationListView;
import org.rhq.enterprise.gui.coregui.client.bundle.list.BundlesListView;
import org.rhq.enterprise.gui.coregui.client.bundle.version.BundleVersionListView;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSearchView;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTileLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 */
public class TaggedView extends LocatableVLayout implements BookmarkableView {

    public static final ViewName VIEW_ID = new ViewName("Tag", MSG.view_tags_tags());

    private TagCloudView tagCloudView;

    private ArrayList<Table> tiles = new ArrayList<Table>();

    private LocatableTileLayout tileLayout;

    public TaggedView(String locatorId) {
        super(locatorId);
        setWidth100();
        setHeight100();
    }

    @Override
    protected void onInit() {
        super.onInit();

        tagCloudView = new TagCloudView(extendLocatorId("TagCloud"));
        tagCloudView.setAutoHeight();
        addMember(tagCloudView);
    }

    @Override
    protected void onDraw() {
        super.onDraw();
    }

    private void viewTag(String tagString) {

        tagCloudView.setSelectedTag(tagString);

        Tag tag = new Tag(tagString);

        Criteria criteria = new Criteria();
        criteria.addCriteria("tagNamespace", tag.getNamespace());
        criteria.addCriteria("tagSemantic", tag.getSemantic());
        criteria.addCriteria("tagName", tag.getName());

        if (tileLayout == null) {

            tileLayout = new LocatableTileLayout(getLocatorId());
            tileLayout.setWidth100();
            tileLayout.setTileHeight(220);
            tileLayout.setTileWidth(getWidth() / 2 - 20);
            addMember(tileLayout);

            BundlesListView bundlesView = new BundlesListView(getLocatorId(), criteria);
            tiles.add(bundlesView);

            BundleVersionListView bundleVersionListView = new BundleVersionListView(getLocatorId(), criteria);
            tiles.add(bundleVersionListView);

            BundleDeploymentListView bundleDeploymentListView = new BundleDeploymentListView(getLocatorId(), criteria);
            tiles.add(bundleDeploymentListView);

            BundleDestinationListView bundleDestinationListView = new BundleDestinationListView(getLocatorId(),
                criteria);
            tiles.add(bundleDestinationListView);

            ResourceSearchView resourceView = new ResourceSearchView(getLocatorId(), criteria, MSG
                .view_taggedResources_title());
            tiles.add(resourceView);

            for (Table t : tiles) {
                t.setShowFooter(false);
                tileLayout.addTile(t);
            }
        }

        for (Table t : tiles) {
            t.refresh(criteria);
        }
    }

    public void renderView(ViewPath viewPath) {
        if (!viewPath.isEnd()) {
            String tagString = viewPath.getCurrent().getPath();
            viewTag(tagString);
        }
    }

}
