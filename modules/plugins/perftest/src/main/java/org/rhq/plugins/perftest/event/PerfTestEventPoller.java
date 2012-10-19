package org.rhq.plugins.perftest.event;

import java.util.HashSet;
import java.util.Set;

import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.pluginapi.event.EventPoller;
import org.rhq.core.pluginapi.inventory.ResourceContext;

/**
 * Emits events when polled. Set the following system properties to define behavior:
 *
 * rhq.perftest.events.count (default is "1")
 * rhq.perftest.events.severity (default is "INFO")
 * rhq.perftest.events.polling-interval (in seconds; if this is not set, this poller object will never be created)
 */
public class PerfTestEventPoller implements EventPoller {

    public static final String PERFTEST_EVENT_TYPE = "PerfTestEventType";
    public static final String SYSPROP_EVENTS_POLLING_INTERVAL = "rhq.perftest.events.polling-interval";
    public static final String SYSPROP_EVENTS_COUNT = "rhq.perftest.events.count";
    public static final String SYSPROP_EVENTS_SEVERITY = "rhq.perftest.events.severity";

    private final ResourceContext resourceContext;

    public PerfTestEventPoller(ResourceContext resourceContext) {
        this.resourceContext = resourceContext;
    }

    public String getEventType() {
        return PERFTEST_EVENT_TYPE;
    }

    public Set<Event> poll() {
        int count = Integer.parseInt(System.getProperty(SYSPROP_EVENTS_COUNT, "1"));
        String severityString = System.getProperty(SYSPROP_EVENTS_SEVERITY, EventSeverity.INFO.name());
        EventSeverity severity = EventSeverity.valueOf(severityString);
        Set<Event> events = new HashSet<Event>(count);
        for (int i = 0; i < count; i++) {
            Event event = new Event(PERFTEST_EVENT_TYPE, "source.loc", System.currentTimeMillis(), severity, "event #"
                + i);
            events.add(event);
        }
        return events;
    }
}
