/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.coregui.client.inventory.common.detail.operation.schedule;

import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeSet;

import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceDateTimeField;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.types.FieldType;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.JobTrigger;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.bean.OperationSchedule;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.gwt.OperationGWTServiceAsync;
import org.rhq.coregui.client.inventory.groups.detail.operation.schedule.GroupOperationScheduleDataSource;
import org.rhq.coregui.client.util.RPCDataSource;
import org.rhq.coregui.client.util.RecordUtility;

/**
 * @author Ian Springer
 */
public abstract class AbstractOperationScheduleDataSource<T extends OperationSchedule> extends
    RPCDataSource<T, Criteria> {

    public static abstract class Field {
        public static final String ID = "id";
        public static final String JOB_NAME = "jobName";
        public static final String JOB_GROUP = "jobGroup";
        public static final String OPERATION_NAME = "operationName";
        public static final String OPERATION_DISPLAY_NAME = "operationDisplayName";
        public static final String PARAMETERS = "parameters";
        public static final String SUBJECT = "subject";
        public static final String SUBJECT_ID = "subjectId";
        public static final String DESCRIPTION = "description";
        public static final String NEXT_FIRE_TIME = "nextFireTime";
        public static final String TIMEOUT = "timeout";
        public static final String JOB_TRIGGER = "jobTrigger";

        // job trigger fields
        public static final String START_TIME = "startTime";
        public static final String REPEAT_INTERVAL = "repeatInterval";
        public static final String REPEAT_COUNT = "repeatCount";
        public static final String END_TIME = "endTime";
        public static final String CRON_EXPRESSION = "cronExpression";
    }

    public static abstract class RequestProperty {
        public static final String PARAMETERS = "parameters";
    }

    protected OperationGWTServiceAsync operationService = GWTServiceLookup.getOperationService();

    private ResourceType resourceType;

    public AbstractOperationScheduleDataSource(ResourceType resourceType) {
        super();
        this.resourceType = resourceType;
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    // TODO: i18n
    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        DataSourceIntegerField idField = new DataSourceIntegerField(Field.ID, MSG
            .dataSource_operationSchedule_field_id());
        idField.setPrimaryKey(true);
        idField.setCanEdit(false);
        fields.add(idField);

        DataSourceTextField operationNameField = createTextField(Field.OPERATION_NAME, MSG
            .dataSource_operationSchedule_field_operationName(), null, 100, true);
        // sort the op def names in the drop down
        TreeSet<OperationDefinition> operationDefinitions = new TreeSet<OperationDefinition>(
            new Comparator<OperationDefinition>() {
                public int compare(OperationDefinition o1, OperationDefinition o2) {
                    return o1.getDisplayName().compareTo(o2.getDisplayName());
                }
            });
        operationDefinitions.addAll(this.resourceType.getOperationDefinitions());
        LinkedHashMap<String, String> valueMap = new LinkedHashMap<String, String>();
        for (OperationDefinition operationDefinition : operationDefinitions) {
            valueMap.put(operationDefinition.getName(), operationDefinition.getDisplayName());
        }
        operationNameField.setValueMap(valueMap);
        fields.add(operationNameField);

        DataSourceTextField operationDisplayNameField = createTextField(Field.OPERATION_DISPLAY_NAME, MSG
            .dataSource_operationSchedule_field_operationDisplayName(), null, 100, true);
        fields.add(operationDisplayNameField);

        DataSourceField subjectField = new DataSourceField(Field.SUBJECT, FieldType.ANY, MSG
            .dataSource_operationSchedule_field_subject());
        subjectField.setCanEdit(false);
        fields.add(subjectField);

        DataSourceTextField descriptionField = createTextField(Field.DESCRIPTION, MSG
            .dataSource_operationSchedule_field_description(), null, 100, false);
        fields.add(descriptionField);

        DataSourceDateTimeField nextFireTimeField = new DataSourceDateTimeField(Field.NEXT_FIRE_TIME, MSG
            .dataSource_operationSchedule_field_nextFireTime());
        nextFireTimeField.setCanEdit(false);
        fields.add(nextFireTimeField);

        DataSourceIntegerField timeoutField = createIntegerField(Field.TIMEOUT, MSG
            .dataSource_operationSchedule_field_timeout(), 30, null, false);
        fields.add(timeoutField);

        return fields;
    }

    protected abstract T createOperationSchedule();

    @Override
    public T copyValues(Record from) {
        T to = createOperationSchedule();

        to.setId(from.getAttributeAsInt(Field.ID));
        to.setJobName(from.getAttribute(Field.JOB_NAME));
        to.setJobGroup(from.getAttribute(Field.JOB_GROUP));
        Subject subject = new Subject();
        subject.setName(from.getAttribute(Field.SUBJECT));
        subject.setId(from.getAttributeAsInt(Field.SUBJECT_ID));
        to.setSubject(subject);
        Configuration parameters = (Configuration) from.getAttributeAsObject(Field.PARAMETERS);
        Integer timeout = RecordUtility.getAttributeAsInteger(from, Field.TIMEOUT);
        if (timeout != null) {
            if (parameters == null) {
                parameters = new Configuration();
            }
            parameters.put(new PropertySimple(OperationDefinition.TIMEOUT_PARAM_NAME, timeout));
        }
        to.setParameters(parameters);
        to.setOperationName(from.getAttribute(Field.OPERATION_NAME));
        to.setOperationDisplayName(from.getAttribute(Field.OPERATION_DISPLAY_NAME));
        to.setDescription(from.getAttribute(Field.DESCRIPTION));
        to.setNextFireTime(from.getAttributeAsDate(Field.NEXT_FIRE_TIME));
        to.setJobTrigger(createJobTrigger(from.getAttributeAsRecord("jobTrigger")));

        return to;
    }

    @Override
    public ListGridRecord copyValues(T from) {
        ListGridRecord to = new ListGridRecord();

        to.setAttribute(Field.ID, from.getId());
        to.setAttribute(Field.JOB_NAME, from.getJobName());
        to.setAttribute(Field.JOB_GROUP, from.getJobGroup());
        to.setAttribute(Field.SUBJECT, from.getSubject().getName());
        to.setAttribute(Field.SUBJECT_ID, from.getSubject().getId());
        Configuration parameters = from.getParameters();
        to.setAttribute(Field.PARAMETERS, parameters);
        to.setAttribute(Field.OPERATION_NAME, from.getOperationName());
        to.setAttribute(Field.OPERATION_DISPLAY_NAME, from.getOperationDisplayName());
        to.setAttribute(Field.DESCRIPTION, from.getDescription());
        to.setAttribute(Field.NEXT_FIRE_TIME, from.getNextFireTime());
        to.setAttribute(Field.TIMEOUT, (parameters != null) ? parameters.getSimpleValue(
            OperationDefinition.TIMEOUT_PARAM_NAME, null) : null);

        JobTrigger jobTrigger = from.getJobTrigger();
        Record jobTriggerRecord = new ListGridRecord();
        jobTriggerRecord.setAttribute(Field.START_TIME, jobTrigger.getStartDate());
        jobTriggerRecord.setAttribute(Field.REPEAT_INTERVAL, jobTrigger.getRepeatInterval());
        jobTriggerRecord.setAttribute(Field.REPEAT_COUNT, jobTrigger.getRepeatCount());
        jobTriggerRecord.setAttribute(Field.END_TIME, jobTrigger.getEndDate());
        jobTriggerRecord.setAttribute(Field.CRON_EXPRESSION, jobTrigger.getCronExpression());
        to.setAttribute("jobTrigger", jobTriggerRecord);

        return to;
    }

    public JobTrigger createJobTrigger(Record jobTriggerRecord) {
        JobTrigger jobTrigger;

        String cronExpression = jobTriggerRecord.getAttribute(Field.CRON_EXPRESSION);
        if (cronExpression != null) {
            jobTrigger = JobTrigger.createCronTrigger(cronExpression);
        } else {
            // calendar mode
            Date startTime = jobTriggerRecord.getAttributeAsDate(Field.START_TIME);
            Long repeatInterval = jobTriggerRecord.getAttributeAsLong(Field.REPEAT_INTERVAL);
            Integer repeatCount = jobTriggerRecord.getAttributeAsInt(Field.REPEAT_COUNT);
            Date endTime = jobTriggerRecord.getAttributeAsDate(Field.END_TIME);

            if (startTime != null) {
                // LATER

                if (repeatInterval != null) {
                    // LATER AND REPEAT

                    if (repeatCount != null) {
                        jobTrigger = JobTrigger.createLaterAndRepeatTrigger(startTime, repeatInterval, repeatCount);
                    } else {
                        jobTrigger = JobTrigger.createLaterAndRepeatTrigger(startTime, repeatInterval, endTime);
                    }
                } else {
                    // LATER ONCE

                    jobTrigger = JobTrigger.createLaterTrigger(startTime);
                }
            } else {
                // NOW
                if (repeatInterval != null) {
                    // NOW AND REPEAT

                    if (repeatCount != null) {
                        jobTrigger = JobTrigger.createNowAndRepeatTrigger(repeatInterval, repeatCount);
                    } else {
                        jobTrigger = JobTrigger.createNowAndRepeatTrigger(repeatInterval, endTime);
                    }
                } else {
                    // NOW ONCE

                    jobTrigger = JobTrigger.createNowTrigger();
                }
            }
        }

        return jobTrigger;
    }

    protected void addRequestPropertiesToRecord(DSRequest request, Record record) {
        Configuration parameters = (Configuration) request
            .getAttributeAsObject(GroupOperationScheduleDataSource.RequestProperty.PARAMETERS);
        record.setAttribute(GroupOperationScheduleDataSource.Field.PARAMETERS, parameters);
    }

    /**
     * All data sources extending this type do not use the normal criteria object to query.
     * So this just returns null.
     */
    @Override
    protected Criteria getFetchCriteria(DSRequest request) {
        return null;
    }
}
