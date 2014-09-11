/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.coregui.server.gwt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.resource.MissingPolicy;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceLineageComposite;
import org.rhq.core.domain.resource.composite.ResourceTypeTemplateCountComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.gwt.ResourceTypeGWTService;
import org.rhq.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ResourceTypeGWTServiceImpl extends AbstractGWTServiceImpl implements ResourceTypeGWTService {

    private static final long serialVersionUID = 1L;

    @Override
    public void setResourceTypeIgnoreFlag(int resourceTypeId, boolean ignoreFlag) throws RuntimeException {
        try {
            ResourceTypeManagerLocal typeManager = LookupUtil.getResourceTypeManager();
            typeManager.setResourceTypeIgnoreFlagAndUninventoryResources(getSessionSubject(), resourceTypeId,
                ignoreFlag);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void setResourceTypeMissingPolicy(int resourceTypeId, MissingPolicy policy) throws RuntimeException {
        try {
            ResourceTypeManagerLocal typeManager = LookupUtil.getResourceTypeManager();
            typeManager.setResourceTypeMissingPolicy(getSessionSubject(), resourceTypeId, policy);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public PageList<ResourceType> findResourceTypesByCriteria(ResourceTypeCriteria criteria) throws RuntimeException {
        try {
            ResourceTypeManagerLocal typeManager = LookupUtil.getResourceTypeManager();

            return SerialUtility.prepare(typeManager.findResourceTypesByCriteria(getSessionSubject(), criteria),
                "ResourceTypes.findResourceTypesByCriteria");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    /**
     * Given a resource ID, this gets all resource types for all ancestors in that resource's lineage.
     */
    @Override
    public ArrayList<ResourceType> getResourceTypesForResourceAncestors(int resourceId) throws RuntimeException {
        try {
            ResourceManagerLocal manager = LookupUtil.getResourceManager();
            List<ResourceLineageComposite> lineage = manager.getResourceLineage(getSessionSubject(), resourceId);
            ArrayList<ResourceType> types = new ArrayList<ResourceType>(lineage.size());
            for (ResourceLineageComposite composite : lineage) {
                types.add(composite.getResource().getResourceType());
            }
            return SerialUtility.prepare(types, "ResourceTypes.getResourceTypesForResourceAncestors");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public ArrayList<ResourceType> getAllResourceTypeAncestors(int resourceTypeId) throws RuntimeException {
        try {
            ResourceTypeManagerLocal typeManager = LookupUtil.getResourceTypeManager();
            List<ResourceType> list = typeManager.getAllResourceTypeAncestors(getSessionSubject(), resourceTypeId);
            return SerialUtility
                .prepare(new ArrayList<ResourceType>(list), "ResourceTypes.getAllResourceTypeAncestors");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public HashMap<Integer, String> getResourceTypeDescendantsWithOperations(int resourceTypeId)
        throws RuntimeException {
        try {
            ResourceTypeManagerLocal typeManager = LookupUtil.getResourceTypeManager();
            HashMap<Integer, String> map = typeManager.getResourceTypeDescendantsWithOperations(getSessionSubject(),
                resourceTypeId);
            return SerialUtility.prepare(map, "ResourceTypes.getResourceTypeDescendantsWithOperations");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public Map<Integer, ResourceTypeTemplateCountComposite> getTemplateCountCompositeMap() throws RuntimeException {
        try {
            ResourceTypeManagerLocal typeManager = LookupUtil.getResourceTypeManager();
            Map<Integer, ResourceTypeTemplateCountComposite> map = typeManager.getTemplateCountCompositeMap();
            return SerialUtility.prepare(map, "ResourceTypes.getTemplateCountCompositeMap");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }
}