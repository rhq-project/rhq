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
package org.rhq.enterprise.server.event;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.annotation.ejb.TransactionTimeout;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.db.H2DatabaseType;
import org.rhq.core.db.OracleDatabaseType;
import org.rhq.core.db.PostgresqlDatabaseType;
import org.rhq.core.db.SQLServerDatabaseType;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.EventCriteria;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventDefinition;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.event.EventSource;
import org.rhq.core.domain.event.composite.EventComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheManagerLocal;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheStats;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.measurement.instrumentation.MeasurementMonitor;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;

/**
 * Manager for Handling of {@link Event}s.
 * @author Heiko W. Rupp
 * @author Ian Springer
 * @author Joseph Marques
 */
@Stateless
@javax.annotation.Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
public class EventManagerBean implements EventManagerLocal, EventManagerRemote {

    // NOTE: We need to do the fancy subselects to figure out the event def id, because the PC does not know the id's of
    //       metadata objects such as EventDefinition (ips, 02/20/08).
    private static final String EVENT_SOURCE_INSERT_STMT = "INSERT INTO RHQ_Event_Source (id, event_def_id, resource_id, location) "
        + "SELECT %s, (SELECT id FROM RHQ_Event_Def WHERE name = ? AND resource_type_id = (SELECT id FROM RHQ_Resource_Type WHERE name = ? AND plugin = ?)), ?, ? FROM RHQ_Numbers WHERE i = 42 "
        + "AND NOT EXISTS (SELECT * FROM RHQ_Event_Source WHERE event_def_id = (SELECT id FROM RHQ_Event_Def WHERE name = ? AND resource_type_id = (SELECT id FROM RHQ_Resource_Type WHERE name = ? AND plugin = ?)) AND resource_id = ? AND location = ?)";

    private static final String EVENT_SOURCE_INSERT_STMT_AUTOINC = "INSERT INTO RHQ_Event_Source (event_def_id, resource_id, location) "
        + "SELECT (SELECT id FROM RHQ_Event_Def WHERE name = ? AND resource_type_id = (SELECT id FROM RHQ_Resource_Type WHERE name = ? AND plugin = ?)), ?, ? FROM RHQ_Numbers WHERE i = 42 "
        + "AND NOT EXISTS (SELECT * FROM RHQ_Event_Source WHERE event_def_id = (SELECT id FROM RHQ_Event_Def WHERE name = ? AND resource_type_id = (SELECT id FROM RHQ_Resource_Type WHERE name = ? AND plugin = ?)) AND resource_id = ? AND location = ?)";

    private static final String EVENT_INSERT_STMT = "INSERT INTO RHQ_Event (id, event_source_id, timestamp, severity, detail) "
        + "VALUES (%s, (SELECT id FROM RHQ_Event_Source WHERE event_def_id = (SELECT id FROM RHQ_Event_Def WHERE name = ? AND resource_type_id = (SELECT id FROM RHQ_Resource_Type WHERE name = ? AND plugin = ?)) AND resource_id = ? AND location = ?), ?, ?, ?)";

    private static final String EVENT_INSERT_STMT_AUTOINC = "INSERT INTO RHQ_Event (event_source_id, timestamp, severity, detail) "
        + "VALUES ((SELECT id FROM RHQ_Event_Source WHERE event_def_id = (SELECT id FROM RHQ_Event_Def WHERE name = ? AND resource_type_id = (SELECT id FROM RHQ_Resource_Type WHERE name = ? AND plugin = ?)) AND resource_id = ? AND location = ?), ?, ?, ?)";

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @javax.annotation.Resource(name = "RHQ_DS")
    private DataSource rhqDs;

    @EJB
    private AlertConditionCacheManagerLocal alertConditionCacheManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;

    Log log = LogFactory.getLog(EventManagerBean.class);

