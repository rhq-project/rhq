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

import java.util.List;

import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.JobTrigger;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.operation.bean.OperationSchedule;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.OperationGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Ian Springer
 */
public abstract class OperationScheduleDataSource<T extends OperationSchedule> extends RPCDataSource<T> {

    public static abstract class Field {
        public static final String ID = "id";
        public static final String JOB_NAME = "jobName";
        public static final String JOB_GROUP = "jobGroup";
        public static final String OPERATION_NAME = "operationName";
        public static final String OPERATION_DISPLAY_NAME = "operationDisplayName";
        public static final String PARAMETERS = "parameters";
        public static final String SUBJECT = "subject";
        public static final String DESCRIPTION = "description";
        public static final String JOB_TRIGGER = "jobTrigger";
    }

    protected OperationGWTServiceAsync operationService = GWTServiceLookup.getOperationService();

    public OperationScheduleDataSource() {
        super();
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    // TODO: i18n
    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        DataSourceIntegerField idField = new DataSourceIntegerField(Field.ID, "ID");
        idField.setPrimaryKey(true);
        idField.setCanEdit(false);
        fields.add(idField);               

        DataSourceTextField operationDisplayNameField = createTextField(Field.OPERATION_DISPLAY_NAME, "Operation", null, 100, true);
        fields.add(operationDisplayNameField);

        DataSourceTextField subjectField = createTextField(Field.SUBJECT, "Owner", null, 100, true);
        fields.add(subjectField);

        DataSourceTextField descriptionField = createTextField(Field.DESCRIPTION, "Description", null, 100, true);
        fields.add(descriptionField);

        DataSourceTextField jobTriggerField = createTextField(Field.JOB_TRIGGER, "Schedule", null, 100, true);
        fields.add(jobTriggerField);

        return fields;
    }

    protected abstract T createOperationSchedule();

    @Override
    public T copyValues(Record from) {
        T to = createOperationSchedule();

        to.setId(from.getAttributeAsInt(Field.ID));
        to.setJobName(from.getAttribute(Field.JOB_NAME));        
        to.setJobGroup(from.getAttribute(Field.JOB_GROUP));
        to.setJobTrigger((JobTrigger)from.getAttributeAsObject(Field.JOB_TRIGGER));
        to.setSubject((Subject)from.getAttributeAsObject(Field.SUBJECT));
        to.setParameters((Configuration)from.getAttributeAsObject(Field.PARAMETERS));
        to.setOperationName(from.getAttribute(Field.OPERATION_NAME));
        to.setOperationDisplayName(from.getAttribute(Field.OPERATION_DISPLAY_NAME));
        to.setDescription(from.getAttribute(Field.DESCRIPTION));

        return to;
    }

    @Override
    public ListGridRecord copyValues(T from) {
        ListGridRecord to = new ListGridRecord();

        to.setAttribute(Field.ID, from.getId());
        to.setAttribute(Field.JOB_NAME, from.getJobName());
        to.setAttribute(Field.JOB_GROUP, from.getJobGroup());
        to.setAttribute(Field.JOB_TRIGGER, from.getJobTrigger());
        to.setAttribute(Field.SUBJECT, from.getSubject());
        to.setAttribute(Field.PARAMETERS, from.getParameters());
        to.setAttribute(Field.OPERATION_NAME, from.getOperationName());
        to.setAttribute(Field.OPERATION_DISPLAY_NAME, from.getOperationDisplayName());        
        to.setAttribute(Field.DESCRIPTION, from.getDescription());

        return to;
    }

}
