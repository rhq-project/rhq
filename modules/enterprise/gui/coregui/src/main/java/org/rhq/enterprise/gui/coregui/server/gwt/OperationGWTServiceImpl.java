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
package org.rhq.enterprise.gui.coregui.server.gwt;

import org.rhq.core.domain.criteria.GroupOperationHistoryCriteria;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.operation.GroupOperationHistory;
import org.rhq.core.domain.operation.OperationHistory;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.gwt.OperationGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 */
public class OperationGWTServiceImpl extends AbstractGWTServiceImpl implements OperationGWTService {

    private OperationManagerLocal operationManager = LookupUtil.getOperationManager();

    public PageList<ResourceOperationHistory> findResourceOperationHistoriesByCriteria(ResourceOperationHistoryCriteria criteria) {
        return SerialUtility.prepare(operationManager.findResourceOperationHistoriesByCriteria(getSessionSubject(), criteria),
                "OperationService.findResourceOperationHistoriesByCriteria");

    }
    public PageList<GroupOperationHistory> findGroupOperationHistoriesByCriteria(GroupOperationHistoryCriteria criteria) {
        return SerialUtility.prepare(operationManager.findGroupOperationHistoriesByCriteria(getSessionSubject(), criteria),
                "OperationService.findGroupOperationHistoriesByCriteria");
    }

}
