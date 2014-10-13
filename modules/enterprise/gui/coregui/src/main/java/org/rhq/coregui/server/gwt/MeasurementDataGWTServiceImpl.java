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
package org.rhq.coregui.server.gwt;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.CallTimeDataCriteria;
import org.rhq.core.domain.criteria.MeasurementDataTraitCriteria;
import org.rhq.core.domain.criteria.MeasurementDefinitionCriteria;
import org.rhq.core.domain.criteria.MeasurementScheduleCriteria;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementBaseline;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.calltime.CallTimeDataComposite;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.measurement.composite.MeasurementOOBComposite;
import org.rhq.core.domain.measurement.composite.MeasurementScheduleComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.gwt.MeasurementDataGWTService;
import org.rhq.coregui.client.util.Moment;
import org.rhq.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.measurement.CallTimeDataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementBaselineManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementOOBManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.measurement.util.MeasurementUtils;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Simeon Pinder
 * @author Greg Hinkle
 */
public class MeasurementDataGWTServiceImpl extends AbstractGWTServiceImpl implements MeasurementDataGWTService {

    private static final long serialVersionUID = 1L;

    private MeasurementDataManagerLocal dataManager = LookupUtil.getMeasurementDataManager();
    private MeasurementOOBManagerLocal measurementOOBManager = LookupUtil.getOOBManager();
    private MeasurementBaselineManagerLocal measurementBaselineManager = LookupUtil.getMeasurementBaselineManager();

    private MeasurementScheduleManagerLocal scheduleManager = LookupUtil.getMeasurementScheduleManager();
    private MeasurementDefinitionManagerLocal definitionManager = LookupUtil.getMeasurementDefinitionManager();

    private CallTimeDataManagerLocal calltimeManager = LookupUtil.getCallTimeDataManager();

