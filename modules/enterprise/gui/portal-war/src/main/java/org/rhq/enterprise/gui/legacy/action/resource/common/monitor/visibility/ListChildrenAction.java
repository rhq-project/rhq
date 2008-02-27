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
package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.visibility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceWithAvailability;
import org.rhq.core.domain.resource.group.composite.AutoGroupComposite;
import org.rhq.enterprise.gui.legacy.AttrConstants;
import org.rhq.enterprise.gui.legacy.MessageConstants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.util.MonitorUtils;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.gui.uibeans.AutoGroupCompositeDisplaySummary;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementException;
import org.rhq.enterprise.server.measurement.uibean.MetricDisplaySummary;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Fetch the children resources for the server
 */
public class ListChildrenAction extends TilesAction {
    protected static final Log log = LogFactory.getLog(ListChildrenAction.class.getName());
    MeasurementDataManagerLocal dataManager;

    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        dataManager = LookupUtil.getMeasurementDataManager();

        WebUser user = SessionUtils.getWebUser(request.getSession());
        Subject subject = user.getSubject();
        Resource resource = (Resource) request.getAttribute(AttrConstants.RESOURCE_ATTR);

        // Get metric time range
        Map pref = user.getMetricRangePreference(true);
        long begin = (Long) pref.get(MonitorUtils.BEGIN);
        long end = (Long) pref.get(MonitorUtils.END);

        List<AutoGroupComposite> children;
        List<AutoGroupCompositeDisplaySummary> displaySummary;
        int parentId = -1;
        int resourceTypeId = -1;
        if (resource == null) {
            parentId = WebUtility.getOptionalIntRequestParameter(request, AttrConstants.AUTOGROUP_PARENT_ATTR, -1);
            if (parentId > 0) // get data for individual autogroup children
            {
                resourceTypeId = WebUtility.getOptionalIntRequestParameter(request, AttrConstants.AUTOGROUP_TYPE_ATTR,
                    -1);
                children = getAutoGroupChildren(subject, parentId, resourceTypeId);
                displaySummary = new ArrayList<AutoGroupCompositeDisplaySummary>(children.size());
                /* We have n children with each child representing exactly 1 resource.
                 * As we are in an autogroup, all children have the same type, so we can just feed them to the backend
                 * in one call with all n resources.
                 */

                // Loop over children, get resources, call some ..forMultiMetrics..
                List<Integer> resourceIds = new ArrayList<Integer>();
                for (AutoGroupComposite child : children) {
                    List resources = child.getResources();
                    ResourceWithAvailability rwa = (ResourceWithAvailability) resources.get(0);
                    resourceIds.add(rwa.getResource().getId());
                }

                // Map<ResourceId, List<Summaries for that resource>
                Map<Integer, List<MetricDisplaySummary>> summaries = dataManager
                    .getNarrowedMetricDisplaySummariesForResourcesAndParent(subject, resourceTypeId, parentId,
                        resourceIds, begin, end);
                for (AutoGroupComposite child : children) {
                    List resources = child.getResources();
                    ResourceWithAvailability rwa = (ResourceWithAvailability) resources.get(0);
                    List<MetricDisplaySummary> sumList = summaries.get(rwa.getResource().getId());
                    displaySummary.add(new AutoGroupCompositeDisplaySummary(child, sumList));
                }
            } else {
                RequestUtils.setError(request, MessageConstants.ERR_RESOURCE_NOT_FOUND);
                return null;
            }
        } else { // resource != null
            // get children of that single resource, which can be individual resources or autogroups
            children = getResourceChildren(resource, subject);

            displaySummary = new ArrayList<AutoGroupCompositeDisplaySummary>(children.size());
            for (AutoGroupComposite child : children) {
                List<MetricDisplaySummary> metrics = getMetrics(resource, child, subject, begin, end);

                // set "info about autogroup" so that we can use that info in the JSP later
                // definitionId is already set in the backend
                if (metrics != null)
                    for (MetricDisplaySummary tmp : metrics) {
                        tmp.setParentId(parentId);
                        tmp.setChildTypeId(resourceTypeId);
                    }

                displaySummary.add(new AutoGroupCompositeDisplaySummary(child, metrics));
            }
        }

        context.putAttribute(AttrConstants.CTX_SUMMARIES, displaySummary);

