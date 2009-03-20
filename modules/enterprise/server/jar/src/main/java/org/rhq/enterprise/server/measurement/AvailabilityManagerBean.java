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
package org.rhq.enterprise.server.measurement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.annotation.ejb.TransactionTimeout;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.ResourceAvailability;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.composite.ResourceIdWithAvailabilityComposite;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.core.util.StopWatch;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheManagerLocal;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheStats;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.common.EntityContext;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.measurement.instrumentation.MeasurementMonitor;
import org.rhq.enterprise.server.resource.ResourceAvailabilityManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;

/**
 * Manager for availability related tasks.
 *
 * @author Heiko W. Rupp
 * @author John Mazzitelli
 */
@Stateless
public class AvailabilityManagerBean implements AvailabilityManagerLocal {
    private final Log log = LogFactory.getLog(AvailabilityManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @javax.annotation.Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
    private DataSource dataSource;

    @EJB
    private AvailabilityManagerLocal availabilityManager;
    @EJB
    private AgentManagerLocal agentManager;
    @EJB
    private AuthorizationManagerLocal authorizationManager;
    @EJB
    private ResourceManagerLocal resourceManager;
    @EJB
    private ResourceGroupManagerLocal resourceGroupManager;
    @EJB
    private ResourceAvailabilityManagerLocal resourceAvailabilityManager;
    @EJB
    private AlertConditionCacheManagerLocal alertConditionCacheManager;

    // doing a bulk delete in here, need to be in its own tx
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @TransactionTimeout(6 * 60 * 60)
    public int purgeAvailabilities(long oldest) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(Availability.NATIVE_QUERY_PURGE);
            stmt.setLong(1, oldest);
            long startTime = System.currentTimeMillis();
            int deleted = stmt.executeUpdate();
            MeasurementMonitor.getMBean().incrementPurgeTime(System.currentTimeMillis() - startTime);
            MeasurementMonitor.getMBean().setPurgedAvailabilities(deleted);
            return deleted;
        } catch (Exception e) {
            throw new RuntimeException("Failed to purge availabilities older than [" + oldest + "]", e);
        } finally {
            JDBCUtil.safeClose(conn, stmt, null);
        }
    }

    public AvailabilityType getCurrentAvailabilityTypeForResource(Subject whoami, int resourceId) {
        return resourceAvailabilityManager.getLatestAvailabilityType(whoami, resourceId);
    }

    public Availability getCurrentAvailabilityForResource(Subject whoami, int resourceId) {
        Availability retAvailability;

        try {
            Query q = entityManager.createNamedQuery(Availability.FIND_CURRENT_BY_RESOURCE);
            q.setParameter("resourceId", resourceId);
            retAvailability = (Availability) q.getSingleResult();

            // make sure user has permissions to even view this resource
            Resource resource = retAvailability.getResource();
            if (!authorizationManager.canViewResource(whoami, resource.getId())) {
                throw new PermissionException("User [" + whoami.getName()
                    + "] does not have permission to view resource");
            }
        } catch (NoResultException nre) {
            // Fall back to searching for the one with the latest start date, but most likely it doesn't exist
            Resource resource = resourceManager.getResourceById(whoami, resourceId);
            List<Availability> availList = resource.getAvailability();
            if ((availList != null) && (availList.size() > 0)) {
                log
                    .warn("Could not query for latest avail but found one - missing null end time (this should never happen)");
                retAvailability = availList.get(availList.size() - 1);
            } else {
                retAvailability = new Availability(resource, new Date(), null);
            }
        }

        return retAvailability;
    }

    public List<AvailabilityPoint> getAvailabilitiesForResource(Subject whoami, int resourceId,
        long fullRangeBeginTime, long fullRangeEndTime, int numberOfPoints) {
        EntityContext context = new EntityContext(resourceId, -1, -1, -1);
        return getAvailabilitiesForContext(whoami, context, fullRangeBeginTime, fullRangeEndTime, numberOfPoints);
    }

    public List<AvailabilityPoint> getAvailabilitiesForResourceGroup(Subject whoami, int groupId,
        long fullRangeBeginTime, long fullRangeEndTime, int numberOfPoints) {
        EntityContext context = new EntityContext(-1, groupId, -1, -1);
        return getAvailabilitiesForContext(whoami, context, fullRangeBeginTime, fullRangeEndTime, numberOfPoints);
    }

    public List<AvailabilityPoint> getAvailabilitiesForAutoGroup(Subject whoami, int parentResourceId,
        int resourceTypeId, long fullRangeBeginTime, long fullRangeEndTime, int numberOfPoints) {
        EntityContext context = new EntityContext(-1, -1, parentResourceId, resourceTypeId);
        return getAvailabilitiesForContext(whoami, context, fullRangeBeginTime, fullRangeEndTime, numberOfPoints);
    }

    private List<AvailabilityPoint> getAvailabilitiesForContext(Subject whoami, EntityContext context,
        long fullRangeBeginTime, long fullRangeEndTime, int numberOfPoints) {

        if (context.category == EntityContext.Category.Resource) {
            if (!authorizationManager.canViewResource(whoami, context.resourceId)) {
                throw new PermissionException("User [" + whoami.getName() + "] does not have permission to view "
                    + context.toShortString());
            }
        } else if (context.category == EntityContext.Category.ResourceGroup) {
            if (!authorizationManager.canViewGroup(whoami, context.groupId)) {
                throw new PermissionException("User [" + whoami.getName() + "] does not have permission to view "
                    + context.toShortString());
            }
        } else {

        }

        if ((numberOfPoints <= 0) || (fullRangeBeginTime >= fullRangeEndTime)) {
            return new ArrayList<AvailabilityPoint>();
        }

        List<Availability> availabilities;
        Date fullRangeBeginDate = new Date(fullRangeBeginTime);
        Date fullRangeEndDate = new Date(fullRangeEndTime);

        try {
            if (context.category == EntityContext.Category.Resource) {
                availabilities = findAvailabilityWithinInterval(context.resourceId, fullRangeBeginDate,
                    fullRangeEndDate);
            } else if (context.category == EntityContext.Category.ResourceGroup) {
                availabilities = findResourceGroupAvailabilityWithinInterval(context.groupId, fullRangeBeginDate,
                    fullRangeEndDate);
            } else if (context.category == EntityContext.Category.AutoGroup) {
                availabilities = findAutoGroupAvailabilityWithinInterval(context.parentResourceId,
                    context.resourceTypeId, fullRangeBeginDate, fullRangeEndDate);
            } else {
                throw new IllegalArgumentException("Do not yet support retrieving availability history for Context["
                    + context.toShortString() + "]");
            }
        } catch (Exception e) {
            log.warn("Can't obtain Availability for " + context.toShortString(), e);

            // create a full list of unknown points
            // the for loop goes backwards so the times are calculated in the same way as the rest of this method
            List<AvailabilityPoint> availabilityPoints = new ArrayList<AvailabilityPoint>(numberOfPoints);
            long totalMillis = fullRangeEndTime - fullRangeBeginTime;
            long perPointMillis = totalMillis / numberOfPoints;
            for (int i = numberOfPoints; i >= 0; i--) {
                availabilityPoints.add(new AvailabilityPoint(i * perPointMillis));
            }

            Collections.reverse(availabilityPoints);
            return availabilityPoints;
        }

        // Check if the availabilities obtained cover the beginning of the whole data range.
        // If not, we need to provide a "surrogate" for the beginning interval. The availabilities
        // obtained from the db are sorted in ascending order of time. So we can insert one
        // pseudo-availability in front of the list if needed.
        if (availabilities.size() > 0) {
            Availability earliestAvailability = availabilities.get(0);
            if (earliestAvailability.getStartTime().getTime() > fullRangeBeginDate.getTime()) {
                Availability surrogateAvailability = new Availability(earliestAvailability.getResource(),
                    fullRangeBeginDate, null);
                surrogateAvailability.setEndTime(earliestAvailability.getStartTime());
                availabilities.add(0, surrogateAvailability); // add at the head of the list
            }
        } else {
            Resource surrogateResource = context.category == EntityContext.Category.Resource ? entityManager.find(
                Resource.class, context.resourceId) : new Resource(-1);
            Availability surrogateAvailability = new Availability(surrogateResource, fullRangeBeginDate, null);
            surrogateAvailability.setEndTime(fullRangeEndDate);
            availabilities.add(surrogateAvailability); // add as the only element
        }

        // Now check if the date range passed in by the user extends into the future. If so, finish the last
        // availability at now and add a surrogate after it, as we know nothing about the future.
        Date now = new Date();
        if (fullRangeEndDate.getTime() > now.getTime()) {
            Availability latestAvailability = availabilities.get(availabilities.size() - 1);
            latestAvailability.setEndTime(now);
            Availability unknownFuture = new Availability(latestAvailability.getResource(), now, null);
            availabilities.add(unknownFuture);
        }

        // Now calculate the individual data points.  We start at the end time of the range
        // and move a current time pointer backwards in time, stopping at each barrier along the way, where a barrier
        // is either the start of a data point or the start of an availability record.  We move backwards
        // in time because the full range may not be neatly divisible by the number of points so we want
        // any "leftover" data that we can't account for in the returned list to be the oldest data possible.
        long totalMillis = fullRangeEndTime - fullRangeBeginTime;
        long perPointMillis = totalMillis / numberOfPoints;
        List<AvailabilityPoint> availabilityPoints = new ArrayList<AvailabilityPoint>(numberOfPoints);

        long currentTime = fullRangeEndTime;
        int currentAvailabilityIndex = availabilities.size() - 1;
        long timeUpInDataPoint = 0;
        boolean hasDownPeriods = false;
        long dataPointStartBarrier = fullRangeEndTime - perPointMillis;

        while (currentTime > fullRangeBeginTime) {
            if (currentAvailabilityIndex <= -1) {
                // no more availability data, the rest of the data points are unknown
                availabilityPoints.add(new AvailabilityPoint(currentTime));
                currentTime -= perPointMillis;
                continue;
            }

            Availability currentAvailability = availabilities.get(currentAvailabilityIndex);
            long availabilityStartBarrier = currentAvailability.getStartTime().getTime();

            if (dataPointStartBarrier >= availabilityStartBarrier) { // the start of the data point comes first or at same time as availability record (remember, we are going backwards in time)

                // end the data point
                if (currentAvailability.getAvailabilityType() == null) {
                    // we are on the edge of the range, but know that at least one point there was red, so
                    // we'll be pessimistic and set our entire point down instead of unknown
                    if (hasDownPeriods) {
                        availabilityPoints.add(new AvailabilityPoint(AvailabilityType.DOWN, currentTime));
                    } else {
                        // we are on the edge of the range - if we have ANY data saying we were UP, consider this UP
                        if (timeUpInDataPoint > 0) {
                            availabilityPoints.add(new AvailabilityPoint(AvailabilityType.UP, currentTime));
                        } else {
                            // happens when this timeslice only has surrogates or known avails
                            availabilityPoints.add(new AvailabilityPoint(currentTime)); // unknown
                        }
                    }
                } else {
                    // if the resource has been up in the current time frame, bump up the counter
                    if (currentAvailability.getAvailabilityType() == AvailabilityType.UP) {
                        timeUpInDataPoint += currentTime - dataPointStartBarrier;
                    }

                    AvailabilityType type = (timeUpInDataPoint != perPointMillis) ? AvailabilityType.DOWN
                        : AvailabilityType.UP;
                    availabilityPoints.add(new AvailabilityPoint(type, currentTime));
                }

                timeUpInDataPoint = 0;
                hasDownPeriods = false;

                // if we reached the start of the current availability record, move to the previous one (going back in time, remember)
                if (dataPointStartBarrier == availabilityStartBarrier) {
                    currentAvailabilityIndex--;
                }

                // move the current time pointer to the next data point and move back to the next data point start time
                currentTime = dataPointStartBarrier;
                dataPointStartBarrier -= perPointMillis;
            } else { // the end of the availability record comes first, in the middle of a data point

                // if the resource has been up in the current time frame, bump up the counter
                if (currentAvailability.getAvailabilityType() == AvailabilityType.UP) {
                    timeUpInDataPoint += currentTime - availabilityStartBarrier;
                } else if (currentAvailability.getAvailabilityType() == AvailabilityType.DOWN) {
                    hasDownPeriods = true;
                }

                // move to the previous availability record
                currentAvailabilityIndex--;

                // move the current time pointer to the start of the next
                currentTime = availabilityStartBarrier;
            }
        }

        // remember we went backwards in time, but we want the returned data to be ascending, so reverse the order
        Collections.reverse(availabilityPoints);

        /* 
         * RHQ-1631, always make the latest availability dot match the current availability - NO MATTER WHAT
         * note: this must occur AFTER reversing the collection so the last dot refers to the most recent time slice
         */
        AvailabilityPoint oldFirstAvailabilityPoint = availabilityPoints.remove(availabilityPoints.size() - 1);
        AvailabilityType newFirstAvailabilityType = oldFirstAvailabilityPoint.getAvailabilityType();
        if (context.category == EntityContext.Category.Resource) {
            newFirstAvailabilityType = getCurrentAvailabilityTypeForResource(whoami, context.resourceId);
        } else if (context.category == EntityContext.Category.ResourceGroup) {
            ResourceGroupComposite composite = resourceGroupManager.getResourceGroupWithAvailabilityById(whoami,
                context.groupId);
            Double firstAvailability = composite.getAvailability();
            newFirstAvailabilityType = firstAvailability == null ? null
                : (firstAvailability == 1.0 ? AvailabilityType.UP : AvailabilityType.DOWN);
        } else {
            // March 20, 2009: we only support the "summary area" for resources and resourceGroups to date
            // as a result, newFirstAvailabilityType will be a pass-through of the type in oldFirstAvailabilityPoint
        }
        availabilityPoints
            .add(new AvailabilityPoint(newFirstAvailabilityType, oldFirstAvailabilityPoint.getTimestamp()));

        // This should never happen, but add a check just to be safe.
        if (availabilityPoints.size() != numberOfPoints) {
            String errorMsg = "Calculation of availability did not produce the proper number of data points! "
                + context.toShortString() + "; begin=[" + fullRangeBeginTime + "(" + new Date(fullRangeBeginTime) + ")"
                + "]; end=[" + fullRangeEndTime + "(" + new Date(fullRangeEndTime) + ")" + "]; numberOfPoints=["
                + numberOfPoints + "]; actual-number=[" + availabilityPoints.size() + "]";
            log.warn(errorMsg);
        }

        return availabilityPoints;
    }

    @SuppressWarnings("unchecked")
    public boolean mergeAvailabilityReport(AvailabilityReport report) {
        int reportSize = report.getResourceAvailability().size();
        String agentName = report.getAgentName();
        StopWatch watch = new StopWatch();

        if (reportSize == 0) {
            log.error("Agent [" + agentName + "] sent an empty availability report.  This is a bug, please report it");
            return true; // even though this report is bogus, do not ask for an immediate full report to avoid unusual infinite recursion due to this error condition
        }

        if (log.isDebugEnabled()) {
            if (reportSize > 1) {
                log.debug("Agent [" + agentName + "]: processing availability report of size: " + reportSize);
            }
        }

        notifyAlertConditionCacheManager("mergeAvailabilityReport", report.getResourceAvailability().toArray(
            new Availability[report.getResourceAvailability().size()]));

        boolean askForFullReport = false;
        Agent agentToUpdate = agentManager.getAgentByName(agentName);

        if (agentToUpdate != null) {
            // do this now, before we might clear() the entity manager
            availabilityManager.updateLastAvailabilityReport(agentToUpdate.getId());
            //agentToUpdate.setLastAvailabilityReport(System.currentTimeMillis());
        }

        int numInserted = 0;

        // if we got a changes-only report, and the agent appears backfilled, then we need
        // to skip this report so as not to waste our time and immediately request and process
        // a full report because, obviously, the agent is no longer down but the server thinks
        // it still is down - we need to know the availabilities for all the resources on that agent
        if (report.isChangesOnlyReport() && isAgentBackfilled(agentName)) {
            askForFullReport = true;
        } else {
            Query q = entityManager.createNamedQuery(Availability.FIND_CURRENT_BY_RESOURCE);
            q.setFlushMode(FlushModeType.COMMIT);

            int count = 0;
            for (Availability reported : report.getResourceAvailability()) {
                if ((++count % 100) == 0) {
                    entityManager.flush();
                    entityManager.clear();
                }

                // availability reports only tell us the current state at the start time - end time is ignored/must be null
                reported.setEndTime(null);

                try {
                    q.setParameter("resourceId", reported.getResource().getId());
                    Availability latest = (Availability) q.getSingleResult();

                    if (reported.getStartTime().getTime() >= latest.getStartTime().getTime()) {
                        //log.info( "new avail (latest/reported)-->" + latest + "/" + reported );

                        // the new availability data is for a time after our last known state change
                        // we are runlength encoded, so only persist data if the availability changed
                        if (latest.getAvailabilityType() != reported.getAvailabilityType()) {
                            entityManager.persist(reported);
                            numInserted++;

                            latest.setEndTime(reported.getStartTime());
                            latest = entityManager.merge(latest);

                            updateResourceAvailability(reported);
                        }

                        // our last known state was unknown, ask for a full report to ensure we are in sync with agent
                        if (latest.getAvailabilityType() == null) {
                            askForFullReport = true;
                        }
                    } else {
                        //log.info( "past avail (latest/reported)==>" + latest + "/" + reported );

                        // The new data is for a time in the past, probably an agent sending a report after
                        // a network outage has been corrected but after we have already backfilled.
                        // We need to insert it into our past timeline.
                        insertAvailability(reported);
                        numInserted++;

                        // this is an unusual report - ask the agent for a full report so as to ensure we are in sync with agent
                        askForFullReport = true;
                    }
                } catch (NoResultException nre) {
                    entityManager.persist(reported);
                    updateResourceAvailability(reported);
                    numInserted++;
                } catch (NonUniqueResultException nure) {
                    // This condition should never happen.  In my world of la-la land, I've done everything
                    // correctly so this never happens.  But, due to the asynchronous nature of things,
                    // I have to believe that this still might happen (albeit rarely).  If it does happen,
                    // and we do nothing about it - bad things arise.  So, if we find that a resource
                    // has 2 or more availabilities with endTime of null, we need to delete all but the
                    // latest one (the one whose start time is the latest).  This should correct the
                    // problem and allow us to continue processing availability reports for that resource
                    log.warn("Resource [" + reported.getResource()
                        + "] has multiple availabilities without an endtime [" + nure.getMessage()
                        + "] - will attempt to remove the extra ones\n" + report.toString(false));

                    q.setParameter("resourceId", reported.getResource().getId());
                    List<Availability> latest = q.getResultList();

                    // delete all but the last one (our query sorts in ASC start time order)
                    int latestCount = latest.size();
                    for (int i = 0; i < (latestCount - 1); i++) {
                        entityManager.remove(latest.get(i));
                    }
                    updateResourceAvailability(latest.get(latestCount - 1));

                    // this is an unusual report - ask the agent for a full report so as to ensure we are in sync with agent
                    askForFullReport = true;
                }
            }

            MeasurementMonitor.getMBean().incrementAvailabilitiesInserted(numInserted);
            MeasurementMonitor.getMBean().incrementAvailabilityInsertTime(watch.getElapsed());
            watch.reset();
        }

        // a single report comes from a single agent - update the agent's last availability report timestamp
        if (agentToUpdate != null) {
            // don't bother asking for a full report if the one we are currently processing is already full
            if (askForFullReport && report.isChangesOnlyReport()) {
                log.debug("The server is unsure that it has up-to-date availabilities for agent ["
                    + agentToUpdate.getName() + "]; asking for a full report to be sent");
                return false;
            }
        } else {
            log.error("Could not figure out which agent sent availability report.  This is a bug, please report it. "
                + report);
        }

        return true; // everything is OK and things look to be in sync
    }

    private void updateResourceAvailability(Availability reported) {
        // update the last known availability data for this resource
        ResourceAvailability currentAvailability = resourceAvailabilityManager.getLatestAvailability(reported
            .getResource().getId());
        if (currentAvailability.getAvailabilityType() != reported.getAvailabilityType()) {
            // but only update the record if necessary (if the AvailabilityType changed)
            currentAvailability.setAvailabilityType(reported.getAvailabilityType());
            entityManager.merge(currentAvailability);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateLastAvailabilityReport(int agentId) {
        // should we catch exceptions here, or allow them to bubble up and be caught?

        String updateStatement = "" //
            + "UPDATE RHQ_AGENT " //
            + "   SET LAST_AVAILABILITY_REPORT = ? " //
            + " WHERE ID = ? ";

        Query query = entityManager.createNativeQuery(updateStatement);
        query.setParameter(1, System.currentTimeMillis());
        query.setParameter(2, agentId);

        query.executeUpdate();
    }

    @SuppressWarnings("unchecked")
    public void setAllAgentResourceAvailabilities(int agentId, AvailabilityType availabilityType) {
        String typeString = (availabilityType != null) ? availabilityType.toString() : "unknown";

        // get all resources that are not already of the given availability type (since these are the ones we need to change)
        Query query = entityManager.createNamedQuery(Availability.FIND_NONMATCHING_WITH_RESOURCE_ID_BY_AGENT_AND_TYPE);
        query.setParameter("agentId", agentId);
        query.setParameter("availabilityType", availabilityType);
        List<ResourceIdWithAvailabilityComposite> resourcesWithStatus = query.getResultList();

        // The above query only returns resources if they have at least one row in Availability.
        // This may be a problem in the future, and may need to be fixed.
        // If a resource has 0 rows of availability, then it is by definition "unknown". If,
        // availabilityType is null, we don't have to do anything since the unknown state hasn't changed.
        // If this method is told to set all agent resources to something of other than unknown (null)
        // availability, then we may need to completely rethink the query we do above so it returns composite
        // objects for all resources, even those that have 0 rows of availability.  Remember though, that once
        // we get an availability report from an agent, a resource will have at least 1 availability row.  So,
        // a resource should rarely have 0 avail rows; if it does, it normally gets one within a minute
        // (since the agent sends avail reports every 60 seconds or so by default).  So this problem might not
        // be as bad as first thought.

        log.debug("Agent #[" + agentId + "] is going to have [" + resourcesWithStatus.size()
            + "] resources backfilled with [" + typeString + "]");

        Date now = new Date();

        // for those resources that have a current availability status that is different, change them
        List<Availability> newAvailabilities = new ArrayList<Availability>(resourcesWithStatus.size());
        for (ResourceIdWithAvailabilityComposite record : resourcesWithStatus) {
            Availability newAvailabilityInterval = getNewInterval(record, now, availabilityType);
            if (newAvailabilityInterval != null) {
                newAvailabilities.add(newAvailabilityInterval);
            }
        }

        resourceAvailabilityManager.updateAllResourcesAvailabilitiesForAgent(agentId, availabilityType);

        // To handle backfilling process, which will mark them down
        notifyAlertConditionCacheManager("setAllAgentResourceAvailabilities", newAvailabilities
            .toArray(new Availability[newAvailabilities.size()]));

        log.debug("Resources for agent #[" + agentId + "] have been fully backfilled with [" + typeString + "]");

        return;
    }

    public boolean isAgentBackfilled(String agentName) {
        // query returns 0 if the agent's platform is DOWN (or does not exist), 1 if not
        Query q = entityManager.createNamedQuery(Availability.QUERY_IS_AGENT_BACKFILLED);
        q.setParameter("agentName", agentName);
        return ((Number) q.getSingleResult()).intValue() == 0;
    }

    /**
     * Starts a new availability interval for a given resource. If the new interval is of the same type as the previous,
     * then the previous will be extended. Otherwise the previous will be terminated and a new one will be started. The
     * Availability objects in the given record will be modified; make sure they are managed by an entity manager if you
     * want the changes to be persisted.
     *
     * @param  record    identifies the resource and its current availability
     * @param  startDate Start date of the new interval (which must be after the current availability interval)
     * @param  aType     the new type of availability (UP, DOWN) that the resource will now have
     *
     * @return the new availability interval for a given resource, or null if there is already an existing availability
     */
    private Availability getNewInterval(ResourceIdWithAvailabilityComposite record, Date startDate,
        AvailabilityType aType) {
        // if there is already an existing availability, update it
        Availability old = record.getAvailability();

        if (old != null) {
            if (old.getAvailabilityType() == aType) {
                // existing availability is the same type, just extend it without creating a new entity
                old.setEndTime(null); // don't really need to do this; just enforces the fact that we extend the last interval
                return null;
            }

            old.setEndTime(startDate);
        }

        Resource resource = new Resource();
        resource.setId(record.getResourceId());

        Availability newAvail = new Availability(resource, startDate, aType);
        entityManager.persist(newAvail);

        return newAvail;
    }

    /**
     * Try to insert <code>toInsert</code> into the resource's availability timeline. It is expected that:
     *
     * <ul>
     *   <li>only the start time in <code>toInsert</code> is valid.</li>
     *   <li><code>toInsert</code> is not to be inserted at the end (that is, it is not the latest availability - it is
     *     something that occurred in the past).</li>
     *   <li>there is at least 1 availability record for the resource</li>
     * </ul>
     *
     * @param toInsert new interval, probably being backfilled from a re-appeared agent
     */
    @SuppressWarnings("unchecked")
    private void insertAvailability(Availability toInsert) {
        // get the existing availability interval where the new availability will be shoe-horned in
        Query query = entityManager.createNamedQuery(Availability.FIND_BY_RESOURCE_AND_DATE);
        query.setParameter("resourceId", toInsert.getResource().getId());
        query.setParameter("aTime", toInsert.getStartTime().getTime());

        Availability existing;

        try {
            existing = (Availability) query.getSingleResult();
        } catch (NoResultException nre) {
            // we are inserting this as the very first interval
            query = entityManager.createNamedQuery(Availability.FIND_BY_RESOURCE);
            query.setParameter("resourceId", toInsert.getResource().getId());
            query.setMaxResults(1); // we only need the very first one
            Availability firstAvail = ((List<Availability>) query.getResultList()).get(0);

            // only add a new row if its a different status; otherwise, just move the first interval back further
            if (firstAvail.getAvailabilityType() != toInsert.getAvailabilityType()) {
                toInsert.setEndTime(firstAvail.getStartTime());
                entityManager.persist(toInsert);
            } else {
                firstAvail.setStartTime(toInsert.getStartTime());
            }

            return;
        }

        // If we are inserting the same availability type, the first one can just continue
        // and there is nothing to do!  But if we are inserting a different availability
        // type as that of the existing interval, the existing interval ends at the newly
        // inserted interval start.
        // Because we are runlength-encoded, we are assured the next interval
        // is the same type as the newly inserted one.  So we just have to move
        // the start time of the next interval back to the start of the newly inserted
        // interval.
        // If the new start time is the same as the existing, then we assume this latest report
        // contains a more up-to-date status so we just move back the next interval
        // all the way back to the existing start and we delete the existing interval.
        if (existing.getAvailabilityType() != toInsert.getAvailabilityType()) {
            // note: we are assured this query will return something; semantics of this
            // method is that it is never called if we are inserting in the last interval
            query = entityManager.createNamedQuery(Availability.FIND_BY_RESOURCE_AND_DATE);
            query.setParameter("resourceId", toInsert.getResource().getId());
            query.setParameter("aTime", existing.getEndTime().getTime() + 1);
            Availability afterExisting = (Availability) query.getSingleResult();
            afterExisting.setStartTime(toInsert.getStartTime()); // move back the next interval

            if (existing.getEndTime().getTime() == toInsert.getStartTime().getTime()) {
                entityManager.remove(existing);
            } else {
                existing.setEndTime(toInsert.getStartTime());
            }
        }

        return;
    }

    /**
     * Find all availability records for a given Resource that match the given interval [startDate, endDate]. The
     * returned objects will probably cover a larger interval than the required one.
     *
     * @param  resourceId identifies the resource for which we want the values
     * @param  startDate  start date of the desired interval
     * @param  endDate    end date of the desired interval
     *
     * @return A list of availabilities that cover at least the given date range
     */
    @SuppressWarnings("unchecked")
    public List<Availability> findAvailabilityWithinInterval(int resourceId, Date startDate, Date endDate) {
        Query q = entityManager.createNamedQuery(Availability.FIND_FOR_RESOURCE_WITHIN_INTERVAL);
        q.setParameter("resourceId", resourceId);
        q.setParameter("start", startDate.getTime());
        q.setParameter("end", endDate.getTime());
        List<Availability> results = q.getResultList();
        return results;
    }

    @SuppressWarnings("unchecked")
    private List<Availability> findResourceGroupAvailabilityWithinInterval(int groupId, Date startDate, Date endDate) {
        Query q = entityManager.createNamedQuery(Availability.FIND_FOR_RESOURCE_GROUP_WITHIN_INTERVAL);
        q.setParameter("groupId", groupId);
        q.setParameter("start", startDate.getTime());
        q.setParameter("end", endDate.getTime());
        List<Availability> results = q.getResultList();
        return results;
    }

    @SuppressWarnings("unchecked")
    private List<Availability> findAutoGroupAvailabilityWithinInterval(int parentResourceId, int resourceTypeId,
        Date startDate, Date endDate) {
        Query q = entityManager.createNamedQuery(Availability.FIND_FOR_AUTO_GROUP_WITHIN_INTERVAL);
        q.setParameter("parentId", parentResourceId);
        q.setParameter("typeId", resourceTypeId);
        q.setParameter("start", startDate.getTime());
        q.setParameter("end", endDate.getTime());
        List<Availability> results = q.getResultList();
        return results;
    }

    public PageList<Availability> findByResource(Subject user, int resourceId, PageControl pageControl) {
        pageControl.initDefaultOrderingField("av.startTime", PageOrdering.DESC);

        Query countQuery = PersistenceUtility.createCountQuery(entityManager, Availability.FIND_BY_RESOURCE_NO_SORT);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, Availability.FIND_BY_RESOURCE_NO_SORT,
            pageControl);

        countQuery.setParameter("resourceId", resourceId);
        query.setParameter("resourceId", resourceId);

        long count = (Long) countQuery.getSingleResult();
        List<Availability> availabilities = query.getResultList();

        return new PageList(availabilities, (int) count, pageControl);
    }

    private void notifyAlertConditionCacheManager(String callingMethod, Availability... availabilities) {
        AlertConditionCacheStats stats = alertConditionCacheManager.checkConditions(availabilities);

        log.debug(callingMethod + ": " + stats.toString());
    }
}