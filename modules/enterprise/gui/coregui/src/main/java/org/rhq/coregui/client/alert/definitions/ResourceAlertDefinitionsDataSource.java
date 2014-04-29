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

package org.rhq.coregui.client.alert.definitions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.fields.DataSourceLinkField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.criteria.AlertDefinitionCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.admin.templates.AlertDefinitionTemplateTypeView;
import org.rhq.coregui.client.inventory.resource.AncestryUtil;

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
    public ArrayList<ListGridField> getListGridFields() {
        ArrayList<ListGridField> fields = super.getListGridFields();

        // add two more columns
        ListGridField parentIdField = new ListGridField(FIELD_PARENT, MSG.view_alerts_field_parent());
        parentIdField.setType(ListGridFieldType.LINK);
        parentIdField.setTarget("_self");
        parentIdField.setWidth(100);
        fields.add(parentIdField);

        ListGridField readOnlyField = new ListGridField(FIELD_READONLY, MSG.view_alerts_field_protected());
        readOnlyField.setWidth(60);
        readOnlyField.setAlign(Alignment.CENTER);
        fields.add(readOnlyField);

        return fields;
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
            record.setAttribute(FIELD_READONLY, MSG.common_val_na());
        } else {
            if (parentId != null && parentId.intValue() != 0) {
                record.setAttribute(
                    FIELD_PARENT,
                    LinkManager.getAdminTemplatesEditLink(AlertDefinitionTemplateTypeView.VIEW_ID.getName(),
                        this.resource.getResourceType().getId()) + "/" + parentId);
                record.setLinkText(MSG.view_alert_definition_for_type());
            } else {
                boolean isAutogroup = groupAlertDefinition.getGroup().getAutoGroupParentResource() != null;
                if (isAutogroup) {
                    record.setAttribute(FIELD_PARENT, "#Resource/AutoGroup/"
                        + groupAlertDefinition.getGroup().getId() + "/Alerts/Definitions/" + groupAlertDefinition.getId()); 
                }
                else {
                    boolean isAutoCluster = groupAlertDefinition.getGroup().getClusterResourceGroup() != null;
                    if (isAutoCluster) {
                        record.setAttribute(FIELD_PARENT,  "#ResourceGroup/AutoCluster/"
                            + groupAlertDefinition.getGroup().getId() + "/Alerts/Definitions/" + groupAlertDefinition.getId());
                    }
                    else {
                        record.setAttribute(FIELD_PARENT, "#ResourceGroup/"
                            + groupAlertDefinition.getGroup().getId() + "/Alerts/Definitions/" + groupAlertDefinition.getId()); 
                    }
                }
                record.setLinkText(MSG.view_alert_definition_for_group());
            }
            record.setAttribute(FIELD_READONLY, (readOnly) ? MSG.common_val_yes() : MSG.common_val_no());
        }

        return record;
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        // add two more columns
        DataSourceLinkField parentIdField = new DataSourceLinkField(FIELD_PARENT, MSG.view_alerts_field_parent());
        fields.add(parentIdField);

        DataSourceTextField readOnlyField = new DataSourceTextField(FIELD_READONLY, MSG.view_alerts_field_protected());
        fields.add(readOnlyField);

        return fields;
    }

    @Override
    protected AlertDefinitionCriteria getFetchCriteria(DSRequest request) {
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

        return criteria;
    }

    @Override
    protected String getSortFieldForColumn(String columnName) {
        if (AncestryUtil.RESOURCE_ANCESTRY.equals(columnName)) {
            return "resource.ancestry";
        }
        if (FIELD_PARENT.equals(columnName)) {
            return "parentId";
        }

        return super.getSortFieldForColumn(columnName);
    }

    @Override
    protected AlertDefinitionCriteria getSimpleCriteriaForAll() {
        AlertDefinitionCriteria criteria = new AlertDefinitionCriteria();
        criteria.addFilterResourceIds(Integer.valueOf(this.resource.getId()));
        criteria.setPageControl(PageControl.getUnlimitedInstance());
        return criteria;
    }
}
