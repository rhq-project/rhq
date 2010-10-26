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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.DSOperationType;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.criteria.RoleCriteria;
import org.rhq.core.domain.resource.group.LdapGroup;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.selection.ResourceGroupSelector;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 */
public class RoleEditView extends LocatableVLayout implements BookmarkableView {

    private Role role;

    private Label message = new Label("Loading...");

    private VLayout editCanvas;
    private DynamicForm form;

    private PermissionEditorView permissionEditorItem;

    private CanvasItem groupSelectorItem;
    private ResourceGroupSelector groupSelector;

    private CanvasItem subjectSelectorItem;
    private RoleSubjectSelector subjectSelector;

    private CanvasItem ldapGroupSelectorItem;
    private RoleLdapGroupSelector ldapGroupSelector;

    private RolesDataSource dataSource;

    public RoleEditView(String locatorId) {
        super(locatorId);

        this.dataSource = RolesDataSource.getInstance();

        this.setPadding(10);
        setOverflow(Overflow.AUTO);

        buildRoleEditor();
        this.editCanvas.hide();

        this.addMember(message);
        this.addMember(editCanvas);
    }

    private Canvas buildRoleEditor() {
        form = new LocatableDynamicForm(extendLocatorId(this.getLocatorId()));

        form.setHiliteRequiredFields(true);
        form.setRequiredTitleSuffix("* :");

        form.setDataSource(this.dataSource);
        form.setUseAllDataSourceFields(true);

        permissionEditorItem = new PermissionEditorView(this.getLocatorId(), "permissionsEditor", "Permissions");
        permissionEditorItem.setShowTitle(false);
        permissionEditorItem.setColSpan(2);

        groupSelectorItem = new CanvasItem("groupSelectionCanvas", "Assigned Resource Groups");
        groupSelectorItem.setTitleOrientation(TitleOrientation.TOP);
        groupSelectorItem.setColSpan(2);
        groupSelectorItem.setCanvas(new Canvas());

        subjectSelectorItem = new CanvasItem("subjectSelectionCanvas", "Assigned Subjects");
        subjectSelectorItem.setTitleOrientation(TitleOrientation.TOP);
        subjectSelectorItem.setColSpan(2);
        subjectSelectorItem.setCanvas(new Canvas());

        //instantiate ldap group selector
        ldapGroupSelectorItem = new CanvasItem("ldapGroupSelectionCanvas", "LDAP Groups");
        ldapGroupSelectorItem.setTitleOrientation(TitleOrientation.TOP);
        ldapGroupSelectorItem.setColSpan(2);
        ldapGroupSelectorItem.setCanvas(new Canvas());

        IButton saveButton = new LocatableIButton(this.extendLocatorId("Save"), "Save");
        saveButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
            public void onClick(com.smartgwt.client.widgets.events.ClickEvent clickEvent) {
                if (form.validate()) {
                    save();
                }
            }
        });

        IButton resetButton = new LocatableIButton(this.extendLocatorId("Reset"), "Reset");
        resetButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
            public void onClick(com.smartgwt.client.widgets.events.ClickEvent clickEvent) {
                form.reset();
            }
        });

        IButton cancelButton = new LocatableIButton(this.extendLocatorId("Cancel"), "Cancel");
        cancelButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
            public void onClick(com.smartgwt.client.widgets.events.ClickEvent clickEvent) {
                History.back();
            }
        });

        HLayout buttonLayout = new HLayout(10);
        buttonLayout.setAlign(Alignment.CENTER);
        buttonLayout.addMember(saveButton);
        buttonLayout.addMember(resetButton);
        buttonLayout.addMember(cancelButton);

        form.setItems(permissionEditorItem, groupSelectorItem, subjectSelectorItem, ldapGroupSelectorItem);

        this.editCanvas = new VLayout();

        editCanvas.addMember(form);
        editCanvas.addMember(buttonLayout);

        return editCanvas;
    }

    public void save() {
        final HashSet<Integer> groupSelection = this.groupSelector.getSelection();
        final HashSet<Integer> userSelection = this.subjectSelector.getSelection();
        final HashSet<String> ldapGroupSelection = this.ldapGroupSelector.getSelectionAlternateIds();

        // The form.saveData() call triggers either RolesDataSource.executeAdd() to create the new Role,
        // or executeUpdate() if saving changes to an existing Role. On success we need to perform the
        // subsequent user or group assignment, so set this callback on completion.         
        form.saveData(new DSCallback() {
            public void execute(DSResponse dsResponse, Object o, DSRequest dsRequest) {

                int roleId = Integer.parseInt(new ListGridRecord(dsRequest.getData()).getAttribute("id"));

                int[] groupIds = new int[groupSelection.size()];
                int i = 0;
                for (Integer id : groupSelection) {
                    groupIds[i++] = id;
                }

                GWTServiceLookup.getRoleService().setAssignedResourceGroups(roleId, groupIds,
                    new AsyncCallback<Void>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError("Failed to save role group assignments.", caught);
                        }

                        public void onSuccess(Void result) {
                            CoreGUI.getMessageCenter().notify(
                                new Message("Succesfully saved role group assignments.", Message.Severity.Info));
                        }
                    });

                int[] subjectIds = new int[userSelection.size()];
                i = 0;
                for (Integer id : userSelection) {
                    subjectIds[i++] = id;
                }

                GWTServiceLookup.getRoleService().setAssignedSubjects(roleId, subjectIds, new AsyncCallback<Void>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to save role user assignments.", caught);
                        History.back();
                    }

                    public void onSuccess(Void result) {
                        CoreGUI.getMessageCenter().notify(
                            new Message("Succesfully saved role user assignments.", Message.Severity.Info));
                        History.back();
                    }
                });

                List<String> selectedGroupList = new ArrayList<String>();
                for (String selection : ldapGroupSelection) {
                    selectedGroupList.add(selection);
                }
                if (!selectedGroupList.isEmpty()) {
                    GWTServiceLookup.getLdapService().setLdapGroupsForRole(roleId, selectedGroupList,
                        new AsyncCallback<Void>() {
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError("Failed to save role user assignments.", caught);
                            }

                            public void onSuccess(Void result) {
                                CoreGUI.getMessageCenter()
                                    .notify(
                                        new Message("Succesfully saved LDAP group role assignments.",
                                            Message.Severity.Info));
                            }
                        });
                }

            }
        });
    }

    @SuppressWarnings("unchecked")
    public void editRecord(Record record) {
        this.groupSelector = new RoleResourceGroupSelector(this.extendLocatorId("Groups"), (Set<ResourceGroup>) record
            .getAttributeAsObject("resourceGroups"));
        this.subjectSelector = new RoleSubjectSelector(this.extendLocatorId("Subjects"), (Set<Subject>) record
            .getAttributeAsObject("subjects"));
        this.ldapGroupSelector = new RoleLdapGroupSelector(this.extendLocatorId("LdapGroups"), record
            .getAttributeAsInt("id"));

        this.groupSelectorItem.setCanvas(this.groupSelector);
        this.subjectSelectorItem.setCanvas(this.subjectSelector);
        this.ldapGroupSelectorItem.setCanvas(this.ldapGroupSelector);

        Set<Permission> permissions = (Set<Permission>) record.getAttributeAsObject("permissions");
        this.permissionEditorItem.setPermissions(permissions);

        try {
            form.editRecord(record);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        message.hide();
        editCanvas.show();
        form.setSaveOperationType(DSOperationType.UPDATE);
    }

    private void editNewInternal() {
        role = new Role();
        ListGridRecord r = dataSource.copyValues(role);
        editRecord(r);

        // This tells form.saveData() to call RolesDataSource.executeAdd() on the new Role's ListGridRecord
        form.setSaveOperationType(DSOperationType.ADD);
    }

    public static void editNew(String locatorId) {
        RoleEditView editView = new RoleEditView(locatorId);
        editView.editNewInternal();
    }

    private void editRole(int roleId, final ViewId current) {
        final int id = Integer.valueOf(current.getBreadcrumbs().get(0).getName());

        if (id > 0) {
            RoleCriteria criteria = new RoleCriteria();
            criteria.addFilterId(id);
            criteria.fetchPermissions(true);
            criteria.fetchResourceGroups(true);
            criteria.fetchSubjects(true);

            GWTServiceLookup.getRoleService().findRolesByCriteria(criteria, new AsyncCallback<PageList<Role>>() {
                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Failed to load role for editing", caught);
                }

                @Override
                public void onSuccess(PageList<Role> result) {
                    final Role role = result.get(0);
                    final Record record = new RolesDataSource().copyValues(role);
                    //if ldap configured
                    GWTServiceLookup.getLdapService().checkLdapConfiguredStatus(new AsyncCallback<Boolean>() {
                        public void onSuccess(Boolean result) {
                            //get available ldap groups
                            GWTServiceLookup.getLdapService().findAvailableGroups(
                                new AsyncCallback<Set<Map<String, String>>>() {
                                    public void onFailure(Throwable caught) {
                                        CoreGUI.getErrorHandler().handleError(
                                            "Failed to retrieve available LDAP groups.", caught);
                                    }

                                    public void onSuccess(Set<Map<String, String>> availableLdapGroups) {
                                        //get assigned ldap groups
                                        Set<LdapGroup> availableGroups = RoleLdapGroupSelector
                                            .convertToCollection(availableLdapGroups);
                                        //update record with both objects.
                                        record.setAttribute("ldapGroupsAvailable", availableGroups);
                                        editRecord(record);
                                        current.getBreadcrumbs().get(0).setDisplayName("Editing: " + role.getName());
                                        CoreGUI.refreshBreadCrumbTrail();
                                    }
                                });
                        }

                        public void onFailure(Throwable caught) {//ldap not configured, proceed
                            editRecord(record);
                            current.getBreadcrumbs().get(0).setDisplayName("Editing: " + role.getName());
                            CoreGUI.refreshBreadCrumbTrail();
                        }
                    });
                }
            });
        } else {
            editNewInternal();
            current.getBreadcrumbs().get(0).setDisplayName("New Role");
            CoreGUI.refreshBreadCrumbTrail();
        }
    }

    @Override
    public void renderView(ViewPath viewPath) {
        int roleId = viewPath.getCurrentAsInt();

        editRole(roleId, viewPath.getCurrent());
    }
}
