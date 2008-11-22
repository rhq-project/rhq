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

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.ResourceAvailability;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;

/**
 * A manager that provides methods for manipulating and querying the cached current availability for resources.
 *
 * @author Joseph Marques
 */
@Stateless
public class ResourceAvailabilityManagerBean implements ResourceAvailabilityManagerLocal {
    @SuppressWarnings("unused")
    private final Log log = LogFactory.getLog(ResourceAvailabilityManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;

    public AvailabilityType getLatestAvailabilityType(Subject whoami, int resourceId) {
        if (!authorizationManager.canViewResource(whoami, resourceId)) {
            throw new PermissionException("User [" + whoami.getName() + "] does not have permission to view resource");
        }
        ResourceAvailability ra = getLatestAvailability(resourceId);
        return (ra != null) ? ra.getAvailabilityType() : null;
    }

    public ResourceAvailability getLatestAvailability(int resourceId) {
        Query query = entityManager.createNamedQuery(ResourceAvailability.QUERY_FIND_BY_RESOURCE_ID);
        query.setParameter("resourceId", resourceId);
        try {
            ResourceAvailability result = (ResourceAvailability) query.getSingleResult();
            return result;
        } catch (NoResultException nre) {
            return null;
        }
    }

    public void markResourcesDownForAgent(int agentId) {
        Query query = entityManager.createNamedQuery(ResourceAvailability.UPDATE_BY_AGENT_ID);
        query.setParameter("availabilityType", AvailabilityType.DOWN);
        query.setParameter("agentId", agentId);
        query.executeUpdate();
    }

}