/*
 * RHQ Management Platform
 * Copyright (C) 2009-2010 Red Hat, Inc.
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
package org.rhq.sample.perspective;

import java.util.HashMap;
import java.util.Map;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.international.StatusMessage;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.client.RemoteClient;
import org.rhq.enterprise.server.auth.SubjectManagerRemote;
import org.rhq.enterprise.server.authz.RoleManagerRemote;
import org.rhq.enterprise.server.perspective.AbstractPerspectiveUIBean;
import org.rhq.enterprise.server.perspective.PerspectiveManagerRemote;
import org.rhq.enterprise.server.perspective.PerspectiveTarget;
import org.rhq.enterprise.server.resource.ResourceManagerRemote;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerRemote;

/**
 * When creating Groups, Roles and the Users controlled by them, the following order seems to work with the least pain
 * and redirection.
 * <p/>
 * 1) Create the 'Everything' Group. a) Go to Resources -> Platforms and click on 'New Group' link. Name the group, go
 * 'Mixed Resources' and go recursive.
 * <p/>
 * Rinse and repeat as many times as necessary ... 2) Create [desired role] with appropriate permissions and at the end
 * add the 'Everything' Group to the role 3) Go to Administration -> Security -> Users and create the 'New User' and
 * select the previously defined 'Role' to assign to the current user.
 * <p/>
 * Once you get this motion down it's less disjoint to do typical authorization.
 *
 * @author Simeon Pinder
 */
@Name("WizardBean")
@Scope(ScopeType.PAGE)
public class WizardBean extends AbstractPerspectiveUIBean {

    // Fields
    private static String NOT_YET_SET = "";
    private String title = "Creating a New EJB3 Administrator account...";
    private String titleNote = "";

    //----------------- Defines Wizard Steps ---------------------------------//

    enum Step {
        One("Enter New Group Info"), Two("Enter New Role Info"), Three("Enter New User Info"), Confirm(
            "Create Group+Role+User"), Complete("Done");

        private String displayName;

        Step(String displayName) {
            this.displayName = displayName;
        }

