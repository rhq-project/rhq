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
import java.util.List;

import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;

import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.form.AbstractRecordEditor;
import org.rhq.enterprise.gui.coregui.client.components.form.EnhancedDynamicForm;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.schedule.ResourceOperationScheduleDataSource;

/**
 * @author Ian Springer
 */
public class ResourceOperationScheduleDetailsView extends AbstractRecordEditor {

    public ResourceOperationScheduleDetailsView(String locatorId, ResourceComposite resourceComposite, int scheduleId) {
        super(locatorId, new ResourceOperationScheduleDataSource(resourceComposite), scheduleId, "Scheduled Operation", null);
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

        SelectItem operationNameItem = new SelectItem(ResourceOperationScheduleDataSource.Field.OPERATION_NAME);
        items.add(operationNameItem);

        TextAreaItem descriptionItem = new TextAreaItem(ResourceOperationScheduleDataSource.Field.DESCRIPTION);
        descriptionItem.setColSpan(form.getNumCols());
        items.add(descriptionItem);

        return items;
    }

    @Override
    protected String getTitleFieldName() {
        return ResourceOperationScheduleDataSource.Field.OPERATION_DISPLAY_NAME;
    }
    
}
