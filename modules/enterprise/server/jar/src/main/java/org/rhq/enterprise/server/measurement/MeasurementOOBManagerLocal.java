/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.enterprise.server.measurement;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.MeasurementBaseline;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.composite.MeasurementOOBComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.server.metrics.domain.AggregateNumericMetric;

/**
 * Interface for the OOB Manager
 * @see org.rhq.enterprise.server.measurement.MeasurementOOBManagerBean
 *
 * @author Heiko W. Rupp
 */
@Local
public interface MeasurementOOBManagerLocal {

    /**
     * Compute oobs from the values in the 1h measurement table that just got added.
     * For the total result, this is an incremental computation. The idea is that
     * it gets run *directly* after the 1h compression (and the baseline recalculation too).
     * @param subject Subject of the caller
     * @param begin Start time of the 1h entries to look at
     */
    void computeOOBsFromHourBeginingAt(Subject subject, long begin);

    /**
     * Computes OOBs using the provided 1 hr data which should be the most recent 1 hr
     * aggregates. These metrics are provided as an argument as opposed to querying for
     * them because we already have the 1 hr aggregates load in memory when metrics
     * aggregation runs prior to calculating OOBs.
     *
     * @param subject
     * @param metrics The most recent 1 hr aggregates
     */
    void computeOOBsForLastHour(Subject subject, Iterable<AggregateNumericMetric> metrics);

    /**
     * Determines and calculates an OOB if necessary, If an OOB is generated, this method
     * saves it to the database.
     * <br/><br/>
     * <strong>Note</strong> This method exists only for transaction demarcation.
     *
     * @param metric The 1 hr metric that is used to determine whether or not an OOB should
     *               be generated
     * @return 1 if an OOB is generated, 0 otherwise
     */
    int calculateOOB(AggregateNumericMetric metric, MeasurementBaseline baseline);

    /**
     * Return OOB Composites that contain all information about the OOBs in a given time as aggregates.
     * @param subject The caller
     * @param metricNameFilter
     * @param resourceNameFilter a resource name to filter for
     * @param parentNameFilter a parent resource name to filter for   @return List of schedules with the corresponing oob aggregates
     * @param pc PageControl to do pagination
     */
    PageList<MeasurementOOBComposite> getSchedulesWithOOBs(Subject subject, String metricNameFilter,
        String resourceNameFilter, String parentNameFilter, PageControl pc);

    /**
     * Computes the OOBs for the last hour.
     * This is done by getting the latest timestamp of the 1h table and invoking
     * #computeOOBsFromHourBeginingAt
     * @param subject caller
     */
    void computeOOBsFromLastHour(Subject subject);

    /**
     * Remove all OOB data for the passed schedule
     * @param subject Caller
     * @param sched the schedule for which we want to clean out the data
     */
    void removeOOBsForSchedule(Subject subject, MeasurementSchedule sched);

    void removeOOBsForGroupAndDefinition(Subject subject, int resourceGroupId, int measurementDefinitionId);

    /**
     * Returns the highest n OOBs for the passed resource id within the last 72h
     * @param subject caller
     * @param resourceId the resource we are interested in
     * @param n max number of entries wanted
     * @return
     */
    PageList<MeasurementOOBComposite> getHighestNOOBsForResource(Subject subject, int resourceId, int n);

    /**
     * Returns the highest n OOBs for the passed group id within the last 72h
     * @param subject caller
     * @param groupId the resource we are interested in
     * @param n max number of entries wanted
     * @return
     */
    PageList<MeasurementOOBComposite> getHighestNOOBsForGroup(Subject subject, int groupId, int n);
}
