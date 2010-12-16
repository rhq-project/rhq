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

package org.rhq.enterprise.gui.coregui.client.components.form;

import java.util.Date;

import com.google.gwt.event.dom.client.KeyCodes;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.events.MouseOutEvent;
import com.smartgwt.client.widgets.events.MouseOutHandler;
import com.smartgwt.client.widgets.events.MouseOverEvent;
import com.smartgwt.client.widgets.events.MouseOverHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.FormItemIfFunction;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.FormItemIcon;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.FormItemClickHandler;
import com.smartgwt.client.widgets.form.fields.events.FormItemIconClickEvent;
import com.smartgwt.client.widgets.form.fields.events.KeyPressEvent;
import com.smartgwt.client.widgets.form.fields.events.KeyPressHandler;
import com.smartgwt.client.widgets.form.validator.Validator;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.Messages;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

/**
 * This form item can be used to display a value normally in "read-only" form (i.e. as a StaticTextItem),
 * but can be toggled into an "editable" form that allows the user to enter a different value.
 * 
 * This default implementation provides editing the value within a text field. However, this class
 * is designed to be extended, thus allowing the subclasses to edit values via
 * checkboxes, radio buttons, etc.
 * 
 * @author John Mazzitelli
 *
 */
public class EditableFormItem extends CanvasItem {

    protected static Messages MSG = CoreGUI.getMessages();

    private boolean readOnly = false;
    private boolean editing = false;
    private FormItem staticItem;
    private FormItem editItem;
    private DynamicForm innerForm;

    private ValueEditedHandler valueEditedHandler;

    public EditableFormItem() {
        this(null, null);
    }

    public EditableFormItem(String name, String title) {
        this(name, title, null);
    }

