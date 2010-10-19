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

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.RemoteService;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.core.domain.resource.composite.DisambiguationReport;
import org.rhq.core.domain.resource.composite.ProblemResourceComposite;
import org.rhq.core.domain.resource.composite.RecentlyAddedResourceComposite;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.resource.composite.ResourceLineageComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * @author Greg Hinkle
 */
public interface ResourceGWTService extends RemoteService {

    PageList<Resource> findResourcesByCriteria(ResourceCriteria criteria);

    PageList<ResourceComposite> findResourceCompositesByCriteria(ResourceCriteria criteria);

    List<ResourceLineageComposite> getResourceLineageAndSiblings(int resourceId);

    List<RecentlyAddedResourceComposite> findRecentlyAddedResources(long ctime, int maxItems);

    List<DisambiguationReport<ProblemResourceComposite>> findProblemResources(long ctime, int maxItems);

    Resource getPlatformForResource(int resourceId);

    List<Integer> uninventoryResources(int[] resourceIds);

    void updateResource(Resource resource);

    void createResource(int parentResourceId, int newResourceTypeId, String newResourceName,
        Configuration newResourceConfiguration);

    Map<Resource, List<Resource>> getQueuedPlatformsAndServers(HashSet<InventoryStatus> statuses, PageControl pc);

    void importResources(int[] resourceIds);

    void ignoreResources(int[] resourceIds);

    void unignoreResources(int[] resourceIds);

    List<ResourceError> findResourceErrors(int resourceId);

    Resource manuallyAddResource(int resourceTypeId, int parentResourceId, Configuration pluginConfiguration);

}