    public PageList<CallTimeDataComposite> findCallTimeDataForContext(EntityContext context,
        CallTimeDataCriteria criteria) {
        try {
            PageList<CallTimeDataComposite> value = calltimeManager.findCallTimeDataForContext(getSessionSubject(),
                context, criteria);
            return SerialUtility.prepare(value, "MeasurementDataService.findCallTimeDataForContext");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public List<MeasurementDataTrait> findCurrentTraitsForResource(int resourceId, DisplayType displayType)
        throws RuntimeException {
        try {
            return SerialUtility.prepare(
                dataManager.findCurrentTraitsForResource(getSessionSubject(), resourceId, displayType),
                "MeasurementDataService.findCurrentTraitsForResource");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public Set<MeasurementData> findLiveData(int resourceId, int[] definitionIds) throws RuntimeException {
        try {
            return SerialUtility.prepare(dataManager.findLiveData(getSessionSubject(), resourceId, definitionIds),
                "MeasurementDataService.findLiveData");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public Set<MeasurementData> findLiveDataForGroup(int groupId, int resourceId[], int[] definitionIds)
        throws RuntimeException {
        try {
            return SerialUtility.prepare(
                dataManager.findLiveDataForGroup(getSessionSubject(), groupId, resourceId, definitionIds),
                "MeasurementDataService.findLiveDataForGroup");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public List<List<MeasurementDataNumericHighLowComposite>> findDataForResource(int resourceId, int[] definitionIds,
        Moment beginTime, Moment endTime, int numPoints) throws RuntimeException {
        try {
            return SerialUtility.prepare(dataManager.findDataForResource(getSessionSubject(), resourceId,
                definitionIds, beginTime.toDate().getTime(), endTime.toDate().getTime(), numPoints),
                "MeasurementDataService.findDataForResource");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public List<List<MeasurementDataNumericHighLowComposite>> findDataForResourceForLast(int resourceId,
        int[] definitionIds, int lastN, int unit, int numPoints) throws RuntimeException {
        List<Long> beginEnd = MeasurementUtils.calculateTimeFrame(lastN, unit);
        try {
            return SerialUtility.prepare(dataManager.findDataForResource(getSessionSubject(), resourceId,
                definitionIds, beginEnd.get(0), beginEnd.get(1), numPoints),
                "MeasurementDataService.findDataForResourceForLast");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public List<List<MeasurementDataNumericHighLowComposite>> findDataForCompatibleGroup(int groupId,
        int[] definitionIds, Moment beginTime, Moment endTime, int numPoints) throws RuntimeException {
        try {
            //iterate over each of the definitionIds to retrieve the display data for each.
            List<List<MeasurementDataNumericHighLowComposite>> results = new ArrayList<List<MeasurementDataNumericHighLowComposite>>();
            for (int nextDefinitionId : definitionIds) {
                results.addAll(dataManager.findDataForCompatibleGroup(getSessionSubject(), groupId, nextDefinitionId,
                    beginTime.toDate().getTime(), endTime.toDate().getTime(), numPoints));
            }
            return SerialUtility.prepare(results, "MeasurementDataService.findDataForCompatibleGroup");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public List<List<MeasurementDataNumericHighLowComposite>> findDataForCompatibleGroupForLast(int groupId,
        int[] definitionIds, int lastN, int unit, int numPoints) throws RuntimeException {
        List<Long> beginEnd = MeasurementUtils.calculateTimeFrame(lastN, unit);
        return findDataForCompatibleGroup(groupId, definitionIds, new Moment(new Date(beginEnd.get(1))), new Moment(
            new Date(beginEnd.get(1))), numPoints);
    }

    public PageList<MeasurementDefinition> findMeasurementDefinitionsByCriteria(MeasurementDefinitionCriteria criteria)
        throws RuntimeException {
        try {
            return SerialUtility.prepare(
                definitionManager.findMeasurementDefinitionsByCriteria(getSessionSubject(), criteria),
                "MeasurementDataService.findMeasurementDefinintionsByCriteria");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public PageList<MeasurementSchedule> findMeasurementSchedulesByCriteria(MeasurementScheduleCriteria criteria)
        throws RuntimeException {
        try {
            return SerialUtility.prepare(scheduleManager.findSchedulesByCriteria(getSessionSubject(), criteria),
                "MeasurementDataService.findMeasurementSchedulesByCriteria");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public PageList<MeasurementScheduleComposite> getMeasurementScheduleCompositesByContext(EntityContext context)
        throws RuntimeException {
        try {
            return SerialUtility.prepare(
                scheduleManager.getMeasurementScheduleCompositesByContext(getSessionSubject(), context,
                    PageControl.getUnlimitedInstance()),
                "MeasurementDataService.getMeasurementScheduleCompositesByContext");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public PageList<MeasurementOOBComposite> getSchedulesWithOOBs(String metricNameFilter, String resourceNameFilter,
        String parentNameFilter, PageControl pc) throws RuntimeException {
        try {
            return SerialUtility.prepare(measurementOOBManager.getSchedulesWithOOBs(getSessionSubject(),
                metricNameFilter, resourceNameFilter, parentNameFilter, pc),
                "MeasurementDataService.getSchedulesWithOOBs");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public PageList<MeasurementOOBComposite> getHighestNOOBsForResource(int resourceId, int n) throws RuntimeException {
        try {
            return SerialUtility.prepare(
                measurementOOBManager.getHighestNOOBsForResource(getSessionSubject(), resourceId, n),
                "MeasurementDataService.getHighestNOOBsForResource");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public PageList<MeasurementOOBComposite> getHighestNOOBsForGroup(int groupId, int n) throws RuntimeException {
        try {
            return SerialUtility.prepare(
                measurementOOBManager.getHighestNOOBsForGroup(getSessionSubject(), groupId, n),
                "MeasurementDataService.getHighestNOOBsForGroup");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public void enableSchedulesForResource(int resourceId, int[] measurementDefinitionIds) throws RuntimeException {
        try {
            scheduleManager.enableSchedulesForResource(getSessionSubject(), resourceId, measurementDefinitionIds);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public void disableSchedulesForResource(int resourceId, int[] measurementDefinitionIds) throws RuntimeException {
        try {
            scheduleManager.disableSchedulesForResource(getSessionSubject(), resourceId, measurementDefinitionIds);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public void updateSchedulesForResource(int resourceId, int[] measurementDefinitionIds, long collectionInterval)
        throws RuntimeException {
        try {
            scheduleManager.updateSchedulesForResource(getSessionSubject(), resourceId, measurementDefinitionIds,
                collectionInterval);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public void enableSchedulesForCompatibleGroup(int resourceGroupId, int[] measurementDefinitionIds)
        throws RuntimeException {
        try {
            scheduleManager.enableSchedulesForCompatibleGroup(getSessionSubject(), resourceGroupId,
                measurementDefinitionIds);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public void disableSchedulesForCompatibleGroup(int resourceGroupId, int[] measurementDefinitionIds)
        throws RuntimeException {
        try {
            scheduleManager.disableSchedulesForCompatibleGroup(getSessionSubject(), resourceGroupId,
                measurementDefinitionIds);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public void updateSchedulesForCompatibleGroup(int resourceGroupId, int[] measurementDefinitionIds,
        long collectionInterval) throws RuntimeException {
        try {
            scheduleManager.updateSchedulesForCompatibleGroup(getSessionSubject(), resourceGroupId,
                measurementDefinitionIds, collectionInterval);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public void enableSchedulesForResourceType(int[] measurementDefinitionIds, boolean updateExistingSchedules)
        throws RuntimeException {
        try {
            scheduleManager.enableSchedulesForResourceType(getSessionSubject(), measurementDefinitionIds,
                updateExistingSchedules);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public void disableSchedulesForResourceType(int[] measurementDefinitionIds, boolean updateExistingSchedules)
        throws RuntimeException {
        try {
            scheduleManager.disableSchedulesForResourceType(getSessionSubject(), measurementDefinitionIds,
                updateExistingSchedules);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public void updateSchedulesForResourceType(int[] measurementDefinitionIds, long collectionInterval,
        boolean updateExistingSchedules) throws RuntimeException {
        try {
            scheduleManager.updateSchedulesForResourceType(getSessionSubject(), measurementDefinitionIds,
                collectionInterval, updateExistingSchedules);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public PageList<MeasurementDataTrait> findTraitsByCriteria(MeasurementDataTraitCriteria criteria)
        throws RuntimeException {
        try {
            PageList<MeasurementDataTrait> results = dataManager.findTraitsByCriteria(getSessionSubject(), criteria);
            if (!results.isEmpty() && null != results.get(0).getSchedule()
                && null != results.get(0).getSchedule().getResource()) {
                List<Resource> resources = new ArrayList<Resource>(results.size());
                for (MeasurementDataTrait result : results) {
                    Resource res = result.getSchedule().getResource();
                    if (null != res) {
                        resources.add(res);
                    }
                }
                ObjectFilter.filterFieldsInCollection(resources, ResourceGWTServiceImpl.importantFieldsSet);
            }

            return SerialUtility.prepare(results, "MeasurementDataService.findTraitsByCriteria");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public MeasurementBaseline getBaselineForResourceAndSchedule(int resourceId, int definitionId)
        throws RuntimeException {
        try {
            MeasurementSchedule scheduleWithBaseline = scheduleManager.getSchedule(getSessionSubject(), resourceId,
                definitionId, true);

            return SerialUtility.prepare(scheduleWithBaseline.getBaseline(), "MeasurementSchedule.getSchedule");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void setUserBaselineMax(int resourceId, int definitionId, Double maxBaseline) throws RuntimeException {
        try {
            if (null == maxBaseline || maxBaseline.isNaN()) {
                throw new IllegalArgumentException("Invalid baseline value, must not be null or NaN.");
            }

            MeasurementSchedule scheduleWithBaseline = scheduleManager.getSchedule(getSessionSubject(), resourceId,
                definitionId, true);
            if (null == scheduleWithBaseline || null == scheduleWithBaseline.getBaseline()) {
                throw new IllegalStateException(
                    "A baseline has not yet been generated for this metric. It must exist before a manual override can be set.  An initial baseline is typically generated within a few hours after metric data begins for the metric.");
            }

            MeasurementBaseline measurementBaseline = scheduleWithBaseline.getBaseline();
            if (maxBaseline < measurementBaseline.getMin()) {
                throw new IllegalArgumentException("Invalid baselineMax value, must not be less than baselineMin.");
            }

            MeasurementBaseline newMeasurementBaseline = scheduleWithBaseline.getBaseline();
            newMeasurementBaseline.setSchedule(scheduleWithBaseline);
            newMeasurementBaseline.setUserEntered(true);
            newMeasurementBaseline.setMax(maxBaseline);
            scheduleManager.updateSchedule(getSessionSubject(), scheduleWithBaseline);

        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void setUserBaselineMin(int resourceId, int definitionId, Double minBaseline) throws RuntimeException {
        try {
            if (null == minBaseline || minBaseline.isNaN()) {
                throw new IllegalArgumentException("Invalid baseline value, must not be null or NaN.");
            }

            MeasurementSchedule scheduleWithBaseline = scheduleManager.getSchedule(getSessionSubject(), resourceId,
                definitionId, true);
            if (null == scheduleWithBaseline || null == scheduleWithBaseline.getBaseline()) {
                throw new IllegalStateException(
                    "A baseline has not yet been generated for this metric. It must exist before a manual override can be set.  An initial baseline is typically generated within a few hours after metric data begins for the metric.");
            }

            MeasurementBaseline measurementBaseline = scheduleWithBaseline.getBaseline();
            if (minBaseline > measurementBaseline.getMax()) {
                throw new IllegalArgumentException("Invalid baseline value, must not be greater than baselineMax.");
            }

            MeasurementBaseline newMeasurementBaseline = scheduleWithBaseline.getBaseline();
            newMeasurementBaseline.setSchedule(scheduleWithBaseline);
            newMeasurementBaseline.setUserEntered(true);
            newMeasurementBaseline.setMin(minBaseline);
            scheduleManager.updateSchedule(getSessionSubject(), scheduleWithBaseline);

        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void setUserBaselineMean(int resourceId, int definitionId, Double meanBaseline) throws RuntimeException {
        try {
            if (null == meanBaseline || meanBaseline.isNaN()) {
                throw new IllegalArgumentException("Invalid baseline value, must not be null or NaN.");
            }

            MeasurementSchedule scheduleWithBaseline = scheduleManager.getSchedule(getSessionSubject(), resourceId,
                definitionId, true);
            if (null == scheduleWithBaseline || null == scheduleWithBaseline.getBaseline()) {
                throw new IllegalStateException(
                    "A baseline has not yet been generated for this metric. It must exist before a manual override can be set.  An initial baseline is typically generated within a few hours after metric data begins for the metric.");
            }

            MeasurementBaseline measurementBaseline = scheduleWithBaseline.getBaseline();
            if (meanBaseline < measurementBaseline.getMin() || meanBaseline > measurementBaseline.getMax()) {
                throw new IllegalArgumentException(
                    "Invalid baseline value, must not be less than baselineMin or greater than baselineMax.");
            }

            MeasurementBaseline newMeasurementBaseline = scheduleWithBaseline.getBaseline();
            newMeasurementBaseline.setSchedule(scheduleWithBaseline);
            newMeasurementBaseline.setUserEntered(true);
            newMeasurementBaseline.setMean(meanBaseline);
            scheduleManager.updateSchedule(getSessionSubject(), scheduleWithBaseline);

        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public MeasurementBaseline calcBaselineForDateRange(Integer measurementScheduleId, Moment startDate, Moment endDate)
        throws RuntimeException {
        try {
            return SerialUtility.prepare(measurementBaselineManager.calculateAutoBaselineInNewTransaction(
                getSessionSubject(), measurementScheduleId, startDate.toDate().getTime(), endDate.toDate().getTime(),
                false), "MeasurementDataService.calcBaselineForDateRange");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

}
