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
import com.smartgwt.client.widgets.form.fields.RadioGroupItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.criteria.AlertDefinitionCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

/**
 * @author John Mazzitelli
 */
public class RecoveryAlertDefinitionForm extends LocatableDynamicForm implements EditAlertDefinitionForm {

    private AlertDefinition alertDefinition;
    private AbstractAlertDefinitionsDataSource alertDataSource;
    private ArrayList<AlertDefinition> allAlertDefinitions;

    private SelectItem recoverAlertSelection;
    private StaticTextItem recoverAlertStatic;

    private RadioGroupItem disableWhenFiredSelection;
    private StaticTextItem disableWhenFiredStatic;

    private boolean formBuilt = false;

    public RecoveryAlertDefinitionForm(String locatorId, AbstractAlertDefinitionsDataSource dataSource) {
        this(locatorId, dataSource, null);
    }

    public RecoveryAlertDefinitionForm(String locatorId, AbstractAlertDefinitionsDataSource dataSource,
        AlertDefinition alertDefinition) {
        super(locatorId);
        this.alertDataSource = dataSource;
        this.alertDefinition = alertDefinition;
    }

    @Override
    protected void onInit() {
        super.onInit();

        if (!formBuilt) {
            setAlertDefinition(alertDefinition);
            makeViewOnly();
        }
    }

    @Override
    public AlertDefinition getAlertDefinition() {
        return alertDefinition;
    }

    @Override
    public void setAlertDefinition(AlertDefinition alertDef) {
        alertDefinition = alertDef;

        buildForm();

        if (alertDef == null) {
            clearFormValues();
        } else {
            refreshWidgets(allAlertDefinitions);

            disableWhenFiredSelection.setValue(alertDef.getWillRecover() ? "yes" : "no");
            disableWhenFiredStatic.setValue(alertDef.getWillRecover() ? "Yes" : "No");
        }

        markForRedraw();
    }

    @Override
    public void makeEditable() {
        recoverAlertSelection.show();
        disableWhenFiredSelection.show();

        recoverAlertStatic.hide();
        disableWhenFiredStatic.hide();

        markForRedraw();
    }

    @Override
    public void makeViewOnly() {
        recoverAlertSelection.hide();
        disableWhenFiredSelection.hide();

        recoverAlertStatic.show();
        disableWhenFiredStatic.show();

        markForRedraw();
    }

    @Override
    public void saveAlertDefinition() {
        // this silliness is to workaround the validation that AlertDefinition setters try to do
        alertDefinition.setRecoveryId(0);
        alertDefinition.setWillRecover(false);

        alertDefinition.setRecoveryId(Integer.valueOf(recoverAlertSelection.getValue().toString()));
        if (alertDefinition.getRecoveryId() != 0) {
            alertDefinition.setWillRecover(false);
        } else {
            alertDefinition.setWillRecover("yes".equals(disableWhenFiredSelection.getValue()));
        }
    }

    @Override
    public void clearFormValues() {
        recoverAlertSelection.clearValue();
        disableWhenFiredSelection.clearValue();

        recoverAlertStatic.clearValue();
        disableWhenFiredStatic.clearValue();

        markForRedraw();
    }

