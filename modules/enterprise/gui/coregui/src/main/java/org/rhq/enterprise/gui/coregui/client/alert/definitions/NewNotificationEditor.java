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
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.FormItemIfFunction;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
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

        notificationSenderSelectItem = new SelectItem("notificationSender", "Notification Sender");

        if (notificationToEdit != null) {
            // we were given a notification to edit, you can't change the sender type, its the only option
            notificationSenderSelectItem.setDisabled(true);
            LinkedHashMap<String, String> senders = new LinkedHashMap<String, String>(1);
            senders.put(notificationToEdit.getSenderName(), notificationToEdit.getSenderName());
            notificationSenderSelectItem.setValueMap(senders);
        } else {
            notificationSenderSelectItem.setValueMap("Loading...");
            notificationSenderSelectItem.setDisabled(true);
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

        notificationSenderSelectItem.setDefaultToFirstOption(true);
        notificationSenderSelectItem.setWrapTitle(false);
        notificationSenderSelectItem.setRedrawOnChange(true);
        notificationSenderSelectItem.setWidth("*");

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
                    saveNewNotification();
                    closeFunction.run();
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
        // TODO put config editor here
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

        // notif.setConfiguration(configuration);
        // notif.setExtraConfiguration(extraConfiguration);
    }

    private class ShowIfSenderFunction implements FormItemIfFunction {
        private final String senderName;

        public ShowIfSenderFunction(String senderName) {
            this.senderName = senderName;
        }

        public boolean execute(FormItem item, Object value, DynamicForm form) {
            String selectedSenderString = form.getValue("notificationSender").toString();
            return senderName.equals(selectedSenderString);
        }
    }
}
