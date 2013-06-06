/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.enterprise.startup.deployment;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentUnit;

/**
 * Marker for RHQ EAR. Deployment Unit Processors will only process RHQ EAR sub deployments.
 *
 * @author Thomas Segismont
 */
class RhqDeploymentMarker {
    private static final AttachmentKey<RhqDeploymentMarker> MARKER = AttachmentKey.create(RhqDeploymentMarker.class);

    private RhqDeploymentMarker() {
        // Defensive
    }

    static void mark(DeploymentUnit unit) {
        unit.putAttachment(MARKER, new RhqDeploymentMarker());
    }

    static boolean isRhqDeployment(DeploymentUnit unit) {
        DeploymentUnit deploymentUnit = unit;
        if (deploymentUnit.getParent() != null) {
            do {
                deploymentUnit = deploymentUnit.getParent();
            } while (deploymentUnit.getParent() != null);
        }
        return deploymentUnit.hasAttachment(MARKER);
    }
}
