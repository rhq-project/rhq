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

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tile.TileLayout;

import org.rhq.core.domain.tagging.Tag;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.bundle.list.BundlesListView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSearchView;

/**
 * @author Greg Hinkle
 */
public class TaggedView extends VLayout implements BookmarkableView {

    private TagCloudView tagCloudView;

    private Criteria criteria;

    private ResourceSearchView resourceView;

    private BundlesListView bundlesView;

    private TileLayout tileLayout;

    public TaggedView() {
        setWidth100();
        setHeight100();
    }

    @Override
    protected void onInit() {
        super.onInit();

        tagCloudView = new TagCloudView();
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

        criteria = new Criteria();
        criteria.addCriteria("tagNamespace", tag.getNamespace());
        criteria.addCriteria("tagSemantic", tag.getSemantic());
        criteria.addCriteria("tagName", tag.getName());


        if (tileLayout == null) {

            tileLayout = new TileLayout();
            tileLayout.setWidth100();
            tileLayout.setTileHeight(250);
            tileLayout.setTileWidth(getWidth() / 2 - 20);
            addMember(tileLayout);

            tileLayout.addTile(getResourceCanvas(criteria));
            tileLayout.addTile(getBundleCanvas(criteria));

        }

        resourceView.refresh(criteria);
        bundlesView.refresh(criteria);
    }


    private Canvas getResourceCanvas(Criteria criteria) {
        resourceView = new ResourceSearchView(criteria, "Tagged Resources", new SortSpecifier[] {},new String[]{"pluginName", "category", "currentAvailability"});
        resourceView.setShowFooter(false);
        return resourceView;
    }


    private Canvas getBundleCanvas(Criteria criteria) {
        bundlesView = new BundlesListView(criteria);
        bundlesView.setShowFooter(false);
        return bundlesView;
    }


    public void renderView(ViewPath viewPath) {
        if (!viewPath.isEnd()) {
            String tagString = viewPath.getCurrent().getPath();
            viewTag(tagString);
        }
    }
}
