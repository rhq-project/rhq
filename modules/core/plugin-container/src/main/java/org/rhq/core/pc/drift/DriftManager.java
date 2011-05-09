package org.rhq.core.pc.drift;

import org.rhq.core.pc.ContainerService;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.agent.AgentService;

public class DriftManager extends AgentService implements ContainerService {
    public DriftManager() {
        super(DriftAgentService.class);
    }

    @Override
    public void setConfiguration(PluginContainerConfiguration configuration) {
    }

    @Override
    public void initialize() {
    }

    @Override
    public void shutdown() {
    }
}
