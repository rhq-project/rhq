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
package org.rhq.core.clientapi.server.event;

import org.rhq.core.communications.command.annotation.Asynchronous;
import org.rhq.core.domain.event.transfer.EventReport;

/**
 * The Server-side interface that provides access to the {@link Event} reporting facilities. Agents will use this
 * interface to report new Events to the Server.
 *
 * @author Ian Springer
 */
public interface EventServerService {
    /**
     * This method is called when new {@link Event}s are to be reported from an Agent to the Server.
     *
     * @param report the report containing the Events
     */
    @Asynchronous(guaranteedDelivery = true)
    void mergeEventReport(EventReport report);
}