/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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
package org.rhq.enterprise.server.db;

import java.util.Set;

import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.RoleManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.DbSetupUtility;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Test that dbsetup initializes the data in the RHQ schema as intended (e.g. test that the Superuser and All Resources
 * are created and have the correct permissions and users assigned to them.
 *
 * @author Ian Springer
 */
@Test(groups = "db", singleThreaded = true)
public class DbSetupTest extends AbstractEJB3Test {

    @Override
    protected void beforeMethod() throws Exception {
        // Recreate a fresh JON DB with the latest schema.
        DbSetupUtility.dbreset();
        DbSetupUtility.dbsetup();
    }

    public void testRoles() throws Exception {
        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
        RoleManagerLocal roleManager = LookupUtil.getRoleManager();
        Subject overlord = subjectManager.getOverlord();

        Role superuserRole = roleManager.getRole(overlord, 1);
        Set<Permission> superuserRoleOriginalPermissions = superuserRole.getPermissions();
        assertTrue("[Superuser] role does not have all global permissions.",
            superuserRoleOriginalPermissions.containsAll(Permission.GLOBAL_ALL));
        assertTrue("[Superuser] role does not have all Resource permissions.",
            superuserRoleOriginalPermissions.containsAll(Permission.RESOURCE_ALL));
                
        Role allResourcesRole = roleManager.getRole(overlord, 2);
        Set<Permission> allResourcesRoleOriginalPermissions = allResourcesRole.getPermissions();
        assertTrue("[All Resources] role does not have MANAGE_INVENTORY permission.",
            allResourcesRoleOriginalPermissions.contains(Permission.MANAGE_INVENTORY));
        assertTrue("[All Resources] role does not have all Resource permissions.",
            allResourcesRoleOriginalPermissions.containsAll(Permission.RESOURCE_ALL));
    }
    
}
