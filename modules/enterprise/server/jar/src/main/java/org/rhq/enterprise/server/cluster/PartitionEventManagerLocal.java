package org.rhq.enterprise.server.cluster;

import java.util.Map;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.cluster.PartitionEvent;
import org.rhq.core.domain.cluster.PartitionEventType;
import org.rhq.core.domain.cluster.composite.FailoverListComposite;
import org.rhq.core.domain.resource.Agent;

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

}
