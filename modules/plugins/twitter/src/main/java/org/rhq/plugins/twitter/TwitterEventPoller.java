/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.twitter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import twitter4j.Status;
import twitter4j.Tweet;

import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.pluginapi.event.EventPoller;

/**
 * Poller class to feed Twitter Status information into
 * the Event subsystem.
 *
 * @author Heiko W. Rupp
 */
public class TwitterEventPoller implements EventPoller {

   private final List<Event> events = new Vector<Event>();
   private String eventType;


    public TwitterEventPoller(String eventType) {
      this.eventType = eventType;
    }

    /** Return the type of events we handle
     * @see org.rhq.core.pluginapi.event.EventPoller#getEventType()
     */
    public String getEventType() {
        return eventType;
    }


    /** Return collected events
     * @see org.rhq.core.pluginapi.event.EventPoller#poll()
     */
    public Set<Event> poll() {
        Set<Event> eventSet = new HashSet<Event>();

        synchronized (events) {
            eventSet.addAll(events);
            events.clear();
        }
        return eventSet;
    }

    /**
     * Add statuses to the list of events to return
     * @param statuses
     */
    public void addStatuses(List<Status> statuses) {

        synchronized (events) {
            for (Status status: statuses) {
                Event ev = new Event(getEventType(), // Event Type
                        status.getUser().getName(), // SourceLocation
                        status.getCreatedAt().getTime(), // Timestamp
                        EventSeverity.INFO, // Severity -- just all the same for now
                        status.getText()
                        );
                events.add(ev);
            }
        }
    }

    /**
     * Add tweets to the list of events to return
     * @param tweets
     */
    public void addTweets(List<Tweet> tweets) {
        synchronized (events) {
            for (Tweet tweet: tweets) {
                Event ev = new Event(getEventType(),
                        tweet.getFromUser(),
                        tweet.getCreatedAt().getTime(),
                        EventSeverity.INFO,
                        tweet.getText());
                events.add(ev);
            }
        }
    }
}