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
import com.smartgwt.client.widgets.tab.Tab;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTab;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTabSet;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author John Mazzitelli
 */
public class SingleAlertDefinitionView extends LocatableVLayout {

    private AlertDefinition alertDefinition;

    private GeneralPropertiesAlertDefinitionForm generalProperties;
    private ConditionsAlertDefinitionForm conditions;
    private NotificationsAlertDefinitionForm notifications;
    private RecoveryAlertDefinitionForm recovery;
    private DampeningAlertDefinitionForm dampening;

    private Button editButton;
    private Button saveButton;
    private Button cancelButton;

    private boolean allowedToModifyAlertDefinitions;

    public SingleAlertDefinitionView(String locatorId, AbstractAlertDefinitionsView alertDefView) {
        this(locatorId, alertDefView, null);
    }

    public SingleAlertDefinitionView(String locatorId, final AbstractAlertDefinitionsView alertDefView,
        AlertDefinition alertDefinition) {
        super(locatorId);

        this.alertDefinition = alertDefinition;
        this.allowedToModifyAlertDefinitions = alertDefView.isAllowedToModifyAlertDefinitions();

        LocatableTabSet tabSet = new LocatableTabSet(this.getLocatorId());
        tabSet.setHeight100();

        Tab generalPropertiesTab = new LocatableTab(tabSet.extendLocatorId("General"), MSG
            .view_alert_common_tab_general());
        generalProperties = new GeneralPropertiesAlertDefinitionForm(this.getLocatorId(), alertDefinition);
        generalPropertiesTab.setPane(generalProperties);

        Tab conditionsTab = new LocatableTab(tabSet.extendLocatorId("Conditions"), MSG
            .view_alert_common_tab_conditions());
        conditions = new ConditionsAlertDefinitionForm(this.getLocatorId(), alertDefView.getResourceType(),
            alertDefinition);
        conditionsTab.setPane(conditions);

        Tab notificationsTab = new LocatableTab(tabSet.extendLocatorId("Notifications"), MSG
            .view_alert_common_tab_notifications());
        notifications = new NotificationsAlertDefinitionForm(this.getLocatorId(), alertDefinition);
        notificationsTab.setPane(notifications);

        Tab recoveryTab = new LocatableTab(tabSet.extendLocatorId("Recovery"), MSG.view_alert_common_tab_recovery());
        recovery = new RecoveryAlertDefinitionForm(this.getLocatorId(), alertDefView.getAlertDefinitionDataSource(),
            alertDefinition);
        recoveryTab.setPane(recovery);

        Tab dampeningTab = new LocatableTab(tabSet.extendLocatorId("Dampening"), MSG.view_alert_common_tab_dampening());
        dampening = new DampeningAlertDefinitionForm(this.getLocatorId(), alertDefinition);
        dampeningTab.setPane(dampening);

        tabSet.setTabs(generalPropertiesTab, conditionsTab, notificationsTab, recoveryTab, dampeningTab);

        final HLayout buttons = new HLayout();
        buttons.setMembersMargin(20);

        editButton = new LocatableButton(this.extendLocatorId("Edit"), MSG.common_button_edit());
        saveButton = new LocatableButton(this.extendLocatorId("Save"), MSG.common_button_save());
        cancelButton = new LocatableButton(this.extendLocatorId("Cancel"), MSG.common_button_cancel());

        editButton.show();
        saveButton.hide();
        cancelButton.hide();

        buttons.addMember(editButton);
        buttons.addMember(saveButton);
        buttons.addMember(cancelButton);

        editButton.setDisabled(!allowedToModifyAlertDefinitions);

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

                alertDefView.commitAlertDefinition(getAlertDefinition());
            }
        });

        cancelButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                setAlertDefinition(getAlertDefinition()); // reverts data back to original
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
        if (!this.allowedToModifyAlertDefinitions) {
            // this is just a safety measure - we should never get here if we don't have perms, but just in case,
            // don't do anything to allow the def to be editable. Should we notify the message center?
            return;
        }

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
