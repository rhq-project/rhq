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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;

import org.rhq.core.clientapi.util.ArrayUtil;
import org.rhq.core.clientapi.util.StringUtil;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.MeasurementBaseline;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.NumericType;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.measurement.util.MeasurementConverter;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.Group;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.gui.common.servlet.HighLowMetricValue;
import org.rhq.enterprise.gui.legacy.AttrConstants;
import org.rhq.enterprise.gui.legacy.DefaultConstants;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.legacy.WebUserPreferences.SavedChartsPortletPreferences;
import org.rhq.enterprise.gui.legacy.beans.ChartDataBean;
import org.rhq.enterprise.gui.legacy.beans.ChartedMetricBean;
import org.rhq.enterprise.gui.legacy.beans.NumericMetricDataPoint;
import org.rhq.enterprise.gui.legacy.exception.ParameterNotFoundException;
import org.rhq.enterprise.gui.legacy.util.MonitorUtils;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.auth.SessionNotFoundException;
import org.rhq.enterprise.server.measurement.AvailabilityManagerLocal;
import org.rhq.enterprise.server.measurement.AvailabilityPoint;
import org.rhq.enterprise.server.measurement.BaselineCreationException;
import org.rhq.enterprise.server.measurement.MeasurementBaselineManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementChartsManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementNotFoundException;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.measurement.uibean.BaseMetricDisplay;
import org.rhq.enterprise.server.measurement.uibean.MetricDisplaySummary;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * An <code>Action</code> that retrieves data from the BizApp to facilitate display of the various pages that provide
 * metrics summaries.
 *
 * @author Ian Springer
 */
public class ViewChartFormPrepareAction extends MetricDisplayRangeFormPrepareAction {
    private static final int NUMBER_OF_DATA_POINTS = DefaultConstants.DEFAULT_CHART_POINTS;
    private static final int DEFAULT_MAX_RESOURCES = 10;

    private final Log log = LogFactory.getLog(ViewChartFormPrepareAction.class);

    MeasurementDataManagerLocal dataManager;
    MeasurementScheduleManagerLocal scheduleManager;
    MeasurementChartsManagerLocal chartsManager;
    ResourceManagerLocal resMgr;
    ResourceGroupManagerLocal resGrpMgr;
    ResourceTypeManagerLocal resTypeMgr;

