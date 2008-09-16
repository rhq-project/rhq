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
