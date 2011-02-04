/*
 * RHQ Management Platform
 * Copyright 2010-2011, Red Hat Middleware LLC, and individual contributors
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;

import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.VLayout;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.components.form.AbstractRecordEditor;
import org.rhq.enterprise.gui.coregui.client.components.form.DurationItem;
import org.rhq.enterprise.gui.coregui.client.components.form.EnhancedDynamicForm;
import org.rhq.enterprise.gui.coregui.client.components.form.TimeUnit;
import org.rhq.enterprise.gui.coregui.client.components.form.UnitType;
import org.rhq.enterprise.gui.coregui.client.components.trigger.JobTriggerEditor;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.schedule.ResourceOperationScheduleDataSource;
import org.rhq.enterprise.gui.coregui.client.util.FormUtility;
import org.rhq.enterprise.gui.coregui.client.util.TypeConversionUtility;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Ian Springer
 */
public abstract class AbstractOperationScheduleDetailsView extends AbstractRecordEditor {

    private static final String FIELD_OPERATION_DESCRIPTION = "operationDescription";
    private static final String FIELD_OPERATION_PARAMETERS = "operationParameters";

    private Map<String, String> operationNameToDescriptionMap = new HashMap<String, String>();
    private Map<String, ConfigurationDefinition> operationNameToParametersDefinitionMap =
        new HashMap<String, ConfigurationDefinition>();
    private StaticTextItem operationDescriptionItem;
    private StaticTextItem operationParametersItem;
    private LocatableHLayout operationParametersConfigurationHolder;
    private JobTriggerEditor triggerEditor;
    private Configuration parameters;
    private EnhancedDynamicForm notesForm;

    public AbstractOperationScheduleDetailsView(String locatorId, AbstractOperationScheduleDataSource dataSource,
                                                ResourceType resourceType, int scheduleId) {
        super(locatorId, dataSource, scheduleId, "Scheduled Operation", null);

        Set<OperationDefinition> operationDefinitions = resourceType.getOperationDefinitions();
        for (OperationDefinition operationDefinition : operationDefinitions) {
            this.operationNameToDescriptionMap.put(operationDefinition.getName(), operationDefinition.getDescription());
            this.operationNameToParametersDefinitionMap.put(operationDefinition.getName(),
                operationDefinition.getParametersConfigurationDefinition());
        }
    }

    protected abstract boolean hasControlPermission();

    @Override
    public void renderView(ViewPath viewPath) {
        super.renderView(viewPath);

        // Existing schedules are not editable. This may change in the future.
        boolean isReadOnly = (!hasControlPermission() || (getRecordId() != 0));
        init(isReadOnly);
    }

    @Override
    protected EnhancedDynamicForm buildForm() {
        EnhancedDynamicForm form = super.buildForm();

        form.setNumCols(3);
        form.setColWidths("140", "140", "*");

        return form;
    }

