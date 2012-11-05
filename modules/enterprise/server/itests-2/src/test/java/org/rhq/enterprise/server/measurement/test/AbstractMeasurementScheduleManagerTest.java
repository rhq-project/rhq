/*
 * RHQ Management Platform
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.enterprise.server.measurement.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.ResourceMeasurementScheduleRequest;
import org.rhq.core.domain.resource.Agent;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.measurement.MeasurementConstants;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TestAgentClient;
import org.rhq.enterprise.server.test.TestServerCommunicationsService;

/**
 * @author Jay Shaughnessy
 *
 */
public class AbstractMeasurementScheduleManagerTest extends AbstractEJB3Test {

    @Override
    public TestServerCommunicationsService prepareForTestAgents() {
        return prepareForTestAgents(new MeasurementScheduleTestServerCommunicationsService());
    }

    public static class MeasurementScheduleTestServerCommunicationsService extends TestServerCommunicationsService {

        private long expectedInterval = MeasurementConstants.MINIMUM_COLLECTION_INTERVAL_MILLIS;
        private boolean expectedIsEnabled = false;
        private boolean isTested = false;
        private List<String> failures = new ArrayList<String>();

        @Override
        public AgentClient getKnownAgentClient(Agent agent) {
            AgentClient testClient = new MeasurementScheduleTestAgentClient(agent, this);
            agentClients.put(agent, testClient);

            return testClient;
        }

        public void init() {
            expectedInterval = MeasurementConstants.MINIMUM_COLLECTION_INTERVAL_MILLIS;
            expectedIsEnabled = false;
            isTested = false;
            failures.clear();
        }

        public void setExpectedInterval(long expectedInterval) {
            this.expectedInterval = expectedInterval;
        }

        public void setExpectedIsEnabled(boolean expectedIsEnabled) {
            this.expectedIsEnabled = expectedIsEnabled;
        }

        public boolean isTested() {
            return isTested;
        }

        public boolean hasFailures() {
            return !failures.isEmpty();
        }

        public List<String> getFailures() {
            return failures;
        }

        public class MeasurementScheduleTestAgentClient extends TestAgentClient {

            public MeasurementScheduleTestAgentClient(Agent agent, TestServerCommunicationsService commService) {
                super(agent, commService);
            }

            @Override
            public void updateCollection(Set<ResourceMeasurementScheduleRequest> resourceSchedules) {
                for (ResourceMeasurementScheduleRequest rmsr : resourceSchedules) {
                    for (MeasurementScheduleRequest msr : rmsr.getMeasurementSchedules()) {
                        isTested = true;

                        if (msr.getInterval() != expectedInterval) {
                            String msg = "Illegal Schedule Interval, expected " + expectedInterval + ", got: " + msr;
                            failures.add(msg);
                        }
                        if (msr.isEnabled() != expectedIsEnabled) {
                            String msg = "Illegal Schedule isEnabled, expected " + expectedIsEnabled + ", got: " + msr;
                            failures.add(msg);
                        }
                    }
                }
            }

        }
    }

}
