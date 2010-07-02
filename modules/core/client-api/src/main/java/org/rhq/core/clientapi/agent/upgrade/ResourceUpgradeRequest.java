/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.core.clientapi.agent.upgrade;

import org.rhq.core.domain.resource.ResourceUpgradeReport;

/**
 * Represents a request to upgrade a resource.
 * 
 * @author Lukas Krejci
 */
public class ResourceUpgradeRequest extends ResourceUpgradeReport {

    private static final long serialVersionUID = 1L;

    private int resourceId;
    
    public ResourceUpgradeRequest() {
        
    }

    public ResourceUpgradeRequest(int resourceId, ResourceUpgradeReport report) {
        setResourceId(resourceId);
        setNewDescription(report.getNewDescription());
        setNewName(report.getNewName());
        setNewResourceKey(report.getNewResourceKey());
    }
    
    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }
    
    @Override
    public int hashCode() {
        return 31 * resourceId;
    }
    
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        
        if (!(other instanceof ResourceUpgradeRequest)) {
            return false;
        }
        
        ResourceUpgradeRequest r = (ResourceUpgradeRequest) other;
        
        return r.getResourceId() == resourceId;
    }
}
