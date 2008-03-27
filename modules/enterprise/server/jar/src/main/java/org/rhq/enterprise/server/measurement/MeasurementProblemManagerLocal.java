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
package org.rhq.enterprise.server.measurement;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.ejb.Local;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.oob.MeasurementOutOfBounds;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.composite.ProblemResourceComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * A manager for working with problems such as out-of-bounds measurements.
 */
@Local
public interface MeasurementProblemManagerLocal {
    /**
     * Returns a list of all "problem resources" where a problem resource has one or more of the following statements
     * true:
     *
     * <ul>
     *   <li>it is known to be {@link AvailabilityType#DOWN down}</li>
     *   <li>one or more of its alerts were triggered</li>
     *   <li>one or more of its measurements were out-of-bounds (compared to their baselines)</li>
     * </ul>
     *
     * If one or more of those are true for any resource, and they became true at or after the given <code>
     * oldestDate</code> time (specified in epoch milliseconds), that resource is returned in the list (assuming the
     * given user has access to view that resource).
     *
     * @param  subject    the user asking for the data
     * @param  oldestDate no problems will be returned that started before this time
     * @param  pc
     *
     * @return the problems resources (only those resources visible to the user will be returned)
     */
    PageList<ProblemResourceComposite> findProblemResources(Subject subject, long oldestDate, PageControl pc);

    /**
     * Add a new {@link MeasurementOutOfBounds OOB} to the database.
     *
     * @param oob
     */
    void addMeasurementOutOfBounds(MeasurementOutOfBounds oob);

    /**
     * Remove the given {@link MeasurementOutOfBounds oob} from the database.
     *
     * @param oob
     */
    void deleteMeasurementOutOfBounds(MeasurementOutOfBounds oob);

    /**
     * Fully loads an OOB entity with the given key. This eagerly loads the schedule and resource data. Note that no
     * authorization checks are made in this method!
     *
     * @param  id identifies the OOB to load
     *
     * @return the OOB fully loaded with full schedule and resource data
     */
    MeasurementOutOfBounds loadMeasurementOutOfBounds(int oobId);

    /**
     * Find measurements that have gone out-of-bounds that occurred since the given <code>oldestDate</code>, where it is
     * specified as epoch milliseconds.
     *
     * @param  subject    the user asking for the data
     * @param  oldestDate no problems will be returned that started before this time
     * @param  pc
     *
     * @return list of current out of bounds measurements
     */
    PageList<MeasurementOutOfBounds> findAllMeasurementOutOfBounds(Subject subject, long oldestDate, PageControl pc);

    /**
     * Find measurements that have gone out-of-bounds that occurred for the given resource since the given <code>
     * oldestDate</code>, where it is specified as epoch milliseconds.
     *
     * @param  subject    the user asking for the data
     * @param  oldestDate no problems will be returned that started before this time
     * @param  resourceId only those measurements for this resource will be returned
     * @param  pc
     *
     * @return list of current out of bounds measurements
     */
    PageList<MeasurementOutOfBounds> findResourceMeasurementOutOfBounds(Subject subject, long oldestDate,
        int resourceId, PageControl pc);

    /**
     * Find measurements that have gone out-of-bounds that occurred for the specified measurement schedule since the
     * given <code>oldestDate</code>, where it is specified as epoch milliseconds. This, in effect, tells you all the
     * out-of-bounds that occurred for a specific resource's specific measurement.
     *
     * @param  subject    the user asking for the data
     * @param  oldestDate no problems will be returned that started before this time
     * @param  scheduleId only those measurements for this resource-specific schedule will be returned
     * @param  pc
     *
     * @return list of current out of bounds measurements
     */
    PageList<MeasurementOutOfBounds> findScheduleMeasurementOutOfBounds(Subject subject, long oldestDate,
        int scheduleId, PageControl pc);

    int findScheduleMeasurementOutOfBoundsCount(long begin, long end, int scheduleId);

    public int findMeasurementOutOfBoundCountForDefinitionAndResources(long begin, long end, int definitionId,
        Collection<Resource> resources);

    /**
     * Get the oob counts for the passed schedules at once and stuff them in a map<scheduleId,oobCount>
     */
    public Map<Integer, Integer> getMeasurementSchedulesOOBCount(long begin, long end, List<Integer> scheduleIds);

    /**
     * Used to purge old OOB data.
     * @param purgeAfter any OOB data older than this epoch time will be purged
     * @return number of purged OOB records
     * @throws SQLException
     */
    int purgeMeasurementOOBs(Date purgeAfter);
}