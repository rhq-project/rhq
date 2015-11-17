/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rhq.enterprise.server.storage;

import java.util.Map;

/**
 * @author jsanda
 */
class ReplicationSettings {

    private int rhqReplicationFactor;

    private int systemAuthReplicationFactor;

    public ReplicationSettings(Map<String, Integer> replicationFactors) {
        rhqReplicationFactor = replicationFactors.get("rhq");
        systemAuthReplicationFactor = replicationFactors.get("system_auth");
    }

    public int getRhqReplicationFactor() {
        return rhqReplicationFactor;
    }

    public int getSystemAuthReplicationFactor() {
        return systemAuthReplicationFactor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ReplicationSettings that = (ReplicationSettings) o;
        return rhqReplicationFactor == that.rhqReplicationFactor &&
            systemAuthReplicationFactor == that.systemAuthReplicationFactor;
    }

    @Override
    public int hashCode() {
        int result = rhqReplicationFactor;
        return 29 * result + systemAuthReplicationFactor;
    }

    @Override
    public String toString() {
        return "ReplicationSettings{" +
            "rhqReplicationFactor=" + rhqReplicationFactor +
            ", systemAuthReplicationFactor=" + systemAuthReplicationFactor +
            '}';
    }
}
