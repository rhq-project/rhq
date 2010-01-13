/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.perspective.activator.context;

import java.util.EnumSet;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.ResourceTypeFacet;

/**
 * @author Ian Springer
 */
public abstract class AbstractResourceOrGroupActivationContext extends GlobalActivationContext {
    protected AbstractResourceOrGroupActivationContext(Subject subject) {
        super(subject);
    }

    @Override
    public ActivationContextScope getScope() {
        return ActivationContextScope.RESOURCE_OR_GROUP;
    }

    public abstract ResourceType getResourceType();
    public abstract EnumSet<ResourceTypeFacet> getFacets();
    protected abstract EnumSet<Permission> getResourcePermissions();

    public boolean hasResourcePermission(Permission permission) {
        return hasGlobalPermission(Permission.MANAGE_INVENTORY) || getResourcePermissions().contains(permission);
    }
}