    public void addEventData(Map<EventSource, Set<Event>> events) {

        if (events == null || events.size() == 0)
            return;

        String statementSql;
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = rhqDs.getConnection();
            DatabaseType dbType = DatabaseTypeFactory.getDatabaseType(conn);

            if (dbType instanceof PostgresqlDatabaseType || dbType instanceof OracleDatabaseType
                || dbType instanceof H2DatabaseType) {
                String nextvalSql = JDBCUtil.getNextValSql(conn, EventSource.TABLE_NAME);
                statementSql = String.format(EVENT_SOURCE_INSERT_STMT, nextvalSql);
            } else if (dbType instanceof SQLServerDatabaseType) {
                statementSql = EVENT_SOURCE_INSERT_STMT_AUTOINC;
            } else {
                throw new IllegalArgumentException("Unknown database type, can't continue: " + dbType);
            }

            // First insert the "keys" (i.e. the EventSources).
            ps = conn.prepareStatement(statementSql);
            try {
                for (EventSource eventSource : events.keySet()) {
                    int paramIndex = 1;
                    ps.setString(paramIndex++, eventSource.getEventDefinition().getName());
                    ps.setString(paramIndex++, eventSource.getEventDefinition().getResourceType().getName());
                    ps.setString(paramIndex++, eventSource.getEventDefinition().getResourceType().getPlugin());
                    ps.setInt(paramIndex++, eventSource.getResource().getId());
                    ps.setString(paramIndex++, eventSource.getLocation());
                    ps.setString(paramIndex++, eventSource.getEventDefinition().getName());
                    ps.setString(paramIndex++, eventSource.getEventDefinition().getResourceType().getName());
                    ps.setString(paramIndex++, eventSource.getEventDefinition().getResourceType().getPlugin());
                    ps.setInt(paramIndex++, eventSource.getResource().getId());
                    ps.setString(paramIndex++, eventSource.getLocation());

                    ps.addBatch();
                }
                ps.executeBatch();
            } finally {
                JDBCUtil.safeClose(ps);
            }

            if (dbType instanceof PostgresqlDatabaseType || dbType instanceof OracleDatabaseType
                || dbType instanceof H2DatabaseType) {
                String nextvalSql = JDBCUtil.getNextValSql(conn, Event.TABLE_NAME);
                statementSql = String.format(EVENT_INSERT_STMT, nextvalSql);
            } else if (dbType instanceof SQLServerDatabaseType) {
                statementSql = EVENT_INSERT_STMT_AUTOINC;
            } else {
                throw new IllegalArgumentException("Unknown database type, can't continue: " + dbType);
            }

            // Then insert the "values" (i.e. the Events).
            ps = conn.prepareStatement(statementSql);
            try {
                for (EventSource eventSource : events.keySet()) {
                    Set<Event> eventData = events.get(eventSource);
                    for (Event event : eventData) {
                        int paramIndex = 1;
                        ps.setString(paramIndex++, eventSource.getEventDefinition().getName());
                        ps.setString(paramIndex++, eventSource.getEventDefinition().getResourceType().getName());
                        ps.setString(paramIndex++, eventSource.getEventDefinition().getResourceType().getPlugin());
                        ps.setInt(paramIndex++, eventSource.getResource().getId());
                        ps.setString(paramIndex++, eventSource.getLocation());
                        ps.setLong(paramIndex++, event.getTimestamp());
                        ps.setString(paramIndex++, event.getSeverity().toString());
                        String detail = dbType.getString(event.getDetail(), Event.DETAIL_MAX_LENGTH);
                        ps.setString(paramIndex++, detail);
                        ps.addBatch();
                    }

                    // We may have trimmed the event detail for storage reasons, but for alerting use the
                    // full, potentially larger detail string.
                    notifyAlertConditionCacheManager("addEventData", eventSource,
                        eventData.toArray(new Event[eventData.size()]));
                }
                ps.executeBatch();
            } finally {
                JDBCUtil.safeClose(ps);
            }

        } catch (Throwable t) {
            // TODO what do we want to do here ?
            log.warn("addEventData: Insert of events failed : " + t.getMessage());
            if (t instanceof SQLException) {
                SQLException e = (SQLException) t;
                Exception e2 = e.getNextException();
                if (e2 != null)
                    log.warn("     : " + e2.getMessage());
                if (t.getCause() != null)
                    log.warn("     : " + t.getCause().getMessage());
            }
        } finally {
            JDBCUtil.safeClose(conn);
        }
    }

    private void notifyAlertConditionCacheManager(String callingMethod, EventSource source, Event... events) {
        AlertConditionCacheStats stats = alertConditionCacheManager.checkConditions(source, events);

        log.debug(callingMethod + ": " + stats.toString());
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @TransactionTimeout(6 * 60 * 60)
    public int purgeEventData(Date deleteUpToTime) throws SQLException {
        Query q = entityManager.createQuery("DELETE FROM Event e WHERE e.timestamp < :cutOff");
        q.setParameter("cutOff", deleteUpToTime.getTime());
        long startTime = System.currentTimeMillis();
        int deleted = q.executeUpdate();
        MeasurementMonitor.getMBean().incrementPurgeTime(System.currentTimeMillis() - startTime);
        MeasurementMonitor.getMBean().setPurgedEvents(deleted);
        return deleted;
    }

    public int[] getEventCounts(Subject subject, int resourceId, long begin, long end, int numBuckets) {

        int[] buckets = new int[numBuckets];

        // TODO possibly rewrite query so that the db calculates the buckets (?)
        List<EventComposite> events = findEventComposites(subject, EntityContext.forResource(resourceId), begin, end,
            null, null, null, PageControl.getUnlimitedInstance());

        long timeDiff = end - begin;
        long timePerBucket = timeDiff / numBuckets;

        for (EventComposite event : events) {
            long evTime = event.getTimestamp().getTime();
            evTime = evTime - begin;
            int bucket = (int) (evTime / timePerBucket);
            buckets[bucket]++;
        }

        return buckets;
    }

    @SuppressWarnings("unchecked")
    public EventComposite getEventDetailForEventId(Subject subject, int eventId) throws EventException {
        Query q = entityManager.createNamedQuery(Event.GET_DETAILS_FOR_EVENT_IDS);
        List<Integer> eventIds = new ArrayList<Integer>(1);
        eventIds.add(eventId);
        q.setParameter("eventIds", eventIds);
        List<EventComposite> composites = q.getResultList();
        if (composites.size() == 1)
            return composites.get(0);
        else {
            throw new EventException("No event found for eventId[" + eventId + "]");
        }
    }

    @SuppressWarnings("unchecked")
    public void deleteEventSourcesForDefinition(EventDefinition def) {
        Query q = entityManager.createNamedQuery(EventSource.QUERY_BY_EVENT_DEFINITION);
        q.setParameter("definition", def);
        List<EventSource> sources = q.getResultList();
        for (EventSource source : sources) {
            entityManager.remove(source);
        }
    }

    public int deleteEventsForContext(Subject subject, EntityContext context, List<Integer> eventIds) {
        if (eventIds == null || eventIds.size() == 0) {
            return 0; // nothing to delete, thus 0 were deleted
        }

        if (context.type == EntityContext.Type.Resource) {
            if (authorizationManager.hasResourcePermission(subject, Permission.MANAGE_EVENTS, context.resourceId) == false) {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permissions to delete events for resource[id=" + context.resourceId + "]");
            }
        } else if (context.type == EntityContext.Type.ResourceGroup) {
            if (authorizationManager.hasGroupPermission(subject, Permission.MANAGE_EVENTS, context.groupId) == false) {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permissions to delete events for resourceGroup[id=" + context.groupId + "]");
            }
        } else if (context.type == EntityContext.Type.AutoGroup) {
            if (authorizationManager.canViewAutoGroup(subject, context.parentResourceId, context.resourceTypeId) == false) {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permission to view event history for autoGroup[parentResourceId="
                    + context.parentResourceId + ", resourceTypeId=" + context.resourceTypeId + "]");
            }
        }

        Query q = entityManager.createNamedQuery(Event.DELETE_BY_EVENT_IDS);
        q.setParameter("eventIds", eventIds);
        int deletedCount = q.executeUpdate();

        return deletedCount;
    }

    public int purgeEventsForContext(Subject subject, EntityContext context) {

        if (context.type == EntityContext.Type.Resource) {
            if (authorizationManager.hasResourcePermission(subject, Permission.MANAGE_EVENTS, context.resourceId) == false) {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permissions to purge events for resource[id=" + context.resourceId + "]");
            }
        } else if (context.type == EntityContext.Type.ResourceGroup) {
            if (authorizationManager.hasGroupPermission(subject, Permission.MANAGE_EVENTS, context.groupId) == false) {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permissions to purge events for resourceGroup[id=" + context.groupId + "]");
            }
        } else {
            throw new IllegalArgumentException(context.getUnknownContextMessage());
        }

        Query purgeQuery = null;
        if (context.type == EntityContext.Type.Resource) {
            purgeQuery = entityManager.createNamedQuery(Event.DELETE_ALL_BY_RESOURCE);
            purgeQuery.setParameter("resourceId", context.resourceId);
        } else if (context.type == EntityContext.Type.ResourceGroup) {
            purgeQuery = entityManager.createNamedQuery(Event.DELETE_ALL_BY_RESOURCE_GROUP);
            purgeQuery.setParameter("groupId", context.groupId);
        }

        int deletedCount = purgeQuery.executeUpdate();

        return deletedCount;
    }

    @SuppressWarnings("unchecked")
    public Map<EventSeverity, Integer> getEventCountsBySeverity(Subject subject, int resourceId, long startDate,
        long endDate) {
        Map<EventSeverity, Integer> results = new HashMap<EventSeverity, Integer>();
        Query q = entityManager.createNamedQuery(Event.QUERY_EVENT_COUNTS_BY_SEVERITY);
        q.setParameter("resourceId", resourceId);
        q.setParameter("start", startDate);
        q.setParameter("end", endDate);
        List<Object[]> rawResults = q.getResultList();
        for (Object[] rawResult : rawResults) {
            EventSeverity severity = (EventSeverity) rawResult[0];
            long count = (Long) rawResult[1];
            results.put(severity, (int) count);
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    public Map<EventSeverity, Integer> getEventCountsBySeverityForGroup(Subject subject, int groupId, long startDate,
        long endDate) {
        Map<EventSeverity, Integer> results = new HashMap<EventSeverity, Integer>();
        Query q = entityManager.createNamedQuery(Event.QUERY_EVENT_COUNTS_BY_SEVERITY_GROUP);
        q.setParameter("groupId", groupId);
        q.setParameter("start", startDate);
        q.setParameter("end", endDate);
        List<Object[]> rawResults = q.getResultList();
        for (Object[] rawResult : rawResults) {
            EventSeverity severity = (EventSeverity) rawResult[0];
            long count = (Long) rawResult[1];
            results.put(severity, (int) count);
        }
        return results;
    }

    public EventSeverity[] getSeverityBucketsByContext(Subject subject, EntityContext context, long begin, long end,
        int bucketCount) {

        EventCriteria criteria = new EventCriteria();
        criteria.addFilterStartTime(begin);
        criteria.addFilterEndTime(end);

        /*
         * if the bucket computation is pushed into the database, it saves on data transfer across the wire. this
         * solution is currently querying N number of strings (event.severity) and N number of longs (event.timestamp),
         * where N is the number of events between 'begin' and 'end'.  if the severity buckets are computed in a single
         * query, the wire load would only be K integers, where K is the bucketCount.
         */
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        ;
        String replacementSelectList = " event.severity, event.timestamp ";
        generator.alterProjection(replacementSelectList);

        if (authorizationManager.isInventoryManager(subject) == false) {
            generator.setAuthorizationResourceFragment(CriteriaQueryGenerator.AuthorizationTokenType.RESOURCE,
                "source.resource", subject.getId());
        }

        CriteriaQueryRunner<Object[]> queryRunner = new CriteriaQueryRunner<Object[]>(criteria, generator,
            entityManager);
        PageList<Object[]> flyWeights = queryRunner.execute();

        EventSeverity[] buckets = new EventSeverity[bucketCount];
        long timeDiff = end - begin;
        long timePerBucket = timeDiff / bucketCount;

        for (Object[] nextFly : flyWeights) {
            long eventTime = (Long) nextFly[1];
            EventSeverity eventSeverity = (EventSeverity) nextFly[0];

            eventTime = eventTime - begin;
            int bucket = (int) (eventTime / timePerBucket);
            if (eventSeverity.isMoreSevereThan(buckets[bucket])) {
                buckets[bucket] = eventSeverity;
            }
        }

        return buckets;
    }

    public PageList<EventComposite> findEventComposites(Subject subject, EntityContext context, long begin, long end,
        EventSeverity[] severities, String source, String detail, PageControl pc) {

        if (context.type == EntityContext.Type.Resource) {
            if (authorizationManager.canViewResource(subject, context.resourceId) == false) {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permission to view event history for resource[id=" + context.resourceId + "]");
            }
        } else if (context.type == EntityContext.Type.ResourceGroup) {
            if (authorizationManager.canViewGroup(subject, context.groupId) == false) {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permission to view event history for resourceGroup[id=" + context.groupId + "]");
            }
        } else if (context.type == EntityContext.Type.AutoGroup) {
            if (authorizationManager.canViewAutoGroup(subject, context.parentResourceId, context.resourceTypeId) == false) {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permission to view event history for autoGroup[parentResourceId="
                    + context.parentResourceId + ", resourceTypeId=" + context.resourceTypeId + "]");
            }
        }

        EventCriteria criteria = new EventCriteria();
        criteria.addFilterStartTime(begin);
        criteria.addFilterEndTime(end);
        criteria.addFilterSeverities(severities);
        if (source != null && !source.trim().equals("")) {
            criteria.addFilterSourceName(source);
        }
        if (detail != null && !detail.trim().equals("")) {
            criteria.addFilterDetail(detail);
        }

        criteria.setPageControl(pc);

        if (context.type == EntityContext.Type.Resource) {
            criteria.addFilterResourceId(context.resourceId);
        } else if (context.type == EntityContext.Type.ResourceGroup) {
            criteria.addFilterResourceGroupId(context.groupId);
        } else if (context.type == EntityContext.Type.AutoGroup) {
            criteria.addFilterAutoGroupParentResourceId(context.parentResourceId);
            criteria.addFilterAutoGroupResourceTypeId(context.resourceTypeId);
        }

        return findEventCompositesByCriteria(subject, criteria);
    }

    public PageList<EventComposite> findEventCompositesByCriteria(Subject subject, EventCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        String replacementSelectList = "" //
            + " new org.rhq.core.domain.event.composite.EventComposite( " //
            + "   event.detail," //
            + "   event.source.resource.id," //
            + "   event.source.resource.name," //
            + "   event.source.resource.ancestry," //
            + "   event.source.resource.resourceType.id," //
            + "   event.id," //
            + "   event.severity," //
            + "   event.source.location," //
            + "   event.timestamp ) ";
        generator.alterProjection(replacementSelectList);

        if (authorizationManager.isInventoryManager(subject) == false) {
            generator.setAuthorizationResourceFragment(CriteriaQueryGenerator.AuthorizationTokenType.RESOURCE,
                "source.resource", subject.getId());
        }

        CriteriaQueryRunner<EventComposite> queryRunner = new CriteriaQueryRunner<EventComposite>(criteria, generator,
            entityManager);
        return queryRunner.execute();
    }

    @SuppressWarnings("unchecked")
    public PageList<Event> findEventsByCriteria(Subject subject, EventCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        ;
        if (authorizationManager.isInventoryManager(subject) == false) {
            generator.setAuthorizationResourceFragment(CriteriaQueryGenerator.AuthorizationTokenType.RESOURCE,
                "source.resource", subject.getId());
        }

        CriteriaQueryRunner<Event> queryRunner = new CriteriaQueryRunner(criteria, generator, entityManager);
        return queryRunner.execute();
    }

    /*
     * Methods kept around because they are part of the remote interface, but all should be treated as deprecated
     */

    public EventSeverity[] getSeverityBuckets(Subject subject, int resourceId, long begin, long end, int numBuckets) {
        return getSeverityBucketsByContext(subject, EntityContext.forResource(resourceId), begin, end, numBuckets);
    }

    public EventSeverity[] getSeverityBucketsForAutoGroup(Subject subject, int parentResourceId, int resourceTypeId,
        long begin, long end, int numBuckets) {
        return getSeverityBucketsByContext(subject, EntityContext.forAutoGroup(parentResourceId, resourceTypeId),
            begin, end, numBuckets);
    }

    public EventSeverity[] getSeverityBucketsForCompGroup(Subject subject, int resourceGroupId, long begin, long end,
        int numBuckets) {
        return getSeverityBucketsByContext(subject, EntityContext.forGroup(resourceGroupId), begin, end, numBuckets);
    }

}
