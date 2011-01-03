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
package org.rhq.enterprise.gui.coregui.client.gwt;

import com.google.gwt.user.client.rpc.RemoteService;

import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.criteria.RoleCriteria;
import org.rhq.core.domain.util.PageList;

/**
 * @author Greg Hinkle
 */
public interface RoleGWTService extends RemoteService {

    PageList<Role> findRolesByCriteria(RoleCriteria criteria) throws RuntimeException;

    Role createRole(Role role) throws RuntimeException;

    Role updateRole(Role role) throws RuntimeException;

    void removeRoles(int[] roleIds) throws RuntimeException;

    void setAssignedResourceGroups(int roleId, int[] resourceGroupIds) throws RuntimeException;

    void setAssignedSubjects(int roleId, int[] subjectIds) throws RuntimeException;

    void setAssignedRolesForSubject(int subjectId, int[] roleIds) throws RuntimeException;
}
