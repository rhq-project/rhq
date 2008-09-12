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
package org.rhq.enterprise.server.cluster;

import java.util.Map;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.cluster.PartitionEvent;
import org.rhq.core.domain.cluster.PartitionEventType;
import org.rhq.core.domain.cluster.PartitionEventDetails;
import org.rhq.core.domain.cluster.composite.FailoverListComposite;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageControl;

/**
 * 
 * 
 * @author Joseph Marques
 * @author Jay Shaughnessy
 */
@Local
public interface PartitionEventManagerLocal {

    FailoverListComposite agentPartitionEvent(Subject subject, String agentToken, PartitionEventType eventType);

    /**
     * This call performs a full repartitioning of the agent population at the time of the call.  
     * @param subject
     * @param eventType
     */
    Map<Agent, FailoverListComposite> cloudPartitionEvent(Subject subject, PartitionEventType eventType);

    /**
     * This is primarily a test entry point.
     * @param event
     */
    void deletePartitionEvent(PartitionEvent event);

    PartitionEvent getPartitionEvent(Subject subject, int partitionEventId);

    PageList<PartitionEvent> getPartitionEvents(Subject subject, PageControl pageControl);

    PageList<PartitionEventDetails> getPartitionEventDetails(Subject subject, int partitionEventId, PageControl pageControl);
}
