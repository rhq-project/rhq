package org.rhq.enterprise.server.cluster;

import java.util.List;

import javax.ejb.Local;

import org.rhq.core.domain.resource.Agent;

@Local
public interface AgentStatusManagerLocal {

    List<Agent> getAgentsWithStatusForServer(String serverName);

    void updateByAlertDefinition(int alertDefinitionId);

    void updateByMeasurementBaseline(int baselineId);

    void updateByResource(int resourceId);

    void updateByAutoBaselineCalculationJob();

}
