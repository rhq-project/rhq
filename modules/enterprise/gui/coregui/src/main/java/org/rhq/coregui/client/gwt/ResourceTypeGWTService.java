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

package org.rhq.coregui.client.gwt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.resource.MissingPolicy;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceTypeTemplateCountComposite;
import org.rhq.core.domain.util.PageList;

@RemoteServiceRelativePath("ResourceTypeGWTService")
public interface ResourceTypeGWTService extends RemoteService {

    void setResourceTypeIgnoreFlag(int resourceTypeId, boolean ignoreFlag) throws RuntimeException;

    void setResourceTypeMissingPolicy(int resourceTypeId, MissingPolicy policy) throws RuntimeException;

    PageList<ResourceType> findResourceTypesByCriteria(ResourceTypeCriteria criteria) throws RuntimeException;

    ArrayList<ResourceType> getResourceTypesForResourceAncestors(int resourceId) throws RuntimeException;

    ArrayList<ResourceType> getAllResourceTypeAncestors(int resourceTypeId) throws RuntimeException;

    HashMap<Integer, String> getResourceTypeDescendantsWithOperations(int resourceTypeId) throws RuntimeException;

    Map<Integer, ResourceTypeTemplateCountComposite> getTemplateCountCompositeMap() throws RuntimeException;
}