        public String getName() {
            return this.name();
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    //Session variables to cache initialized components for final transaction.
    private Step currentStep = Step.One;
    private String start = null;
    private String end = null;
    private ResourceGroup resourceGroup = null;
    private Role role = null;
    private Subject newSubject = null;

    //-------------------- Variable Definition by STEP -----------------------------
    //// STEP 1: Create the group to see all values.(See )
    private String groupName = NOT_YET_SET; //REQUIRED
    private String groupDescription = "";
    private String groupLocation = "";
    private boolean isRecursive = false;

    //define enumeration to enforce type restriction.

    enum Group {//Compatible == homogeneous AND mixed != homogeneous
        Compatible, Mixed
    }

    private String groupType = Group.Mixed.name(); //REQUIRED: defaults to mixed to all resources show up
    private String step1Note = "By choosing 'Next' a new group will be created and persisted to the database.";

    //// STEP 2: Create appropriate Role
    private String roleName = NOT_YET_SET; //REQUIRED
    private String roleDescription = "";

    /////GLOBAL Permissions
    private boolean manageSecurityEnabled = false;
    private String manageSecurityNote = "**(users/roles) --This permission "
        + "implicitly grants (and explicitly forces selection of) all other permissions";
    private boolean manageInventoryEnabled = false;
    private String manageInventoryNote = "(resources/groups)";
    private boolean administerRhqServerSettingsEnabled = false;

    /////RESOURCE Permissions
    private boolean modifyEnabled = true;
    private boolean deleteEnabled = true;
    private boolean createChildrenEnabled = true;
    private boolean alertEnabled = true;
    private boolean measureEnabled = true;
    private boolean contentEnabled = true;
    private boolean controlEnabled = true;
    private boolean configureEnabled = true;
    private String step2Note = "By choosing 'Next' a new Role will be created and persisted to the database.";

    //// STEP 3: Create appropriate user
    private String firstName = NOT_YET_SET; //REQUIRED
    private String lastName = "";
    private String newUserName = NOT_YET_SET;//REQUIRED
    private String step3Note = "By choosing 'Complete', the 'Create Jon Administrator' user will be completed.";
    private String phone;
    private String email;
    private String department;
    private String password; //REQUIRED
    private String password2; //REQUIRED
    private boolean enableLogin = true;

    private PageList<Resource> resources;
    private Map<Integer, String> resourceUrlMap = new HashMap<Integer, String>();

    //// STEP N:

    //// STEP N+1:

    // Methods

    public WizardBean() {
        return;
    }

    //----------------------------    The JSF event processor for all steps.---------------------

    public String processActions() throws Exception {

        String stepCompleted = "(incomplete)";

        RemoteClient remoteClient;
        Subject subject;
        try {
            remoteClient = this.perspectiveClient.getRemoteClient();
            subject = this.perspectiveClient.getSubject();
        } catch (Exception e) {
            this.facesMessages.add(StatusMessage.Severity.FATAL, "Failed to connect to RHQ Server - cause: " + e);
            return null;
        }

        switch (this.currentStep) {
        case One: //create Group for visibility

            ResourceGroup rg = new ResourceGroup(groupName);
            rg.setDescription(groupDescription);
            rg.setLocation(groupLocation);
            rg.setRecursive(isRecursive);
            //TODO: figure out how to make these calls correctly.
            String groupDefinition = "Compatible";
            //                                      rg.getGroupCategory()
            //                                      rg.
            //                                      groupManager.createResourceGroup(subject, rg);
            resourceGroup = rg;
            stepCompleted = this.currentStep.name();
            setCurrentStep(Step.Two);
            break;
        case Two: //create Role for permissions
            Role role = new Role(roleName);
            role.setDescription(roleDescription);
            if (manageSecurityEnabled) {
                role.addPermission(Permission.MANAGE_SECURITY);
            }
            if (manageInventoryEnabled) {
                role.addPermission(Permission.MANAGE_INVENTORY);
            }
            if (administerRhqServerSettingsEnabled) {
                role.addPermission(Permission.MANAGE_SETTINGS);
            }
            if (modifyEnabled) {
                role.addPermission(Permission.MODIFY_RESOURCE);
            }
            if (deleteEnabled) {
                role.addPermission(Permission.DELETE_RESOURCE);
            }
            if (createChildrenEnabled) {
                role.addPermission(Permission.CREATE_CHILD_RESOURCES);
            }
            if (alertEnabled) {
                role.addPermission(Permission.MANAGE_ALERTS);
            }
            if (measureEnabled) {
                role.addPermission(Permission.MANAGE_MEASUREMENTS);
            }
            if (contentEnabled) {
                role.addPermission(Permission.MANAGE_CONTENT);
            }
            if (controlEnabled) {
                role.addPermission(Permission.CONTROL);
            }
            if (configureEnabled) {
                role.addPermission(Permission.CONFIGURE);
            }
            this.role = role;

            stepCompleted = this.currentStep.name();
            setCurrentStep(Step.Three);
            break;
        case Three: //create User and attach previous two
            newSubject = new Subject();
            newSubject.setDepartment(department);
            newSubject.setEmailAddress(email);
            newSubject.setFirstName(firstName);
            newSubject.setFirstName(lastName);
            newSubject.setName(newUserName);
            newSubject.setPhoneNumber(phone);

            stepCompleted = this.currentStep.name();
            setCurrentStep(Step.Confirm);
            break;
        case Confirm: //create User and attach previous two
            //do check for no null values
            //commit group created

            ResourceGroupManagerRemote groupManager = remoteClient.getResourceGroupManagerRemote();
            this.resourceGroup = groupManager.createResourceGroup(subject, this.resourceGroup);
            //commit Role created

            ResourceManagerRemote resourceManager = remoteClient.getResourceManagerRemote();
            ResourceCriteria criteria = new ResourceCriteria();
            criteria.addFilterResourceTypeName("EJB3 Session Bean");

            this.resources = resourceManager.findResourcesByCriteria(subject, criteria);
            int[] resourceIds = new int[resources.size()];

            for (int i = 0; i < resources.size(); i++) {
                int id = resources.get(i).getId();
                resourceIds[i] = id;
            }
            groupManager.addResourcesToGroup(subject, this.resourceGroup.getId(), resourceIds);

            PerspectiveManagerRemote perspectiveManager = remoteClient.getPerspectiveManagerRemote();
            this.resourceUrlMap = perspectiveManager.getTargetUrls(subject, PerspectiveTarget.RESOURCE, resourceIds,
                false, false);

            RoleManagerRemote roleManager = remoteClient.getRoleManagerRemote();
            this.role = roleManager.createRole(subject, this.role);

            //commit User previously created
            SubjectManagerRemote subjectManager = remoteClient.getSubjectManagerRemote();
            this.newSubject = subjectManager.createSubject(subject, this.newSubject);

            roleManager.addRolesToSubject(subject, this.newSubject.getId(), new int[] { this.role.getId() });

            roleManager.addRolesToResourceGroup(subject, this.resourceGroup.getId(), new int[] { this.role.getId() });

            //null out all reference and reset Screens all to !completed
            remoteClient.disconnect();
            stepCompleted = this.currentStep.name();
            setCurrentStep(Step.Complete);
            break;
        default:
            System.out.println("Unrecognized screen condition. No processing is being done.");
            break;
        }

        return stepCompleted;
    }

    //----- a few methods for navigation ----------------------

    /**
     * Backs up the wizard process to previous or to the initial screen.
     */
    public String processReverse() {
        Step prev = null;
        for (int i = 0; i < Step.values().length; i++) {
            Step screen = Step.values()[i];
            if (prev == null) {
                prev = screen;
            }
            if (screen == this.currentStep) {
                this.currentStep = prev;
                break;
            }
        }
        return this.currentStep.name();
    }

    /** Reinitializes the process and resets all variables.*/
    public void cancel() {
        //null out variables and reset
        currentStep = Step.One;
        resourceGroup = null;
        role = null;
        newSubject = null;
    }

    public Step[] getAllSteps() {
        return Step.values();
    }

    public String getStart() {
        if (start == null) {
            if (Step.values().length > 0) {
                start = Step.values()[0].getName();
            }
        }
        return start;
    }

    public String getEnd() {
        if (end == null) {
            if (Step.values().length > 0) {
                end = Step.values()[Step.values().length - 1].getName();
            }
        }
        return end;
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        WizardBean b = new WizardBean();
        System.out.println("CURRENT STEP:" + b.getCurrentStep() + ":" + b.getCurrentStep().getName() + ":");
        b.processActions();//move to step 2
        b.processActions();//move to step 3
        b.processReverse();//move back to step 2
        b.processActions();//move to step 3
        b.processActions();//move to step Finish
    }

    ////##################   Generic Getter/Setter logic. ############################################

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTITLENote() {
        return titleNote;
    }

    public void setTitleNote(String titleNote) {
        this.titleNote = titleNote;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupDescription() {
        return groupDescription;
    }

    public void setGroupDescription(String groupDescription) {
        this.groupDescription = groupDescription;
    }

    public String getGroupLocation() {
        return groupLocation;
    }

    public void setGroupLocation(String groupLocation) {
        this.groupLocation = groupLocation;
    }

    public boolean isRecursive() {
        return isRecursive;
    }

    public void setRecursive(boolean isRecursive) {
        this.isRecursive = isRecursive;
    }

    public String getGroupType() {
        return groupType;
    }

    public void setGroupType(String groupType) {
        this.groupType = groupType;
    }

    public String getStep1Note() {
        return step1Note;
    }

    public void setStep1Note(String step1Note) {
        this.step1Note = step1Note;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleDescription() {
        return roleDescription;
    }

    public void setRoleDescription(String roleDescription) {
        this.roleDescription = roleDescription;
    }

    public boolean isManageSecurityEnabled() {
        return manageSecurityEnabled;
    }

    public void setManageSecurityEnabled(boolean manageSecurityEnabled) {
        this.manageSecurityEnabled = manageSecurityEnabled;
        if (this.manageInventoryEnabled) {
            //TODO: have to go through and enable/disable all depending upon boolean value.
        }
    }

    public String getManageSecurityNote() {
        return manageSecurityNote;
    }

    public void setManageSecurityNote(String manageSecurityNote) {
        this.manageSecurityNote = manageSecurityNote;
    }

    public boolean isManageInventoryEnabled() {
        return manageInventoryEnabled;
    }

    public void setManageInventoryEnabled(boolean manageInventoryEnabled) {
        this.manageInventoryEnabled = manageInventoryEnabled;
    }

    public String getManageInventoryNote() {
        return manageInventoryNote;
    }

    public void setManageInventoryNote(String manageInventoryNote) {
        this.manageInventoryNote = manageInventoryNote;
    }

    public boolean isAdministerRhqServerSettingsEnabled() {
        return administerRhqServerSettingsEnabled;
    }

    public void setAdministerRhqServerSettingsEnabled(boolean administerRhqServerSettingsEnabled) {
        this.administerRhqServerSettingsEnabled = administerRhqServerSettingsEnabled;
    }

    public boolean isModifyEnabled() {
        return modifyEnabled;
    }

    public void setModifyEnabled(boolean modifyEnabled) {
        this.modifyEnabled = modifyEnabled;
    }

    public boolean isDeleteEnabled() {
        return deleteEnabled;
    }

    public void setDeleteEnabled(boolean deleteEnabled) {
        this.deleteEnabled = deleteEnabled;
    }

    public boolean isCreateChildrenEnabled() {
        return createChildrenEnabled;
    }

    public void setCreateChildrenEnabled(boolean createChildrenEnabled) {
        this.createChildrenEnabled = createChildrenEnabled;
    }

    public boolean isAlertEnabled() {
        return alertEnabled;
    }

    public void setAlertEnabled(boolean alertEnabled) {
        this.alertEnabled = alertEnabled;
    }

    public boolean isMeasureEnabled() {
        return measureEnabled;
    }

    public void setMeasureEnabled(boolean measureEnabled) {
        this.measureEnabled = measureEnabled;
    }

    public boolean isContentEnabled() {
        return contentEnabled;
    }

    public void setContentEnabled(boolean contentEnabled) {
        this.contentEnabled = contentEnabled;
    }

    public boolean isControlEnabled() {
        return controlEnabled;
    }

    public void setControlEnabled(boolean controlEnabled) {
        this.controlEnabled = controlEnabled;
    }

    public boolean isConfigureEnabled() {
        return configureEnabled;
    }

    public void setConfigureEnabled(boolean configureEnabled) {
        this.configureEnabled = configureEnabled;
    }

    public String getStep2Note() {
        return step2Note;
    }

    public void setStep2Note(String step2Note) {
        this.step2Note = step2Note;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getNewUserName() {
        return newUserName;
    }

    public void setNewUserName(String username) {
        this.newUserName = username;
    }

    public String getStep3Note() {
        return step3Note;
    }

    public void setStep3Note(String step3Note) {
        this.step3Note = step3Note;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword2() {
        return password2;
    }

    public void setPassword2(String password2) {
        this.password2 = password2;
    }

    public boolean isEnableLogin() {
        return enableLogin;
    }

    public void setEnableLogin(boolean enableLogin) {
        this.enableLogin = enableLogin;
    }

    public Step getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(Step currentStep) {
        this.currentStep = currentStep;
    }

    public ResourceGroup getResourceGroup() {
        return resourceGroup;
    }

    public PageList<Resource> getResources() {
        return resources;
    }

    public Map<Integer, String> getResourceUrlMap() {
        return this.resourceUrlMap;
    }

    public String getGroupsUrl() throws Exception {
        PerspectiveManagerRemote perspectiveManager = this.perspectiveClient.getRemoteClient()
            .getPerspectiveManagerRemote();
        String url = perspectiveManager.getMenuItemUrl(this.perspectiveClient.getSubject(), ((Group
            .valueOf(this.groupType) == Group.Mixed) ? "groups.mixedGroups" : "groups.compatibleGroups"), false, false);
        return url;
    }

    public String getRolesUrl() throws Exception {
        PerspectiveManagerRemote perspectiveManager = this.perspectiveClient.getRemoteClient()
            .getPerspectiveManagerRemote();
        String url = perspectiveManager.getMenuItemUrl(this.perspectiveClient.getSubject(),
            "administration.security.roles", false, false);
        return url;
    }

    public String getUsersUrl() throws Exception {
        PerspectiveManagerRemote perspectiveManager = this.perspectiveClient.getRemoteClient()
            .getPerspectiveManagerRemote();
        String url = perspectiveManager.getMenuItemUrl(this.perspectiveClient.getSubject(),
            "administration.security.users", false, false);
        return url;
    }

}
