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
package org.rhq.core.pc.event;

import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.event.transfer.EventReport;

/**
 * A thread for sending {@link EventReport}s to the Server.
 *
 * @author Ian Springer
 */
public class EventSenderRunner implements Callable<EventReport>, Runnable {
    private static final Log LOG = LogFactory.getLog(EventSenderRunner.class);

    private EventManager eventManager;

    public EventSenderRunner(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    public EventReport call() throws Exception {
        EventReport report = this.eventManager.swapReport();
        this.eventManager.sendEventReport(report);
        return report;
    }

    public void run() {
        try {
            call();
        } catch (Exception e) {
            LOG.error("Could not send Event report", e);
        }
    }
}