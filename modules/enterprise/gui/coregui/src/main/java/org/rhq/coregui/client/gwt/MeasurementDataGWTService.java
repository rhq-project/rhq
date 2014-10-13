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
package org.rhq.coregui.client.gwt;

import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

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
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.util.Moment;

/**
 * @author Greg Hinkle
 */
@RemoteServiceRelativePath("MeasurementDataGWTService")
public interface MeasurementDataGWTService extends RemoteService {

    PageList<CallTimeDataComposite> findCallTimeDataForContext(EntityContext context, CallTimeDataCriteria criteria);

    List<MeasurementDataTrait> findCurrentTraitsForResource(int resourceId, DisplayType displayType)
        throws RuntimeException;

    Set<MeasurementData> findLiveData(int resourceId, int[] definitionIds) throws RuntimeException;
    
    Set<MeasurementData> findLiveDataForGroup(int groupId, int resourceId[], int[] definitionIds) throws RuntimeException;

    List<List<MeasurementDataNumericHighLowComposite>> findDataForResource(int resourceId, int[] definitionIds,
        Moment beginTime, Moment endTime, int numPoints) throws RuntimeException;
    
    List<List<MeasurementDataNumericHighLowComposite>> findDataForResourceForLast(int resourceId, int[] definitionIds,
        int lastN, int unit, int numPoints) throws RuntimeException;

    List<List<MeasurementDataNumericHighLowComposite>> findDataForCompatibleGroup(int groupId, int[] definitionIds,
        Moment beginTime, Moment endTime, int numPoints) throws RuntimeException;
    
    List<List<MeasurementDataNumericHighLowComposite>> findDataForCompatibleGroupForLast(int groupId, int[] definitionIds,
        int lastN, int unit, int numPoints) throws RuntimeException;

    PageList<MeasurementDefinition> findMeasurementDefinitionsByCriteria(MeasurementDefinitionCriteria criteria)
        throws RuntimeException;

    PageList<MeasurementSchedule> findMeasurementSchedulesByCriteria(MeasurementScheduleCriteria criteria)
        throws RuntimeException;

    PageList<MeasurementScheduleComposite> getMeasurementScheduleCompositesByContext(EntityContext context)
        throws RuntimeException;

    PageList<MeasurementOOBComposite> getSchedulesWithOOBs(String metricNameFilter, String resourceNameFilter,
        String parentNameFilter, PageControl pc) throws RuntimeException;

    PageList<MeasurementOOBComposite> getHighestNOOBsForResource(int resourceId, int n) throws RuntimeException;

    PageList<MeasurementOOBComposite> getHighestNOOBsForGroup(int groupId, int n) throws RuntimeException;

    void enableSchedulesForResource(int resourceId, int[] measurementDefinitionIds) throws RuntimeException;

    void disableSchedulesForResource(int resourceId, int[] measurementDefinitionIds) throws RuntimeException;

    void updateSchedulesForResource(int resourceId, int[] measurementDefinitionIds, long collectionInterval)
        throws RuntimeException;

    void enableSchedulesForCompatibleGroup(int resourceGroupId, int[] measurementDefinitionIds) throws RuntimeException;

    void disableSchedulesForCompatibleGroup(int resourceGroupId, int[] measurementDefinitionIds)
        throws RuntimeException;

    void updateSchedulesForCompatibleGroup(int resourceGroupId, int[] measurementDefinitionIds, long collectionInterval)
        throws RuntimeException;

    void enableSchedulesForResourceType(int[] measurementDefinitionIds, boolean updateExistingSchedules)
        throws RuntimeException;

    void disableSchedulesForResourceType(int[] measurementDefinitionIds, boolean updateExistingSchedules)
        throws RuntimeException;

    void updateSchedulesForResourceType(int[] measurementDefinitionIds, long collectionInterval,
        boolean updateExistingSchedules) throws RuntimeException;

    PageList<MeasurementDataTrait> findTraitsByCriteria(MeasurementDataTraitCriteria criteria) throws RuntimeException;

    MeasurementBaseline getBaselineForResourceAndSchedule(int resourceId, int definitionId)  throws RuntimeException;

    void setUserBaselineMax(int resourceId, int definitionId, Double maxBaseline)  throws RuntimeException;

    void setUserBaselineMin(int resourceId, int definitionId, Double minBaseline)  throws RuntimeException;

    void setUserBaselineMean(int resourceId, int definitionId, Double meanBaseline)  throws RuntimeException;

    MeasurementBaseline calcBaselineForDateRange(Integer measurementScheduleId, Moment startDate, Moment endDate) throws RuntimeException;

}