    /**
     * Retrieve data needed to display a Metrics Display Form. Respond to certain button clicks that alter the form
     * display.
     */
    @Override
    public ActionForward workflow(ComponentContext cc, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        super.workflow(cc, mapping, form, request, response);

        ViewChartForm chartForm = (ViewChartForm) form;

        Subject subject = WebUtility.getSubject(request);

        int groupId = -1;
        Resource resource = RequestUtils.getResource(request);
        int parent = WebUtility.getOptionalIntRequestParameter(request, "parent", -1);
        int type = WebUtility.getOptionalIntRequestParameter(request, ParamConstants.RESOURCE_TYPE_ID_PARAM, -1);
        if (resource == null) {
            // no resource? Look for a group
            groupId = WebUtility.getOptionalIntRequestParameter(request, ParamConstants.GROUP_ID_PARAM, -1);
            if ((groupId == -1) && (parent == 1)) {
                return removeBadDashboardLink(request);
            }
        }

        dataManager = LookupUtil.getMeasurementDataManager();
        scheduleManager = LookupUtil.getMeasurementScheduleManager();
        chartsManager = LookupUtil.getMeasurementChartsManager();
        resMgr = LookupUtil.getResourceManager();
        resGrpMgr = LookupUtil.getResourceGroupManager();
        resTypeMgr = LookupUtil.getResourceTypeManager();

        if (resource != null) {
            chartForm.setId(resource.getId());
            chartForm.setCategory(resource.getResourceType().getCategory().name());

            try {
                int childResourceTypeId = WebUtility.getChildResourceTypeId(request);
                chartForm.setCtype(childResourceTypeId);
                chartForm.setType(childResourceTypeId);
            } catch (ParameterNotFoundException e) {
                // This is not an autogroup.
            }
        } else if ((groupId > 0) || (parent > 0)) {
            int definitionId = WebUtility.getOptionalIntRequestParameter(request, "definitionId", -1);
            if (definitionId > 0) {
                chartForm.setM(new Integer[] { definitionId });
            }

            if (parent > 0) {
                chartForm.setCtype(type);
                chartForm.setParent(parent);
                Resource parentResource = resMgr.getResourceById(subject, parent);
                ResourceType childType = resTypeMgr.getResourceTypeById(subject, type);
                String name = parentResource.getName() + "/" + childType.getName();
                chartForm.setChartName(name);
            }

            if (groupId > 0) {
                chartForm.setGroupId(groupId);
                Group group = resGrpMgr.getResourceGroupById(subject, groupId, GroupCategory.COMPATIBLE);
                chartForm.setChartName(group.getName());
            }
        }

        // These private methods have side-effects and must be
        // called in this order. Lame, I know, but I wanted to
        // have this stuff in easier-to-manage code blocks (JW).
        _setupDateRange(request, chartForm);
        _setupMetricIds(request, chartForm);
        List<List<Resource>> resources = null;
        List<Resource> allResources = null;
        String[] r = request.getParameterValues("r");
        String[] resourceIdsParam = request.getParameterValues("resourceIds");

        if (resource != null) {
            resources = _setupResources(request, chartForm, resource, subject);
            allResources = resources.get(0);
        } else if ((groupId > 0) || (parent > 0)) {
            if (groupId > 0) {
                allResources = resGrpMgr.findResourcesForResourceGroup(subject, groupId, GroupCategory.COMPATIBLE);
            } else {
                allResources = resGrpMgr.findResourcesForAutoGroup(subject, parent, type);
            }

            List<Resource> checked = new ArrayList<Resource>(DEFAULT_MAX_RESOURCES);
            int i = 0;

            Integer[] resourceIds = chartForm.getResourceIds();
            List<Integer> resIds;

            if (resourceIds != null) {
                resIds = Arrays.asList(resourceIds);
            } else {
                /* Form does not have the list of participating resources so we need
                 * to add it to the form so that they will all be checked at the bottom of the ui
                 * Use the 'r' parameters if supplied, otherwise, all group resources
                 */
                if ((null != r) && (r.length > 0)) {
                    resourceIds = new Integer[r.length];
                    resIds = new ArrayList<Integer>();
                    for (int j = 0; j < r.length; ++j) {
                        if (j < DEFAULT_MAX_RESOURCES) {
                            Integer resourceId = 0;
                            try {
                                resourceId = Integer.valueOf(r[j]);
                            } catch (NumberFormatException e) {
                                // this should not happen, resource ids should be guaranteed ints
                                resourceId = -1;
                            }
                            resIds.add(resourceId);
                            resourceIds[j] = resourceId;
                        }
                    }
                } else {
                    int allResourcesSize = allResources.size();
                    resourceIds = new Integer[allResourcesSize];
                    resIds = new ArrayList<Integer>();
                    for (int j = 0; j < allResourcesSize; j++) {
                        if (j < DEFAULT_MAX_RESOURCES) {
                            Integer resourceId = allResources.get(j).getId();
                            resIds.add(resourceId);
                            resourceIds[j] = resourceId;
                        }
                    }
                }

                chartForm.setResourceIds(resourceIds);
            }

            for (Resource res : allResources) {
                if (i < DEFAULT_MAX_RESOURCES) {
                    if (resIds.contains(res.getId())) {
                        checked.add(res);
                        i++;
                    }
                } else {
                    break;
                }
            }

            resources = new ArrayList<List<Resource>>(2);
            resources.add(allResources);
            resources.add(checked);

            request.setAttribute("resources", allResources.toArray(new Resource[allResources.size()]));
            request.setAttribute("resourcesSize", allResources.size());

            // TODO fill in the right ones depending on what was selected
            request.setAttribute("checkedResources", checked.toArray(new Resource[checked.size()]));
            request.setAttribute("checkedResourcesSize", checked.size());
        } else if (((r != null) && (r.length > 0)) || ((resourceIdsParam != null) && (resourceIdsParam.length > 0))) // multiple scathered resources
        {
            /*
             * We have different paths to get here. One is that only r or only resourceIds are filled. In that case,
             * just display everything they have. If r and resourceIds are filled, then it means that r is the list of
             * all resources and resourceIds the ones that were checked.
             */

            if ((resourceIdsParam != null) && (r == null)) {
                r = resourceIdsParam;
            }

            int length = r.length;
            resources = new ArrayList<List<Resource>>(2);
            int[] resIds = new int[length];
            for (int i = 0; i < length; i++) {
                resIds[i] = Integer.parseInt(r[i]);
            }

            allResources = resMgr.findResourceByIds(subject, resIds, false, PageControl.getUnlimitedInstance());
            resources.add(allResources);

            // now see which ones are checked
            List<Resource> checked = new ArrayList<Resource>(allResources.size());
            if ((resourceIdsParam != null) && (resourceIdsParam.length > 0)) {
                for (String tmp : resourceIdsParam) {
                    // get Resource from the allResources list as it needs to be in it and we don't need to
                    // go to the backend again.
                    int id = Integer.parseInt(tmp);
                    for (Resource re : allResources) {
                        if (re.getId() == id) {
                            checked.add(re);
                            break;
                        }
                    }
                }
            } else {
                checked.addAll(allResources); // they are all checked :)
            }

            resources.add(checked);
            request.setAttribute("resources", allResources.toArray(new Resource[allResources.size()]));
            request.setAttribute("resourcesSize", allResources.size());

            // TODO checkedResources can be at most 10 !!
            request.setAttribute("checkedResources", checked.toArray(new Resource[checked.size()]));
            request.setAttribute("checkedResourcesSize", checked.size());
        } else {
            resources = new ArrayList<List<Resource>>(2);
            allResources = new ArrayList<Resource>(); // TODO what are we?
            resources.add(allResources);
            resources.add(allResources);
        }

        if (allResources.isEmpty()) {
            return removeBadDashboardLink(request);
        }

        try {
            List<Resource> checkedResources = resources.get(1);
            _setupMetricData(request, chartForm, checkedResources, subject);
        } catch (MeasurementNotFoundException e) {
            return removeBadDashboardLink(request);
        }

        request.setAttribute("canSaveChart", "true");

        _setupPageData(request, chartForm, allResources, subject);
        _setupBaselineExpectedRange(request, chartForm, allResources.get(0), subject);

        _setupParentResources(request, subject);

        return null;
    }

