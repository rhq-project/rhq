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
import java.util.UUID;

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
 * Test that various dbupgrade schema versions update the data model as intended.
 *
 * @author Ian Springer
 */
@Test(groups = "db", singleThreaded = true)
public class DbUpgradeTest extends AbstractEJB3Test {

    @Override
    protected void beforeMethod() throws Exception {
        // Recreate a fresh JON DB with the JON 2.3.1 schema, then upgrade it to the JON 3.0.0 schema.
        DbSetupUtility.dbreset();
        DbSetupUtility.dbsetup("2.3.1");
        DbSetupUtility.dbupgrade(DbSetupUtility.JON300_SCHEMA_VERSION);
    }

    @Override
    protected void afterMethod() throws Exception {
        // Upgrade to the latest schema version so the DB is left in a state usable by other tests.
        DbSetupUtility.dbupgrade("LATEST");
    }

    public void testUpgradeToV2_119() throws Exception {
        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
        RoleManagerLocal roleManager = LookupUtil.getRoleManager();
        Subject overlord = subjectManager.getOverlord();

        Role customRole = new Role("role" + UUID.randomUUID());
        customRole.addPermission(Permission.MANAGE_REPOSITORIES);
        customRole.addPermission(Permission.MANAGE_MEASUREMENTS);
        customRole = roleManager.createRole(overlord, customRole);
        Set<Permission> customRoleOriginalPermissions = customRole.getPermissions();        
        assertFalse(customRoleOriginalPermissions.contains(Permission.VIEW_USERS));

        Role superuserRole = roleManager.getRole(overlord, 1);
        Set<Permission> superuserRoleOriginalPermissions = superuserRole.getPermissions();
        assertFalse(superuserRoleOriginalPermissions.contains(Permission.VIEW_USERS));
                
        Role allResourcesRole = roleManager.getRole(overlord, 2);
        Set<Permission> allResourcesRoleOriginalPermissions = allResourcesRole.getPermissions();
        assertFalse(allResourcesRoleOriginalPermissions.contains(Permission.VIEW_USERS));
        
        // Now upgrade the DB schema to v2.119, which introduces the VIEW_USERS global permission and adds it to any
        // existing roles.
        DbSetupUtility.dbupgrade("2.119");
        
        customRole = roleManager.getRole(overlord, customRole.getId());
        Set<Permission> customRoleNewPermissions = customRole.getPermissions();
        customRoleOriginalPermissions.add(Permission.VIEW_USERS);
        assertEquals(customRoleOriginalPermissions, customRoleNewPermissions);

        superuserRole = roleManager.getRole(overlord, 1);
        Set<Permission> superuserRoleNewPermissions = superuserRole.getPermissions();
        superuserRoleOriginalPermissions.add(Permission.VIEW_USERS);
        assertEquals(superuserRoleOriginalPermissions, superuserRoleNewPermissions);

        allResourcesRole = roleManager.getRole(overlord, 2);
        Set<Permission> allResourcesRoleNewPermissions = allResourcesRole.getPermissions();
        allResourcesRoleOriginalPermissions.add(Permission.VIEW_USERS);
        assertEquals(allResourcesRoleOriginalPermissions, allResourcesRoleNewPermissions);
    }
    
}
