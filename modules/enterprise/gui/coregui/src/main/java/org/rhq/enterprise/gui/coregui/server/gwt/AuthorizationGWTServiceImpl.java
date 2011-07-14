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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.rhq.core.domain.authz.Permission;
import org.rhq.enterprise.gui.coregui.client.gwt.AuthorizationGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 */
public class AuthorizationGWTServiceImpl extends AbstractGWTServiceImpl implements AuthorizationGWTService {

    private static final long serialVersionUID = 1L;

    private AuthorizationManagerLocal authorizationManager = LookupUtil.getAuthorizationManager();

    public Set<Permission> getExplicitResourcePermissions(int resourceId) throws RuntimeException {
        try {
            return SerialUtility.prepare(new HashSet<Permission>(authorizationManager.getExplicitResourcePermissions(
                getSessionSubject(), resourceId)), "AuthorizationManager.getExplicitResourcePermissions");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public Set<Permission> getImplicitResourcePermissions(int resourceId) throws RuntimeException {
        try {
            return SerialUtility.prepare(new HashSet<Permission>(authorizationManager.getImplicitResourcePermissions(
                getSessionSubject(), resourceId)), "AuthorizationManager.getImplicitResourcePermissions");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public Set<Permission> getExplicitGroupPermissions(int groupId) throws RuntimeException {
        try {
            return SerialUtility.prepare(new HashSet<Permission>(authorizationManager.getExplicitGroupPermissions(
                getSessionSubject(), groupId)), "AuthorizationManager.getExplicitGroupPermissions");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public Set<Permission> getImplicitGroupPermissions(int groupId) throws RuntimeException {
        try {
            return SerialUtility.prepare(new HashSet<Permission>(authorizationManager.getImplicitGroupPermissions(
                getSessionSubject(), groupId)), "AuthorizationManager.getImplicitGroupPermissions");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public Set<Permission> getExplicitGlobalPermissions() throws RuntimeException {
        try {
            return SerialUtility.prepare(new HashSet<Permission>(authorizationManager
                .getExplicitGlobalPermissions(getSessionSubject())),
                "AuthorizationManager.getExplicitGlobalPermissions");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public boolean hasResourcePermission(Permission permission, Collection<Integer> resourceIds) {
        try {
            boolean result = authorizationManager.hasResourcePermission(getSessionSubject(), permission, resourceIds);
            return result;
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

}
