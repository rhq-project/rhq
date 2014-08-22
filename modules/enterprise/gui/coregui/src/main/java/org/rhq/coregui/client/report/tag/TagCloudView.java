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
package org.rhq.coregui.client.report.tag;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;

import org.rhq.core.domain.criteria.TagCriteria;
import org.rhq.core.domain.tagging.Tag;
import org.rhq.core.domain.tagging.compsite.TagReportComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton.ButtonColor;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.message.Message.Severity;

/**
 * Shows the tags in a large HTML block, where the most used tags are shown in bigger fonts.
 * 
 * @author Greg Hinkle
 * @author John  Mazzitelli
 */
public class TagCloudView extends EnhancedVLayout {

    private static final String REMOVE_ICON = "[skin]/images/actions/remove.png";
    private static final int MIN_FONTSIZE = 8;
    private static final int MAX_FONTSIZE = 22;

    private PageList<TagReportComposite> tags;
    private String selectedTag;
    private IButton deleteButton;

    public TagCloudView() {
        super();
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        refresh();
    }

    private void refresh() {
        removeMembers(getMembers());

        TagCriteria tagCriteria = new TagCriteria();
        tagCriteria.setPageControl(PageControl.getUnlimitedInstance());

        GWTServiceLookup.getTagService().findTagReportCompositesByCriteria(tagCriteria,
            new AsyncCallback<PageList<TagReportComposite>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_tagCloud_error_fetchFailure(), caught);
                }

                public void onSuccess(PageList<TagReportComposite> result) {
                    drawTags(result);
                }
            });
    }

    private void drawTags(PageList<TagReportComposite> tags) {
        if (tags == null) {
            return; // tags are still loading
        }

        this.tags = tags;

        // determine the maximum number of times any single tag is used
        long max = 0;
        for (TagReportComposite tag : tags) {
            if (tag.getTotal() > max) {
                max = tag.getTotal();
            }
        }

        // sort the tags so they appear alphabetically
        Collections.sort(tags, new Comparator<TagReportComposite>() {
            public int compare(TagReportComposite o1, TagReportComposite o2) {
                return o1.getTag().toString().compareTo(o2.getTag().toString());
            }
        });

        // build the HTML block that contains all the tags with variable font sizes
        // where the font size represents the relative number of times the tag is used
        StringBuilder buf = new StringBuilder();
        for (TagReportComposite tag : tags) {

            int font = (int) ((((double) tag.getTotal()) / (double) max) * (MAX_FONTSIZE - MIN_FONTSIZE))
                + MIN_FONTSIZE;

            buf.append("<a href=\"").append(LinkManager.getTagLink(tag.getTag().toString())).append(
                "\" style=\"font-size: ").append(font).append("pt; margin: 8px;\"");

            buf.append(" title=\"").append(MSG.view_tagCloud_error_tagUsedCount(String.valueOf(tag.getTotal())))
                .append("\"");

            if (tag.getTag().toString().equals(selectedTag)) {
                buf.append(" class=\"selectedTag\"");
            }

            buf.append(">").append(tag.getTag()).append("</a> ");
        }
        HTMLFlow flow = new HTMLFlow(buf.toString());
        addMember(flow);
    }

    public String getSelectedTag() {
        return this.selectedTag;
    }

    /**
     * Determines which, if any, tag is currently selected.
     * 
     * @param selectedTag the full tag name, or <code>null</code> if nothing is selected
     */
    public void setSelectedTag(String selectedTag) {
        this.selectedTag = selectedTag;
        TagReportComposite selected = getSelectedTagReportComposite();
        getDeleteButton().setDisabled(selected == null || selected.getTotal() > 0); // don't be able to delete non-existent tag or tag that is used
        removeMembers(getMembers());
        drawTags(this.tags);
    }

    /**
     * This view can provide a delete button that, when clicked, will delete the currently
     * selected tag. This view doesn't put this delete button anywhere, the caller can place
     * this button where ever it deems appropriate.
     * 
     * @return a delete button component that can be placed as a member of a canvas
     */
    public IButton getDeleteButton() {
        if (this.deleteButton == null) {
            final IButton button = new DeleteButton();
            button.setIcon(REMOVE_ICON);
            button.setIconWidth(16);
            button.setIconHeight(16);
            button.setAutoFit(true);
            button.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    final TagReportComposite selected = getSelectedTagReportComposite();
                    if (selected != null) {
                        HashSet<Tag> doomedTag = new HashSet<Tag>(1);
                        doomedTag.add(selected.getTag());
                        GWTServiceLookup.getTagService().removeTags(doomedTag, new AsyncCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                tags.remove(selected);
                                CoreGUI.goToView(LinkManager.getTagLink(null));
                                CoreGUI.getMessageCenter().notify(
                                    new Message(MSG.view_tagCloud_deleteTagSuccess(selected.getTag().toString()),
                                        Severity.Info));
                            }

                            @Override
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError(
                                    MSG.view_tagCloud_deleteTagFailure(selected.getTag().toString()), caught);
                            }
                        });
                    }
                }
            });
            this.deleteButton = button;
        }
        return this.deleteButton;
    }

    private TagReportComposite getSelectedTagReportComposite() {
        if (selectedTag != null && tags != null) {
            for (TagReportComposite tag : tags) {
                if (tag.getTag().toString().equals(selectedTag)) {
                    return tag;
                }
            }
        }
        return null;
    }

    class DeleteButton extends EnhancedIButton {
        public DeleteButton() {
            super(MSG.view_tagCloud_deleteTag(), ButtonColor.RED);
        }

        @Override
        protected void onDestroy() {
            super.onDestroy();
            deleteButton = null;
        }
    }
}
