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
package org.rhq.enterprise.server.resource.group.definition.mbean;

import java.util.Map;

import org.rhq.core.domain.resource.group.GroupDefinition;
import org.rhq.core.domain.resource.group.ResourceGroup;

/**
 * An MBean that exposes call-time metrics for the cost of recalculating
 * DynaGroups from their owning {@link GroupDefinition}s 
 * 
 * @author Joseph Marques
 */
public interface GroupDefinitionRecalculationThreadMonitorMBean {

    /**
     * Clears the metrics data, starting all values back to 0 as if starting fresh.
     */
    void clear();

    /**
     * Returns the count of the total number of group definitions currently managed by the system
     *
     * @return count of the total number of group definitions currently managed by the system
     */
    long getGroupDefinitionCount();

    /**
     * Returns the count of the number of group definitions currently set for auto-recalculation, this can never be
     *         greater than {@link #getGroupDefinitionCount()}
     *
     * @return count of the number of group definitions currently set for auto-recalculation, this can never be
     *         greater than {@link #getGroupDefinitionCount()}
     */
    long getAutoRecalculatingGroupDefinitionCount();

    /**
     * Returns the count of the number of {@link ResourceGroup}s in the system current managed by some 
     * {@link GroupDefinition}
     *
     * @return count of the number of {@link ResourceGroup}s in the system current managed by some 
     * {@link GroupDefinition}
     */
    long getDynaGroupCount();

    /**
     * Returns the time (in millis) that it took to recalculate all of the DynaGroups in the system whose owning
     *         {@link GroupDefinition} was set to automatically recalculate on a periodic basis (this metric only
     *         reflects the last known / collected time for the recalculation thread)
     *
     * @return the time (in millis) that it took to recalculate all of the DynaGroups in the system whose owning
     *         {@link GroupDefinition} was set to automatically recalculate on a periodic basis (this metric only
     *         reflects the last known / collected time for the recalculation thread)
     */
    long getAutoRecalculationThreadTime();

    /**
     * Sets the time (in millis) that it took to recalculate all of the DynaGroups in the system whose owning
     *         {@link GroupDefinition} was set to automatically recalculate on a periodic basis (this metric only
     *         reflects the last known / collected time for the recalculation thread)
     *
     *@param timeInMillis the time (in millis) that it took to recalculate all of the DynaGroups in the system whose owning
     *         {@link GroupDefinition} was set to automatically recalculate on a periodic basis (this metric only
     *         reflects the last known / collected time for the recalculation thread)
     */
    void updateAutoRecalculationThreadTime(long timeInMillis);

    /**
     * Returns a map of statistics broken down by group definition.
     * 
     * @return complex data
     */
    Map<String, Map<String, Object>> getStatistics();

    /**
     * Updates the internal {@link GroupDefinitionRecalculationThreadMonitor.GroupDefinitionRecalculationStatistic}
     * for the {@link GroupDefinition} with the given name.
     * 
     * @param groupDefinitionName the name of the {@link GroupDefinition} whose internal statistics will be updated
     * @param newDynaGroupCount the count of the number of DynaGroups managed by this {@link GroupDefinition}
     * @param success whether or not the last recalculation was successful; if successful, newDynaGroupCount will
     *                reflect the count after recalculation, otherwise it will reflect the last known count
     * @param executionTime the time (in millis) that it took to recalculate this {@link GroupDefinition}
     */
    void updateStatistic(String groupDefinitionName, int newDynaGroupCount, boolean success, long executionTime);

}