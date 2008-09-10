/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.cluster.instance;

import java.util.List;

import javax.ejb.Local;

import org.rhq.core.domain.cluster.AffinityGroup;
import org.rhq.core.domain.cluster.Server;
import org.rhq.core.domain.resource.Agent;

/**
 * @author Joseph Marques
 */

@Local
public interface ServerManagerLocal {

    int create(Server server);

    String getIdentity();

    List<Agent> getAgents();

    List<Agent> getAgentsWithStatus();

    void deleteServer(Server server);

    void deleteAffinityGroup(AffinityGroup affinityGroup);

    Server getServer() throws ServerNotFoundException;

    void changeHaServerModeIfNeeded();
}
