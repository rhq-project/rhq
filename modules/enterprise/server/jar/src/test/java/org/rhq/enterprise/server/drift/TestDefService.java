/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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

package org.rhq.enterprise.server.drift;

import java.util.List;

import org.rhq.common.drift.Headers;
import org.rhq.core.clientapi.agent.drift.DriftAgentService;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.drift.DriftFile;
import org.rhq.core.domain.drift.DriftSnapshot;

class TestDefService implements DriftAgentService {

    public TestDefService() {
    }

    @Override
    public boolean requestDriftFiles(int resourceId, Headers headers, List<? extends DriftFile> driftFiles) {
        return true;
    }

    @Override
    public void scheduleDriftDetection(int resourceId, DriftDefinition DriftDefinition) {

    }

    @Override
    public void detectDrift(int resourceId, DriftDefinition DriftDefinition) {
    }

    @Override
    public void unscheduleDriftDetection(int resourceId, DriftDefinition DriftDefinition) {
    }

    @Override
    public void updateDriftDetection(int resourceId, DriftDefinition DriftDefinition) {
    }

    @Override
    public void updateDriftDetection(int resourceId, DriftDefinition driftDef, DriftSnapshot driftSnapshot) {
    }

    @Override
    public void ackChangeSet(int resourceId, String driftDefName) {
    }

    @Override
    public void ackChangeSetContent(int resourceId, String driftDefName, String token) {
    }

    @Override
    public void pinSnapshot(int resourceId, String configName, DriftSnapshot snapshot) {
    }
}
