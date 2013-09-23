/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.plugins.apache;

import static org.rhq.core.domain.measurement.AvailabilityType.DOWN;
import static org.rhq.core.domain.measurement.AvailabilityType.UP;

import org.rhq.core.domain.measurement.AvailabilityType;

/**
 * @author Thomas Segismont
 */
public final class AvailabilityResult {

    public enum ErrorType {
        CANNOT_CONNECT, CONNECTION_TIMEOUT, UNKNOWN, NONE
    }

    private final AvailabilityType availabilityType;

    private final ErrorType errorType;

    private final String message;

    private final Throwable throwable;

    private AvailabilityResult(AvailabilityType availabilityType, ErrorType errorType, String message,
        Throwable throwable) {
        this.availabilityType = availabilityType;
        this.errorType = errorType;
        this.message = message;
        this.throwable = throwable;
    }

    public static AvailabilityResult availabilityIsUp() {
        return new AvailabilityResult(UP, ErrorType.NONE, null, null);
    }

    public static AvailabilityResult availabilityIsDown(String message) {
        return availabilityIsDown(ErrorType.UNKNOWN, message, null);
    }

    public static AvailabilityResult availabilityIsDown(String message, Throwable throwable) {
        return availabilityIsDown(ErrorType.UNKNOWN, message, throwable);
    }

    public static AvailabilityResult availabilityIsDown(ErrorType errorType, String message) {
        return availabilityIsDown(errorType, message, null);
    }

    public static AvailabilityResult availabilityIsDown(ErrorType errorType, String message, Throwable throwable) {
        if (errorType == null) {
            throw new IllegalArgumentException("errorType is null");
        }
        if (errorType == ErrorType.NONE) {
            throw new IllegalArgumentException("Cannot set errorType to '" + ErrorType.NONE.name()
                + "' for availabilityType '" + DOWN.name() + "'");
        }
        return new AvailabilityResult(DOWN, errorType, null, null);
    }

    public AvailabilityType getAvailabilityType() {
        return availabilityType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getThrowable() {
        return throwable;
    }
}
