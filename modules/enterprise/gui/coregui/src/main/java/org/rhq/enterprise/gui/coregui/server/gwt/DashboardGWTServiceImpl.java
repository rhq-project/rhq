/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.gui.coregui.server.gwt;

import org.rhq.core.domain.criteria.DashboardCriteria;
import org.rhq.core.domain.dashboard.Dashboard;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.coregui.client.gwt.DashboardGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.dashboard.DashboardManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 */
public class DashboardGWTServiceImpl extends AbstractGWTServiceImpl implements DashboardGWTService {

    private static final long serialVersionUID = 1L;

    private DashboardManagerLocal dashboardManager = LookupUtil.getDashboardManagerLocal();

    public PageList<Dashboard> findDashboardsByCriteria(DashboardCriteria criteria) throws RuntimeException {
        try {
            return SerialUtility.prepare(dashboardManager.findDashboardsByCriteria(getSessionSubject(), criteria),
                "DashboardManager.findDashboardsByCriteria");
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public Dashboard storeDashboard(Dashboard dashboard) throws RuntimeException {
        try {
            return SerialUtility.prepare(dashboardManager.storeDashboard(getSessionSubject(), dashboard),
                "DashboardManager.storeDashboard");
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public void removeDashboard(int dashboardId) throws RuntimeException {
        try {
            dashboardManager.removeDashboard(getSessionSubject(), dashboardId);
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }
}
