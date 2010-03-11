/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.plugin.SummaryCounts;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceBossGWTService;
import org.rhq.enterprise.server.resource.InventorySummary;
import org.rhq.enterprise.server.resource.ResourceBossLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author John Sanda
 */
public class ResourceBossGWTServiceImpl extends AbstractGWTServiceImpl implements ResourceBossGWTService {

    private ResourceBossLocal resourceBoss = LookupUtil.getResourceBoss();

    public SummaryCounts getInventorySummaryForLoggedInUser() {
        Subject subject = getSessionSubject();
        return getInventorySummary(subject);
    }

    public SummaryCounts getInventorySummary(Subject user) {
        InventorySummary inventorySummary = resourceBoss.getInventorySummary(user);
        SummaryCounts counts = new SummaryCounts();
        counts.setPlatformCount(inventorySummary.getPlatformCount());
        counts.setServerCount(inventorySummary.getServerCount());

        return counts;
    }
}
