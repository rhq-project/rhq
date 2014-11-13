/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.coregui.client.inventory.resource.detail;

import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;

import org.rhq.core.domain.resource.CreateResourceHistory;
import org.rhq.core.domain.resource.DeleteResourceHistory;
import org.rhq.core.domain.util.ResourceTypeUtility;
import org.rhq.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * Details view for a single child history entity.
 * 
 * @author John Mazzitelli
 */
public class ChildHistoryDetails extends EnhancedVLayout {
    private CreateResourceHistory createHistory;
    private DeleteResourceHistory deleteHistory;

    public ChildHistoryDetails(CreateResourceHistory history) {
        super();
        createHistory = history;
        deleteHistory = null;
    }

    public ChildHistoryDetails(DeleteResourceHistory history) {
        super();
        createHistory = null;
        deleteHistory = history;
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        DynamicForm form = null;
        if (createHistory != null) {
            form = buildForCreate(createHistory);
        } else if (deleteHistory != null) {
            form = buildForDelete(deleteHistory);
        }
        addMember(form);
    }

    private DynamicForm buildForCreate(CreateResourceHistory history) {
        DynamicForm form = new DynamicForm();
        form.setWidth100();
        form.setHeight100();
        form.setWrapItemTitles(false);

        StaticTextItem id = new StaticTextItem("id", MSG.common_title_id());
        id.setValue(history.getId());

        StaticTextItem type = new StaticTextItem("type", MSG.common_title_type());
        String typeValue = Canvas.imgHTML(ChildHistoryView.CHILD_CREATED_ICON);
        typeValue += MSG.view_resource_inventory_childhistory_createdChild();
        type.setValue(typeValue);

        StaticTextItem createdTimestamp = new StaticTextItem("created", MSG.common_title_dateCreated());
        createdTimestamp.setValue(TimestampCellFormatter.format(history.getCreatedDate(),
            TimestampCellFormatter.DATE_TIME_FORMAT_FULL));

        StaticTextItem modifiedTimestamp = new StaticTextItem("created", MSG.common_title_lastUpdated());
        modifiedTimestamp.setValue(TimestampCellFormatter.format(history.getLastModifiedDate(),
            TimestampCellFormatter.DATE_TIME_FORMAT_FULL));

        StaticTextItem subject = new StaticTextItem("subject", MSG.common_title_user());
        subject.setValue(history.getSubjectName());

        StaticTextItem status = new StaticTextItem("status", MSG.common_title_status());
        switch (history.getStatus()) {
        case SUCCESS:
            status.setValue(MSG.common_status_success());
            break;
        case FAILURE:
            status.setValue(MSG.common_status_failed());
            break;
        case IN_PROGRESS:
            status.setValue(MSG.common_status_inprogress());
            break;
        case INVALID_ARTIFACT:
            status.setValue(MSG.view_resource_inventory_childhistory_status_invalidArtifact());
            break;
        case INVALID_CONFIGURATION:
            status.setValue(MSG.view_resource_inventory_childhistory_status_invalidConfig());
            break;
        case TIMED_OUT:
            status.setValue(MSG.common_status_timedOut());
            break;
        default:
            status.setValue("?");
        }

        StaticTextItem createdResourceName = new StaticTextItem("createdResourceName", MSG.common_title_resource_name());
        createdResourceName.setValue(history.getCreatedResourceName());

        StaticTextItem createdResourceKey = new StaticTextItem("createdResourceKey", MSG.common_title_resource_key());
        createdResourceKey.setValue(history.getNewResourceKey());

        StaticTextItem createdResourceType = new StaticTextItem("createdResourceType", MSG.common_title_resource_type());
        if (history.getResourceType() != null) {
            createdResourceType.setValue(ResourceTypeUtility.displayName(history.getResourceType()));
        } else {
            createdResourceType.setValue(MSG.common_status_unknown());
        }

        TextAreaItem errorMessage = new TextAreaItem("errorMessage", MSG.common_severity_error());
        errorMessage.setValue(history.getErrorMessage());
        errorMessage.setTitleOrientation(TitleOrientation.TOP);
        errorMessage.setColSpan(2);
        errorMessage.setWidth("100%");
        errorMessage.setHeight("100%");

        if (history.getErrorMessage() != null && history.getErrorMessage().length() > 0) {
            form.setItems(id, type, createdTimestamp, modifiedTimestamp, subject, createdResourceName,
                createdResourceKey, createdResourceType, status, errorMessage);
        } else {
            form.setItems(id, type, createdTimestamp, modifiedTimestamp, subject, createdResourceName,
                createdResourceKey, createdResourceType, status);
        }

        return form;
    }

