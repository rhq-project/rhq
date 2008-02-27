/*
 * JBoss, a division of Red Hat.
 * Copyright 2008, Red Hat Middleware, LLC. All rights reserved.
 */
package org.rhq.core.domain.event;

/**
 * The severity of an {@link Event}. Values are ordered from least to most severe.
 */
public enum EventSeverity {

    DEBUG, INFO, WARN, ERROR, FATAL;

    /**
     * Determine if this severity is more severe than that of other
     * @param other EventSeverity to compare
     * @return if more severe
     */
    public boolean isMoreSevereThan(EventSeverity other) {
        if (other == null) // If other is null, our severity is always higher
            return true;

        return (ordinal() > other.ordinal());
    }

    /**
     * Determine if this severity is at least as severe as that of other
     * @param other EventSeverity to compare
     * @return if at least as severe
     */
    public boolean isAtLeastAsSevereAs(EventSeverity other) {
        if (other == null) // If other is null, our severity is always higher
            return true;

        return (ordinal() >= other.ordinal());
    }

    /**
     * Getter for use in JSP pages
     * @return the same value as {@link EventSeverity#ordinal()}
     */
    public int getOrdinal() {
        return ordinal();
    }

    public EventSeverity[] getValues() {
        return values();
    }
}
