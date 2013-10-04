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

package org.rhq.coregui.server.gwt;

import org.rhq.core.domain.resource.composite.PlatformMetricsSummary;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.gwt.PlatformUtilizationGWTService;
import org.rhq.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.resource.PlatformUtilizationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author jsanda
 */
public class PlatformUtilizationGWTServiceImpl extends AbstractGWTServiceImpl implements PlatformUtilizationGWTService {

    private static final long serialVersionUID = 1L;

    private PlatformUtilizationManagerLocal platformUtilizationMgr = LookupUtil.getPlatformUtilizationManager();

    @Override
    public PageList<PlatformMetricsSummary> loadPlatformMetrics() {
        try {
            return SerialUtility.prepare(platformUtilizationMgr.loadPlatformMetrics(getSessionSubject()), "");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }
}
