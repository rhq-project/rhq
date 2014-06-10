/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.core.domain.resource.composite;

import java.io.Serializable;

import org.rhq.core.domain.resource.Agent;

/**
 * (resourceId, synthetic, agent) tuple. Synthetic is true if the resource in question is synthetic.
 * 
 * @author Jirka Kremser
 */
public class ResourceIdWithAgentComposite implements Serializable {

    private static final long serialVersionUID = 43L;
    private final int resourceId;
    private final Agent agent;
    private final boolean synthetic;

    public ResourceIdWithAgentComposite(int resourceId, boolean synthetic, Agent agent) {
        this.resourceId = resourceId;
        this.synthetic = synthetic;
        this.agent = agent;
    }

    public int getResourceId() {
        return resourceId;
    }

    public Agent getAgent() {
        return agent;
    }

    public boolean isSynthetic() {
        return synthetic;
    }
}
