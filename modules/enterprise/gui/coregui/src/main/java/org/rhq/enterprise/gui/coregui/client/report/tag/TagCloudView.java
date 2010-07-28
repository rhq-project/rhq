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

import java.util.Collections;
import java.util.Comparator;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.criteria.TagCriteria;
import org.rhq.core.domain.tagging.compsite.TagReportComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.HeaderLabel;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;

/**
 * @author Greg Hinkle
 */
public class TagCloudView extends VLayout {

    private PageList<TagReportComposite> tags;

    private String selectedTag;

    private boolean simple = false;

    public TagCloudView() {
    }

    public TagCloudView(boolean simple) {
        this.simple = simple;
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        removeMembers(getMembers());

        GWTServiceLookup.getTagService().findTagReportCompositesByCriteria(new TagCriteria(),
                new AsyncCallback<PageList<TagReportComposite>>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to load tags", caught);
                    }

                    public void onSuccess(PageList<TagReportComposite> result) {
                        drawTags(result);
                    }
                });
    }


    private void drawTags(PageList<TagReportComposite> tags) {

        if (tags == null) {
            return; // Tags still loading
        }

        this.tags = tags;

        if (!simple) {
            addMember(new HeaderLabel("Tag Cloud"));
        }

        long max = 0;
        long total = 0;
        for (TagReportComposite tag : tags) {
            if (tag.getTotal() > max) {
                max = tag.getTotal();
            }
            total += tag.getTotal();
        }

        Collections.sort(tags, new Comparator<TagReportComposite>() {
            public int compare(TagReportComposite o1, TagReportComposite o2) {
                return o1.getTag().toString().compareTo(o2.getTag().toString());
            }
        });

        StringBuilder buf = new StringBuilder();

        int minFont = 8;
        int maxFont = 22;

        for (TagReportComposite tag : tags) {

            int font = (int) ((((double) tag.getTotal()) / (double) max) * (maxFont - minFont)) + minFont;

            buf.append("<a href=\"#Reports/Inventory/Tag Cloud/" + tag.getTag().toString() + "\" style=\"font-size: " + font + "pt; margin: 8px;\"");


            buf.append(" title=\"Tag used " + tag.getTotal() + " times\"");


            if (tag.getTag().toString().equals(selectedTag)) {
                buf.append(" class=\"selectedTag\"");
            }

            buf.append(">" + tag.getTag().toString() + "</a> ");
        }

        HTMLFlow flow = new HTMLFlow(buf.toString());

        addMember(flow);
    }


    public void setSelectedTag(String selectedTag) {
        this.selectedTag = selectedTag;
        removeMembers(getMembers());
        drawTags(tags);
    }

}
