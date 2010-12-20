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

import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;

import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.form.AbstractRecordEditor;
import org.rhq.enterprise.gui.coregui.client.components.form.EnhancedDynamicForm;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.schedule.ResourceOperationScheduleDataSource;

/**
 * @author Ian Springer
 */
public class ResourceOperationScheduleDetailsView extends AbstractRecordEditor {

    private static final String FIELD_OPERATION_DESCRIPTION = "operationDescription";

    private ResourceComposite resourceComposite;
    private Map<String, String> operationNameToDescriptionMap = new HashMap<String, String>();
    private SelectItem operationNameItem;
    private StaticTextItem operationDescriptionItem;

    public ResourceOperationScheduleDetailsView(String locatorId, ResourceComposite resourceComposite, int scheduleId) {
        super(locatorId, new ResourceOperationScheduleDataSource(resourceComposite), scheduleId, "Scheduled Operation", null);
        this.resourceComposite = resourceComposite;
        ResourceType resourceType = this.resourceComposite.getResource().getResourceType();
        Set<OperationDefinition> operationDefinitions = resourceType.getOperationDefinitions();
        for (OperationDefinition operationDefinition : operationDefinitions) {
            this.operationNameToDescriptionMap.put(operationDefinition.getName(), operationDefinition.getDescription());
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
                updateOperationDescriptionItem();
            }
        });

        this.operationDescriptionItem = new StaticTextItem(FIELD_OPERATION_DESCRIPTION, "Operation Description");
        items.add(this.operationDescriptionItem);

        TextAreaItem notesItem = new TextAreaItem(ResourceOperationScheduleDataSource.Field.DESCRIPTION, "Notes");
        notesItem.setColSpan(form.getNumCols());
        items.add(notesItem);

        return items;
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        updateOperationDescriptionItem();
    }

    @Override
    protected String getTitleFieldName() {
        return ResourceOperationScheduleDataSource.Field.OPERATION_DISPLAY_NAME;
    }

    private void updateOperationDescriptionItem() {
        String operationName = this.operationNameItem.getValueAsString();
        String value;
        if (operationName == null) {
            value = "<i>Select an operation to view its description.</i>";
        } else {
            value = this.operationNameToDescriptionMap.get(operationName);
        }
        this.operationDescriptionItem.setValue(value);
    }

}
