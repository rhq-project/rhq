package org.rhq.enterprise.server.cluster;

import javax.ejb.Local;

import org.rhq.core.domain.cluster.composite.FailoverListComposite;

@Local
public interface FailoverListManagerLocal {
    FailoverListComposite getForSingleAgent(String agentRegistrationToken);
}
