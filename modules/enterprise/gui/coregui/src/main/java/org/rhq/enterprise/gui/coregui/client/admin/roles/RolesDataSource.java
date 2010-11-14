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
package org.rhq.enterprise.gui.coregui.client.admin.roles;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.criteria.RoleCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.admin.users.UsersDataSource;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.RoleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupsDataSource;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class RolesDataSource extends RPCDataSource<Role> {

    public static abstract class Field {
        public static final String ID = "id";
        public static final String NAME = "name";
        public static final String DESCRIPTION = "description";
        public static final String RESOURCE_GROUPS = "resourceGroups";
        public static final String GLOBAL_PERMISSIONS = "globalPermissions";
        public static final String RESOURCE_PERMISSIONS = "resourcePermissions";
        public static final String SUBJECTS = "subjects";
        public static final String LDAP_GROUPS = "ldapGroups";
    }

    private RoleGWTServiceAsync roleService = GWTServiceLookup.getRoleService();

    private static RolesDataSource INSTANCE;

    public static RolesDataSource getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new RolesDataSource();
        }
        return INSTANCE;
    }

    public RolesDataSource() {
        super();
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        DataSourceField idDataField = new DataSourceIntegerField(Field.ID, "ID");
        idDataField.setPrimaryKey(true);
        idDataField.setCanEdit(false);
        fields.add(idDataField);

        DataSourceTextField nameField = new DataSourceTextField(Field.NAME, "Name", 100, true);
        fields.add(nameField);

        DataSourceTextField descriptionField = new DataSourceTextField(Field.DESCRIPTION, "Description", 100, false);
        fields.add(descriptionField);

        return fields;
    }

    public void executeFetch(final DSRequest request, final DSResponse response) {
        RoleCriteria criteria = getFetchCriteria(request);

        roleService.findRolesByCriteria(criteria, new AsyncCallback<PageList<Role>>() {
            public void onFailure(Throwable caught) {
                sendFailureResponse(request, response, "Failed to fetch roles.", caught);
            }

            public void onSuccess(PageList<Role> result) {
                sendSuccessResponse(request, response, result);
            }
        });
    }

    @Override
    protected void executeAdd(Record recordToAdd, final DSRequest request, final DSResponse response) {
        Role newRole = copyValues(recordToAdd);

        roleService.createRole(newRole, new AsyncCallback<Role>() {
            public void onFailure(Throwable caught) {
                Map<String, String> errors = new HashMap<String, String>();
                errors.put("name", "A role with name already exists.");
                response.setErrors(errors);
                response.setStatus(RPCResponse.STATUS_VALIDATION_ERROR);
                processResponse(request.getRequestId(), response);
            }

            public void onSuccess(Role result) {
                CoreGUI.getMessageCenter().notify(
                    new Message("Role [" + result.getName() + "] added.", Message.Severity.Info));
                response.setData(new Record[] { copyValues(result) });
                processResponse(request.getRequestId(), response);
            }
        });

    }

    @Override
    protected void executeUpdate(Record editedRecord, Record oldRecord, final DSRequest request,
                                 final DSResponse response) {
        Role updatedRole = copyValues(editedRecord);
        
        roleService.updateRole(updatedRole, new AsyncCallback<Role>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to update role.", caught);
            }

            public void onSuccess(Role result) {
                CoreGUI.getMessageCenter().notify(
                    new Message("Role [" + result.getName() + "] updated.", Message.Severity.Info));
                response.setData(new Record[] { copyValues(result) });
                processResponse(request.getRequestId(), response);
            }
        });
    }

    @Override
    protected void executeRemove(final Record recordToRemove, final DSRequest request, final DSResponse response) {
        final Role deletedRole = copyValues(recordToRemove);

        final String rolename = deletedRole.getName();
        roleService.removeRoles(new int[] { deletedRole.getId() }, new AsyncCallback<Void>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to delete role [" + rolename + "].", caught);
            }

            public void onSuccess(Void result) {
                CoreGUI.getMessageCenter().notify(
                    new Message("Role [" + deletedRole.getName() + "] deleted..", Message.Severity.Info));
                response.setData(new Record[] {recordToRemove});
                processResponse(request.getRequestId(), response);
            }
        });

    }

    @SuppressWarnings("unchecked")
    public Role copyValues(Record from) {
        Role to = new Role();

        to.setId(from.getAttributeAsInt(Field.ID));
        to.setName(from.getAttributeAsString(Field.NAME));
        to.setDescription(from.getAttributeAsString(Field.DESCRIPTION));

        // TODO
        /*to.setResourceGroups((Set<ResourceGroup>) from.getAttributeAsObject(Field.RESOURCE_GROUPS));
        to.setPermissions((Set<Permission>) from.getAttributeAsObject(Field.GLOBAL_PERMISSIONS));
        to.setSubjects((Set<Subject>) from.getAttributeAsObject(Field.SUBJECTS));
        to.setSubjects((Set<Subject>) from.getAttributeAsObject(Field.LDAP_GROUPS));*/

        return to;
    }

    public ListGridRecord copyValues(Role sourceRole) {
        ListGridRecord targetRecord = new ListGridRecord();

        targetRecord.setAttribute(Field.ID, sourceRole.getId());
        targetRecord.setAttribute(Field.NAME, sourceRole.getName());
        targetRecord.setAttribute(Field.DESCRIPTION, sourceRole.getDescription());

        ListGridRecord[] resourceGroupRecords = ResourceGroupsDataSource.getInstance().buildRecords(sourceRole.getResourceGroups());
        targetRecord.setAttribute(Field.RESOURCE_GROUPS, resourceGroupRecords);

        // First split the set of permissions into two subsets - one for global perms and one for resource perms.
        Set<Permission> permissions = sourceRole.getPermissions();
        Set<Permission> globalPermissions = new HashSet<Permission>();
        Set<Permission> resourcePermissions = new HashSet<Permission>();
        for (Permission permission : permissions) {
            if (permission.getTarget() == Permission.Target.GLOBAL) {
                globalPermissions.add(permission);
            } else {
                resourcePermissions.add(permission);
            }
        }

        ListGridRecord[] globalPermissionRecords = toRecordArray(globalPermissions);
        targetRecord.setAttribute(Field.GLOBAL_PERMISSIONS, globalPermissionRecords);

        ListGridRecord[] resourcePermissionRecords = toRecordArray(resourcePermissions);
        targetRecord.setAttribute(Field.RESOURCE_PERMISSIONS, resourcePermissionRecords);

        ListGridRecord[] subjectRecords = UsersDataSource.getInstance().buildRecords(sourceRole.getSubjects());
        targetRecord.setAttribute(Field.SUBJECTS, subjectRecords);

        return targetRecord;
    }

    private static ListGridRecord[] toRecordArray(Set<Permission> globalPermissions) {
        ListGridRecord[] globalPermissionRecords = new ListGridRecord[globalPermissions.size()];
        for (Permission permission : globalPermissions) {
            ListGridRecord globalPermissionRecord = new ListGridRecord();
            globalPermissionRecord.setAttribute("name", permission.name());
            globalPermissionRecord.setAttribute("displayName", permission.name()); // TODO
            globalPermissionRecord.setAttribute("description", ""); // TODO
        }
        return globalPermissionRecords;
    }

    private RoleCriteria getFetchCriteria(DSRequest request) {
        RoleCriteria criteria = new RoleCriteria();
        criteria.setPageControl(getPageControl(request));

        Integer subjectId = request.getCriteria().getAttributeAsInt("subjectId");
        if (subjectId != null) {
            criteria.addFilterSubjectId(subjectId);
        }

        criteria.fetchResourceGroups(true);
        criteria.fetchPermissions(true);
        criteria.fetchSubjects(true);
        // TODO: Uncomment this.
        //criteria.fetchLdapGroups(true);

        return criteria;
    }
}
