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
import org.jetbrains.annotations.NotNull;

import org.jboss.annotation.ejb.TransactionTimeout;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.db.H2DatabaseType;
import org.rhq.core.db.OracleDatabaseType;
import org.rhq.core.db.PostgresqlDatabaseType;
import org.rhq.core.db.SQLServerDatabaseType;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.EventCriteria;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventDefinition;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.event.EventSource;
import org.rhq.core.domain.event.composite.EventComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheManagerLocal;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheStats;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.common.EntityContext;
import org.rhq.enterprise.server.measurement.instrumentation.MeasurementMonitor;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;

/**
 * Manager for Handling of {@link Event}s.
 * @author Heiko W. Rupp
 * @author Ian Springer
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
    private DatabaseType dbType;

    @EJB
    private AlertConditionCacheManagerLocal alertConditionCacheManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;

    @EJB
    private ResourceGroupManagerLocal resGrpMgr;

    Log log = LogFactory.getLog(EventManagerBean.class);

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
                        ps.setString(paramIndex++, event.getDetail());
                        ps.addBatch();
                    }

                    notifyAlertConditionCacheManager("addEventData", eventSource, eventData.toArray(new Event[eventData
                        .size()]));
                }
                ps.executeBatch();
            } finally {
                JDBCUtil.safeClose(ps);
            }

        } catch (Throwable t) {
            // TODO what do we want to do here ?
            log.warn("addEventData: Insert of events failed : " + t.getMessage());
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

    @NotNull
    @SuppressWarnings("unchecked")
    private List<Event> findEventsForResources(Subject subject, List<Resource> resources, long startDate, long endDate) {

        if (resources == null || resources.size() == 0)
            return new ArrayList<Event>();

        // TODO rewrite using getEvents

        Query q = entityManager.createNamedQuery(Event.FIND_EVENTS_FOR_RESOURCES_AND_TIME);
        q.setParameter("resources", resources);
        q.setParameter("start", startDate);
        q.setParameter("end", endDate);
        List<Event> ret = q.getResultList();

        return ret;
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

    public EventSeverity[] getSeverityBuckets(Subject subject, int resourceId, long begin, long end, int numBuckets) {
        if (authorizationManager.canViewResource(subject, resourceId) == false) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have permission to view event buckets for resource[id=" + resourceId + "]");
        }

        try {
            Resource res = entityManager.find(Resource.class, resourceId);
            List<Resource> resources = new ArrayList<Resource>(1);
            resources.add(res);
            return getSeverityBucketsForResources(subject, resources, begin, end, numBuckets);
        } catch (NoResultException nre) {
            return new EventSeverity[numBuckets];
        }
    }

    public EventSeverity[] getSeverityBucketsForAutoGroup(Subject subject, int parentId, int type, long begin,
        long end, int numBuckets) {

        List<Resource> resources = resGrpMgr.findResourcesForAutoGroup(subject, parentId, type);
        for (Resource res : resources) {
            if (authorizationManager.canViewResource(subject, res.getId()) == false) {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permission to view event buckets for autoGroup[parentResourceId=" + parentId
                    + ", resourceTypeId=" + type + "], root cause: missing permission to view resource[id="
                    + res.getId() + "]");
            }
        }

        return getSeverityBucketsForResources(subject, resources, begin, end, numBuckets);

    }

    public EventSeverity[] getSeverityBucketsForCompGroup(Subject subject, int groupId, long begin, long end,
        int numBuckets) {

        if (authorizationManager.canViewGroup(subject, groupId) == false) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have permission to view event buckets for resourceGroup[id=" + groupId + "]");
        }

        List<Resource> resources = resGrpMgr.findResourcesForResourceGroup(subject, groupId, GroupCategory.COMPATIBLE);
        return getSeverityBucketsForResources(subject, resources, begin, end, numBuckets);

    }

    /**
     * Provide the buckets for a timeline with the (most severe) severity for each bucket.
     * @param subject    Subject of the caller
     * @param resources  List of resources for which we want to know the data
     * @param begin      Begin date
     * @param end        End date
     * @param numBuckets Number of buckets to distribute into.
     * @return
     */
    private EventSeverity[] getSeverityBucketsForResources(Subject subject, List<Resource> resources, long begin,
        long end, int numBuckets) {
        EventSeverity[] buckets = new EventSeverity[numBuckets];
        if (resources == null || resources.size() == 0) {
            return buckets; // TODO fill with some fake severity 'none' ?
        }

        // TODO possibly rewrite query so that the db calculates the buckets (?)
        List<Event> events = findEventsForResources(subject, resources, begin, end);

        long timeDiff = end - begin;
        long timePerBucket = timeDiff / numBuckets;

        for (Event event : events) {
            long evTime = event.getTimestamp();
            evTime = evTime - begin;
            int bucket = (int) (evTime / timePerBucket);
            if (event.getSeverity().isMoreSevereThan(buckets[bucket])) {
                buckets[bucket] = event.getSeverity();
            }
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

    public int deleteEvents(Subject subject, List<Integer> eventIds) {
        if (eventIds == null || eventIds.size() == 0) {
            return 0; // nothing to delete, thus 0 were deleted
        }
        Query q = entityManager.createNamedQuery(Event.DELETE_BY_EVENT_IDS);
        q.setParameter("eventIds", eventIds);
        int deletedCount = q.executeUpdate();

        return deletedCount;
    }

    public int deleteAllEventsForResource(Subject subject, int resourceId) {
        Query q = entityManager.createNamedQuery(Event.DELETE_ALL_BY_RESOURCE);
        q.setParameter("resourceId", resourceId);
        int deletedCount = q.executeUpdate();

        return deletedCount;
    }

    public int deleteAllEventsForCompatibleGroup(Subject subject, int groupId) {
        Query q = entityManager.createNamedQuery(Event.DELETE_ALL_BY_RESOURCE_GROUP);
        q.setParameter("groupId", groupId);
        int deletedCount = q.executeUpdate();

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
        List<Object[]> rawResults = (List<Object[]>) q.getResultList();
        for (Object[] rawResult : rawResults) {
            EventSeverity severity = (EventSeverity) rawResult[0];
            long count = (Long) rawResult[1];
            results.put(severity, (int) count);
        }
        return results;
    }

    public PageList<EventComposite> findEventComposites(Subject subject, EntityContext context, long begin, long end,
        EventSeverity[] severities, String source, String detail, PageControl pc) {

        if (context.category == EntityContext.Category.Resource) {
            if (authorizationManager.canViewResource(subject, context.resourceId) == false) {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permission to view event history for resource[id=" + context.resourceId + "]");
            }
        } else if (context.category == EntityContext.Category.ResourceGroup) {
            if (authorizationManager.canViewGroup(subject, context.groupId) == false) {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permission to view event history for resourceGroup[id=" + context.groupId + "]");
            }
        } else if (context.category == EntityContext.Category.AutoGroup) {
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
        criteria.addFilterSourceName(source);
        criteria.addFilterDetail(detail);

        criteria.setPageControl(pc);

        if (context.category == EntityContext.Category.Resource) {
            criteria.addFilterResourceId(context.resourceId);
        } else if (context.category == EntityContext.Category.ResourceGroup) {
            criteria.addFilterResourceGroupId(context.groupId);
        } else if (context.category == EntityContext.Category.AutoGroup) {
            criteria.addFilterAutoGroupParentResourceId(context.parentResourceId);
            criteria.addFilterAutoGroupResourceTypeId(context.resourceTypeId);
        }

        return findEventCompositesByCriteria(subject, criteria);
    }

    @SuppressWarnings("unchecked")
    public PageList<EventComposite> findEventCompositesByCriteria(Subject subject, EventCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(criteria);
        String replacementSelectList = "" //
            + " new org.rhq.core.domain.event.composite.EventComposite( " //
            + "   event.detail," //
            + "   event.source.resource.id," //
            + "   event.source.resource.name," //
            + "   event.id," //
            + "   event.severity," //
            + "   event.source.location," //
            + "   event.timestamp ) ";
        generator.alterProjection(replacementSelectList);

        if (authorizationManager.isInventoryManager(subject) == false) {
            generator.setAuthorizationResourceFragment(CriteriaQueryGenerator.AuthorizationTokenType.RESOURCE,
                "source.resource", subject.getId());
        }

        CriteriaQueryRunner<EventComposite> queryRunner = new CriteriaQueryRunner(criteria, generator, entityManager);
        return queryRunner.execute();
    }

    @SuppressWarnings("unchecked")
    public PageList<Event> findEventsByCriteria(Subject subject, EventCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(criteria);
        if (authorizationManager.isInventoryManager(subject) == false) {
            generator.setAuthorizationResourceFragment(CriteriaQueryGenerator.AuthorizationTokenType.RESOURCE,
                "source.resource", subject.getId());
        }

        CriteriaQueryRunner<Event> queryRunner = new CriteriaQueryRunner(criteria, generator, entityManager);
        return queryRunner.execute();
    }

}
