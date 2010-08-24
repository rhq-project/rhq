/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.traits;

import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import org.rhq.core.domain.criteria.MeasurementDataTraitCriteria;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMeasurementDataTraitDataSource;

import java.util.List;

/**
 * A DataSource for reading traits for the current Resource.
 *
 * @author Ian Springer
 */
public class TraitsDataSource extends AbstractMeasurementDataTraitDataSource {
    private int resourceId;

    public TraitsDataSource(int resourceId) {
        this.resourceId = resourceId;
    }

    @Override
    protected List<DataSourceField> createFields() {
        List<DataSourceField> fields = super.createFields();
        DataSourceField resourceIdField = new DataSourceIntegerField(MeasurementDataTraitCriteria.FILTER_FIELD_RESOURCE_ID,
                "Resource Id");
        resourceIdField.setHidden(true);
        fields.add(resourceIdField);
        return fields;
    }

    @Override
    public ListGridRecord copyValues(MeasurementDataTrait from) {
        ListGridRecord record = super.copyValues(from);
        record.setAttribute(MeasurementDataTraitCriteria.FILTER_FIELD_RESOURCE_ID, this.resourceId);
        return record;
    }
}
