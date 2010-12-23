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
package org.rhq.enterprise.gui.coregui.client.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.criteria.GroupOperationHistoryCriteria;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.operation.GroupOperationHistory;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.operation.bean.GroupOperationSchedule;
import org.rhq.core.domain.operation.bean.ResourceOperationSchedule;
import org.rhq.core.domain.operation.composite.ResourceOperationLastCompletedComposite;
import org.rhq.core.domain.operation.composite.ResourceOperationScheduleComposite;
import org.rhq.core.domain.resource.composite.DisambiguationReport;
import org.rhq.core.domain.util.PageList;

/**
 * @author Greg Hinkle
 */
public interface OperationGWTService extends RemoteService {

    PageList<ResourceOperationHistory> findResourceOperationHistoriesByCriteria(
        ResourceOperationHistoryCriteria criteria) throws RuntimeException;

    PageList<GroupOperationHistory> findGroupOperationHistoriesByCriteria(GroupOperationHistoryCriteria criteria)
        throws RuntimeException;

    List<DisambiguationReport<ResourceOperationLastCompletedComposite>> findRecentCompletedOperations(int pageSize)
        throws RuntimeException;

    List<DisambiguationReport<ResourceOperationScheduleComposite>> findScheduledOperations(int pageSize)
        throws RuntimeException;

    void invokeResourceOperation(int resourceId, String operationName, Configuration parameters, String description,
        int timeout) throws RuntimeException;

    void scheduleResourceOperation(int resourceId, String operationName, Configuration parameters, String description,
        int timeout, String cronString) throws RuntimeException;

    List<ResourceOperationSchedule> findScheduledResourceOperations(int resourceId) throws RuntimeException;

    List<GroupOperationSchedule> findScheduledGroupOperations(int groupId) throws RuntimeException;

}
