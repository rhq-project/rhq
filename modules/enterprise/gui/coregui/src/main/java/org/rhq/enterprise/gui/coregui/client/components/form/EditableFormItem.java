/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.components.form;

import com.google.gwt.event.dom.client.KeyCodes;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.events.MouseOutEvent;
import com.smartgwt.client.widgets.events.MouseOutHandler;
import com.smartgwt.client.widgets.events.MouseOverEvent;
import com.smartgwt.client.widgets.events.MouseOverHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.FormItemIfFunction;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.FormItemIcon;
import com.smartgwt.client.widgets.form.fields.events.FormItemClickHandler;
import com.smartgwt.client.widgets.form.fields.events.FormItemIconClickEvent;
import com.smartgwt.client.widgets.form.fields.events.KeyPressEvent;
import com.smartgwt.client.widgets.form.fields.events.KeyPressHandler;

import org.rhq.enterprise.gui.coregui.client.ImageManager;

/**
 * This form item can be used to display a value normally in "read-only" form (i.e. as a StaticTextItem),
 * but can be toggled into an "editable" form that allows the user to enter a different value.
 * 
 * This default implementation provides editing the value within a text field. However, this class
 * is designed to be extended, thus allowing the subclasses to edit values via checkboxes, radio buttons, etc.
 * 
 * @author John Mazzitelli
 */
public class EditableFormItem extends SimpleEditableFormItem {

    public EditableFormItem() {
        super();
    }

    public EditableFormItem(String name, String title) {
        super(name, title);
    }

    public EditableFormItem(String name, String title, ValueEditedHandler handler) {
        super(name, title, handler);
    }

    protected void addHandlers() {

        final FormItemIcon blankIcon = createBlankIcon();
        final FormItemIcon editIcon = createEditIcon();

        // if we are not in edit-mode, we want to show the edit icon, but only if the mouse hovers over us
        this.innerForm.addMouseOutHandler(new MouseOutHandler() {
            public void onMouseOut(MouseOutEvent event) {
                if (!isEditing()) {
                    EditableFormItem.this.staticItem.setIcons(blankIcon);
                    EditableFormItem.this.markForRedraw();
                }
            }
        });
        this.innerForm.addMouseOverHandler(new MouseOverHandler() {
            public void onMouseOver(MouseOverEvent event) {
                if (!isEditing() && !isReadOnly()) {
                    EditableFormItem.this.staticItem.setIcons(editIcon);
                    EditableFormItem.this.markForRedraw();
                }
            }
        });
    }

    protected FormItem prepareStaticFormItem() {
        FormItemIcon blankIcon = createBlankIcon();
        FormItem item = instantiateStaticFormItem();
        item.setShowTitle(false);
        item.setIcons(blankIcon);
        item.setIconVAlign(VerticalAlignment.CENTER);
        item.setIconHeight(16);
        item.setIconWidth(16);
        item.setShowIcons(true);
        item.setShowIfCondition(new FormItemIfFunction() {
            public boolean execute(FormItem item, Object value, DynamicForm form) {
                return !isEditing();
            }
        });
        item.setTextBoxStyle("editableText");

        return item;
    }

    protected FormItem prepareEditFormItem() {
        FormItemIcon cancelIcon = createCancelIcon();
        FormItemIcon approveIcon = createApproveIcon();
        FormItem item = instantiateEditFormItem();
        item.setShowTitle(false);
        item.setIcons(approveIcon, cancelIcon);
        item.setIconVAlign(VerticalAlignment.CENTER);
        item.setIconHeight(16);
        item.setIconWidth(16);
        item.setShowIcons(true);
        item.setShowIfCondition(new FormItemIfFunction() {
            public boolean execute(FormItem item, Object value, DynamicForm form) {
                return isEditing();
            }
        });
        item.addKeyPressHandler(new KeyPressHandler() {
            public void onKeyPress(KeyPressEvent event) {
                if ((event.getCharacterValue() != null) && (event.getCharacterValue() == KeyCodes.KEY_ENTER)) {
                    if (EditableFormItem.this.innerForm.validate(false)) {
                        Object newValue = event.getItem().getValue();
                        setApprovedNewValue(newValue);
                        switchToStaticMode();
                    }
                }
            }
        });

        return item;
    }

    protected FormItemIcon createEditIcon() {
        FormItemIcon editIcon = new FormItemIcon();
        editIcon.setSrc(ImageManager.getEditIcon());
        editIcon.setPrompt(MSG.common_button_edit()); // TODO have better message?
        editIcon.addFormItemClickHandler(new FormItemClickHandler() {
            public void onFormItemClick(FormItemIconClickEvent event) {
                // should never get here if read-only (the icon is hidden) but just to be sure, check read-only status again
                if (!isReadOnly()) {
                    switchToEditMode();
                }
            }
        });
        return editIcon;
    }

    protected FormItemIcon createApproveIcon() {
        FormItemIcon approveIcon = new FormItemIcon();
        approveIcon.setSrc(ImageManager.getApproveIcon());
        approveIcon.setPrompt(MSG.common_button_ok()); // TODO have better message?
        approveIcon.addFormItemClickHandler(new FormItemClickHandler() {
            public void onFormItemClick(FormItemIconClickEvent event) {
                if (EditableFormItem.this.innerForm.validate(false)) {
                    Object newValue = event.getItem().getValue();
                    setApprovedNewValue(newValue);
                    switchToStaticMode();
                }
            }
        });
        return approveIcon;
    }

    protected FormItemIcon createCancelIcon() {
        FormItemIcon cancelIcon = new FormItemIcon();
        cancelIcon.setSrc(ImageManager.getCancelIcon());
        cancelIcon.setPrompt(MSG.common_button_cancel()); // TODO have better message?
        cancelIcon.addFormItemClickHandler(new FormItemClickHandler() {
            public void onFormItemClick(FormItemIconClickEvent event) {
                switchToStaticMode();
            }
        });
        return cancelIcon;
    }

    protected FormItemIcon createBlankIcon() {
        FormItemIcon blankIcon = new FormItemIcon();
        blankIcon.setSrc("blank.png");
        return blankIcon;
    }

}
