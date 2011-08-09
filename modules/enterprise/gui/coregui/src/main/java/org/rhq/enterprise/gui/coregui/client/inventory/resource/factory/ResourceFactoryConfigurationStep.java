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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.factory;

import java.util.TreeSet;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.components.form.DurationItem;
import org.rhq.enterprise.gui.coregui.client.components.form.TimeUnit;
import org.rhq.enterprise.gui.coregui.client.components.wizard.AbstractWizardStep;
import org.rhq.enterprise.gui.coregui.client.gwt.ConfigurationGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.operation.schedule.AbstractOperationScheduleDataSource;
import org.rhq.enterprise.gui.coregui.client.util.selenium.Locatable;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Jay Shaughnessy
 * @author Greg Hinkle
 */
public class ResourceFactoryConfigurationStep extends AbstractWizardStep {

    private boolean noConfigurationNeeded = false; // if true, it has been determined the user doesn't have to set any config
    private LocatableVLayout vLayout;
    private ConfigurationEditor editor;
    private DurationItem timeoutItem;
    AbstractResourceFactoryWizard wizard;

    public ResourceFactoryConfigurationStep(AbstractResourceFactoryWizard wizard) {
        this.wizard = wizard;
    }

    public Canvas getCanvas(Locatable parent) {
        if (vLayout == null) {
            String locatorId = (null == parent) ? "ResourceFactoryConfig" : parent
                .extendLocatorId("ResourceFactoryConfig");
            vLayout = new LocatableVLayout(locatorId);

            final ConfigurationDefinition def = wizard.getNewResourceConfigurationDefinition();
            if (def != null) {

                final Configuration startingConfig = wizard.getNewResourceStartingConfiguration();

                ConfigurationGWTServiceAsync configurationService = GWTServiceLookup.getConfigurationService();
                configurationService.getOptionValuesForConfigDefinition(def,new AsyncCallback<ConfigurationDefinition>() {


                    @Override
                    public void onFailure(Throwable throwable) {
                        ConfigurationEditor configurationEditor = new ConfigurationEditor(vLayout.extendLocatorId("Editor"),
                            def, startingConfig);
//                        configurationEditor.setReadOnly(isReadOnly());
//                        operationParametersConfigurationHolder.addMember(configurationEditor);
//                        operationParametersConfigurationHolder.show();
                        vLayout.addMember(configurationEditor);

                    }

                    @Override
                    public void onSuccess(ConfigurationDefinition result) {
                        ConfigurationEditor configurationEditor = new ConfigurationEditor(vLayout.extendLocatorId("Editor"),
                            result, startingConfig);
//                        configurationEditor.setReadOnly(isReadOnly());
//                        operationParametersConfigurationHolder.addMember(configurationEditor);
//                        operationParametersConfigurationHolder.show();
                        vLayout.addMember(configurationEditor);
                    }
                });



//                Configuration startingConfig = wizard.getNewResourceStartingConfiguration();
//                editor = new ConfigurationEditor(vLayout.extendLocatorId("Editor"), def, startingConfig);
//                vLayout.addMember(editor);
            }

            TreeSet<TimeUnit> supportedUnits = new TreeSet<TimeUnit>();
            supportedUnits.add(TimeUnit.SECONDS);
            supportedUnits.add(TimeUnit.MINUTES);
            timeoutItem = new DurationItem(AbstractOperationScheduleDataSource.Field.TIMEOUT, MSG
                .view_operationScheduleDetails_field_timeout(), TimeUnit.MILLISECONDS, supportedUnits, false, false,
                vLayout);
            timeoutItem.setContextualHelp(MSG.widget_resourceFactoryWizard_timeoutHelp());

            DynamicForm timeoutForm = new DynamicForm();
            timeoutForm.setFields(timeoutItem);
            timeoutForm.setMargin(10);
            vLayout.addMember(timeoutForm);
        }
        return vLayout;
    }

    public boolean nextPage() {
        if (noConfigurationNeeded == true || (editor != null && editor.validate())) {
            wizard.setNewResourceConfiguration((noConfigurationNeeded) ? null : editor.getConfiguration());
            wizard.setNewResourceCreateTimeout(timeoutItem.getValueAsInteger());
            wizard.execute();
            return true;
        }

        return false;
    }

    public String getName() {
        return MSG.widget_resourceFactoryWizard_editConfigStepName();
    }
}