        return null;
    }

    public List<MetricDisplaySummary> getMetrics(Resource parentResource, AutoGroupComposite resourceGroupComposite,
        Subject subject, long beginTime, long endTime) throws MeasurementException {
        if (log.isTraceEnabled()) {
            log.trace("finding metric summaries for resourceType ["
                + resourceGroupComposite.getResourceType().getName());
        }

        List<MetricDisplaySummary> metricSummaries = null;
        // TODO GH: Why are we only getting the first one? --> single resource case
        //

        List resources = resourceGroupComposite.getResources();
        if ((resources != null) && (resources.size() == 1)) {
            ResourceWithAvailability resource = (ResourceWithAvailability) resources.get(0);

            metricSummaries = dataManager.getMetricDisplaySummariesForMetrics(subject, resource.getResource().getId(),
                DataType.MEASUREMENT, beginTime, endTime, true, true);
        } else if ((resources != null) && (resources.size() > 1)) {
            List<Resource> res = new ArrayList<Resource>();
            for (Object o : resources) {
                if (o instanceof ResourceWithAvailability) {
                    ResourceWithAvailability rwa = (ResourceWithAvailability) o;
                    res.add(rwa.getResource());
                }
            }

            Map<Integer, List<MetricDisplaySummary>> sumMap = dataManager
                .getNarrowedMetricDisplaySummaryForCompatibleResources(subject, res, beginTime, endTime);
            metricSummaries = sumMap.values().iterator().next();

            // fill in some data that does not come from the backend
            for (MetricDisplaySummary tmp : metricSummaries) {
                tmp.setParentId(parentResource.getId());
                tmp.setChildTypeId(resourceGroupComposite.getResourceType().getId());
            }
        }

        return metricSummaries;
    }

    private List<AutoGroupComposite> getResourceChildren(Resource resource, Subject subject) {
        ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
        List<AutoGroupComposite> children = resourceManager.getChildrenAutoGroups(subject, resource.getId());

        AutoGroupComposite resourceGroupComposite = resourceManager.getResourceAutoGroup(subject, resource.getId());
        if (resourceGroupComposite != null)
            resourceGroupComposite.setMainResource(true);
        else
            return new ArrayList<AutoGroupComposite>();

        // now increase everyone's depth by one to account for the resource
        for (AutoGroupComposite child : children) {
            child.setDepth(child.getDepth() + 1);
        }

        children.add(0, resourceGroupComposite);

        Resource parentResource = resourceManager.getParentResource(resource.getId());
        AutoGroupComposite parentGroupComposite = null;
        if (parentResource != null) {
            parentGroupComposite = resourceManager.getResourceAutoGroup(subject, parentResource.getId());
        }

        if (parentGroupComposite != null) {
            // now increase everyone's depth by one to account for the parent
            for (AutoGroupComposite child : children) {
                child.setDepth(child.getDepth() + 1);
            }

            children.add(0, parentGroupComposite);
        }

        return children;
    }

    /**
     * Get a list of the individual 'child resources' of that autogroup.
     */
    private List<AutoGroupComposite> getAutoGroupChildren(Subject subject, int parentId, int resourceTypeId) {
        List<AutoGroupComposite> children = new ArrayList<AutoGroupComposite>();
        ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
        ResourceTypeManagerLocal resourceTypeMananger = LookupUtil.getResourceTypeManager();
        Resource parentResource = resourceManager.getResourceById(subject, parentId);
        ResourceType resourceType = null;
        try {
            resourceType = resourceTypeMananger.getResourceTypeById(subject, resourceTypeId);
        } catch (ResourceTypeNotFoundException e) {
            return children; // empty list if we don't know the child type
        }

        if ((resourceType != null) && (parentResource != null)) {
            // first get the resources in the autogroup
            List<ResourceWithAvailability> resourcesForAutoGroup = resourceManager.getResourcesByParentAndType(subject,
                parentResource, resourceType);

            List<Integer> resourceIds = new ArrayList<Integer>();
            for (ResourceWithAvailability resourceInAutoGroup : resourcesForAutoGroup) {
                int id = resourceInAutoGroup.getResource().getId();
                resourceIds.add(id);
            }

            // And then the composite to return
            List<AutoGroupComposite> composite = resourceManager.getResourcesAutoGroups(subject, resourceIds);

            return composite;
        }

        return children; // empty
    }
}