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
package org.rhq.enterprise.server.resource;

import javax.ejb.Local;
import org.rhq.core.domain.auth.Subject;

/**
 * The boss for "high-level" operations involving {@link org.rhq.core.domain.resource.Resource}s and/or
 * {@link org.rhq.core.domain.resource.group.Group}s.
 *
 * @author Ian Springer
 */
@Local
public interface ResourceBossLocal {
    /**
     * Returns a summary of inventoried {@link org.rhq.core.domain.resource.Resource}s and
     * {@link org.rhq.core.domain.resource.group.Group}s that are viewable by the specified user. The summary includes
     * the total number of platforms, servers, services, {@link org.rhq.core.domain.resource.group.CompatibleGroup}s,
     * and {@link org.rhq.core.domain.resource.group.MixedGroup}s. Only Resources with an inventory status of COMMITTED
     * are tallied.
     *
     * @param  user a JON user
     *
     * @return a summary of inventory that is viewable by the specified user
     */
    InventorySummary getInventorySummary(Subject user);
}