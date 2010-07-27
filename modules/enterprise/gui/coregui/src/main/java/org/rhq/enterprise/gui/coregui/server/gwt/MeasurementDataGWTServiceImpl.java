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

import org.rhq.core.domain.criteria.MeasurementDefinitionCriteria;
import org.rhq.core.domain.criteria.MeasurementScheduleCriteria;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.calltime.CallTimeDataComposite;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.gwt.MeasurementDataGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.measurement.CallTimeDataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 */
public class MeasurementDataGWTServiceImpl extends AbstractGWTServiceImpl implements MeasurementDataGWTService {

    private static final long serialVersionUID = 1L;

    private MeasurementDataManagerLocal dataManager = LookupUtil.getMeasurementDataManager();
    private CallTimeDataManagerLocal callTimeDataManager = LookupUtil.getCallTimeDataManager();

    private MeasurementScheduleManagerLocal scheduleManager = LookupUtil.getMeasurementScheduleManager();
    private MeasurementDefinitionManagerLocal definitionManager = LookupUtil.getMeasurementDefinitionManager();

    public List<MeasurementDataTrait> findCurrentTraitsForResource(int resourceId, DisplayType displayType) {
        return SerialUtility.prepare(dataManager.findCurrentTraitsForResource(getSessionSubject(), resourceId,
            displayType), "MeasurementDataService.findCurrentTraitsForResource");
    }

    public Set<MeasurementData> findLiveData(int resourceId, int[] definitionIds) {
        return SerialUtility.prepare(dataManager.findLiveData(getSessionSubject(), resourceId, definitionIds),
            "MeasurementDataService.findLiveData");
    }

    public List<List<MeasurementDataNumericHighLowComposite>> findDataForResource(int resourceId, int[] definitionIds,
        long beginTime, long endTime, int numPoints) {
        return SerialUtility.prepare(dataManager.findDataForResource(getSessionSubject(), resourceId, definitionIds,
            beginTime, endTime, numPoints), "MeasurementDataService.findDataForResource");
    }

    public PageList<CallTimeDataComposite> findCallTimeDataForResource(int scheduleId, long start, long end,
        PageControl pageControl) {
        return SerialUtility.prepare(callTimeDataManager.findCallTimeDataForResource(getSessionSubject(), scheduleId,
            start, end, pageControl), "MeasurementDataService.findCallTimeDataForResource");
    }

    public PageList<MeasurementDefinition> findMeasurementDefinitionsByCriteria(MeasurementDefinitionCriteria criteria) {
        return SerialUtility.prepare(definitionManager.findMeasurementDefinitionsByCriteria(getSessionSubject(),
            criteria), "MeasurementDataService.findMeasurementDefinintionsByCriteria");
    }

    public PageList<MeasurementSchedule> findMeasurementSchedulesByCriteria(MeasurementScheduleCriteria criteria) {
        return SerialUtility.prepare(scheduleManager.findSchedulesByCriteria(getSessionSubject(), criteria),
            "MeasurementDataService.findMeasurementSchedulesByCriteria");
    }
}
