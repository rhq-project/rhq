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
package org.rhq.enterprise.gui.coregui.client.admin.templates;

import com.smartgwt.client.data.Criteria;

import org.rhq.core.domain.criteria.MeasurementScheduleCriteria;
import org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMeasurementScheduleListView;

/**
 * A view for viewing and updating the default metric schedules ("metric templates") for a particular ResourceType.
 *
 * @author Ian Springer
 */
public class TemplateSchedulesView extends AbstractMeasurementScheduleListView {
    private static final String TITLE = "Template Metric Collection Schedules";

    private static final String[] EXCLUDED_FIELD_NAMES = new String[] { MeasurementScheduleCriteria.FILTER_FIELD_RESOURCE_TYPE_ID };

    private boolean updateExistingSchedules = true;

    public TemplateSchedulesView(String locatorId, int resourceTypeId) {
        super(locatorId, TITLE, new TemplateSchedulesDataSource(resourceTypeId), createCriteria(resourceTypeId),
            EXCLUDED_FIELD_NAMES);
    }

    private static Criteria createCriteria(int resourceTypeId) {
        Criteria criteria = new Criteria();
        criteria.addCriteria(MeasurementScheduleCriteria.FILTER_FIELD_RESOURCE_TYPE_ID, resourceTypeId);
        return criteria;
    }

    @Override
    protected void configureTable() {
        super.configureTable();
        addExtraWidget(new UpdateExistingSchedulesWidget(this));
    }

    public boolean isUpdateExistingSchedules() {
        return updateExistingSchedules;
    }

    public void setUpdateExistingSchedules(boolean updateExistingSchedules) {
        this.updateExistingSchedules = updateExistingSchedules;
    }
}
