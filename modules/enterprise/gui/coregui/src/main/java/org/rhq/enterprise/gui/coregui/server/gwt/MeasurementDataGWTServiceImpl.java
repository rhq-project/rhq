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
package org.rhq.enterprise.gui.coregui.server.gwt;

import java.util.List;
import java.util.Set;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.MeasurementDataTraitCriteria;
import org.rhq.core.domain.criteria.MeasurementDefinitionCriteria;
import org.rhq.core.domain.criteria.MeasurementScheduleCriteria;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.calltime.CallTimeDataComposite;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.measurement.composite.MeasurementOOBComposite;
import org.rhq.core.domain.measurement.composite.MeasurementScheduleComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.coregui.client.gwt.MeasurementDataGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.measurement.CallTimeDataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementOOBManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 */
public class MeasurementDataGWTServiceImpl extends AbstractGWTServiceImpl implements MeasurementDataGWTService {

    private static final long serialVersionUID = 1L;

    private MeasurementDataManagerLocal dataManager = LookupUtil.getMeasurementDataManager();
    private CallTimeDataManagerLocal callTimeDataManager = LookupUtil.getCallTimeDataManager();
    private MeasurementOOBManagerLocal measurementOOBManager = LookupUtil.getOOBManager();

    private MeasurementScheduleManagerLocal scheduleManager = LookupUtil.getMeasurementScheduleManager();
    private MeasurementDefinitionManagerLocal definitionManager = LookupUtil.getMeasurementDefinitionManager();

