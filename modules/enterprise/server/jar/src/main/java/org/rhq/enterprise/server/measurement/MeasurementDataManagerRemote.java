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
package org.rhq.enterprise.server.measurement;

import java.util.List;
import java.util.Set;

import javax.ejb.Remote;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.MeasurementDataTraitCriteria;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.util.PageList;

@Remote
public interface MeasurementDataManagerRemote {

    MeasurementAggregate getAggregate(Subject subject, int scheduleId, long startTime, long endTime);

    List<MeasurementDataTrait> findTraits(Subject subject, int resourceId, int definitionId);

    List<MeasurementDataTrait> findCurrentTraitsForResource(Subject subject, int resourceId, DisplayType displayType);

    PageList<MeasurementDataTrait> findTraitsByCriteria(Subject subject, MeasurementDataTraitCriteria criteria);

    Set<MeasurementData> findLiveData(Subject subject, int resourceId, int[] definitionIds);

    Set<MeasurementData> findLiveDataForGroup(Subject subject, int groupId, int[] resourceIds, int[] definitionIds);

    List<List<MeasurementDataNumericHighLowComposite>> findDataForCompatibleGroup(Subject subject, int groupId,
        int definitionId, long beginTime, long endTime, int numPoints);

    List<List<MeasurementDataNumericHighLowComposite>> findDataForResource(Subject subject, int resourceId,
        int[] definitionIds, long beginTime, long endTime, int numPoints);
}
