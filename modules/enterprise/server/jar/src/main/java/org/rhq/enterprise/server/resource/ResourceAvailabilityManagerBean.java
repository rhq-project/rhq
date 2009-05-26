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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.db.H2DatabaseType;
import org.rhq.core.db.OracleDatabaseType;
import org.rhq.core.db.PostgresqlDatabaseType;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.ResourceAvailability;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;

/**
 * A manager that provides methods for manipulating and querying the cached current availability for resources.
 *
 * @author Joseph Marques
 */
@Stateless
@javax.annotation.Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
public class ResourceAvailabilityManagerBean implements ResourceAvailabilityManagerLocal {

    private final Log log = LogFactory.getLog(ResourceAvailabilityManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @javax.annotation.Resource(name = "RHQ_DS")
    private DataSource rhqDs;
    private DatabaseType dbType;

    @EJB
    private AuthorizationManagerLocal authorizationManager;

    @PostConstruct
    public void init() {
        Connection conn = null;
        try {
            conn = rhqDs.getConnection();
            dbType = DatabaseTypeFactory.getDatabaseType(conn);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            JDBCUtil.safeClose(conn);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void insertNeededAvailabilityForImportedResources(List<Integer> resourceIds) {
        // Hibernate diddn't want to swallow ResourceAvailability.INSERT_BY_RESOURCE_IDS, so had to go native
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            int[] values = new int[resourceIds.size()];
            for (int i = 0; i < resourceIds.size(); i++) {
                values[i] = resourceIds.get(i);
            }

            String query = "" //
                + "INSERT INTO RHQ_RESOURCE_AVAIL ( ID, RESOURCE_ID ) " //
                + "     SELECT %s, res.ID " //
                + "       FROM RHQ_RESOURCE res " //
                + "  LEFT JOIN RHQ_RESOURCE_AVAIL avail ON res.ID = avail.RESOURCE_ID " //
                + "      WHERE res.ID IN ( :resourceIds ) " //
                + "        AND avail.ID IS NULL ";

            conn = rhqDs.getConnection();
            String nextValSqlFragment = null;
            if (dbType instanceof PostgresqlDatabaseType) {
                nextValSqlFragment = "nextval('%s_id_seq'::text)";
            } else if (dbType instanceof OracleDatabaseType) {
                nextValSqlFragment = "%s_id_seq.nextval";
            } else if (dbType instanceof H2DatabaseType) {
                nextValSqlFragment = "nextval('%s_id_seq')";
            } else {
                throw new IllegalStateException("insertNeededAvailabilityForImportedResources does not support "
                    + dbType);
            }
            String nextValSql = String.format(nextValSqlFragment, ResourceAvailability.TABLE_NAME);
            query = String.format(query, nextValSql);
            query = JDBCUtil.transformQueryForMultipleInParameters(query, ":resourceIds", values.length);
            ps = conn.prepareStatement(query);
            JDBCUtil.bindNTimes(ps, values, 1);
            ps.execute();
        } catch (SQLException e) {
            log.warn("Could not create default  metrics for schedules: " + e.getMessage());
        } finally {
            JDBCUtil.safeClose(ps);
            JDBCUtil.safeClose(conn);
        }
    }

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

    public void updateAllResourcesAvailabilitiesForAgent(int agentId, AvailabilityType availabilityType) {
        Query query = entityManager.createNamedQuery(ResourceAvailability.UPDATE_BY_AGENT_ID);
        query.setParameter("availabilityType", availabilityType);
        query.setParameter("agentId", agentId);
        query.executeUpdate();
    }
}