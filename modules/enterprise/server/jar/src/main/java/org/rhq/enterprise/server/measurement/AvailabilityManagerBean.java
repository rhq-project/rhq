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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.ejb3.annotation.TransactionTimeout;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.AvailabilityCriteria;
import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.discovery.AvailabilityReport.Datum;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.ResourceAvailability;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.composite.ResourceIdWithAvailabilityComposite;
import org.rhq.core.domain.resource.group.composite.ResourceGroupAvailability;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite.GroupAvailabilityType;
import org.rhq.core.domain.server.PersistenceUtility;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.util.StopWatch;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheManagerLocal;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheStats;
import org.rhq.enterprise.server.alert.engine.model.AvailabilityDurationCacheElement;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.measurement.instrumentation.MeasurementMonitor;
import org.rhq.enterprise.server.resource.ResourceAvailabilityManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.scheduler.jobs.AlertAvailabilityDurationJob;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;

/**
 * Manager for availability related tasks.
 *
 * @author Heiko W. Rupp
 * @author John Mazzitelli
 */
@Stateless
public class AvailabilityManagerBean implements AvailabilityManagerLocal, AvailabilityManagerRemote {
    private final Log log = LogFactory.getLog(AvailabilityManagerBean.class);

    static private final int MERGE_BATCH_SIZE;

    static {

        // The value of 200 has been settled upon after testing several batch sizes. If changed it still must be less
        // than 1000 for Oracle IN clause limitation reasons.
        int mergeBatchSize = 200;
        try {
            mergeBatchSize = Integer.parseInt(System.getProperty("rhq.server.availability.merge.batch.size", "200"));
        } catch (Throwable t) {
            //
        }
        MERGE_BATCH_SIZE = (mergeBatchSize > 999) ? 999 : mergeBatchSize;
    }

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

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

    // For Avail Duration Alert Condition Checks
    @javax.annotation.Resource
    private TimerService timerService;

    // doing a bulk delete in here, need to be in its own tx
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @TransactionTimeout(6 * 60 * 60)
    public int purgeAvailabilities(long oldest) {
        try {
            Query purgeQuery = entityManager.createNativeQuery(Availability.NATIVE_QUERY_PURGE);
            purgeQuery.setParameter(1, oldest);
            long startTime = System.currentTimeMillis();
            int deleted = purgeQuery.executeUpdate();
            MeasurementMonitor.getMBean().incrementPurgeTime(System.currentTimeMillis() - startTime);
            MeasurementMonitor.getMBean().setPurgedAvailabilities(deleted);
            return deleted;
        } catch (Exception e) {
            throw new RuntimeException("Failed to purge availabilities older than [" + oldest + "]", e);
        }
    }

    public AvailabilityType getCurrentAvailabilityTypeForResource(Subject subject, int resourceId) {
        return resourceAvailabilityManager.getLatestAvailabilityType(subject, resourceId);
    }

    public Availability getCurrentAvailabilityForResource(Subject subject, int resourceId) {
        Availability retAvailability;
        if (authorizationManager.canViewResource(subject, resourceId) == false) {
            throw new PermissionException("User [" + subject
                + "] does not have permission to view current availability for resource[id=" + resourceId + "]");
        }

        try {
            Query q = entityManager.createNamedQuery(Availability.FIND_CURRENT_BY_RESOURCE);
            q.setParameter("resourceId", resourceId);
            retAvailability = (Availability) q.getSingleResult();
        } catch (NoResultException nre) {
            // Fall back to searching for the one with the latest start date, but most likely it doesn't exist
            Resource resource = resourceManager.getResourceById(subject, resourceId);
            List<Availability> availList = resource.getAvailability();
            if ((availList != null) && (availList.size() > 0)) {
                log.warn("Could not query for latest avail but found one - missing null end time (this should never happen)");
                retAvailability = availList.get(availList.size() - 1);
            } else {
                retAvailability = new Availability(resource, AvailabilityType.UNKNOWN);
            }
        }

        return retAvailability;
    }

    @Override
    public List<Availability> getAvailabilitiesForResource(Subject subject, int resourceId, long startTime, long endTime) {

        if (!authorizationManager.canViewResource(subject, resourceId)) {
            throw new PermissionException("User [" + subject.getName() + "] does not have permission to view ["
                + resourceId + "]");
        }

        List<Availability> result;
        Date startDate = new Date(startTime);
        Date endDate = new Date(endTime);

        AvailabilityCriteria c = new AvailabilityCriteria();
        c.addFilterResourceId(resourceId);
        c.addFilterInterval(startTime, endTime); // reduce by 1 ms to fake exclusive end time on the range.
        c.addSortStartTime(PageOrdering.ASC);
        result = findAvailabilityByCriteria(subject, c);

        // The criteria interval filter is inclusive.  But since availN(endTime) == availN+1(startTime) we can get
        // unwanted avails in the query result, when the range falls on an avail border.
        // For example, assume the following three Availability records exist:
        // AV-1 [0..100]
        // AV-2        [100..200]
        // AV-3                 [200..300]
        //
        // If we happen to query for startTime=100 and endTime=200 we'll end up with 3 avails in the result:
        // Result = { AV-1 [0..100], AV-2 [100..200], AV-3 [200..300] }. We really only want AV-2 [100..200] because
        // the other two end up having 0 length, |100..100| and |200..200|.
        //
        // Remove any unwanted entries.
        for (Iterator<Availability> i = result.iterator(); i.hasNext();) {
            Availability av = i.next();
            if ((null != av.getEndTime() && av.getEndTime().equals(startTime) || av.getStartTime().equals(endTime))) {
                i.remove();
            }
        }

        // Check if the availabilities obtained cover the startTime of the range.
        // If not, we need to provide a "surrogate" for the beginning interval. The availabilities
        // obtained from the DB are sorted in ascending order of time. So we can insert one
        // pseudo-availability in front of the list if needed. Note that due to avail purging
        // we can end up with periods without avail data. The surrogate will be for type UNKNOWN.
        if (result.size() > 0) {
            Availability firstAvailability = result.get(0);
            if (firstAvailability.getStartTime() > startDate.getTime()) {
                Availability surrogateAvailability = new Availability(firstAvailability.getResource(),
                    startDate.getTime(), AvailabilityType.UNKNOWN);
                surrogateAvailability.setEndTime(firstAvailability.getStartTime());
                result.add(0, surrogateAvailability); // add at the head of the list
            }
        } else {
            Resource surrogateResource = entityManager.find(Resource.class, resourceId);
            Availability surrogateAvailability = new Availability(surrogateResource, startDate.getTime(),
                AvailabilityType.UNKNOWN);
            surrogateAvailability.setEndTime(endDate.getTime());
            result.add(surrogateAvailability); // add as the only element, covering the entire interval
        }

        // Now, limit the Availability ranges to the desired range. Detach the entities prior to to the update so
        // the value changes are not propagated to the DB.
        entityManager.detach(result.get(0));
        if (result.size() > 1) {
            entityManager.detach(result.get(result.size() - 1));
        }
        result.get(0).setStartTime(startDate.getTime());
        result.get(result.size() - 1).setEndTime(endDate.getTime());

        return result;
    }

