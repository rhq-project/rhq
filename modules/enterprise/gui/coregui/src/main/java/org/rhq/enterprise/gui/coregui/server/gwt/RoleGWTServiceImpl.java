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
package org.rhq.enterprise.gui.coregui.server.gwt;

import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.criteria.RoleCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.coregui.client.gwt.RoleGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.authz.RoleManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 */
public class RoleGWTServiceImpl extends AbstractGWTServiceImpl implements RoleGWTService {

    private static final long serialVersionUID = 1L;

    private RoleManagerLocal roleManager = LookupUtil.getRoleManager();

    public PageList<Role> findRolesByCriteria(RoleCriteria criteria) throws RuntimeException {
        try {
            return SerialUtility.prepare(roleManager.findRolesByCriteria(getSessionSubject(), criteria),
                "RoleService.findRolesByCriteria");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public Role createRole(Role role) throws RuntimeException {
        try {
            return SerialUtility.prepare(roleManager.createRole(getSessionSubject(), role), "RoleService.createRole");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public Role updateRole(Role role) throws RuntimeException {
        try {
            return SerialUtility.prepare(roleManager.updateRole(getSessionSubject(), role), "RoleService.updateRole");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public void removeRoles(int[] roleIds) throws RuntimeException {
        try {
            roleManager.deleteRoles(getSessionSubject(), roleIds);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public void setAssignedResourceGroups(int roleId, int[] resourceGroupIds) throws RuntimeException {
        try {
            roleManager.setAssignedResourceGroups(getSessionSubject(), roleId, resourceGroupIds);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public void setAssignedSubjects(int roleId, int[] subjectIds) throws RuntimeException {
        try {
            roleManager.setAssignedSubjects(getSessionSubject(), roleId, subjectIds);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public void setAssignedRolesForSubject(int subjectId, int[] roleIds) throws RuntimeException {
        try {
            roleManager.setAssignedSubjectRoles(getSessionSubject(), subjectId, roleIds);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }
}