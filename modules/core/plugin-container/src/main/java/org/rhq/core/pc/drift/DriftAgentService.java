package org.rhq.core.pc.drift;

import org.rhq.core.domain.drift.DriftConfiguration;

public interface DriftAgentService {

    void scheduleDriftDetection(int resourceId, DriftConfiguration driftConfiguration);

}
