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
    Set<Permission> getExplicitResourcePermissions(int resourceId);

    /**
     * Gets the set of permissions that the current user implicitly possesses for the specified {@link org.rhq.core.domain.resource.Resource}.
     *
     * @param  resourceId the id of some Resource to check permissions against
     *
     * @return the set of permissions that the current user implicitly possesses for the specified {@link org.rhq.core.domain.resource.Resource} - never null
     */
    Set<Permission> getImplicitResourcePermissions(int resourceId);

    /**
     * Gets the set of permissions that the current user explicitly possesses for the specified {@link org.rhq.core.domain.resource.group.Group}.
     *
     * @param  groupId the id of some Group to check permissions against
     *
     * @return the set of permissions that the current user explicitly possesses for the specified {@link org.rhq.core.domain.resource.group.Group} - never null
     */
    Set<Permission> getExplicitGroupPermissions(int groupId);

    /**
     * Gets the set of permissions that the current user implicitly possesses for the specified {@link org.rhq.core.domain.resource.group.Group}.
     *
     * @param  groupId the id of some Group to check permissions against
     *
     * @return the set of permissions that the current user implicitly possesses for the specified {@link org.rhq.core.domain.resource.group.Group}
     */
    Set<Permission> getImplicitGroupPermissions(int groupId);

    /**
     * Gets the set of global permissions that the current user explicitly possesses.
     *
     * @return the set of global permissions that the current user possesses - never null
     */
    Set<Permission> getExplicitGlobalPermissions();

    /**
     * Lightweight check of whether current user has manage inventory permissions.
     *
     * @return Boolean answer to manage inventory permissions status.
     */
    Boolean checkUserGlobalPermission(Permission permission);

}
