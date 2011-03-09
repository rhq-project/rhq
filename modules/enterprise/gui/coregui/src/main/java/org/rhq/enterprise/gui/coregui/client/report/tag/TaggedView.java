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
import com.smartgwt.client.widgets.tab.Tab;

import org.rhq.core.domain.tagging.Tag;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.bundle.deployment.BundleDeploymentListView;
import org.rhq.enterprise.gui.coregui.client.bundle.destination.BundleDestinationListView;
import org.rhq.enterprise.gui.coregui.client.bundle.list.BundlesListView;
import org.rhq.enterprise.gui.coregui.client.bundle.version.BundleVersionListView;
import org.rhq.enterprise.gui.coregui.client.components.TitleBar;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSearchView;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTab;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTabSet;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 */
public class TaggedView extends LocatableVLayout implements BookmarkableView {

    public static final ViewName VIEW_ID = new ViewName("Tags", MSG.view_tags_tags());

    private TagCloudView tagCloudView;
    private LocatableTabSet container;
    private ArrayList<Table> viewsWithTags = new ArrayList<Table>();

    public TaggedView(String locatorId) {
        super(locatorId);
        setWidth100();
        setHeight100();
    }

    @Override
    protected void onInit() {
        super.onInit();

        TitleBar titleBar = new TitleBar(this, TaggedView.VIEW_ID.getTitle(), "global/Tag_24.png");
        titleBar.setExtraSpace(10);
        addMember(titleBar);

        tagCloudView = new TagCloudView(extendLocatorId("TagCloud"));
        tagCloudView.setAutoHeight();
        tagCloudView.setExtraSpace(10);
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

        if (container == null) {

            container = new LocatableTabSet(extendLocatorId("tags"));
            container.setWidth100();
            container.setHeight100();
            addMember(container);

            Tab tab;

            BundlesListView bundlesView = new BundlesListView(getLocatorId(), criteria, null);
            viewsWithTags.add(bundlesView);
            tab = new LocatableTab(extendLocatorId("bundleList"), MSG.view_bundle_bundles());
            tab.setPane(bundlesView);
            container.addTab(tab);

            BundleVersionListView bundleVersionListView = new BundleVersionListView(getLocatorId(), criteria);
            viewsWithTags.add(bundleVersionListView);
            tab = new LocatableTab(extendLocatorId("bundleVersionList"), MSG.view_bundle_bundleVersions());
            tab.setPane(bundleVersionListView);
            container.addTab(tab);

            // TODO: get manage_bundle perm, if user has it pass true
            BundleDeploymentListView bundleDeploymentListView = new BundleDeploymentListView(getLocatorId(), criteria,
                false);
            viewsWithTags.add(bundleDeploymentListView);
            tab = new LocatableTab(extendLocatorId("bundleDeploymentsList"), MSG.view_bundle_bundleDeployments());
            tab.setPane(bundleDeploymentListView);
            container.addTab(tab);

            BundleDestinationListView bundleDestinationListView = new BundleDestinationListView(getLocatorId(),
                criteria);
            viewsWithTags.add(bundleDestinationListView);
            tab = new LocatableTab(extendLocatorId("bundleDestinationsList"), MSG.view_bundle_bundleDestinations());
            tab.setPane(bundleDestinationListView);
            container.addTab(tab);

            ResourceSearchView resourceView = new ResourceSearchView(getLocatorId(), criteria, MSG
                .view_taggedResources_title());
            viewsWithTags.add(resourceView);
            tab = new LocatableTab(extendLocatorId("resourceList"), MSG.view_taggedResources_title());
            tab.setPane(resourceView);
            container.addTab(tab);

            for (Table t : viewsWithTags) {
                t.setShowFooter(false);
            }
        }

        for (Table t : viewsWithTags) {
            t.refresh();
        }
    }

    public void renderView(ViewPath viewPath) {
        if (!viewPath.isEnd()) {
            String tagString = viewPath.getCurrent().getPath();
            viewTag(tagString);
        }
    }
}
