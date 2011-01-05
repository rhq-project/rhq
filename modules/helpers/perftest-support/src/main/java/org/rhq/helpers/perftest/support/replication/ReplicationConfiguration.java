/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.helpers.perftest.support.replication;

import org.rhq.helpers.perftest.support.config.ExportConfiguration;

/**
 * A configuration object to describe the replication.
 *
 * @author Lukas Krejci
 */
public class ReplicationConfiguration {

    private ExportConfiguration replicationConfiguration;
       
    private ReplicaModifier modifier;

    /**
     * @return the entities
     */
    public ExportConfiguration getReplicationConfiguration() {
        return replicationConfiguration;
    }

    /**
     * @param entities the entities to set
     */
    public void setReplicationConfiguration(ExportConfiguration replicationConfiguration) {
        this.replicationConfiguration = replicationConfiguration;
    }

    /**
     * @return the modifier
     */
    public ReplicaModifier getModifier() {
        return modifier;
    }

    /**
     * @param modifier the modifier to set
     */
    public void setModifier(ReplicaModifier modifier) {
        this.modifier = modifier;
    }
}
