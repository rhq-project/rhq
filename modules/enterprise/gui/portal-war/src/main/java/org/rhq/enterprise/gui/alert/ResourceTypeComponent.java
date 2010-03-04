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
package org.rhq.enterprise.gui.alert;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Unwrap;
import org.jboss.seam.annotations.web.RequestParameter;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;

@AutoCreate
@Scope(ScopeType.PAGE)
@Name("resourceType")
public class ResourceTypeComponent {

    @RequestParameter("id")
    private Integer resourceId;

    @In("#{webUser.subject}")
    private Subject subject;

    @In
    private ResourceManagerLocal resourceManager;

    @In
    private AlertDefinition alertDefinition;

    private ResourceType resourceType;

    @Unwrap
    public ResourceType lookupResourceType() {
        if (this.alertDefinition.getResource() != null) {
            this.resourceType = this.alertDefinition.getResource().getResourceType();
        } else if (this.alertDefinition.getResourceGroup() != null) {
            this.resourceType = this.alertDefinition.getResourceGroup().getResourceType();
        } else if (this.resourceId != null) {
            Resource resource = this.resourceManager.getResource(this.subject, this.resourceId);

            if (resource != null) {
                this.resourceType = resource.getResourceType();
            }
        }

        return this.resourceType;
    }
}