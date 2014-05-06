/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.domain.resource.composite;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;

/**
 * Summary information about a resource's availability history
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class ResourceAvailabilitySummary implements Serializable {

    private static final long serialVersionUID = 1L;

    private long now; // set to the current time when this object was created
    private long upTime;
    private long downTime;
    private long disabledTime;
    private long unknownTime;
    private int failures;
    private int disabled;
    private long lastChange;
    private AvailabilityType current;

    public ResourceAvailabilitySummary(List<Availability> availabilities) {
        this.now = System.currentTimeMillis();

        long upTime = 0;
        long downTime = 0;
        long disabledTime = 0;
        long unknownTime = 0;
        int failures = 0;
        int disabled = 0;
        long lastChange = 0;
        AvailabilityType current = AvailabilityType.UNKNOWN;

        for (Availability avail : availabilities) {
            switch (avail.getAvailabilityType()) {
            case UP: {
                upTime += getStartEndTimeDuration(avail);
                break;
            }
            case DOWN: {
                downTime += getStartEndTimeDuration(avail);
                failures++;
                break;
            }
            case DISABLED: {
                disabledTime += getStartEndTimeDuration(avail);
                disabled++;
                break;
            }
            case UNKNOWN: {
                // ignore the initial unknown range that occurs prior to a resource being in inventory
                if (avail.getStartTime() > 0L) {
                    unknownTime += getStartEndTimeDuration(avail);
                }
                break;
            }
            default:
                // Only stored avail types are relevant, MISSING, for example, is never stored
                break;
            }

            if (avail.getEndTime() == null) {
                lastChange = avail.getStartTime().longValue();
                current = avail.getAvailabilityType();
            }
        }

        this.upTime = upTime;
        this.downTime = downTime;
        this.disabledTime = disabledTime;
        this.unknownTime = unknownTime;
        this.failures = failures;
        this.disabled = disabled;
        this.lastChange = lastChange;
        this.current = current;
    }

    public ResourceAvailabilitySummary(long now, long upTime, long downTime, long disabledTime, long unknownTime,
        int failures, int disabled, long lastChange, AvailabilityType current) {
        this.now = now;
        this.upTime = upTime;
        this.downTime = downTime;
        this.disabledTime = disabledTime;
        this.unknownTime = unknownTime;
        this.failures = failures;
        this.disabled = disabled;
        this.lastChange = lastChange;
        this.current = current;
    }

    protected ResourceAvailabilitySummary() {
        // required for GWT
    }

    /**
     * Returns the mean-time-between-failures metric. The computation only takes into
     * account the uptimes and downtimes. Any time periods of UNKNOWN or DISABLED
     * availability are ignored.
     *
     * If there has been no failures yet, or there has only been one failure, the
     * MTBF cannot be calculated - in this case, 0 is returned.
     *
     * @return MTBF value in milliseconds
     */
    public long getMTBF() {
        if (failures <= 1) {
            return 0;
        }

        // if our current state is UP, don't count the current up period in the calculations
        if (this.current == AvailabilityType.UP) {
            return (upTime - (this.now - this.lastChange)) / (failures - 1);
        } else {
            return upTime / (failures - 1);
        }
    }

    /**
     * Returns the mean-time-to-repair metric. The computation only takes into
     * account the uptimes and downtimes. Any time periods of UNKNOWN or DISABLED
     * availability are ignored.
     *
     * If there has been no failures yet, the MTTR cannot be calculated - in this case, 0 is returned.
     *
     * @return MTTR value in milliseconds
     */
    public long getMTTR() {
        if (failures <= 0) {
            return 0;
        }

        // if our current state is DOWN, don't count the current down period in the calculations
        // since we don't have a subsequent UP and thus don't know the length of time it will be down before repair.
        if (this.current == AvailabilityType.DOWN) {
            return (failures > 1) ? (downTime - (this.now - this.lastChange)) / (failures - 1) : 0;
        } else {
            return downTime / failures;
        }
    }

    /**
     * Returns the percentage of time the availability has been UP.
     * For the purposes of this calculation, a DISABLED time period
     * explicitly counts as a "non-UP" time period.
     * This ignores any time periods of UNKNOWN availability.
     *
     * @return uptime percentage
     */
    public double getUpPercentage() {
        long totalTime = upTime + downTime + disabledTime;
        return totalTime != 0 ? (((double) upTime) / totalTime) : 0;
    }

    /**
     * Returns the time the availability has been UP in milliseconds.
     * For the purposes of this calculation, only the times that UP
     * was the explicit state are used (that is, DOWN, DISABLED and UNKNOWN
     * time periods are excluded from the returned cumulative uptime).
     *
     * @return cumulative uptime in milliseconds
     */
    public long getUpTime() {
        return upTime;
    }

    /**
     * Returns the percentage of time the availability has been DOWN.
     * For the purposes of this calculation, a DISABLED time period
     * is not considered DOWN.
     * This ignores any time periods of UNKNOWN availability.
     *
     * @return downtime percentage
     */
    public double getDownPercentage() {
        long totalTime = upTime + downTime + disabledTime;
        return totalTime != 0 ? (((double) downTime) / totalTime) : 0;
    }

    /**
     * Returns the time the availability has been DOWN in milliseconds.
     * For the purposes of this calculation, only the times that DOWN
     * was the explicit state are used (that is, UP, DISABLED and UNKNOWN
     * time periods are excluded from the returned cumulative downtime).
     *
     * @return cumulative downtime in milliseconds
     */
    public long getDownTime() {
        return downTime;
    }

    /**
     * Returns the percentage of time the availability has been DISABLED.
     * This ignores any time periods of UNKNOWN availability.
     *
     * @return percentage of time availability was disabled
     */
    public double getDisabledPercentage() {
        long totalTime = upTime + downTime + disabledTime;
        return totalTime != 0 ? (((double) disabledTime) / totalTime) : 0;
    }

    /**
     * Returns the time the availability has been DISABLED in milliseconds.
     *
     * @return cumulative time in disabled state, in milliseconds
     */
    public long getDisabledTime() {
        return disabledTime;
    }

    /**
     * Returns the time the availability has been UNKNOWN in milliseconds.
     *
     * @return cumulative time the availability was unknown, in milliseconds
     */
    public long getUnknownTime() {
        return unknownTime;
    }

    /**
     * Returns the time the availability was in a known state
     * (which includes UP, DOWN and DISABLED). This obviously does not include
     * the times which the availability was in the UNKNOWN state.
     *
     * @return cumulative time the availability was known, in milliseconds
     */
    public long getKnownTime() {
        return upTime + downTime + disabledTime;
    }

    /**
     * Returns the number of discrete times the availability went DOWN, which is
     * considered a failure.
     *
     * @return count of the number of failures
     */
    public int getFailures() {
        return failures;
    }

    /**
     * Returns the number of discrete times the availability flipped to DISABLED.
     *
     * @return count of the number of times availability was disabled
     */
    public int getDisabled() {
        return disabled;
    }

    public Date getLastChange() {
        return new Date(lastChange);
    }

    public AvailabilityType getCurrent() {
        return current;
    }

    /**
     * Returns the "current time" which is actually the time that was current at the time
     * when this object was created.
     *
     * @return the "current time" when this object was created
     */
    public long getCurrentTime() {
        return now;
    }

    private long getStartEndTimeDuration(Availability avail) {
        long endTime = (avail.getEndTime() != null ? avail.getEndTime().longValue() : this.now);
        return endTime - avail.getStartTime().longValue();
    }
}