    @Override
    protected List<FormItem> createFormItems(EnhancedDynamicForm form) {
        List<FormItem> items = new ArrayList<FormItem>();

        if (!isNewRecord()) {
            StaticTextItem idItem = new StaticTextItem(AbstractOperationScheduleDataSource.Field.ID);
            items.add(idItem);
        }

        SelectItem operationNameItem = new SelectItem(AbstractOperationScheduleDataSource.Field.OPERATION_NAME);
        items.add(operationNameItem);
        operationNameItem.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                refreshOperationDescriptionItem();
                refreshOperationParametersItem();
            }
        });

        this.operationDescriptionItem = new StaticTextItem(FIELD_OPERATION_DESCRIPTION, "Description");
        this.operationDescriptionItem.setShowTitle(false);
        items.add(this.operationDescriptionItem);

        this.operationParametersItem = new StaticTextItem(FIELD_OPERATION_PARAMETERS, "Parameters");
        this.operationParametersItem.setColSpan(2);
        items.add(this.operationParametersItem);

        return items;
    }

    @Override
    protected LocatableVLayout buildContentPane() {
        LocatableVLayout contentPane = super.buildContentPane();

        this.operationParametersConfigurationHolder = new LocatableHLayout(extendLocatorId("ConfigHolder"));
        this.operationParametersConfigurationHolder.setVisible(false);
        contentPane.addMember(this.operationParametersConfigurationHolder);

        HTMLFlow hr = new HTMLFlow("<p/><hr/><p/>");
        contentPane.addMember(hr);

        if (isNewRecord()) {
            this.triggerEditor = new JobTriggerEditor(extendLocatorId("TriggerEditor"));
            contentPane.addMember(this.triggerEditor);
            hr = new HTMLFlow("<p/><hr/><p/>");
            contentPane.addMember(hr);
        }

        this.notesForm = new EnhancedDynamicForm(extendLocatorId("NotesForm"), isReadOnly(),
            isNewRecord());
        this.notesForm.setColWidths("140", "50%", "140", "50%");

        List<FormItem> notesFields = new ArrayList<FormItem>();

        TreeSet<TimeUnit> supportedUnits = new TreeSet<TimeUnit>();
        supportedUnits.add(TimeUnit.SECONDS);
        supportedUnits.add(TimeUnit.MINUTES);
        supportedUnits.add(TimeUnit.HOURS);
        DurationItem timeoutItem = new DurationItem(AbstractOperationScheduleDataSource.Field.TIMEOUT, "Timeout",
                supportedUnits, false, isReadOnly(), this.notesForm);
        timeoutItem.setContextualHelp("a time duration - if specified, if the duration elapses before a scheduled operation execution has completed, the RHQ Server will timeout the operation and consider it to have failed; note, it is usually not possible to abort the underlying managed resource operation if it was already initiated");
        notesFields.add(timeoutItem);

        if (!isNewRecord()) {
            StaticTextItem nextFireTimeItem = new StaticTextItem(AbstractOperationScheduleDataSource.Field.NEXT_FIRE_TIME,
                    "Next Fire Time");
            notesFields.add(nextFireTimeItem);
        }

        TextAreaItem notesItem = new TextAreaItem(ResourceOperationScheduleDataSource.Field.DESCRIPTION, "Notes");
        notesItem.setWidth(450);
        notesItem.setHeight(60);
        FormUtility.addContextualHelp(notesItem, "an optional description of this scheduled operation (e.g. \"nightly maintenance app server restart\")");
        notesFields.add(notesItem);

        this.notesForm.setFields(notesFields.toArray(new FormItem[notesFields.size()]));

        contentPane.addMember(this.notesForm);

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
    protected Record createNewRecord() {
        Record record = super.createNewRecord();
        Subject sessionSubject = UserSessionManager.getSessionSubject();
        AbstractOperationScheduleDataSource.SubjectRecord subjectRecord =
                new AbstractOperationScheduleDataSource.SubjectRecord(sessionSubject);
        record.setAttribute(ResourceOperationScheduleDataSource.Field.SUBJECT, subjectRecord);
        return record;
    }

    @Override
    protected void editRecord(Record record) {
        refreshOperationDescriptionItem();
        refreshOperationParametersItem();

        super.editRecord(record);
    }

    @Override
    protected void editExistingRecord(Record record) {
        FormItem nextFireTimeItem = this.notesForm.getField(ResourceOperationScheduleDataSource.Field.NEXT_FIRE_TIME);
        nextFireTimeItem.setValue(getForm().getValue(ResourceOperationScheduleDataSource.Field.NEXT_FIRE_TIME));

        DurationItem timeoutItem = (DurationItem) this.notesForm.getField(AbstractOperationScheduleDataSource.Field.TIMEOUT);
        Object value = getForm().getValue(AbstractOperationScheduleDataSource.Field.TIMEOUT);
        Integer integerValue = TypeConversionUtility.toInteger(value);
        timeoutItem.setValue(integerValue, UnitType.TIME);

        FormItem notesItem = this.notesForm.getField(ResourceOperationScheduleDataSource.Field.DESCRIPTION);
        notesItem.setValue(getForm().getValue(ResourceOperationScheduleDataSource.Field.DESCRIPTION));

        super.editExistingRecord(record);
    }

    @Override
    protected void save(DSRequest requestProperties) {
        requestProperties.setAttribute("parameters", this.parameters);

        if (!this.triggerEditor.validate()) {
            // TODO: print error Message
            return;
        }
        EnhancedDynamicForm form = getForm();

        Record jobTriggerRecord = new ListGridRecord();

        Date startTime = this.triggerEditor.getStartTime();
        jobTriggerRecord.setAttribute(ResourceOperationScheduleDataSource.Field.START_TIME, startTime);

        Date endTime = this.triggerEditor.getEndTime();
        jobTriggerRecord.setAttribute(ResourceOperationScheduleDataSource.Field.END_TIME, endTime);

        Integer repeatCount = this.triggerEditor.getRepeatCount();
        jobTriggerRecord.setAttribute(ResourceOperationScheduleDataSource.Field.REPEAT_COUNT, repeatCount);

        Long repeatInterval = this.triggerEditor.getRepeatInterval();
        jobTriggerRecord.setAttribute(ResourceOperationScheduleDataSource.Field.REPEAT_INTERVAL, repeatInterval);

        String cronExpression = this.triggerEditor.getCronExpression();
        jobTriggerRecord.setAttribute(ResourceOperationScheduleDataSource.Field.CRON_EXPRESSION, cronExpression);

        form.setValue("jobTrigger", jobTriggerRecord);

        DurationItem timeoutItem = (DurationItem)this.notesForm.getItem(AbstractOperationScheduleDataSource.Field.TIMEOUT);
        form.setValue(AbstractOperationScheduleDataSource.Field.TIMEOUT, timeoutItem.getValueAsInteger());

        FormItem notesItem = this.notesForm.getField(AbstractOperationScheduleDataSource.Field.DESCRIPTION);
        form.setValue(AbstractOperationScheduleDataSource.Field.DESCRIPTION, (String)notesItem.getValue());

        super.save(requestProperties);
    }

    private void refreshOperationDescriptionItem() {
        String operationName = getSelectedOperationName();
        String value;
        if (operationName == null) {
            value = "<i>Select an operation to see its description.</i>";
        } else {
            value = this.operationNameToDescriptionMap.get(operationName);
        }
        this.operationDescriptionItem.setValue(value);
    }

    private void refreshOperationParametersItem() {
        String operationName = getSelectedOperationName();
        String value;
        if (operationName == null) {
            value = "<i>Select an operation to see its parameters.</i>";
        } else {
            ConfigurationDefinition parametersDefinition = this.operationNameToParametersDefinitionMap.get(operationName);
            if (parametersDefinition == null || parametersDefinition.getPropertyDefinitions().isEmpty()) {
                value = "<i>" + MSG.view_operationCreateWizard_parametersStep_noParameters() + "</i>";

                for (Canvas child : this.operationParametersConfigurationHolder.getChildren()) {
                    child.destroy();
                }
                this.operationParametersConfigurationHolder.hide();
            } else {
                value = isNewRecord() ? "<i>Enter parameters below...</i>" : "";

                for (Canvas child : this.operationParametersConfigurationHolder.getChildren()) {
                    child.destroy();
                }

                // Add spacer so params are indented.
                VLayout horizontalSpacer = new VLayout();
                horizontalSpacer.setWidth(165);
                this.operationParametersConfigurationHolder.addMember(horizontalSpacer);

                Configuration defaultConfiguration = (parametersDefinition.getDefaultTemplate() != null) ?
                    parametersDefinition.getDefaultTemplate().createConfiguration() : new Configuration();
                ConfigurationEditor configurationEditor = new ConfigurationEditor("ParametersEditor", parametersDefinition,
                    defaultConfiguration);
                configurationEditor.setReadOnly(isReadOnly());
                this.parameters = configurationEditor.getConfiguration();
                this.operationParametersConfigurationHolder.addMember(configurationEditor);
                this.operationParametersConfigurationHolder.show();
            }
        }
        this.operationParametersItem.setValue(value);
    }

    private String getSelectedOperationName() {
        FormItem operationNameItem = getForm().getField(ResourceOperationScheduleDataSource.Field.OPERATION_NAME);
        return (String)operationNameItem.getValue();
    }

}
