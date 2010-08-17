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

import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;

import org.rhq.core.domain.alert.AlertDefinition;

/**
 * @author John Mazzitelli
 */
public class SingleAlertDefinitionView extends VLayout {

    private AlertDefinition alertDefinition;
    private GeneralPropertiesAlertDefinitionForm generalProperties;
    private ConditionsAlertDefinitionForm conditions;
    private NotificationsAlertDefinitionForm notifications;
    private RecoveryAlertDefinitionForm recovery;
    private DampeningAlertDefinitionForm dampening;

    private Button editButton;
    private Button saveButton;
    private Button cancelButton;

    public SingleAlertDefinitionView() {
        this(null);
    }

    public SingleAlertDefinitionView(AlertDefinition alertDefinition) {
        this.alertDefinition = alertDefinition;

        TabSet tabSet = new TabSet();
        tabSet.setHeight100();

        Tab generalPropertiesTab = new Tab("General Properties");
        generalProperties = new GeneralPropertiesAlertDefinitionForm(alertDefinition);
        generalPropertiesTab.setPane(generalProperties);

        Tab conditionsTab = new Tab("Conditions");
        conditions = new ConditionsAlertDefinitionForm(alertDefinition);
        conditionsTab.setPane(conditions);

        Tab notificationsTab = new Tab("Notifications");
        notifications = new NotificationsAlertDefinitionForm(alertDefinition);
        notificationsTab.setPane(notifications);

        Tab recoveryTab = new Tab("Recovery");
        recovery = new RecoveryAlertDefinitionForm(alertDefinition);
        recoveryTab.setPane(recovery);

        Tab dampeningTab = new Tab("Dampening");
        dampening = new DampeningAlertDefinitionForm(alertDefinition);
        dampeningTab.setPane(dampening);

        tabSet.setTabs(generalPropertiesTab, conditionsTab, notificationsTab, recoveryTab, dampeningTab);

        final HLayout buttons = new HLayout();
        buttons.setMembersMargin(20);

        editButton = new Button("Edit");
        saveButton = new Button("Save");
        cancelButton = new Button("Cancel");

        editButton.show();
        saveButton.hide();
        cancelButton.hide();

        buttons.addMember(editButton);
        buttons.addMember(saveButton);
        buttons.addMember(cancelButton);

        editButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                makeEditable();
            }
        });

        saveButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                saveAlertDefinition();
                setAlertDefinition(getAlertDefinition()); // loads data into static fields
                makeViewOnly();

                // TODO getAlertDefinition() should now have the new user data - commit it to DB
            }
        });

        cancelButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                makeViewOnly();
            }
        });

        setMembersMargin(10);
        addMember(tabSet);
        addMember(buttons);
    }

    public AlertDefinition getAlertDefinition() {
        return alertDefinition;
    }

    public void setAlertDefinition(AlertDefinition alertDef) {
        alertDefinition = alertDef;

        generalProperties.setAlertDefinition(alertDef);
        conditions.setAlertDefinition(alertDef);
        notifications.setAlertDefinition(alertDef);
        recovery.setAlertDefinition(alertDef);
        dampening.setAlertDefinition(alertDef);

        makeViewOnly();
    }

    public void makeEditable() {
        saveButton.show();
        cancelButton.show();
        editButton.hide();

        generalProperties.makeEditable();
        conditions.makeEditable();
        notifications.makeEditable();
        recovery.makeEditable();
        dampening.makeEditable();
    }

    public void makeViewOnly() {
        saveButton.hide();
        cancelButton.hide();
        editButton.show();

        generalProperties.makeViewOnly();
        conditions.makeViewOnly();
        notifications.makeViewOnly();
        recovery.makeViewOnly();
        dampening.makeViewOnly();
    }

    public void saveAlertDefinition() {
        generalProperties.saveAlertDefinition();
        conditions.saveAlertDefinition();
        notifications.saveAlertDefinition();
        recovery.saveAlertDefinition();
        dampening.saveAlertDefinition();
    }
}
