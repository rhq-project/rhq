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

package org.rhq.enterprise.server.purge;

import javax.ejb.Local;

/**
 * @author Thomas Segismont
 */
@Local
public interface PurgeManagerLocal {
    /**
     * Purges all availabilities that are old. The <code>oldest</code> time is the epoch milliseconds of the oldest
     * availability that is to be retained. The
     * {@link org.rhq.core.domain.measurement.Availability#getEndTime() end time} is the time that is examined. No
     * availability row with a <code>null</code>
     * {@link org.rhq.core.domain.measurement.Availability#getEndTime() end time} will ever be purged.
     *
     * @param  oldest oldest time (in epoch milliseconds) to retain; older records get purged
     * @return the number of availabilities that were purged
     */
    int purgeAvailabilities(long oldest);

    int purgeTraits(long oldest);

    /**
     * Deletes event data older than the specified time.
     *
     * @param deleteUpToTime event data older than this time will be deleted
     * @return number of deleted Events
     */
    int purgeEventData(long deleteUpToTime);

    /**
     * Deletes call-time data older than the specified time.
     *
     * @param deleteUpToTime call-time data older than this time will be deleted
     */
    int purgeCallTimeData(long deleteUpToTime);

    /**
     * Remove alerts for the specified range of time.
     */
    int deleteAlerts(long beginTime, long endTime);

    /**
     * Remove OOBs for schedules that had their baselines calculated after
     * a certain cutoff point. This is used to get rid of outdated OOB data for
     * baselines that got recalculated, as the new baselines will be 'big' enough for
     * what have been OOBs before and we don't have any baseline history.
     *
     * @param cutoffTime The reference time to determine new baselines
     */
    void removeOutdatedOOBs(long cutoffTime);

    /**
     * Deletes orphaned bundle audit messages.
     *
     * @return the number or audit messages deleted
     */
    int purgeOrphanedBundleResourceDeploymentHistory();

    /**
     * SUPPORTS JPA DRIFT SERVER PLUGIN
     * This will remove all drift files that are no longer referenced by drift entries. This is a maintenance method
     * to help reclaim space on the backend.
     *
     * @param purgeMillis orphaned drift files older than this time will be deleted
     * @return number of orphaned drift files that were removed
     */
    int purgeOrphanedDriftFilesInDatabase(long purgeMillis);

    /**
     * Deletes event data older than the specified time.
     *
     * @param deleteUpToTime event data older than this time will be deleted
     * @return number of deleted events
     */
    int purgePartitionEvents(long deleteUpToTime);
}
