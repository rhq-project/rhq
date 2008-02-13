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
package org.rhq.enterprise.server.alert;

import javax.ejb.Local;
import org.rhq.core.domain.event.alert.AlertDampeningEvent;

@Local
public interface AlertDampeningManagerLocal {
    public AlertDampeningEvent getLatestEventByAlertDefinitionId(int alertDefinitionId);

    /**
     * Performs processing as needed for the type of event on the given AlertDefinition. If the condition set for an
     * alert definition has been satisfied, and the chosen dampening rule has also been satisfied, it will fire an
     * alert. In most instances, this can be calculated immediately and synchronously at the time this method is called.
     * However, in at least one instance, this can not be known immediately. Thus, in this case, the method will start a
     * timer to keep track of when it should fire an alert in the future. Subsequent calls to this method will either
     * update or delete the timer as appropriate to satisfy the business semantics of the supported dampening rules.
     */
    public void processEventType(int alertDefinitionId, AlertDampeningEvent.Type eventType);
}