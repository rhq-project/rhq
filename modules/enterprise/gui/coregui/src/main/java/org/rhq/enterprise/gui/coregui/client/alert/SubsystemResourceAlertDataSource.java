/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.alert;

import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.fields.DataSourceIntegerField;

import org.rhq.core.domain.criteria.AlertCriteria;

/**
 * @author Ian Springer
 */
public class SubsystemResourceAlertDataSource extends AlertDataSource {
    @Override
    protected void onInit() {
        super.onInit();

        DataSourceField[] fields = getFields();
        DataSourceField[] updatedFields = new DataSourceField[fields.length + 1];

        // TODO: Replace 'Resource Id' column with 'Resource Name' and 'Resource Lineage' columns.
        DataSourceField resourceIdField = new DataSourceIntegerField(AlertCriteria.SORT_FIELD_RESOURCE_ID, MSG
            .common_title_resource_id());
        updatedFields[0] = resourceIdField;

        System.arraycopy(fields, 0, updatedFields, 1, fields.length);
    }
}
