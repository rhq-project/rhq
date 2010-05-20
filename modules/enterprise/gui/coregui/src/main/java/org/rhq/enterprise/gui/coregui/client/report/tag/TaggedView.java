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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.criteria.TagCriteria;
import org.rhq.core.domain.tagging.Tag;
import org.rhq.core.domain.tagging.compsite.TagReportComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.bundle.list.BundlesListView;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSearchView;

/**
 * @author Greg Hinkle
 */
public class TaggedView extends VLayout implements BookmarkableView {


    private SectionStack sectionStack;

    private Criteria criteria;

    private SectionStackSection resourceSection;
    private ResourceSearchView resourceView;

    private SectionStackSection bundleSection;
    private BundlesListView bundlesView;

    public TaggedView() {
        setWidth100();
        setHeight100();
    }

    @Override
    protected void onInit() {
        super.onInit();

        TagCloudView tagCloudView = new TagCloudView();
        tagCloudView.setAutoHeight();
        addMember(tagCloudView);

        sectionStack = new SectionStack();
        sectionStack.setHeight("*");

        sectionStack.addSection(getResourceStack());

        sectionStack.addSection(getBundleStack());

        addMember(sectionStack);

    }

    private void viewTag(String tagString) {


        Tag tag = new Tag(tagString);

        criteria = new Criteria();
        criteria.addCriteria("tagNamespace", tag.getNamespace());
        criteria.addCriteria("tagSemantic", tag.getSemantic());
        criteria.addCriteria("tagName", tag.getName());


        resourceView.setCriteria(criteria);
        bundlesView.setCriteria(criteria);


        TagCriteria criteria = new TagCriteria();
        criteria.addFilterNamespace(tag.getNamespace());
        criteria.addFilterSemantic(tag.getSemantic());
        criteria.addFilterName(tag.getName());

        GWTServiceLookup.getTagService().findTagReportCompositesByCriteria(criteria, new AsyncCallback<PageList<TagReportComposite>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to load tag report",caught);
            }

            public void onSuccess(PageList<TagReportComposite> result) {
                TagReportComposite tagReport = result.get(0);
                sectionStack.setSectionTitle(0, "Resources: " + tagReport.getResourceCount());
                sectionStack.setSectionTitle(1, "Bundles: " + tagReport.getBundleCount());
            }
        });
    }

    private SectionStackSection getResourceStack() {
        resourceSection = new SectionStackSection();
        resourceSection.setTitle("Resources");


        resourceView = new ResourceSearchView();
        resourceSection.addItem(resourceView);
        return resourceSection;
    }

    private SectionStackSection getBundleStack() {
        bundleSection = new SectionStackSection();
        bundleSection.setTitle("Bundles");

        bundlesView = new BundlesListView();
        bundleSection.addItem(bundlesView);
        return bundleSection;
    }

    public void renderView(ViewPath viewPath) {
        if (!viewPath.isEnd()) {
            String tagString = viewPath.getCurrent().getPath();
            viewTag(tagString);
        }
    }
}
