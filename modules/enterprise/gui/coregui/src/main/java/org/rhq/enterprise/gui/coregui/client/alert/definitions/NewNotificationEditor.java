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

package org.rhq.enterprise.gui.coregui.client.alert.definitions;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

/**
 * @author John Mazzitelli
 */
public class NewNotificationEditor extends LocatableDynamicForm {

    private final AlertDefinition alertDefinition; // the definition we are adding the notification to
    private final ArrayList<AlertNotification> notifications; // if we are creating a new notification, it gets added to this list
    private final AlertNotification notificationToEdit; // the notification that this editor is editing (may be null)
    private final Runnable closeFunction; // this is called after a button is pressed and the editor should close 

    private SelectItem notificationSenderSelectItem;
    private CanvasItem senderCanvasItem;

    public NewNotificationEditor(String locatorId, AlertDefinition alertDefinition,
        ArrayList<AlertNotification> notifs, AlertNotification notifToEdit, Runnable closeFunc) {

        super(locatorId);
        this.alertDefinition = alertDefinition;
        this.notifications = notifs;
        this.notificationToEdit = notifToEdit;
        this.closeFunction = closeFunc;
    }

    @Override
    protected void onInit() {
        super.onInit();

        setMargin(20);

        // this is the container that will house the sender-specific form components
        senderCanvasItem = new CanvasItem();
        senderCanvasItem.setShowTitle(false);
        senderCanvasItem.setColSpan(2);

        notificationSenderSelectItem = new SelectItem("notificationSender", "Notification Sender");
        notificationSenderSelectItem.setDefaultToFirstOption(true);
        notificationSenderSelectItem.setWrapTitle(false);
        notificationSenderSelectItem.setRedrawOnChange(true);
        notificationSenderSelectItem.setWidth("*");

        if (notificationToEdit != null) {
            // we were given a notification to edit, you can't change the sender type, its the only option
            notificationSenderSelectItem.setDisabled(true);
            String senderName = notificationToEdit.getSenderName();
            LinkedHashMap<String, String> senders = new LinkedHashMap<String, String>(1);
            senders.put(senderName, senderName);
            notificationSenderSelectItem.setValueMap(senders);
            switchToAlertSender(senderName);
            senderCanvasItem.setVisible(true);
        } else {
            notificationSenderSelectItem.setValueMap("Loading...");
            notificationSenderSelectItem.setDisabled(true);
            senderCanvasItem.setVisible(false); // don't show it yet, until we determine what senders exist
            // we are creating a new notification, need to provide all senders as options
            GWTServiceLookup.getAlertDefinitionService().getAllAlertSenders(new AsyncCallback<String[]>() {
                @Override
                public void onSuccess(String[] result) {
                    if (result != null && result.length > 0) {
                        LinkedHashMap<String, String> senders = new LinkedHashMap<String, String>(result.length);
                        for (String senderName : result) {
                            senders.put(senderName, senderName);
                        }
                        notificationSenderSelectItem.setValueMap(senders);
                        notificationSenderSelectItem.setDisabled(false);
                        notificationSenderSelectItem.redraw();
                        switchToAlertSender(result[0]);
                        senderCanvasItem.show();
                    } else {
                        CoreGUI.getErrorHandler().handleError("No alert senders available");
                    }
                }

                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Cannot get alert senders", caught);
                }
            });
        }

        notificationSenderSelectItem.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                String newAlertSender = event.getValue().toString();
                switchToAlertSender(newAlertSender);
            }
        });

        SpacerItem spacer1 = new SpacerItem();
        spacer1.setColSpan(2);
        spacer1.setHeight(5);

        SpacerItem spacer2 = new SpacerItem();
        spacer2.setColSpan(2);
        spacer2.setHeight(5);

        ButtonItem ok = new ButtonItem("okButtonItem", "OK");
        ok.setEndRow(false);
        ok.setAlign(Alignment.RIGHT);
        ok.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (validate(false)) {
                    AbstractNotificationSenderForm senderForm = (AbstractNotificationSenderForm) senderCanvasItem
                        .getCanvas();
                    if (senderForm.validate()) {
                        saveNewNotification();
                        closeFunction.run();
                    }
                }
            }
        });

        ButtonItem cancel = new ButtonItem("cancelButtonItem", "Cancel");
        cancel.setStartRow(false);
        cancel.setAlign(Alignment.LEFT);
        cancel.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                closeFunction.run();
            }
        });

        ArrayList<FormItem> formItems = new ArrayList<FormItem>();
        formItems.add(notificationSenderSelectItem);
        formItems.add(spacer1);
        formItems.add(senderCanvasItem);
        formItems.add(spacer2);
        formItems.add(ok);
        formItems.add(cancel);

        setFields(formItems.toArray(new FormItem[formItems.size()]));
    };

    private void saveNewNotification() {
        AlertNotification notif;

        if (notificationToEdit == null) {
            // we are adding a new notification - we just add it to the end of the list
            String selectedSender = notificationSenderSelectItem.getValue().toString();
            notif = new AlertNotification(selectedSender);
            notif.setAlertDefinition(alertDefinition);
            notifications.add(notif);
        } else {
            notif = notificationToEdit;
        }

        AbstractNotificationSenderForm senderForm = (AbstractNotificationSenderForm) senderCanvasItem.getCanvas();
        notif.setConfiguration(senderForm.getConfiguration());
        notif.setExtraConfiguration(senderForm.getExtraConfiguration());
    }

    private void switchToAlertSender(String newAlertSender) {
        Canvas oldCanvas = senderCanvasItem.getCanvas();
        if (oldCanvas != null) {
            oldCanvas.markForDestroy();
        }
        AbstractNotificationSenderForm newCanvas = createNotificationSenderForm(newAlertSender);
        senderCanvasItem.setCanvas(newCanvas);
        markForRedraw();
    }

    private AbstractNotificationSenderForm createNotificationSenderForm(String sender) {
        String newLocatorId = extendLocatorId(sender);
        AbstractNotificationSenderForm newCanvas;

        // NOTE: today there is no way for an alert server plugin developer
        // to be able to provide us with a custom UI component to render (like we used to be able
        // to do when the ui was implemented in JSF and the server plugin can give us a JSF snippet).
        // We have to hard code the names of the "special" plugins that require special UIs which
        // are necessary to build the sender configuration for these special alert senders.
        // For those that want to write their own custom alert plugins, you are restricted to
        // using configuration definitions as the only way to configure the sender.
        if ("System Users".equals(sender)) {
            newCanvas = new SystemUsersNotificationSenderForm(newLocatorId, notificationToEdit, sender);
        } else if ("System Roles".equals(sender)) {
            newCanvas = null; // TODO
        } else if ("Resource Operations".equals(sender)) {
            newCanvas = null; // TODO
        } else {
            // catch all - all other senders are assumed to just have simple configuration definition
            // that can be used by our configuration editor UI component to ask for config values.
            newCanvas = new SimpleNotificationSenderForm(newLocatorId, notificationToEdit, sender);
        }
        return newCanvas;
    }
}