    @Override
    public List<ResourceGroupAvailability> getAvailabilitiesForResourceGroup(Subject subject, int groupId,
        long startTime, long endTime) {

        if (!authorizationManager.canViewGroup(subject, groupId)) {
            throw new PermissionException("User [" + subject.getName() + "] does not have permission to view ["
                + groupId + "]");
        }

        List<ResourceGroupAvailability> result = new ArrayList<ResourceGroupAvailability>();
        Date startDate = new Date(startTime);
        Date endDate = new Date(endTime);

        // all avails for all explicit resources for the time range, ordered by asc startTime
        List<Availability> allAvailabilities = findResourceGroupAvailabilityWithinInterval(groupId, startDate, endDate);

        // If we have no availabilities we need to return a single group avail, either EMPTY or WARN (all UNKNOWN)
        if (allAvailabilities.isEmpty()) {
            ResourceGroupAvailability groupAvail = new ResourceGroupAvailability(groupId);
            groupAvail.setStartTime(startTime);
            groupAvail.setEndTime(endTime);
            int explicitMemberCount = resourceGroupManager.getExplicitGroupMemberCount(groupId);
            groupAvail.setGroupAvailabilityType((0 == explicitMemberCount) ? GroupAvailabilityType.EMPTY
                : GroupAvailabilityType.WARN);
            result.add(groupAvail);

            return result;
        }

        // OK, let's try and explain explain what we are doing here. The goal is to have a continuous set of intervals
        // extending from startTime to endTime showing all changes in group avail.  Each avail change for any member
        // could signify a change in the overall group avail.  We must first establish the initial group avail and
        // then walk forward, checking for changes at each resource avail startTime for a group member.
        // One subtlety is that group membership can change over time. We don't track when resources come and go, we
        // are dealing with the avails for the *current* explicit members.  So, we'll only work with the avails we have
        // and not worry about missing avails at any given time. In other words, no "surrogate" UNKNOWN avail insertion.

        // OK, calculate the initial group avail
        Long atTime = startTime;
        int atTimeIndex = 0;
        ResourceGroupAvailability currentGroupAvail = null;
        int size = allAvailabilities.size();

        do {
            GroupAvailabilityType groupAvailTypeAtTime = getGroupAvailabilityType(atTime, allAvailabilities);

            // if this is a change in group avail type then add it to the result
            if (null == currentGroupAvail || currentGroupAvail.getGroupAvailabilityType() != groupAvailTypeAtTime) {
                if (null != currentGroupAvail) {
                    currentGroupAvail.setEndTime(atTime);
                }

                // leave endTime unset, we don't know endTime until we know the next startTime, or are done
                currentGroupAvail = new ResourceGroupAvailability(groupId);
                currentGroupAvail.setStartTime(atTime);
                currentGroupAvail.setGroupAvailabilityType(groupAvailTypeAtTime);
                result.add(currentGroupAvail);
            }

            // move atTime to the next possible startTime
            while (atTimeIndex < size && allAvailabilities.get(atTimeIndex).getStartTime() <= atTime) {
                ++atTimeIndex;
            }

            if (atTimeIndex < size) {
                atTime = allAvailabilities.get(atTimeIndex).getStartTime();
            }

        } while (atTimeIndex < size);

        currentGroupAvail.setEndTime(endTime);

        return result;
    }

    private GroupAvailabilityType getGroupAvailabilityType(long atTime, List<Availability> allAvailabilities) {

        long count = 0;
        long disabled = 0;
        long down = 0;
        long unknown = 0;
        long up = 0;

        for (Availability av : allAvailabilities) {
            // if the Avail straddles startTime (startTime inclusive, endTime exclusive) then it is relevant.
            // EndTime is exclusive because Avail(N).endTime=Avail(N+1).startTime and we don't want to consider
            // two records. The relevant availType is the one starting, not ending at atTime.
            Long startTime = av.getStartTime();
            Long endTime = av.getEndTime();
            if (startTime <= atTime && (null == endTime || endTime > atTime)) {
                ++count;
                switch (av.getAvailabilityType()) {
                case UP:
                    ++up;
                    break;
                case DOWN:
                    ++down;
                    break;
                case UNKNOWN:
                    ++unknown;
                    break;
                case DISABLED:
                    ++disabled;
                    break;
                }
            }
        }

        if (0 == count) {
            return GroupAvailabilityType.EMPTY;
        }

        if (down == count) {
            return GroupAvailabilityType.DOWN;
        }

        if (down > 0 || unknown > 0) {
            return GroupAvailabilityType.WARN;
        }

        if (disabled > 0) {
            return GroupAvailabilityType.DISABLED;
        }

        return GroupAvailabilityType.UP;
    }

    public List<AvailabilityPoint> findAvailabilitiesForResource(Subject subject, int resourceId,
        long fullRangeBeginTime, long fullRangeEndTime, int numberOfPoints, boolean withCurrentAvailability) {
        EntityContext context = new EntityContext(resourceId, -1, -1, -1);
        return getAvailabilitiesForContext(subject, context, fullRangeBeginTime, fullRangeEndTime, numberOfPoints,
            withCurrentAvailability);
    }

