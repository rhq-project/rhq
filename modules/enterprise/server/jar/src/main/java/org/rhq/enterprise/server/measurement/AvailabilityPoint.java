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

import java.io.Serializable;
import java.util.Date;
import org.rhq.core.domain.measurement.AvailabilityType;

/**
 * An {@link AvailabilityType} for a point in time. This object not only tells you if the resource is UP or DOWN but it
 * also tells you if the status is known or unknown (that is, either the data existed in the database and the
 * availability type was known; or there is no data in the database that explicitly tells us what the resource status is
 * - in that case, the availability will be DOWN but it will be listed as "unknown".
 *
 * @author Heiko W. Rupp
 * @author John Mazzitelli
 */
public class AvailabilityPoint implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int value;
    private final long timestamp;
    private final boolean known;

    /**
     * Creates an availability point with an explicitly known value.
     *
     * @param value     the availability status
     * @param timestamp the time when the resource was in the given availability status
     */
    public AvailabilityPoint(AvailabilityType value, long timestamp) {
        if (value != null) {
            this.value = value.ordinal();
            this.timestamp = timestamp;
            this.known = true;
        } else {
            this.value = AvailabilityType.DOWN.ordinal();
            this.timestamp = timestamp;
            this.known = false;
        }
    }

    /**
     * Creates an availability point with an unknown value (whose {@link #getAvailabilityType()} will return
     * {@link AvailabilityType#DOWN}.
     *
     * @param timestamp the time when the resource was in the given availability status
     */
    public AvailabilityPoint(long timestamp) {
        this.value = AvailabilityType.DOWN.ordinal();
        this.timestamp = timestamp;
        this.known = false;
    }

    /**
     * Returns <code>true</code> if the availability data is explicitly known. If <code>false</code>, there was no
     * explicit data in the database that indicated an availability, so the resource's availability will be assumed to
     * be DOWN.
     *
     * @return <code>true</code> if {@link #getAvailabilityType()} returns a status that is explicitly known about the
     *         resource
     */
    public boolean isKnown() {
        return known;
    }

    /**
     * The timestamp that this data point object represents.
     *
     * @return the time when the resource was at the given {@link #getAvailabilityType() availability status}
     */
    public long getTimestamp() {
        return this.timestamp;
    }

    public AvailabilityType getAvailabilityType() {
        return AvailabilityType.values()[this.value];
    }

    /**
     * Returns the ordinal value of the {@link #getAvailabilityType() availability type}.
     *
     * @return availability type ordinal value
     *
     * @see    #getAvailabilityType()
     */
    public int getValue() {
        return this.value;
    }

    public String toString() {
        StringBuilder str = new StringBuilder("AvailabilityPoint ");
        str.append("value=[" + this.value);
        if (!this.known) {
            str.append('?');
        }

        str.append("], timestamp=[" + this.timestamp);
        str.append("(" + new Date(this.timestamp) + ")]");

        return str.toString();
    }
}