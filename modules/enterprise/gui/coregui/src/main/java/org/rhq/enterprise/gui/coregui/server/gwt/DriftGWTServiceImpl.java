/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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

import org.rhq.core.domain.criteria.DriftChangeSetCriteria;
import org.rhq.core.domain.criteria.DriftCriteria;
import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.gwt.DriftGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.drift.DriftManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Jay Shaughnessy
 */
public class DriftGWTServiceImpl extends AbstractGWTServiceImpl implements DriftGWTService {
    private static final long serialVersionUID = 1L;

    private DriftManagerLocal driftManager = LookupUtil.getDriftManager();

    @Override
    public int deleteDrifts(int[] driftIds) throws RuntimeException {
        try {
            // TODO
            //return this.driftManager.deleteDrifts(getSessionSubject(), driftIds);
            return 0;
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public PageList<DriftChangeSet> findDriftChangeSetsByCriteria(DriftChangeSetCriteria criteria)
        throws RuntimeException {
        try {
            PageList<DriftChangeSet> result = this.driftManager.findDriftChangeSetsByCriteria(getSessionSubject(),
                criteria);
            return SerialUtility.prepare(result, "DriftService.findDriftChangeSetsByCriteria");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public PageList<Drift> findDriftsByCriteria(DriftCriteria criteria) throws RuntimeException {
        try {
            PageList<Drift> result = this.driftManager.findDriftsByCriteria(getSessionSubject(), criteria);
            return SerialUtility.prepare(result, "DriftService.findDriftsByCriteria");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

}