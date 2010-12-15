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
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;

import org.rhq.core.domain.operation.composite.OperationScheduleComposite;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.OperationGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Ian Springer
 */
public abstract class OperationScheduleCompositeDataSource extends RPCDataSource<OperationScheduleComposite> {

    public static abstract class Field {

        public static final String OPERATION_JOB_ID = "operationJobId";
        public static final String OPERATION_NAME = "operationName";
        public static final String OPERATION_NEXT_FIRE_TIME = "operationNextFireTime";
    }

    protected OperationGWTServiceAsync operationService = GWTServiceLookup.getOperationService();

    public OperationScheduleCompositeDataSource() {
        super();
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        // TODO: i18n
        DataSourceIntegerField idField = new DataSourceIntegerField(Field.OPERATION_JOB_ID, "ID");
        idField.setPrimaryKey(true);
        idField.setCanEdit(false);
        fields.add(idField);

        DataSourceTextField nameField = createTextField(Field.OPERATION_NAME, MSG.common_title_name(), 3, 100, true);
        fields.add(nameField);

        // TODO: i18n
        DataSourceTextField nextFireTimeField = createTextField(Field.OPERATION_NEXT_FIRE_TIME, "Next Fire Time", null,
            100, false);
        fields.add(nextFireTimeField);

        return fields;
    }

}
