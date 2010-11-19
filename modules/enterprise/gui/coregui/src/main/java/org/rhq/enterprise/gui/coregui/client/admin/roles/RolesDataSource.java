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
import com.smartgwt.client.types.FieldType;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.criteria.RoleCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoadedListener;
import org.rhq.enterprise.gui.coregui.client.UserPermissionsManager;
import org.rhq.enterprise.gui.coregui.client.admin.users.UsersDataSource;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.RoleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupsDataSource;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * A DataSource for RHQ {@link Role role}s.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class RolesDataSource extends RPCDataSource<Role> {

    public static abstract class Field {
        public static final String ID = "id";
        public static final String NAME = "name";
        public static final String DESCRIPTION = "description";
        public static final String RESOURCE_GROUPS = "resourceGroups";
        public static final String PERMISSIONS = "permissions";
        public static final String SUBJECTS = "subjects";
        public static final String LDAP_GROUPS = "ldapGroups";
    }

    public static abstract class CriteriaField {
        public static final String SUBJECT_ID = "subjectId";
    }

    public static final int ID_SUPERUSER = 1;
    public static final int ID_ALL_RESOURCES = 2;

    private static RolesDataSource INSTANCE;

    private RoleGWTServiceAsync roleService = GWTServiceLookup.getRoleService();
    private Set<Permission> globalPermissions;

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

        UserPermissionsManager.getInstance().loadGlobalPermissions(new PermissionsLoadedListener() {
            public void onPermissionsLoaded(Set<Permission> permissions) {
                RolesDataSource.this.globalPermissions = permissions;
            }
        });
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        DataSourceIntegerField idDataField = new DataSourceIntegerField(Field.ID, "ID");
        idDataField.setPrimaryKey(true);
        idDataField.setCanEdit(false);
        fields.add(idDataField);

        DataSourceTextField nameField = createTextField(Field.NAME, MSG.common_title_name(), 3, 100, true);
        fields.add(nameField);

        DataSourceTextField descriptionField = createTextField(Field.DESCRIPTION, MSG.common_title_description(), null,
            100, false);
        fields.add(descriptionField);

        DataSourceField resourceGroupsField = new DataSourceField(Field.RESOURCE_GROUPS, FieldType.ANY, "Resource Groups");
        fields.add(resourceGroupsField);

        DataSourceField permissionsField = new DataSourceField(Field.PERMISSIONS, FieldType.ANY, "Permissions");
        //fields.add(permissionsField);

        DataSourceField subjectsField = new DataSourceField(Field.SUBJECTS, FieldType.ANY, "Subjects");
        fields.add(subjectsField);

        DataSourceField ldapGroupsField = new DataSourceField(Field.LDAP_GROUPS, FieldType.ANY, "LDAP Groups");
        fields.add(ldapGroupsField);

        return fields;
    }

    public void executeFetch(final DSRequest request, final DSResponse response) {
        RoleCriteria criteria = getFetchCriteria(request);

        roleService.findRolesByCriteria(criteria, new AsyncCallback<PageList<Role>>() {
            public void onFailure(Throwable caught) {
                sendFailureResponse(request, response, MSG.view_adminRoles_failRoles(), caught);
            }

            public void onSuccess(PageList<Role> result) {
                sendSuccessResponse(request, response, result);
            }
        });
    }

    @Override
    protected void executeAdd(Record recordToAdd, final DSRequest request, final DSResponse response) {
        Role roleToAdd = copyValues(recordToAdd);

        final String rolename = roleToAdd.getName();
        roleService.createRole(roleToAdd, new AsyncCallback<Role>() {
            public void onFailure(Throwable caught) {
                Map<String, String> errorMessages = new HashMap<String, String>();
                errorMessages.put(Field.NAME, MSG.view_adminRoles_roleExists(rolename));
                sendValidationErrorResponse(request, response, errorMessages);
            }

            public void onSuccess(Role addedRole) {
                sendSuccessResponse(request, response, addedRole, new Message(MSG.view_adminRoles_roleAdded(rolename)));
            }
        });

    }

    @Override
    protected void executeUpdate(Record recordToUpdate, Record oldRecord, final DSRequest request,
        final DSResponse response) {
        Role roleToUpdate = copyValues(recordToUpdate);

        final String rolename = roleToUpdate.getName();
        roleService.updateRole(roleToUpdate, new AsyncCallback<Role>() {
            public void onFailure(Throwable caught) {
                sendFailureResponse(request, response, MSG.view_adminRoles_roleUpdateFailed(rolename), caught);
            }

            public void onSuccess(Role updatedRole) {
                sendSuccessResponse(request, response, updatedRole, new Message(MSG
                    .view_adminRoles_roleUpdated(rolename)));
            }
        });
    }

    @Override
    protected void executeRemove(final Record recordToRemove, final DSRequest request, final DSResponse response) {
        final Role roleToRemove = copyValues(recordToRemove);

        final String rolename = roleToRemove.getName();
        roleService.removeRoles(new int[] { roleToRemove.getId() }, new AsyncCallback<Void>() {
            public void onFailure(Throwable caught) {
                sendFailureResponse(request, response, MSG.view_adminRoles_roleDeleteFailed(rolename), caught);
            }

            public void onSuccess(Void result) {
                sendSuccessResponse(request, response, roleToRemove, new Message(MSG
                    .view_adminRoles_roleDeleted(rolename)));
            }
        });

    }

    @SuppressWarnings("unchecked")
    public Role copyValues(Record from) {
        Role to = new Role();

        to.setId(from.getAttributeAsInt(Field.ID));
        to.setName(from.getAttributeAsString(Field.NAME));
        to.setDescription(from.getAttributeAsString(Field.DESCRIPTION));

        Record[] permissionRecords = from.getAttributeAsRecordArray(Field.PERMISSIONS);
        Set<Permission> permissions = toPermissionSet(permissionRecords);
        to.setPermissions(permissions);

        // TODO
        /*to.setResourceGroups((Set<ResourceGroup>) from.getAttributeAsObject(Field.RESOURCE_GROUPS));
        to.setSubjects((Set<Subject>) from.getAttributeAsObject(Field.SUBJECTS));
        to.setSubjects((Set<Subject>) from.getAttributeAsObject(Field.LDAP_GROUPS));*/

        return to;
    }

    public ListGridRecord copyValues(Role sourceRole) {
        ListGridRecord targetRecord = new ListGridRecord();

        targetRecord.setAttribute(Field.ID, sourceRole.getId());
        targetRecord.setAttribute(Field.NAME, sourceRole.getName());
        targetRecord.setAttribute(Field.DESCRIPTION, sourceRole.getDescription());

        ListGridRecord[] resourceGroupRecords = ResourceGroupsDataSource.getInstance().buildRecords(
            sourceRole.getResourceGroups());
        targetRecord.setAttribute(Field.RESOURCE_GROUPS, resourceGroupRecords);

        Set<Permission> permissions = sourceRole.getPermissions();
        ListGridRecord[] permissionRecords = toRecordArray(permissions);
        targetRecord.setAttribute(Field.PERMISSIONS, permissionRecords);

        ListGridRecord[] subjectRecords = UsersDataSource.getInstance().buildRecords(sourceRole.getSubjects());
        targetRecord.setAttribute(Field.SUBJECTS, subjectRecords);

        return targetRecord;
    }

    public static Set<Permission> toPermissionSet(Record[] permissionRecords) {
        Set<Permission> permissions = new HashSet<Permission>();
        for (Record permissionRecord : permissionRecords) {
            String permissionName = permissionRecord.getAttribute("name");
            Permission permission = Permission.valueOf(permissionName);
            permissions.add(permission);
        }
        return permissions;
    }

    public static ListGridRecord[] toRecordArray(Set<Permission> permissions) {
        ListGridRecord[] permissionRecords = new ListGridRecord[permissions.size()];
        int index = 0;
        for (Permission permission : permissions) {
            ListGridRecord permissionRecord = new ListGridRecord();
            permissionRecord.setAttribute("name", permission.name());
            permissionRecords[index++] = permissionRecord;
        }
        return permissionRecords;
    }

    private RoleCriteria getFetchCriteria(DSRequest request) {
        RoleCriteria criteria = new RoleCriteria();

        // Pagination
        criteria.setPageControl(getPageControl(request));

        // Filtering
        Integer id = getFilter(request, Field.ID, Integer.class);
        criteria.addFilterId(id);

        Integer subjectId = request.getCriteria().getAttributeAsInt(CriteriaField.SUBJECT_ID);
        if (subjectId != null) {
            criteria.addFilterSubjectId(subjectId);
        }

        // Fetching
        criteria.fetchPermissions(true);
        if (this.globalPermissions.contains(Permission.MANAGE_SECURITY)) {
            criteria.fetchSubjects(true);
            criteria.fetchResourceGroups(true);
        }

        return criteria;
    }
    
}
