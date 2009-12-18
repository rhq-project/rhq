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

import org.rhq.enterprise.server.perspective.activator.context.GlobalActivationContext;

/**
 * @author Ian Springer
 */
public class InventoryActivator extends AbstractSubjectActivator {
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
    public boolean matches(GlobalActivationContext context) {
        for (ResourceConditionSet resourceConditionSet : this.resourceConditionSets) {
           // TODO: Query DB to see if condition set matches one or more inventoried Resources.
        }
        return false;
    }
}