    public List<AvailabilityPoint> findAvailabilitiesForResourceGroup(Subject subject, int groupId,
        long fullRangeBeginTime, long fullRangeEndTime, int numberOfPoints, boolean withCurrentAvailability) {
        EntityContext context = new EntityContext(-1, groupId, -1, -1);
        return getAvailabilitiesForContext(subject, context, fullRangeBeginTime, fullRangeEndTime, numberOfPoints,
            withCurrentAvailability);
    }

    public List<AvailabilityPoint> findAvailabilitiesForAutoGroup(Subject subject, int parentResourceId,
        int resourceTypeId, long fullRangeBeginTime, long fullRangeEndTime, int numberOfPoints,
        boolean withCurrentAvailability) {
        EntityContext context = new EntityContext(-1, -1, parentResourceId, resourceTypeId);
        return getAvailabilitiesForContext(subject, context, fullRangeBeginTime, fullRangeEndTime, numberOfPoints,
            withCurrentAvailability);
    }

    private List<AvailabilityPoint> getAvailabilitiesForContext(Subject subject, EntityContext context,
        long fullRangeBeginTime, long fullRangeEndTime, int numberOfPoints, boolean withCurrentAvailability) {

        if (context.type == EntityContext.Type.Resource) {
            if (!authorizationManager.canViewResource(subject, context.resourceId)) {
                throw new PermissionException("User [" + subject.getName() + "] does not have permission to view "
                    + context.toShortString());
            }
        } else if (context.type == EntityContext.Type.ResourceGroup) {
            if (!authorizationManager.canViewGroup(subject, context.groupId)) {
                throw new PermissionException("User [" + subject.getName() + "] does not have permission to view "
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
            if (context.type == EntityContext.Type.Resource) {
                AvailabilityCriteria c = new AvailabilityCriteria();
                c.addFilterResourceId(context.resourceId);
                c.addFilterInterval(fullRangeBeginTime, fullRangeEndTime);
                c.addSortStartTime(PageOrdering.ASC);
                availabilities = findAvailabilityByCriteria(subject, c);

            } else if (context.type == EntityContext.Type.ResourceGroup) {
                availabilities = findResourceGroupAvailabilityWithinInterval(context.groupId, fullRangeBeginDate,
                    fullRangeEndDate);

            } else if (context.type == EntityContext.Type.AutoGroup) {
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
                availabilityPoints.add(new AvailabilityPoint(AvailabilityType.UNKNOWN, i * perPointMillis));
            }

            Collections.reverse(availabilityPoints);
            return availabilityPoints;
        }

        // Check if the availabilities obtained cover the beginning of the whole data range.
        // If not, we need to provide a "surrogate" for the beginning interval. The availabilities
        // obtained from the db are sorted in ascending order of time. So we can insert one
        // pseudo-availability in front of the list if needed. Note that due to avail purging
        // we can end up with periods without avail data.
        if (availabilities.size() > 0) {
            Availability earliestAvailability = availabilities.get(0);
            if (earliestAvailability.getStartTime() > fullRangeBeginDate.getTime()) {
                Availability surrogateAvailability = new SurrogateAvailability(earliestAvailability.getResource(),
                    fullRangeBeginDate.getTime());
                surrogateAvailability.setEndTime(earliestAvailability.getStartTime());
                availabilities.add(0, surrogateAvailability); // add at the head of the list
            }
        } else {
            Resource surrogateResource = context.type == EntityContext.Type.Resource ? entityManager.find(
                Resource.class, context.resourceId) : new Resource(-1);
            Availability surrogateAvailability = new SurrogateAvailability(surrogateResource,
                fullRangeBeginDate.getTime());
            surrogateAvailability.setEndTime(fullRangeEndDate.getTime());
            availabilities.add(surrogateAvailability); // add as the only element
        }

        // Now check if the date range passed in by the user extends into the future. If so, finish the last
        // availability at now and add a surrogate after it, as we know nothing about the future.
        long now = System.currentTimeMillis();
        if (fullRangeEndDate.getTime() > now) {
            Availability latestAvailability = availabilities.get(availabilities.size() - 1);
            latestAvailability.setEndTime(now);
            Availability unknownFuture = new SurrogateAvailability(latestAvailability.getResource(), now);
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
        long timeDisabledInDataPoint = 0;
        boolean hasDownPeriods = false;
        boolean hasDisabledPeriods = false;
        boolean hasUnknownPeriods = false;
        long dataPointStartBarrier = fullRangeEndTime - perPointMillis;

        while (currentTime > fullRangeBeginTime) {
            if (currentAvailabilityIndex <= -1) {
                // no more availability data, the rest of the data points are unknown
                availabilityPoints.add(new AvailabilityPoint(AvailabilityType.UNKNOWN, currentTime));
                currentTime -= perPointMillis;
                continue;
            }

            Availability currentAvailability = availabilities.get(currentAvailabilityIndex);
            long availabilityStartBarrier = currentAvailability.getStartTime();

            // the start of the data point comes first or at same time as availability record (remember, we are going
            // backwards in time)
            if (dataPointStartBarrier >= availabilityStartBarrier) {

                // end the data point
                if (currentAvailability instanceof SurrogateAvailability) {
                    // we are on the edge of the range with a surrogate for this data point.  Be pessimistic,
                    // if we have had any down time, set to down, then disabled, then up, and finally unknown.
                    if (hasDownPeriods) {
                        availabilityPoints.add(new AvailabilityPoint(AvailabilityType.DOWN, currentTime));

                    } else if (hasDisabledPeriods) {
                        availabilityPoints.add(new AvailabilityPoint(AvailabilityType.DISABLED, currentTime));

                    } else if (timeUpInDataPoint > 0) {
                        availabilityPoints.add(new AvailabilityPoint(AvailabilityType.UP, currentTime));

                    } else {
                        availabilityPoints.add(new AvailabilityPoint(AvailabilityType.UNKNOWN, currentTime));
                    }
                } else {
                    // bump up the proper counter or set the proper flag for the current time frame
                    switch (currentAvailability.getAvailabilityType()) {
                    case UP:
                        timeUpInDataPoint += currentTime - dataPointStartBarrier;
                        break;
                    case DOWN:
                        hasDownPeriods = true;
                        break;
                    case DISABLED:
                        hasDisabledPeriods = true;
                        break;
                    case UNKNOWN:
                        hasUnknownPeriods = true;
                        break;
                    }

                    // if the period has been all green,  then set it to UP, otherwise, be pessimistic if there is any
                    // mix of avail types
                    if (timeUpInDataPoint == perPointMillis) {
                        availabilityPoints.add(new AvailabilityPoint(AvailabilityType.UP, currentTime));

                    } else if (hasDownPeriods) {
                        availabilityPoints.add(new AvailabilityPoint(AvailabilityType.DOWN, currentTime));

                    } else if (hasDisabledPeriods) {
                        availabilityPoints.add(new AvailabilityPoint(AvailabilityType.DISABLED, currentTime));

                    } else {
                        availabilityPoints.add(new AvailabilityPoint(AvailabilityType.UNKNOWN, currentTime));
                    }
                }

                timeUpInDataPoint = 0;
                hasDownPeriods = false;
                hasDisabledPeriods = false;
                hasUnknownPeriods = false;

                // if we reached the start of the current availability record, move to the previous one (going back in time, remember)
                if (dataPointStartBarrier == availabilityStartBarrier) {
                    currentAvailabilityIndex--;
                }

                // move the current time pointer to the next data point and move back to the next data point start time
                currentTime = dataPointStartBarrier;
                dataPointStartBarrier -= perPointMillis;

                // the division determing perPointMillis drops the remainder, which may leave us slightly short.
                // if we go negative, we're done.
                if (dataPointStartBarrier < 0) {
                    break;
                }

            } else { // the end of the availability record comes first, in the middle of a data point

                switch (currentAvailability.getAvailabilityType()) {
                case UP:
                    // if the resource has been up in the current time frame, bump up the counter
                    timeUpInDataPoint += currentTime - availabilityStartBarrier;
                    break;
                case DOWN:
                    hasDownPeriods = true;
                    break;
                case DISABLED:
                    hasDisabledPeriods = true;
                    break;
                case UNKNOWN:
                default:
                    hasUnknownPeriods = true;
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
         * RHQ-1631, make the latest availability dot match the current availability IF desired by the user
         * note: this must occur AFTER reversing the collection so the last dot refers to the most recent time slice
         */
        if (withCurrentAvailability) {
            AvailabilityPoint oldFirstAvailabilityPoint = availabilityPoints.remove(availabilityPoints.size() - 1);
            AvailabilityType newFirstAvailabilityType = oldFirstAvailabilityPoint.getAvailabilityType();
            if (context.type == EntityContext.Type.Resource) {
                newFirstAvailabilityType = getCurrentAvailabilityTypeForResource(subject, context.resourceId);

            } else if (context.type == EntityContext.Type.ResourceGroup) {
                ResourceGroupComposite composite = resourceGroupManager.getResourceGroupComposite(subject,
                    context.groupId);
                switch (composite.getExplicitAvailabilityType()) {
                case EMPTY:
                    newFirstAvailabilityType = null;
                    break;
                case DOWN:
                case WARN:
                    newFirstAvailabilityType = AvailabilityType.DOWN;
                    break;
                case DISABLED:
                    newFirstAvailabilityType = AvailabilityType.DISABLED;
                    break;
                default:
                    newFirstAvailabilityType = AvailabilityType.UP;
                }

            } else {
                // March 20, 2009: we only support the "summary area" for resources and resourceGroups to date
                // as a result, newFirstAvailabilityType will be a pass-through of the type in oldFirstAvailabilityPoint
            }
            availabilityPoints.add(new AvailabilityPoint(newFirstAvailabilityType, oldFirstAvailabilityPoint
                .getTimestamp()));
        }

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

    // This class does nothing more than give us a way to identify that this is not a real Availability, it's
    // a surrogate used in the AvailabilityPoint logic above.  We used to use a null availType but to flag the
    // surrogate but that is no longer allowed in Availability, and this is more explicit anyway.
    private static class SurrogateAvailability extends Availability {
        private static final long serialVersionUID = 1L;

        public SurrogateAvailability(Resource resource, Long startTime) {
            super(resource, startTime, null);
        }
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void setResourceAvailabilities(int[] resourceIds, AvailabilityType avail) {
        long now = System.currentTimeMillis();
        AvailabilityReport report = new AvailabilityReport(true, null);
        report.setServerSideReport(true);
        for (int resourceId : resourceIds) {
            report.addAvailability(new Datum(resourceId, avail, now));
        }
        this.availabilityManager.mergeAvailabilityReport(report);
        return;
    }

    @TransactionAttribute(TransactionAttributeType.NEVER)
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

        // translate data into Availability objects for downstream processing
        List<Availability> availabilities = new ArrayList<Availability>(report.getResourceAvailability().size());
        for (AvailabilityReport.Datum datum : report.getResourceAvailability()) {
            availabilities.add(new Availability(new Resource(datum.getResourceId()), datum.getStartTime(), datum
                .getAvailabilityType()));
        }

        Integer agentToUpdate = agentManager.getAgentIdByName(agentName);

        // if this report is from an agent update the lastAvailReport time
        if (!report.isServerSideReport() && agentToUpdate != null) {
            availabilityManager.updateLastAvailabilityReportInNewTransaction(agentToUpdate.intValue());
        }

        MergeInfo mergeInfo = new MergeInfo(report);

        // if this report is from an agent, and is a changes-only report, and the agent appears backfilled,
        // then we need to skip this report so as not to waste our time. Then, immediately request and process
        // a full report because, obviously, the agent is no longer down but the server thinks
        // it still is down - we need to know the availabilities for all the resources on that agent
        if (!report.isServerSideReport() && report.isChangesOnlyReport()
            && agentManager.isAgentBackfilled(agentToUpdate.intValue())) {

            mergeInfo.setAskForFullReport(true);

        } else {
            // process the report in batches to avoid an overly long transaction and to potentially increase the
            // speed in which an avail change becomes visible.

            while (!availabilities.isEmpty()) {
                int size = availabilities.size();
                int end = (MERGE_BATCH_SIZE < size) ? MERGE_BATCH_SIZE : size;

                List<Availability> availBatch = availabilities.subList(0, end);
                availabilityManager.mergeAvailabilitiesInNewTransaction(availBatch, mergeInfo);

                // Advance our progress and possibly help GC. This will remove the processed avails from the backing list
                availBatch.clear();
            }

            MeasurementMonitor.getMBean().incrementAvailabilityReports(report.isChangesOnlyReport());
            MeasurementMonitor.getMBean().incrementAvailabilitiesInserted(mergeInfo.getNumInserted());
            MeasurementMonitor.getMBean().incrementAvailabilityInsertTime(watch.getElapsed());
            watch.reset();
        }

        if (!report.isServerSideReport()) {
            if (agentToUpdate != null) {
                // don't bother asking for a full report if the one we are currently processing is already full
                if (mergeInfo.isAskForFullReport() && report.isChangesOnlyReport()) {
                    log.debug("The server is unsure that it has up-to-date availabilities for agent [" + agentName
                        + "]; asking for a full report to be sent");
                    return false;
                }
            } else {
                log.error("Could not figure out which agent sent availability report. "
                    + "This error is harmless and should stop appearing after a short while if the platform of the agent ["
                    + agentName + "] was recently removed. In any other case this is a bug." + report);
            }
        }

        return true; // everything is OK and things look to be in sync
    }

    static class MergeInfo {
        private AvailabilityReport report;
        private int numInserted = 0;
        private boolean askForFullReport = false;

        public MergeInfo(AvailabilityReport report) {
            super();
            this.report = report;
        }

        public int getNumInserted() {
            return numInserted;
        }

        public void incrementNumInserted() {
            ++this.numInserted;
        }

        public boolean isAskForFullReport() {
            return askForFullReport;
        }

        public void setAskForFullReport(boolean askForFullReport) {
            this.askForFullReport = askForFullReport;
        }

        public boolean isEnablementReport() {
            return report.isEnablementReport();
        }

        public boolean isServerSideReport() {
            return report.isServerSideReport();
        }

        public String toString(boolean includeAll) {
            return report.toString(includeAll);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void mergeAvailabilitiesInNewTransaction(List<Availability> availabilities, MergeInfo mergeInfo) {

        // We will alert only on the avails for enabled resources. Keep track of any that are disabled.
        List<Availability> disabledAvailabilities = new ArrayList<Availability>();

        Query q = entityManager.createNamedQuery(Availability.FIND_LATEST_BY_RESOURCE_IDS);
        List<Integer> resourceIds = new ArrayList<Integer>(availabilities.size());
        for (Availability reported : availabilities) {
            resourceIds.add(reported.getResource().getId());
        }
        q.setParameter("resourceIds", resourceIds);
        List<Availability> latestAvailabilitiesList = q.getResultList();
        resourceIds.clear(); // done with this, perhaps helps GC
        resourceIds = null;

        // populate Map of resourceIds to latestAvailability
        // there should be a single latest avail per resource. mark any situation where we have multiple
        Object nonUniqueMarker = new Object();
        Map<Integer, Object> latestAvailabilities = new HashMap(availabilities.size() + 100);
        for (Availability latestAvailability : latestAvailabilitiesList) {
            Integer resourceId = latestAvailability.getResource().getId();
            if (latestAvailabilities.containsKey(resourceId)) {
                latestAvailabilities.put(resourceId, nonUniqueMarker);
            } else {
                latestAvailabilities.put(resourceId, latestAvailability);
            }
        }

        // keep track of the changes in availability so we can update the relevant ResourceAvailabilities in a batch
        List<Availability> changedAvailabilities = new ArrayList<Availability>(availabilities.size());

        for (Availability reported : availabilities) {

            // availability reports only tell us the current state at the start time; end time is ignored/must be null
            reported.setEndTime(null);

            // get the latest avail for the reported resource
            //q.setParameter("resourceId", reported.getResource().getId());
            Integer resourceId = reported.getResource().getId();
            Object latestObject = latestAvailabilities.get(resourceId);
            Availability latest = null;

            if (null == latestObject) { // this is like NoResultException
                // This should not happen unless the Resource in the report is stale, which can happen in certain
                // sync scenarios. A Resource is given its initial Availability/ResourceAvailability when it is
                // persisted so it is guaranteed to have Availability, so, the Resource must not exist. At least
                // it must not exist in my utopian view of the world. Let's just make sure...
                Resource attachedResource = entityManager.find(Resource.class, reported.getResource().getId());

                if ((null == attachedResource) || (InventoryStatus.COMMITTED != attachedResource.getInventoryStatus())) {
                    // expected case
                    log.info("Skipping mergeAvailabilityReport() for stale resource [" + reported.getResource()
                        + "]. These messages should go away after the next agent synchronization with the server.");

                    continue;

                } else {
                    // this should not really happen but is possible in rare failure situations, it means the resource
                    // exists but has no latest Availability record (i.e. sendTime == null).  Correct the situation and
                    // then process the reported avail.
                    log.warn("Resource [" + reported.getResource()
                        + "] has no latest availability record (i.e. no endtime) - will attempt to repair.\n"
                        + mergeInfo.toString(false));

                    try {
                        List<Availability> attachedAvails = attachedResource.getAvailability();
                        Availability attachedLastAvail = null;

                        if (attachedAvails.isEmpty()) {
                            latest = new Availability(attachedResource, 0L, AvailabilityType.UNKNOWN);
                            entityManager.persist(latest);

                        } else {
                            latest = attachedAvails.get(attachedAvails.size() - 1);
                            latest.setEndTime(null);
                            latest = entityManager.merge(latest);
                        }

                        // update the Map to reflect the repaired latest avail
                        latestAvailabilities.put(resourceId, latest);

                        updateResourceAvailability(latest);

                        // ask the agent for a full report so as to ensure we are in sync with agent
                        mergeInfo.setAskForFullReport(true);

                    } catch (Throwable t) {
                        log.warn("Unable to repair NoResult latest availablity for Resource [" + reported.getResource()
                            + "]", t);
                        continue;
                    }
                }
            } else if (latestObject == nonUniqueMarker) { // this is like NonUniqueResultException
                // This condition should never happen.  In my world of la-la land, I've done everything
                // correctly so this never happens.  But, due to the asynchronous nature of things,
                // I have to believe that this still might happen (albeit rarely).  If it does happen,
                // and we do nothing about it - bad things arise.  So, if we find that a resource
                // has 2 or more availabilities with endTime of null, we need to delete all but the
                // latest one (the one whose start time is the latest).  This should correct the
                // problem and allow us to continue processing availability reports for that resource
                log.warn("Resource [" + reported.getResource()
                    + "] has multiple availabilities without an endtime - will attempt to remove the extra ones\n"
                    + mergeInfo.toString(false));

                try {
                    q = entityManager.createNamedQuery(Availability.FIND_CURRENT_BY_RESOURCE);
                    q.setParameter("resourceId", resourceId);

                    List<Availability> latestList = q.getResultList();

                    // delete all but the last one (our query sorts in ASC start time order)
                    int latestCount = latestList.size();
                    for (int i = 0; i < (latestCount - 1); i++) {
                        entityManager.remove(latestList.get(i));
                    }

                    latest = latestList.get(latestCount - 1);
                    updateResourceAvailability(latest);

                    // update the Map to reflect the repaired latest avail
                    latestAvailabilities.put(resourceId, latest);

                    // this is an unusual report - ask the agent for a full report so as to ensure we are in sync with agent
                    mergeInfo.setAskForFullReport(true);

                } catch (Throwable t) {
                    log.warn(
                        "Unable to repair NonUnique Result latest availablity for Resource [" + reported.getResource()
                            + "]", t);
                    continue;
                }
            } else {
                latest = (Availability) latestObject;
            }

            AvailabilityType latestType = latest.getAvailabilityType();
            AvailabilityType reportedType = reported.getAvailabilityType();

            // If the current avail is DISABLED, and this report is not trying to re-enable the resource,
            // Then ignore the reported avail.
            if (AvailabilityType.DISABLED == latestType) {
                if (!(mergeInfo.isEnablementReport() && (AvailabilityType.UNKNOWN == reportedType))) {
                    disabledAvailabilities.add(reported);
                    continue;
                }
            }

            if (reported.getStartTime() >= latest.getStartTime()) {
                //log.info( "new avail (latest/reported)-->" + latest + "/" + reported );

                // the new availability data is for a time after our last known state change
                // we are run-length encoded, so only persist data if the availability changed
                if (latest.getAvailabilityType() != reported.getAvailabilityType()) {
                    entityManager.persist(reported);
                    // the reported avail is the new latest avail, update the Map in case we have multiple reported
                    // changes for the same resource in this report
                    latestAvailabilities.put(resourceId, reported);

                    mergeInfo.incrementNumInserted();

                    latest.setEndTime(reported.getStartTime());
                    latest = entityManager.merge(latest);

                    changedAvailabilities.add(reported);
                }

                // our last known state was unknown, ask for a full report to ensure we are in sync with agent
                if (latest.getAvailabilityType() == AvailabilityType.UNKNOWN) {
                    mergeInfo.setAskForFullReport(true);
                }
            } else {
                //log.info( "past avail (latest/reported)==>" + latest + "/" + reported );

                // The new data is for a time in the past, probably an agent sending a report after
                // a network outage has been corrected but after we have already backfilled.
                // We need to insert it into our past timeline.
                insertAvailability(reported);
                mergeInfo.incrementNumInserted();

                // this is an unusual report - ask the agent for a full report so as to ensure we are in sync with agent
                mergeInfo.setAskForFullReport(true);
            }
        }

        // update the affected ResourceAvailabilities
        updateResourceAvailabilities(changedAvailabilities);

        latestAvailabilities.clear(); // done with these, perhaps helps GC
        latestAvailabilities = null;
        changedAvailabilities.clear();
        changedAvailabilities = null;

        // notify alert condition cache manager for all reported avails for for enabled resources
        availabilities.removeAll(disabledAvailabilities);
        notifyAlertConditionCacheManager("mergeAvailabilityReport",
            availabilities.toArray(new Availability[availabilities.size()]));

        return;
    }

    private void updateResourceAvailability(Availability reported) {
        ResourceAvailability currentAvailability = resourceAvailabilityManager.getLatestAvailability(reported
            .getResource().getId());

        updateResourceAvailabilityIfNecessary(reported, currentAvailability);
    }

    // update the current availability data for this resource but only if necessary (actually changed)
    private void updateResourceAvailabilityIfNecessary(Availability reported, ResourceAvailability currentAvailability) {
        if (currentAvailability != null && currentAvailability.getAvailabilityType() != reported.getAvailabilityType()) {
            currentAvailability.setAvailabilityType(reported.getAvailabilityType());
            entityManager.merge(currentAvailability);

        } else if (currentAvailability == null) {
            // This should not happen unless the Resource in the report is stale, which can happen in certain
            // sync scenarios. A Resource is given its initial ResourceAvailability when it is persisted so it
            // is guaranteed to have currentAvailability, so, the Resource must not exist.
            log.info("Skipping updateResourceAvailabilityIfNecessary() for stale resource [" + reported.getResource()
                + "]. These messages should go away after the next agent synchronization with the server.");
        }
    }

    // pulls all the ResourceAvailabilities in one query to reduce DB round trips
    private void updateResourceAvailabilities(List<Availability> reportedChanges) {
        if (null == reportedChanges || reportedChanges.isEmpty()) {
            return;
        }

        Query q = entityManager.createNamedQuery(ResourceAvailability.QUERY_FIND_BY_RESOURCE_IDS);
        List<Integer> resourceIds = new ArrayList<Integer>(reportedChanges.size());
        for (Availability reported : reportedChanges) {
            resourceIds.add(reported.getResource().getId());
        }
        q.setParameter("resourceIds", resourceIds);
        List<ResourceAvailability> resourceAvailabilityList = q.getResultList();
        resourceIds.clear(); // done with this, perhaps helps GC
        resourceIds = null;

        // populate Map of resourceIds to resourceAvailability
        Map<Integer, ResourceAvailability> resourceAvailabilities = new HashMap(reportedChanges.size());
        for (ResourceAvailability resourceAvailability : resourceAvailabilityList) {
            resourceAvailabilities.put(resourceAvailability.getResourceId(), resourceAvailability);
        }

        for (Availability reported : reportedChanges) {
            ResourceAvailability currentAvailability = resourceAvailabilities.get(reported.getResource().getId());

            // update the last known availability data for this resource but only if necessary (actually changed)
            updateResourceAvailabilityIfNecessary(reported, currentAvailability);
        }

        resourceAvailabilities.clear(); // done with these, perhaps helps GC
        resourceAvailabilities = null;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateLastAvailabilityReportInNewTransaction(int agentId) {
        // should we catch exceptions here, or allow them to bubble up and be caught?

        /*
         * since we already know we have to update the agent row with the last avail report time, might as well
         * set the backfilled to false here (as opposed to called agentManager.setBackfilled(agentId, false)
         */
        String updateStatement = "" //
            + "UPDATE Agent " //
            + "   SET lastAvailabilityReport = :reportTime, backFilled = FALSE " //
            + " WHERE id = :agentId ";

        Query query = entityManager.createQuery(updateStatement);
        query.setParameter("reportTime", System.currentTimeMillis());
        query.setParameter("agentId", agentId);

        query.executeUpdate();
    }

    @SuppressWarnings("unchecked")
    public void updateAgentResourceAvailabilities(int agentId, AvailabilityType platformAvailType,
        AvailabilityType childAvailType) {

        platformAvailType = (null == platformAvailType) ? AvailabilityType.DOWN : platformAvailType;
        childAvailType = (null == childAvailType) ? AvailabilityType.UNKNOWN : childAvailType;

        // get the platform resource if not already at platformAvailType (since this is the one
        // we need to change)
        Query query = entityManager
            .createNamedQuery(Availability.FIND_PLATFORM_COMPOSITE_BY_AGENT_AND_NONMATCHING_TYPE);
        query.setParameter("agentId", agentId);
        query.setParameter("availabilityType", platformAvailType);
        // should be 0 or 1 entry
        List<ResourceIdWithAvailabilityComposite> platformResourcesWithStatus = query.getResultList();

        // get the child resources not disabled and not already at childAvailType
        // (since these are the ones we need to change)
        query = entityManager.createNamedQuery(Availability.FIND_CHILD_COMPOSITE_BY_AGENT_AND_NONMATCHING_TYPE);
        query.setParameter("agentId", agentId);
        query.setParameter("availabilityType", childAvailType);
        query.setParameter("disabled", AvailabilityType.DISABLED);
        List<ResourceIdWithAvailabilityComposite> resourcesWithStatus = query.getResultList();

        // The above queries only return resources if they have at least one row in Availability. This should
        // not be a problem since a new Resource gets an initial UNKNOWN Availability record at persist-time.

        if (log.isDebugEnabled()) {
            log.debug("Agent #[" + agentId + "] is going to have [" + resourcesWithStatus.size()
                + "] resources backfilled with [" + childAvailType.getName() + "]");
        }

        Date now = new Date();

        int newAvailsSize = platformResourcesWithStatus.size() + resourcesWithStatus.size();
        List<Availability> newAvailabilities = new ArrayList<Availability>(newAvailsSize);

        // if the platform is being set to a new status handle it now
        if (!platformResourcesWithStatus.isEmpty()) {
            Availability newAvailabilityInterval = getNewInterval(platformResourcesWithStatus.get(0), now,
                platformAvailType);
            if (newAvailabilityInterval != null) {
                newAvailabilities.add(newAvailabilityInterval);
            }

            resourceAvailabilityManager.updateAgentResourcesLatestAvailability(agentId, platformAvailType, true);
        }

        // for those resources that have a current availability status that is different, change them
        for (ResourceIdWithAvailabilityComposite record : resourcesWithStatus) {
            Availability newAvailabilityInterval = getNewInterval(record, now, childAvailType);
            if (newAvailabilityInterval != null) {
                newAvailabilities.add(newAvailabilityInterval);
            }
        }

        resourceAvailabilityManager.updateAgentResourcesLatestAvailability(agentId, childAvailType, false);

        // To handle backfilling process, which will mark them unknown
        notifyAlertConditionCacheManager("setAllAgentResourceAvailabilities",
            newAvailabilities.toArray(new Availability[newAvailabilities.size()]));

        if (log.isDebugEnabled()) {
            log.debug("Resources for agent #[" + agentId + "] have been fully backfilled.");
        }

        return;
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

            old.setEndTime(startDate.getTime());
        }

        Resource resource = new Resource();
        resource.setId(record.getResourceId());

        Availability newAvail = new Availability(resource, startDate.getTime(), aType);
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
        query.setParameter("aTime", toInsert.getStartTime());

        Availability existing;

        try {
            existing = (Availability) query.getSingleResult();

        } catch (NoResultException nre) {
            // this should never happen since we create an initial Availability when the resource is persisted.
            log.warn("Resource [" + toInsert.getResource()
                + "] has no Availabilities, this should not happen.  Correcting situation by adding an Availability.");

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

        } catch (NonUniqueResultException nure) {
            // This should not happen but can happen if the startTime exactly matches an existing start time. In
            // this case assume we have somehow been passed a duplicate report, and ignore the entry.
            log.warn("Resource [" + toInsert.getResource()
                + "] received a duplicate Availability. It is being ignored: " + toInsert);

            return;
        }

        // If we are inserting the same availability type, the first one can just continue
        // and there is nothing to do!
        if (existing.getAvailabilityType() != toInsert.getAvailabilityType()) {

            // get the afterExisting availability. note: we are assured this query will return something;
            // semantics of this method is that it is never called if we are inserting in the last interval
            query = entityManager.createNamedQuery(Availability.FIND_BY_RESOURCE_AND_DATE);
            query.setParameter("resourceId", toInsert.getResource().getId());
            query.setParameter("aTime", existing.getEndTime() + 1);
            Availability afterExisting = (Availability) query.getSingleResult();

            if (toInsert.getAvailabilityType() == afterExisting.getAvailabilityType()) {
                // the inserted avail type is the same as the following avail type, we don't need to
                // insert a new avail record, just adjust the start/end times of the existing records.

                if (existing.getStartTime() == toInsert.getStartTime()) {
                    // Edge Case: If the insertTo start time equals the existing start time
                    // just remove the existing record and let afterExisting cover the interval.
                    entityManager.remove(existing);
                } else {
                    existing.setEndTime(toInsert.getStartTime());
                }

                // stretch next interval to cover the inserted interval
                afterExisting.setStartTime(toInsert.getStartTime());

            } else {
                // the inserted avail type is NOT the same as the following avail type, we likely need to
                // insert a new avail record.

                if (existing.getStartTime() == toInsert.getStartTime()) {
                    // Edge Case: If the insertTo start time equals the existing end time
                    // just update the existing avail type to be the new avail type and keep the same boundary.
                    existing.setAvailabilityType(toInsert.getAvailabilityType());

                } else {
                    // insert the new avail type interval, witch is different than existing and afterExisting.
                    existing.setEndTime(toInsert.getStartTime());
                    toInsert.setEndTime(afterExisting.getStartTime());
                    entityManager.persist(toInsert);
                }
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
     * @Deprecated used in portal war EventsView.jsp.  Use {@link #findAvailabilityByCriteria(Subject, AvailabilityCriteria)}
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    public List<Availability> findAvailabilityWithinInterval(int resourceId, Date startDate, Date endDate) {
        Query q = entityManager.createNamedQuery(Availability.FIND_FOR_RESOURCE_WITHIN_INTERVAL);
        q.setParameter("resourceId", resourceId);
        q.setParameter("start", startDate.getTime());
        q.setParameter("end", endDate.getTime());
        List<Availability> results = q.getResultList();
        return results;
    }

    @SuppressWarnings("unchecked")
    public PageList<Availability> findAvailabilityByCriteria(Subject subject, AvailabilityCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);

        if (authorizationManager.isInventoryManager(subject) == false) {
            generator.setAuthorizationResourceFragment(CriteriaQueryGenerator.AuthorizationTokenType.RESOURCE,
                "resource", subject.getId());
        }

        CriteriaQueryRunner<Availability> queryRunner = new CriteriaQueryRunner(criteria, generator, entityManager);
        PageList<Availability> result = queryRunner.execute();
        return result;
    }

    /**
     * @return all avails for all member resources for the specified interval, ordered by asc startTime
     */
    @SuppressWarnings("unchecked")
    private List<Availability> findResourceGroupAvailabilityWithinInterval(int groupId, Date startDate, Date endDate) {
        Query q = entityManager.createNamedQuery(Availability.FIND_FOR_RESOURCE_GROUP_WITHIN_INTERVAL);
        q.setParameter("groupId", groupId);
        q.setParameter("start", startDate.getTime());
        q.setParameter("end", endDate.getTime());
        List<Availability> results = q.getResultList();
        return results;
    }

    /**
     * @Deprecated used in portal war, should probably go away when portal war goes away
     */
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

    /**
     * @Deprecated used in portal war ListAvailabilityHistoryUIBEan.  Use {@link #findAvailabilityByCriteria(Subject, AvailabilityCriteria)}
     * Note that this methods uses startTime DESC sorting, which must be explicitly set in AvailabilityCriteria.
     */
    @Deprecated
    public PageList<Availability> findAvailabilityForResource(Subject subject, int resourceId, PageControl pageControl) {
        if (authorizationManager.canViewResource(subject, resourceId) == false) {
            throw new PermissionException("User [" + subject
                + "] does not have permission to view Availability history for resource[id=" + resourceId + "]");
        }

        pageControl.initDefaultOrderingField("av.startTime", PageOrdering.DESC);

        Query countQuery = PersistenceUtility.createCountQuery(entityManager, Availability.FIND_BY_RESOURCE_NO_SORT);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, Availability.FIND_BY_RESOURCE_NO_SORT,
            pageControl);

        countQuery.setParameter("resourceId", resourceId);
        query.setParameter("resourceId", resourceId);

        long count = (Long) countQuery.getSingleResult();
        List<Availability> availabilities = query.getResultList();

        return new PageList<Availability>(availabilities, (int) count, pageControl);
    }

    private void notifyAlertConditionCacheManager(String callingMethod, Availability... availabilities) {
        AlertConditionCacheStats stats = alertConditionCacheManager.checkConditions(availabilities);

        if (log.isDebugEnabled()) {
            log.debug(callingMethod + ": " + stats.toString());
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void scheduleAvailabilityDurationCheck(AvailabilityDurationCacheElement cacheElement, Resource resource) {

        String operator = cacheElement.getAlertConditionOperator().name();
        String durationString = (String) cacheElement.getAlertConditionOperatorOption();
        long duration = Long.valueOf(durationString).longValue() * 1000;

        if (log.isDebugEnabled()) {
            Date jobTime = new Date(System.currentTimeMillis() + duration);
            log.debug("Scheduling availability duration job for [" + DateFormat.getDateTimeInstance().format(jobTime)
                + "]");
        }

        HashMap<String, String> infoMap = new HashMap<String, String>();
        // the condition id is needed to ensure we limit the future avail checking to the one relevant alert condition
        infoMap.put(AlertAvailabilityDurationJob.DATAMAP_CONDITION_ID,
            String.valueOf(cacheElement.getAlertConditionTriggerId()));
        infoMap.put(AlertAvailabilityDurationJob.DATAMAP_RESOURCE_ID, String.valueOf(resource.getId()));
        infoMap.put(AlertAvailabilityDurationJob.DATAMAP_OPERATOR, operator);
        infoMap.put(AlertAvailabilityDurationJob.DATAMAP_DURATION, durationString); // in seconds

        timerService.createSingleActionTimer(duration, new TimerConfig(infoMap, false));
    }

    @Timeout
    public void handleAvailabilityDurationCheck(Timer timer) {
        try {
            AlertAvailabilityDurationJob.execute((HashMap<String, String>) timer.getInfo());
        } catch (Throwable t) {
            log.error("Failed to handle availability duration timer - will try again later. Cause: " + t);
        }
    }
}
