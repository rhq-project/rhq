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

import java.util.Collection;
import java.util.Set;

import com.google.gwt.user.client.rpc.RemoteService;

import org.rhq.core.domain.authz.Permission;

/**
 * @author Greg Hinkle
 */
public interface AuthorizationGWTService extends RemoteService {

    /**
     * Gets the set of permissions that the current user explicitly possesses for the specified {@link org.rhq.core.domain.resource.Resource}.
     *
     * @param  resourceId the id of some Resource to check permissions against
     *
     * @return the set of permissions that the current user possesses for the specified {@link org.rhq.core.domain.resource.Resource} - never null
     */
    Set<Permission> getExplicitResourcePermissions(int resourceId) throws RuntimeException;

    /**
     * Gets the set of permissions that the current user implicitly possesses for the specified {@link org.rhq.core.domain.resource.Resource}.
     *
     * @param  resourceId the id of some Resource to check permissions against
     *
     * @return the set of permissions that the current user implicitly possesses for the specified {@link org.rhq.core.domain.resource.Resource} - never null
     */
    Set<Permission> getImplicitResourcePermissions(int resourceId) throws RuntimeException;

    /**
     * Gets the set of permissions that the current user explicitly possesses for the specified {@link org.rhq.core.domain.resource.group.Group}.
     *
     * @param  groupId the id of some Group to check permissions against
     *
     * @return the set of permissions that the current user explicitly possesses for the specified {@link org.rhq.core.domain.resource.group.Group} - never null
     */
    Set<Permission> getExplicitGroupPermissions(int groupId) throws RuntimeException;

    /**
     * Gets the set of permissions that the current user implicitly possesses for the specified {@link org.rhq.core.domain.resource.group.Group}.
     *
     * @param  groupId the id of some Group to check permissions against
     *
     * @return the set of permissions that the current user implicitly possesses for the specified {@link org.rhq.core.domain.resource.group.Group}
     */
    Set<Permission> getImplicitGroupPermissions(int groupId) throws RuntimeException;

    /**
     * Gets the set of global permissions that the current user explicitly possesses.
     *
     * @return the set of global permissions that the current user possesses - never null
     */
    Set<Permission> getExplicitGlobalPermissions() throws RuntimeException;

    /**
     * Returns true if the current user possesses either: 1) the specified resource permission for *all* of the
     * specified resources, or 2) the global MANAGE_INVENTORY permission which, by definition, gives full access to the
     * inventory (all resources and all groups) NOTE: The size of the collection must be less than or equal to 1000 (due
     * to an Oracle limitation).
     *
     * @param  permission  a resource permission (i.e. permission.getTarget() == Permission.Target.RESOURCE)
     * @param  resourceIds the ids of some Resources to check permissions against (size of collection must be <= 1000)
     *
     * @return true if the current user possesses the specified resource permission for the specified resource
     */
    boolean hasResourcePermission(Permission permission, Collection<Integer> resourceIds);

}