    private void buildForm() {
        if (!formBuilt) {
            disableWhenFiredSelection = new RadioGroupItem("disableWhenFired", "Disable When Fired");
            LinkedHashMap<String, String> yesNo = new LinkedHashMap<String, String>(2);
            yesNo.put("yes", "Yes");
            yesNo.put("no", "No");
            disableWhenFiredSelection.setValueMap(yesNo);
            disableWhenFiredSelection.setDefaultValue("no");
            disableWhenFiredSelection.setWrapTitle(false);
            disableWhenFiredStatic = new StaticTextItem("disableWhenFiredStatic", "Disable When Fired");
            disableWhenFiredStatic.setWrapTitle(false);

            recoverAlertSelection = new SelectItem("recoveryAlert", "Recover Alert");
            recoverAlertSelection.setDefaultValue("0");
            recoverAlertSelection.setWrapTitle(false);
            recoverAlertSelection.setRedrawOnChange(true);

            recoverAlertStatic = new StaticTextItem("recoveryAlertStatic", "Recover Alert");
            recoverAlertStatic.setDefaultValue(getNoRecoveryMenuItemTitle());
            recoverAlertStatic.setWrapTitle(false);

            // if a recovery alert is set, then this alert definition must not disable itself when fired
            // because it will be needed to recover its recovery alert the next time it fires. disabling is only
            // for non-recoverable alerts or for alerts that will themselves be recovered.
            // therefore, force the disableWhenFired selection to go to no and do not allow it to be changed when appropriate
            recoverAlertSelection.addChangedHandler(new ChangedHandler() {
                @Override
                public void onChanged(ChangedEvent event) {
                    String recoveryAlertDefId = event.getItem().getValue().toString();
                    refreshDisableWhenFiredSelection(recoveryAlertDefId);
                }
            });

            refreshWidgets(null); // for it to at least show the initial "no-op" entry
            loadAllAlertDefinitionsAndRefreshRecoverAlertSelection(); // this gets the real entries asynchronously

            setFields(recoverAlertSelection, recoverAlertStatic, disableWhenFiredSelection, disableWhenFiredStatic);

            formBuilt = true;
        }
    }

    private AlertDefinition lookupAlertDefinition(Integer id) {
        if (id != null && id.intValue() != 0) {
            if (this.allAlertDefinitions != null) {
                for (AlertDefinition def : this.allAlertDefinitions) {
                    if (def.getId() == id.intValue()) {
                        return def;
                    }
                }
            }
        }

        return null;
    }

    private void loadAllAlertDefinitionsAndRefreshRecoverAlertSelection() {
        if (allAlertDefinitions == null) {
            AlertDefinitionCriteria criteria = alertDataSource.getSimpleCriteriaForAll();
            GWTServiceLookup.getAlertService().findAlertDefinitionsByCriteria(criteria,
                new AsyncCallback<PageList<AlertDefinition>>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Cannot build recovery menu", caught);
                    }

                    public void onSuccess(PageList<AlertDefinition> result) {
                        allAlertDefinitions = result;
                        refreshWidgets(allAlertDefinitions);
                    }
                });
        } else {
            // we already got the values before, just refresh the menu (in case this.alertDef changed)
            refreshWidgets(allAlertDefinitions);
        }
    }

    private void refreshWidgets(ArrayList<AlertDefinition> allDefs) {
        int allDefsSize = (allDefs != null) ? allDefs.size() : 0;
        LinkedHashMap<String, String> alertMap = new LinkedHashMap<String, String>(allDefsSize + 1);
        alertMap.put("0", getNoRecoveryMenuItemTitle());
        if (allDefs != null) {
            for (AlertDefinition def : allDefs) {
                if (alertDefinition == null || alertDefinition.getId() != def.getId()) {
                    alertMap.put(String.valueOf(def.getId()), def.getName());
                }
            }
        }
        recoverAlertSelection.setValueMap(alertMap);

        if (alertDefinition != null) {
            AlertDefinition recoveryAlert = lookupAlertDefinition(alertDefinition.getRecoveryId());
            if (recoveryAlert != null) {
                String recoveryIdStr = String.valueOf(recoveryAlert.getId());
                recoverAlertSelection.setValue(recoveryIdStr);
                recoverAlertStatic.setValue(recoveryAlert.getName());
                refreshDisableWhenFiredSelection(recoveryIdStr);
            } else {
                recoverAlertSelection.setValue("0");
                recoverAlertStatic.setValue(getNoRecoveryMenuItemTitle());
                refreshDisableWhenFiredSelection("0");
            }
        }

        recoverAlertSelection.redraw();
    }

    private void refreshDisableWhenFiredSelection(String recoveryAlertDefId) {
        if ("0".equals(recoveryAlertDefId)) {
            disableWhenFiredSelection.setDisabled(false);
        } else {
            disableWhenFiredSelection.setValue("no");
            disableWhenFiredSelection.setDisabled(true);
        }
        disableWhenFiredSelection.redraw();
    }

    private String getNoRecoveryMenuItemTitle() {
        StringBuilder str = new StringBuilder();
        str.append("-- ");
        str.append("No Recovery");
        str.append(" --");
        return str.toString();
    }
}
