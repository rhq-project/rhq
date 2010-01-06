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
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.perspective.activator.context.GlobalActivationContext;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Ian Springer
 */
public class InventoryActivator extends AbstractGlobalActivator {
    static final long serialVersionUID = 1L;

    private final Log log = LogFactory.getLog(this.getClass());

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
                return traitsSatisfied(context, rcs.getTraitMatchers(), resources);
            }
        }

        return false;
    }

    /** 
     * If any returned resource satisfies all trait activators then return true since it means there
     * is at least one inventoried resource satisfying all activation conditions.
     * If a trait activator exists for a trait not returned by the resource it is ignored.
     * // TODO: is a warning needed for ignored trait conditions? 
     */
    private boolean traitsSatisfied(GlobalActivationContext context, Map<String, Matcher> traitMatchers,
        PageList<Resource> resources) {

        // return true if there are no trait activators to satisfy
        if (traitMatchers.isEmpty()) {
            return true;
        }

        MeasurementDataManagerLocal measurementDataManager = LookupUtil.getMeasurementDataManager();

        for (Resource resource : resources) {
            boolean traitsSatisfied = true;
            List<MeasurementDataTrait> traits = measurementDataManager.findCurrentTraitsForResource(context
                .getSubject(), resource.getId(), null);

            int numTraitsTested = 0;
            for (MeasurementDataTrait trait : traits) {
                Matcher traitMatcher = traitMatchers.get(trait.getName());
                if (null != traitMatcher) {
                    ++numTraitsTested;

                    traitMatcher.reset(trait.getValue());
                    if (!traitMatcher.find()) {
                        traitsSatisfied = false;
                        break;
                    }
                }
            }

            if (traitsSatisfied) {
                if (numTraitsTested != traitMatchers.size()) {
                    String error = "Potential error in perspective descriptor. Not all trait activators matched trait for resource type: "
                        + traitMatchers.keySet();
                    log.warn(error);
                    return false;
                }
                return true;
            }
        }

        return false;
    }
}