    public EditableFormItem(String name, String title, ValueEditedHandler handler) {
        if (name != null) {
            setName(name);
        }
        if (title != null) {
            setTitle(title);
        }
        if (handler != null) {
            setValueEditedHandler(handler);
        }

        final FormItemIcon blankIcon = createBlankIcon();
        final FormItemIcon editIcon = createEditIcon();

        this.innerForm = new LocatableDynamicForm(getName() + "_innerForm");
        this.staticItem = prepareStaticFormItem();
        this.editItem = prepareEditFormItem();
        this.innerForm.setItems(staticItem, editItem);
        this.innerForm.setCanFocus(true);
        this.innerForm.setNumCols(1);

        // if we are not in edit-mode, we want to show the edit icon, but only if the mouse hovers over us
        this.innerForm.addMouseOutHandler(new MouseOutHandler() {
            @Override
            public void onMouseOut(MouseOutEvent event) {
                if (!isEditing()) {
                    EditableFormItem.this.staticItem.setIcons(blankIcon);
                    EditableFormItem.this.markForRedraw();
                }
            }
        });
        this.innerForm.addMouseOverHandler(new MouseOverHandler() {
            @Override
            public void onMouseOver(MouseOverEvent event) {
                if (!isEditing() && !isReadOnly()) {
                    EditableFormItem.this.staticItem.setIcons(editIcon);
                    EditableFormItem.this.markForRedraw();
                }
            }
        });

        setCanvas(this.innerForm);
        setCanFocus(true);
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
            @Override
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
            @Override
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

    /**
     * This instantiates the static item. The form item created by this method will
     * be further customized by {@link #prepareStaticFormItem()}.
     *  
     * @return the form item used to show the static (read-only) value
     */
    protected FormItem instantiateStaticFormItem() {
        StaticTextItem item = new StaticTextItem();
        return item;
    }

    /**
     * This instantiates the edit item. The form item created by this method will
     * be further customized by {@link #prepareEditFormItem()}.
     * 
     * Subclasses will usually override this method to provide a different form
     * item such as a checkbox, radio buttons, etc.
     *  
     * @return the form item used to edit the value
     */
    protected FormItem instantiateEditFormItem() {
        TextItem item = new TextItem();
        return item;
    }

    protected FormItemIcon createEditIcon() {
        FormItemIcon editIcon = new FormItemIcon();
        editIcon.setSrc(ImageManager.getEditIcon());
        editIcon.setPrompt(MSG.common_button_edit()); // TODO have better message?
        editIcon.addFormItemClickHandler(new FormItemClickHandler() {
            @Override
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
            @Override
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
            @Override
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

    /**
     * This sets the {@link #setEditing(boolean) edit flag} to false and ensures
     * the component switches to the static, read-only mode.
     */
    protected void switchToStaticMode() {
        if (isEditing()) {
            setEditing(false);
            markForRedraw();
        }
    }

    /**
     * This sets the {@link #setEditing(boolean) edit flag} to true and ensures
     * the component switches to the edit, read-write mode.
     */
    protected void switchToEditMode() {
        if (!isEditing()) {
            setEditing(true);
            setEditItemValue(this.editItem, this.staticItem.getValue());
            markForRedraw();
        }
    }

    protected boolean isEditing() {
        return this.editing;
    }

    /**
     * This just sets the internal flag indicating if the form is in editing mode.
     * To switch modes, you use {@link #switchToEditMode()} and {@link #switchToStaticMode()}.
     *
     * @param editing the new editing flag
     */
    protected void setEditing(boolean editing) {
        this.editing = editing;
    }

    /**
     * When the user has accepted a new edited value for this form item, this method is called.
     * This method is responsible for invoking the {@link #getValueEditedHandler() handler}.
     *  
     * @param newValue the new value just accepted by the user and validated by the system
     */
    protected void setApprovedNewValue(Object newValue) {
        setStaticItemValue(this.staticItem, newValue);
        if (this.valueEditedHandler != null) {
            this.valueEditedHandler.editedValue(newValue);
        }
    }

    /**
     * When a user first elects to edit a value (i.e. switches from static, read-only mode
     * to edit mode), the static value needs to be converted to an object that is appropriate
     * for the edit form item value. This method performs that necessary conversion and
     * sets the edit item's value to that converted value. The converted value is also returned.
     *  
     * @param editItem the edit form item whose value needs to be set
     * @param value the static value that needs to be converted and set in the given edit item
     * 
     * @return the converted value that was set in the edit item
     */
    protected Object setEditItemValue(FormItem editItem, Object value) {
        String convertedValue = (value != null) ? value.toString() : null;
        editItem.setValue(convertedValue);
        return convertedValue;
    }

    /**
     * When a user enters and accepts a new value, the new value needs to be converted
     * to an appropriate value for display in the static, read-only item.
     * This method performs that necessary conversion and sets the static item's value
     * to that converted value. The converted value is also returned.
     *  
     * @param staticItem the static form item whose value needs to be set
     * @param value the value that needs to be converted for display in the static item
     * 
     * @return the converted value that was set in the static item
     */
    protected Object setStaticItemValue(FormItem staticItem, Object value) {
        String convertedValue = (value != null) ? value.toString() : null;
        staticItem.setValue(convertedValue);
        return convertedValue;
    }

    //
    // Below are delegating methods to setValue().
    // When setting this form item's value, we are really setting the static item's value.
    //

    @Override
    public void setValue(boolean value) {
        this.staticItem.setValue(value);
    }

    @Override
    public void setValue(Date value) {
        this.staticItem.setValue(value);
    }

    @Override
    public void setValue(double value) {
        this.staticItem.setValue(value);
    }

    @Override
    public void setValue(float value) {
        this.staticItem.setValue(value);
    }

    @Override
    public void setValue(int value) {
        this.staticItem.setValue(value);
    }

    @Override
    public void setValue(Object value) {
        this.staticItem.setValue(value);
    }

    @Override
    public void setValue(String value) {
        this.staticItem.setValue(value);
    }

    @Override
    public Object getValue() {
        return this.staticItem.getValue();
    }

    //
    // this delegates to the edit item allowing custom validators to be added
    //

    @Override
    public void setValidators(Validator... validators) {
        this.editItem.setValidators(validators);
    }

    /**
     * If the user is not allowed to edit this form item, <code>true</code> is returned.
     * @return flag to indicate if the user is allowed to change the value or not
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        this.staticItem.setTextBoxStyle((!readOnly) ? "editableText" : null);
        markForRedraw();
    }

    /**
     * Returns the handler that will be called when a user edits the current value.
     * This is only called after the user accepts the new value and it passes validation.
     * 
     * @return the callback handler that is invoked when the old value is edited resulting in a new value
     */
    public ValueEditedHandler getValueEditedHandler() {
        return this.valueEditedHandler;
    }

    public void setValueEditedHandler(ValueEditedHandler handler) {
        this.valueEditedHandler = handler;
    }

    /**
     * Marks the canvas item for redraw, which enables both the static and edit items to redraw themselves.
     */
    private void markForRedraw() {
        getForm().markForRedraw();
        this.innerForm.markForRedraw();
    }

    /**
     * When a user has changed the value of the editable field item, this
     * is the type of callback that is called to be informed of the change.
     */
    public static interface ValueEditedHandler {
        public void editedValue(Object newValue);
    }
}
