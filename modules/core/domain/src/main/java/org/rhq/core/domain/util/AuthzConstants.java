 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.domain.util;

import org.rhq.core.domain.auth.Subject;

public final class AuthzConstants {
    // Root Resource Type
    public static final String rootResType = "covalentAuthzRootResourceType";

    // This assumes that the root resource is always initialized
    // with the first id available in a sequence that starts at 0
    public static final Integer rootResourceId = new Integer(0);
    public static final Integer rootSubjectId = new Integer(1);
    public static final String groupResourceTypeName = "covalentAuthzResourceGroup";
    public static final String rootResourceGroupName = "ROOT_RESOURCE_GROUP";
    public static final Integer rootResourceGroupId = new Integer(1);
    public static final Integer authzResourceGroupId = new Integer(0);

    // Appdef Resource Types
    public static final String platformResType = "covalentEAMPlatform";
    public static final String serverResType = "covalentEAMServer";
    public static final String serviceResType = "covalentEAMService";
    public static final String applicationResType = "covalentEAMApplication";
    public static final String groupResType = "covalentAuthzResourceGroup";

    // Appdef Operations

    // Platform Operations
    public static final String platformOpCreatePlatform = "createPlatform";
    public static final String platformOpModifyPlatform = "modifyPlatform";
    public static final String platformOpRemovePlatform = "removePlatform";
    public static final String platformOpAddServer = "addServer";
    public static final String platformOpViewPlatform = "viewPlatform";
    public static final String platformOpMonitorPlatform = "monitorPlatform";
    public static final String platformOpControlPlatform = "controlPlatform";
    public static final String platformOpManageAlerts = "managePlatformAlerts";

    // Server Operations
    public static final String serverOpCreateServer = "createServer";
    public static final String serverOpModifyServer = "modifyServer";
    public static final String serverOpRemoveServer = "removeServer";
    public static final String serverOpAddService = "addService";
    public static final String serverOpViewServer = "viewServer";
    public static final String serverOpMonitorServer = "monitorServer";
    public static final String serverOpControlServer = "controlServer";
    public static final String serverOpManageAlerts = "manageServerAlerts";

    // Service Operations
    public static final String serviceOpCreateService = "createService";
    public static final String serviceOpModifyService = "modifyService";
    public static final String serviceOpRemoveService = "removeService";
    public static final String serviceOpViewService = "viewService";
    public static final String serviceOpMonitorService = "monitorService";
    public static final String serviceOpControlService = "controlService";
    public static final String serviceOpManageAlerts = "manageServiceAlerts";

    // Application Operations
    public static final String appOpCreateApplication = "createApplication";
    public static final String appOpModifyApplication = "modifyApplication";
    public static final String appOpRemoveApplication = "removeApplication";
    public static final String appOpViewApplication = "viewApplication";
    public static final String appOpMonitorApplication = "monitorApplication";
    public static final String appOpControlApplication = "controlApplication";
    public static final String appOpManageAlerts = "manageApplicationAlerts";

    // Group Operations
    public static final String groupOpViewResourceGroup = "viewResourceGroup";
    public static final String groupOpManageAlerts = "manageGroupAlerts";

    // View permission constants - defined in authz-data.xml
    public static final Integer perm_viewSubject = new Integer(8);
    public static final Integer perm_viewRole = new Integer(16);
    public static final Integer perm_viewResourceGroup = new Integer(28);
    public static final Integer perm_viewPlatform = new Integer(305);
    public static final Integer perm_viewServer = new Integer(311);
    public static final Integer perm_viewService = new Integer(315);
    public static final Integer perm_viewApplication = new Integer(319);

    // Modify permission constants - defined in authz-data.xml
    public static final Integer perm_modifySubject = new Integer(6);
    public static final Integer perm_modifyRole = new Integer(11);
    public static final Integer perm_modifyResourceGroup = new Integer(24);
    public static final Integer perm_modifyPlatform = new Integer(301);
    public static final Integer perm_modifyServer = new Integer(307);
    public static final Integer perm_modifyService = new Integer(313);
    public static final Integer perm_modifyApplication = new Integer(317);

    // remove permission constants - defined in authz-data.xml
    public static final Integer perm_removeSubject = new Integer(7);
    public static final Integer perm_removeRole = new Integer(30);
    public static final Integer perm_removeResourceGroup = new Integer(31);
    public static final Integer perm_removePlatform = new Integer(302);
    public static final Integer perm_removeServer = new Integer(308);
    public static final Integer perm_removeService = new Integer(314);
    public static final Integer perm_removeApplication = new Integer(318);

    // Authz Stuff...

    public static final String rootRoleName = "Super User Role";
    public static final Integer rootRoleId = new Integer(1);
    public static final String creatorRoleName = "RESOURCE_CREATOR_ROLE";
    public static final String subjectResourceTypeName = "covalentAuthzSubject";
    public static final String typeResourceTypeName = "covalentAuthzRootResourceType";
    public static final String roleResourceTypeName = "covalentAuthzRole";
    public static final int overlordId = 1;
    public static final Integer overlordIdInteger = new Integer(1);
    public static final String overlordName = "admin";
    public static final String overlordDsn = "covalentAuthzInternalDsn";
    public static final String authzResourceGroupName = "covalentAuthzResourceGroup";

    public static final String rootOpCAMAdmin = "administerCAM";

    public static final String typeOpCreateResource = "createResource";
    public static final String typeOpModifyResourceType = "modifyResourceType";
    public static final String typeOpAddOperation = "addOperation";
    public static final String typeOpRemoveOperation = "removeOperation";

    public static final String subjectOpViewSubject = "viewSubject";
    public static final String subjectOpModifySubject = "modifySubject";
    public static final String subjectOpRemoveSubject = "removeSubject";
    public static final String subjectOpCreateSubject = "createSubject";

    public static final String roleOpCreateRole = "createRole";
    public static final String roleOpModifyRole = "modifyRole";
    public static final String roleOpRemoveRole = "removeRole";
    public static final String roleOpViewRole = "viewRole";

    public static final String groupOpModifyResourceGroup = "modifyResourceGroup";
    public static final String groupOpAddRole = "addRole";

    public static final String groupOpRemoveResourceGroup = "removeResourceGroup";
    public static final String privateRoleGroupName = "camPrivateRoleGroup:";
    public static final int authzDefaultResourceGroupType = 13;

    public static final Integer authzSubject = new Integer(1);
    public static final Integer authzRole = new Integer(2);
    public static final Integer authzGroup = new Integer(3);
    public static final Integer authzPlatform = new Integer(301);
    public static final Integer authzServer = new Integer(303);
    public static final Integer authzService = new Integer(305);
    public static final Integer authzApplication = new Integer(308);
    public static final Integer authzLocation = new Integer(309);

    // TODO: see AuthorizationManager.isBuiltInSuperuser()
    public static boolean isOverlord(Integer subject) {
        return subject.equals(AuthzConstants.overlordIdInteger);
    }

    public static boolean isOverlord(Subject subject) {
        return isOverlord(subject.getId());
    }
}