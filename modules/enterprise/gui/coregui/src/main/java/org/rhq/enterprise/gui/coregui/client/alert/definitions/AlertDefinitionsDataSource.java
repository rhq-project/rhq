/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.alert.definitions;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.xml.bind.annotation.XmlTransient;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.types.FieldType;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertDampening;
import org.rhq.core.domain.alert.AlertDampeningEvent;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertDefinitionContext;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.alert.BooleanExpression;
import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.criteria.AlertDefinitionCriteria;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Greg Hinkle
 */
public class AlertDefinitionsDataSource extends RPCDataSource<AlertDefinition> {


    public AlertDefinitionsDataSource() {

        DataSourceIntegerField idField = new DataSourceIntegerField("id","ID");
        idField.setPrimaryKey(true);
        addField(idField);

        DataSourceTextField nameField = new DataSourceTextField("name","Name");
        addField(nameField);

        DataSourceTextField ctimeField = new DataSourceTextField("ctime","Created Time");
        ctimeField.setType(FieldType.DATETIME);
        addField(ctimeField);

        DataSourceTextField mtimeField = new DataSourceTextField("mtime", "Modified Time");
        mtimeField.setType(FieldType.DATETIME);
        addField(mtimeField);

        DataSourceTextField descriptionField = new DataSourceTextField("description","Description");
        addField(descriptionField);

        DataSourceTextField priorityField = new DataSourceTextField("priority","Priority");
        addField(priorityField);

        DataSourceTextField enabledField = new DataSourceTextField("enabled","Enabled");
        enabledField.setType(FieldType.BOOLEAN);
        addField(enabledField);
        
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response) {

        AlertDefinitionCriteria criteria = getCriteria(request);

        GWTServiceLookup.getAlertService().findAlertDefinitionsByCriteria(
                criteria,
                new AsyncCallback<PageList<AlertDefinition>>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to load alert definition data", caught);
                        response.setStatus(DSResponse.STATUS_FAILURE);
                        processResponse(request.getRequestId(), response);
                    }

                    public void onSuccess(PageList<AlertDefinition> result) {
                        response.setData(buildRecords(result));
                        processResponse(request.getRequestId(), response);
                    }
                }
        );
    }


    protected AlertDefinitionCriteria getCriteria(DSRequest request) {
        AlertDefinitionCriteria criteria = new AlertDefinitionCriteria();
        criteria.fetchConditions(true);

        Criteria requestCriteria = request.getCriteria();
        if (requestCriteria != null) {
            Map values = requestCriteria.getValues();
            for (Object key : values.keySet()) {
                String fieldName = (String) key;
                if (fieldName.equals("resourceId")) {
                    Integer resourceId = (Integer) values.get(fieldName);
                    criteria.addFilterResourceIds(resourceId);
                }
                // TODO: Add support for other fields we need to filter by (e.g. resourceGroupId).
            }
        }

        criteria.setPageControl(getPageControl(request));
        return criteria;
    }


    @Override
    public AlertDefinition copyValues(ListGridRecord from) {
        return null;  // TODO: Implement this method.
    }

    @Override
    public ListGridRecord copyValues(AlertDefinition from) {
        ListGridRecord record = new ListGridRecord();

        record.setAttribute("id", from.getId());
        record.setAttribute("name", from.getName());
        record.setAttribute("ctime", new Date(from.getCtime()));
        record.setAttribute("mtime", new Date(from.getMtime()));
        record.setAttribute("parentId", from.getParentId());
        record.setAttribute("description", from.getDescription());
        record.setAttribute("priority", from.getPriority().getDisplayName());
        record.setAttribute("enabled", from.getEnabled());

        record.setAttribute("recoveryId", from.getRecoveryId());
        record.setAttribute("willRecover", from.getWillRecover());
        record.setAttribute("notifyFiltered", from.getNotifyFiltered());
        record.setAttribute("controlFiltered", from.getControlFiltered());
        record.setAttribute("deleted", from.getDeleted());
        record.setAttribute("readOnly", from.isReadOnly());
        record.setAttribute("conditionExpression", from.getConditionExpression());


        return record;
    }


/*
The following is not yet translated into the record
    private AlertDefinition groupAlertDefinition;
    private Set<AlertDefinition> groupAlertDefinitionChildren = new LinkedHashSet<AlertDefinition>();
    private AlertDampening alertDampening;
    private BooleanExpression conditionExpression;
    private Set<AlertCondition> conditions = new LinkedHashSet<AlertCondition>(1); // Most alerts will only have one condition.
    private List<AlertNotification> alertNotifications = new ArrayList<AlertNotification>();
    private OperationDefinition operationDefinition;
    private Set<AlertDampeningEvent> alertDampeningEvents = new HashSet<AlertDampeningEvent>();
    private Set<Alert> alerts = new LinkedHashSet<Alert>();
*/

}