    private DynamicForm buildForDelete(DeleteResourceHistory history) {
        DynamicForm form = new DynamicForm();
        form.setWidth100();
        form.setHeight100();
        form.setWrapItemTitles(false);

        StaticTextItem id = new StaticTextItem("id", MSG.common_title_id());
        id.setValue(history.getId());

        StaticTextItem type = new StaticTextItem("type", MSG.common_title_type());
        String typeValue = Canvas.imgHTML(ChildHistoryView.CHILD_DELETED_ICON);
        typeValue += MSG.view_resource_inventory_childhistory_deletedChild();
        type.setValue(typeValue);

        StaticTextItem createdTimestamp = new StaticTextItem("created", MSG.common_title_dateCreated());
        createdTimestamp.setValue(TimestampCellFormatter.format(history.getCreatedDate(),
            TimestampCellFormatter.DATE_TIME_FORMAT_FULL));

        StaticTextItem modifiedTimestamp = new StaticTextItem("created", MSG.common_title_lastUpdated());
        modifiedTimestamp.setValue(TimestampCellFormatter.format(history.getLastModifiedDate(),
            TimestampCellFormatter.DATE_TIME_FORMAT_FULL));

        StaticTextItem subject = new StaticTextItem("subject", MSG.common_title_user());
        subject.setValue(history.getSubjectName());

        StaticTextItem status = new StaticTextItem("status", MSG.common_title_status());
        switch (history.getStatus()) {
        case SUCCESS:
            status.setValue(MSG.common_status_success());
            break;
        case FAILURE:
            status.setValue(MSG.common_status_failed());
            break;
        case IN_PROGRESS:
            status.setValue(MSG.common_status_inprogress());
            break;
        case TIMED_OUT:
            status.setValue(MSG.common_status_timedOut());
            break;
        default:
            status.setValue("?");
        }

        StaticTextItem deletedResourceName = new StaticTextItem("deletedResourceName", MSG.common_title_resource_name());
        StaticTextItem deletedResourceType = new StaticTextItem("deletedResourceType", MSG.common_title_resource_type());

        if (history.getResource() != null) {
            deletedResourceName.setValue(history.getResource().getName());
            if (history.getResource().getResourceType() != null) {
                deletedResourceType.setValue(ResourceTypeUtility.displayName(history.getResource().getResourceType()));
            } else {
                deletedResourceType.setValue(MSG.common_status_unknown());
            }
        } else {
            deletedResourceName.setValue(MSG.common_status_unknown());
            deletedResourceType.setValue(MSG.common_status_unknown());
        }

        TextAreaItem errorMessage = new TextAreaItem("errorMessage", MSG.common_severity_error());
        errorMessage.setValue(history.getErrorMessage());
        errorMessage.setTitleOrientation(TitleOrientation.TOP);
        errorMessage.setColSpan(2);
        errorMessage.setWidth("100%");
        errorMessage.setHeight("100%");

        if (history.getErrorMessage() != null && history.getErrorMessage().length() > 0) {
            form.setItems(id, type, createdTimestamp, modifiedTimestamp, subject, deletedResourceName,
                deletedResourceType, status, errorMessage);
        } else {
            form.setItems(id, type, createdTimestamp, modifiedTimestamp, subject, deletedResourceName,
                deletedResourceType, status);
        }

        return form;
    }
}
