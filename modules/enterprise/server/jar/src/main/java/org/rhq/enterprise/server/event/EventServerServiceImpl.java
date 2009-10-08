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
package org.rhq.enterprise.server.event;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.server.event.EventServerService;
import org.rhq.core.domain.event.transfer.EventReport;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * The remote POJO implementation that takes an {@link Event} report from an agent and merges that data into the
 * Server's database.
 *
 * @author Ian Springer
 */
public class EventServerServiceImpl implements EventServerService {
    private Log log = LogFactory.getLog(this.getClass());

    public void mergeEventReport(EventReport report) {
        long startTime = System.currentTimeMillis();
        EventManagerLocal eventManager = LookupUtil.getEventManager();
        eventManager.addEventData(report.getEvents());
        long elapsedTime = (System.currentTimeMillis() - startTime);

        if (elapsedTime >= 10000L) {
            log.info("Performance: event report merge [" + report.getEvents().size() + "] timing: (" + elapsedTime +
                    ")ms");
        } else if (log.isDebugEnabled()) {
            log.debug("Performance: event report merge [" + report.getEvents().size() + "] timing: (" + elapsedTime +
                    ")ms");
        }
    }
}