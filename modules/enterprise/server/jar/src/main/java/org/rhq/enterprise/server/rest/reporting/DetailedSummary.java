/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation version 2 of the License.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program; if not, write to the Free Software
 *  * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package org.rhq.enterprise.server.rest.reporting;

import org.rhq.core.domain.drift.DriftComplianceStatus;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.composite.ResourceInstallCount;

import static org.rhq.enterprise.server.rest.reporting.ReportFormatHelper.parseAncestry;

/**
* @author John Sanda
*/
public class DetailedSummary {
    private ResourceInstallCount installCount;
    private Resource resource;

    public DetailedSummary(ResourceInstallCount installCount, Resource resource) {
        this.installCount = installCount;
        this.resource = resource;
    }

    public String getTypeName() {
        return installCount.getTypeName();
    }

    public String getPlugin() {
        return installCount.getTypePlugin();
    }

    public String getCategory() {
        return installCount.getCategory().getDisplayName();
    }

    public String getVersion() {
        return installCount.getVersion();
    }

    public String getCount() {
        return Long.toString(installCount.getCount());
    }

    public boolean isInCompliance() {
        return installCount.isInCompliance();
    }

    public String getResourceName() {
        if (resource == null) {
            return "";
        }
        return resource.getName();
    }

    public String getAncestry() {
        if (resource == null) {
            return "";
        }
        return parseAncestry(resource.getAncestry());
    }

    public String getDescription() {
        if (resource == null) {
            return "";
        }
        return resource.getDescription();
    }

    public String getResourceTypeName() {
        if (resource == null) {
            return "";
        }
        return resource.getResourceType().getName();
    }

    public String getResourceVersion() {
        if (resource == null) {
            return "";
        }
        return resource.getVersion();
    }

    public String getCurrentAvailability() {
        if (resource == null) {
            return "";
        }
        return resource.getCurrentAvailability().getAvailabilityType().toString();
    }

    public boolean isResourceInCompliance() {
        for (DriftDefinition def : resource.getDriftDefinitions()) {
            if (def.getComplianceStatus() != DriftComplianceStatus.IN_COMPLIANCE) {
                return false;
            }
        }
        return true;
    }
}
