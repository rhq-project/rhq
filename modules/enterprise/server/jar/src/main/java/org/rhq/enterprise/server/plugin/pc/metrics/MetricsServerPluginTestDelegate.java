/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation version 2 of the License.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program; if not, write to the Free Software
 *  * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package org.rhq.enterprise.server.plugin.pc.metrics;

import java.util.List;

import org.rhq.core.domain.auth.Subject;

/**
 * @author John Sanda
 */
public interface MetricsServerPluginTestDelegate {

    void purgeRawData();

    void purge1HourData();

    void purge6HourData();

    void purge24HourData();

    void insert1HourData(List<AggregateTestData> data);

    List<AggregateTestData> find1HourData(Subject subject, int scheduleId, long startTime, long endTime);

    List<AggregateTestData> find6HourData(Subject subject, int scheduleId, long startTime, long endTime);

}
