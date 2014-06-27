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
package org.rhq.enterprise.server.remote;

import java.util.Map;

import javax.management.ObjectName;

import org.rhq.core.util.ObjectNameFactory;
import org.rhq.enterprise.communications.ServiceContainer;
import org.rhq.enterprise.server.remote.RemoteSafeInvocationHandlerMetrics.Calltime;

public interface RemoteSafeInvocationHandlerMetricsMBean {
    /**
     * The name of this metric collector MBean as it will be registered under by the service container
     */
    ObjectName OBJECTNAME_METRICS = ObjectNameFactory.create(ServiceContainer.JMX_DOMAIN + ":type=RemoteApiMetrics");

    /**
     * Clears the metrics data, starting all values back to 0 as if starting fresh.
     */
    void clear();

    /**
     * Returns the total number of invocations that were received but failed to be processed succesfully.
     *
     * @return count of failed invocations
     */
    long getNumberFailedInvocations();

    /**
     * Returns the total number of invocations that were received and processed successfully.
     *
     * @return count of invocations successfully processed
     */
    long getNumberSuccessfulInvocations();

    /**
     * Returns the sum of all invocations received, successful or not.
     *
     * @return total number of invocations received
     */
    long getNumberTotalInvocations();

    /**
     * Returns the average execution time (in milliseconds) it took to execute all
     * {@link #getNumberSuccessfulInvocationsReceived() successful invocations received}.
     *
     * @return average execute time for all successful invocations.
     */
    long getAverageExecutionTime();

    /**
     * Returns a map of individual invocations and their metrics such
     * as number of times invoked, min/max/avg execution times.
     *
     * @return calltime data
     */
    Map<String, Calltime> getCallTimeData();

    /**
     * This obtains the same calltime data as {@link #getCallTimeData()} except the
     * map that is returned contains only primitive objects so remote clients do not have
     * to have this MBean class definition in their classloaders. The order of the map
     * values is important - each element in the array is as follows:
     *
     * <ol>
     * <li>count</li>
     * <li>successes</li>
     * <li>failures</li>
     * <li>minimum execution time</li>
     * <li>maximum execution time</li>
     * <li>average execution time</li>
     * </ol>
     *
     * @return the calltime data stored in a map containing primitive arrays. Keyed on API name.
     */
    Map<String, long[]> getCallTimeDataAsPrimitives();
}
