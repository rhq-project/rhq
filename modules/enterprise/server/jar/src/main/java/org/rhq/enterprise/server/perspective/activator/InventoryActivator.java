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
package org.rhq.enterprise.server.perspective.activator;

import java.util.List;
import java.util.Set;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.perspective.activator.context.GlobalActivationContext;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Ian Springer
 */
public class InventoryActivator extends AbstractGlobalActivator {
    static final long serialVersionUID = 1L;

    private List<ResourceConditionSet> resourceConditionSets;

    public InventoryActivator(List<ResourceConditionSet> resourceConditionSets) {
        this.resourceConditionSets = resourceConditionSets;
    }

    /**
     * Returns true if any of the condition sets match an inventoried Resource.
     *
     * @param context
     * @return
     */
    public boolean isActive(GlobalActivationContext context) {
        ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
        Subject subject = context.getSubject();

        for (ResourceConditionSet rcs : this.resourceConditionSets) {
            ResourceCriteria criteria = new ResourceCriteria();

            criteria.addFilterPluginName(rcs.getPluginName());
            criteria.addFilterResourceTypeName(rcs.getResourceTypeName());
            Set<Permission> requiredPermissions = rcs.getPermissions();
            if (!((null == requiredPermissions) || requiredPermissions.isEmpty())) {
                Permission[] arr = requiredPermissions.toArray(new Permission[requiredPermissions.size()]);
                criteria.addRequiredPermissions(arr);
            }

            PageList<Resource> resources = resourceManager.findResourcesByCriteria(context.getSubject(), criteria);
            if (!((null == resources) || resources.isEmpty())) {
                return ActivatorHelper.areTraitsSatisfied(subject, rcs.getTraitMatchers(), resources, false);
            }
        }

        return false;
    }
}