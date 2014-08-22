/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
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
import org.rhq.core.db.SQLServerDatabaseType;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.ResourceAvailability;
import org.rhq.core.util.collection.ArrayUtils;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;

/**
 * A manager that provides methods for manipulating and querying the cached current availability for Resources.
 *
 * @author Joseph Marques
 */
@Stateless
public class ResourceAvailabilityManagerBean implements ResourceAvailabilityManagerLocal {

    private final Log log = LogFactory.getLog(ResourceAvailabilityManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @javax.annotation.Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
    private DataSource rhqDs;

    private DatabaseType dbType;

    @EJB
    private AuthorizationManagerLocal authorizationManager;

    @PostConstruct
    public void init() {
        dbType = DatabaseTypeFactory.getDefaultDatabaseType();
    }

    // This is rarely needed now that we get an entry in RHQ_RESOURCE_AVAIL when the resource is persisted. But
    // there are upgrade scenarios ( moving up to JON 3.1) where it can still get applied.  Once all customers
    // are on 3.1 I believe this can go away (jshaughn)
    //
    public void insertNeededAvailabilityForImportedResources(List<Integer> resourceIds) {
        // Hibernate didn't want to swallow ResourceAvailability.INSERT_BY_RESOURCE_IDS, so we had to go native.
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            String query;
            if (dbType instanceof SQLServerDatabaseType) {
                query = "" //
                    + "INSERT INTO RHQ_RESOURCE_AVAIL ( RESOURCE_ID, AVAILABILITY_TYPE ) " //
                    + "     SELECT res.ID, 2 " // set to UNKNOWN=2
                    + "       FROM RHQ_RESOURCE res " //
                    + "  LEFT JOIN RHQ_RESOURCE_AVAIL avail ON res.ID = avail.RESOURCE_ID " //
                    + "      WHERE res.ID IN ( :resourceIds ) " //
                    + "        AND avail.ID IS NULL ";
            } else {
                query = "" //
                    + "INSERT INTO RHQ_RESOURCE_AVAIL ( ID, RESOURCE_ID, AVAILABILITY_TYPE ) " //
                    + "     SELECT %s, res.ID, 2 " // set to UNKNOWN=2
                    + "       FROM RHQ_RESOURCE res " //
                    + "  LEFT JOIN RHQ_RESOURCE_AVAIL avail ON res.ID = avail.RESOURCE_ID " //
                    + "      WHERE res.ID IN ( :resourceIds ) " //
                    + "        AND avail.ID IS NULL ";

                String nextValSqlFragment;
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
            }

            conn = rhqDs.getConnection();

            // Do one query per 1000 Resource id's to prevent Oracle from failing because of an IN clause with more
            // than 1000 items.
            int[] resourceIdArray = ArrayUtils.unwrapCollection(resourceIds);
            int fromIndex = 0;
            while (fromIndex < resourceIdArray.length) {
                int toIndex = (resourceIdArray.length < (fromIndex + 1000)) ? resourceIdArray.length
                    : (fromIndex + 1000);

                int[] resourceIdSubArray = Arrays.copyOfRange(resourceIdArray, fromIndex, toIndex);
                String transformedQuery = JDBCUtil.transformQueryForMultipleInParameters(query, ":resourceIds",
                    resourceIdSubArray.length);
                ps = conn.prepareStatement(transformedQuery);
                JDBCUtil.bindNTimes(ps, resourceIdSubArray, 1);
                ps.execute();
                ps.close();
                fromIndex = toIndex;
            }
        } catch (SQLException e) {
            log.warn("Could not insert cached current availabilities for newly imported Resources: " + e);
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
        } catch (RuntimeException re) {
            Throwable cause = re.getCause();
            if (cause instanceof SQLException) {
                log.error("Failed to get latest avail for Resource [" + resourceId + "]: "
                    + JDBCUtil.convertSQLExceptionToString((SQLException) cause));
            }
            throw re;
        }
    }

    public void updateAgentResourcesLatestAvailability(int agentId, AvailabilityType availabilityType,
        boolean isPlatform) {
        Query query = entityManager.createNamedQuery((isPlatform) ? ResourceAvailability.UPDATE_PLATFORM_BY_AGENT_ID
            : ResourceAvailability.UPDATE_CHILD_BY_AGENT_ID);
        query.setParameter("availabilityType", availabilityType);
        query.setParameter("agentId", agentId);
        if (!isPlatform) {
            query.setParameter("disabled", AvailabilityType.DISABLED);
        }
        query.executeUpdate();
    }

}