    private void _setupParentResources(HttpServletRequest request, Subject subject) {
        Resource[] resources = (Resource[]) request.getAttribute("resources");
        for (int i = 0; i < resources.length; i++) {
            Resource resource = resources[i];
            Resource parent = resMgr.getParentResource(resource.getId());
            resource.setParentResource(parent);
        }
    }

    private ActionForward removeBadDashboardLink(HttpServletRequest request) throws Exception, SessionNotFoundException {
        // This was probably a bad favorites chart
        String query = request.getQueryString();
        WebUser user = SessionUtils.getWebUser(request.getSession());
        WebUserPreferences preferences = user.getWebPreferences();
        SavedChartsPortletPreferences savedCharts = preferences.getSavedChartsPortletPreferences();
        savedCharts.removeByURL(query);
        return null;
    }

    private void _setupDateRange(HttpServletRequest request, ViewChartForm chartForm) {
        // Decide what timeframe we're showing - it may have been
        // shifted on previous views of this page.
        MetricRange range = (MetricRange) request.getAttribute(ParamConstants.METRIC_RANGE);
        if (null == range) {
            // This is the first time out.
            range = new MetricRange();
            range.setBegin(chartForm.getStartDate().getTime());
            range.setEnd(chartForm.getEndDate().getTime());
            range.shiftNow();
            request.setAttribute(ParamConstants.METRIC_RANGE, range);
        }

        // Since we have two ways to adjust the range of data we're
        // looking at (paging back and forth and explicitly choosing
        // something in the display range tile), we will try to always
        // keep the display range tile "up to date".  That is, if the
        // end date is "now", we'll select "last n" and otherwise
        // we'll select "date range".
        chartForm.synchronizeDisplayRange();
    }

