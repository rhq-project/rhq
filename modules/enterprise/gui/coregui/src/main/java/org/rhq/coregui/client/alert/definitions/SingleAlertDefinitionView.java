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

package org.rhq.coregui.client.alert.definitions;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.VisibilityChangedEvent;
import com.smartgwt.client.widgets.events.VisibilityChangedHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;
import com.smartgwt.client.widgets.tab.events.TabDeselectedEvent;
import com.smartgwt.client.widgets.tab.events.TabDeselectedHandler;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * @author John Mazzitelli
 * @author Jirka Kremser
 */
public class SingleAlertDefinitionView extends EnhancedVLayout {

    private AlertDefinition alertDefinition;
    private AbstractAlertDefinitionsView alertDefView;

    private GeneralPropertiesAlertDefinitionForm generalProperties;
    private ConditionsAlertDefinitionForm conditions;
    private NotificationsAlertDefinitionForm notifications;
    private RecoveryAlertDefinitionForm recovery;
    private DampeningAlertDefinitionForm dampening;

    private Button editButton;
    private Button saveButton;
    private Button cancelButton;

    private TabSet tabSet;
    private Tab generalPropertiesTab;
    private HandlerRegistration handlerRegistration;
    private boolean needsCleanup = false;

    private boolean isAuthorizedToModifyAlertDefinitions;

    public SingleAlertDefinitionView(AbstractAlertDefinitionsView alertDefView) {
        this(alertDefView, null);
    }

    @Override
    public void destroy() {
        if (handlerRegistration == null) { // the leaving dialog has been answered
            super.destroy();
            needsCleanup = false;
        } else {
            needsCleanup = true;
        }
    }

    public SingleAlertDefinitionView(final AbstractAlertDefinitionsView alertDefView, AlertDefinition alertDefinition) {
        super();

        this.alertDefinition = alertDefinition;
        this.isAuthorizedToModifyAlertDefinitions = alertDefView.isAuthorizedToModifyAlertDefinitions();
        this.alertDefView = alertDefView;

        tabSet = new TabSet();
        tabSet.setHeight100();

        generalPropertiesTab = new Tab(MSG.common_title_generalProp());
        generalProperties = new GeneralPropertiesAlertDefinitionForm(alertDefinition);
        generalPropertiesTab.setPane(generalProperties);
        generalPropertiesTab.addTabDeselectedHandler(new TabDeselectedHandler() {

            @Override
            public void onTabDeselected(TabDeselectedEvent event) {
                if (!generalProperties.validate()) {
                    event.cancel();
                }
            }
        });

        Tab conditionsTab = new Tab(MSG.view_alert_common_tab_conditions());
        conditions = new ConditionsAlertDefinitionForm(alertDefView.getResourceType(), alertDefinition);
        conditionsTab.setPane(conditions);

        Tab notificationsTab = new Tab(MSG.view_alert_common_tab_notifications());
        notifications = new NotificationsAlertDefinitionForm(alertDefinition);
        notificationsTab.setPane(notifications);

        Tab recoveryTab = new Tab(MSG.view_alert_common_tab_recovery());
        recovery = new RecoveryAlertDefinitionForm(alertDefView.getAlertDefinitionDataSource(), alertDefinition);
        recoveryTab.setPane(recovery);

        Tab dampeningTab = new Tab(MSG.view_alert_common_tab_dampening());
        dampening = new DampeningAlertDefinitionForm(alertDefinition);
        dampeningTab.setPane(dampening);

        tabSet.setTabs(generalPropertiesTab, conditionsTab, notificationsTab, recoveryTab, dampeningTab);

        final HLayout buttons = new HLayout();
        buttons.setMembersMargin(20);

        editButton = new Button(MSG.common_button_edit());
        saveButton = new Button(MSG.common_button_save());
        cancelButton = new Button(MSG.common_button_cancel());

        editButton.show();
        saveButton.hide();
        cancelButton.hide();

        buttons.addMember(editButton);
        buttons.addMember(saveButton);
        buttons.addMember(cancelButton);

        editButton.setDisabled(!isAuthorizedToModifyAlertDefinitions);

        editButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                makeEditable();
            }
        });

        saveButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                save();
            }
        });

        cancelButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                unregisterHandler();
                setAlertDefinition(getAlertDefinition()); // reverts data back to original
                makeViewOnly();
            }
        });

        setMembersMargin(10);
        addMember(tabSet);
        addMember(buttons);
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        if (alertDefinition == null || alertDefinition.getId() == 0) {
            // disable the back button when creating new definition
            setBackButtonDisabled(true);
        }
    }

    public AlertDefinition getAlertDefinition() {
        return alertDefinition;
    }

    public boolean isResetMatching() {
        return conditions.isResetMatching() || dampening.isResetMatching();
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
        // if (!this.allowedToModifyAlertDefinitions) {
        // this is just a safety measure - we should never get here if we don't have perms, but just in case,
        // don't do anything to allow the def to be editable. Should we notify the message center?
        //   return;
        // }

        saveButton.show();
        cancelButton.show();
        editButton.hide();

        generalProperties.makeEditable();
        conditions.makeEditable();
        notifications.makeEditable();
        recovery.makeEditable();
        dampening.makeEditable();

        unregisterHandler();
        handlerRegistration = addVisibilityChangedHandler(new VisibilityChangedHandler() {
            public void onVisibilityChanged(VisibilityChangedEvent event) {
                if (!event.getIsVisible()) {
                    SC.ask(MSG.view_alert_definitions_leaveUnsaved(), new BooleanCallback() {
                        public void execute(Boolean value) {
                            if (value) {
                                save();
                            } else {
                                unregisterHandler();
                            }
                        }
                    });
                }
            }
        });
        // disable the back button
        setBackButtonDisabled(true);
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
        setBackButtonDisabled(false);
    }

    public void saveAlertDefinition() {
        generalProperties.saveAlertDefinition();
        conditions.saveAlertDefinition();
        notifications.saveAlertDefinition();
        recovery.saveAlertDefinition();
        dampening.saveAlertDefinition();
    }

    private void save() {
        if (generalProperties.validate()) {
            boolean resetMatching = isResetMatching();
            saveAlertDefinition();
            setAlertDefinition(getAlertDefinition()); // loads data into static fields
            editButton.disable();
            makeViewOnly();

            alertDefView.commitAlertDefinition(getAlertDefinition(), resetMatching,
                new AsyncCallback<AlertDefinition>() {
                    @Override
                    public void onSuccess(final AlertDefinition alertDef) {
                        setAlertDefinition(alertDef);
                        unregisterHandler();
                        editButton.enable();
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        unregisterHandler();
                        // no error handling, the notification is done in the subclasses of AbstractAlertDefinitionsView
                        editButton.enable();
                    }
                });
        } else {
            tabSet.selectTab(generalPropertiesTab);
        }
    }

    private void setBackButtonDisabled(boolean enabled) {
        Canvas layoutCandidate = getParentElement();
        if (layoutCandidate instanceof EnhancedVLayout) {
            EnhancedVLayout parentLayout = (EnhancedVLayout) getParentElement();
            Canvas backButton = parentLayout.getMember("backButton");
            if (backButton != null) {
                backButton.setDisabled(enabled);
            }
        }
    }

    private void unregisterHandler() {
        if (handlerRegistration != null) {
            handlerRegistration.removeHandler();
            handlerRegistration = null;
            if (needsCleanup)
                destroy();
        }
    }
}
