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

package org.rhq.core.domain.drift;

import java.io.Serializable;

import org.rhq.core.domain.resource.Resource;

public class DriftComposite implements Serializable {

    private static final long serialVersionUID = 1L;

    private Drift<?, ?> drift;

    private Resource resource;

    private String driftDefName;

    public DriftComposite() {
    }

    public DriftComposite(Drift<?, ?> drift, Resource resource, String driftDefName) {
        this.drift = drift;
        this.resource = resource;
        this.driftDefName = driftDefName;
    }

    public Drift<?, ?> getDrift() {
        return drift;
    }

    public void setDrift(Drift<?, ?> drift) {
        this.drift = drift;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public String getDriftDefinitionName() {
        return driftDefName;
    }

    public void setDriftDefName(String driftDefName) {
        this.driftDefName = driftDefName;
    }

}
