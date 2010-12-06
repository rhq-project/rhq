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
package org.rhq.enterprise.gui.coregui.client.components.tagging;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.TextMatchStyle;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.MouseOutEvent;
import com.smartgwt.client.widgets.events.MouseOutHandler;
import com.smartgwt.client.widgets.events.MouseOverEvent;
import com.smartgwt.client.widgets.events.MouseOverHandler;
import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.form.fields.events.KeyPressEvent;
import com.smartgwt.client.widgets.form.fields.events.KeyPressHandler;
import com.smartgwt.client.widgets.layout.Layout;
import com.smartgwt.client.widgets.layout.LayoutSpacer;

import org.rhq.core.domain.criteria.TagCriteria;
import org.rhq.core.domain.tagging.Tag;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.selenium.Locatable;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDialog;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableImg;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 */
public class TagEditorView extends LocatableLayout {

    private LinkedHashSet<Tag> tags = new LinkedHashSet<Tag>();

    private boolean editing = false;
    private boolean readOnly;
    private TagsChangedCallback callback;

    private boolean vertical = false;
    private boolean alwaysEdit = false;

    public TagEditorView(String locatorId, Set<Tag> tags, boolean readOnly, TagsChangedCallback callback) {
        super(locatorId);

        if (tags != null) {
            this.tags.addAll(tags);
        }
        this.readOnly = readOnly;
        this.callback = callback;
    }

    public LinkedHashSet<Tag> getTags() {
        return tags;
    }

    public void setTags(LinkedHashSet<Tag> tags) {
        this.tags = tags;
        setup();
    }

    public void setVertical(boolean vertical) {
        this.vertical = vertical;
    }

    public void setAlwaysEdit(boolean alwaysEdit) {
        this.alwaysEdit = alwaysEdit;
        this.editing = true;
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        setup();
    }

    private void setup() {
        for (Canvas child : getMembers()) {
            child.destroy();
        }

        Layout layout = vertical ? new LocatableVLayout(getLocatorId()) : new LocatableHLayout(getLocatorId());
        if (!vertical)
            layout.setMembersMargin(8);

        HTMLFlow title = new HTMLFlow("<b>" + MSG.view_tags_tags() + ":</b>");
        title.setAutoWidth();
        layout.addMember(title);

        for (final Tag tag : tags) {
            LocatableHLayout tagLayout = new LocatableHLayout(((Locatable) layout).extendLocatorId(tag.getName()));
            tagLayout.setHeight(18);
            //tagLayout.set

            HTMLFlow tagString = new HTMLFlow("<a href=\"" + LinkManager.getTagLink(tag.toString()) + "\">"
                + tag.toString() + "</a>");
            tagString.setAutoWidth();

            tagLayout.addMember(tagString);

            if (!readOnly) {
                final LayoutSpacer spacer = new LayoutSpacer();
                spacer.setHeight(16);
                spacer.setWidth(16);

                final Img remove = new LocatableImg(tagLayout.extendLocatorId("Remove"),
                    "[skin]/images/actions/remove.png", 16, 16);
                remove.setTooltip(MSG.view_tags_tooltip_1());
                remove.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent clickEvent) {
                        tags.remove(tag);
                        save();
                    }
                });
                tagLayout.addMember(remove);
                tagLayout.addMember(spacer);
                remove.hide();

                tagLayout.addMouseOverHandler(new MouseOverHandler() {
                    public void onMouseOver(MouseOverEvent mouseOverEvent) {
                        remove.show();
                        spacer.hide();
                    }
                });
                tagLayout.addMouseOutHandler(new MouseOutHandler() {
                    public void onMouseOut(MouseOutEvent mouseOutEvent) {
                        spacer.show();
                        remove.hide();
                    }
                });
            }

            tagLayout.setHeight(16);
            layout.addMember(tagLayout);
        }

        if (!readOnly) {
            final Img modeImg = new LocatableImg(((Locatable) layout).getLocatorId(), "[skin]/images/actions/add.png",
                16, 16);

            modeImg.setTooltip(MSG.view_tags_tooltip_2());
            modeImg.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent clickEvent) {

                    showTagInput(modeImg.getAbsoluteLeft(), modeImg.getAbsoluteTop());
                }
            });
            layout.addMember(modeImg);
        }

        layout.setAutoWidth();
        addMember(layout);

        markForRedraw();
    }

    private void showTagInput(int left, int top) {
        final LocatableDialog dialog = new LocatableDialog(getLocatorId());
        final LocatableDynamicForm form = new LocatableDynamicForm(dialog.getLocatorId());
        final ComboBoxItem tagInput = new ComboBoxItem("tag");

        tagInput.setShowTitle(false);
        tagInput.setHideEmptyPickList(true);
        //            tagInput.setOptionDataSource(new TaggingDataSource());
        TagCriteria criteria = new TagCriteria();
        criteria.addSortNamespace(PageOrdering.ASC);
        criteria.addSortSemantic(PageOrdering.ASC);
        criteria.addSortName(PageOrdering.ASC);
        GWTServiceLookup.getTagService().findTagsByCriteria(criteria, new AsyncCallback<PageList<Tag>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_tags_error_1(), caught);
            }

            public void onSuccess(PageList<Tag> result) {
                String[] values = new String[result.size()];
                int i = 0;
                for (Tag tag : result) {
                    values[i++] = tag.toString();
                }
                tagInput.setValueMap(values);
            }
        });

        tagInput.setValueField("tag");
        tagInput.setDisplayField("tag");
        tagInput.setType("comboBox");
        tagInput.setTextMatchStyle(TextMatchStyle.SUBSTRING);
        tagInput.setTooltip(MSG.view_tags_tooltip_3());
        /*tagInput.addBlurHandler(new BlurHandler() {
            public void onBlur(BlurEvent blurEvent) {
                String tag = form.getValueAsString("tag");
                if (tag != null) {
                    Tag newTag = new Tag(tag);
                    tags.add(newTag);
                    save();
        //                        TagEditorView.this.setup();
                }
            }
        });*/
        tagInput.addKeyPressHandler(new KeyPressHandler() {
            public void onKeyPress(KeyPressEvent event) {
                if ((event.getCharacterValue() != null) && (event.getCharacterValue() == KeyCodes.KEY_ENTER)) {
                    String tag = form.getValueAsString("tag");
                    if (tag != null) {
                        Tag newTag = new Tag(tag);
                        tags.add(newTag);
                        save();
                        dialog.destroy();
                        //                            TagEditorView.this.setup();
                    }
                }
            }
        });

        form.setFields(tagInput);

        dialog.setIsModal(true);
        dialog.setShowHeader(false);
        dialog.setShowEdges(false);
        dialog.setEdgeSize(10);
        dialog.setWidth(200);
        dialog.setHeight(30);

        dialog.setShowToolbar(false);

        Map bodyDefaults = new HashMap();
        bodyDefaults.put("layoutLeftMargin", 5);
        bodyDefaults.put("membersMargin", 10);
        dialog.setBodyDefaults(bodyDefaults);

        dialog.addItem(form);

        dialog.setDismissOnEscape(true);
        dialog.setDismissOnOutsideClick(true);

        dialog.show();
        dialog.moveTo(left - 8, top - 4);
        tagInput.focusInItem();
    }

    private void save() {
        this.callback.tagsChanged(tags);
        TagEditorView.this.setup();
    }
}
