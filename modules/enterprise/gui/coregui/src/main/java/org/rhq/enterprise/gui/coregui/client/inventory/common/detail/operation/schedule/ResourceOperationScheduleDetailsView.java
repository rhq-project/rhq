/*
 * RHQ Management Platform
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.rhq.enterprise.gui.coregui.client.inventory.common.detail.operation.schedule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;

import org.rhq.core.domain.common.JobTrigger;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.components.form.AbstractRecordEditor;
import org.rhq.enterprise.gui.coregui.client.components.form.EnhancedDynamicForm;
import org.rhq.enterprise.gui.coregui.client.components.trigger.JobTriggerEditor;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.schedule.ResourceOperationScheduleDataSource;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Ian Springer
 */
public class ResourceOperationScheduleDetailsView extends AbstractRecordEditor {

    private static final String FIELD_OPERATION_DESCRIPTION = "operationDescription";
    private static final String FIELD_OPERATION_PARAMETERS = "operationParameters";

    private ResourceComposite resourceComposite;
    private Map<String, String> operationNameToDescriptionMap = new HashMap<String, String>();
    private Map<String, ConfigurationDefinition> operationNameToParametersDefinitionMap =
        new HashMap<String, ConfigurationDefinition>();
    private SelectItem operationNameItem;
    private StaticTextItem operationDescriptionItem;
    private StaticTextItem operationParametersItem;
    private LocatableHLayout operationParametersConfigurationHolder;
    private JobTriggerEditor triggerEditor;

    public ResourceOperationScheduleDetailsView(String locatorId, ResourceComposite resourceComposite, int scheduleId) {
        super(locatorId, new ResourceOperationScheduleDataSource(resourceComposite), scheduleId, "Scheduled Operation", null);

        this.resourceComposite = resourceComposite;
        ResourceType resourceType = this.resourceComposite.getResource().getResourceType();
        Set<OperationDefinition> operationDefinitions = resourceType.getOperationDefinitions();
        for (OperationDefinition operationDefinition : operationDefinitions) {
            this.operationNameToDescriptionMap.put(operationDefinition.getName(), operationDefinition.getDescription());
            this.operationNameToParametersDefinitionMap.put(operationDefinition.getName(),
                operationDefinition.getParametersConfigurationDefinition());
        }
    }

    @Override
    public void renderView(ViewPath viewPath) {
        super.renderView(viewPath);

        // Existing schedules are not editable. This may change in the future.
        boolean isReadOnly = (getRecordId() != 0);
        init(isReadOnly);
    }

    @Override
    protected List<FormItem> createFormItems(EnhancedDynamicForm form) {
        List<FormItem> items = new ArrayList<FormItem>();

        this.operationNameItem = new SelectItem(ResourceOperationScheduleDataSource.Field.OPERATION_NAME);
        items.add(this.operationNameItem);
        this.operationNameItem.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                refreshOperationDescriptionItem();
                refreshOperationParametersItem();
            }
        });

        this.operationDescriptionItem = new StaticTextItem(FIELD_OPERATION_DESCRIPTION, "Operation Description");
        items.add(this.operationDescriptionItem);

        this.operationParametersItem = new StaticTextItem(FIELD_OPERATION_PARAMETERS, "Operation Parameters");
        items.add(this.operationParametersItem);

        return items;
    }

    @Override
    protected LocatableVLayout buildContentPane() {
        LocatableVLayout contentPane = super.buildContentPane();

        this.operationParametersConfigurationHolder = new LocatableHLayout(extendLocatorId("ConfigHolder"));
        this.operationParametersConfigurationHolder.setVisible(false);
        contentPane.addMember(this.operationParametersConfigurationHolder);

        if (isNewRecord()) {
            this.triggerEditor = new JobTriggerEditor(extendLocatorId("TriggerEditor"));
            contentPane.addMember(this.triggerEditor);
        }
        
        EnhancedDynamicForm notesForm = new EnhancedDynamicForm(extendLocatorId("NotesForm"), isReadOnly(),
            isNewRecord());
        TextAreaItem notesItem = new TextAreaItem(ResourceOperationScheduleDataSource.Field.DESCRIPTION, "Notes");
        notesItem.setStartRow(true);
        notesItem.setColSpan(4);
        notesForm.setFields(notesItem);
        contentPane.addMember(notesForm);

        return contentPane;
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        refreshOperationDescriptionItem();
        refreshOperationParametersItem();
    }

    @Override
    protected String getTitleFieldName() {
        return ResourceOperationScheduleDataSource.Field.OPERATION_DISPLAY_NAME;
    }

    @Override
    protected void save() {
        try {
            JobTrigger trigger = this.triggerEditor.getJobTrigger();
            System.out.println(trigger);
        }
        catch (Exception e) {
            e.printStackTrace();
            CoreGUI.getMessageCenter().notify(new Message(e.getMessage(), Message.Severity.Warning));
            return;
        }

        super.save();
    }

    private void refreshOperationDescriptionItem() {
        String operationName = this.operationNameItem.getValueAsString();
        String value;
        if (operationName == null) {
            value = "<i>Select an operation.</i>";
        } else {
            value = this.operationNameToDescriptionMap.get(operationName);
        }
        this.operationDescriptionItem.setValue(value);
    }

    private void refreshOperationParametersItem() {
        String operationName = this.operationNameItem.getValueAsString();
        String value;
        if (operationName == null) {
            value = "<i>Select an operation.</i>";
        } else {
            ConfigurationDefinition parametersDefinition = this.operationNameToParametersDefinitionMap.get(operationName);
            if (parametersDefinition == null || parametersDefinition.getPropertyDefinitions().isEmpty()) {
                value = "<i>" + MSG.view_operationCreateWizard_parametersStep_noParameters() + "</i>";

                for (Canvas child : this.operationParametersConfigurationHolder.getChildren()) {
                    child.destroy();
                }
                this.operationParametersConfigurationHolder.hide();
            } else {
                value = "<i>Enter parameters below...</i>";

                for (Canvas child : this.operationParametersConfigurationHolder.getChildren()) {
                    child.destroy();
                }
                Configuration defaultConfiguration = (parametersDefinition.getDefaultTemplate() != null) ?
                    parametersDefinition.getDefaultTemplate().createConfiguration() : new Configuration();
                ConfigurationEditor configurationEditor = new ConfigurationEditor("ParametersEditor", parametersDefinition,
                    defaultConfiguration);
                configurationEditor.setReadOnly(isReadOnly());
                this.operationParametersConfigurationHolder.addMember(configurationEditor);
                this.operationParametersConfigurationHolder.show();
            }
        }
        this.operationParametersItem.setValue(value);
    }

}
