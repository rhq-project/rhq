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

import org.rhq.core.domain.common.ProductInfo;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.components.configuration.PropertyValueChangeEvent;
import org.rhq.enterprise.gui.coregui.client.components.configuration.PropertyValueChangeListener;
import org.rhq.enterprise.gui.coregui.client.components.form.DurationItem;
import org.rhq.enterprise.gui.coregui.client.components.form.TimeUnit;
import org.rhq.enterprise.gui.coregui.client.components.wizard.AbstractWizardStep;
import org.rhq.enterprise.gui.coregui.client.gwt.ConfigurationGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.operation.schedule.AbstractOperationScheduleDataSource;
import org.rhq.enterprise.gui.coregui.client.util.selenium.Locatable;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * This step displays a config editor for the user to enter the new Resource's initial Resource or plugin configuration.
 *
 * @author Jay Shaughnessy
 * @author Greg Hinkle
 */
public class ResourceFactoryConfigurationStep extends AbstractWizardStep implements PropertyValueChangeListener {

    private LocatableVLayout vLayout;
    private ConfigurationEditor editor;
    private Configuration startingConfig;
    private DurationItem timeoutItem;
    private AbstractResourceFactoryWizard wizard;

    public ResourceFactoryConfigurationStep(AbstractResourceFactoryWizard wizard) {
        this.wizard = wizard;
    }

    public Canvas getCanvas(Locatable parent) {
        boolean newCanvas = this.vLayout == null;

        if (newCanvas) {
            String locatorId = (null == parent) ? "ResourceFactoryConfig" : parent
                .extendLocatorId("ResourceFactoryConfig");
            this.vLayout = new LocatableVLayout(locatorId);

            // only create the timeout member 1 time, even if we end up recreating the config editor
            TreeSet<TimeUnit> supportedUnits = new TreeSet<TimeUnit>();
            supportedUnits.add(TimeUnit.SECONDS);
            supportedUnits.add(TimeUnit.MINUTES);
            timeoutItem = new DurationItem(AbstractOperationScheduleDataSource.Field.TIMEOUT,
                MSG.view_operationScheduleDetails_field_timeout(), TimeUnit.MILLISECONDS, supportedUnits, false, false,
                vLayout);
            ProductInfo productInfo = CoreGUI.get().getProductInfo();
            timeoutItem.setContextualHelp(MSG.widget_resourceFactoryWizard_timeoutHelp(productInfo.getShortName()));

            DynamicForm timeoutForm = new DynamicForm();
            timeoutForm.setFields(timeoutItem);
            timeoutForm.setMargin(10);
            vLayout.addMember(timeoutForm);
        }

        // if this is a newCanvas, or if the starting config has changed, create a new config editor. The starting
        // config (i.e. template) may have changed if the user, via the previous button, backed up and changed the
        // selected template. 
        if (newCanvas || this.startingConfig != wizard.getNewResourceStartingConfiguration()) {

            final ConfigurationDefinition def = wizard.getNewResourceConfigurationDefinition();
            if (def != null) {

                this.startingConfig = wizard.getNewResourceStartingConfiguration();

                if (!newCanvas) {
                    Canvas doomedConfigEditor = this.vLayout.getMember(0);
                    this.vLayout.removeMember(doomedConfigEditor);
                    doomedConfigEditor.destroy();
                }

                this.startingConfig = wizard.getNewResourceStartingConfiguration();

                ConfigurationGWTServiceAsync configurationService = GWTServiceLookup.getConfigurationService();
                configurationService.getOptionValuesForConfigDefinition(-1, def,
                    new AsyncCallback<ConfigurationDefinition>() {
                        public void onSuccess(ConfigurationDefinition result) {
                            createAndAddConfigurationEditor(result);
                        }

                        public void onFailure(Throwable throwable) {
                            createAndAddConfigurationEditor(def);
                        }

                        private void createAndAddConfigurationEditor(ConfigurationDefinition def) {
                            editor = new ConfigurationEditor(vLayout.extendLocatorId("Editor"), def, startingConfig);
                            editor.setAllPropertiesWritable(true);
                            editor.addPropertyValueChangeListener(ResourceFactoryConfigurationStep.this);
                            wizard.getView().updateButtonEnablement();
                            vLayout.addMember(editor, 0);
                        }
                    });
            }
        }

        return vLayout;
    }

    @Override
    public boolean isNextButtonEnabled() {
        return (editor == null) || ((editor != null) && editor.isValid());
    }

    public boolean nextPage() {
        // Finish.
        if ((editor != null) && editor.isValid()) {
            wizard.setNewResourceConfiguration(editor.getConfiguration());
            wizard.setNewResourceCreateTimeout(timeoutItem.getValueAsInteger());
            wizard.execute();
            return true;
        }

        return false;
    }

    public String getName() {
        return MSG.widget_resourceFactoryWizard_editConfigStepName();
    }

    @Override
    public void propertyValueChanged(PropertyValueChangeEvent event) {
        wizard.getView().updateButtonEnablement();
    }

}