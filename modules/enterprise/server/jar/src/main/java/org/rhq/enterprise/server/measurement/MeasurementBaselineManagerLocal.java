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

import java.util.List;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.MeasurementBaseline;
import org.rhq.core.domain.resource.Resource;

/**
 * A manager for {@link MeasurementBaseline}s.
 *
 * @author Heiko W. Rupp
 * @author John Mazzitelli
 * @author Joseph Marques
 */
@Local
public interface MeasurementBaselineManagerLocal {
    /**
     * This is the entry point which calls out to all other *calculateAutoBaselines* methods seen
     * in this interface.  This is the method that should be called on a regular basis by some sort
     * of job scheduler / timer to determine whether baselines even need to be calculated.  This is
     * an inexpensive operation and can be run every minute if needed because it will only calculate
     * baselines for the ones that need it (as configured by the system preferences for how frequently
     * they should be recalculated, and how much data should be used in that recalculation).
     */
    void calculateAutoBaselines();

    /**
     * Calculates baselines for all scheduled measurements. The measurement data that was collected between the given
     * start and end times is used to calculate the baselines.
     *
     * @param  amountOfData  will use amountOfData to compute new min/max/mean for baselines as appropriate
     * @param  olderThanTime deletes baselines older than the time specified
     *
     * @return the time that all the baselines were computed;
     */
    long calculateAutoBaselines(long amountOfData, long olderThanTime);

    /**
     * Deletes baselines that are older than the time specified
     * @param olderThanTime deletes baselines older than the time specified
     * @return number of rows deleted
     * @throws Exception
     */
    int _calculateAutoBaselinesDELETE(long olderThanTime) throws Exception;

    /**
     * <strong>Note</strong> This method exists only for transaction demarcation.
     *
     * @return A list of schedules that do not have baselines. This list is not assumed
     * to be an exhaustive list of schedules that lack a baseline. As such, this method
     * will be called repeatedly during baseline calculations to get all of the necessary
     * schedules.
     */
    List<Integer> getSchedulesWithoutBaselines();

    /**
     * Given a list of schedules, this method calculates and stores baselines using the
     * amount of 1 hr data specified and older than the time specified.
     * <br/><br/>
     * <strong>Note</strong> This method exists only for transaction demarcation.
     *
     * @param schedules The schedules that do not yet have baselines
     * @param olderThan Use 1 hr data prior to this time
     * @param amountOfData  The amount of data to use for calculating baselines. This value
     *                      is treated as a duration. For example, a value of 259200000
     *                      would be treated as 3 days.
     */
    void calculateBaselines(List<Integer> schedules, long olderThan, long amountOfData);

    /**
     * Persists the newly calculated baselines.
     * <br/><br/>
     * <strong>Note</strong> This method exists only for transaction demarcation.
     *
     * @param baselines The baselines to persist.
     */
    void saveNewBaselines(List<MeasurementBaseline> baselines);

    MeasurementBaseline getBaselineIfEqual(Subject subject, int groupId, int definitionId);

    /**
     * Calculate a baseline value for the given metric based on the specified date range, optionally setting the
     * metric's baseline to the calculated value.
     *
     * @param  subject
     * @param  measurementScheduleId measurement id to recalc.
     * @param  startDate             begin date range
     * @param  endDate               end date range
     * @param  save                  whether or not to save the recalculated baseline
     *
     * @return the calculated baseline value
     *
     * @throws BaselineCreationException
     * @throws MeasurementNotFoundException
     */
    MeasurementBaseline calculateAutoBaseline(Subject subject, Integer measurementScheduleId, long startDate,
        long endDate, boolean save) throws BaselineCreationException, MeasurementNotFoundException;

    // internal use only for transactioning reasons
    MeasurementBaseline calculateAutoBaselineInNewTransaction(Subject subject, Integer measurementScheduleId,
        long startDate, long endDate, boolean save) throws BaselineCreationException, MeasurementNotFoundException;

    MeasurementBaseline calculateAutoBaseline(Subject subject, int groupId, int definitionId, long startDate,
        long endDate, boolean save) throws BaselineCreationException, MeasurementNotFoundException;

    // internal use only for transactioning reasons
    MeasurementBaseline calculateAutoBaselineForGroupInNewTransaction(Subject subject, int groupId, int definitionId,
        long startDate, long endDate, boolean save) throws BaselineCreationException, MeasurementNotFoundException;

    /**
     * Unset the {@link MeasurementBaseline#setUserEntered(boolean) user-entered} property in baselines so that we can
     * do autobaselines on it again.
     *
     * @param subject
     * @param resourceIds
     * @param definitionIds measurement definition IDs
     */
    void enableAutoBaselineCalculation(Subject subject, Integer[] resourceIds, Integer[] definitionIds);

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //
    // The following are shared with the Remote Interface
    //
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    /**
     * Return a list of {@link MeasurementBaseline} objects for the {@link Resource} represented by the given id.
     *
     * @param subject    the user request to view the baseline history for the given resource
     * @param resourceId the id of the resource whose baselines are to be returned
     *
     * @return a list of baselines for all measurements scheduled on the given resource
     */
    List<MeasurementBaseline> findBaselinesForResource(Subject subject, int resourceId);
}