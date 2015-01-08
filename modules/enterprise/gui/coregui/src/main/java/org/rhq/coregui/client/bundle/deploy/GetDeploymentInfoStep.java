/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.coregui.client.bundle.deploy;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.validator.IsIntegerValidator;

import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.components.wizard.AbstractWizardStep;
import org.rhq.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.coregui.client.gwt.GWTServiceLookup;

/**
 * @author Jay Shaughnessy
 *
 */
public class GetDeploymentInfoStep extends AbstractWizardStep {

    private DynamicForm form;
    private final BundleDeployWizard wizard;
    private final BundleGWTServiceAsync bundleServer = GWTServiceLookup.getBundleService();

    public GetDeploymentInfoStep(BundleDeployWizard wizard) {
        this.wizard = wizard;
    }

    public String getName() {
        return MSG.view_bundle_deployWizard_getInfoStep();
    }

    public Canvas getCanvas() {
        if (form == null) {
            form = new DynamicForm();
            form.setWidth100();
            form.setNumCols(2);
            form.setColWidths("50%", "*");

            bundleServer.getBundleDeploymentName(wizard.getDestination().getId(), wizard.getBundleVersion().getId(),
                -1, //
                new AsyncCallback<String>() {

                    public void onSuccess(String result) {
                        final StaticTextItem nameTextItem = new StaticTextItem("name", MSG
                            .view_bundle_deployWizard_getInfo_deploymentName());
                        nameTextItem.setWidth(300);
                        wizard.setSubtitle(result);
                        nameTextItem.setValue(result);

                        final TextAreaItem descriptionTextAreaItem = new TextAreaItem("description", MSG
                            .view_bundle_deployWizard_getInfo_deploymentDesc());
                        descriptionTextAreaItem.setWidth(300);
                        descriptionTextAreaItem.addChangedHandler(new ChangedHandler() {
                            public void onChanged(ChangedEvent event) {
                                Object value = event.getValue();
                                if (value == null) {
                                    value = "";
                                }
                                wizard.setNewDeploymentDescription(value.toString());
                            }
                        });

                        final CheckboxItem cleanDeploymentCBItem = new CheckboxItem("cleanDeployment", MSG
                            .view_bundle_deployWizard_getInfo_clean());
                        cleanDeploymentCBItem.setValue(wizard.isCleanDeployment());
                        cleanDeploymentCBItem.addChangedHandler(new ChangedHandler() {
                            public void onChanged(ChangedEvent event) {
                                wizard.setCleanDeployment((Boolean) event.getValue());
                            }
                        });

                        final TextItem discoveryDelayItem = new TextItem("discoveryDelay", MSG.view_bundle_deployWizard_discoveryDelay());
                        discoveryDelayItem.setValue(getDiscoveryDelayConfigurationValue());
                        discoveryDelayItem.setWidth(300);
                        discoveryDelayItem.setRequired(false);
                        discoveryDelayItem.setValidators(new IsIntegerValidator());
                        discoveryDelayItem.addChangedHandler(new ChangedHandler() {
                            @Override
                            public void onChanged(ChangedEvent changedEvent) {
                                wizard.getNewDeploymentConfig().setSimpleValue("org.rhq.discoveryDelay", (String) changedEvent.getValue());
                            }
                        });

                        final TextItem deployTimeoutItem = new TextItem("deploymentTimeout", "Timeout for deployment process (default 4 hours)");
                        deployTimeoutItem.setValue(getDeploymentTimeoutValue());
                        deployTimeoutItem.setWidth(300);
                        deployTimeoutItem.setRequired(false);
                        deployTimeoutItem.setValidators(new IsIntegerValidator());
                        deployTimeoutItem.addChangedHandler(new ChangedHandler() {
                            @Override
                            public void onChanged(ChangedEvent changedEvent) {
                                wizard.getNewDeploymentConfig().setSimpleValue("org.rhq.deploymentTimeout", (String) changedEvent.getValue());
                            }
                        });

                        form.setItems(nameTextItem, descriptionTextAreaItem, cleanDeploymentCBItem, discoveryDelayItem, deployTimeoutItem);
                    }

                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_bundle_deployWizard_error_7(), caught);
                    }
                });
        }

        return form;
    }

    public boolean nextPage() {
        return form.validate();
    }

    private String getDiscoveryDelayConfigurationValue() {
        // This could be the value from either recipe or the default one
        Integer rValue = Integer.valueOf(30);

        // If user wanted to override the value in recipe, allow it
        Property discoveryDelayProperty = wizard.getNewDeploymentConfig().get("org.rhq.discoveryDelay");

        if(discoveryDelayProperty == null) {
            // If there was no recipe value, assume the current value is the default one and check live-deployment
            BundleDeployment liveDeployment = wizard.getLiveDeployment();

            // If previous deployment had modified deploymentDelay, use it
            if(liveDeployment != null) {
                Integer previousDeploymentDelay = liveDeployment.getDiscoveryDelay();
                if(previousDeploymentDelay != null) {
                    rValue = previousDeploymentDelay;
                }
            }
        }

        wizard.getNewDeploymentConfig().setSimpleValue("org.rhq.discoveryDelay", rValue.toString());
        return rValue.toString();
    }

    private String getDeploymentTimeoutValue() {
        Integer rValue = Integer.valueOf(4 * 60 * 60);
        PropertySimple timeoutProperty = (PropertySimple) wizard.getNewDeploymentConfig().get("org.rhq.deploymentTimeout");
        if(timeoutProperty != null) {
            rValue = timeoutProperty.getIntegerValue();
        }
        return rValue.toString();
    }
}
