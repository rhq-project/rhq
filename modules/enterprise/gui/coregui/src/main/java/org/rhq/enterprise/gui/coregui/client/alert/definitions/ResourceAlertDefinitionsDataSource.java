/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.enterprise.gui.coregui.client.alert.definitions;

import java.util.Map;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.fields.DataSourceLinkField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.criteria.AlertDefinitionCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;

/**
 * @author John Mazzitelli
 */
public class ResourceAlertDefinitionsDataSource extends AbstractAlertDefinitionsDataSource {

    protected static final String FIELD_PARENT = "parent"; // may be template or group alert def parent
    protected static final String FIELD_READONLY = "readOnly"; // not necessarily the actual boolean; sometimes we display "N/A"

    private Resource resource;

    public ResourceAlertDefinitionsDataSource(Resource resource) {
        super();
        this.resource = resource;
    }

    @Override
    public ListGridRecord copyValues(AlertDefinition from) {
        ListGridRecord record = super.copyValues(from);

        Integer parentId = from.getParentId(); // a valid non-zero number means the alert def came from a template
        AlertDefinition groupAlertDefinition = from.getGroupAlertDefinition();
        boolean readOnly = from.isReadOnly();

        if ((parentId == null || parentId.intValue() == 0) && (groupAlertDefinition == null)) {
            record.setAttribute(FIELD_PARENT, "");
            record.setLinkText("");
            record.setAttribute(FIELD_READONLY, "N/A");
        } else {
            // TODO: fix the URLs so they point to the new GWT pages when they are implemented
            if (parentId != null && parentId.intValue() != 0) {
                record.setAttribute(FIELD_PARENT, "/alerts/Config.do?mode=viewRoles&type="
                    + this.resource.getResourceType().getId() + "&from=" + from.getId() + "&ad=" + parentId);
                record.setLinkText("View Template");
            } else {
                record.setAttribute(FIELD_PARENT, "#ResourceGroup/" + groupAlertDefinition.getResourceGroup().getId()
                    + "/Alerts/Definitions/" + groupAlertDefinition.getId());
                record.setLinkText("View Group Definition");
            }
            record.setAttribute(FIELD_READONLY, readOnly);
        }

        return record;
    }

    @Override
    protected void setupFields() {
        super.setupFields();

        // add two more columns
        DataSourceLinkField parentIdField = new DataSourceLinkField(FIELD_PARENT, "Parent");
        addField(parentIdField);

        DataSourceTextField readOnlyField = new DataSourceTextField(FIELD_READONLY, "Read Only");
        addField(readOnlyField);
    }

    @Override
    protected AlertDefinitionCriteria getCriteria(DSRequest request) {
        AlertDefinitionCriteria criteria = new AlertDefinitionCriteria();

        criteria.fetchGroupAlertDefinition(true);
        criteria.fetchConditions(true);
        criteria.fetchAlertNotifications(true);

        Criteria requestCriteria = request.getCriteria();
        if (requestCriteria != null) {
            Map values = requestCriteria.getValues();
            for (Object key : values.keySet()) {
                String fieldName = (String) key;
                if (fieldName.equals(ResourceAlertDefinitionsView.CRITERIA_RESOURCE_ID)) {
                    Integer resourceId = (Integer) values.get(fieldName);
                    criteria.addFilterResourceIds(resourceId);
                }
            }
        }

        criteria.setPageControl(getPageControl(request));
        return criteria;
    }

    @Override
    protected AlertDefinitionCriteria getSimpleCriteriaForAll() {
        AlertDefinitionCriteria criteria = new AlertDefinitionCriteria();
        criteria.addFilterResourceIds(Integer.valueOf(this.resource.getId()));
        criteria.setPageControl(PageControl.getUnlimitedInstance());
        return criteria;
    }
}
