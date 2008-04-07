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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.jetbrains.annotations.NotNull;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.db.OracleDatabaseType;
import org.rhq.core.db.PostgresqlDatabaseType;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.event.EventSource;
import org.rhq.core.domain.event.composite.EventComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheManagerLocal;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheStats;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;

/**
 * Manager for Handling of {@link Event}s.
 * @author Heiko W. Rupp
 * @author Ian Springer
 */
@Stateless
@javax.annotation.Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
public class EventManagerBean implements EventManagerLocal {

    // NOTE: We need to do the fancy subselects to figure out the event def id, because the PC does not know the id's of
    //       metadata objects such as EventDefinition (ips, 02/20/08).
    private static final String EVENT_SOURCE_INSERT_STMT = "INSERT INTO RHQ_Event_Source (id, event_def_id, resource_id, location) "
        + "SELECT %s, (SELECT id FROM RHQ_Event_Def WHERE name = ? AND resource_type_id = (SELECT id FROM RHQ_Resource_Type WHERE name = ? AND plugin = ?)), ?, ? FROM RHQ_Numbers WHERE i = 42 "
        + "AND NOT EXISTS (SELECT * FROM RHQ_Event_Source WHERE event_def_id = (SELECT id FROM RHQ_Event_Def WHERE name = ? AND resource_type_id = (SELECT id FROM RHQ_Resource_Type WHERE name = ? AND plugin = ?)) AND resource_id = ? AND location = ?)";

    private static final String EVENT_INSERT_STMT = "INSERT INTO RHQ_Event (id, event_source_id, timestamp, severity, detail) "
        + "VALUES (%s, (SELECT id FROM RHQ_Event_Source WHERE event_def_id = (SELECT id FROM RHQ_Event_Def WHERE name = ? AND resource_type_id = (SELECT id FROM RHQ_Resource_Type WHERE name = ? AND plugin = ?)) AND resource_id = ? AND location = ?), ?, ?, ?)";

    private static final int DEFAULT_EVENTS_PAGE_SIZE = 15;

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @javax.annotation.Resource(name = "RHQ_DS")
    private DataSource rhqDs;
    private DatabaseType dbType;

    @EJB
    private AlertConditionCacheManagerLocal alertConditionCacheManager;

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

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = rhqDs.getConnection();

            // First insert the "keys" (i.e. the EventSources).
            String nextvalSql = JDBCUtil.getNextValSql(conn, EventSource.TABLE_NAME);
            String statementSql = String.format(EVENT_SOURCE_INSERT_STMT, nextvalSql);
            ps = conn.prepareStatement(statementSql);
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

