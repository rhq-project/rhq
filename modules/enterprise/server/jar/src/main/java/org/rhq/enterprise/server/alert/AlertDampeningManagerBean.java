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
package org.rhq.enterprise.server.alert;

import java.util.EnumSet;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.alert.AlertDampening;
import org.rhq.core.domain.alert.AlertDampeningEvent;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertDampeningEvent.Type;
import org.rhq.enterprise.server.RHQConstants;

/**
 * @author Joseph Marques
 */

@Stateless
public class AlertDampeningManagerBean implements AlertDampeningManagerLocal {

    private final Log log = LogFactory.getLog(AlertDampeningManagerBean.class);

    @EJB
    private AlertManagerLocal alertManager;

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    public AlertDampeningEvent getLatestEventByAlertDefinitionId(int alertDefinitionId) {
        Query latestEventQuery = entityManager
            .createNamedQuery(AlertDampeningEvent.QUERY_FIND_LATEST_BY_ALERT_DEFINITION_ID);
        latestEventQuery.setParameter("alertDefinitionId", alertDefinitionId);
        try {
            AlertDampeningEvent latestEvent = (AlertDampeningEvent) latestEventQuery.getSingleResult();
            return latestEvent;
        } catch (NoResultException nre) {
            return null; // expected a good deal of the time
        }
    }

    @SuppressWarnings("unchecked")
    private boolean shouldFireDurationCountAlert(int alertDefinitionId, int eventCountThreshold, long lastSeconds) {

        long oldestEventTime = System.currentTimeMillis() - (lastSeconds * 1000);

        Query query = entityManager.createNamedQuery(AlertDampeningEvent.QUERY_FIND_BY_TIME_AND_TYPES);
        query.setParameter("alertDefinitionId", alertDefinitionId);
        query.setParameter("eventTypes", EnumSet.of(Type.POSITIVE, Type.POSITIVE_AGAIN));
        query.setParameter("oldestEventTime", oldestEventTime);

        /*
         * Make sure we get at most the number of events we need in order to trigger the DURATION_COUNT dampening rule
         */
        query.setMaxResults(eventCountThreshold);

        List<AlertDampeningEvent> oldestEvents = query.getResultList();
        deleteAlertEventsOlderThan(alertDefinitionId, oldestEventTime);

        // if we have enough, it'll be exactly equal to the number need (thanks to setMaxResults)
        boolean shouldFire = (oldestEvents.size() == eventCountThreshold);
        log.debug("Need " + eventCountThreshold + " events " + "for the last " + lastSeconds + " seconds" + ", "
            + "found " + oldestEvents.size());

        if (shouldFire) {
            for (AlertDampeningEvent event : oldestEvents) {
                event.setAlertDefinition(null);
                entityManager.remove(event);
            }
        }

        // let the caller know what happened, so they can appropriately fire an alert or not
        return shouldFire;
    }