    public List<MeasurementDataTrait> findCurrentTraitsForResource(int resourceId, DisplayType displayType)
        throws RuntimeException {
        try {
            return SerialUtility.prepare(dataManager.findCurrentTraitsForResource(getSessionSubject(), resourceId,
                displayType), "MeasurementDataService.findCurrentTraitsForResource");
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public Set<MeasurementData> findLiveData(int resourceId, int[] definitionIds) throws RuntimeException {
        try {
            return SerialUtility.prepare(dataManager.findLiveData(getSessionSubject(), resourceId, definitionIds),
                "MeasurementDataService.findLiveData");
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public List<List<MeasurementDataNumericHighLowComposite>> findDataForResource(int resourceId, int[] definitionIds,
        long beginTime, long endTime, int numPoints) throws RuntimeException {
        try {
            return SerialUtility.prepare(dataManager.findDataForResource(getSessionSubject(), resourceId,
                definitionIds, beginTime, endTime, numPoints), "MeasurementDataService.findDataForResource");
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public PageList<CallTimeDataComposite> findCallTimeDataForResource(int scheduleId, long start, long end,
        PageControl pageControl) throws RuntimeException {
        try {
            return SerialUtility.prepare(callTimeDataManager.findCallTimeDataForResource(getSessionSubject(),
                scheduleId, start, end, pageControl), "MeasurementDataService.findCallTimeDataForResource");
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public PageList<MeasurementDefinition> findMeasurementDefinitionsByCriteria(MeasurementDefinitionCriteria criteria)
        throws RuntimeException {
        try {
            return SerialUtility.prepare(definitionManager.findMeasurementDefinitionsByCriteria(getSessionSubject(),
                criteria), "MeasurementDataService.findMeasurementDefinintionsByCriteria");
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public PageList<MeasurementSchedule> findMeasurementSchedulesByCriteria(MeasurementScheduleCriteria criteria)
        throws RuntimeException {
        try {
            return SerialUtility.prepare(scheduleManager.findSchedulesByCriteria(getSessionSubject(), criteria),
                "MeasurementDataService.findMeasurementSchedulesByCriteria");
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public PageList<MeasurementScheduleComposite> getMeasurementScheduleCompositesByContext(EntityContext context)
        throws RuntimeException {
        try {
            return SerialUtility.prepare(scheduleManager.getMeasurementScheduleCompositesByContext(getSessionSubject(),
                context, PageControl.getUnlimitedInstance()),
                "MeasurementDataService.getMeasurementScheduleCompositesByContext");
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public PageList<MeasurementOOBComposite> getSchedulesWithOOBs(String metricNameFilter, String resourceNameFilter,
        String parentNameFilter, PageControl pc) throws RuntimeException {
        try {
            return SerialUtility.prepare(measurementOOBManager.getSchedulesWithOOBs(getSessionSubject(),
                metricNameFilter, resourceNameFilter, parentNameFilter, pc),
                "MeasurementDataService.getSchedulesWithOOBs");
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public PageList<MeasurementOOBComposite> getHighestNOOBsForResource(int resourceId, int n) throws RuntimeException {
        try {
            return SerialUtility.prepare(measurementOOBManager.getHighestNOOBsForResource(getSessionSubject(),
                resourceId, n), "MeasurementDataService.getHighestNOOBsForResource");
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public void enableSchedulesForResource(int resourceId, int[] measurementDefinitionIds) throws RuntimeException {
        try {
            scheduleManager.enableSchedulesForResource(getSessionSubject(), resourceId, measurementDefinitionIds);
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public void disableSchedulesForResource(int resourceId, int[] measurementDefinitionIds) throws RuntimeException {
        try {
            scheduleManager.disableSchedulesForResource(getSessionSubject(), resourceId, measurementDefinitionIds);
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public void updateSchedulesForResource(int resourceId, int[] measurementDefinitionIds, long collectionInterval)
        throws RuntimeException {
        try {
            scheduleManager.updateSchedulesForResource(getSessionSubject(), resourceId, measurementDefinitionIds,
                collectionInterval);
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public void enableSchedulesForCompatibleGroup(int resourceGroupId, int[] measurementDefinitionIds)
        throws RuntimeException {
        try {
            scheduleManager.enableSchedulesForCompatibleGroup(getSessionSubject(), resourceGroupId,
                measurementDefinitionIds);
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public void disableSchedulesForCompatibleGroup(int resourceGroupId, int[] measurementDefinitionIds)
        throws RuntimeException {
        try {
            scheduleManager.disableSchedulesForCompatibleGroup(getSessionSubject(), resourceGroupId,
                measurementDefinitionIds);
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public void updateSchedulesForCompatibleGroup(int resourceGroupId, int[] measurementDefinitionIds,
        long collectionInterval) throws RuntimeException {
        try {
            scheduleManager.updateSchedulesForCompatibleGroup(getSessionSubject(), resourceGroupId,
                measurementDefinitionIds, collectionInterval);
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public void enableSchedulesForResourceType(int[] measurementDefinitionIds, boolean updateExistingSchedules)
        throws RuntimeException {
        try {
            scheduleManager.updateDefaultCollectionIntervalForMeasurementDefinitions(getSessionSubject(),
                measurementDefinitionIds, 0, updateExistingSchedules);
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public void disableSchedulesForResourceType(int[] measurementDefinitionIds, boolean updateExistingSchedules)
        throws RuntimeException {
        try {
            scheduleManager.updateDefaultCollectionIntervalForMeasurementDefinitions(getSessionSubject(),
                measurementDefinitionIds, -1, updateExistingSchedules);
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public void updateSchedulesForResourceType(int[] measurementDefinitionIds, long collectionInterval,
        boolean updateExistingSchedules) throws RuntimeException {
        try {
            scheduleManager.updateDefaultCollectionIntervalForMeasurementDefinitions(getSessionSubject(),
                measurementDefinitionIds, collectionInterval, updateExistingSchedules);
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public PageList<MeasurementDataTrait> findTraitsByCriteria(MeasurementDataTraitCriteria criteria)
        throws RuntimeException {
        try {
            return SerialUtility.prepare(dataManager.findTraitsByCriteria(getSessionSubject(), criteria),
                "MeasurementDataService.findTraitsByCriteria");
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }
}