            // Then insert the "values" (i.e. the Events).
            nextvalSql = JDBCUtil.getNextValSql(conn, Event.TABLE_NAME);
            statementSql = String.format(EVENT_INSERT_STMT, nextvalSql);
            ps = conn.prepareStatement(statementSql);
            for (EventSource eventSource : events.keySet()) {
                Set<Event> eventData = events.get(eventSource);
                for (Event event : eventData) {
                    int paramIndex = 1;
                    ps.setString(paramIndex++, eventSource.getEventDefinition().getName());
                    ps.setString(paramIndex++, eventSource.getEventDefinition().getResourceType().getName());
                    ps.setString(paramIndex++, eventSource.getEventDefinition().getResourceType().getPlugin());
                    ps.setInt(paramIndex++, eventSource.getResource().getId());
                    ps.setString(paramIndex++, eventSource.getLocation());
                    ps.setTimestamp(paramIndex++, new java.sql.Timestamp(event.getTimestamp().getTime()));
                    ps.setString(paramIndex++, event.getSeverity().toString());
                    ps.setString(paramIndex++, event.getDetail());
                    ps.addBatch();
                }

                notifyAlertConditionCacheManager("addEventData", eventSource, eventData.toArray(new Event[0]));
            }
            ps.executeBatch();

        } catch (SQLException e) {
            // TODO what do we want to do here ?
            log.warn("addEventData: Insert of events failed : " + e.getMessage());
        } finally {
            JDBCUtil.safeClose(conn, ps, null);
        }
    }

    @SuppressWarnings("unused")
    private void notifyAlertConditionCacheManager(String callingMethod, EventSource source, Event... events) {
        AlertConditionCacheStats stats = alertConditionCacheManager.checkConditions(source, events);

        log.debug(callingMethod + ": " + stats.toString());
    }

    public int purgeEventData(Date deleteUpToTime) throws SQLException {
        Query q = entityManager.createQuery("DELETE FROM Event e WHERE e.timestamp < :cutOff");
        q.setParameter("cutOff", deleteUpToTime);
        int deleted = q.executeUpdate();
        return deleted;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public List<Event> getEventsForResources(Subject subject, List<Resource> resources, long startDate, long endDate) {

        if (resources == null || resources.size() == 0)
            return new ArrayList<Event>();

        // TODO rewrite using getEvents

        Query q = entityManager.createNamedQuery(Event.FIND_EVENTS_FOR_RESOURCES_AND_TIME);
        q.setParameter("resources", resources);
        q.setParameter("start", new Date(startDate));
        q.setParameter("end", new Date(endDate));
        List<Event> ret = q.getResultList();

        return ret;
    }

    public List<EventComposite> getEventsForAutoGroup(Subject subject, int parent, int type, long begin, long endDate,
        EventSeverity severity) {

        List<Resource> resources = resGrpMgr.getResourcesForAutoGroup(subject, parent, type);
        int[] resourceIds = new int[resources.size()];
        int i = 0;
        for (Resource res : resources)
            resourceIds[i++] = res.getId();

        PageList<EventComposite> comp = getEvents(subject, resourceIds, begin, endDate, severity, -1, null, null,
            new PageControl());
        return comp;
    }

    public List<EventComposite> getEventsForAutoGroup(Subject subject, int parent, int type, long begin, long endDate,
        EventSeverity severity, int eventId, String source, String searchString, PageControl pc) {

        List<Resource> resources = resGrpMgr.getResourcesForAutoGroup(subject, parent, type);
        int[] resourceIds = new int[resources.size()];
        int i = 0;
        for (Resource res : resources)
            resourceIds[i++] = res.getId();

        PageList<EventComposite> comp = getEvents(subject, resourceIds, begin, endDate, severity, -1, source,
            searchString, pc);

        return comp;
    }

    public List<EventComposite> getEventsForCompGroup(Subject subject, int groupId, long begin, long endDate,
        EventSeverity severity) {

        List<Resource> resources = resGrpMgr.getResourcesForResourceGroup(subject, groupId, GroupCategory.COMPATIBLE);
        int[] resourceIds = new int[resources.size()];
        int i = 0;
        for (Resource res : resources)
            resourceIds[i++] = res.getId();

        PageList<EventComposite> comp = getEvents(subject, resourceIds, begin, endDate, severity, -1, null, null,
            new PageControl());
        return comp;
    }

    public List<EventComposite> getEventsForCompGroup(Subject subject, int groupId, long begin, long endDate,
        EventSeverity severity, int eventId, String source, String searchString, PageControl pc) {

        List<Resource> resources = resGrpMgr.getResourcesForResourceGroup(subject, groupId, GroupCategory.COMPATIBLE);
        int[] resourceIds = new int[resources.size()];
        int i = 0;
        for (Resource res : resources)
            resourceIds[i++] = res.getId();

        PageList<EventComposite> comp = getEvents(subject, resourceIds, begin, endDate, severity, -1, source,
            searchString, pc);

        return comp;
    }

    public int[] getEventCounts(Subject subject, int resourceId, long begin, long end, int numBuckets) {

        int[] buckets = new int[numBuckets];

        // TODO possibly rewrite query so that the db calculates the buckets (?)
        List<EventComposite> events = getEventsForResource(subject, resourceId, begin, end, null);

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

        List<Resource> resources = resGrpMgr.getResourcesForAutoGroup(subject, parentId, type);
        return getSeverityBucketsForResources(subject, resources, begin, end, numBuckets);

    }

    public EventSeverity[] getSeverityBucketsForCompGroup(Subject subject, int groupId, long begin, long end,
        int numBuckets) {

        List<Resource> resources = resGrpMgr.getResourcesForResourceGroup(subject, groupId, GroupCategory.COMPATIBLE);
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
        List<Event> events = getEventsForResources(subject, resources, begin, end);

        long timeDiff = end - begin;
        long timePerBucket = timeDiff / numBuckets;

        for (Event event : events) {
            long evTime = event.getTimestamp().getTime();
            evTime = evTime - begin;
            int bucket = (int) (evTime / timePerBucket);
            if (event.getSeverity().isMoreSevereThan(buckets[bucket]))
                buckets[bucket] = event.getSeverity();
        }

        return buckets;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public List<EventComposite> getEventsForResource(Subject subject, int resourceId, long startDate, long endDate,
        EventSeverity severity) {

        PageList<EventComposite> comp = getEvents(subject, new int[] { resourceId }, startDate, endDate, severity, -1,
            null, null, new PageControl());
        return comp;
    }

    /**
     * Return a list of events for the passed event.
     * We got an event id passed, so the user wants to directly see this event and
     * not the most up to date one.
     * We will present him with the given event surrounded by previous and later
     * events for the same resource(s).
     */
    public PageList<EventComposite> getEventsForEvent(Subject subject, int[] resourceIds, int eventId, PageControl pc) {
        PageList<EventComposite> pl = new PageList<EventComposite>(pc);

        if (eventId <= 0 || resourceIds == null || resourceIds.length == 0)
            return pl;

        String query = "SELECT detail, id, substr(location, 1, 30) ";
        query += ", severity, timestamp, res_id , ack_user, ack_time  FROM ( ";
        String innerQuery1 = " SELECT e1.detail, e1.id, evs.location, e1.severity, e1.timestamp, evs.resource_id as res_id, e1.ack_user as ack_user, e1.ack_time as ack_time "
            + " FROM rhq_event e1, rhq_event e  ";
        innerQuery1 += "JOIN RHQ_Event_Source evs ON evs.id = e.event_source_id ";
        innerQuery1 += "WHERE e1.timestamp < e.timestamp AND e.id = ? AND evs.resource_id IN ( ";
        innerQuery1 += JDBCUtil.generateInBinds(resourceIds.length);
        innerQuery1 += " ) ";
        innerQuery1 += " ORDER BY e1.id DESC";
        innerQuery1 = addResultsLimitToQuery(innerQuery1, DEFAULT_EVENTS_PAGE_SIZE / 2);
        query += innerQuery1;
        query += ") inner1 UNION (";
        String innerQuery2 = "SELECT e1.detail, e1.id, evs.location, e1.severity, e1.timestamp, evs.resource_id as res_id, e1.ack_user as ack_user, e1.ack_time as ack_time "
            + " FROM rhq_event e1, rhq_event e ";
        innerQuery2 += " JOIN RHQ_Event_Source evs ON evs.id = e.event_source_id "
            + " WHERE e1.timestamp >= e.timestamp AND e.id = ? AND evs.resource_id IN ( ";
        innerQuery2 += JDBCUtil.generateInBinds(resourceIds.length);
        innerQuery2 += " ) ";
        innerQuery2 += "ORDER BY e1.id ASC";
        innerQuery2 = addResultsLimitToQuery(innerQuery2, (DEFAULT_EVENTS_PAGE_SIZE / 2) + 1);
        query += innerQuery2;
        query += ") ORDER BY timestamp, id";

        Connection conn = null;
        PreparedStatement stm = null;
        ResultSet rs = null;
        try {
            conn = rhqDs.getConnection();
            stm = conn.prepareStatement(query);
            stm.setInt(1, eventId);
            int i = 2;
            JDBCUtil.bindNTimes(stm, resourceIds, i);
            i += resourceIds.length;
            stm.setInt(i, eventId);
            i++;
            JDBCUtil.bindNTimes(stm, resourceIds, i);
            i += resourceIds.length;

            rs = stm.executeQuery();
            while (rs.next()) {
                EventComposite ec = new EventComposite(rs.getString(1), rs.getInt(2), rs.getString(3), EventSeverity
                    .valueOf(rs.getString(4)), rs.getTimestamp(5), rs.getInt(6), rs.getString(7), rs.getTimestamp(8));
                pl.add(ec);
            }

        } catch (SQLException sq) {
            log.error("getEventsForEvent: Error retrieving events: " + sq.getMessage(), sq);
            return pl;
        } finally {
            JDBCUtil.safeClose(conn, stm, rs);
        }

        return pl;
    }

    public PageList<EventComposite> getEvents(Subject subject, int[] resourceIds, long begin, long end,
        EventSeverity severity, int eventId, String source, String searchString, PageControl pc) {

        PageList<EventComposite> pl = new PageList<EventComposite>(pc);
        if (eventId > -1) {
            /*
             * We got an event id passed, so the user wants to directly see this event and
             * not the most up to date one.
             * We will present him with the given event surrounded by previous and later
             * events for the same resource(s).
             */
            pl = getEventsForEvent(subject, resourceIds, eventId, pc);
            if (pl.size() > 0)
                return pl;
        }

        if (resourceIds == null || resourceIds.length == 0)
            return pl;

        /*
         * We're still here - either the specified event was not found or we got called without 
         * passing any specific event. Return a bunch of events for the resource etc.
         */
        String query = setupEventsQuery(resourceIds, severity, source, searchString, pc, false);
        runEventsQuery(resourceIds, begin, end, severity, source, searchString, pc, pl, query);
        query = setupEventsQuery(resourceIds, severity, source, searchString, pc, true);
        int totals = runEventsCountQuery(resourceIds, begin, end, severity, source, searchString, pc, pl, query);
        pl.setTotalSize(totals);

        return pl;
    }

    private void runEventsQuery(int[] resourceIds, long begin, long end, EventSeverity severity, String source,
        String searchString, PageControl pc, PageList<EventComposite> pl, String query) {
        Connection conn = null;
        PreparedStatement stm = null;
        ResultSet rs = null;
        try {
            conn = rhqDs.getConnection();
            stm = conn.prepareStatement(query);
            int i = 1;
            JDBCUtil.bindNTimes(stm, resourceIds, 1);
            i += resourceIds.length;
            stm.setTimestamp(i++, new Timestamp(begin));
            stm.setTimestamp(i++, new Timestamp(end));
            if (severity != null)
                stm.setString(i++, severity.toString());
            if (isFilled(searchString))
                stm.setString(i++, PersistenceUtility.formatSearchParameter(searchString));
            if (isFilled(source))
                stm.setString(i++, PersistenceUtility.formatSearchParameter(source));

            rs = stm.executeQuery();
            while (rs.next()) {
                EventComposite ec = new EventComposite(rs.getString(1), rs.getInt(2), rs.getString(3), EventSeverity
                    .valueOf(rs.getString(4)), rs.getTimestamp(5), rs.getInt(6), rs.getString(7), rs.getTimestamp(8));
                pl.add(ec);
            }

        } catch (SQLException sq) {
            log.error("runEventsQuery: Error retrieving events: " + sq.getMessage(), sq);
            log.error("query is [" + query + "].");
            // these values should be useful in working out how we built the query in setupEventsQuery()
            log.error("resourceIds[] has [" + resourceIds.length + "] members.");
            log.error("severity is [" + severity + "].");
            log.error("source is [" + source + "].");
            log.error("searchString is [" + searchString + "].");
            log.error("PageControl is [" + pc + "].");
        } finally {
            JDBCUtil.safeClose(conn, stm, rs);
        }
        return;
    }

    private int runEventsCountQuery(int[] resourceIds, long begin, long end, EventSeverity severity, String source,
        String searchString, PageControl pc, PageList<EventComposite> pl, String query) {
        Connection conn = null;
        PreparedStatement stm = null;
        ResultSet rs = null;
        int num = 0;
        try {
            conn = rhqDs.getConnection();
            stm = conn.prepareStatement(query);
            int i = 1;
            JDBCUtil.bindNTimes(stm, resourceIds, 1);
            i += resourceIds.length;
            stm.setTimestamp(i++, new Timestamp(begin));
            stm.setTimestamp(i++, new Timestamp(end));
            if (severity != null)
                stm.setString(i++, severity.toString());
            if (isFilled(searchString))
                stm.setString(i++, PersistenceUtility.formatSearchParameter(searchString));
            if (isFilled(source))
                stm.setString(i++, PersistenceUtility.formatSearchParameter(source));

            rs = stm.executeQuery();
            while (rs.next()) {
                num = rs.getInt(1);
            }

        } catch (SQLException sq) {
            log.error("runEventsCountQuery: Error retrieving events: " + sq.getMessage(), sq);
        } finally {
            JDBCUtil.safeClose(conn, stm, rs);
        }
        return num;
    }

    private String setupEventsQuery(int[] resourceIds, EventSeverity severity, String source, String searchString,
        PageControl pc, boolean isCountQuery) {
        String query;

        if (isCountQuery) {
            query = "SELECT count(ev.id) ";
        } else {
            query = "SELECT ev.detail, ev.id, substr(evs.location, 1, 30) "
                + ", ev.severity, ev.timestamp, evs.resource_id, ev.ack_user, ev.ack_time ";
        }
        query += "FROM RHQ_Event ev INNER JOIN RHQ_Event_Source evs ON evs.id = ev.event_source_id "
            + "WHERE evs.resource_id IN ( ";

        query += JDBCUtil.generateInBinds(resourceIds.length);
        query += " ) ";

        query += " AND ev.timestamp BETWEEN ? AND ? ";
        if (severity != null)
            query += " AND ev.severity = ? ";
        if (isFilled(searchString))
            query += " AND upper(ev.detail) LIKE ? ";
        if (isFilled(source))
            query += " AND upper(evs.location) LIKE ? ";
        if (!isCountQuery) {
            query = addSortingToQuery(query, pc);
            // NOTE: Add paging to the query last, since for Oracle, the whole query will become an inner SELECT in the
            //       paging SELECTs.
            query = addPagingToQuery(query, pc);
        }
        return query;
    }

    private String addSortingToQuery(String query, PageControl pageControl) {
        StringBuilder queryWithSorting = new StringBuilder(query);
        if (!isFilled(pageControl.getPrimarySortColumn()) || "null".equals(pageControl.getPrimarySortColumn())) {
            pageControl.setPrimarySort("ev.timestamp", PageOrdering.ASC);
        }
        queryWithSorting.append(" ORDER BY ").append(pageControl.getPrimarySortColumn());
        if (pageControl.getPrimarySortOrder() != null) {
            queryWithSorting.append(" ").append(pageControl.getPrimarySortOrder());
        }
        if (!pageControl.getPrimarySortColumn().equalsIgnoreCase("ev.id")) {
            queryWithSorting.append(", ev.id");
        }
        return queryWithSorting.toString();
    }

    private String addPagingToQuery(String query, PageControl pageControl) {
        StringBuilder queryWithPaging = new StringBuilder();
        if (this.dbType instanceof PostgresqlDatabaseType) {
            queryWithPaging.append(query);
            queryWithPaging.append(" LIMIT ").append(pageControl.getPageSize());
            queryWithPaging.append(" OFFSET ").append(pageControl.getStartRow());
        } else if (this.dbType instanceof OracleDatabaseType) {
            int minRowNum = pageControl.getStartRow() + 1;
            int maxRowNum = minRowNum + pageControl.getPageSize() - 1;
            queryWithPaging.append("SELECT * FROM (SELECT /*+ FIRST_ROWS(n) */ allResults.*, rownum rnum FROM (");
            queryWithPaging.append(query);
            queryWithPaging.append(") allResults WHERE rownum <= ").append(maxRowNum).append(") WHERE rnum >= ")
                .append(minRowNum);
        } else {
            throw new RuntimeException("Unknown database type : " + this.dbType);
        }
        return queryWithPaging.toString();
    }

    private String addResultsLimitToQuery(String query, int maxResults) {
        StringBuilder string = new StringBuilder();
        if (this.dbType instanceof PostgresqlDatabaseType) {
            string.append(query);
            string.append(" LIMIT ").append(maxResults);
        } else if (this.dbType instanceof OracleDatabaseType) {
            string.append("SELECT * FROM (");
            string.append(query);
            string.append(") WHERE rownum <= ").append(maxResults);
        } else {
            throw new RuntimeException("Unknown database type : " + this.dbType);
        }
        return string.toString();
    }

    @SuppressWarnings("unchecked")
    public EventComposite getEventDetailForEventId(Subject subject, int eventId) {

        Query q = entityManager.createNamedQuery(Event.GET_DETAILS_FOR_EVENT_IDS);
        List<Integer> eventIds = new ArrayList<Integer>(1);
        eventIds.add(eventId);
        q.setParameter("eventIds", eventIds);
        List<EventComposite> composites = q.getResultList();
        if (composites.size() == 1)
            return composites.get(0);
        else {
            log.warn("getEventDetailForEventId: found " + composites.size() + " details");
            return new EventComposite();
        }
    }

    public EventComposite ackEvent(Subject subject, int eventId) {

        EventComposite comp = new EventComposite();
        try {
            Event e = entityManager.find(Event.class, eventId);
            Date now = new Date();
            e.setAckTime(now);
            e.setAckUser(subject.getName());
            comp.setAckTime(now);
            comp.setAckUser(subject.getName());
        } catch (NoResultException nre) {
            if (log.isDebugEnabled())
                log.debug("ackEvent: Event with id " + eventId + " not found");
        }
        return comp;
    }

    private boolean isFilled(String in) {
        return in != null && !in.equals("");
    }
}