    public void processEventType(int alertDefinitionId, AlertDampeningEvent.Type eventType) {
        try {
            // get the alert definition in preparation for lots of processing on it
            AlertDefinition alertDefinition = entityManager.find(AlertDefinition.class, alertDefinitionId);

            /*
             * some dampening event occurred, handle it accordingly.  if it was positive, check whether this
             * AlertDefinition can fire an alert according to its dampening category rules.  currently, these is no
             * supported dampening event that can fire as the result of a partial condition set match.
             */
            boolean fire = false;

            AlertDampening alertDampening = alertDefinition.getAlertDampening();
            AlertDampening.Category category = alertDampening.getCategory();

            log.debug("Alert condition processing for " + alertDefinition);
            log.debug("Dampening rules are: " + alertDampening);

            if (category == AlertDampening.Category.NONE) {
                if ((eventType == AlertDampeningEvent.Type.POSITIVE)
                    || (eventType == AlertDampeningEvent.Type.POSITIVE_AGAIN)) {
                    /*
                     * technically we should always fire for the NONE category, but since this method has other
                     * consequences we'll call it and pass 1 as the second argument
                     */
                    fire = this.shouldFireConsecutiveCountAlert(alertDefinitionId, 1);
                }
            } else if (category == AlertDampening.Category.CONSECUTIVE_COUNT) {
                /*
                 * we don't care if the condition set becomes untrue, we need a number of events to be true in a row; a
                 * false event effectively resets that counter, so we need not perform a check in that instance
                 */
                if ((eventType == AlertDampeningEvent.Type.POSITIVE)
                    || (eventType == AlertDampeningEvent.Type.POSITIVE_AGAIN)) {
                    int count = alertDampening.getValue();

                    fire = this.shouldFireConsecutiveCountAlert(alertDefinitionId, count);
                }
            } else if (category == AlertDampening.Category.PARTIAL_COUNT) {
                if ((eventType == AlertDampeningEvent.Type.POSITIVE)
                    || (eventType == AlertDampeningEvent.Type.POSITIVE_AGAIN)) {
                    int count = alertDampening.getValue();
                    int period = alertDampening.getPeriod();

                    fire = this.shouldFirePartialCountAlert(alertDefinitionId, count, period);
                }
            } else if (category == AlertDampening.Category.DURATION_COUNT) {
                /*
                 * we don't care if the condition set becomes untrue, the count is all about how many times it was known
                 * to be positive (or positive again) during the collection period
                 */
                if ((eventType == AlertDampeningEvent.Type.POSITIVE)
                    || (eventType == AlertDampeningEvent.Type.POSITIVE_AGAIN)) {
                    int count = alertDampening.getValue();
                    long period = alertDampening.getPeriod() * alertDampening.getPeriodUnits().getNumberOfSeconds();

                    // check whether "value" number of positive events have occurred within the last "period" seconds
                    fire = this.shouldFireDurationCountAlert(alertDefinitionId, count, period);
                }
            } else {
                log.info("Category " + alertDampening.getCategory()
                    + " is not supported for alert dampening processing");
            }

            /*
             * If the dampening rules say we can fire, then create a new alert, and find the unmatched alert condition
             * logs and attach them to this new alert.
             */
            if (fire) {
                log.debug("Dampening rules were satisfied");
                alertManager.fireAlert(alertDefinitionId);
            } else {
                log.debug("Dampening rules were not satisfied");
            }
        } catch (Exception e) {
            log.error("Error operating on the passed dampening eventType of " + eventType + " "
                + "for the alert definition with id of " + alertDefinitionId, e);
        }
    }

    private boolean shouldFireConsecutiveCountAlert(int alertDefinitionId, long count) {
        // consecutive is a more specific case of general where count == period
        return shouldFirePartialCountAlert(alertDefinitionId, count, count);
    }

    private boolean shouldFirePartialCountAlert(int alertDefinitionId, long countNeeded, long period) {

        List<AlertDampeningEvent> events = getRecentAlertDampeningEvents(alertDefinitionId, period);
        deleteAlertEventsOlderThan(alertDefinitionId, events.get(events.size() - 1).getEventTime());

        long positiveFires = 0;
        for (AlertDampeningEvent event : events) {
            if ((event.getEventType() == AlertDampeningEvent.Type.POSITIVE)
                || (event.getEventType() == AlertDampeningEvent.Type.POSITIVE_AGAIN)) {
                positiveFires++;
            }
        }

        log.debug("Need " + countNeeded + " events " + "for the last " + period + " events" + ", " + "found "
            + positiveFires);

        if (positiveFires >= countNeeded) {
            for (AlertDampeningEvent event : events) {
                event.setAlertDefinition(null);
                entityManager.remove(event);
            }
            return true;
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private List<AlertDampeningEvent> getRecentAlertDampeningEvents(int alertDefinitionId, long maxResults) {
        Query query = entityManager.createNamedQuery(AlertDampeningEvent.QUERY_FIND_BY_ALERT_DEFINITION_ID);
        query.setParameter("alertDefinitionId", alertDefinitionId);
        query.setMaxResults((int) maxResults);

        List<AlertDampeningEvent> results = query.getResultList();
        return results;
    }

    private void deleteAlertEventsOlderThan(Integer alertDefinitionId, long olderThan) {
        Query query = entityManager.createNamedQuery(AlertDampeningEvent.QUERY_DELETE_BY_TIMESTAMP);
        query.setParameter("alertDefinitionId", alertDefinitionId);
        query.setParameter("oldest", olderThan);

        int deletedCount = query.executeUpdate();

        if (deletedCount > 0) {
            log.debug("Deleted " + deletedCount + " stale AlertDampeningEvent" + ((deletedCount == 1) ? "" : "s")
                + " for AlertDefinition[id=" + alertDefinitionId + "]");
        }
    }

}