    private void _setupMetricIds(HttpServletRequest request, ViewChartForm chartForm) {
        String[] m = request.getParameterValues(ParamConstants.METRIC_ID_PARAM);
        Integer[] metricIds = ArrayUtil.stringToInteger(m);
        chartForm.setM(metricIds);
        String[] origM = request.getParameterValues("origM");
        if ((origM != null) && (origM.length != 0)) {
            Integer[] originallySelectedMetricIds = ArrayUtil.stringToInteger(origM);
            chartForm.setOrigM(originallySelectedMetricIds);
        } else {
            chartForm.setOrigM(chartForm.getM().clone());
        }
    }

    private List<List<Resource>> _setupResources(HttpServletRequest request, ViewChartForm chartForm,
        Resource resource, Subject subject) throws Exception {
        List<List<Resource>> resources = new ArrayList<List<Resource>>();
        List<Resource> allResources = new ArrayList<Resource>();
        resources.add(allResources);
        List<Resource> checkedResources = new ArrayList<Resource>();
        resources.add(checkedResources);
        if ((chartForm.getCtype() != null) && chartForm.getCtype() != -1) {
            // It's an autogroup - get the child resources...
            List<Resource> childResources = new ArrayList<Resource>();

            //ab.findChildResources( sessionId, adeId,;
            //                  atid,
            //                 PageControl.PAGE_ALL ); // TODO
            Integer[] resourceIds = RequestUtils.getResourceIds(request);

            // if we've been passed a list of resource ids, we are
            // comparing metrics and need to prune out all but the
            // resources corresponding to the passed-in resource ids
            if (resourceIds != null) {
                log.debug("r=" + StringUtil.arrayToString(resourceIds));
                for (Resource childResource : childResources) {
                    boolean found = false;
                    for (Integer resourceId : resourceIds) {
                        if (childResource.getId() == resourceId) {
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        log.debug("removing resource: " + childResource.getId());
                        childResources.remove(childResource); // TODO (ips): Use Iterator#remove() instead to avoid ConcurrentModification exceptions.
                    }
                }
            }
        } else {
            // It's a single resource.
            allResources.add(resource);
        }

        request.setAttribute("resources", allResources.toArray(new Resource[allResources.size()]));
        request.setAttribute("resourcesSize", allResources.size());

        // If no specific resourceIds were checked, checkedResources is the same as
        // resources and chartForm.resourceIds contains all resource ids.
        Integer[] resourceIds = RequestUtils.getResourceIds(request);
        chartForm.setResourceIds(resourceIds);
        if ((null == resourceIds) || (resourceIds.length == 0)) {
            int maxResources = _getMaxResources(request, allResources.size());
            log.debug("maxResources=" + maxResources);
            checkedResources.addAll(allResources.subList(0, maxResources));
            Integer[] rids = new Integer[checkedResources.size()];
            for (int i = 0; i < rids.length; ++i) {
                rids[i] = checkedResources.get(i).getId();
            }

            chartForm.setResourceIds(rids);
            if (log.isDebugEnabled()) {
                log.debug("no resourceIds specified: " + StringUtil.arrayToString(rids));
            }
        } else {
            for (Integer resourceId : resourceIds) {
                for (Resource aResource : allResources) {
                    if (aResource.getId() == resourceId) {
                        checkedResources.add(resource);
                    }
                }
            }

            if (log.isDebugEnabled()) {
                log.debug("resourceIds specified: " + StringUtil.arrayToString(resourceIds));
            }
        }

        request.setAttribute("checkedResources", checkedResources.toArray(new Resource[checkedResources.size()]));
        request.setAttribute("checkedResourcesSize", checkedResources.size());

        return resources;
    }

    private void _setupMetricData(HttpServletRequest request, ViewChartForm chartForm, List<Resource> resources,
        Subject subject) throws Exception {
        AvailabilityManagerLocal availabilityManager = LookupUtil.getAvailabilityManager();
        //EventsBoss eventsBoss = ContextUtils.getEventsBoss(getServlet().getServletContext());
        //List eventPointsList = new ArrayList(resources.length);

        // Get data for charts and put it in session.  In reality only
        // one of either resources or metrics can have more than one
        // entry, so it's really not as much of a nested loop as it
        // seems.  However, the code is written this way so that it
        // can be used in both the multi-resource and the multi-metric
        // case.
        Integer[] metricDefIds = chartForm.getM();
        if (log.isTraceEnabled()) {
            log.trace("number of metrics: " + metricDefIds.length);
            log.trace("number of resources: " + resources.size());
        }
        String[] chartKeys = new String[metricDefIds.length];
        List<List<List<NumericMetricDataPoint>>> chartDataPointsListList = new ArrayList<List<List<NumericMetricDataPoint>>>(
            metricDefIds.length);
        boolean displayAvailability = false;
        List<List<NumericMetricDataPoint>> availabilityChartDataPointsList = new ArrayList<List<NumericMetricDataPoint>>();
        List<Integer> metricDefIdList = new ArrayList<Integer>();

        for (int i = 0; i < metricDefIds.length; i++) {
            int metricDefId = metricDefIds[i];
            List<List<NumericMetricDataPoint>> chartDataPointsList = new ArrayList<List<NumericMetricDataPoint>>(
                resources.size());

            // TODO get eventPointsList

            // Use current time concatenated with metric definition id for key.
            chartKeys[i] = String.valueOf(System.currentTimeMillis()) + metricDefId;
            request.getSession().setAttribute(chartKeys[i], new ChartDataBean(chartDataPointsList));
            metricDefIdList.add(metricDefId);
            chartDataPointsListList.add(chartDataPointsList);
        }

        int[] metricDefinitionIds = new int[metricDefIdList.size()];
        for (int i = 0; i < metricDefIdList.size(); i++) {
            metricDefinitionIds[i] = metricDefIdList.get(i);
        }

        long startDate = chartForm.getStartDate().getTime();
        long endDate = chartForm.getEndDate().getTime();
        for (Resource resource : resources) {
            List<List<MeasurementDataNumericHighLowComposite>> metricDataPointsList = dataManager
                .getMeasurementDataForResource(subject, resource.getId(), metricDefinitionIds, startDate, endDate,
                    NUMBER_OF_DATA_POINTS);
            if (log.isDebugEnabled()) {
                log.debug("Found " + metricDataPointsList.size() + " data points.");
                if (log.isTraceEnabled()) {
                    log.trace("data: " + metricDataPointsList);
                }
            }

            if (displayAvailability) {
                List<AvailabilityPoint> availabilityPoints = availabilityManager.getAvailabilitiesForResource(subject,
                    resource.getId(), startDate, endDate, NUMBER_OF_DATA_POINTS, false);
                List<NumericMetricDataPoint> chartDataPoints = new ArrayList<NumericMetricDataPoint>(availabilityPoints
                    .size());
                for (AvailabilityPoint availabilityPoint : availabilityPoints) {
                    NumericMetricDataPoint chartDataPoint = new NumericMetricDataPoint(availabilityPoint);
                    chartDataPoints.add(chartDataPoint);
                }

                availabilityChartDataPointsList.add(chartDataPoints);
            }

            for (int i = 0; i < metricDataPointsList.size(); i++) {
                List<MeasurementDataNumericHighLowComposite> metricDataPoints = metricDataPointsList.get(i); // data points for a single metric on a single resource
                int metricId = metricDefinitionIds[i];
                if (log.isDebugEnabled()) {
                    log.debug("mtid=" + metricId + ", rid=" + resource.getId() + ", startDate=" + startDate
                        + ", endDate=" + endDate);
                }

                // TODO: ispringer: don't transform from Composite to DataPoint, just get the UI to display the composite directly
                List<NumericMetricDataPoint> chartDataPoints = new ArrayList<NumericMetricDataPoint>(metricDataPoints
                    .size());
                for (MeasurementDataNumericHighLowComposite metricDataPoint : metricDataPoints) {
                    NumericMetricDataPoint chartDataPoint = new HighLowMetricValue(metricDataPoint);
                    chartDataPoints.add(chartDataPoint);
                }

                List<List<NumericMetricDataPoint>> chartDataPointsList = chartDataPointsListList.get(i);
                chartDataPointsList.add(chartDataPoints);
            }

        }

        /*
         * We need one List<Event> per graph that we are drawing -- they need to be paired with the
         * MetricDataPoints
         */

        request.getSession().setAttribute(AttrConstants.CHART_DATA_KEYS, chartKeys);
        request.getSession().setAttribute(AttrConstants.CHART_DATA_KEYS_SIZE, chartKeys.length);

    }

    private static final class BaseMetricDisplayComparator implements Comparator<BaseMetricDisplay> {
        public int compare(BaseMetricDisplay bmd1, BaseMetricDisplay bmd2) {
            return bmd1.getLabel().compareTo(bmd2.getLabel());
        }
    }

    private static final BaseMetricDisplayComparator comp = new BaseMetricDisplayComparator();

    private void _setupPageData(HttpServletRequest request, ViewChartForm chartForm, List<Resource> resources,
        Subject subject) throws Exception {
        int[] metricDefinitionIds = new int[chartForm.getOrigM().length];
        for (int i = 0; i < chartForm.getOrigM().length; i++) {
            metricDefinitionIds[i] = chartForm.getOrigM()[i];
        }

        List<MetricDisplaySummary> allMetricSummaries = new ArrayList<MetricDisplaySummary>();
        for (Resource resource : resources) {
            int[] metricScheduleIds = new int[metricDefinitionIds.length];
            for (int i = 0; i < metricDefinitionIds.length; i++) {
                int definitionId = metricDefinitionIds[i];
                MeasurementSchedule schedule = scheduleManager.getMeasurementSchedule(subject, definitionId, resource
                    .getId(), false);
                metricScheduleIds[i] = schedule.getId();
            }

            List<MetricDisplaySummary> metricSummariesList = chartsManager.getMetricDisplaySummariesForResource(
                subject, resource.getId(), metricScheduleIds, chartForm.getStartDate().getTime(), chartForm
                    .getEndDate().getTime());
            MonitorUtils.formatMetrics(metricSummariesList, request.getLocale(), getResources(request));
            allMetricSummaries.addAll(metricSummariesList);
        }

        Collections.sort(allMetricSummaries, comp);
        request.setAttribute("metricSummaries", allMetricSummaries);
        request.setAttribute("metricSummariesSize", allMetricSummaries.size());

        // Create an array of charted metric beans for the metrics, and make sure it's sorted in the same order as the
        // metric summaries list.
        ChartedMetricBean[] chartedMetrics = new ChartedMetricBean[chartForm.getM().length];
        for (int i = 0; i < chartedMetrics.length; i++) {
            for (MetricDisplaySummary metricSummary : allMetricSummaries) {
                if (metricSummary.getDefinitionId().equals(chartForm.getM()[i])) {
                    MeasurementUnits units = MeasurementUnits.valueOf(metricSummary.getUnits());
                    chartedMetrics[i] = new ChartedMetricBean(metricSummary.getLabel(), units, metricSummary
                        .getCollectionType());
                    break;
                }
            }
        }

        request.setAttribute("chartedMetrics", chartedMetrics);
    }

    /**
     * Populates the form properties that are needed for the BaselineExpectedRangeParams.jsp tile, which is only
     * included in SMSR mode.
     */
    private void _setupBaselineExpectedRange(HttpServletRequest request, ViewChartForm chartForm, Resource resource,
        Subject subject) throws Exception {
        MeasurementScheduleManagerLocal scheduleManager = LookupUtil.getMeasurementScheduleManager();
        MeasurementBaselineManagerLocal baselineManager = LookupUtil.getMeasurementBaselineManager();
        int metricId = chartForm.getM()[0];

        // This tile is only present in single-metric, single-resource mode and for dynamic, numeric metrics.
        if (chartForm.getMode().equals(ParamConstants.MODE_MON_CHART_SMSR) && (metricId != 0)) {
            MeasurementSchedule schedule = scheduleManager.getMeasurementSchedule(subject, metricId, resource.getId(),
                true);
            if (schedule.getDefinition().getNumericType() != NumericType.DYNAMIC) {
                chartForm.setSuppressBaselineSection(true);
                return;
            }

            // Set the name to be displayed.
            //chartForm.setChartName(schedule.getDefinition().getName());

            // Format the baseline, old baseline, high and low ranges.
            MeasurementBaseline newBLValue = null;
            try {
                /*
                 * as a convenience to the user, let's try and calculate the baseline for the selected time-range, but
                 * don't persist it unless the user explicitly sets it (which should / will be guarded by the
                 * MANAGE_MEASUREMENTS permissions
                 *
                 * thus, since this is a system-side effect, let the overlord make the call out
                 */
                //Subject overlord = LookupUtil.getSubjectManager().getOverlord();
                newBLValue = baselineManager.calculateAutoBaseline(subject, schedule.getId(), chartForm.getStartDate()
                    .getTime(), chartForm.getEndDate().getTime(), false);
            } catch (BaselineCreationException e) {
                log.debug("Baseline could not be calculated, possibly " + " due to lack of data", e);
            }

            if (newBLValue != null) {
                chartForm.setNewBaseline(MeasurementConverter.format(newBLValue.getMean(), schedule.getDefinition()
                    .getUnits(), true));
                chartForm.setNewBaselineRaw(String.valueOf(newBLValue.getMean()));
            }

            MeasurementBaseline baselineValue = schedule.getBaseline();
            if (baselineValue != null) {
                if (baselineValue.getMean() != null) {
                    chartForm.setBaseline(MeasurementConverter.format(baselineValue.getMean(), schedule.getDefinition()
                        .getUnits(), true));
                    chartForm.setBaselineRaw(String.valueOf(baselineValue.getMean()));
                }

                if (baselineValue.getMax() != null) {
                    chartForm.setHighRange(MeasurementConverter.format(baselineValue.getMax(), schedule.getDefinition()
                        .getUnits(), true));
                    chartForm.setHighRangeRaw(String.valueOf(baselineValue.getMax()));
                }

                if (baselineValue.getMin() != null) {
                    chartForm.setLowRange(MeasurementConverter.format(baselineValue.getMin(), schedule.getDefinition()
                        .getUnits(), true));
                    chartForm.setLowRangeRaw(String.valueOf(baselineValue.getMin()));
                }
            }

            if ((chartForm.getBaseline() == null) || (chartForm.getBaseline().length() == 0)) {
                chartForm.setShowBaseline(false);
            } else {
                Boolean justSavedBaseline = (Boolean) request.getAttribute("justSavedBaseline");
                if ((justSavedBaseline != null) && justSavedBaseline) {
                    chartForm.setShowBaseline(true);

                    Boolean baselineWasNull = (Boolean) request.getAttribute("baselineWasNull");
                    if ((baselineWasNull != null) && baselineWasNull) {
                        chartForm.setShowLowRange(true);
                        chartForm.setShowHighRange(true);
                    }
                }
            }

            if ((chartForm.getHighRange() == null) || (chartForm.getHighRange().length() == 0)) {
                chartForm.setShowHighRange(false);
            } else {
                Boolean justSavedHighRange = (Boolean) request.getAttribute("justSavedHighRange");
                if ((justSavedHighRange != null) && justSavedHighRange) {
                    chartForm.setShowHighRange(true);
                }
            }

            if ((chartForm.getLowRange() == null) || (chartForm.getLowRange().length() == 0)) {
                chartForm.setShowLowRange(false);
            } else {
                Boolean justSavedLowRange = (Boolean) request.getAttribute("justSavedLowRange");
                if ((justSavedLowRange != null) && justSavedLowRange) {
                    chartForm.setShowLowRange(true);
                }
            }
        }
    }

    private int _getMaxResources(HttpServletRequest request, int allResourcesLength) {
        int maxResources = DEFAULT_MAX_RESOURCES;
        String maxResourcesS = RequestUtils.message(request, "resource.common.monitor.visibility.chart.MaxResources");
        if ((null != maxResourcesS) && !maxResourcesS.startsWith("???")) {
            try {
                maxResources = Integer.parseInt(maxResourcesS);
            } catch (NumberFormatException e) {
                log.trace("invalid resource.common.monitor.visibility.chart.MaxResources resource: " + maxResourcesS);
            }
        }

        if (maxResources > allResourcesLength) {
            maxResources = allResourcesLength;
        }

        return maxResources;
    }
}