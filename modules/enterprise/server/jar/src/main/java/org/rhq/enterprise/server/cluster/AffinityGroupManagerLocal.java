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
package org.rhq.enterprise.server.cluster;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.cluster.AffinityGroup;
import org.rhq.core.domain.cluster.Server;
import org.rhq.core.domain.cluster.composite.AffinityGroupCountComposite;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * @author Joseph Marques
 */
@Local
public interface AffinityGroupManagerLocal {
    AffinityGroup getById(Subject subject, int affinityGroupId);

    int create(Subject subject, AffinityGroup affinityGroup) throws AffinityGroupException;

    int delete(Subject subject, Integer[] affinityGroupIds);

    AffinityGroup update(Subject subject, AffinityGroup affinityGroup) throws AffinityGroupException;

    void addAgentsToGroup(Subject subject, int affinityGroupId, Integer[] agentIds);

    void removeAgentsFromGroup(Subject subject, Integer[] agentIds);

    void addServersToGroup(Subject subject, int affinityGroupId, Integer[] serverIds);

    void removeServersFromGroup(Subject subject, Integer[] serverIds);

    int getAffinityGroupCount();

    PageList<Server> getServerMembers(Subject subject, int affinityGroupId, PageControl pageControl);

    PageList<Server> getServerNonMembers(Subject subject, int affinityGroupId, PageControl pageControl);

    PageList<Agent> getAgentMembers(Subject subject, int affinityGroupId, PageControl pageControl);

    PageList<Agent> getAgentNonMembers(Subject subject, int affinityGroupId, PageControl pageControl);

    PageList<AffinityGroupCountComposite> getComposites(Subject subject, PageControl pageControl);
}
