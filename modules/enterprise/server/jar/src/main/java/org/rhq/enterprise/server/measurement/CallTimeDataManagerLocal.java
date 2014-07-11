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

import java.util.Set;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.CallTimeDataCriteria;
import org.rhq.core.domain.measurement.calltime.CallTimeData;
import org.rhq.core.domain.measurement.calltime.CallTimeDataComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * The manager for call-time metric data.
 */
@Local
public interface CallTimeDataManagerLocal extends CallTimeDataManagerRemote {
    void addCallTimeData(Set<CallTimeData> callTimeDataSet);

    PageList<CallTimeDataComposite> findCallTimeDataForCompatibleGroup(Subject subject, int groupId, long beginTime,
        long endTime, PageControl pageControl);

    PageList<CallTimeDataComposite> findCallTimeDataForAutoGroup(Subject subject, int parentResourceId,
        int childResourceTypeId, long beginTime, long endTime, PageControl pageControl);

    PageList<CallTimeDataComposite> findCallTimeDataForContext(Subject subject, EntityContext context,
        CallTimeDataCriteria criteria);

    PageList<CallTimeDataComposite> findCallTimeDataForContext(Subject subject, EntityContext context, long beginTime,
        long endTime, String destination, PageControl pageControl);

    /*
     * internal methods that are exposed here so as to enable finer-grained manipulation of transactional boundaries
     */
    void insertCallTimeDataKeys(Set<CallTimeData> callTimeDataSet);

    void insertCallTimeDataValues(Set<CallTimeData> callTimeDataSet);
}
